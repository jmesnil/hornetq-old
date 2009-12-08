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

package org.hornetq.core.management;

/**
 * A BridgeControlMBean
 *
 * @author <a href="jmesnil@redhat.com">Jeff Mesnil</a>
 *
 */
public interface BridgeControl extends HornetQComponentControl
{
   String getName();

   String getQueueName();

   String getForwardingAddress();

   String getFilterString();

   String getTransformerClassName();

   String[] getConnectorPair() throws Exception;

   String getDiscoveryGroupName();

   long getRetryInterval();

   double getRetryIntervalMultiplier();

   int getReconnectAttempts();

   boolean isFailoverOnServerShutdown();

   boolean isUseDuplicateDetection();
}
