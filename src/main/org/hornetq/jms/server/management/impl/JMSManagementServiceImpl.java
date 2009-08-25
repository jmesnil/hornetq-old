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

package org.hornetq.jms.server.management.impl;


import java.util.List;

import javax.management.ObjectName;

import org.hornetq.core.management.AddressControl;
import org.hornetq.core.management.ManagementService;
import org.hornetq.core.management.ObjectNames;
import org.hornetq.core.management.QueueControl;
import org.hornetq.core.management.ResourceNames;
import org.hornetq.core.messagecounter.MessageCounter;
import org.hornetq.core.messagecounter.MessageCounterManager;
import org.hornetq.jms.HornetQQueue;
import org.hornetq.jms.HornetQTopic;
import org.hornetq.jms.client.HornetQConnectionFactory;
import org.hornetq.jms.server.JMSServerManager;
import org.hornetq.jms.server.management.JMSManagementService;
import org.hornetq.jms.server.management.JMSServerControl;
import org.hornetq.jms.server.management.jmx.impl.ReplicationAwareConnectionFactoryControlWrapper;
import org.hornetq.jms.server.management.jmx.impl.ReplicationAwareJMSQueueControlWrapper;
import org.hornetq.jms.server.management.jmx.impl.ReplicationAwareJMSServerControlWrapper;
import org.hornetq.jms.server.management.jmx.impl.ReplicationAwareTopicControlWrapper;

/*
 * @author <a href="mailto:jmesnil@redhat.com">Jeff Mesnil</a>
 * 
 * @version <tt>$Revision$</tt>
 */
public class JMSManagementServiceImpl implements JMSManagementService
{

   // Constants -----------------------------------------------------

   // Attributes ----------------------------------------------------

   private final ManagementService managementService;

   // Static --------------------------------------------------------

   public JMSManagementServiceImpl(final ManagementService managementService)
   {
      this.managementService = managementService;
   }

   // Public --------------------------------------------------------

   // JMSManagementRegistration implementation ----------------------

   public synchronized JMSServerControl registerJMSServer(final JMSServerManager server) throws Exception
   {
      ObjectName objectName = ObjectNames.getJMSServerObjectName();
      JMSServerControlImpl control = new JMSServerControlImpl(server);
      JMSServerControl replicatingProxy = new ReplicationAwareJMSServerControlWrapper(control, 
                                                                                      managementService.getReplicationOperationInvoker());
      managementService.registerInJMX(objectName,
                                      replicatingProxy);
      managementService.registerInRegistry(ResourceNames.JMS_SERVER, control);
      return replicatingProxy;
   }

   public synchronized void unregisterJMSServer() throws Exception
   {
      ObjectName objectName = ObjectNames.getJMSServerObjectName();
      managementService.unregisterFromJMX(objectName);
      managementService.unregisterFromRegistry(ResourceNames.JMS_SERVER);
   }

   public synchronized void registerQueue(final HornetQQueue queue,
                             final String jndiBinding) throws Exception
   {
      QueueControl coreQueueControl = (QueueControl)managementService.getResource(ResourceNames.CORE_QUEUE + queue.getAddress());
      MessageCounterManager messageCounterManager = managementService.getMessageCounterManager();
      MessageCounter counter = new MessageCounter(queue.getName(),
                                                  null,
                                                  coreQueueControl,
                                                  false,
                                                  coreQueueControl.isDurable(),
                                                  messageCounterManager.getMaxDayCount());
      messageCounterManager.registerMessageCounter(queue.getName(), counter);
      ObjectName objectName = ObjectNames.getJMSQueueObjectName(queue.getQueueName());
      JMSQueueControlImpl control = new JMSQueueControlImpl(queue,
                                                    coreQueueControl,
                                                    jndiBinding,
                                                    counter);
      managementService.registerInJMX(objectName,
                                      new ReplicationAwareJMSQueueControlWrapper(control, 
                                                                                 managementService.getReplicationOperationInvoker()));
      managementService.registerInRegistry(ResourceNames.JMS_QUEUE + queue.getQueueName(), control);
   }

   public synchronized void unregisterQueue(final String name) throws Exception
   {
      ObjectName objectName = ObjectNames.getJMSQueueObjectName(name);
      managementService.unregisterFromJMX(objectName);
      managementService.unregisterFromRegistry(ResourceNames.JMS_QUEUE + name);
   }

   public synchronized void registerTopic(final HornetQTopic topic,
                             final String jndiBinding) throws Exception
   {
      ObjectName objectName = ObjectNames.getJMSTopicObjectName(topic.getTopicName());
      AddressControl addressControl = (AddressControl)managementService.getResource(ResourceNames.CORE_ADDRESS + topic.getAddress());
      TopicControlImpl control = new TopicControlImpl(topic, addressControl, jndiBinding, managementService);
      managementService.registerInJMX(objectName, new ReplicationAwareTopicControlWrapper(control,
                                                                                          managementService.getReplicationOperationInvoker()));
      managementService.registerInRegistry(ResourceNames.JMS_TOPIC + topic.getTopicName(), control);
   }

   public synchronized void unregisterTopic(final String name) throws Exception
   {
      ObjectName objectName = ObjectNames.getJMSTopicObjectName(name);
      managementService.unregisterFromJMX(objectName);
      managementService.unregisterFromRegistry(ResourceNames.JMS_TOPIC + name);
   }

   public synchronized void registerConnectionFactory(final String name,
                                         final HornetQConnectionFactory connectionFactory,
                                         final List<String> bindings) throws Exception
   {
      ObjectName objectName = ObjectNames.getConnectionFactoryObjectName(name);
      ConnectionFactoryControlImpl control = new ConnectionFactoryControlImpl(connectionFactory, name, bindings);
      managementService.registerInJMX(objectName,
                                      new ReplicationAwareConnectionFactoryControlWrapper(control,
                                                                                          managementService.getReplicationOperationInvoker()));
      managementService.registerInRegistry(ResourceNames.JMS_CONNECTION_FACTORY + name, control);
   }

   public synchronized void unregisterConnectionFactory(final String name) throws Exception
   {
      ObjectName objectName = ObjectNames.getConnectionFactoryObjectName(name);
      managementService.unregisterFromJMX(objectName);
      managementService.unregisterFromRegistry(ResourceNames.JMS_CONNECTION_FACTORY + name);
   }

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------
}