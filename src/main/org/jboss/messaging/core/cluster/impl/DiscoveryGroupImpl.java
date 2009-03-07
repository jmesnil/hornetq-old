/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005-2008, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.messaging.core.cluster.impl;

import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jboss.messaging.core.buffers.ChannelBuffers;
import org.jboss.messaging.core.cluster.DiscoveryGroup;
import org.jboss.messaging.core.cluster.DiscoveryListener;
import org.jboss.messaging.core.config.TransportConfiguration;
import org.jboss.messaging.core.logging.Logger;
import org.jboss.messaging.core.remoting.spi.MessagingBuffer;
import org.jboss.messaging.utils.Pair;

/**
 * A DiscoveryGroupImpl
 *
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 * 
 * Created 17 Nov 2008 13:21:45
 *
 */
public class DiscoveryGroupImpl implements Runnable, DiscoveryGroup
{
   private static final Logger log = Logger.getLogger(DiscoveryGroupImpl.class);

   private static final int SOCKET_TIMEOUT = 500;

   private MulticastSocket socket;

   private final List<DiscoveryListener> listeners = new ArrayList<DiscoveryListener>();

   private final String name;

   private Thread thread;

   private boolean received;

   private final Object waitLock = new Object();

   private final Map<Pair<TransportConfiguration, TransportConfiguration>, Long> connectors = new HashMap<Pair<TransportConfiguration, TransportConfiguration>, Long>();

   private final long timeout;

   private volatile boolean started;

   private final String nodeID;
   
   private final InetAddress groupAddress;
   
   private final int groupPort;

   public DiscoveryGroupImpl(final String nodeID,
                             final String name,
                             final InetAddress groupAddress,
                             final int groupPort,
                             final long timeout) throws Exception
   {
      this.nodeID = nodeID;

      this.name = name;     

      this.timeout = timeout;     
      
      this.groupAddress = groupAddress;
      
      this.groupPort = groupPort;
   }

   public synchronized void start() throws Exception
   {
      if (started)
      {
         return;
      }
      
      socket = new MulticastSocket(groupPort);

      socket.joinGroup(groupAddress);

      socket.setSoTimeout(SOCKET_TIMEOUT);

      started = true;
      
      thread = new Thread(this);

      thread.setDaemon(true);

      thread.start();
   }

   public void stop()
   {
      synchronized (this)
      {
         if (!started)
         {
            return;
         }

         started = false;
      }

      try
      {
         thread.join();
      }
      catch (InterruptedException e)
      {
      }

      socket.close();
      
      socket = null;
      
      thread = null;
   }

   public boolean isStarted()
   {
      return started;
   }

   public String getName()
   {
      return name;
   }

   public synchronized List<Pair<TransportConfiguration, TransportConfiguration>> getConnectors()
   {
      return new ArrayList<Pair<TransportConfiguration, TransportConfiguration>>(connectors.keySet());
   }

   public boolean waitForBroadcast(final long timeout)
   {
      synchronized (waitLock)
      {
         long start = System.currentTimeMillis();

         long toWait = timeout;

         while (!received && toWait > 0)
         {
            try
            {
               waitLock.wait(toWait);
            }
            catch (InterruptedException e)
            {
            }

            long now = System.currentTimeMillis();

            toWait -= now - start;

            start = now;
         }

         boolean ret = received;

         received = false;

         return ret;
      }
   }

   public void run()
   {
      try
      {
         // TODO - can we use a smaller buffer size?
         final byte[] data = new byte[65535];

         while (true)
         {
            if (!started)
            {
               return;
            }

            final DatagramPacket packet = new DatagramPacket(data, data.length);

            try
            {
               socket.receive(packet);
            }
            catch (InterruptedIOException e)
            {
               if (!started)
               {
                  return;
               }
               else
               {
                  continue;
               }
            }
            
            MessagingBuffer buffer = ChannelBuffers.wrappedBuffer(data);
            
            String originatingNodeID = buffer.readString();

            if (nodeID.equals(originatingNodeID))
            {
               // Ignore traffic from own node
               continue;
            }

            int size = buffer.readInt();

            boolean changed = false;

            synchronized (this)
            {
               for (int i = 0; i < size; i++)
               {
                  TransportConfiguration connector = new TransportConfiguration();
                  
                  connector.decode(buffer);

                  boolean existsBackup = buffer.readBoolean();

                  TransportConfiguration backupConnector = null;

                  if (existsBackup)
                  {
                     backupConnector = new TransportConfiguration();
                     
                     backupConnector.decode(buffer);
                  }

                  Pair<TransportConfiguration, TransportConfiguration> connectorPair = new Pair<TransportConfiguration, TransportConfiguration>(connector,
                                                                                                                                                backupConnector);

                  Long oldVal = connectors.put(connectorPair, System.currentTimeMillis());

                  if (oldVal == null)
                  {
                     changed = true;
                  }
               }

               long now = System.currentTimeMillis();

               Iterator<Map.Entry<Pair<TransportConfiguration, TransportConfiguration>, Long>> iter = connectors.entrySet()
                                                                                                                .iterator();
               // Weed out any expired connectors

               while (iter.hasNext())
               {
                  Map.Entry<Pair<TransportConfiguration, TransportConfiguration>, Long> entry = iter.next();

                  if (entry.getValue() + timeout <= now)
                  {
                     iter.remove();

                     changed = true;
                  }
               }
            }

            if (changed)
            {
               callListeners();
            }

            synchronized (waitLock)
            {
               received = true;

               waitLock.notify();
            }
         }
      }
      catch (Exception e)
      {
         log.error("Failed to receive datagram", e);
      }
   }

   public synchronized void registerListener(final DiscoveryListener listener)
   {
      listeners.add(listener);

      if (!connectors.isEmpty())
      {
         listener.connectorsChanged();
      }
   }

   public synchronized void unregisterListener(final DiscoveryListener listener)
   {
      listeners.remove(listener);
   }

   private void callListeners()
   {
      for (DiscoveryListener listener : listeners)
      {
         try
         {
            listener.connectorsChanged();
         }
         catch (Throwable t)
         {
            // Catch it so exception doesn't prevent other listeners from running
            log.error("Failed to call discovery listener", t);
         }
      }
   }

}
