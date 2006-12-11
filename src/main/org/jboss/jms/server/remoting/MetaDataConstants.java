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
package org.jboss.jms.server.remoting;

/**
 * Constants for passing stuff in aop meta-data
 * 
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 * @version <tt>$Revision 1.1 $</tt>
 *
 * $Id$
 */
public class MetaDataConstants
{
   public static final String JMS = "JMS";
   
   public static final String REMOTING_SESSION_ID = "REMOTING_SESSION_ID";
   
   public static final String CALLBACK_HANDLER = "CALLBACK_HANDLER";
   
   public static final String CONSUMER_ID = "CONSUMER_ID";   
   
   public static final String PREFETCH_SIZE = "BUFFER_SIZE";
   
   public static final String CLIENT_CONNECTION_ID = "CC_ID";
   
   public static final String VERSION_NUMBER = "VERSION_NUMBER";
   
   public static final String JMS_CLIENT_VM_ID = "JMS_CLIENT_VM_ID";

   public static final String CF_DELEGATES = "CF_DELEGATES";
   
   public static final String SERVER_ID = "SERVER_ID";
   
   public static final String REMOTING_CONNECTION = "REMOTING_CONNECTION";
   
   public static final String FAILOVER_MAP = "CF_FAIL_IND";
   
   public static final String CONNECTION_VERSION = "CONNECTION_VERSION";

   public static final String MAX_DELIVERIES = "MAX_DELS";
}
