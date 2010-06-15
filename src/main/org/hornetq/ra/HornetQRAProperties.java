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
package org.hornetq.ra;

import java.io.Serializable;

import org.hornetq.core.logging.Logger;

/**
 * The RA default properties - these are set in the ra.xml file
 *
 * @author <a href="mailto:adrian@jboss.com">Adrian Brock</a>
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 * @author <a href="mailto:andy.taylor@jboss.org">Andy Taylor</a>
 * @version $Revision: $
 */
public class HornetQRAProperties extends ConnectionFactoryProperties implements Serializable
{
   /** Serial version UID */
   static final long serialVersionUID = -2772367477755473248L;

   /** The logger */
   private static final Logger log = Logger.getLogger(HornetQRAProperties.class);

   /** Trace enabled */
   private static boolean trace = HornetQRAProperties.log.isTraceEnabled();

   /** The user name */
   private String userName;

   /** The password */
   private String password;

   /** Use XA */
   private Boolean useXA;

   /** Use Local TX instead of XA */
   private Boolean localTx = false;
   
   
   /** Class used to locate the Transaction Manager.
    *  Using JBoss5 as the default locator */
   private String transactionManagerLocatorClass = "org.hornetq.integration.jboss.tm.JBoss5TransactionManagerLocator;org.hornetq.integration.jboss.tm.JBoss4TransactionManagerLocator";
   
   /** Method used to locate the TM */
   private String transactionManagerLocatorMethod = "getTm;getTM";

   /**
    * Constructor
    */
   public HornetQRAProperties()
   {
      if (HornetQRAProperties.trace)
      {
         HornetQRAProperties.log.trace("constructor()");
      }
   }

   /**
    * Get the user name
    * @return The value
    */
   public String getUserName()
   {
      if (HornetQRAProperties.trace)
      {
         HornetQRAProperties.log.trace("getUserName()");
      }

      return userName;
   }

   /**
    * Set the user name
    * @param userName The value
    */
   public void setUserName(final String userName)
   {
      if (HornetQRAProperties.trace)
      {
         HornetQRAProperties.log.trace("setUserName(" + userName + ")");
      }

      this.userName = userName;
   }

   /**
    * Get the password
    * @return The value
    */
   public String getPassword()
   {
      if (HornetQRAProperties.trace)
      {
         HornetQRAProperties.log.trace("getPassword()");
      }

      return password;
   }

   /**
    * Set the password
    * @param password The value
    */
   public void setPassword(final String password)
   {
      if (HornetQRAProperties.trace)
      {
         HornetQRAProperties.log.trace("setPassword(****)");
      }

      this.password = password;
   }

   /**
    * Get the use XA flag
    * @return The value
    */
   public Boolean getUseLocalTx()
   {
      if (HornetQRAProperties.trace)
      {
         HornetQRAProperties.log.trace("getUseLocalTx()");
      }

      return localTx;
   }

   /**
    * Set the use XA flag
    * @param localTx The value
    */
   public void setUseLocalTx(final Boolean localTx)
   {
      if (HornetQRAProperties.trace)
      {
         HornetQRAProperties.log.trace("setUseLocalTx(" + localTx + ")");
      }

      this.localTx = localTx;
   }


   public void setTransactionManagerLocatorClass(final String transactionManagerLocatorClass)
   {
      this.transactionManagerLocatorClass = transactionManagerLocatorClass;
   }

   public String getTransactionManagerLocatorClass()
   {
      return transactionManagerLocatorClass;
   }

   public String getTransactionManagerLocatorMethod()
   {
      return transactionManagerLocatorMethod;
   }

   public void setTransactionManagerLocatorMethod(final String transactionManagerLocatorMethod)
   {
      this.transactionManagerLocatorMethod = transactionManagerLocatorMethod;
   }



   /**
    * Get the use XA flag
    * @return The value
    */
   public Boolean getUseXA()
   {
      if (HornetQRAProperties.trace)
      {
         HornetQRAProperties.log.trace("getUseXA()");
      }

      return useXA;
   }

   /**
    * Set the use XA flag
    * @param xa The value
    */
   public void setUseXA(final Boolean xa)
   {
      if (HornetQRAProperties.trace)
      {
         HornetQRAProperties.log.trace("setUseXA(" + xa + ")");
      }

      useXA = xa;
   }

   /**
    * Use XA for communication
    * @return The value
    */
   public boolean isUseXA()
   {
      if (HornetQRAProperties.trace)
      {
         HornetQRAProperties.log.trace("isUseXA()");
      }

      return useXA != null && useXA;
   }
   
   @Override
   public String toString()
   {
      return "HornetQRAProperties[useXA=" + useXA + ", localTx=" + localTx +
         ", userName=" + userName + ", password=" + password + "]";
   }
}
