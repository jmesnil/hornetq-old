/*
 * JBoss, Home of Professional Open SourceCopyright 2005-2008, Red Hat
 * Middleware LLC, and individual contributors by the @authors tag. See the
 * copyright.txt in the distribution for a full listing of individual
 * contributors.
 * 
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * 
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */
package org.jboss.messaging.example;

import static org.jboss.messaging.core.config.impl.ConfigurationImpl.DEFAULT_MANAGEMENT_ADDRESS;
import static org.jboss.messaging.core.config.impl.ConfigurationImpl.DEFAULT_MANAGEMENT_NOTIFICATION_ADDRESS;

import java.util.Set;

import org.jboss.messaging.core.client.ClientConsumer;
import org.jboss.messaging.core.client.ClientMessage;
import org.jboss.messaging.core.client.ClientProducer;
import org.jboss.messaging.core.client.ClientSession;
import org.jboss.messaging.core.client.ClientSessionFactory;
import org.jboss.messaging.core.client.MessageHandler;
import org.jboss.messaging.core.client.impl.ClientSessionFactoryImpl;
import org.jboss.messaging.core.client.management.impl.ManagementHelper;
import org.jboss.messaging.core.config.TransportConfiguration;
import org.jboss.messaging.core.exception.MessagingException;
import org.jboss.messaging.core.management.impl.ManagementServiceImpl;
import org.jboss.messaging.jms.client.JBossTextMessage;
import org.jboss.messaging.util.SimpleString;

/*
 * Uses the core messaging API to send and receive a message to a queue.
 * 
 * @author <a href="jmesnil@redhat.com">Jeff Mesnil</a>
 */
public class ManagementClient
{
   public static void main(final String[] args) throws Exception
   {
      SimpleString replytoQueue = new SimpleString("replyto.adminQueue");

      ClientSessionFactory sessionFactory = new ClientSessionFactoryImpl(new TransportConfiguration("org.jboss.messaging.integration.transports.netty.NettyConnectorFactory"));
      final ClientSession clientSession = sessionFactory.createSession(false, true, true);
      SimpleString queue = new SimpleString("queuejms.testQueue");

      sendMessages(clientSession, queue);

      // add temporary destination and queue
      clientSession.addDestination(replytoQueue, false, true);
      clientSession.createQueue(replytoQueue, replytoQueue, null, false, true, true);

      ClientConsumer mngmntConsumer = clientSession.createConsumer(replytoQueue);
      mngmntConsumer.setMessageHandler(new MessageHandler()
      {

         public void onMessage(final ClientMessage message)
         {
            System.out.println("received management message");
            if (ManagementHelper.isOperationResult(message))
            {
               System.out.println("\toperation succeeded:" + ManagementHelper.hasOperationSucceeded(message));
               if (ManagementHelper.hasOperationSucceeded(message))
               {
                  System.out.println("\t- result=" + message.getProperty(new SimpleString("sendMessageToDLQ")));
               }
               else
               {
                  System.out.println("\t- exception=" + ManagementHelper.getOperationExceptionMessage(message));
               }
            }
            else if (ManagementHelper.isAttributesResult(message))
            {
               System.out.println("\tattributes:");
               System.out.println("\t- MessageCount=" + message.getProperty(new SimpleString("MessageCount")));
               System.out.println("\t- Durable=" + message.getProperty(new SimpleString("Durable")));
            }
            try
            {
               message.acknowledge();
            }
            catch (MessagingException e)
            {
               e.printStackTrace();
            }
         }

      });

      SimpleString notifQueue = new SimpleString("notifQueue");
      clientSession.createQueue(DEFAULT_MANAGEMENT_NOTIFICATION_ADDRESS, notifQueue, null, false, true, true);
      ClientConsumer notifConsumer = clientSession.createConsumer(notifQueue);
      notifConsumer.setMessageHandler(new MessageHandler()
      {

         public void onMessage(final ClientMessage message)
         {
            System.out.println("received notification" + message);
            Set<SimpleString> propertyNames = message.getPropertyNames();

            for (SimpleString key : propertyNames)
            {
               System.out.println(key + "=" + message.getProperty(key));
            }
            try
            {
               message.acknowledge();
            }
            catch (MessagingException e)
            {
               e.printStackTrace();
            }
         }

      });
      clientSession.start();

      // add and remove a destination to receive two notifications from the
      // server
      clientSession.addDestination(new SimpleString("anotherQueue"), false, true);
      clientSession.removeDestination(new SimpleString("anotherQueue"), false);

      ClientProducer producer = clientSession.createProducer(replytoQueue);

      // to set a new value for an attribute, invoke the corresponding setter
      // method
      ClientMessage mngmntMessage = clientSession.createClientMessage(false);
      ManagementHelper.putOperationInvocation(mngmntMessage,
                                              replytoQueue,
                                              ManagementServiceImpl.getMessagingServerObjectName(),
                                              "setMessageCounterSamplePeriod",
                                              (long)30000);
      producer.send(DEFAULT_MANAGEMENT_ADDRESS, mngmntMessage);
      System.out.println("sent management message to set an attribute");

      // create a message to retrieve one or many attributes
      mngmntMessage = clientSession.createClientMessage(false);
      ManagementHelper.putAttributes(mngmntMessage,
                                     replytoQueue,
                                     ManagementServiceImpl.getQueueObjectName(queue, queue),
                                     "MessageCount",
                                     "Durable");

      producer.send(DEFAULT_MANAGEMENT_ADDRESS, mngmntMessage);
      System.out.println("sent management message to retrieve attributes");

      // create a message to invoke the operation sendMessageToDLQ(long) on the
      // queue
      mngmntMessage = clientSession.createClientMessage(false);
      ManagementHelper.putOperationInvocation(mngmntMessage,
                                              replytoQueue,
                                              ManagementServiceImpl.getQueueObjectName(queue, queue),
                                              "sendMessageToDLQ",
                                              (long)6161);
      producer.send(DEFAULT_MANAGEMENT_ADDRESS, mngmntMessage);
      System.out.println("sent management message to invoke operation");

      Thread.sleep(5000);

      mngmntConsumer.close();

      consumeMessages(clientSession, queue);

      notifConsumer.close();
      clientSession.removeDestination(replytoQueue, false);
      clientSession.deleteQueue(replytoQueue);
      clientSession.deleteQueue(notifQueue);

      clientSession.close();
   }

   private static void consumeMessages(final ClientSession clientSession, final SimpleString queue) throws MessagingException
   {
      ClientConsumer clientConsumer = clientSession.createConsumer(queue);
      ClientMessage m = null;
      do
      {
         m = clientConsumer.receive(5000);
         if (m != null)
         {
            m.acknowledge();
         }
      }
      while (m != null);
      clientSession.commit();
      System.out.println("consumed all the messages from " + queue);
   }

   private static void sendMessages(final ClientSession clientSession, final SimpleString queue) throws MessagingException
   {
      ClientProducer clientProducer = clientSession.createProducer(queue);
      ClientMessage message = clientSession.createClientMessage(JBossTextMessage.TYPE,
                                                                false,
                                                                0,
                                                                System.currentTimeMillis(),
                                                                (byte)1);
      message.getBody().putString("Hello, World!");
      clientProducer.send(message);
      clientProducer.send(message);
      clientProducer.send(message);
      clientProducer.send(message);
      System.out.println("sent 4 messages to " + queue);
   }
}
