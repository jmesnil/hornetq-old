/*
 * JBoss, the OpenSource J2EE webOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.jms.server.container;

import org.jboss.aop.Interceptor;
import org.jboss.aop.Invocation;
import org.jboss.aop.MethodInvocation;

/**
 * The server implementation of the connection
 * 
 * @author <a href="mailto:adrian@jboss.org>Adrian Brock</a>
 * @version $Revision$
 */
public class ServerConnectionInterceptor
   implements Interceptor
{
   // Constants -----------------------------------------------------

   // Attributes ----------------------------------------------------

   // Static --------------------------------------------------------

   public static ServerConnectionInterceptor singleton = new ServerConnectionInterceptor();

   // Constructors --------------------------------------------------

   // Public --------------------------------------------------------

   // Interceptor implementation ------------------------------------

   public String getName()
   {
      return "ServerConnectionInterceptor";
   }

   public Object invoke(Invocation invocation) throws Throwable
   {
      MethodInvocation mi = (MethodInvocation) invocation;
      String methodName = ((MethodInvocation) invocation).method.getName();
      if (methodName.equals("createSession"))
         return null;
      else if (methodName.equals("closing") || methodName.equals("close"))
         return null;
      throw new UnsupportedOperationException(mi.method.toString()); 
   }

   // Protected ------------------------------------------------------

   // Package Private ------------------------------------------------

   // Private --------------------------------------------------------

   // Inner Classes --------------------------------------------------

}
