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
package org.jboss.test.messaging.jms.message;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.DeliveryMode;

import org.jboss.test.messaging.jms.message.base.MessageTestBase;
import java.net.URL;
import java.net.URLClassLoader;
import java.io.File;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * A test that sends/receives object messages to the JMS provider and verifies their integrity.
 *
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 * @author <a href="mailto:clebert.suconic@jboss.org">Clebert Suconic</a>
 * @version <tt>$Revision$</tt>
 *
 * $Id$
 */
public class ObjectMessageTest extends MessageTestBase
{
   // Constants ------------------------------------------------------------------------------------

   // Static ---------------------------------------------------------------------------------------
   
   // Attributes -----------------------------------------------------------------------------------

   // Constructors ---------------------------------------------------------------------------------

   public ObjectMessageTest(String name)
   {
      super(name);
   }

   // Public ---------------------------------------------------------------------------------------

   public void setUp() throws Exception
   {
      super.setUp();
      message = session.createObjectMessage();
   }

   public void tearDown() throws Exception
   {
      message = null;
      super.tearDown();
   }


   public void testClassLoaderIsolation() throws Exception
   {

      ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
      try
      {
         queueProd.setDeliveryMode(DeliveryMode.PERSISTENT);

         ObjectMessage om = (ObjectMessage) message;

         SomeObject testObject = new SomeObject(3, 7);

         ClassLoader testClassLoader = newClassLoader(testObject.getClass());

         om.setObject(testObject);

         queueProd.send(message);

         Thread.currentThread().setContextClassLoader(testClassLoader);

         ObjectMessage r = (ObjectMessage) queueCons.receive();

         Object testObject2 = r.getObject();

         assertEquals("org.jboss.test.messaging.jms.message.SomeObject",
            testObject2.getClass().getName());
         assertNotSame(testObject, testObject2);
         assertNotSame(testObject.getClass(), testObject2.getClass());
         assertNotSame(testObject.getClass().getClassLoader(),
            testObject2.getClass().getClassLoader());
         assertSame(testClassLoader,
            testObject2.getClass().getClassLoader());
      }
      finally
      {
         Thread.currentThread().setContextClassLoader(originalClassLoader);
      }

   }

   // Protected ------------------------------------------------------------------------------------

   protected void prepareMessage(Message m) throws JMSException
   {
      super.prepareMessage(m);

      ObjectMessage om = (ObjectMessage)m;
      om.setObject("this is the serializable object");

   }

   protected void assertEquivalent(Message m, int mode) throws JMSException
   {
      super.assertEquivalent(m, mode);

      ObjectMessage om = (ObjectMessage)m;
      assertEquals("this is the serializable object", om.getObject());
   }

   protected static ClassLoader newClassLoader(Class anyUserClass) throws Exception
   {
      URL classLocation = anyUserClass.getProtectionDomain().getCodeSource().getLocation();
      StringTokenizer tokenString = new StringTokenizer(System.getProperty("java.class.path"),
         File.pathSeparator);
      String pathIgnore = System.getProperty("java.home");
      if (pathIgnore == null)
      {
         pathIgnore = classLocation.toString();
      }

      ArrayList urls = new ArrayList();
      while (tokenString.hasMoreElements())
      {
         String value = tokenString.nextToken();
         URL itemLocation = new File(value).toURL();
         if (!itemLocation.equals(classLocation) &&
                      itemLocation.toString().indexOf(pathIgnore) >= 0)
         {
            urls.add(itemLocation);
         }
      }

      URL[] urlArray = (URL[]) urls.toArray(new URL[urls.size()]);

      ClassLoader masterClassLoader = URLClassLoader.newInstance(urlArray, null);


      ClassLoader appClassLoader = URLClassLoader.newInstance(new URL[]{classLocation},
                                      masterClassLoader);

      return appClassLoader;
   }

}
