/**
 * JBoss, the OpenSource J2EE WebOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.jms.client;

import java.io.Serializable;

import javax.jms.Connection;
import javax.jms.ConnectionConsumer;
import javax.jms.ConnectionMetaData;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueSession;
import javax.jms.ServerSessionPool;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicSession;
import javax.jms.XAConnection;
import javax.jms.XAQueueConnection;
import javax.jms.XAQueueSession;
import javax.jms.XASession;
import javax.jms.XATopicConnection;
import javax.jms.XATopicSession;

import org.jboss.jms.delegate.ConnectionDelegate;
import org.jboss.jms.delegate.SessionDelegate;

/**
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 * @author <a href="mailto:tim.l.fox@gmail.com">Tim Fox</a>
 * @version <tt>$Revision$</tt>
 *
 * $Id$
 */
public class JBossConnection implements
    Connection, QueueConnection, TopicConnection,
    XAConnection, XAQueueConnection, XATopicConnection, Serializable
{
   
   // Constants -----------------------------------------------------
   private static final long serialVersionUID = -3715868654823177898L;
   
   static final int TYPE_GENERIC_CONNECTION = 0;
   static final int TYPE_QUEUE_CONNECTION = 1;
   static final int TYPE_TOPIC_CONNECTION = 2;
   
   // Static --------------------------------------------------------

   // Attributes ----------------------------------------------------

   protected ConnectionDelegate delegate;
   private int connectionType;   

   // Constructors --------------------------------------------------

   public JBossConnection(ConnectionDelegate delegate, int connectionType)
         throws JMSException
   {
      this.delegate = delegate;
      this.connectionType = connectionType;      
   }

   // Connection implementation -------------------------------------

   public Session createSession(boolean transacted, int acknowledgeMode) throws JMSException
   {
      return createSessionInternal(transacted, acknowledgeMode, false, TYPE_GENERIC_CONNECTION);
   }

   public String getClientID() throws JMSException
   {
      return delegate.getClientID();
   }

   public void setClientID(String clientID) throws JMSException
   {
      delegate.setClientID(clientID);
   }

   public ConnectionMetaData getMetaData() throws JMSException
   {
      return delegate.getConnectionMetaData();
   }

   public ExceptionListener getExceptionListener() throws JMSException
   {
      return delegate.getExceptionListener();
   }

   public void setExceptionListener(ExceptionListener listener) throws JMSException
   {
      delegate.setExceptionListener(listener);
   }

   public void start() throws JMSException
   {
      delegate.start();
   }

   public void stop() throws JMSException
   {
      delegate.stop();
   }

   public void close() throws JMSException
   {
      delegate.closing();
      delegate.close();
   }

   public ConnectionConsumer createConnectionConsumer(
         Destination destination,
         String messageSelector,
         ServerSessionPool sessionPool,
         int maxMessages)
         throws JMSException
   {
      return delegate.createConnectionConsumer(destination, null, messageSelector, sessionPool, maxMessages);
   }

   public ConnectionConsumer createDurableConnectionConsumer(
         Topic topic,
         String subscriptionName,
         String messageSelector,
         ServerSessionPool sessionPool,
         int maxMessages)
         throws JMSException
   {      
      //As spec. section 4.11
      if (connectionType == TYPE_QUEUE_CONNECTION)
      {
         String msg = "Cannot create a durable connection consumer on a QueueConnection";
         throw new IllegalStateException(msg);
      }
      return delegate.createConnectionConsumer(topic, subscriptionName, messageSelector, sessionPool, maxMessages);
   }
   
   // QueueConnection implementation ---------------------------------

   public QueueSession createQueueSession(boolean transacted,
                                          int acknowledgeMode) throws JMSException
   {    
       return createSessionInternal(transacted, acknowledgeMode, false,
                                    JBossSession.TYPE_QUEUE_SESSION);
   }
   
   public ConnectionConsumer createConnectionConsumer(Queue queue, String messageSelector,
                                                      ServerSessionPool sessionPool,
                                                      int maxMessages) throws JMSException
    {
      return delegate.createConnectionConsumer(queue, null, messageSelector, sessionPool, maxMessages);
    }
   
   // TopicConnection implementation ---------------------------------

   public TopicSession createTopicSession(boolean transacted,
                                          int acknowledgeMode) throws JMSException
   {
      return createSessionInternal(transacted, acknowledgeMode, false,
                                   JBossSession.TYPE_TOPIC_SESSION);
   }
   
   public ConnectionConsumer createConnectionConsumer(Topic topic, String messageSelector,
                                                      ServerSessionPool sessionPool,
                                                      int maxMessages) throws JMSException
   {
      return delegate.createConnectionConsumer(topic, null, messageSelector, sessionPool, maxMessages);
   }
   
   // XAConnection implementation -------------------------------------

   public XASession createXASession() throws JMSException
   {      
       return createSessionInternal(true, Session.SESSION_TRANSACTED, true,
                                    JBossSession.TYPE_GENERIC_SESSION);
   }
   
   // XAQueueConnection implementation ---------------------------------

   public XAQueueSession createXAQueueSession() throws JMSException
   {       
      return createSessionInternal(true, Session.SESSION_TRANSACTED, true,
                                   JBossSession.TYPE_QUEUE_SESSION);

   }
   
   // XATopicConnection implementation ---------------------------------

   public XATopicSession createXATopicSession() throws JMSException
   {      
      return createSessionInternal(true, Session.SESSION_TRANSACTED, true,
                                   JBossSession.TYPE_TOPIC_SESSION);

   }

   // Public --------------------------------------------------------
   
   public Serializable getConnectionID()
   {
      return delegate.getConnectionID();
   }

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------
   
   protected JBossSession createSessionInternal(boolean transacted, int acknowledgeMode,
                                                boolean isXA, int type) throws JMSException
   {
      if (transacted)
      {
         acknowledgeMode = Session.SESSION_TRANSACTED;
      }
      SessionDelegate sessionDelegate = delegate.createSessionDelegate(transacted, acknowledgeMode, isXA);
      return new JBossSession(sessionDelegate, type);
   }

   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------
}
