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
package org.jboss.messaging.core.message;

import org.jboss.messaging.core.Message;
import org.jboss.jms.destination.JBossDestination;
import org.jboss.jms.message.JBossMessage;
import org.jboss.jms.message.JBossObjectMessage;
import org.jboss.jms.message.JBossTextMessage;
import org.jboss.jms.message.JBossBytesMessage;
import org.jboss.jms.message.JBossMapMessage;
import org.jboss.jms.message.JBossStreamMessage;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>  
 * @version <tt>$Revision$</tt>
 * 
 * $Id$
 */
public class MessageFactory
{
   // Constants -----------------------------------------------------

   // Static --------------------------------------------------------

   public static Message createMessage(byte type)
   {
      Message m = null;
      
      if (type == JBossMessage.TYPE)
      {
         m = new JBossMessage();
      }
      else if (type == JBossObjectMessage.TYPE)
      {
         m = new JBossObjectMessage();
      }
      else if (type == JBossTextMessage.TYPE)
      {
         m = new JBossTextMessage();
      }
      else if (type == JBossBytesMessage.TYPE)
      {
         m = new JBossBytesMessage();
      }
      else if (type == JBossMapMessage.TYPE)
      {
         m = new JBossMapMessage();
      }
      else if (type == JBossStreamMessage.TYPE)
      {
         m = new JBossStreamMessage();
      }
     
      return m;
   }
   
   public static CoreMessage createCoreMessage(long messageID)
   {
      return createCoreMessage(messageID, false, 0, 0, (byte)4, null, null, 0);
   }

   public static CoreMessage createCoreMessage(long messageID,
                                               boolean reliable,
                                               Serializable payload)
   {
      return createCoreMessage(messageID, reliable, 0, 0, (byte)4, null, payload, 0);
   }

   public static CoreMessage createCoreMessage(long messageID,
                                               boolean reliable,
                                               long expiration,
                                               long timestamp,
                                               byte priority,
                                               Map coreHeaders,
                                               Serializable payload,
                                               int persistentChannelCount)
   {
      CoreMessage cm =
         new CoreMessage(messageID, reliable, expiration, timestamp, priority, coreHeaders, null, persistentChannelCount);
      cm.setPayload(payload);
      return cm;
   }
   
   public static CoreMessage createCoreMessage(long messageID,
         boolean reliable,
         long expiration,
         long timestamp,
         byte priority,
         Map coreHeaders,
         Serializable payload)
   {
      return createCoreMessage(messageID, reliable, expiration, timestamp, priority, coreHeaders, payload, 0);
   }
   
   public static Message createJBossMessage(long messageID,
                                          boolean reliable, 
                                          long expiration, 
                                          long timestamp,
                                          byte priority,
                                          Map coreHeaders,
                                          byte[] payloadAsByteArray,                                          
                                          int persistentChannelCount,
                                          byte type,
                                          String jmsType,                                       
                                          String correlationID,
                                          byte[] correlationIDBytes,
                                          JBossDestination destination,
                                          JBossDestination replyTo,                                                                              
                                          HashMap jmsProperties)

   {

      Message m = null;
      
      if (type == JBossMessage.TYPE)
      {
         m = new JBossMessage(messageID, reliable, expiration, timestamp, priority, coreHeaders,
                              payloadAsByteArray, persistentChannelCount, jmsType, correlationID, correlationIDBytes,
                              destination, replyTo, jmsProperties);
      }
      else if (type == JBossObjectMessage.TYPE)
      {
         m = new JBossObjectMessage(messageID, reliable, expiration, timestamp, priority, coreHeaders,
               payloadAsByteArray, persistentChannelCount, jmsType, correlationID, correlationIDBytes,
               destination, replyTo, jmsProperties);
      }
      else if (type == JBossTextMessage.TYPE)
      {
         m = new JBossTextMessage(messageID, reliable, expiration, timestamp, priority, coreHeaders,
               payloadAsByteArray, persistentChannelCount, jmsType, correlationID, correlationIDBytes,
               destination, replyTo, jmsProperties);
      }
      else if (type == JBossBytesMessage.TYPE)
      {
         m = new JBossBytesMessage(messageID, reliable, expiration, timestamp, priority, coreHeaders,
               payloadAsByteArray, persistentChannelCount, jmsType, correlationID, correlationIDBytes,
               destination, replyTo, jmsProperties);
      }
      else if (type == JBossMapMessage.TYPE)
      {
         m = new JBossMapMessage(messageID, reliable, expiration, timestamp, priority, coreHeaders,
               payloadAsByteArray, persistentChannelCount, jmsType, correlationID, correlationIDBytes,
               destination, replyTo, jmsProperties);
      }
      else if (type == JBossStreamMessage.TYPE)
      {
         m = new JBossStreamMessage(messageID, reliable, expiration, timestamp, priority, coreHeaders,
               payloadAsByteArray, persistentChannelCount, jmsType, correlationID, correlationIDBytes,
               destination, replyTo, jmsProperties);
      }
      else
      {
          throw new IllegalArgumentException("Unknow type " + type);                       
      }

      return m;

   }
      
   // Attributes ----------------------------------------------------
   
   // Constructors --------------------------------------------------
   
   // Public --------------------------------------------------------

   // Package protected ---------------------------------------------
   
   // Protected -----------------------------------------------------
   
   // Private -------------------------------------------------------
   
   // Inner classes -------------------------------------------------   
}
