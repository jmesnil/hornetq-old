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
package org.jboss.messaging.core.plugin.postoffice;

import org.jboss.messaging.core.Queue;
import org.jboss.messaging.core.plugin.contract.Condition;

/**
 * 
 * A DefaultBinding
 *
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 * @version <tt>$Revision: 1.1 $</tt>
 *
 * $Id$
 *
 */
public class DefaultBinding implements Binding
{
   private int nodeId;
   
   private Condition condition;
   
   private Queue queue;

   private boolean failed;

   public DefaultBinding()
   {
   }

   public DefaultBinding(int nodeId, Condition condition, Queue queue, boolean failed)
   {
      this.nodeId = nodeId;

      this.condition = condition;

      this.queue = queue;

      this.failed = failed;
   }

   public int getNodeId()
   {
      return nodeId;
   }

   public Condition getCondition()
   {
      return condition;
   }

   public Queue getQueue()
   {
      return queue;
   }

   public boolean isFailed()
   {
      return failed;
   }

   public void setFailed(boolean failed)
   {
      this.failed = failed;
   }

   public String toString()
   {
       return "Node" + nodeId + " condition=" + condition + " queue=" + queue + " queueClass=" + queue.getClass().getName();
   }
   
}
