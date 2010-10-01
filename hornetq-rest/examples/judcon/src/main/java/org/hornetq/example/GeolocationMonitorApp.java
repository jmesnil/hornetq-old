/*
 * Copyright 2010 Red Hat, Inc.
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

package org.hornetq.example;

import java.util.Scanner;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.Topic;

import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.jms.HornetQJMSClient;
import org.hornetq.core.remoting.impl.netty.NettyConnectorFactory;
import org.hornetq.utils.json.JSONObject;

public class GeolocationMonitorApp
{

   public static void main(String[] args) throws Throwable
   {
      System.out.println("Start Geolocation monitoring application");
      
      // use HornetQ helper class instead of JNDI
      TransportConfiguration connector = new TransportConfiguration(NettyConnectorFactory.class.getName());
      ConnectionFactory cf = HornetQJMSClient.createConnectionFactory(connector);
      Topic topic = HornetQJMSClient.createTopic("trackers");
      
      Connection conn = cf.createConnection();
      Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
      MessageConsumer consumer = session.createConsumer(topic);
      consumer.setMessageListener(new MessageListener()
      {
         public void onMessage(Message message)
         {
            try
            {
               BytesMessage msg = (BytesMessage)message;
               long length = msg.getBodyLength();
               byte[] body = new byte[(int)length];
               msg.readBytes(body);
               String text = new String(body, "UTF-8");
               //System.out.println(text);
               display(text);
            }
            catch (Throwable t)
            {
               t.printStackTrace();
            }
         }
      });
      
      System.out.println("Listening for messages on JMS topic " + topic.getTopicName() + "...\n");
      conn.start();
      
      waitForInput();
      
      conn.close();
      System.out.println("Goodbye!");
   }
   
   
   private static void display(String jsonStr) throws Throwable
   {
      JSONObject obj = new JSONObject(jsonStr);
      String alias = obj.getString("alias");
      if (obj.isNull("position"))
      {
         System.out.println(alias + " has left");
         return;
      }
      JSONObject position = obj.getJSONObject("position");
      JSONObject coords = position.getJSONObject("coords");
      double latitude = coords.getDouble("latitude");
      double longitude = coords.getDouble("longitude");
      
      System.out.format("%s is at %.2f %.2f\n", 
                        alias, 
                        longitude,
                        latitude); 
   }

   private static void waitForInput()
   {
      while (true)
      {
         Scanner in = new Scanner(System.in);
         String line = in.nextLine();
         if ("Q".equalsIgnoreCase(line))
         {
            return;
         }
      }
   }
}
