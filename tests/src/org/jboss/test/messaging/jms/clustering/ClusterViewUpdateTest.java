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

package org.jboss.test.messaging.jms.clustering;

import org.jboss.test.messaging.tools.ServerManagement;
import org.jboss.jms.client.JBossConnectionFactory;
import org.jboss.jms.client.state.ConnectionState;
import org.jboss.jms.client.delegate.ClientClusteredConnectionFactoryDelegate;
import org.jboss.jms.client.delegate.ClientConnectionFactoryDelegate;
import javax.jms.Connection;
import javax.jms.Session;
import javax.jms.ConnectionFactory;

/**
 * @author <a href="mailto:clebert.suconic@jboss.org">Clebert Suconic</a>
 * @version <tt>$Revision$</tt>
 *
 * $Id$
 */
public class ClusterViewUpdateTest extends ClusteringTestBase
{

   // Constants ------------------------------------------------------------------------------------

   // Attributes -----------------------------------------------------------------------------------

   // Static ---------------------------------------------------------------------------------------

   // Constructors ---------------------------------------------------------------------------------

   public ClusterViewUpdateTest(String name)
   {
      super(name);
   }

   // Public ---------------------------------------------------------------------------------------

   public void testUpdateConnectionFactory() throws Exception
   {
      Connection conn = createConnectionOnServer(cf, 0);

      JBossConnectionFactory jbcf = (JBossConnectionFactory)cf;

      ClientClusteredConnectionFactoryDelegate cfDelegate =
         (ClientClusteredConnectionFactoryDelegate)jbcf.getDelegate();

      assertEquals(3, cfDelegate.getDelegates().length);

      Connection conn1 = cf.createConnection();

      assertEquals(1, getServerId(conn1));

      log.info("*** killing server");
      ServerManagement.killAndWait(1);
      log.info("killed server");

      log.info("sleeping 5 secs ...");
      Thread.sleep(5000);

      // first part of the test, verifies if the CF was updated
      assertEquals(2, cfDelegate.getDelegates().length);
      conn.close();

      log.info("sleeping 10 secs ...");
      Thread.sleep(10000);

      // Second part, verifies a possible race condition on failoverMap and handleFilover

      log.info("ServerId=" + getServerId(conn1));
      assertTrue(1 != getServerId(conn1));

      //Session sess = conn1.createSession(true, Session.SESSION_TRANSACTED);
      conn1.close();
      
      log.info("Done!!");

   }

   public void testUpdateMixedConnectionFactory() throws Exception
   {
      Connection conn = createConnectionOnServer(cf, 0);
      JBossConnectionFactory jbcf = (JBossConnectionFactory)cf;

      ClientClusteredConnectionFactoryDelegate cfDelegate =
         (ClientClusteredConnectionFactoryDelegate)jbcf.getDelegate();

      assertEquals(3, cfDelegate.getDelegates().length);

      ConnectionFactory httpCF = (ConnectionFactory)ic[0].lookup("/HTTPConnectionFactory");
      JBossConnectionFactory jbhttpCF = (JBossConnectionFactory) httpCF;
      
      Connection httpConn = createConnectionOnServer(httpCF, 0);

      ClientClusteredConnectionFactoryDelegate httpcfDelegate =
         (ClientClusteredConnectionFactoryDelegate)jbhttpCF.getDelegate();

      assertEquals(3, httpcfDelegate.getDelegates().length);


      validateCFs(cfDelegate, httpcfDelegate);

      Connection conn1 = cf.createConnection();
      Connection httpConn1 = httpCF.createConnection();

      assertEquals(1, getServerId(conn1));
      assertEquals(1, getServerId(httpConn1));

      ServerManagement.killAndWait(1);

      log.info("sleeping 5 secs ...");
      Thread.sleep(5000);

      // first part of the test, verifies if the CF was updated
      assertEquals(2, cfDelegate.getDelegates().length);
      assertEquals(2, httpcfDelegate.getDelegates().length);

      validateCFs(cfDelegate, httpcfDelegate);

      conn.close();
      httpConn.close();

      log.info("sleeping 10 secs ...");
      Thread.sleep(10000);

      // Second part, verifies a possible racing condition on failoverMap and handleFilover

      log.info("ServerId=" + getServerId(conn1));
      assertTrue(1 != getServerId(conn1));

      //Session sess = conn1.createSession(true, Session.SESSION_TRANSACTED);
      conn1.close();
      httpConn.close();

   }

   /**
    * Test if an update on failoverMap on the connectionFactory would
    * cause any problems during failover
    */
   public void testUpdateConnectionFactoryRaceCondition() throws Exception
   {
      // This connection needs to be opened, as we need the callback to update CF from this conn
      Connection conn = createConnectionOnServer(cf, 0);
      JBossConnectionFactory jbcf = (JBossConnectionFactory) cf;
      ClientClusteredConnectionFactoryDelegate cfDelegate =
         (ClientClusteredConnectionFactoryDelegate) jbcf.getDelegate();
      assertEquals(3, cfDelegate.getDelegates().length);

      Connection conn1 = cf.createConnection();

      Connection conn2 = cf.createConnection();

      assertEquals(2, getServerId(conn2));

      assertEquals(1, getServerId(conn1));

      ConnectionState state = this.getConnectionState(conn1);

      // Disable Leasing for Failover
      state.getRemotingConnection().removeConnectionListener();

      ServerManagement.killAndWait(1);

      Thread.sleep(10000);

      // This will force Failover from Valve to kick in
      conn1.createSession(true, Session.SESSION_TRANSACTED);

      // first part of the test, verifies if the CF was updated
      assertEquals(2, cfDelegate.getDelegates().length);

      assertTrue(1 != getServerId(conn1));

      conn.close();
      conn1.close();
      conn2.close();

   }

   // Package protected ----------------------------------------------------------------------------

   // Protected ------------------------------------------------------------------------------------

   protected void setUp() throws Exception
   {
      config = "all+http";
      nodeCount = 3;
      super.setUp();
   }

   protected void tearDown() throws Exception
   {
      super.tearDown();
      config="all";
   }

   // Private --------------------------------------------------------------------------------------

   // Validate if two distinct CFs are valid
   private void validateCFs(ClientClusteredConnectionFactoryDelegate cfDelegate,
                            ClientClusteredConnectionFactoryDelegate httpcfDelegate)
   {
      ClientConnectionFactoryDelegate delegatesSocket[] = cfDelegate.getDelegates();
      ClientConnectionFactoryDelegate delegatesHTTP[] = httpcfDelegate.getDelegates();

      log.info("ValidateCFs:");

      assertEquals(delegatesSocket.length, delegatesHTTP.length);
      for (int i=0;i<delegatesSocket.length;i++)
      {
         log.info("DelegateSocket[" + i + "]=" + delegatesSocket[i].getServerLocatorURI());
         log.info("DelegateHttp[" + i + "]=" + delegatesHTTP[i].getServerLocatorURI());
         for (int j=0;j<delegatesHTTP.length;j++)
         {
            assertFalse(delegatesSocket[i].getServerLocatorURI().
               equals(delegatesHTTP[j].getServerLocatorURI()));
         }
      }
   }

   // Inner classes --------------------------------------------------------------------------------

}
