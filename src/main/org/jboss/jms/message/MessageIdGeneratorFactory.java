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
package org.jboss.jms.message;

import java.util.HashMap;
import java.util.Map;

import javax.jms.JMSException;

import org.jboss.jms.delegate.ConnectionFactoryDelegate;

/**
 * This class manages instances of MessageIdGenerator. It ensures there is one instance per instance
 * of JMS server as specified by the server id.
 *
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 * @version 1.1
 *
 * MessageIdGeneratorFactory.java,v 1.1 2006/03/07 17:11:14 timfox Exp
 */
public class MessageIdGeneratorFactory
{
   public static MessageIdGeneratorFactory instance = new MessageIdGeneratorFactory();

   //TODO Make configurable
   private static final int BLOCK_SIZE = 256;

   private Map holders;

   private MessageIdGeneratorFactory()
   {
      holders = new HashMap();
   }

   public synchronized boolean containsMessageIdGenerator(String serverId)
   {
      return holders.containsKey(serverId);
   }

   public synchronized MessageIdGenerator getGenerator(String serverId,
                                                       ConnectionFactoryDelegate cfd)
      throws JMSException
   {
      Holder h = (Holder)holders.get(serverId);

      if (h == null)
      {
         h = new Holder(new MessageIdGenerator(cfd, BLOCK_SIZE));

         holders.put(serverId, h);
      }
      else
      {
         h.refCount++;
      }
      return h.generator;
   }

   public synchronized void returnGenerator(String serverId)
   {
      Holder h = (Holder)holders.get(serverId);

      if (h == null)
      {
         throw new IllegalArgumentException("Cannot find generator for serverid:" + serverId);
      }

      h.refCount--;

      if (h.refCount == 0)
      {
         holders.remove(serverId);
      }
   }

   public synchronized void clear()
   {
      holders.clear();
   }

   private class Holder
   {
      private Holder(MessageIdGenerator gen)
      {
         this.generator = gen;
      }

      MessageIdGenerator generator;

      int refCount = 1;
   }

}
