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

package org.hornetq.tests.integration.cluster.distribution;

import org.hornetq.api.core.client.ClientConsumer;
import org.hornetq.api.core.client.ClientMessage;
import org.hornetq.api.core.client.ClientProducer;
import org.hornetq.api.core.client.ClientSession;
import org.hornetq.api.core.client.ClientSessionFactory;
import org.hornetq.core.logging.Logger;
import org.hornetq.core.message.impl.MessageImpl;

public class ClusterHeadersRemovedTest extends ClusterTestBase
{
   private static final Logger log = Logger.getLogger(ClusterHeadersRemovedTest.class);

   @Override
   protected void setUp() throws Exception
   {
      super.setUp();

      setupServer(0, isFileStorage(), isNetty());
      setupServer(1, isFileStorage(), isNetty());
   }

   @Override
   protected void tearDown() throws Exception
   {
      closeAllConsumers();

      closeAllSessionFactories();

      closeAllServerLocatorsFactories();

      stopServers(0, 1);

      super.tearDown();
   }

   protected boolean isNetty()
   {
      return false;
   }
   
   public void testHeadersRemoved() throws Exception
   {
      setupClusterConnection("cluster1", 0, 1, "queues", false, 1, isNetty(), false);
      setupClusterConnection("clusterX", 1, -1, "queues", false, 1, isNetty(), false);
      startServers(1, 0);

      setupSessionFactory(0, isNetty());
      setupSessionFactory(1, isNetty());

      createQueue(0, "queues.testaddress", "queue0", null, false);
      createQueue(1, "queues.testaddress", "queue0", null, false);
      
      addConsumer(1, 1, "queue0", null);
      
      waitForBindings(0, "queues.testaddress", 1, 0, true);
      waitForBindings(0, "queues.testaddress", 1, 1, false);
      
      waitForBindings(1, "queues.testaddress", 1, 1, true);
      
      ClientSessionFactory sf = sfs[0];

      ClientSession session0 = sf.createSession(false, true, true);

      try
      {
         ClientProducer producer = session0.createProducer("queues.testaddress");

         for (int i = 0; i < 10; i++)
         {
            ClientMessage message = session0.createMessage(true);

            producer.send(message);
         }
      }
      finally
      {
         session0.close();
      }
      
      ClientConsumer consumer = super.getConsumer(1);
      
      for (int i = 0; i < 10; i++)
      {
         ClientMessage message = consumer.receive(5000);
         
         assertNotNull(message);
         
         assertFalse(message.containsProperty(MessageImpl.HDR_ROUTE_TO_IDS));
      }
   }

}
