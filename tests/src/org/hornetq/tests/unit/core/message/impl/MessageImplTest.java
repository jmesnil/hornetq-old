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

package org.hornetq.tests.unit.core.message.impl;

import java.util.Set;

import junit.framework.Assert;

import org.hornetq.api.core.Message;
import org.hornetq.api.core.SimpleString;
import org.hornetq.core.client.impl.ClientMessageImpl;
import org.hornetq.core.logging.Logger;
import org.hornetq.tests.util.RandomUtil;
import org.hornetq.tests.util.UnitTestCase;

/**
 * 
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 *
 */
public class MessageImplTest extends UnitTestCase
{
   private static final Logger log = Logger.getLogger(MessageImplTest.class);

   public void getSetAttributes()
   {
      for (int j = 0; j < 10; j++)
      {
         byte[] bytes = new byte[1000];
         for (int i = 0; i < bytes.length; i++)
         {
            bytes[i] = RandomUtil.randomByte();
         }

         final byte type = RandomUtil.randomByte();
         final boolean durable = RandomUtil.randomBoolean();
         final long expiration = RandomUtil.randomLong();
         final long timestamp = RandomUtil.randomLong();
         final byte priority = RandomUtil.randomByte();
         Message message1 = new ClientMessageImpl(type, durable, expiration, timestamp, priority, 100);

         Message message = message1;

         Assert.assertEquals(type, message.getType());
         Assert.assertEquals(durable, message.isDurable());
         Assert.assertEquals(expiration, message.getExpiration());
         Assert.assertEquals(timestamp, message.getTimestamp());
         Assert.assertEquals(priority, message.getPriority());

         final SimpleString destination = new SimpleString(RandomUtil.randomString());
         final boolean durable2 = RandomUtil.randomBoolean();
         final long expiration2 = RandomUtil.randomLong();
         final long timestamp2 = RandomUtil.randomLong();
         final byte priority2 = RandomUtil.randomByte();

         message.setAddress(destination);
         Assert.assertEquals(destination, message.getAddress());

         message.setDurable(durable2);
         Assert.assertEquals(durable2, message.isDurable());

         message.setExpiration(expiration2);
         Assert.assertEquals(expiration2, message.getExpiration());

         message.setTimestamp(timestamp2);
         Assert.assertEquals(timestamp2, message.getTimestamp());

         message.setPriority(priority2);
         Assert.assertEquals(priority2, message.getPriority());

      }
   }

   public void testExpired()
   {
      Message message = new ClientMessageImpl();

      Assert.assertEquals(0, message.getExpiration());
      Assert.assertFalse(message.isExpired());

      message.setExpiration(System.currentTimeMillis() + 1000);
      Assert.assertFalse(message.isExpired());

      message.setExpiration(System.currentTimeMillis() - 1);
      Assert.assertTrue(message.isExpired());

      message.setExpiration(System.currentTimeMillis() - 1000);
      Assert.assertTrue(message.isExpired());

      message.setExpiration(0);
      Assert.assertFalse(message.isExpired());
   }

   public void testProperties()
   {
      for (int j = 0; j < 10; j++)
      {
         Message msg = new ClientMessageImpl();

         SimpleString prop1 = new SimpleString("prop1");
         boolean val1 = RandomUtil.randomBoolean();
         msg.putBooleanProperty(prop1, val1);

         SimpleString prop2 = new SimpleString("prop2");
         byte val2 = RandomUtil.randomByte();
         msg.putByteProperty(prop2, val2);

         SimpleString prop3 = new SimpleString("prop3");
         byte[] val3 = RandomUtil.randomBytes();
         msg.putBytesProperty(prop3, val3);

         SimpleString prop4 = new SimpleString("prop4");
         double val4 = RandomUtil.randomDouble();
         msg.putDoubleProperty(prop4, val4);

         SimpleString prop5 = new SimpleString("prop5");
         float val5 = RandomUtil.randomFloat();
         msg.putFloatProperty(prop5, val5);

         SimpleString prop6 = new SimpleString("prop6");
         int val6 = RandomUtil.randomInt();
         msg.putIntProperty(prop6, val6);

         SimpleString prop7 = new SimpleString("prop7");
         long val7 = RandomUtil.randomLong();
         msg.putLongProperty(prop7, val7);

         SimpleString prop8 = new SimpleString("prop8");
         short val8 = RandomUtil.randomShort();
         msg.putShortProperty(prop8, val8);

         SimpleString prop9 = new SimpleString("prop9");
         SimpleString val9 = new SimpleString(RandomUtil.randomString());
         msg.putStringProperty(prop9, val9);

         Assert.assertEquals(9, msg.getPropertyNames().size());
         Assert.assertTrue(msg.getPropertyNames().contains(prop1));
         Assert.assertTrue(msg.getPropertyNames().contains(prop2));
         Assert.assertTrue(msg.getPropertyNames().contains(prop3));
         Assert.assertTrue(msg.getPropertyNames().contains(prop4));
         Assert.assertTrue(msg.getPropertyNames().contains(prop5));
         Assert.assertTrue(msg.getPropertyNames().contains(prop6));
         Assert.assertTrue(msg.getPropertyNames().contains(prop7));
         Assert.assertTrue(msg.getPropertyNames().contains(prop8));
         Assert.assertTrue(msg.getPropertyNames().contains(prop9));

         Assert.assertTrue(msg.containsProperty(prop1));
         Assert.assertTrue(msg.containsProperty(prop2));
         Assert.assertTrue(msg.containsProperty(prop3));
         Assert.assertTrue(msg.containsProperty(prop4));
         Assert.assertTrue(msg.containsProperty(prop5));
         Assert.assertTrue(msg.containsProperty(prop6));
         Assert.assertTrue(msg.containsProperty(prop7));
         Assert.assertTrue(msg.containsProperty(prop8));
         Assert.assertTrue(msg.containsProperty(prop9));

         Assert.assertEquals(val1, msg.getObjectProperty(prop1));
         Assert.assertEquals(val2, msg.getObjectProperty(prop2));
         Assert.assertEquals(val3, msg.getObjectProperty(prop3));
         Assert.assertEquals(val4, msg.getObjectProperty(prop4));
         Assert.assertEquals(val5, msg.getObjectProperty(prop5));
         Assert.assertEquals(val6, msg.getObjectProperty(prop6));
         Assert.assertEquals(val7, msg.getObjectProperty(prop7));
         Assert.assertEquals(val8, msg.getObjectProperty(prop8));
         Assert.assertEquals(val9, msg.getObjectProperty(prop9));

         SimpleString val10 = new SimpleString(RandomUtil.randomString());
         // test overwrite
         msg.putStringProperty(prop9, val10);
         Assert.assertEquals(val10, msg.getObjectProperty(prop9));

         int val11 = RandomUtil.randomInt();
         msg.putIntProperty(prop9, val11);
         Assert.assertEquals(val11, msg.getObjectProperty(prop9));

         msg.removeProperty(prop1);
         Assert.assertEquals(8, msg.getPropertyNames().size());
         Assert.assertTrue(msg.getPropertyNames().contains(prop2));
         Assert.assertTrue(msg.getPropertyNames().contains(prop3));
         Assert.assertTrue(msg.getPropertyNames().contains(prop4));
         Assert.assertTrue(msg.getPropertyNames().contains(prop5));
         Assert.assertTrue(msg.getPropertyNames().contains(prop6));
         Assert.assertTrue(msg.getPropertyNames().contains(prop7));
         Assert.assertTrue(msg.getPropertyNames().contains(prop8));
         Assert.assertTrue(msg.getPropertyNames().contains(prop9));

         msg.removeProperty(prop2);
         Assert.assertEquals(7, msg.getPropertyNames().size());
         Assert.assertTrue(msg.getPropertyNames().contains(prop3));
         Assert.assertTrue(msg.getPropertyNames().contains(prop4));
         Assert.assertTrue(msg.getPropertyNames().contains(prop5));
         Assert.assertTrue(msg.getPropertyNames().contains(prop6));
         Assert.assertTrue(msg.getPropertyNames().contains(prop7));
         Assert.assertTrue(msg.getPropertyNames().contains(prop8));
         Assert.assertTrue(msg.getPropertyNames().contains(prop9));

         msg.removeProperty(prop9);
         Assert.assertEquals(6, msg.getPropertyNames().size());
         Assert.assertTrue(msg.getPropertyNames().contains(prop3));
         Assert.assertTrue(msg.getPropertyNames().contains(prop4));
         Assert.assertTrue(msg.getPropertyNames().contains(prop5));
         Assert.assertTrue(msg.getPropertyNames().contains(prop6));
         Assert.assertTrue(msg.getPropertyNames().contains(prop7));
         Assert.assertTrue(msg.getPropertyNames().contains(prop8));

         msg.removeProperty(prop3);
         msg.removeProperty(prop4);
         msg.removeProperty(prop5);
         msg.removeProperty(prop6);
         msg.removeProperty(prop7);
         msg.removeProperty(prop8);
         Assert.assertEquals(0, msg.getPropertyNames().size());
      }
   }

   // Protected -------------------------------------------------------------------------------

   protected void assertMessagesEquivalent(final Message msg1, final Message msg2)
   {
      Assert.assertEquals(msg1.isDurable(), msg2.isDurable());

      Assert.assertEquals(msg1.getExpiration(), msg2.getExpiration());

      Assert.assertEquals(msg1.getTimestamp(), msg2.getTimestamp());

      Assert.assertEquals(msg1.getPriority(), msg2.getPriority());

      Assert.assertEquals(msg1.getType(), msg2.getType());

      UnitTestCase.assertEqualsByteArrays(msg1.getBodyBuffer().toByteBuffer().array(), msg2.getBodyBuffer()
                                                                                           .toByteBuffer()
                                                                                           .array());

      Assert.assertEquals(msg1.getAddress(), msg2.getAddress());

      Set<SimpleString> props1 = msg1.getPropertyNames();

      Set<SimpleString> props2 = msg2.getPropertyNames();

      Assert.assertEquals(props1.size(), props2.size());

      for (SimpleString propname : props1)
      {
         Object val1 = msg1.getObjectProperty(propname);

         Object val2 = msg2.getObjectProperty(propname);

         Assert.assertEquals(val1, val2);
      }
   }

   // Private ----------------------------------------------------------------------------------

}
