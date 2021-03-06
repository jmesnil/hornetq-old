/*
 * Copyright 2009 Red Hat, Inc.
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.hornetq.core.server.impl;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.hornetq.api.core.HornetQBuffer;
import org.hornetq.api.core.HornetQBuffers;
import org.hornetq.api.core.management.ManagementHelper;
import org.hornetq.api.core.management.NotificationType;
import org.hornetq.core.client.impl.ClientConsumerImpl;
import org.hornetq.core.filter.Filter;
import org.hornetq.core.logging.Logger;
import org.hornetq.core.message.BodyEncoder;
import org.hornetq.core.persistence.StorageManager;
import org.hornetq.core.postoffice.Binding;
import org.hornetq.core.postoffice.QueueBinding;
import org.hornetq.core.server.HandleStatus;
import org.hornetq.core.server.LargeServerMessage;
import org.hornetq.core.server.MessageReference;
import org.hornetq.core.server.Queue;
import org.hornetq.core.server.ServerConsumer;
import org.hornetq.core.server.ServerMessage;
import org.hornetq.core.server.ServerSession;
import org.hornetq.core.server.management.ManagementService;
import org.hornetq.core.server.management.Notification;
import org.hornetq.core.transaction.Transaction;
import org.hornetq.core.transaction.impl.TransactionImpl;
import org.hornetq.spi.core.protocol.SessionCallback;
import org.hornetq.spi.core.remoting.ReadyListener;
import org.hornetq.utils.Future;
import org.hornetq.utils.LinkedListIterator;
import org.hornetq.utils.TypedProperties;

/**
 * Concrete implementation of a ClientConsumer.
 *
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 * @author <a href="mailto:jmesnil@redhat.com">Jeff Mesnil</a>
 * @author <a href="mailto:clebert.suconic@jboss.com">Clebert Suconic</a>
 * 
 * @version <tt>$Revision: 3783 $</tt> $Id: ServerConsumerImpl.java 3783 2008-02-25 12:15:14Z timfox $
 */
public class ServerConsumerImpl implements ServerConsumer, ReadyListener
{
   // Constants ------------------------------------------------------------------------------------

   private static final Logger log = Logger.getLogger(ServerConsumerImpl.class);

   // Static ---------------------------------------------------------------------------------------

   private static final boolean trace = ServerConsumerImpl.log.isTraceEnabled();

   private static void trace(final String message)
   {
      ServerConsumerImpl.log.trace(message);
   }

   // Attributes -----------------------------------------------------------------------------------

   private final long id;

   private final Queue messageQueue;

   private final Filter filter;

   private final int minLargeMessageSize;

   private final ServerSession session;

   private final Executor executor;

   private final Object lock = new Object();

   private volatile AtomicInteger availableCredits = new AtomicInteger(0);

   private boolean started;

   private volatile LargeMessageDeliverer largeMessageDeliverer = null;

   private boolean largeMessageInDelivery;

   /**
    * if we are a browse only consumer we don't need to worry about acknowledgemenets or being started/stopeed by the session.
    */
   private final boolean browseOnly;

   private BrowserDeliverer browserDeliverer;

   private final boolean strictUpdateDeliveryCount;

   private final StorageManager storageManager;

   private final java.util.Queue<MessageReference> deliveringRefs = new ConcurrentLinkedQueue<MessageReference>();

   private final SessionCallback callback;

   private volatile boolean closed;

   private final boolean preAcknowledge;

   private final ManagementService managementService;

   private final Binding binding;

   private boolean transferring = false;
   
   /* As well as consumer credit based flow control, we also tap into TCP flow control (assuming transport is using TCP)
    * This is useful in the case where consumer-window-size = -1, but we don't want to OOM by sending messages ad infinitum to the Netty
    * write queue when the TCP buffer is full, e.g. the client is slow or has died.    
    */
   private AtomicBoolean writeReady = new AtomicBoolean(true);

   private final long creationTime;

   // Constructors ---------------------------------------------------------------------------------

   public ServerConsumerImpl(final long id,
                             final ServerSession session,
                             final QueueBinding binding,
                             final Filter filter,
                             final boolean started,
                             final boolean browseOnly,
                             final StorageManager storageManager,
                             final SessionCallback callback,
                             final boolean preAcknowledge,
                             final boolean strictUpdateDeliveryCount,
                             final ManagementService managementService) throws Exception
   {
      this.id = id;

      this.filter = filter;

      this.session = session;

      this.binding = binding;

      messageQueue = binding.getQueue();

      this.executor = messageQueue.getExecutor();

      this.started = browseOnly || started;

      this.browseOnly = browseOnly;

      this.storageManager = storageManager;

      this.callback = callback;

      this.preAcknowledge = preAcknowledge;

      this.managementService = managementService;

      minLargeMessageSize = session.getMinLargeMessageSize();

      this.strictUpdateDeliveryCount = strictUpdateDeliveryCount;
      
      this.callback.addReadyListener(this);

      this.creationTime = System.currentTimeMillis();
      
      if (browseOnly)
      {
         browserDeliverer = new BrowserDeliverer(messageQueue.iterator());
      }
      else
      {
         messageQueue.addConsumer(this);
      }
   }

   // ServerConsumer implementation
   // ----------------------------------------------------------------------

   public long getID()
   {
      return id;
   }
   
   public boolean isBrowseOnly()
   {
      return browseOnly;
   }

   public long getCreationTime()
   {
      return creationTime;
   }
   
   public String getConnectionID()
   {
      return this.session.getConnectionID().toString();
   }
   
   public String getSessionID()
   {
      return this.session.getName();
   }

   public HandleStatus handle(final MessageReference ref) throws Exception
   {
      if (availableCredits != null && availableCredits.get() <= 0)
      {
         return HandleStatus.BUSY;
      }
      
// TODO - https://jira.jboss.org/browse/HORNETQ-533      
//      if (!writeReady.get())
//      {
//         return HandleStatus.BUSY;
//      }
      
      synchronized (lock)
      {
         // If the consumer is stopped then we don't accept the message, it
         // should go back into the
         // queue for delivery later.
         if (!started || transferring)
         {
            return HandleStatus.BUSY;
         }

         // If there is a pendingLargeMessage we can't take another message
         // This has to be checked inside the lock as the set to null is done inside the lock
         if (largeMessageInDelivery)
         {
            return HandleStatus.BUSY;
         }

         final ServerMessage message = ref.getMessage();

         if (filter != null && !filter.match(message))
         {
            return HandleStatus.NO_MATCH;
         }

         if (!browseOnly)
         {
            if (!preAcknowledge)
            {
               deliveringRefs.add(ref);
            }

            ref.handled();

            ref.incrementDeliveryCount();

            // If updateDeliveries = false (set by strict-update),
            // the updateDeliveryCount would still be updated after c
            if (strictUpdateDeliveryCount)
            {
               if (ref.getMessage().isDurable() && ref.getQueue().isDurable() && !ref.getQueue().isInternalQueue())
               {
                  storageManager.updateDeliveryCount(ref);
               }
            }

            if (preAcknowledge)
            {
               if (message.isLargeMessage())
               {
                  // we must hold one reference, or the file will be deleted before it could be delivered
                  ((LargeServerMessage)message).incrementDelayDeletionCount();
               }

               // With pre-ack, we ack *before* sending to the client
               ref.getQueue().acknowledge(ref);
            }

         }

         if (message.isLargeMessage())
         {
            deliverLargeMessage(ref, message);
         }
         else
         {
            deliverStandardMessage(ref, message);
         }

         return HandleStatus.HANDLED;
      }
   }

   public Filter getFilter()
   {
      return filter;
   }

   public void close(final boolean failed) throws Exception
   {
      callback.removeReadyListener(this);
      
      setStarted(false);

      if (largeMessageDeliverer != null)
      {
         largeMessageDeliverer.finish();
      }

      if (browseOnly)
      {
         browserDeliverer.close();
      }
      else
      {
         messageQueue.removeConsumer(this);
      }

      session.removeConsumer(id);

      LinkedList<MessageReference> refs = cancelRefs(failed, false, null);

      Iterator<MessageReference> iter = refs.iterator();

      closed = true;

      Transaction tx = new TransactionImpl(storageManager);

      while (iter.hasNext())
      {
         MessageReference ref = iter.next();

         ref.getQueue().cancel(tx, ref);
      }

      tx.rollback();

      if (!browseOnly)
      {
         TypedProperties props = new TypedProperties();

         props.putSimpleStringProperty(ManagementHelper.HDR_ADDRESS, binding.getAddress());

         props.putSimpleStringProperty(ManagementHelper.HDR_CLUSTER_NAME, binding.getClusterName());

         props.putSimpleStringProperty(ManagementHelper.HDR_ROUTING_NAME, binding.getRoutingName());

         props.putSimpleStringProperty(ManagementHelper.HDR_FILTERSTRING, filter == null ? null
                                                                                        : filter.getFilterString());

         props.putIntProperty(ManagementHelper.HDR_DISTANCE, binding.getDistance());

         props.putIntProperty(ManagementHelper.HDR_CONSUMER_COUNT, messageQueue.getConsumerCount());

         Notification notification = new Notification(null, NotificationType.CONSUMER_CLOSED, props);

         managementService.sendNotification(notification);
      }
   }

   /**
    * Prompt delivery and send a "forced delivery" message to the consumer.
    * When the consumer receives such a "forced delivery" message, it discards it
    * and knows that there are no other messages to be delivered.
    */
   public synchronized void forceDelivery(final long sequence)
   {
      promptDelivery();

      Future future = new Future();

      executor.execute(future);

      boolean ok = future.await(10000);

      if (!ok)
      {
         log.warn("Timed out waiting for executor");
      }

      try
      {
         // We execute this on the same executor to make sure the force delivery message is written after
         // any delivery is completed

         ServerMessage forcedDeliveryMessage = new ServerMessageImpl(storageManager.generateUniqueID(), 50);

         forcedDeliveryMessage.putLongProperty(ClientConsumerImpl.FORCED_DELIVERY_MESSAGE, sequence);
         forcedDeliveryMessage.setAddress(messageQueue.getName());

         callback.sendMessage(forcedDeliveryMessage, id, 0);
      }
      catch (Exception e)
      {
         ServerConsumerImpl.log.error("Failed to send forced delivery message", e);
      }
   }

   public LinkedList<MessageReference> cancelRefs(final boolean failed, final boolean lastConsumedAsDelivered, final Transaction tx) throws Exception
   {
      boolean performACK = lastConsumedAsDelivered;

      LinkedList<MessageReference> refs = new LinkedList<MessageReference>();

      if (!deliveringRefs.isEmpty())
      {
         for (MessageReference ref : deliveringRefs)
         {
            if (performACK)
            {
               acknowledge(false, tx, ref.getMessage().getMessageID());

               performACK = false;
            }
            else
            {
               if (!failed)
               {
                  //We don't decrement delivery count if the client failed, since there's a possibility that refs were actually delivered but we just didn't get any acks for them
                  //before failure
                  ref.decrementDeliveryCount();
               }

               refs.add(ref);
            }
         }

         deliveringRefs.clear();
      }

      return refs;
   }

   public void setStarted(final boolean started)
   {
      synchronized (lock)
      {
         this.started = browseOnly || started;
      }

      // Outside the lock
      if (started)
      {
         promptDelivery();
      }
   }

   public void setTransferring(final boolean transferring)
   {
      synchronized (lock)
      {
         this.transferring = transferring;

         if (transferring)
         {
            // Now we must wait for any large message delivery to finish
            while (largeMessageInDelivery)
            {
               try
               {
                  Thread.sleep(1);
               }
               catch (InterruptedException ignore)
               {
               }
            }
         }
      }

      // Outside the lock
      if (transferring)
      {
         // And we must wait for any force delivery to be executed - this is executed async so we add a future to the
         // executor and
         // wait for it to complete

         Future future = new Future();

         executor.execute(future);

         boolean ok = future.await(10000);

         if (!ok)
         {
            log.warn("Timed out waiting for executor to complete");
         }
      }

      if (!transferring)
      {
         promptDelivery();
      }
   }

   public void receiveCredits(final int credits) throws Exception
   {      
      if (credits == -1)
      {
         // No flow control
         availableCredits = null;
         
         //There may be messages already in the queue
         promptDelivery();
      }
      else if (credits == 0)
      {
         //reset, used on slow consumers
         availableCredits.set(0);
      }
      else
      {
         int previous = availableCredits.getAndAdd(credits);

         if (ServerConsumerImpl.trace)
         {
            ServerConsumerImpl.trace("Received " + credits +
                                     " credits, previous value = " +
                                     previous +
                                     " currentValue = " +
                                     availableCredits.get());
         }

         if (previous <= 0 && previous + credits > 0)
         {
            promptDelivery();
         }
      }
   }

   public Queue getQueue()
   {
      return messageQueue;
   }

   public void acknowledge(final boolean autoCommitAcks, final Transaction tx, final long messageID) throws Exception
   {
      if (browseOnly)
      {
         return;
      }
      
      // Acknowledge acknowledges all refs delivered by the consumer up to and including the one explicitly
      // acknowledged

      MessageReference ref;
      do
      {
         ref = deliveringRefs.poll();

         if (ref == null)
         {
            throw new IllegalStateException(System.identityHashCode(this) + " Could not find reference on consumerID=" +
                                            id +
                                            ", messageId = " +
                                            messageID +
                                            " queue = " +
                                            messageQueue.getName() +
                                            " closed = " +
                                            closed);
         }

         if (autoCommitAcks || tx == null)
         {
            ref.getQueue().acknowledge(ref);
         }
         else
         {
            ref.getQueue().acknowledge(tx, ref);
         }
      }
      while (ref.getMessage().getMessageID() != messageID);
   }
   
   public void individualAcknowledge(final boolean autoCommitAcks, final Transaction tx, final long messageID) throws Exception
   {
      if (browseOnly)
      {
         return;
      }
      
      MessageReference ref = removeReferenceByID(messageID);
      
      if (ref == null)
      {
         throw new IllegalStateException("Cannot find ref to ack " + messageID);
      }
      
      if (autoCommitAcks)
      {
         ref.getQueue().acknowledge(ref);
      }
      else
      {
         ref.getQueue().acknowledge(tx, ref);
      }
   }

   public MessageReference removeReferenceByID(final long messageID) throws Exception
   {
      if (browseOnly)
      {
         return null;
      }

      // Expiries can come in out of sequence with respect to delivery order

      Iterator<MessageReference> iter = deliveringRefs.iterator();

      MessageReference ref = null;

      while (iter.hasNext())
      {
         MessageReference theRef = iter.next();

         if (theRef.getMessage().getMessageID() == messageID)
         {
            iter.remove();

            ref = theRef;

            break;
         }
      }

      return ref;
   }
      
   public void readyForWriting(final boolean ready)
   {
      if (ready)
      {
         writeReady.set(true);
         
         promptDelivery();
      }
      else
      {
         writeReady.set(false);
      }
   }

   /** To be used on tests only */
   public AtomicInteger getAvailableCredits()
   {
      return availableCredits;
   }

   // Private --------------------------------------------------------------------------------------

   private void promptDelivery()
   {
      synchronized (lock)
      {
         // largeMessageDeliverer is aways set inside a lock
         // if we don't acquire a lock, we will have NPE eventually
         if (largeMessageDeliverer != null)
         {
            resumeLargeMessage();
         }
         else
         {
            if (browseOnly)
            {
               executor.execute(browserDeliverer);
            }
            else
            {
               messageQueue.deliverAsync();
            }
         }
      }
   }

   private void resumeLargeMessage()
   {
      executor.execute(resumeLargeMessageRunnable);
   }

   private void deliverLargeMessage(final MessageReference ref, final ServerMessage message) throws Exception
   {
      largeMessageInDelivery = true;

      final LargeMessageDeliverer localDeliverer = new LargeMessageDeliverer((LargeServerMessage)message, ref);

      // it doesn't need lock because deliverLargeMesasge is already inside the lock()
      largeMessageDeliverer = localDeliverer;
      largeMessageDeliverer.deliver();
   }

   /**
    * @param ref
    * @param message
    */
   private void deliverStandardMessage(final MessageReference ref, final ServerMessage message)
   {
      int packetSize = callback.sendMessage(message, id, ref.getDeliveryCount());

      if (availableCredits != null)
      {
         availableCredits.addAndGet(-packetSize);
      }
   }

   // Inner classes
   // ------------------------------------------------------------------------

   private final Runnable resumeLargeMessageRunnable = new Runnable()
   {
      public void run()
      {
         synchronized (lock)
         {
            try
            {
               if (largeMessageDeliverer == null || largeMessageDeliverer.deliver())
               {
                  if (browseOnly)
                  {
                     executor.execute(browserDeliverer);
                  }
                  else
                  {
                     // prompt Delivery only if chunk was finished

                     messageQueue.deliverAsync();
                  }
               }
            }
            catch (Exception e)
            {
               ServerConsumerImpl.log.error("Failed to run large message deliverer", e);
            }
         }
      }
   };

   /** Internal encapsulation of the logic on sending LargeMessages.
    *  This Inner class was created to avoid a bunch of loose properties about the current LargeMessage being sent*/
   private final class LargeMessageDeliverer
   {
      private long sizePendingLargeMessage;

      private LargeServerMessage largeMessage;

      private final MessageReference ref;

      private boolean sentInitialPacket = false;

      /** The current position on the message being processed */
      private long positionPendingLargeMessage;

      private BodyEncoder context;

      public LargeMessageDeliverer(final LargeServerMessage message, final MessageReference ref) throws Exception
      {
         largeMessage = message;

         largeMessage.incrementDelayDeletionCount();

         this.ref = ref;
      }

      public boolean deliver() throws Exception
      {
         synchronized (lock)
         {
            if (largeMessage == null)
            {
               return true;
            }

            if (availableCredits != null && availableCredits.get() <= 0)
            {
               return false;
            }

            if (!sentInitialPacket)
            {
               context = largeMessage.getBodyEncoder();

               sizePendingLargeMessage = context.getLargeBodySize();
               
               context.open();

               sentInitialPacket = true;

               int packetSize = callback.sendLargeMessage(largeMessage,
                                                          id,
                                                          context.getLargeBodySize(),
                                                          ref.getDeliveryCount());

               if (availableCredits != null)
               {
                  availableCredits.addAndGet(-packetSize);
               }

               // Execute the rest of the large message on a different thread so as not to tie up the delivery thread
               // for too long

               resumeLargeMessage();

               return false;
            }
            else
            {
               if (availableCredits != null && availableCredits.get() <= 0)
               {
                  if (ServerConsumerImpl.trace)
                  {
                     ServerConsumerImpl.trace("deliverLargeMessage: Leaving loop of send LargeMessage because of credits");
                  }

                  return false;
               }

               int localChunkLen = 0;

               localChunkLen = (int)Math.min(sizePendingLargeMessage - positionPendingLargeMessage, minLargeMessageSize);

               HornetQBuffer bodyBuffer = HornetQBuffers.fixedBuffer(localChunkLen);

               context.encode(bodyBuffer, localChunkLen);

               byte[] body = bodyBuffer.toByteBuffer().array();

               int packetSize = callback.sendLargeMessageContinuation(id,
                                                                      body,
                                                                      positionPendingLargeMessage + localChunkLen < sizePendingLargeMessage,
                                                                      false);

               int chunkLen = body.length;

               if (ServerConsumerImpl.trace)
               {
                  ServerConsumerImpl.trace("deliverLargeMessage: Sending " + packetSize +
                                           " availableCredits now is " +
                                           availableCredits);
               }

               if (availableCredits != null)
               {
                  availableCredits.addAndGet(-packetSize);
               }

               positionPendingLargeMessage += chunkLen;

               if (positionPendingLargeMessage < sizePendingLargeMessage)
               {
                  resumeLargeMessage();

                  return false;
               }
            }

            if (ServerConsumerImpl.trace)
            {
               ServerConsumerImpl.trace("Finished deliverLargeMessage");
            }

            finish();

            return true;
         }
      }

      public void finish() throws Exception
      {
         synchronized (lock)
         {
            if (largeMessage == null)
            {
               // handleClose could be calling close while handleDeliver is also calling finish.
               // As a result one of them could get here after the largeMessage is already gone.
               // On that case we just ignore this call
               return;
            }
            context.close();

            largeMessage.releaseResources();

            largeMessage.decrementDelayDeletionCount();

            if (preAcknowledge && !browseOnly)
            {
               // PreAck will have an extra reference
               largeMessage.decrementDelayDeletionCount();
            }

            largeMessageDeliverer = null;

            largeMessageInDelivery = false;

            largeMessage = null;
         }
      }
   }

   private class BrowserDeliverer implements Runnable
   {
      private MessageReference current = null;

      public BrowserDeliverer(final LinkedListIterator<MessageReference> iterator)
      {
         this.iterator = iterator;
      }

      private final LinkedListIterator<MessageReference> iterator;
      
      public synchronized void close()
      {
         iterator.close();
      }

      public synchronized void run()
      {
         // if the reference was busy during the previous iteration, handle it now
         if (current != null)
         {
            try
            {
               HandleStatus status = handle(current);

               if (status == HandleStatus.BUSY)
               {
                  return;
               }

               current = null;
            }
            catch (Exception e)
            {
               ServerConsumerImpl.log.error("Exception while browser handled from " + messageQueue + ": " + current, e);
               return;
            }
         }

         while (iterator.hasNext())
         {
            MessageReference ref = iterator.next();
            try
            {
               HandleStatus status = handle(ref);
               if (status == HandleStatus.BUSY)
               {
                  // keep a reference on the current message reference
                  // to handle it next time the browser deliverer is executed
                  current = ref;
                  break;
               }
            }
            catch (Exception e)
            {
               ServerConsumerImpl.log.error("Exception while browser handled from " + messageQueue + ": " + ref, e);
               break;
            }
         }

      }

   }
}
