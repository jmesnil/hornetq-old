/*
  * JBoss, Home of Professional Open Source
  * Copyright 2005, JBoss Inc., and individual contributors as indicated
  * by the @authors tag. See the copyright.txt in the distribution for a
  * full listing of individual contributors.
  *
  * This is free software; you can redistribute it and/or modify it
  * under the terms of the GNU Lesser General Public License as
  * published by the Free Software Foundation; either version 2.1 of
  * the License, or (at your option) any later version.
  *
  * This software is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public
  * License along with this software; if not, write to the Free
  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
  */
package org.jboss.jms.client.state;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.jms.MessageListener;
import javax.jms.Session;

import org.jboss.jms.client.delegate.ClientBrowserDelegate;
import org.jboss.jms.client.delegate.ClientConsumerDelegate;
import org.jboss.jms.client.delegate.ClientProducerDelegate;
import org.jboss.jms.client.delegate.ClientSessionDelegate;
import org.jboss.jms.client.delegate.DelegateSupport;
import org.jboss.jms.client.remoting.MessageCallbackHandler;
import org.jboss.jms.delegate.SessionDelegate;
import org.jboss.jms.destination.JBossDestination;
import org.jboss.jms.server.Version;
import org.jboss.jms.server.endpoint.DeliveryInfo;
import org.jboss.jms.server.endpoint.DeliveryRecovery;
import org.jboss.jms.client.tx.MessagingXAResource;
import org.jboss.jms.client.tx.ResourceManager;
import org.jboss.logging.Logger;
import org.jboss.messaging.util.ClearableQueuedExecutor;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.QueuedExecutor;

/**
 * State corresponding to a session. This state is acessible inside aspects/interceptors.
 * 
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 * @author <a href="mailto:clebert.suconic@jboss.com">Clebert Suconic</a>
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 * @version <tt>$Revision$</tt>
 *
 * $Id$
 */
public class SessionState extends HierarchicalStateSupport
{

   // Constants ------------------------------------------------------------------------------------

   protected static Logger log = Logger.getLogger(SessionState.class);

   // Static ---------------------------------------------------------------------------------------

   // Attributes -----------------------------------------------------------------------------------

   private ConnectionState parent;
   private SessionDelegate delegate;

   private int sessionID;
   private int acknowledgeMode;
   private boolean transacted;
   private boolean xa;

   private MessagingXAResource xaResource;
   private Object currentTxId;

   // Executor used for executing onMessage methods
   private ClearableQueuedExecutor executor;

   private boolean recoverCalled;
   
   // List<DeliveryInfo>
   private List clientAckList;

   private DeliveryInfo autoAckInfo;
   private Map callbackHandlers;
   
   private int dupsOKBatchSize;
   
   private LinkedList asfMessages = new LinkedList();
   
   //The distinguished message listener - for ASF
   private MessageListener sessionListener;
      
   // Constructors ---------------------------------------------------------------------------------

   public SessionState(ConnectionState parent, ClientSessionDelegate delegate,
                       boolean transacted, int ackMode, boolean xa,
                       int dupsOKBatchSize)
   {
      super(parent, (DelegateSupport)delegate);

      this.sessionID = delegate.getID();

      children = new HashSet();
      this.acknowledgeMode = ackMode;
      this.transacted = transacted;
      this.xa = xa;
      
      this.dupsOKBatchSize = dupsOKBatchSize;

      if (xa)
      {
         // Create an XA resource
         xaResource = new MessagingXAResource(parent.getResourceManager(), this);
      }

      // Note we create the transaction even if XA - XA transactions must behave like
      // local tx when not enlisted in a global tx

      if (transacted)
      {
         // Create a local tx
         currentTxId = parent.getResourceManager().createLocalTx();
      }

      executor = new ClearableQueuedExecutor(new LinkedQueue());

      clientAckList = new ArrayList();

      // TODO could optimise this to use the same map of callbackmanagers (which holds refs
      // to callbackhandlers) in the connection, instead of maintaining another map
      callbackHandlers = new HashMap();
   }

   // HierarchicalState implementation -------------------------------------------------------------

   public DelegateSupport getDelegate()
   {
      return (DelegateSupport)delegate;
   }

   public void setDelegate(DelegateSupport delegate)
   {
      this.delegate = (SessionDelegate)delegate;
   }

   public HierarchicalState getParent()
   {
      return parent;
   }

   public void setParent(HierarchicalState parent)
   {
      this.parent = (ConnectionState)parent;
   }

   public Version getVersionToUse()
   {
      return parent.getVersionToUse();
   }
   
   public int getDupsOKBatchSize()
   {
      return dupsOKBatchSize;
   }
   
   public MessageListener getDistinguishedListener()
   {
      return this.sessionListener;
   }
   
   public void setDistinguishedListener(MessageListener listener)
   {
      this.sessionListener = listener;
   }
   
   public LinkedList getASFMessages()
   {
      return asfMessages;
   }

   // HierarchicalStateSupport overrides -----------------------------------------------------------

   public void synchronizeWith(HierarchicalState ns) throws Exception
   {
      SessionState newState = (SessionState)ns;

      int oldSessionID = sessionID;
      sessionID = newState.sessionID;
      
      // We need to clear anything waiting in the session executor - since there may be messages
      // from before failover waiting in there and we don't want them to get delivered after
      // failover.
      executor.clearAllExceptCurrentTask();
      
      ClientSessionDelegate newDelegate = (ClientSessionDelegate)newState.getDelegate();

      for (Iterator i = getChildren().iterator(); i.hasNext(); )
      {
         HierarchicalState child = (HierarchicalState)i.next();

         if (child instanceof ConsumerState)
         {
            ConsumerState consState = (ConsumerState)child;
            ClientConsumerDelegate consDelegate = (ClientConsumerDelegate)consState.getDelegate();

            // create a new consumer over the new session for each consumer on the old session
            ClientConsumerDelegate newConsDelegate = (ClientConsumerDelegate)newDelegate.
               createConsumerDelegate((JBossDestination)consState.getDestination(),
                                      consState.getSelector(),
                                      consState.isNoLocal(),
                                      consState.getSubscriptionName(),
                                      consState.isConnectionConsumer());
            log.debug(this + " created new consumer " + newConsDelegate);

            consDelegate.synchronizeWith(newConsDelegate);
            log.debug(this + " synchronized failover consumer " + consDelegate);
         }
         else if (child instanceof ProducerState)
         {
            ProducerState prodState = (ProducerState)child;
            ClientProducerDelegate prodDelegate = (ClientProducerDelegate)prodState.getDelegate();

            // create a new producer over the new session for each producer on the old session
            ClientProducerDelegate newProdDelegate = (ClientProducerDelegate)newDelegate.
               createProducerDelegate((JBossDestination)prodState.getDestination());
            log.debug(this + " created new producer " + newProdDelegate);

            prodDelegate.synchronizeWith(newProdDelegate);
            log.debug(this + " synchronized failover producer " + prodDelegate);
         }
         else if (child instanceof BrowserState)
         {
            BrowserState browserState = (BrowserState)child;
            ClientBrowserDelegate browserDelegate =
               (ClientBrowserDelegate)browserState.getDelegate();

            // create a new browser over the new session for each browser on the old session
            ClientBrowserDelegate newBrowserDelegate = (ClientBrowserDelegate)newDelegate.
               createBrowserDelegate(browserState.getJmsDestination(),
                                     browserState.getMessageSelector());
            log.debug(this + " created new browser " + newBrowserDelegate);

            browserDelegate.synchronizeWith(newBrowserDelegate);
            log.debug(this + " synchronized failover browser " + browserDelegate);
         }
      }

      ConnectionState connState = (ConnectionState)getParent();
      ResourceManager rm = connState.getResourceManager();

      // We need to failover from one session ID to another in the resource manager
      rm.handleFailover(connState.getServerID(), oldSessionID, newState.sessionID);

      List ackInfos = Collections.EMPTY_LIST;

      if (!isTransacted() || (isXA() && getCurrentTxId() == null))
      {
         // TODO - the check "(isXA() && getCurrentTxId() == null)" shouldn't be necessary any more
         // since xa sessions no longer fall back to non transacted
         
         // Non transacted session or an XA session with no transaction set (it falls back
         // to AUTO_ACKNOWLEDGE)

         log.debug(this + " is not transacted (or XA with no transaction set), " +
            "retrieving deliveries from session state");

         // We remove any unacked non-persistent messages - this is because we don't want to ack
         // them since the server won't know about them and will get confused

         if (getAcknowledgeMode() == Session.CLIENT_ACKNOWLEDGE)
         {
            for(Iterator i = getClientAckList().iterator(); i.hasNext(); )
            {
               DeliveryInfo info = (DeliveryInfo)i.next();
               if (!info.getMessageProxy().getMessage().isReliable())
               {
                  i.remove();
                  log.debug("removed non persistent delivery " + info);
               }
            }

            ackInfos = getClientAckList();
         }
         else
         {
            DeliveryInfo autoAck = getAutoAckInfo();
            if (autoAck != null)
            {
               if (!autoAck.getMessageProxy().getMessage().isReliable())
               {
                  // unreliable, discard
                  setAutoAckInfo(null);
               }
               else
               {
                  // reliable
                  ackInfos = new ArrayList();
                  ackInfos.add(autoAck);
               }
            }
         }

         log.debug(this + " retrieved " + ackInfos.size() + " deliveries");
      }
      else
      {
         // Transacted session - we need to get the acks from the resource manager. BTW we have
         // kept the old resource manager.

         ackInfos = rm.getDeliveriesForSession(getSessionID());
      }

      if (!ackInfos.isEmpty())
      {
         List recoveryInfos = new ArrayList();
         for (Iterator i = ackInfos.iterator(); i.hasNext(); )
         {
            DeliveryInfo del = (DeliveryInfo)i.next();
            DeliveryRecovery recInfo =
               new DeliveryRecovery(del.getMessageProxy().getDeliveryId(),
                                    del.getMessageProxy().getMessage().getMessageID(),
                                    del.getQueueName());

            recoveryInfos.add(recInfo);
         }

         log.debug(this + " sending delivery recovery " + recoveryInfos + " on failover");
         newDelegate.recoverDeliveries(recoveryInfos);
      }
      else
      {
         log.debug(this + " no delivery recovery info to send on failover");
      }           
   }
   
   // Public ---------------------------------------------------------------------------------------

   /**
    * @return List<AckInfo>
    */
   public List getClientAckList()
   {
      return clientAckList;
   }
   
   public void setClientAckList(List list)
   {
      this.clientAckList = list;
   }

   public DeliveryInfo getAutoAckInfo()
   {
      return autoAckInfo;
   }

   public void setAutoAckInfo(DeliveryInfo info)
   {
      if (info != null && autoAckInfo != null)
      {
         throw new IllegalStateException("There is already a delivery set for auto ack");
      }
      autoAckInfo = info;
   }

   public int getAcknowledgeMode()
   {
      return acknowledgeMode;
   }

   public boolean isTransacted()
   {
      return transacted;
   }

   public boolean isXA()
   {
      return xa;
   }

   public MessagingXAResource getXAResource()
   {
      return xaResource;
   }

   public QueuedExecutor getExecutor()
   {
      return executor;
   }

   public Object getCurrentTxId()
   {
      return currentTxId;
   }

   public boolean isRecoverCalled()
   {
      return recoverCalled;
   }

   public void setCurrentTxId(Object id)
   {
      this.currentTxId = id;
   }

   public void setRecoverCalled(boolean recoverCalled)
   {
      this.recoverCalled = recoverCalled;
   }

   public MessageCallbackHandler getCallbackHandler(int consumerID)
   {
      return (MessageCallbackHandler)callbackHandlers.get(new Integer(consumerID));
   }

   public void addCallbackHandler(MessageCallbackHandler handler)
   {
      callbackHandlers.put(new Integer(handler.getConsumerId()), handler);
   }

   public void removeCallbackHandler(MessageCallbackHandler handler)
   {
      callbackHandlers.remove(new Integer(handler.getConsumerId()));
   }

   public int getSessionID()
   {
      return sessionID;
   }
   
   public String toString()
   {
      return "SessionState[" + sessionID + "]";
   }

   // Package protected ----------------------------------------------------------------------------

   // Protected ------------------------------------------------------------------------------------

   // Private --------------------------------------------------------------------------------------

   // Inner classes --------------------------------------------------------------------------------

}

