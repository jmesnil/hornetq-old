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

package org.hornetq.core.server;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;

import org.hornetq.core.config.Configuration;
import org.hornetq.core.logging.Logger;
import org.hornetq.core.security.HornetQSecurityManager;
import org.hornetq.core.security.impl.HornetQSecurityManagerImpl;
import org.hornetq.core.server.impl.HornetQServerImpl;

/**
 * A HornetQ
 *
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 * 
 * Created 24 Jan 2009 15:17:18
 *
 *
 */
public class HornetQ
{
   private static final Logger log = Logger.getLogger(HornetQ.class);

   public static HornetQServer newHornetQServer(final Configuration config, boolean enablePersistence)
   {
      HornetQSecurityManager securityManager = new HornetQSecurityManagerImpl();

      HornetQServer server = newHornetQServer(config,
                                              ManagementFactory.getPlatformMBeanServer(),
                                              securityManager,
                                              enablePersistence);

      return server;
   }

   public static HornetQServer newHornetQServer(final Configuration config)
   {
      return newHornetQServer(config, config.isPersistenceEnabled());
   }

   public static HornetQServer newHornetQServer(final Configuration config,
                                                final MBeanServer mbeanServer,
                                                final boolean enablePersistence)
   {
      HornetQSecurityManager securityManager = new HornetQSecurityManagerImpl();

      HornetQServer server = newHornetQServer(config, mbeanServer, securityManager, enablePersistence);

      return server;
   }

   public static HornetQServer newHornetQServer(final Configuration config, final MBeanServer mbeanServer)
   {
      return newHornetQServer(config, mbeanServer, true);
   }

   public static HornetQServer newHornetQServer(final Configuration config,
                                                final MBeanServer mbeanServer,
                                                final HornetQSecurityManager securityManager)
   {
      HornetQServer server = newHornetQServer(config, mbeanServer, securityManager, true);

      return server;
   }

   public static HornetQServer newHornetQServer(final Configuration config,
                                                final MBeanServer mbeanServer,
                                                final HornetQSecurityManager securityManager,
                                                final boolean enablePersistence)
   {
      config.setPersistenceEnabled(enablePersistence);

      HornetQServer server = new HornetQServerImpl(config, mbeanServer, securityManager);

      return server;
   }

}
