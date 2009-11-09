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

package org.hornetq.tests.integration.divert;

import java.util.ArrayList;
import java.util.List;

import org.hornetq.core.client.ClientConsumer;
import org.hornetq.core.client.ClientMessage;
import org.hornetq.core.client.ClientProducer;
import org.hornetq.core.client.ClientSession;
import org.hornetq.core.client.ClientSessionFactory;
import org.hornetq.core.client.impl.ClientSessionFactoryImpl;
import org.hornetq.core.config.Configuration;
import org.hornetq.core.config.TransportConfiguration;
import org.hornetq.core.config.cluster.DivertConfiguration;
import org.hornetq.core.logging.Logger;
import org.hornetq.core.server.HornetQ;
import org.hornetq.core.server.HornetQServer;
import org.hornetq.tests.util.ServiceTestBase;
import org.hornetq.utils.SimpleString;

/**
 * A DivertTest
 *
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 * 
 * Created 14 Jan 2009 14:05:01
 *
 *
 */
public class DivertTest extends ServiceTestBase
{
   private static final Logger log = Logger.getLogger(DivertTest.class);

   private static final int TIMEOUT = 500;
   
   public void testSingleNonExclusiveDivert() throws Exception
   {
      Configuration conf = createDefaultConfig();

      conf.setClustered(true);

      final String testAddress = "testAddress";

      final String forwardAddress = "forwardAddress";

      DivertConfiguration divertConf = new DivertConfiguration("divert1",
                                                               "divert1",
                                                               testAddress,
                                                               forwardAddress,
                                                               false,
                                                               null,
                                                               null);

      List<DivertConfiguration> divertConfs = new ArrayList<DivertConfiguration>();

      divertConfs.add(divertConf);

      conf.setDivertConfigurations(divertConfs);

      HornetQServer messagingService = HornetQ.newHornetQServer(conf, false);

      messagingService.start();

      ClientSessionFactory sf = new ClientSessionFactoryImpl(new TransportConfiguration("org.hornetq.core.remoting.impl.invm.InVMConnectorFactory"));

      ClientSession session = sf.createSession(false, true, true);

      final SimpleString queueName1 = new SimpleString("queue1");

      final SimpleString queueName2 = new SimpleString("queue2");

      session.createQueue(new SimpleString(forwardAddress), queueName1, null, false);

      session.createQueue(new SimpleString(testAddress), queueName2, null, false);

      session.start();

      ClientProducer producer = session.createProducer(new SimpleString(testAddress));

      ClientConsumer consumer1 = session.createConsumer(queueName1);

      ClientConsumer consumer2 = session.createConsumer(queueName2);

      final int numMessages = 1;

      final SimpleString propKey = new SimpleString("testkey");

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = session.createClientMessage(false);

         message.putIntProperty(propKey, i);

         producer.send(message);
      }

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = consumer1.receive(TIMEOUT);

         assertNotNull(message);

         assertEquals((Integer)i, (Integer)message.getObjectProperty(propKey));

         message.acknowledge();
      }

      assertNull(consumer1.receiveImmediate());

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = consumer2.receive(TIMEOUT);

         assertNotNull(message);

         assertEquals((Integer)i, (Integer)message.getObjectProperty(propKey));

         message.acknowledge();
      }

      assertNull(consumer2.receiveImmediate());

      session.close();

      sf.close();

      messagingService.stop();
   }

   public void testSingleNonExclusiveDivert2() throws Exception
   {
      Configuration conf = createDefaultConfig();

      conf.setClustered(true);

      final String testAddress = "testAddress";

      final String forwardAddress = "forwardAddress";

      DivertConfiguration divertConf = new DivertConfiguration("divert1",
                                                               "divert1",
                                                               testAddress,
                                                               forwardAddress,
                                                               false,
                                                               null,
                                                               null);

      List<DivertConfiguration> divertConfs = new ArrayList<DivertConfiguration>();

      divertConfs.add(divertConf);

      conf.setDivertConfigurations(divertConfs);

      HornetQServer messagingService = HornetQ.newHornetQServer(conf, false);

      messagingService.start();

      ClientSessionFactory sf = new ClientSessionFactoryImpl(new TransportConfiguration("org.hornetq.core.remoting.impl.invm.InVMConnectorFactory"));

      ClientSession session = sf.createSession(false, true, true);

      final SimpleString queueName1 = new SimpleString("queue1");

      final SimpleString queueName2 = new SimpleString("queue2");

      final SimpleString queueName3 = new SimpleString("queue3");

      final SimpleString queueName4 = new SimpleString("queue4");

      session.createQueue(new SimpleString(forwardAddress), queueName1, null, false);

      session.createQueue(new SimpleString(testAddress), queueName2, null, false);

      session.createQueue(new SimpleString(testAddress), queueName3, null, false);

      session.createQueue(new SimpleString(testAddress), queueName4, null, false);

      session.start();

      ClientProducer producer = session.createProducer(new SimpleString(testAddress));

      ClientConsumer consumer1 = session.createConsumer(queueName1);

      ClientConsumer consumer2 = session.createConsumer(queueName2);

      ClientConsumer consumer3 = session.createConsumer(queueName3);

      ClientConsumer consumer4 = session.createConsumer(queueName4);

      final int numMessages = 10;

      final SimpleString propKey = new SimpleString("testkey");

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = session.createClientMessage(false);

         message.putIntProperty(propKey, i);

         producer.send(message);
      }

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = consumer1.receive(TIMEOUT);

         assertNotNull(message);

         assertEquals((Integer)i, (Integer)message.getObjectProperty(propKey));

         message.acknowledge();
      }

      assertNull(consumer1.receiveImmediate());

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = consumer2.receive(TIMEOUT);

         assertNotNull(message);

         assertEquals((Integer)i, (Integer)message.getObjectProperty(propKey));

         message.acknowledge();
      }

      assertNull(consumer2.receiveImmediate());

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = consumer3.receive(TIMEOUT);

         assertNotNull(message);

         assertEquals((Integer)i, (Integer)message.getObjectProperty(propKey));

         message.acknowledge();
      }

      assertNull(consumer3.receiveImmediate());

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = consumer4.receive(TIMEOUT);

         assertNotNull(message);

         assertEquals((Integer)i, (Integer)message.getObjectProperty(propKey));

         message.acknowledge();
      }

      assertNull(consumer4.receiveImmediate());

      session.close();

      sf.close();

      messagingService.stop();
   }

   public void testSingleNonExclusiveDivert3() throws Exception
   {
      Configuration conf = createDefaultConfig();

      conf.setClustered(true);

      final String testAddress = "testAddress";

      final String forwardAddress = "forwardAddress";

      DivertConfiguration divertConf = new DivertConfiguration("divert1",
                                                               "divert1",
                                                               testAddress,
                                                               forwardAddress,
                                                               false,
                                                               null,
                                                               null);

      List<DivertConfiguration> divertConfs = new ArrayList<DivertConfiguration>();

      divertConfs.add(divertConf);

      conf.setDivertConfigurations(divertConfs);

      HornetQServer messagingService = HornetQ.newHornetQServer(conf, false);

      messagingService.start();

      ClientSessionFactory sf = new ClientSessionFactoryImpl(new TransportConfiguration("org.hornetq.core.remoting.impl.invm.InVMConnectorFactory"));

      ClientSession session = sf.createSession(false, true, true);

      final SimpleString queueName1 = new SimpleString("queue1");

      session.createQueue(new SimpleString(forwardAddress), queueName1, null, false);

      session.start();

      ClientProducer producer = session.createProducer(new SimpleString(testAddress));

      ClientConsumer consumer1 = session.createConsumer(queueName1);

      final int numMessages = 10;

      final SimpleString propKey = new SimpleString("testkey");

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = session.createClientMessage(false);

         message.putIntProperty(propKey, i);

         producer.send(message);
      }

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = consumer1.receive(TIMEOUT);

         assertNotNull(message);

         assertEquals((Integer)i, (Integer)message.getObjectProperty(propKey));

         message.acknowledge();
      }

      assertNull(consumer1.receiveImmediate());

      session.close();

      sf.close();

      messagingService.stop();
   }

   public void testSingleExclusiveDivert() throws Exception
   {
      Configuration conf = createDefaultConfig();

      conf.setClustered(true);

      final String testAddress = "testAddress";

      final String forwardAddress = "forwardAddress";

      DivertConfiguration divertConf = new DivertConfiguration("divert1",
                                                               "divert1",
                                                               testAddress,
                                                               forwardAddress,
                                                               true,
                                                               null,
                                                               null);

      List<DivertConfiguration> divertConfs = new ArrayList<DivertConfiguration>();

      divertConfs.add(divertConf);

      conf.setDivertConfigurations(divertConfs);

      HornetQServer messagingService = HornetQ.newHornetQServer(conf, false);

      messagingService.start();

      ClientSessionFactory sf = new ClientSessionFactoryImpl(new TransportConfiguration("org.hornetq.core.remoting.impl.invm.InVMConnectorFactory"));

      ClientSession session = sf.createSession(false, true, true);

      final SimpleString queueName1 = new SimpleString("queue1");

      final SimpleString queueName2 = new SimpleString("queue2");

      final SimpleString queueName3 = new SimpleString("queue3");

      final SimpleString queueName4 = new SimpleString("queue4");

      session.createQueue(new SimpleString(forwardAddress), queueName1, null, false);

      session.createQueue(new SimpleString(testAddress), queueName2, null, false);
      session.createQueue(new SimpleString(testAddress), queueName3, null, false);
      session.createQueue(new SimpleString(testAddress), queueName4, null, false);

      session.start();

      ClientProducer producer = session.createProducer(new SimpleString(testAddress));

      ClientConsumer consumer1 = session.createConsumer(queueName1);

      ClientConsumer consumer2 = session.createConsumer(queueName2);

      ClientConsumer consumer3 = session.createConsumer(queueName3);

      ClientConsumer consumer4 = session.createConsumer(queueName4);

      final int numMessages = 10;

      final SimpleString propKey = new SimpleString("testkey");

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = session.createClientMessage(false);

         message.putIntProperty(propKey, i);

         producer.send(message);
      }

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = consumer1.receive(TIMEOUT);

         assertNotNull(message);

         assertEquals((Integer)i, (Integer)message.getObjectProperty(propKey));

         message.acknowledge();
      }

      assertNull(consumer1.receiveImmediate());

      assertNull(consumer2.receiveImmediate());

      assertNull(consumer3.receiveImmediate());

      assertNull(consumer4.receiveImmediate());

      session.close();

      sf.close();

      messagingService.stop();
   }

   public void testMultipleNonExclusiveDivert() throws Exception
   {
      Configuration conf = createDefaultConfig();

      conf.setClustered(true);

      final String testAddress = "testAddress";

      final String forwardAddress1 = "forwardAddress1";
      final String forwardAddress2 = "forwardAddress2";
      final String forwardAddress3 = "forwardAddress3";

      DivertConfiguration divertConf1 = new DivertConfiguration("divert1",
                                                                "divert1",
                                                                testAddress,
                                                                forwardAddress1,
                                                                false,
                                                                null,
                                                                null);

      DivertConfiguration divertConf2 = new DivertConfiguration("divert2",
                                                                "divert2",
                                                                testAddress,
                                                                forwardAddress2,
                                                                false,
                                                                null,
                                                                null);

      DivertConfiguration divertConf3 = new DivertConfiguration("divert3",
                                                                "divert3",
                                                                testAddress,
                                                                forwardAddress3,
                                                                false,
                                                                null,
                                                                null);

      List<DivertConfiguration> divertConfs = new ArrayList<DivertConfiguration>();

      divertConfs.add(divertConf1);
      divertConfs.add(divertConf2);
      divertConfs.add(divertConf3);

      conf.setDivertConfigurations(divertConfs);

      HornetQServer messagingService = HornetQ.newHornetQServer(conf, false);

      messagingService.start();

      ClientSessionFactory sf = new ClientSessionFactoryImpl(new TransportConfiguration("org.hornetq.core.remoting.impl.invm.InVMConnectorFactory"));

      ClientSession session = sf.createSession(false, true, true);

      final SimpleString queueName1 = new SimpleString("queue1");

      final SimpleString queueName2 = new SimpleString("queue2");

      final SimpleString queueName3 = new SimpleString("queue3");

      final SimpleString queueName4 = new SimpleString("queue4");

      session.createQueue(new SimpleString(forwardAddress1), queueName1, null, false);

      session.createQueue(new SimpleString(forwardAddress2), queueName2, null, false);

      session.createQueue(new SimpleString(forwardAddress3), queueName3, null, false);

      session.createQueue(new SimpleString(testAddress), queueName4, null, false);

      session.start();

      ClientProducer producer = session.createProducer(new SimpleString(testAddress));

      ClientConsumer consumer1 = session.createConsumer(queueName1);

      ClientConsumer consumer2 = session.createConsumer(queueName2);

      ClientConsumer consumer3 = session.createConsumer(queueName3);

      ClientConsumer consumer4 = session.createConsumer(queueName4);

      final int numMessages = 10;

      final SimpleString propKey = new SimpleString("testkey");

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = session.createClientMessage(false);

         message.putIntProperty(propKey, i);

         producer.send(message);
      }

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = consumer1.receive(TIMEOUT);

         assertNotNull(message);

         assertEquals(i, message.getIntProperty(propKey).intValue());

         message.acknowledge();
      }

      assertNull(consumer1.receiveImmediate());

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = consumer2.receive(TIMEOUT);

         assertNotNull(message);

         assertEquals(i, message.getIntProperty(propKey).intValue());

         message.acknowledge();
      }

      assertNull(consumer2.receiveImmediate());

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = consumer3.receive(TIMEOUT);

         assertNotNull(message);

         assertEquals(i, message.getIntProperty(propKey).intValue());

         message.acknowledge();
      }

      assertNull(consumer3.receiveImmediate());

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = consumer4.receive(TIMEOUT);

         assertNotNull(message);

         assertEquals(i, message.getIntProperty(propKey).intValue());

         message.acknowledge();
      }

      assertNull(consumer4.receiveImmediate());

      session.close();

      sf.close();

      messagingService.stop();
   }

   public void testMultipleExclusiveDivert() throws Exception
   {
      Configuration conf = createDefaultConfig();

      conf.setClustered(true);

      final String testAddress = "testAddress";

      final String forwardAddress1 = "forwardAddress1";
      final String forwardAddress2 = "forwardAddress2";
      final String forwardAddress3 = "forwardAddress3";

      DivertConfiguration divertConf1 = new DivertConfiguration("divert1",
                                                                "divert1",
                                                                testAddress,
                                                                forwardAddress1,
                                                                true,
                                                                null,
                                                                null);

      DivertConfiguration divertConf2 = new DivertConfiguration("divert2",
                                                                "divert2",
                                                                testAddress,
                                                                forwardAddress2,
                                                                true,
                                                                null,
                                                                null);

      DivertConfiguration divertConf3 = new DivertConfiguration("divert3",
                                                                "divert3",
                                                                testAddress,
                                                                forwardAddress3,
                                                                true,
                                                                null,
                                                                null);

      List<DivertConfiguration> divertConfs = new ArrayList<DivertConfiguration>();

      divertConfs.add(divertConf1);
      divertConfs.add(divertConf2);
      divertConfs.add(divertConf3);

      conf.setDivertConfigurations(divertConfs);

      HornetQServer messagingService = HornetQ.newHornetQServer(conf, false);

      messagingService.start();

      ClientSessionFactory sf = new ClientSessionFactoryImpl(new TransportConfiguration("org.hornetq.core.remoting.impl.invm.InVMConnectorFactory"));

      ClientSession session = sf.createSession(false, true, true);

      final SimpleString queueName1 = new SimpleString("queue1");

      final SimpleString queueName2 = new SimpleString("queue2");

      final SimpleString queueName3 = new SimpleString("queue3");

      final SimpleString queueName4 = new SimpleString("queue4");

      session.createQueue(new SimpleString(forwardAddress1), queueName1, null, false);

      session.createQueue(new SimpleString(forwardAddress2), queueName2, null, false);

      session.createQueue(new SimpleString(forwardAddress3), queueName3, null, false);

      session.createQueue(new SimpleString(testAddress), queueName4, null, false);

      session.start();

      ClientProducer producer = session.createProducer(new SimpleString(testAddress));

      ClientConsumer consumer1 = session.createConsumer(queueName1);

      ClientConsumer consumer2 = session.createConsumer(queueName2);

      ClientConsumer consumer3 = session.createConsumer(queueName3);

      ClientConsumer consumer4 = session.createConsumer(queueName4);

      final int numMessages = 10;

      final SimpleString propKey = new SimpleString("testkey");

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = session.createClientMessage(false);

         message.putIntProperty(propKey, i);

         producer.send(message);
      }

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = consumer1.receive(TIMEOUT);

         assertNotNull(message);

         assertEquals(i, message.getIntProperty(propKey).intValue());

         message.acknowledge();
      }

      assertNull(consumer1.receiveImmediate());

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = consumer2.receive(TIMEOUT);

         assertNotNull(message);

         assertEquals(i, message.getIntProperty(propKey).intValue());

         message.acknowledge();
      }

      assertNull(consumer2.receiveImmediate());

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = consumer3.receive(TIMEOUT);

         assertNotNull(message);

         assertEquals(i, message.getIntProperty(propKey).intValue());

         message.acknowledge();
      }

      assertNull(consumer3.receiveImmediate());

      assertNull(consumer4.receiveImmediate());

      session.close();

      sf.close();

      messagingService.stop();
   }

   public void testMixExclusiveAndNonExclusiveDiverts() throws Exception
   {
      Configuration conf = createDefaultConfig();

      conf.setClustered(true);

      final String testAddress = "testAddress";

      final String forwardAddress1 = "forwardAddress1";
      final String forwardAddress2 = "forwardAddress2";
      final String forwardAddress3 = "forwardAddress3";

      DivertConfiguration divertConf1 = new DivertConfiguration("divert1",
                                                                "divert1",
                                                                testAddress,
                                                                forwardAddress1,
                                                                true,
                                                                null,
                                                                null);

      DivertConfiguration divertConf2 = new DivertConfiguration("divert2",
                                                                "divert2",
                                                                testAddress,
                                                                forwardAddress2,
                                                                true,
                                                                null,
                                                                null);

      DivertConfiguration divertConf3 = new DivertConfiguration("divert3",
                                                                "divert3",
                                                                testAddress,
                                                                forwardAddress3,
                                                                false,
                                                                null,
                                                                null);

      List<DivertConfiguration> divertConfs = new ArrayList<DivertConfiguration>();

      divertConfs.add(divertConf1);
      divertConfs.add(divertConf2);
      divertConfs.add(divertConf3);

      conf.setDivertConfigurations(divertConfs);

      HornetQServer messagingService = HornetQ.newHornetQServer(conf, false);

      messagingService.start();

      ClientSessionFactory sf = new ClientSessionFactoryImpl(new TransportConfiguration("org.hornetq.core.remoting.impl.invm.InVMConnectorFactory"));

      ClientSession session = sf.createSession(false, true, true);

      final SimpleString queueName1 = new SimpleString("queue1");

      final SimpleString queueName2 = new SimpleString("queue2");

      final SimpleString queueName3 = new SimpleString("queue3");

      final SimpleString queueName4 = new SimpleString("queue4");

      session.createQueue(new SimpleString(forwardAddress1), queueName1, null, false);

      session.createQueue(new SimpleString(forwardAddress2), queueName2, null, false);

      session.createQueue(new SimpleString(forwardAddress3), queueName3, null, false);

      session.createQueue(new SimpleString(testAddress), queueName4, null, false);

      session.start();

      ClientProducer producer = session.createProducer(new SimpleString(testAddress));

      ClientConsumer consumer1 = session.createConsumer(queueName1);

      ClientConsumer consumer2 = session.createConsumer(queueName2);

      ClientConsumer consumer3 = session.createConsumer(queueName3);

      ClientConsumer consumer4 = session.createConsumer(queueName4);

      final int numMessages = 10;

      final SimpleString propKey = new SimpleString("testkey");

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = session.createClientMessage(false);

         message.putIntProperty(propKey, i);

         producer.send(message);
      }

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = consumer1.receive(TIMEOUT);

         assertNotNull(message);

         assertEquals(i, message.getIntProperty(propKey).intValue());

         message.acknowledge();
      }

      assertNull(consumer1.receiveImmediate());

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = consumer2.receive(TIMEOUT);

         assertNotNull(message);

         assertEquals(i, message.getIntProperty(propKey).intValue());

         message.acknowledge();
      }

      assertNull(consumer2.receiveImmediate());

      assertNull(consumer3.receiveImmediate());

      assertNull(consumer4.receiveImmediate());

      session.close();

      sf.close();

      messagingService.stop();
   }

   // If no exclusive diverts match then non exclusive ones should be called
   public void testSingleExclusiveNonMatchingAndNonExclusiveDiverts() throws Exception
   {
      Configuration conf = createDefaultConfig();

      conf.setClustered(true);

      final String testAddress = "testAddress";

      final String forwardAddress1 = "forwardAddress1";
      final String forwardAddress2 = "forwardAddress2";
      final String forwardAddress3 = "forwardAddress3";

      final String filter = "animal='antelope'";

      DivertConfiguration divertConf1 = new DivertConfiguration("divert1",
                                                                "divert1",
                                                                testAddress,
                                                                forwardAddress1,
                                                                true,
                                                                filter,
                                                                null);

      DivertConfiguration divertConf2 = new DivertConfiguration("divert2",
                                                                "divert2",
                                                                testAddress,
                                                                forwardAddress2,
                                                                false,
                                                                null,
                                                                null);

      DivertConfiguration divertConf3 = new DivertConfiguration("divert3",
                                                                "divert3",
                                                                testAddress,
                                                                forwardAddress3,
                                                                false,
                                                                null,
                                                                null);

      List<DivertConfiguration> divertConfs = new ArrayList<DivertConfiguration>();

      divertConfs.add(divertConf1);
      divertConfs.add(divertConf2);
      divertConfs.add(divertConf3);

      conf.setDivertConfigurations(divertConfs);

      HornetQServer messagingService = HornetQ.newHornetQServer(conf, false);

      messagingService.start();

      ClientSessionFactory sf = new ClientSessionFactoryImpl(new TransportConfiguration("org.hornetq.core.remoting.impl.invm.InVMConnectorFactory"));

      ClientSession session = sf.createSession(false, true, true);

      final SimpleString queueName1 = new SimpleString("queue1");

      final SimpleString queueName2 = new SimpleString("queue2");

      final SimpleString queueName3 = new SimpleString("queue3");

      final SimpleString queueName4 = new SimpleString("queue4");

      session.createQueue(new SimpleString(forwardAddress1), queueName1, null, false);

      session.createQueue(new SimpleString(forwardAddress2), queueName2, null, false);

      session.createQueue(new SimpleString(forwardAddress3), queueName3, null, false);

      session.createQueue(new SimpleString(testAddress), queueName4, null, false);

      session.start();

      ClientProducer producer = session.createProducer(new SimpleString(testAddress));

      ClientConsumer consumer1 = session.createConsumer(queueName1);

      ClientConsumer consumer2 = session.createConsumer(queueName2);

      ClientConsumer consumer3 = session.createConsumer(queueName3);

      ClientConsumer consumer4 = session.createConsumer(queueName4);

      final int numMessages = 10;

      final SimpleString propKey = new SimpleString("testkey");

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = session.createClientMessage(false);

         message.putStringProperty(new SimpleString("animal"), new SimpleString("giraffe"));

         message.putIntProperty(propKey, i);

         producer.send(message);
      }

      // for (int i = 0; i < numMessages; i++)
      // {
      // ClientMessage message = consumer1.receive(200);
      //         
      // assertNotNull(message);
      //         
      // assertEquals((Integer)i, (Integer)message.getProperty(propKey));
      //         
      // message.acknowledge();
      // }

      assertNull(consumer1.receiveImmediate());

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = consumer2.receive(TIMEOUT);

         assertNotNull(message);

         assertEquals((Integer)i, (Integer)message.getObjectProperty(propKey));

         message.acknowledge();
      }

      assertNull(consumer2.receiveImmediate());

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = consumer3.receive(TIMEOUT);

         assertNotNull(message);

         assertEquals((Integer)i, (Integer)message.getObjectProperty(propKey));

         message.acknowledge();
      }

      assertNull(consumer3.receiveImmediate());

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = consumer4.receive(TIMEOUT);

         assertNotNull(message);

         assertEquals((Integer)i, (Integer)message.getObjectProperty(propKey));

         message.acknowledge();
      }

      assertNull(consumer4.receiveImmediate());

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = session.createClientMessage(false);

         message.putStringProperty(new SimpleString("animal"), new SimpleString("antelope"));

         message.putIntProperty(propKey, i);

         producer.send(message);
      }

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = consumer1.receive(TIMEOUT);

         assertNotNull(message);

         assertEquals((Integer)i, (Integer)message.getObjectProperty(propKey));

         message.acknowledge();
      }

      assertNull(consumer1.receiveImmediate());

      assertNull(consumer2.receiveImmediate());

      assertNull(consumer3.receiveImmediate());

      assertNull(consumer4.receiveImmediate());

      session.close();

      sf.close();

      messagingService.stop();
   }

   public void testRoundRobinDiverts() throws Exception
   {
      Configuration conf = createDefaultConfig();

      conf.setClustered(true);

      final String testAddress = "testAddress";

      final String forwardAddress1 = "forwardAddress1";
      final String forwardAddress2 = "forwardAddress2";
      final String forwardAddress3 = "forwardAddress3";

      DivertConfiguration divertConf1 = new DivertConfiguration("divert1",
                                                                "thename",
                                                                testAddress,
                                                                forwardAddress1,
                                                                false,
                                                                null,
                                                                null);

      DivertConfiguration divertConf2 = new DivertConfiguration("divert2",
                                                                "thename",
                                                                testAddress,
                                                                forwardAddress2,
                                                                false,
                                                                null,
                                                                null);

      DivertConfiguration divertConf3 = new DivertConfiguration("divert3",
                                                                "thename",
                                                                testAddress,
                                                                forwardAddress3,
                                                                false,
                                                                null,
                                                                null);

      List<DivertConfiguration> divertConfs = new ArrayList<DivertConfiguration>();

      divertConfs.add(divertConf1);
      divertConfs.add(divertConf2);
      divertConfs.add(divertConf3);

      conf.setDivertConfigurations(divertConfs);

      HornetQServer messagingService = HornetQ.newHornetQServer(conf, false);

      messagingService.start();

      ClientSessionFactory sf = new ClientSessionFactoryImpl(new TransportConfiguration("org.hornetq.core.remoting.impl.invm.InVMConnectorFactory"));

      ClientSession session = sf.createSession(false, true, true);

      final SimpleString queueName1 = new SimpleString("queue1");

      final SimpleString queueName2 = new SimpleString("queue2");

      final SimpleString queueName3 = new SimpleString("queue3");

      final SimpleString queueName4 = new SimpleString("queue4");

      session.createQueue(new SimpleString(forwardAddress1), queueName1, null, false);

      session.createQueue(new SimpleString(forwardAddress2), queueName2, null, false);

      session.createQueue(new SimpleString(forwardAddress3), queueName3, null, false);

      session.createQueue(new SimpleString(testAddress), queueName4, null, false);

      session.start();

      ClientProducer producer = session.createProducer(new SimpleString(testAddress));

      ClientConsumer consumer1 = session.createConsumer(queueName1);

      ClientConsumer consumer2 = session.createConsumer(queueName2);

      ClientConsumer consumer3 = session.createConsumer(queueName3);

      ClientConsumer consumer4 = session.createConsumer(queueName4);

      final int numMessages = 10;

      final SimpleString propKey = new SimpleString("testkey");

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = session.createClientMessage(false);

         message.putIntProperty(propKey, i);

         producer.send(message);
      }

      for (int i = 0; i < numMessages;)
      {
         ClientMessage message = consumer1.receive(TIMEOUT);

         assertNotNull(message);

         assertEquals((Integer)i, (Integer)message.getObjectProperty(propKey));

         message.acknowledge();

         i++;

         if (i == numMessages)
         {
            break;
         }

         message = consumer2.receive(TIMEOUT);

         assertNotNull(message);

         assertEquals((Integer)i, (Integer)message.getObjectProperty(propKey));

         message.acknowledge();

         i++;

         if (i == numMessages)
         {
            break;
         }

         message = consumer3.receive(TIMEOUT);

         assertNotNull(message);

         assertEquals((Integer)i, (Integer)message.getObjectProperty(propKey));

         message.acknowledge();

         i++;
      }

      assertNull(consumer1.receiveImmediate());
      assertNull(consumer2.receiveImmediate());
      assertNull(consumer3.receiveImmediate());

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = consumer4.receive(TIMEOUT);

         assertNotNull(message);

         assertEquals((Integer)i, (Integer)message.getObjectProperty(propKey));

         message.acknowledge();
      }

      assertNull(consumer4.receiveImmediate());

      session.close();

      sf.close();

      messagingService.stop();
   }

   public void testDeployDivertsSameUniqueName() throws Exception
   {
      Configuration conf = createDefaultConfig();

      conf.setClustered(true);

      final String testAddress = "testAddress";

      final String forwardAddress1 = "forwardAddress1";
      final String forwardAddress2 = "forwardAddress2";
      final String forwardAddress3 = "forwardAddress3";

      DivertConfiguration divertConf1 = new DivertConfiguration("divert1",
                                                                "thename1",
                                                                testAddress,
                                                                forwardAddress1,
                                                                false,
                                                                null,
                                                                null);

      DivertConfiguration divertConf2 = new DivertConfiguration("divert1",
                                                                "thename2",
                                                                testAddress,
                                                                forwardAddress2,
                                                                false,
                                                                null,
                                                                null);

      DivertConfiguration divertConf3 = new DivertConfiguration("divert2",
                                                                "thename3",
                                                                testAddress,
                                                                forwardAddress3,
                                                                false,
                                                                null,
                                                                null);

      List<DivertConfiguration> divertConfs = new ArrayList<DivertConfiguration>();

      divertConfs.add(divertConf1);
      divertConfs.add(divertConf2);
      divertConfs.add(divertConf3);

      conf.setDivertConfigurations(divertConfs);

      HornetQServer messagingService = HornetQ.newHornetQServer(conf, false);

      messagingService.start();

      // Only the first and third should be deployed

      ClientSessionFactory sf = new ClientSessionFactoryImpl(new TransportConfiguration("org.hornetq.core.remoting.impl.invm.InVMConnectorFactory"));

      ClientSession session = sf.createSession(false, true, true);

      final SimpleString queueName1 = new SimpleString("queue1");

      final SimpleString queueName2 = new SimpleString("queue2");

      final SimpleString queueName3 = new SimpleString("queue3");

      final SimpleString queueName4 = new SimpleString("queue4");

      session.createQueue(new SimpleString(forwardAddress1), queueName1, null, false);

      session.createQueue(new SimpleString(forwardAddress2), queueName2, null, false);

      session.createQueue(new SimpleString(forwardAddress3), queueName3, null, false);

      session.createQueue(new SimpleString(testAddress), queueName4, null, false);

      session.start();

      ClientProducer producer = session.createProducer(new SimpleString(testAddress));

      ClientConsumer consumer1 = session.createConsumer(queueName1);

      ClientConsumer consumer2 = session.createConsumer(queueName2);

      ClientConsumer consumer3 = session.createConsumer(queueName3);

      ClientConsumer consumer4 = session.createConsumer(queueName4);

      final int numMessages = 10;

      final SimpleString propKey = new SimpleString("testkey");

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = session.createClientMessage(false);

         message.putIntProperty(propKey, i);

         producer.send(message);
      }

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = consumer1.receive(TIMEOUT);

         assertNotNull(message);

         assertEquals(i, message.getIntProperty(propKey).intValue());

         message.acknowledge();
      }

      assertNull(consumer1.receiveImmediate());

      assertNull(consumer2.receiveImmediate());

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = consumer3.receive(TIMEOUT);

         assertNotNull(message);

         assertEquals(i, message.getIntProperty(propKey).intValue());

         message.acknowledge();
      }

      assertNull(consumer3.receiveImmediate());

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = consumer4.receive(TIMEOUT);

         assertNotNull(message);

         assertEquals(i, message.getIntProperty(propKey).intValue());

         message.acknowledge();
      }

      assertNull(consumer4.receiveImmediate());

      session.close();

      sf.close();

      messagingService.stop();
   }

}
