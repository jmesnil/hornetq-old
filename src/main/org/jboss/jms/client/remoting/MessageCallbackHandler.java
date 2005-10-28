/**
 * JBoss, the OpenSource J2EE WebOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.jms.client.remoting;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;

import org.jboss.jms.delegate.ConsumerDelegate;
import org.jboss.jms.delegate.SessionDelegate;
import org.jboss.jms.message.JBossMessage;
import org.jboss.jms.util.JBossJMSException;
import org.jboss.logging.Logger;
import org.jboss.remoting.Client;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.transport.Connector;

import EDU.oswego.cs.dl.util.concurrent.BoundedBuffer;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;


/**
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 * @version <tt>$Revision$</tt>
 *
 * $Id$
 */
public class MessageCallbackHandler implements InvokerCallbackHandler, Runnable
{
   // Constants -----------------------------------------------------
   
   private static final Logger log = Logger.getLogger(MessageCallbackHandler.class);
   
   // Static --------------------------------------------------------
   
   // Attributes ----------------------------------------------------
   
   protected int capacity = 1;
   protected BoundedBuffer messages;   //Change this - no longer required, for now i just set capacity to 1
   protected SessionDelegate sessionDelegate;
   protected ConsumerDelegate consumerDelegate;
   protected String receiverID;
   
   private volatile boolean receiving;  //why volatile?
   
   private volatile boolean listening;
      
   private Thread receivingThread;
      
   protected MessageListener listener;
   
   protected Thread listenerThread;
   
   protected boolean isConnectionConsumer;
   
   private int listenerThreadCount = 0;
   
   private volatile boolean closed;  // why volatile ?

   private boolean isReceiving()
   {
      return receiving;
   }
   
   private void setReceiving(boolean receiving)
   {
      this.receiving = receiving;
   }
   
   private boolean isListening()
   {
      return listening;
   }
   
   private void setListening(boolean listening)
   {
      this.listening = listening;
   }
   
   private boolean isClosed()
   {
      return closed;
   }
   
   private void setClosed(boolean closed)
   {
      this.closed = closed;
   }
      
   private void clearBuffer()
   {
      //temporary hack
      messages = new BoundedBuffer(1);
   }
   
   // Constructors --------------------------------------------------

   public MessageCallbackHandler(boolean isCC)
   {
      this.messages = new BoundedBuffer(capacity);
      this.isConnectionConsumer = isCC;
   }

   // InvokerCallbackHandler implementation -------------------------
   
   public void handleCallback(Callback callback) throws HandleCallbackException
   {
      if (log.isTraceEnabled()) { log.trace("receiving message " + callback.getParameter() + " from the remoting layer"); }

      Message m = (Message)callback.getParameter();
      
            
      try
      {
         if (!isListening() && !isReceiving())
         {
            //There is a small chance, but possible that a receive times out or a message listener
            //is unset before then later it receives a message
            //this message cannot be handled so it needs to be sent back
            //to the server to be cancelled
            consumerDelegate.cancelMessage(m.getJMSMessageID());
         }
         
         if (isClosed())
         {
            consumerDelegate.cancelMessage(m.getJMSMessageID());
            log.warn("Tried to deliver message but consumer is closed!!");
         }
      }
      catch (JMSException e)
      {
         log.error("Failed to cancel message", e);
      }
  
      try
      {			
         JBossMessage jm = (JBossMessage)m;	
         //if this is the handler for a connection consumer we don't want to set the session delegate
         //since this is only used for client acknowledgement which is illegal for a session
         //used for an MDB
         if (!this.isConnectionConsumer)
         {
            jm.setSessionDelegate(sessionDelegate);
         }
         messages.put(m);
         if (log.isTraceEnabled()) { log.trace("message " + m + " queued in the client delivery queue"); }
      }
      catch(InterruptedException e)
      {
         String msg = "Interrupted attempt to put message in the delivery buffer";
         log.warn(msg);
         throw new HandleCallbackException(msg, e);
      }
   }
   
   // Runnable implementation ---------------------------------------
   
   /**
    * Receive messages for the message listener.
    */
   public void run()
   {
      if (log.isTraceEnabled()) { log.trace("listener thread started"); }
      
      if (log.isTraceEnabled()) { log.trace("calling setListening"); }
      
      setListening(true);
      
      if (log.isTraceEnabled()) { log.trace("set listening"); }
      
      try
      {      
         while(true)
         {            
            //Tell the server we are ready to receive a message
            
            if (log.isTraceEnabled()) { log.trace("clearing buffer"); }
            clearBuffer();
            if (log.isTraceEnabled()) { log.trace("calling readyForMessage"); }
            consumerDelegate.getMessage();
            if (log.isTraceEnabled()) { log.trace("called readyForMessage"); }
                        
            if (log.isTraceEnabled()) { log.trace("blocking to take a message"); }
            JBossMessage m = (JBossMessage)messages.take();
            preDeliver(m);   
            listener.onMessage(m);
            if (log.isTraceEnabled()) { log.trace("message successfully handled by listener"); }
            postDeliver(m);         
         }     
      }
      catch(InterruptedException e)
      {
         log.debug("message listener thread interrupted, exiting");         
      }
      catch(Throwable t)
      {
         // TODO - different actions for client error/commuication error
         // TODO - try several times and then give up?
         log.error(t);
      }
      finally
      {      
         //Tell the server we don't want any more messages
         consumerDelegate.stopDelivering();
         setListening(false);
      }
   }
   
   // Public --------------------------------------------------------
   
   
   public void setSessionDelegate(SessionDelegate delegate)
   {
      this.sessionDelegate = delegate;
   }
   
   public void setConsumerDelegate(ConsumerDelegate delegate)
   {
      this.consumerDelegate = delegate;
   }
   
   public void setReceiverID(String receiverID)
   {
      this.receiverID = receiverID;
   }
   
   public synchronized MessageListener getMessageListener()
   {
      return listener;
   }
   
   public synchronized void setMessageListener(MessageListener listener) throws JMSException
   {
      if (receiving)
      {
         throw new JBossJMSException("Another thread is already receiving");
      }            
      
      if (listenerThread != null)
      {
         listenerThread.interrupt();
         try
         {
            listenerThread.join();
         }
         catch (InterruptedException e)
         {
            log.error("Thread interrupted", e);
         }
      }
      
      this.listener = listener;
      if (listener != null)
      {
         listenerThread = new Thread(this, "MessageListenerThread-" + listenerThreadCount++);
         listenerThread.start();
      }
   }

   /**
    * TODO Get rid of this (http://jira.jboss.org/jira/browse/JBMESSAGING-92)
    */
   private Connector callbackServer;
   // I keep the client reference since I need to use the same client to remove a listener (sessionID)
   private Client client;
   public void setCallbackServer(Connector callbackServer, Client client)
   {
      this.callbackServer = callbackServer;
      this.client = client;
   }


   /**
    * Method used by the client thread to get a Message, if available.
    *
    * @param timeout - the timeout value in milliseconds. A zero timeount never expires, and the
    *        call blocks indefinitely. A -1 timeout means receiveNoWait(): return the next message
    *        or null if one is not immediately available. Returns null if the consumer is
    *        concurrently closed.
    */
   public Message receive(long timeout) throws JMSException, InterruptedException
   {
      
      long startTimestamp = System.currentTimeMillis();
      
      // make sure the current state allows receiving
      
      synchronized(this)
      {
         if (isListening())
         {
            throw new JBossJMSException("A message listener is already registered");
         }
         if (isReceiving())
         {
            throw new JBossJMSException("Another thread is already receiving");
         }
         setReceiving(true);
      }
      
      try
      {
         receivingThread = Thread.currentThread();
         
         //Tell the server we're ready to accept a message
         clearBuffer();
         consumerDelegate.getMessage();
         
         JBossMessage m = null;
         while(true)
         {
            try
            {
               if (timeout == 0)
               {
                  if (log.isTraceEnabled()) log.trace("receive with no timeout");
                    
                  m = ((JBossMessage)messages.take());

                  if (log.isTraceEnabled()) { log.trace("Got message: " + m); }
               }
               else if (timeout == -1)
               {
                  //ReceiveNoWait
                  if (log.isTraceEnabled()) { log.trace("receive noWait"); }
                  
                  /*
                   * FIXME
                   * 
                   * We have a problem with receive no wait
                   * Even if there is a message sitting waiting in the server queue
                   * then if we don't wait at all here, there is probably not
                   * enough time for the message to get here so the client can receive it
                   * So temporarily I have hacked it to wait for a bit, even though it
                   * is no wait. !!!!!!!!!!!!
                   * 
                   * What we really need to do is introduce a synchronous
                   * "giveMeNextMessage" method on the channel, that gives the first
                   * available message that is sitting in the queue, or null if there
                   * is none
                   * Then we would call that method here.
                   * 
                   * 
                   * 
                   */

                  m = ((JBossMessage)messages.poll(1000));
                 
                  if (m == null)
                  {
                     if (log.isTraceEnabled()) { log.trace("No message available"); }
                     return null;
                  }
               }
               else
               {
                  if (log.isTraceEnabled()) { log.trace("receive timeout " + timeout + " ms"); }

                  m = ((JBossMessage)messages.poll(timeout));
                                    
                  if (m == null)
                  {
                     // timeout expired
                     if (log.isTraceEnabled()) { log.trace("Timeout expired"); }
                     return null;
                  }
               }
            }
            catch(InterruptedException e)
            {
               if (log.isTraceEnabled()) { log.trace("Thread was interrupted"); }
               if (isClosed())
               {
                  if (log.isTraceEnabled()) { log.trace("It's closed"); }
                  return null;
               }
               else
               {
                  throw e;
               }
            }
            
            if (log.isTraceEnabled()) { log.trace("Calling delivered()"); }
            
            // notify that the message has been delivered (not necessarily acknowledged though)

            receivingThread = null; // Crucial - in case thread is interrupted during pre or postDeliver
            preDeliver(m);
            postDeliver(m);
            
            if (!m.isExpired())
            {
               if (log.isTraceEnabled()) { log.trace("Message is not expired, returning it to the caller"); }
               return m;
            }

            log.debug("Message expired");

            // discard the message, adjust timeout and reenter the buffer
            if (timeout != 0)
            {
               timeout -= System.currentTimeMillis() - startTimestamp;
            }
         }
      }
      finally
      {
         //Tell the server we don't want the next message
         try
         {
            consumerDelegate.stopDelivering();
         }
         catch (Exception e )
         {
            //Might be closed - ignore
         }
         setReceiving(false);
         receivingThread = null;
      }
   }

   /**
    * Evict messages sitting in the buffer waiting to be picked up.
    *
    * @return List<Serializable> containing the ids of the messages evicted from the buffer.
    */
//   public List reset()
//   {
//      List ids = new ArrayList();
//      try
//      {
//         JBossMessage m = null;
//         while((m = (JBossMessage)messages.poll(0)) != null)
//         {
//            ids.add(m.getMessageID());
//         }
//      }
//      catch(Exception e)
//      {
//         log.warn(e);
//      }
//
//      if (log.isTraceEnabled()) { log.trace("evicted " + ids.size() + " cached messages"); }
//
//      return ids;
//   }

   /**
    * @return List<Serializable> containing the ids of the messages evicted from the buffer.
    */
   public void close()
   {
//      if (closed)
//      {
//         return Collections.EMPTY_LIST;
//      }

      setClosed(true);

      log.debug("closing " + this);

      if (receivingThread != null)
      {
         if (log.isTraceEnabled()) { log.trace("interrupting receiving thread"); }
         receivingThread.interrupt();
      }

      // evict messages sitting in the buffer, they need to be "undelivered"
      //List evicted = reset();
      messages = null;

      // TODO Get rid of this (http://jira.jboss.org/jira/browse/JBMESSAGING-92)
      try
      {
         // unregister this callback handler and stop the callback server

         client.removeListener(this);
         if (log.isTraceEnabled()) { log.trace("removed listener from callback server"); }

         callbackServer.stop();
         if (log.isTraceEnabled()) { log.trace("closed callback server " + callbackServer.getInvokerLocator()); }
      }
      catch(Throwable e)
      {
         log.warn("Failed to clean up callback handler/callback server", e);
      }

      //return evicted;
   }

   public String toString()
   {
      return "MessageCallbackHandler[" + receiverID + "]";
   }

   // Package protected ---------------------------------------------
   
   // Protected -----------------------------------------------------
   
   // Private -------------------------------------------------------
   
   public void preDeliver(Message m) throws JMSException
   {
      //If this is the callback-handler for a connection consumer we don't want
      //to acknowledge or add anything to the tx for this session
      if (!this.isConnectionConsumer)
      {
         sessionDelegate.preDeliver(m.getJMSMessageID(), receiverID);
      }
   }
   
   public void postDeliver(Message m) throws JMSException
   {
      //If this is the callback-handler for a connection consumer we don't want
      //to acknowledge or add anything to the tx for this session
      if (!this.isConnectionConsumer)
      {
         sessionDelegate.postDeliver(m.getJMSMessageID(), receiverID);
      }
   }
   
   // Inner classes -------------------------------------------------
}




