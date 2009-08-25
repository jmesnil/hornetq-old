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


package org.hornetq.core.postoffice;

import java.io.Serializable;
import java.util.List;

import org.hornetq.utils.SimpleString;

/**
 * A QueueInfo
 *
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 * 
 * Created 21 Jan 2009 20:55:06
 *
 *
 */
public class QueueInfo implements Serializable
{
   private static final long serialVersionUID = 3451892849198803182L;

   private final SimpleString routingName;
   
   private final SimpleString clusterName;
   
   private final SimpleString address;
   
   private final SimpleString filterString;
   
   private final int id;
   
   private List<SimpleString> filterStrings;
   
   private int numberOfConsumers;
   
   private final int distance;
   
   public QueueInfo(final SimpleString routingName, final SimpleString clusterName, final SimpleString address, final SimpleString filterString, final int id,
                    final Integer distance)
   {
      if (routingName == null)
      {
         throw new IllegalArgumentException("Routing name is null");
      }
      if (clusterName == null)
      {
         throw new IllegalArgumentException("Cluster name is null");
      }
      if (address == null)
      {
         throw new IllegalArgumentException("Address is null");
      }
      if (distance == null)
      {
         throw new IllegalArgumentException("Distance is null");
      }
      this.routingName = routingName;
      this.clusterName = clusterName;
      this.address = address;      
      this.filterString = filterString;
      this.id = id;
      this.distance = distance;
   }

   public SimpleString getRoutingName()
   {
      return routingName;
   }
   
   public SimpleString getClusterName()
   {
      return clusterName;
   }

   public SimpleString getAddress()
   {
      return address;
   }
   
   public SimpleString getFilterString()
   {
      return filterString;
   }
   
   public int getDistance()
   {
      return distance;
   }
   
   public int getID()
   {
      return id;
   }

   public List<SimpleString> getFilterStrings()
   {
      return filterStrings;
   }
   
   public void setFilterStrings(final List<SimpleString> filterStrings)
   {
      this.filterStrings = filterStrings;
   }

   public int getNumberOfConsumers()
   {
      return numberOfConsumers;
   }     
   
   public void incrementConsumers()
   {
      this.numberOfConsumers++;
   }
   
   public void decrementConsumers()
   {
      this.numberOfConsumers--;
   }
}