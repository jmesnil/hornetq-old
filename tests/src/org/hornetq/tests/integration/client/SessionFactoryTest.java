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
package org.hornetq.tests.integration.client;

import static org.hornetq.tests.util.RandomUtil.randomString;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hornetq.core.client.ClientSession;
import org.hornetq.core.client.ClientSessionFactory;
import org.hornetq.core.client.impl.ClientSessionFactoryImpl;
import org.hornetq.core.config.Configuration;
import org.hornetq.core.config.TransportConfiguration;
import org.hornetq.core.config.cluster.BroadcastGroupConfiguration;
import org.hornetq.core.config.impl.ConfigurationImpl;
import org.hornetq.core.exception.HornetQException;
import org.hornetq.core.logging.Logger;
import org.hornetq.core.remoting.impl.invm.TransportConstants;
import org.hornetq.core.server.HornetQ;
import org.hornetq.core.server.HornetQServer;
import org.hornetq.tests.util.RandomUtil;
import org.hornetq.tests.util.ServiceTestBase;
import org.hornetq.utils.Pair;

/**
 * 
 * A ClientSessionFactoryTest
 *
 * @author <a href="mailto:andy.taylor@jboss.org">Andy Taylor</a>
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 *
 *
 */
public class SessionFactoryTest extends ServiceTestBase
{
   private static final Logger log = Logger.getLogger(SessionFactoryTest.class);

   private final String groupAddress = "230.1.2.3";

   private final int groupPort = 8765;

   private HornetQServer liveService;

   private HornetQServer backupService;

   private TransportConfiguration liveTC;

   private TransportConfiguration backupTC;
   
   protected void tearDown() throws Exception
   {      
      if (liveService != null && liveService.isStarted())
      {         
         liveService.stop();
      }     
      if (backupService != null && backupService.isStarted())
      {         
         backupService.stop();
      }
      liveService = null;
      backupService = null;
      liveTC = null;
      backupTC = null;
      
      super.tearDown();
   }
   
   public void testSerializable() throws Exception
   {
      ClientSessionFactory cf = new ClientSessionFactoryImpl();
      
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      
      ObjectOutputStream oos = new ObjectOutputStream(baos);
      
      oos.writeObject(cf);
      
      oos.close();
      
      byte[] bytes = baos.toByteArray();
      
      ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
      
      ObjectInputStream ois = new ObjectInputStream(bais);
      
      ClientSessionFactoryImpl csi = (ClientSessionFactoryImpl)ois.readObject();
      
      assertNotNull(csi);
   }

   public void testDefaultConstructor() throws Exception
   {
      try
      {
         startLiveAndBackup();         
         ClientSessionFactory cf = new ClientSessionFactoryImpl();
         assertFactoryParams(cf,
                             null,
                             null,
                             0,
                             ClientSessionFactoryImpl.DEFAULT_DISCOVERY_REFRESH_TIMEOUT,
                             ClientSessionFactoryImpl.DEFAULT_CLIENT_FAILURE_CHECK_PERIOD,
                             ClientSessionFactoryImpl.DEFAULT_CONNECTION_TTL,
                             ClientSessionFactoryImpl.DEFAULT_CALL_TIMEOUT,                         
                             ClientSessionFactoryImpl.DEFAULT_MIN_LARGE_MESSAGE_SIZE,
                             ClientSessionFactoryImpl.DEFAULT_CONSUMER_WINDOW_SIZE,
                             ClientSessionFactoryImpl.DEFAULT_CONSUMER_MAX_RATE,
                             ClientSessionFactoryImpl.DEFAULT_CONFIRMATION_WINDOW_SIZE,
                             ClientSessionFactoryImpl.DEFAULT_PRODUCER_MAX_RATE,
                             ClientSessionFactoryImpl.DEFAULT_BLOCK_ON_ACKNOWLEDGE,
                             ClientSessionFactoryImpl.DEFAULT_BLOCK_ON_PERSISTENT_SEND,
                             ClientSessionFactoryImpl.DEFAULT_BLOCK_ON_NON_PERSISTENT_SEND,
                             ClientSessionFactoryImpl.DEFAULT_AUTO_GROUP,
                             ClientSessionFactoryImpl.DEFAULT_PRE_ACKNOWLEDGE,
                             ClientSessionFactoryImpl.DEFAULT_CONNECTION_LOAD_BALANCING_POLICY_CLASS_NAME,
                             ClientSessionFactoryImpl.DEFAULT_ACK_BATCH_SIZE,
                             ClientSessionFactoryImpl.DEFAULT_DISCOVERY_INITIAL_WAIT_TIMEOUT,
                             ClientSessionFactoryImpl.DEFAULT_USE_GLOBAL_POOLS,
                             ClientSessionFactoryImpl.DEFAULT_SCHEDULED_THREAD_POOL_MAX_SIZE,
                             ClientSessionFactoryImpl.DEFAULT_THREAD_POOL_MAX_SIZE,
                             ClientSessionFactoryImpl.DEFAULT_RETRY_INTERVAL,
                             ClientSessionFactoryImpl.DEFAULT_RETRY_INTERVAL_MULTIPLIER,
                             ClientSessionFactoryImpl.DEFAULT_RECONNECT_ATTEMPTS,
                             ClientSessionFactoryImpl.DEFAULT_FAILOVER_ON_SERVER_SHUTDOWN);         
         try
         {
            ClientSession session = cf.createSession(false, true, true);
            fail("Should throw exception");
         }
         catch (HornetQException e)
         {
            e.printStackTrace();
            // Ok
         }    
         final List<Pair<TransportConfiguration, TransportConfiguration>> staticConnectors = new ArrayList<Pair<TransportConfiguration, TransportConfiguration>>();
         Pair<TransportConfiguration, TransportConfiguration> pair0 = new Pair<TransportConfiguration, TransportConfiguration>(this.liveTC,
                                                                                                                               this.backupTC);
         staticConnectors.add(pair0);
         cf.setStaticConnectors(staticConnectors);
         ClientSession session = cf.createSession(false, true, true);
         assertNotNull(session);
         session.close();
         testSettersThrowException(cf);
      }
      finally
      {
         stopLiveAndBackup();
      }
   }

   public void testDiscoveryConstructor() throws Exception
   {
      try
      {
         startLiveAndBackup();
         ClientSessionFactory cf = new ClientSessionFactoryImpl(groupAddress, groupPort);
         assertFactoryParams(cf,
                             null,
                             groupAddress,
                             groupPort,
                             ClientSessionFactoryImpl.DEFAULT_DISCOVERY_REFRESH_TIMEOUT,
                             ClientSessionFactoryImpl.DEFAULT_CLIENT_FAILURE_CHECK_PERIOD,
                             ClientSessionFactoryImpl.DEFAULT_CONNECTION_TTL,
                             ClientSessionFactoryImpl.DEFAULT_CALL_TIMEOUT,                             
                             ClientSessionFactoryImpl.DEFAULT_MIN_LARGE_MESSAGE_SIZE,
                             ClientSessionFactoryImpl.DEFAULT_CONSUMER_WINDOW_SIZE,
                             ClientSessionFactoryImpl.DEFAULT_CONSUMER_MAX_RATE,
                             ClientSessionFactoryImpl.DEFAULT_CONFIRMATION_WINDOW_SIZE,
                             ClientSessionFactoryImpl.DEFAULT_PRODUCER_MAX_RATE,
                             ClientSessionFactoryImpl.DEFAULT_BLOCK_ON_ACKNOWLEDGE,
                             ClientSessionFactoryImpl.DEFAULT_BLOCK_ON_PERSISTENT_SEND,
                             ClientSessionFactoryImpl.DEFAULT_BLOCK_ON_NON_PERSISTENT_SEND,
                             ClientSessionFactoryImpl.DEFAULT_AUTO_GROUP,
                             ClientSessionFactoryImpl.DEFAULT_PRE_ACKNOWLEDGE,
                             ClientSessionFactoryImpl.DEFAULT_CONNECTION_LOAD_BALANCING_POLICY_CLASS_NAME,
                             ClientSessionFactoryImpl.DEFAULT_ACK_BATCH_SIZE,
                             ClientSessionFactoryImpl.DEFAULT_DISCOVERY_INITIAL_WAIT_TIMEOUT,
                             ClientSessionFactoryImpl.DEFAULT_USE_GLOBAL_POOLS,
                             ClientSessionFactoryImpl.DEFAULT_SCHEDULED_THREAD_POOL_MAX_SIZE,
                             ClientSessionFactoryImpl.DEFAULT_THREAD_POOL_MAX_SIZE,
                             ClientSessionFactoryImpl.DEFAULT_RETRY_INTERVAL,
                             ClientSessionFactoryImpl.DEFAULT_RETRY_INTERVAL_MULTIPLIER,
                             ClientSessionFactoryImpl.DEFAULT_RECONNECT_ATTEMPTS,
                             ClientSessionFactoryImpl.DEFAULT_FAILOVER_ON_SERVER_SHUTDOWN);
         ClientSession session = cf.createSession(false, true, true);
         assertNotNull(session);
         session.close();
         testSettersThrowException(cf);
      }
      finally
      {
         stopLiveAndBackup();
      }
   }

   public void testStaticConnectorListConstructor() throws Exception
   {
      try
      {
         startLiveAndBackup();
         final List<Pair<TransportConfiguration, TransportConfiguration>> staticConnectors = new ArrayList<Pair<TransportConfiguration, TransportConfiguration>>();
         Pair<TransportConfiguration, TransportConfiguration> pair0 = new Pair<TransportConfiguration, TransportConfiguration>(this.liveTC,
                                                                                                                               this.backupTC);
         staticConnectors.add(pair0);

         ClientSessionFactory cf = new ClientSessionFactoryImpl(staticConnectors);
         assertFactoryParams(cf,
                             staticConnectors,
                             null,
                             0,
                             ClientSessionFactoryImpl.DEFAULT_DISCOVERY_REFRESH_TIMEOUT,
                             ClientSessionFactoryImpl.DEFAULT_CLIENT_FAILURE_CHECK_PERIOD,
                             ClientSessionFactoryImpl.DEFAULT_CONNECTION_TTL,
                             ClientSessionFactoryImpl.DEFAULT_CALL_TIMEOUT,                             
                             ClientSessionFactoryImpl.DEFAULT_MIN_LARGE_MESSAGE_SIZE,
                             ClientSessionFactoryImpl.DEFAULT_CONSUMER_WINDOW_SIZE,
                             ClientSessionFactoryImpl.DEFAULT_CONSUMER_MAX_RATE,
                             ClientSessionFactoryImpl.DEFAULT_CONFIRMATION_WINDOW_SIZE,
                             ClientSessionFactoryImpl.DEFAULT_PRODUCER_MAX_RATE,
                             ClientSessionFactoryImpl.DEFAULT_BLOCK_ON_ACKNOWLEDGE,
                             ClientSessionFactoryImpl.DEFAULT_BLOCK_ON_PERSISTENT_SEND,
                             ClientSessionFactoryImpl.DEFAULT_BLOCK_ON_NON_PERSISTENT_SEND,
                             ClientSessionFactoryImpl.DEFAULT_AUTO_GROUP,
                             ClientSessionFactoryImpl.DEFAULT_PRE_ACKNOWLEDGE,
                             ClientSessionFactoryImpl.DEFAULT_CONNECTION_LOAD_BALANCING_POLICY_CLASS_NAME,
                             ClientSessionFactoryImpl.DEFAULT_ACK_BATCH_SIZE,
                             ClientSessionFactoryImpl.DEFAULT_DISCOVERY_INITIAL_WAIT_TIMEOUT,
                             ClientSessionFactoryImpl.DEFAULT_USE_GLOBAL_POOLS,
                             ClientSessionFactoryImpl.DEFAULT_SCHEDULED_THREAD_POOL_MAX_SIZE,
                             ClientSessionFactoryImpl.DEFAULT_THREAD_POOL_MAX_SIZE,
                             ClientSessionFactoryImpl.DEFAULT_RETRY_INTERVAL,
                             ClientSessionFactoryImpl.DEFAULT_RETRY_INTERVAL_MULTIPLIER,
                             ClientSessionFactoryImpl.DEFAULT_RECONNECT_ATTEMPTS,
                             ClientSessionFactoryImpl.DEFAULT_FAILOVER_ON_SERVER_SHUTDOWN);
         ClientSession session = cf.createSession(false, true, true);
         assertNotNull(session);
         session.close();
         testSettersThrowException(cf);
      }
      finally
      {
         stopLiveAndBackup();
      }
   }

   public void testStaticConnectorLiveAndBackupConstructor() throws Exception
   {
      try
      {
         startLiveAndBackup();
         final List<Pair<TransportConfiguration, TransportConfiguration>> staticConnectors = new ArrayList<Pair<TransportConfiguration, TransportConfiguration>>();
         Pair<TransportConfiguration, TransportConfiguration> pair0 = new Pair<TransportConfiguration, TransportConfiguration>(this.liveTC,
                                                                                                                               this.backupTC);
         staticConnectors.add(pair0);

         ClientSessionFactory cf = new ClientSessionFactoryImpl(this.liveTC, this.backupTC);
         assertFactoryParams(cf,
                             staticConnectors,
                             null,
                             0,
                             ClientSessionFactoryImpl.DEFAULT_DISCOVERY_REFRESH_TIMEOUT,
                             ClientSessionFactoryImpl.DEFAULT_CLIENT_FAILURE_CHECK_PERIOD,
                             ClientSessionFactoryImpl.DEFAULT_CONNECTION_TTL,
                             ClientSessionFactoryImpl.DEFAULT_CALL_TIMEOUT,                             
                             ClientSessionFactoryImpl.DEFAULT_MIN_LARGE_MESSAGE_SIZE,
                             ClientSessionFactoryImpl.DEFAULT_CONSUMER_WINDOW_SIZE,
                             ClientSessionFactoryImpl.DEFAULT_CONSUMER_MAX_RATE,
                             ClientSessionFactoryImpl.DEFAULT_CONFIRMATION_WINDOW_SIZE,
                             ClientSessionFactoryImpl.DEFAULT_PRODUCER_MAX_RATE,
                             ClientSessionFactoryImpl.DEFAULT_BLOCK_ON_ACKNOWLEDGE,
                             ClientSessionFactoryImpl.DEFAULT_BLOCK_ON_PERSISTENT_SEND,
                             ClientSessionFactoryImpl.DEFAULT_BLOCK_ON_NON_PERSISTENT_SEND,
                             ClientSessionFactoryImpl.DEFAULT_AUTO_GROUP,
                             ClientSessionFactoryImpl.DEFAULT_PRE_ACKNOWLEDGE,
                             ClientSessionFactoryImpl.DEFAULT_CONNECTION_LOAD_BALANCING_POLICY_CLASS_NAME,
                             ClientSessionFactoryImpl.DEFAULT_ACK_BATCH_SIZE,
                             ClientSessionFactoryImpl.DEFAULT_DISCOVERY_INITIAL_WAIT_TIMEOUT,
                             ClientSessionFactoryImpl.DEFAULT_USE_GLOBAL_POOLS,
                             ClientSessionFactoryImpl.DEFAULT_SCHEDULED_THREAD_POOL_MAX_SIZE,
                             ClientSessionFactoryImpl.DEFAULT_THREAD_POOL_MAX_SIZE,
                             ClientSessionFactoryImpl.DEFAULT_RETRY_INTERVAL,
                             ClientSessionFactoryImpl.DEFAULT_RETRY_INTERVAL_MULTIPLIER,
                             ClientSessionFactoryImpl.DEFAULT_RECONNECT_ATTEMPTS,
                             ClientSessionFactoryImpl.DEFAULT_FAILOVER_ON_SERVER_SHUTDOWN);
         ClientSession session = cf.createSession(false, true, true);
         assertNotNull(session);
         session.close();
         testSettersThrowException(cf);
      }
      finally
      {
         stopLiveAndBackup();
      }
   }

   public void testStaticConnectorLiveConstructor() throws Exception
   {
      try
      {
         startLiveAndBackup();
         final List<Pair<TransportConfiguration, TransportConfiguration>> staticConnectors = new ArrayList<Pair<TransportConfiguration, TransportConfiguration>>();
         Pair<TransportConfiguration, TransportConfiguration> pair0 = new Pair<TransportConfiguration, TransportConfiguration>(this.liveTC,
                                                                                                                               null);
         staticConnectors.add(pair0);

         ClientSessionFactory cf = new ClientSessionFactoryImpl(this.liveTC);
         assertFactoryParams(cf,
                             staticConnectors,
                             null,
                             0,
                             ClientSessionFactoryImpl.DEFAULT_DISCOVERY_REFRESH_TIMEOUT,
                             ClientSessionFactoryImpl.DEFAULT_CLIENT_FAILURE_CHECK_PERIOD,
                             ClientSessionFactoryImpl.DEFAULT_CONNECTION_TTL,
                             ClientSessionFactoryImpl.DEFAULT_CALL_TIMEOUT,                             
                             ClientSessionFactoryImpl.DEFAULT_MIN_LARGE_MESSAGE_SIZE,
                             ClientSessionFactoryImpl.DEFAULT_CONSUMER_WINDOW_SIZE,
                             ClientSessionFactoryImpl.DEFAULT_CONSUMER_MAX_RATE,
                             ClientSessionFactoryImpl.DEFAULT_CONFIRMATION_WINDOW_SIZE,
                             ClientSessionFactoryImpl.DEFAULT_PRODUCER_MAX_RATE,
                             ClientSessionFactoryImpl.DEFAULT_BLOCK_ON_ACKNOWLEDGE,
                             ClientSessionFactoryImpl.DEFAULT_BLOCK_ON_PERSISTENT_SEND,
                             ClientSessionFactoryImpl.DEFAULT_BLOCK_ON_NON_PERSISTENT_SEND,
                             ClientSessionFactoryImpl.DEFAULT_AUTO_GROUP,
                             ClientSessionFactoryImpl.DEFAULT_PRE_ACKNOWLEDGE,
                             ClientSessionFactoryImpl.DEFAULT_CONNECTION_LOAD_BALANCING_POLICY_CLASS_NAME,
                             ClientSessionFactoryImpl.DEFAULT_ACK_BATCH_SIZE,
                             ClientSessionFactoryImpl.DEFAULT_DISCOVERY_INITIAL_WAIT_TIMEOUT,
                             ClientSessionFactoryImpl.DEFAULT_USE_GLOBAL_POOLS,
                             ClientSessionFactoryImpl.DEFAULT_SCHEDULED_THREAD_POOL_MAX_SIZE,
                             ClientSessionFactoryImpl.DEFAULT_THREAD_POOL_MAX_SIZE,
                             ClientSessionFactoryImpl.DEFAULT_RETRY_INTERVAL,
                             ClientSessionFactoryImpl.DEFAULT_RETRY_INTERVAL_MULTIPLIER,
                             ClientSessionFactoryImpl.DEFAULT_RECONNECT_ATTEMPTS,
                             ClientSessionFactoryImpl.DEFAULT_FAILOVER_ON_SERVER_SHUTDOWN);
         ClientSession session = cf.createSession(false, true, true);
         assertNotNull(session);
         session.close();
         testSettersThrowException(cf);
      }
      finally
      {
         stopLiveAndBackup();
      }
   }

   public void testGettersAndSetters()
   {
      ClientSessionFactory cf = new ClientSessionFactoryImpl();

      List<Pair<TransportConfiguration, TransportConfiguration>> staticConnectors = new ArrayList<Pair<TransportConfiguration, TransportConfiguration>>();
      Pair<TransportConfiguration, TransportConfiguration> pair0 = new Pair<TransportConfiguration, TransportConfiguration>(this.liveTC,
                                                                                                                            this.backupTC);
      staticConnectors.add(pair0);

      String discoveryAddress = randomString();
      int discoveryPort = RandomUtil.randomPositiveInt();
      long discoveryRefreshTimeout = RandomUtil.randomPositiveLong();
      long clientFailureCheckPeriod = RandomUtil.randomPositiveLong();
      long connectionTTL = RandomUtil.randomPositiveLong();
      long callTimeout = RandomUtil.randomPositiveLong();
      int minLargeMessageSize = RandomUtil.randomPositiveInt();
      int consumerWindowSize = RandomUtil.randomPositiveInt();
      int consumerMaxRate = RandomUtil.randomPositiveInt();
      int confirmationWindowSize = RandomUtil.randomPositiveInt();
      int producerMaxRate = RandomUtil.randomPositiveInt();
      boolean blockOnAcknowledge = RandomUtil.randomBoolean();
      boolean blockOnPersistentSend = RandomUtil.randomBoolean();
      boolean blockOnNonPersistentSend = RandomUtil.randomBoolean();
      boolean autoGroup = RandomUtil.randomBoolean();
      boolean preAcknowledge = RandomUtil.randomBoolean();
      String loadBalancingPolicyClassName = RandomUtil.randomString();
      int ackBatchSize = RandomUtil.randomPositiveInt();
      long initialWaitTimeout = RandomUtil.randomPositiveLong();
      boolean useGlobalPools = RandomUtil.randomBoolean();
      int scheduledThreadPoolMaxSize = RandomUtil.randomPositiveInt();
      int threadPoolMaxSize = RandomUtil.randomPositiveInt();
      long retryInterval = RandomUtil.randomPositiveLong();
      double retryIntervalMultiplier = RandomUtil.randomDouble();
      int reconnectAttempts = RandomUtil.randomPositiveInt();
      boolean failoverOnServerShutdown = RandomUtil.randomBoolean();

      cf.setStaticConnectors(staticConnectors);
      cf.setDiscoveryAddress(discoveryAddress);
      cf.setDiscoveryPort(discoveryPort);
      cf.setDiscoveryRefreshTimeout(discoveryRefreshTimeout);
      cf.setClientFailureCheckPeriod(clientFailureCheckPeriod);
      cf.setConnectionTTL(connectionTTL);
      cf.setCallTimeout(callTimeout);
      cf.setMinLargeMessageSize(minLargeMessageSize);
      cf.setConsumerWindowSize(consumerWindowSize);
      cf.setConsumerMaxRate(consumerMaxRate);
      cf.setConfirmationWindowSize(confirmationWindowSize);
      cf.setProducerMaxRate(producerMaxRate);
      cf.setBlockOnAcknowledge(blockOnAcknowledge);
      cf.setBlockOnPersistentSend(blockOnPersistentSend);
      cf.setBlockOnNonPersistentSend(blockOnNonPersistentSend);
      cf.setAutoGroup(autoGroup);
      cf.setPreAcknowledge(preAcknowledge);
      cf.setConnectionLoadBalancingPolicyClassName(loadBalancingPolicyClassName);
      cf.setAckBatchSize(ackBatchSize);
      cf.setDiscoveryInitialWaitTimeout(initialWaitTimeout);
      cf.setUseGlobalPools(useGlobalPools);
      cf.setScheduledThreadPoolMaxSize(scheduledThreadPoolMaxSize);
      cf.setThreadPoolMaxSize(threadPoolMaxSize);
      cf.setRetryInterval(retryInterval);
      cf.setRetryIntervalMultiplier(retryIntervalMultiplier);
      cf.setReconnectAttempts(reconnectAttempts);
      cf.setFailoverOnServerShutdown(failoverOnServerShutdown);

      assertEquals(staticConnectors, cf.getStaticConnectors());
      assertEquals(discoveryAddress, cf.getDiscoveryAddress());
      assertEquals(discoveryPort, cf.getDiscoveryPort());
      assertEquals(discoveryRefreshTimeout, cf.getDiscoveryRefreshTimeout());
      assertEquals(clientFailureCheckPeriod, cf.getClientFailureCheckPeriod());
      assertEquals(connectionTTL, cf.getConnectionTTL());
      assertEquals(callTimeout, cf.getCallTimeout());      
      assertEquals(minLargeMessageSize, cf.getMinLargeMessageSize());
      assertEquals(consumerWindowSize, cf.getConsumerWindowSize());
      assertEquals(consumerMaxRate, cf.getConsumerMaxRate());
      assertEquals(confirmationWindowSize, cf.getConfirmationWindowSize());
      assertEquals(producerMaxRate, cf.getProducerMaxRate());
      assertEquals(blockOnAcknowledge, cf.isBlockOnAcknowledge());
      assertEquals(blockOnPersistentSend, cf.isBlockOnPersistentSend());
      assertEquals(blockOnNonPersistentSend, cf.isBlockOnNonPersistentSend());
      assertEquals(autoGroup, cf.isAutoGroup());
      assertEquals(preAcknowledge, cf.isPreAcknowledge());
      assertEquals(loadBalancingPolicyClassName, cf.getConnectionLoadBalancingPolicyClassName());
      assertEquals(ackBatchSize, cf.getAckBatchSize());
      assertEquals(initialWaitTimeout, cf.getDiscoveryInitialWaitTimeout());
      assertEquals(useGlobalPools, cf.isUseGlobalPools());
      assertEquals(scheduledThreadPoolMaxSize, cf.getScheduledThreadPoolMaxSize());
      assertEquals(threadPoolMaxSize, cf.getThreadPoolMaxSize());
      assertEquals(retryInterval, cf.getRetryInterval());
      assertEquals(retryIntervalMultiplier, cf.getRetryIntervalMultiplier());
      assertEquals(reconnectAttempts, cf.getReconnectAttempts());
      assertEquals(failoverOnServerShutdown, cf.isFailoverOnServerShutdown());

   }

   private void testSettersThrowException(ClientSessionFactory cf)
   {
      List<Pair<TransportConfiguration, TransportConfiguration>> staticConnectors = new ArrayList<Pair<TransportConfiguration, TransportConfiguration>>();
      Pair<TransportConfiguration, TransportConfiguration> pair0 = new Pair<TransportConfiguration, TransportConfiguration>(this.liveTC,
                                                                                                                            this.backupTC);
      staticConnectors.add(pair0);

      String discoveryAddress = randomString();
      int discoveryPort = RandomUtil.randomPositiveInt();
      long discoveryRefreshTimeout = RandomUtil.randomPositiveLong();
      long clientFailureCheckPeriod = RandomUtil.randomPositiveLong();
      long connectionTTL = RandomUtil.randomPositiveLong();
      long callTimeout = RandomUtil.randomPositiveLong();      
      int minLargeMessageSize = RandomUtil.randomPositiveInt();
      int consumerWindowSize = RandomUtil.randomPositiveInt();
      int consumerMaxRate = RandomUtil.randomPositiveInt();
      int confirmationWindowSize = RandomUtil.randomPositiveInt();
      int producerMaxRate = RandomUtil.randomPositiveInt();
      boolean blockOnAcknowledge = RandomUtil.randomBoolean();
      boolean blockOnPersistentSend = RandomUtil.randomBoolean();
      boolean blockOnNonPersistentSend = RandomUtil.randomBoolean();
      boolean autoGroup = RandomUtil.randomBoolean();
      boolean preAcknowledge = RandomUtil.randomBoolean();
      String loadBalancingPolicyClassName = RandomUtil.randomString();
      int ackBatchSize = RandomUtil.randomPositiveInt();
      long initialWaitTimeout = RandomUtil.randomPositiveLong();
      boolean useGlobalPools = RandomUtil.randomBoolean();
      int scheduledThreadPoolMaxSize = RandomUtil.randomPositiveInt();
      int threadPoolMaxSize = RandomUtil.randomPositiveInt();
      long retryInterval = RandomUtil.randomPositiveLong();
      double retryIntervalMultiplier = RandomUtil.randomDouble();
      int reconnectAttempts = RandomUtil.randomPositiveInt();
      boolean failoverOnServerShutdown = RandomUtil.randomBoolean();

      try
      {
         cf.setStaticConnectors(staticConnectors);
         fail("Should throw exception");
      }
      catch (IllegalStateException e)
      {
         // OK
      }
      try
      {
         cf.setDiscoveryAddress(discoveryAddress);
         fail("Should throw exception");
      }
      catch (IllegalStateException e)
      {
         // OK
      }
      try
      {
         cf.setDiscoveryPort(discoveryPort);
         fail("Should throw exception");
      }
      catch (IllegalStateException e)
      {
         // OK
      }
      try
      {
         cf.setDiscoveryRefreshTimeout(discoveryRefreshTimeout);
         fail("Should throw exception");
      }
      catch (IllegalStateException e)
      {
         // OK
      }
      try
      {
         cf.setClientFailureCheckPeriod(clientFailureCheckPeriod);
         fail("Should throw exception");
      }
      catch (IllegalStateException e)
      {
         // OK
      }
      try
      {
         cf.setConnectionTTL(connectionTTL);
         fail("Should throw exception");
      }
      catch (IllegalStateException e)
      {
         // OK
      }
      try
      {
         cf.setCallTimeout(callTimeout);
         fail("Should throw exception");
      }
      catch (IllegalStateException e)
      {
         // OK
      }
      try
      {
         cf.setMinLargeMessageSize(minLargeMessageSize);
         fail("Should throw exception");
      }
      catch (IllegalStateException e)
      {
         // OK
      }
      try
      {
         cf.setConsumerWindowSize(consumerWindowSize);
         fail("Should throw exception");
      }
      catch (IllegalStateException e)
      {
         // OK
      }
      try
      {
         cf.setConsumerMaxRate(consumerMaxRate);
         fail("Should throw exception");
      }
      catch (IllegalStateException e)
      {
         // OK
      }
      try
      {
         cf.setConfirmationWindowSize(confirmationWindowSize);
         fail("Should throw exception");
      }
      catch (IllegalStateException e)
      {
         // OK
      }
      try
      {
         cf.setProducerMaxRate(producerMaxRate);
         fail("Should throw exception");
      }
      catch (IllegalStateException e)
      {
         // OK
      }
      try
      {
         cf.setBlockOnAcknowledge(blockOnAcknowledge);
         fail("Should throw exception");
      }
      catch (IllegalStateException e)
      {
         // OK
      }
      try
      {
         cf.setBlockOnPersistentSend(blockOnPersistentSend);
         fail("Should throw exception");
      }
      catch (IllegalStateException e)
      {
         // OK
      }
      try
      {
         cf.setBlockOnNonPersistentSend(blockOnNonPersistentSend);
         fail("Should throw exception");
      }
      catch (IllegalStateException e)
      {
         // OK
      }
      try
      {
         cf.setAutoGroup(autoGroup);
         fail("Should throw exception");
      }
      catch (IllegalStateException e)
      {
         // OK
      }
      try
      {
         cf.setPreAcknowledge(preAcknowledge);
         fail("Should throw exception");
      }
      catch (IllegalStateException e)
      {
         // OK
      }
      try
      {
         cf.setConnectionLoadBalancingPolicyClassName(loadBalancingPolicyClassName);
         fail("Should throw exception");
      }
      catch (IllegalStateException e)
      {
         // OK
      }
      try
      {
         cf.setAckBatchSize(ackBatchSize);
         fail("Should throw exception");
      }
      catch (IllegalStateException e)
      {
         // OK
      }
      try
      {
         cf.setDiscoveryInitialWaitTimeout(initialWaitTimeout);
         fail("Should throw exception");
      }
      catch (IllegalStateException e)
      {
         // OK
      }
      try
      {
         cf.setUseGlobalPools(useGlobalPools);
         fail("Should throw exception");
      }
      catch (IllegalStateException e)
      {
         // OK
      }
      try
      {
         cf.setScheduledThreadPoolMaxSize(scheduledThreadPoolMaxSize);
         fail("Should throw exception");
      }
      catch (IllegalStateException e)
      {
         // OK
      }
      try
      {
         cf.setThreadPoolMaxSize(threadPoolMaxSize);
         fail("Should throw exception");
      }
      catch (IllegalStateException e)
      {
         // OK
      }
      try
      {
         cf.setRetryInterval(retryInterval);
         fail("Should throw exception");
      }
      catch (IllegalStateException e)
      {
         // OK
      }
      try
      {
         cf.setRetryIntervalMultiplier(retryIntervalMultiplier);
         fail("Should throw exception");
      }
      catch (IllegalStateException e)
      {
         // OK
      }
      try
      {
         cf.setReconnectAttempts(reconnectAttempts);
         fail("Should throw exception");
      }
      catch (IllegalStateException e)
      {
         // OK
      }
      try
      {
         cf.setFailoverOnServerShutdown(failoverOnServerShutdown);
         fail("Should throw exception");
      }
      catch (IllegalStateException e)
      {
         // OK
      }

      cf.getStaticConnectors();
      cf.getDiscoveryAddress();
      cf.getDiscoveryPort();
      cf.getDiscoveryRefreshTimeout();
      cf.getClientFailureCheckPeriod();
      cf.getConnectionTTL();
      cf.getCallTimeout();
      cf.getMinLargeMessageSize();
      cf.getConsumerWindowSize();
      cf.getConsumerMaxRate();
      cf.getConfirmationWindowSize();
      cf.getProducerMaxRate();
      cf.isBlockOnAcknowledge();
      cf.isBlockOnPersistentSend();
      cf.isBlockOnNonPersistentSend();
      cf.isAutoGroup();
      cf.isPreAcknowledge();
      cf.getConnectionLoadBalancingPolicyClassName();
      cf.getAckBatchSize();
      cf.getDiscoveryInitialWaitTimeout();
      cf.isUseGlobalPools();
      cf.getScheduledThreadPoolMaxSize();
      cf.getThreadPoolMaxSize();
      cf.getRetryInterval();
      cf.getRetryIntervalMultiplier();
      cf.getReconnectAttempts();
      cf.isFailoverOnServerShutdown();

   }

   private void assertFactoryParams(ClientSessionFactory cf,
                                    List<Pair<TransportConfiguration, TransportConfiguration>> staticConnectors,
                                    String discoveryAddress,
                                    int discoveryPort,
                                    long discoveryRefreshTimeout,
                                    long clientFailureCheckPeriod,
                                    long connectionTTL,
                                    long callTimeout,                                   
                                    int minLargeMessageSize,
                                    int consumerWindowSize,
                                    int consumerMaxRate,
                                    int confirmationWindowSize,
                                    int producerMaxRate,
                                    boolean blockOnAcknowledge,
                                    boolean blockOnPersistentSend,
                                    boolean blockOnNonPersistentSend,
                                    boolean autoGroup,
                                    boolean preAcknowledge,
                                    String loadBalancingPolicyClassName,
                                    int ackBatchSize,
                                    long initialWaitTimeout,
                                    boolean useGlobalPools,
                                    int scheduledThreadPoolMaxSize,
                                    int threadPoolMaxSize,
                                    long retryInterval,
                                    double retryIntervalMultiplier,
                                    int reconnectAttempts,
                                    boolean failoverOnServerShutdown)
   {
      List<Pair<TransportConfiguration, TransportConfiguration>> cfStaticConnectors = cf.getStaticConnectors();
      if (staticConnectors == null)
      {
         assertNull(cfStaticConnectors);
      }
      else
      {
         assertEquals(staticConnectors.size(), cfStaticConnectors.size());

         for (int i = 0; i < staticConnectors.size(); i++)
         {
            assertEquals(staticConnectors.get(i), cfStaticConnectors.get(i));
         }
      }
      assertEquals(cf.getDiscoveryAddress(), discoveryAddress);
      assertEquals(cf.getDiscoveryPort(), discoveryPort);
      assertEquals(cf.getDiscoveryRefreshTimeout(), discoveryRefreshTimeout);
      assertEquals(cf.getClientFailureCheckPeriod(), clientFailureCheckPeriod);
      assertEquals(cf.getConnectionTTL(), connectionTTL);
      assertEquals(cf.getCallTimeout(), callTimeout);
      assertEquals(cf.getMinLargeMessageSize(), minLargeMessageSize);
      assertEquals(cf.getConsumerWindowSize(), consumerWindowSize);
      assertEquals(cf.getConsumerMaxRate(), consumerMaxRate);
      assertEquals(cf.getConfirmationWindowSize(), confirmationWindowSize);
      assertEquals(cf.getProducerMaxRate(), producerMaxRate);
      assertEquals(cf.isBlockOnAcknowledge(), blockOnAcknowledge);
      assertEquals(cf.isBlockOnPersistentSend(), blockOnPersistentSend);
      assertEquals(cf.isBlockOnNonPersistentSend(), blockOnNonPersistentSend);
      assertEquals(cf.isAutoGroup(), autoGroup);
      assertEquals(cf.isPreAcknowledge(), preAcknowledge);
      assertEquals(cf.getConnectionLoadBalancingPolicyClassName(), loadBalancingPolicyClassName);
      assertEquals(cf.getAckBatchSize(), ackBatchSize);
      assertEquals(cf.getDiscoveryInitialWaitTimeout(), initialWaitTimeout);
      assertEquals(cf.isUseGlobalPools(), useGlobalPools);
      assertEquals(cf.getScheduledThreadPoolMaxSize(), scheduledThreadPoolMaxSize);
      assertEquals(cf.getThreadPoolMaxSize(), threadPoolMaxSize);
      assertEquals(cf.getRetryInterval(), retryInterval);
      assertEquals(cf.getRetryIntervalMultiplier(), retryIntervalMultiplier);
      assertEquals(cf.getReconnectAttempts(), reconnectAttempts);
      assertEquals(cf.isFailoverOnServerShutdown(), failoverOnServerShutdown);
   }

   private void stopLiveAndBackup() throws Exception
   {
      if (liveService.isStarted())
      {
         log.info("stopping live");
         liveService.stop();
      }
      if (backupService.isStarted())
      {
         log.info("stopping backup");
         backupService.stop();
      }
   }

   private void startLiveAndBackup() throws Exception
   {
      Map<String, Object> backupParams = new HashMap<String, Object>();
      Configuration backupConf = new ConfigurationImpl();
      backupConf.setSecurityEnabled(false);
      backupConf.setClustered(true);
      backupParams.put(TransportConstants.SERVER_ID_PROP_NAME, 1);
      backupConf.getAcceptorConfigurations()
                .add(new TransportConfiguration("org.hornetq.core.remoting.impl.invm.InVMAcceptorFactory",
                                                backupParams));
      backupConf.setBackup(true);
      backupService = HornetQ.newHornetQServer(backupConf, false);
      backupService.start();

      Configuration liveConf = new ConfigurationImpl();
      liveConf.setSecurityEnabled(false);
      liveTC = new TransportConfiguration("org.hornetq.core.remoting.impl.invm.InVMConnectorFactory");
      liveConf.getAcceptorConfigurations()
              .add(new TransportConfiguration("org.hornetq.core.remoting.impl.invm.InVMAcceptorFactory"));
      Map<String, TransportConfiguration> connectors = new HashMap<String, TransportConfiguration>();
      backupTC = new TransportConfiguration("org.hornetq.core.remoting.impl.invm.InVMConnectorFactory",
                                            backupParams);
      connectors.put(backupTC.getName(), backupTC);
      connectors.put(liveTC.getName(), liveTC);
      liveConf.setConnectorConfigurations(connectors);
      liveConf.setBackupConnectorName(backupTC.getName());
      liveConf.setClustered(true);

      List<Pair<String, String>> connectorNames = new ArrayList<Pair<String, String>>();
      connectorNames.add(new Pair<String, String>(liveTC.getName(), backupTC.getName()));

      final long broadcastPeriod = 250;

      final String bcGroupName = "bc1";

      final int localBindPort = 5432;

      BroadcastGroupConfiguration bcConfig1 = new BroadcastGroupConfiguration(bcGroupName,
                                                                              null,
                                                                              localBindPort,
                                                                              groupAddress,
                                                                              groupPort,
                                                                              broadcastPeriod,
                                                                              connectorNames);

      List<BroadcastGroupConfiguration> bcConfigs1 = new ArrayList<BroadcastGroupConfiguration>();
      bcConfigs1.add(bcConfig1);
      liveConf.setBroadcastGroupConfigurations(bcConfigs1);

      liveService = HornetQ.newHornetQServer(liveConf, false);
      liveService.start();
   }
}
