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

package org.hornetq.core.journal.impl;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.hornetq.core.asyncio.BufferCallback;
import org.hornetq.core.asyncio.impl.AsynchronousFileImpl;
import org.hornetq.core.journal.SequentialFile;
import org.hornetq.core.logging.Logger;
import org.hornetq.utils.HornetQThreadFactory;

/**
 *
 * A AIOSequentialFileFactory
 *
 * @author clebert.suconic@jboss.com
 *
 */
public class AIOSequentialFileFactory extends AbstractSequentialFileFactory
{

   private static final Logger log = Logger.getLogger(AIOSequentialFileFactory.class);

   private static final boolean trace = AIOSequentialFileFactory.log.isTraceEnabled();

   private final ReuseBuffersController buffersControl = new ReuseBuffersController();

   private ExecutorService pollerExecutor;

   // This method exists just to make debug easier.
   // I could replace log.trace by log.info temporarily while I was debugging
   // Journal
   private static final void trace(final String message)
   {
      AIOSequentialFileFactory.log.trace(message);
   }

   public AIOSequentialFileFactory(final String journalDir)
   {
      this(journalDir,
           JournalConstants.DEFAULT_JOURNAL_BUFFER_SIZE_AIO,
           JournalConstants.DEFAULT_JOURNAL_BUFFER_TIMEOUT_AIO,
           false);
   }

   public AIOSequentialFileFactory(final String journalDir,
                                   final int bufferSize,
                                   final int bufferTimeout,
                                   final boolean logRates)
   {
      super(journalDir, true, bufferSize, bufferTimeout, logRates);
   }

   public SequentialFile createSequentialFile(final String fileName, final int maxIO)
   {
      return new AIOSequentialFile(this,
                                   bufferSize,
                                   bufferTimeout,
                                   journalDir,
                                   fileName,
                                   maxIO,
                                   buffersControl.callback,
                                   writeExecutor,
                                   pollerExecutor);
   }

   public boolean isSupportsCallbacks()
   {
      return true;
   }

   public static boolean isSupported()
   {
      return AsynchronousFileImpl.isLoaded();
   }

   public ByteBuffer newBuffer(int size)
   {
      if (size % 512 != 0)
      {
         size = (size / 512 + 1) * 512;
      }

      return buffersControl.newBuffer(size);
   }

   public void clearBuffer(final ByteBuffer directByteBuffer)
   {
      AsynchronousFileImpl.clearBuffer(directByteBuffer);
   }

   public int getAlignment()
   {
      return 512;
   }

   // For tests only
   public ByteBuffer wrapBuffer(final byte[] bytes)
   {
      ByteBuffer newbuffer = newBuffer(bytes.length);
      newbuffer.put(bytes);
      return newbuffer;
   }

   public int calculateBlockSize(final int position)
   {
      int alignment = getAlignment();

      int pos = (position / alignment + (position % alignment != 0 ? 1 : 0)) * alignment;

      return pos;
   }

   /* (non-Javadoc)
    * @see org.hornetq.core.journal.SequentialFileFactory#releaseBuffer(java.nio.ByteBuffer)
    */
   @Override
   public synchronized void releaseBuffer(final ByteBuffer buffer)
   {
      AsynchronousFileImpl.destroyBuffer(buffer);
   }

   @Override
   public void start()
   {
      super.start();

      pollerExecutor = Executors.newCachedThreadPool(new HornetQThreadFactory("HornetQ-AIO-poller-pool" + System.identityHashCode(this),
                                                                              true,
                                                                              AbstractSequentialFileFactory.getThisClassLoader()));

   }

   @Override
   public void stop()
   {
      buffersControl.stop();

      if (pollerExecutor != null)
      {
         pollerExecutor.shutdown();

         try
         {
            if (!pollerExecutor.awaitTermination(AbstractSequentialFileFactory.EXECUTOR_TIMEOUT, TimeUnit.SECONDS))
            {
               AIOSequentialFileFactory.log.warn("Timed out on AIO poller shutdown",
                                                 new Exception("Timed out on AIO writer shutdown"));
            }
         }
         catch (InterruptedException e)
         {
         }
      }

      super.stop();
   }

   @Override
   protected void finalize()
   {
      stop();
   }

   /** Class that will control buffer-reuse */
   private class ReuseBuffersController
   {
      private volatile long bufferReuseLastTime = System.currentTimeMillis();

      /** This queue is fed by {@link JournalImpl.ReuseBuffersController.LocalBufferCallback}} which is called directly by NIO or NIO.
       * On the case of the AIO this is almost called by the native layer as soon as the buffer is not being used any more
       * and ready to be reused or GCed */
      private final ConcurrentLinkedQueue<ByteBuffer> reuseBuffersQueue = new ConcurrentLinkedQueue<ByteBuffer>();

      private boolean stopped = false;

      final BufferCallback callback = new LocalBufferCallback();

      public ByteBuffer newBuffer(final int size)
      {
         // if a new buffer wasn't requested in 10 seconds, we clear the queue
         // This is being done this way as we don't need another Timeout Thread
         // just to cleanup this
         if (bufferSize > 0 && System.currentTimeMillis() - bufferReuseLastTime > 10000)
         {
            if (AIOSequentialFileFactory.trace)
            {
               AIOSequentialFileFactory.trace("Clearing reuse buffers queue with " + reuseBuffersQueue.size() +
                                              " elements");
            }

            bufferReuseLastTime = System.currentTimeMillis();

            clearPoll();
         }

         // if a buffer is bigger than the configured-bufferSize, we just create a new
         // buffer.
         if (size > bufferSize)
         {
            return AsynchronousFileImpl.newBuffer(size);
         }
         else
         {
            // We need to allocate buffers following the rules of the storage
            // being used (AIO/NIO)
            int alignedSize = calculateBlockSize(size);

            // Try getting a buffer from the queue...
            ByteBuffer buffer = reuseBuffersQueue.poll();

            if (buffer == null)
            {
               // if empty create a new one.
               buffer = AsynchronousFileImpl.newBuffer(bufferSize);

               buffer.limit(alignedSize);
            }
            else
            {
               clearBuffer(buffer);

               // set the limit of the buffer to the bufferSize being required
               buffer.limit(alignedSize);
            }

            buffer.rewind();

            return buffer;
         }
      }

      public synchronized void stop()
      {
         stopped = true;
         clearPoll();
      }

      public synchronized void clearPoll()
      {
         ByteBuffer reusedBuffer;

         while ((reusedBuffer = reuseBuffersQueue.poll()) != null)
         {
            releaseBuffer(reusedBuffer);
         }
      }

      private class LocalBufferCallback implements BufferCallback
      {
         public void bufferDone(final ByteBuffer buffer)
         {
            synchronized (ReuseBuffersController.this)
            {

               if (stopped)
               {
                  releaseBuffer(buffer);
               }
               else
               {
                  bufferReuseLastTime = System.currentTimeMillis();

                  // If a buffer has any other than the configured bufferSize, the buffer
                  // will be just sent to GC
                  if (buffer.capacity() == bufferSize)
                  {
                     reuseBuffersQueue.offer(buffer);
                  }
                  else
                  {
                     releaseBuffer(buffer);
                  }
               }
            }
         }
      }
   }
}
