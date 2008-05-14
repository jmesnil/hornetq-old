/*
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.messaging.tests.unit.core.remoting.impl;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.jboss.messaging.tests.integration.core.remoting.mina.TestSupport.MANY_MESSAGES;
import static org.jboss.messaging.tests.util.RandomUtil.randomLong;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import junit.framework.TestCase;

import org.jboss.messaging.core.config.Configuration;
import org.jboss.messaging.core.logging.Logger;
import org.jboss.messaging.core.remoting.NIOConnector;
import org.jboss.messaging.core.remoting.NIOSession;
import org.jboss.messaging.core.remoting.PacketDispatcher;
import org.jboss.messaging.core.remoting.PacketReturner;
import org.jboss.messaging.core.remoting.impl.PacketDispatcherImpl;
import org.jboss.messaging.core.remoting.impl.wireformat.Ping;
import org.jboss.messaging.tests.integration.core.remoting.mina.PingHandler;
import org.jboss.messaging.tests.unit.core.remoting.TestPacketHandler;

/**
 * @author <a href="mailto:jmesnil@redhat.com">Jeff Mesnil</a>.
 * 
 * @version <tt>$Revision$</tt>
 */
public abstract class SessionTestBase extends TestCase
{
   // Constants -----------------------------------------------------

   private static final Logger log = Logger.getLogger(SessionTestBase.class);

   // Attributes ----------------------------------------------------

   protected PingHandler serverPacketHandler;

   protected PacketDispatcher serverDispatcher;
   protected PacketDispatcher clientDispatcher;

   protected NIOConnector connector;

   protected NIOSession session;

   // Static --------------------------------------------------------

   // Constructors --------------------------------------------------

   // Public --------------------------------------------------------

   public void testConnected() throws Exception
   {
      NIOConnector connector = createNIOConnector(new PacketDispatcherImpl(null));
      NIOSession session = connector.connect();

      assertTrue(session.isConnected());
      
      assertTrue(connector.disconnect());
      assertFalse(session.isConnected());
      
   }    
      
   public void testWrite() throws Exception
   {
      serverPacketHandler.expectMessage(1);

      Ping packet = new Ping(randomLong());
      packet.setTargetID(serverPacketHandler.getID());
      
      session.write(packet);

      assertTrue(serverPacketHandler.await(2, SECONDS));

      List<Ping> messages = serverPacketHandler.getPackets();
      assertEquals(1, messages.size());
      assertEquals(packet.getSessionID(), messages.get(0).getSessionID());
   }

   public void testWriteMany() throws Exception
   {
      serverPacketHandler.expectMessage(MANY_MESSAGES);

      Ping[] packets = new Ping[MANY_MESSAGES];
      for (int i = 0; i < MANY_MESSAGES; i++)
      {
         packets[i] = new Ping(i);
         packets[i].setTargetID(serverPacketHandler.getID());
         session.write(packets[i]);
      }

      assertTrue(serverPacketHandler.await(10, SECONDS));

      List<Ping> receivedPackets = serverPacketHandler.getPackets();
      assertEquals(MANY_MESSAGES, receivedPackets.size());
      for (int i = 0; i < MANY_MESSAGES; i++)
      {
         Ping receivedPacket = receivedPackets.get(i);
         assertEquals(packets[i].getSessionID(), receivedPacket.getSessionID());
      }
   }

   public void testClientHandlePacketSentByServer() throws Exception
   {
      TestPacketHandler clientHandler = new TestPacketHandler(generateID());
      clientDispatcher.register(clientHandler);

      serverPacketHandler.expectMessage(1);
      clientHandler.expectMessage(1);

      Ping packet = new Ping(randomLong());
      packet.setTargetID(serverPacketHandler.getID());
      packet.setResponseTargetID(serverPacketHandler.getID());
      // send a packet to create a sender when the server
      // handles the packet
      session.write(packet);

      assertTrue(serverPacketHandler.await(2, SECONDS));

      assertNotNull(serverPacketHandler.getLastSender());
      PacketReturner sender = serverPacketHandler.getLastSender();
      Ping packetFromServer = new Ping(randomLong());
      packetFromServer.setTargetID(clientHandler.getID());
      sender.send(packetFromServer);
      
      assertTrue(clientHandler.await(2, SECONDS));

      List<Ping> packets = clientHandler.getPackets();
      assertEquals(1, packets.size());
      Ping packetReceivedByClient = packets.get(0);
      assertEquals(packetFromServer.getSessionID(), packetReceivedByClient.getSessionID());
   }
   
   // Protected -----------------------------------------------------

   @Override
   protected void setUp() throws Exception
   {
      serverDispatcher = startServer();
      
      clientDispatcher = new PacketDispatcherImpl(null);

      connector = createNIOConnector(clientDispatcher);
      session = connector.connect();
      
      serverPacketHandler = new PingHandler(generateID());
      serverDispatcher.register(serverPacketHandler);
      
   }

   @Override
   protected void tearDown() throws Exception
   {
      serverDispatcher.unregister(serverPacketHandler.getID());

      connector.disconnect();
      stopServer();
      
      connector = null;
      session = null;
      serverDispatcher = null;
   }
   
   protected abstract Configuration createRemotingConfiguration();
   
   protected abstract NIOConnector createNIOConnector(PacketDispatcher dispatcher);

   protected abstract PacketDispatcher startServer() throws Exception;
   
   protected abstract void stopServer();
   
   private AtomicLong idSequence = new AtomicLong(0);
   
   private long generateID()
   {
   	return idSequence.getAndIncrement();
   }
}
