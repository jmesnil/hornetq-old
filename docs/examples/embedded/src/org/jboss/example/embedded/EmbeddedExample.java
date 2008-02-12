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
package org.jboss.example.embedded;

import static org.jboss.messaging.core.remoting.TransportType.TCP;

import javax.jms.Session;

import org.jboss.jms.client.api.*;
import org.jboss.jms.client.impl.ClientConnectionFactoryImpl;
import org.jboss.jms.message.JBossTextMessage;
import org.jboss.messaging.core.DestinationType;
import org.jboss.messaging.core.Message;
import org.jboss.messaging.core.MessagingServer;
import org.jboss.messaging.core.Queue;
import org.jboss.messaging.core.impl.MessageImpl;
import org.jboss.messaging.core.impl.QueueImpl;
import org.jboss.messaging.core.impl.server.MessagingServerImpl;
import org.jboss.messaging.core.remoting.RemotingConfiguration;

/**
 * @author <a href="ataylor@redhat.com">Andy Taylor</a>
 */
public class EmbeddedExample
{
   public static void main(String args[]) throws Exception
   {
      RemotingConfiguration remotingConf = new RemotingConfiguration(TCP, "localhost", 5400);
      MessagingServer messagingServer = new MessagingServerImpl(remotingConf);
      messagingServer.start();
      ClientConnectionFactory cf = new ClientConnectionFactoryImpl(remotingConf);
      ClientConnection clientConnection = cf.createConnection(null, null);
      ClientSession clientSession = clientConnection.createClientSession(false, true, true, 0);
      clientSession.createQueue("Queue1", "Queue1", null, false, false);
      ClientProducer clientProducer = clientSession.createProducer();

      ClientConsumer clientConsumer = clientSession.createConsumer("Queue1", null, false, false, true);
      clientConnection.start();
      MessageImpl message = new MessageImpl(JBossTextMessage.TYPE, true, 0, System.currentTimeMillis(), (byte) 1);
      message.setPayload("Hello".getBytes());
      clientProducer.send("Queue1", message);


      Message m = clientConsumer.receive(0);
      System.out.println("m = " + new String(m.getPayload()));
      clientConnection.close();

      messagingServer.stop();
   }
}
