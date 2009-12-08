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
package org.hornetq.jms.example;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.InitialContext;

import org.hornetq.common.example.HornetQExample;
import org.hornetq.jms.HornetQQueue;
import org.hornetq.jms.server.management.impl.JMSManagementHelper;

/**
 * This examples demonstrates a connection created to a server. Failure of the network connection is then simulated
 * 
 * The network is brought back up and the client reconnects and resumes transparently.
 *
 * @author <a href="tim.fox@jboss.com>Tim Fox</a>
 */
public class ReattachExample extends HornetQExample
{
   public static void main(final String[] args)
   {
      new ReattachExample().run(args);
   }

   @Override
   public boolean runExample() throws Exception
   {
      Connection connection = null;
      InitialContext initialContext = null;

      try
      {
         // Step 1. Create an initial context to perform the JNDI lookup.
         initialContext = getContext(0);

         // Step 2. Perform a lookup on the queue
         Queue queue = (Queue)initialContext.lookup("/queue/exampleQueue");

         // Step 3. Perform a lookup on the Connection Factory
         ConnectionFactory cf = (ConnectionFactory)initialContext.lookup("/ConnectionFactory");

         // Step 4. Create a JMS Connection
         connection = cf.createConnection();

         // Step 5. Create a JMS Session
         Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

         // Step 6. Create a JMS Message Producer
         MessageProducer producer = session.createProducer(queue);

         // Step 7. Create a Text Message
         TextMessage message = session.createTextMessage("This is a text message");

         System.out.println("Sent message: " + message.getText());

         // Step 8. Send the Message
         producer.send(message);

         // Step 9. Create a JMS Message Consumer
         MessageConsumer messageConsumer = session.createConsumer(queue);

         // Step 10. Start the Connection
         connection.start();

         // Step 11. To simulate a temporary problem on the network, we stop the remoting acceptor on the
         // server which will close all connections
         stopAcceptor(initialContext);

         System.out.println("Acceptor now stopped, will wait for 10 seconds. This simulates the network connection failing for a while");

         // Step 12. Wait a while then restart the acceptor
         Thread.sleep(10000);

         System.out.println("Re-starting acceptor");

         startAcceptor(initialContext);

         System.out.println("Restarted acceptor. The client will now reconnect.");

         // Step 13. We receive the message
         TextMessage messageReceived = (TextMessage)messageConsumer.receive(5000);

         System.out.println("Received message: " + messageReceived.getText());

         return true;
      }
      finally
      {
         // Step 14. Be sure to close our JMS resources!
         if (initialContext != null)
         {
            initialContext.close();
         }

         if (connection != null)
         {
            connection.close();
         }
      }
   }

   private void stopAcceptor(final InitialContext ic) throws Exception
   {
      stopStartAcceptor(ic, true);
   }

   private void startAcceptor(final InitialContext ic) throws Exception
   {
      stopStartAcceptor(ic, false);
   }

   // To do this we send a management message to close the acceptor, we do this on a different
   // connection factory which uses a different remoting connection so we can still send messages
   // when the main connection has been stopped
   private void stopStartAcceptor(final InitialContext initialContext, final boolean stop) throws Exception
   {
      ConnectionFactory cf = (ConnectionFactory)initialContext.lookup("/ConnectionFactory2");

      Connection connection = null;
      try
      {
         connection = cf.createConnection();

         Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

         Queue managementQueue = new HornetQQueue("hornetq.management", "hornetq.management");

         MessageProducer producer = session.createProducer(managementQueue);

         connection.start();

         Message m = session.createMessage();

         String oper = stop ? "stop" : "start";

         JMSManagementHelper.putOperationInvocation(m, "core.acceptor.netty-acceptor", oper);

         producer.send(m);
      }
      finally
      {
         if (connection != null)
         {
            connection.close();
         }
      }
   }

}
