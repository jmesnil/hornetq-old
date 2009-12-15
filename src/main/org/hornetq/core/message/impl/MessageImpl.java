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

package org.hornetq.core.message.impl;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hornetq.core.buffers.HornetQBuffer;
import org.hornetq.core.buffers.HornetQBuffers;
import org.hornetq.core.buffers.impl.ResetLimitWrappedHornetQBuffer;
import org.hornetq.core.client.LargeMessageBuffer;
import org.hornetq.core.exception.HornetQException;
import org.hornetq.core.logging.Logger;
import org.hornetq.core.message.BodyEncoder;
import org.hornetq.core.message.PropertyConversionException;
import org.hornetq.core.remoting.impl.wireformat.PacketImpl;
import org.hornetq.utils.DataConstants;
import org.hornetq.utils.SimpleString;
import org.hornetq.utils.TypedProperties;

/**
 * A concrete implementation of a message
 *
 * All messages handled by HornetQ core are of this type
 *
 * @author <a href="mailto:ovidiu@feodorov.com">Ovidiu Feodorov</a>
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 * @author <a href="mailto:clebert.suconic@jboss.com">Clebert Suconic</a>
 * @author <a href="mailto:ataylor@redhat.com">Andy Taylor</a>
 * @version <tt>$Revision: 2740 $</tt>
 *
 *
 * $Id: MessageSupport.java 2740 2007-05-30 11:36:28Z timfox $
 */
public abstract class MessageImpl implements MessageInternal
{
   // Constants -----------------------------------------------------

   private static final Logger log = Logger.getLogger(MessageImpl.class);

   public static final SimpleString HDR_ACTUAL_EXPIRY_TIME = new SimpleString("_HQ_ACTUAL_EXPIRY");

   public static final SimpleString HDR_ORIGINAL_ADDRESS = new SimpleString("_HQ_ORIG_ADDRESS");

   public static final SimpleString HDR_ORIG_MESSAGE_ID = new SimpleString("_HQ_ORIG_MESSAGE_ID");

   public static final SimpleString HDR_GROUP_ID = new SimpleString("_HQ_GROUP_ID");

   public static final SimpleString HDR_SCHEDULED_DELIVERY_TIME = new SimpleString("_HQ_SCHED_DELIVERY");

   public static final SimpleString HDR_DUPLICATE_DETECTION_ID = new SimpleString("_HQ_DUPL_ID");

   public static final SimpleString HDR_ROUTE_TO_IDS = new SimpleString("_HQ_ROUTE_TO");

   public static final SimpleString HDR_FROM_CLUSTER = new SimpleString("_HQ_FROM_CLUSTER");

   public static final SimpleString HDR_LAST_VALUE_NAME = new SimpleString("_HQ_LVQ_NAME");

   // Attributes ----------------------------------------------------

   protected long messageID;

   protected SimpleString address;

   protected byte type;

   protected boolean durable;

   /** GMT milliseconds at which this message expires. 0 means never expires * */
   protected long expiration;

   protected long timestamp;

   protected TypedProperties properties;

   protected byte priority;

   protected HornetQBuffer buffer;

   protected ResetLimitWrappedHornetQBuffer bodyBuffer;

   protected boolean bufferValid;

   private int endOfBodyPosition = -1;

   private int endOfMessagePosition;

   private boolean copied = true;

   private boolean bufferUsed;

   // Constructors --------------------------------------------------

   protected MessageImpl()
   {
      properties = new TypedProperties();
   }

   /**
    * overridden by the client message, we need access to the connection so we can create the appropriate HornetQBuffer.
    * @param type
    * @param durable
    * @param expiration
    * @param timestamp
    * @param priority
    * @param initialMessageBufferSize
    */
   protected MessageImpl(final byte type,
                         final boolean durable,
                         final long expiration,
                         final long timestamp,
                         final byte priority,
                         final int initialMessageBufferSize)
   {
      this();
      this.type = type;
      this.durable = durable;
      this.expiration = expiration;
      this.timestamp = timestamp;
      this.priority = priority;
      createBody(initialMessageBufferSize);
   }

   protected MessageImpl(final long messageID, final int initialMessageBufferSize)
   {
      this(initialMessageBufferSize);
      this.messageID = messageID;
   }

   protected MessageImpl(final int initialMessageBufferSize)
   {
      this();
      createBody(initialMessageBufferSize);
   }

   /*
    * Copy constructor
    */
   protected MessageImpl(final MessageImpl other)
   {
      messageID = other.getMessageID();
      address = other.getAddress();
      type = other.getType();
      durable = other.isDurable();
      expiration = other.getExpiration();
      timestamp = other.getTimestamp();
      priority = other.getPriority();
      properties = new TypedProperties(other.getProperties());

      bufferValid = other.bufferValid;
      endOfBodyPosition = other.endOfBodyPosition;
      endOfMessagePosition = other.endOfMessagePosition;
      copied = other.copied;

      if (other.buffer != null)
      {
         createBody(other.buffer.capacity());
         // We need to copy the underlying buffer too, since the different messsages thereafter might have different
         // properties set on them, making their encoding different
         buffer = other.buffer.copy(0, other.buffer.capacity());
         buffer.setIndex(other.buffer.readerIndex(), other.buffer.writerIndex());
      }
   }

   // Message implementation ----------------------------------------

   public int getEncodeSize()
   {
      int headersPropsSize = getHeadersAndPropertiesEncodeSize();

      int bodyPos = endOfBodyPosition == -1 ? buffer.writerIndex() : endOfBodyPosition;

      int bodySize = bodyPos - PacketImpl.PACKET_HEADERS_SIZE - DataConstants.SIZE_INT;

      return DataConstants.SIZE_INT + bodySize + DataConstants.SIZE_INT + headersPropsSize;
   }

   public int getHeadersAndPropertiesEncodeSize()
   {
      return DataConstants.SIZE_LONG + // Message ID
             /* address */SimpleString.sizeofString(address) +
             DataConstants./* Type */SIZE_BYTE +
             DataConstants./* Durable */SIZE_BOOLEAN +
             DataConstants./* Expiration */SIZE_LONG +
             DataConstants./* Timestamp */SIZE_LONG +
             DataConstants./* Priority */SIZE_BYTE +
             /* PropertySize and Properties */properties.getEncodeSize();
   }

   public void encodeHeadersAndProperties(final HornetQBuffer buffer)
   {
      buffer.writeLong(messageID);
      buffer.writeSimpleString(address);
      buffer.writeByte(type);
      buffer.writeBoolean(durable);
      buffer.writeLong(expiration);
      buffer.writeLong(timestamp);
      buffer.writeByte(priority);
      properties.encode(buffer);
   }

   public void decodeHeadersAndProperties(final HornetQBuffer buffer)
   {
      messageID = buffer.readLong();
      address = buffer.readSimpleString();
      type = buffer.readByte();
      durable = buffer.readBoolean();
      expiration = buffer.readLong();
      timestamp = buffer.readLong();
      priority = buffer.readByte();
      properties.decode(buffer);
   }

   public HornetQBuffer getBodyBuffer()
   {
      if (bodyBuffer == null)
      {
         if (buffer instanceof LargeMessageBuffer == false)
         {
            bodyBuffer = new ResetLimitWrappedHornetQBuffer(PacketImpl.PACKET_HEADERS_SIZE + DataConstants.SIZE_INT,
                                                            buffer,
                                                            this);
         }
         else
         {
            return buffer;
         }
      }

      return bodyBuffer;
   }

   public long getMessageID()
   {
      return messageID;
   }

   public SimpleString getAddress()
   {
      return address;
   }

   public void setAddress(final SimpleString address)
   {
      if (this.address != address)
      {
         this.address = address;

         bufferValid = false;
      }
   }

   public byte getType()
   {
      return type;
   }

   public boolean isDurable()
   {
      return durable;
   }

   public void setDurable(final boolean durable)
   {
      if (this.durable != durable)
      {
         this.durable = durable;

         bufferValid = false;
      }
   }

   public long getExpiration()
   {
      return expiration;
   }

   public void setExpiration(final long expiration)
   {
      if (this.expiration != expiration)
      {
         this.expiration = expiration;

         bufferValid = false;
      }
   }

   public long getTimestamp()
   {
      return timestamp;
   }

   public void setTimestamp(final long timestamp)
   {
      if (this.timestamp != timestamp)
      {
         this.timestamp = timestamp;

         bufferValid = false;
      }
   }

   public byte getPriority()
   {
      return priority;
   }

   public void setPriority(final byte priority)
   {
      if (this.priority != priority)
      {
         this.priority = priority;

         bufferValid = false;
      }
   }

   public boolean isExpired()
   {
      if (expiration == 0)
      {
         return false;
      }

      return System.currentTimeMillis() - expiration >= 0;
   }

   public Map<String, Object> toMap()
   {
      Map<String, Object> map = new HashMap<String, Object>();

      map.put("messageID", messageID);
      map.put("address", address.toString());
      map.put("type", type);
      map.put("durable", durable);
      map.put("expiration", expiration);
      map.put("timestamp", timestamp);
      map.put("priority", priority);
      for (SimpleString propName : properties.getPropertyNames())
      {
         map.put(propName.toString(), properties.getProperty(propName));
      }
      return map;
   }

   public void decodeFromBuffer(final HornetQBuffer buffer)
   {
      this.buffer = buffer;

      decode();
   }

   public void bodyChanged()
   {
      // If the body is changed we must copy the buffer otherwise can affect the previously sent message
      // which might be in the Netty write queue
      checkCopy();

      bufferValid = false;

      endOfBodyPosition = -1;
   }

   public synchronized void checkCopy()
   {
      if (!copied)
      {
         forceCopy();

         copied = true;
      }
   }

   public void resetCopied()
   {
      copied = false;
   }

   public int getEndOfMessagePosition()
   {
      return endOfMessagePosition;
   }

   public int getEndOfBodyPosition()
   {
      return endOfBodyPosition;
   }

   // Encode to journal or paging
   public void encode(final HornetQBuffer buff)
   {
      encodeToBuffer();

      buff.writeBytes(buffer, PacketImpl.PACKET_HEADERS_SIZE, endOfMessagePosition - PacketImpl.PACKET_HEADERS_SIZE);
   }

   // Decode from journal or paging
   public void decode(final HornetQBuffer buff)
   {
      int start = buff.readerIndex();

      endOfBodyPosition = buff.readInt();

      endOfMessagePosition = buff.getInt(endOfBodyPosition - PacketImpl.PACKET_HEADERS_SIZE + start);

      int length = endOfMessagePosition - PacketImpl.PACKET_HEADERS_SIZE;

      buffer.setIndex(0, PacketImpl.PACKET_HEADERS_SIZE);

      buffer.writeBytes(buff, start, length);

      decode();

      buff.readerIndex(start + length);
   }

   public synchronized HornetQBuffer getEncodedBuffer()
   {
      HornetQBuffer buff = encodeToBuffer();

      if (bufferUsed)
      {
         HornetQBuffer copied = buff.copy(0, buff.capacity());

         copied.setIndex(0, endOfMessagePosition);

         return copied;
      }
      else
      {
         buffer.setIndex(0, endOfMessagePosition);

         bufferUsed = true;
         
         return buffer;
      }
   }

   // Properties
   // ---------------------------------------------------------------------------------------

   public void putBooleanProperty(final SimpleString key, final boolean value)
   {
      properties.putBooleanProperty(key, value);

      bufferValid = false;
   }

   public void putByteProperty(final SimpleString key, final byte value)
   {
      properties.putByteProperty(key, value);

      bufferValid = false;
   }

   public void putBytesProperty(final SimpleString key, final byte[] value)
   {
      properties.putBytesProperty(key, value);

      bufferValid = false;
   }

   public void putShortProperty(final SimpleString key, final short value)
   {
      properties.putShortProperty(key, value);

      bufferValid = false;
   }

   public void putIntProperty(final SimpleString key, final int value)
   {
      properties.putIntProperty(key, value);

      bufferValid = false;
   }

   public void putLongProperty(final SimpleString key, final long value)
   {
      properties.putLongProperty(key, value);

      bufferValid = false;
   }

   public void putFloatProperty(final SimpleString key, final float value)
   {
      properties.putFloatProperty(key, value);

      bufferValid = false;
   }

   public void putDoubleProperty(final SimpleString key, final double value)
   {
      properties.putDoubleProperty(key, value);

      bufferValid = false;
   }

   public void putStringProperty(final SimpleString key, final SimpleString value)
   {
      properties.putSimpleStringProperty(key, value);

      bufferValid = false;
   }

   public void putObjectProperty(final SimpleString key, final Object value) throws PropertyConversionException
   {
      if (value == null)
      {
         // This is ok - when we try to read the same key it will return null too

         properties.removeProperty(key);
      }
      else if (value instanceof Boolean)
      {
         properties.putBooleanProperty(key, (Boolean)value);
      }
      else if (value instanceof Byte)
      {
         properties.putByteProperty(key, (Byte)value);
      }
      else if (value instanceof Short)
      {
         properties.putShortProperty(key, (Short)value);
      }
      else if (value instanceof Integer)
      {
         properties.putIntProperty(key, (Integer)value);
      }
      else if (value instanceof Long)
      {
         properties.putLongProperty(key, (Long)value);
      }
      else if (value instanceof Float)
      {
         properties.putFloatProperty(key, (Float)value);
      }
      else if (value instanceof Double)
      {
         properties.putDoubleProperty(key, (Double)value);
      }
      else if (value instanceof String)
      {
         properties.putSimpleStringProperty(key, new SimpleString((String)value));
      }
      else if (value instanceof SimpleString)
      {
         properties.putSimpleStringProperty(key, (SimpleString)value);
      }
      else
      {
         throw new PropertyConversionException(value.getClass() + " is not a valid property type");
      }

      bufferValid = false;
   }

   public void putObjectProperty(final String key, final Object value) throws PropertyConversionException
   {
      putObjectProperty(new SimpleString(key), value);

      bufferValid = false;
   }

   public void putBooleanProperty(final String key, final boolean value)
   {
      properties.putBooleanProperty(new SimpleString(key), value);

      bufferValid = false;
   }

   public void putByteProperty(final String key, final byte value)
   {
      properties.putByteProperty(new SimpleString(key), value);

      bufferValid = false;
   }

   public void putBytesProperty(final String key, final byte[] value)
   {
      properties.putBytesProperty(new SimpleString(key), value);

      bufferValid = false;
   }

   public void putShortProperty(final String key, final short value)
   {
      properties.putShortProperty(new SimpleString(key), value);

      bufferValid = false;
   }

   public void putIntProperty(final String key, final int value)
   {
      properties.putIntProperty(new SimpleString(key), value);

      bufferValid = false;
   }

   public void putLongProperty(final String key, final long value)
   {
      properties.putLongProperty(new SimpleString(key), value);

      bufferValid = false;
   }

   public void putFloatProperty(final String key, final float value)
   {
      properties.putFloatProperty(new SimpleString(key), value);

      bufferValid = false;
   }

   public void putDoubleProperty(final String key, final double value)
   {
      properties.putDoubleProperty(new SimpleString(key), value);

      bufferValid = false;
   }

   public void putStringProperty(final String key, final String value)
   {
      properties.putSimpleStringProperty(new SimpleString(key), new SimpleString(value));

      bufferValid = false;
   }

   public void putTypedProperties(final TypedProperties otherProps)
   {
      properties.putTypedProperties(otherProps);

      bufferValid = false;
   }

   public Object getObjectProperty(final SimpleString key)
   {
      return properties.getProperty(key);
   }

   public Boolean getBooleanProperty(final SimpleString key) throws PropertyConversionException
   {
      return properties.getBooleanProperty(key);
   }

   public Boolean getBooleanProperty(final String key) throws PropertyConversionException
   {
      return properties.getBooleanProperty(new SimpleString(key));
   }

   public Byte getByteProperty(final SimpleString key) throws PropertyConversionException
   {
      return properties.getByteProperty(key);
   }

   public Byte getByteProperty(final String key) throws PropertyConversionException
   {
      return properties.getByteProperty(new SimpleString(key));
   }

   public byte[] getBytesProperty(final SimpleString key) throws PropertyConversionException
   {
      return properties.getBytesProperty(key);
   }

   public byte[] getBytesProperty(final String key) throws PropertyConversionException
   {
      return getBytesProperty(new SimpleString(key));
   }

   public Double getDoubleProperty(final SimpleString key) throws PropertyConversionException
   {
      return properties.getDoubleProperty(key);
   }

   public Double getDoubleProperty(final String key) throws PropertyConversionException
   {
      return properties.getDoubleProperty(new SimpleString(key));
   }

   public Integer getIntProperty(final SimpleString key) throws PropertyConversionException
   {
      return properties.getIntProperty(key);
   }

   public Integer getIntProperty(final String key) throws PropertyConversionException
   {
      return properties.getIntProperty(new SimpleString(key));
   }

   public Long getLongProperty(final SimpleString key) throws PropertyConversionException
   {
      return properties.getLongProperty(key);
   }

   public Long getLongProperty(final String key) throws PropertyConversionException
   {
      return properties.getLongProperty(new SimpleString(key));
   }

   public Short getShortProperty(final SimpleString key) throws PropertyConversionException
   {
      return properties.getShortProperty(key);
   }

   public Short getShortProperty(final String key) throws PropertyConversionException
   {
      return properties.getShortProperty(new SimpleString(key));
   }

   public Float getFloatProperty(final SimpleString key) throws PropertyConversionException
   {
      return properties.getFloatProperty(key);
   }

   public Float getFloatProperty(final String key) throws PropertyConversionException
   {
      return properties.getFloatProperty(new SimpleString(key));
   }

   public String getStringProperty(final SimpleString key) throws PropertyConversionException
   {
      SimpleString str = getSimpleStringProperty(key);

      if (str == null)
      {
         return null;
      }
      else
      {
         return str.toString();
      }
   }

   public String getStringProperty(final String key) throws PropertyConversionException
   {
      return getStringProperty(new SimpleString(key));
   }

   public SimpleString getSimpleStringProperty(final SimpleString key) throws PropertyConversionException
   {
      return properties.getSimpleStringProperty(key);
   }

   public SimpleString getSimpleStringProperty(final String key) throws PropertyConversionException
   {
      return properties.getSimpleStringProperty(new SimpleString(key));
   }

   public Object getObjectProperty(final String key)
   {
      return properties.getProperty(new SimpleString(key));
   }

   public Object removeProperty(final SimpleString key)
   {
      bufferValid = false;

      return properties.removeProperty(key);
   }

   public Object removeProperty(final String key)
   {
      bufferValid = false;

      return properties.removeProperty(new SimpleString(key));
   }

   public boolean containsProperty(final SimpleString key)
   {
      return properties.containsProperty(key);
   }

   public boolean containsProperty(final String key)
   {
      return properties.containsProperty(new SimpleString(key));
   }

   public Set<SimpleString> getPropertyNames()
   {
      return properties.getPropertyNames();
   }

   public HornetQBuffer getWholeBuffer()
   {
      return buffer;
   }

   public BodyEncoder getBodyEncoder() throws HornetQException
   {
      return new DecodingContext();
   }

   // Public --------------------------------------------------------

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   // Private -------------------------------------------------------


   private TypedProperties getProperties()
   {
      return properties;
   }
   
   // This must be synchronized as it can be called concurrently id the message is being delivered concurently to
   // many queues - the first caller in this case will actually encode it
   private synchronized HornetQBuffer encodeToBuffer()
   {
      if (!bufferValid)
      {
         if (bufferUsed)
         {
            // Cannot use same buffer - must copy

            forceCopy();
         }

         if (endOfBodyPosition == -1)
         {
            // Means sending message for first time
            endOfBodyPosition = buffer.writerIndex();
         }

         // write it
         buffer.setInt(PacketImpl.PACKET_HEADERS_SIZE, endOfBodyPosition);

         // Position at end of body and skip past the message end position int.
         // check for enough room in the buffer even tho it is dynamic
         if((endOfBodyPosition + 4) > buffer.capacity())
         {
            buffer.setIndex(0, endOfBodyPosition);
            buffer.writeInt(0);
         }
         else
         {
            buffer.setIndex(0, endOfBodyPosition + DataConstants.SIZE_INT);
         }

         encodeHeadersAndProperties(buffer);

         // Write end of message position

         endOfMessagePosition = buffer.writerIndex();

         buffer.setInt(endOfBodyPosition, endOfMessagePosition);

         bufferValid = true;
      }

      return buffer;
   }

   private void decode()
   {
      endOfBodyPosition = buffer.getInt(PacketImpl.PACKET_HEADERS_SIZE);

      buffer.readerIndex(endOfBodyPosition + DataConstants.SIZE_INT);

      decodeHeadersAndProperties(buffer);

      endOfMessagePosition = buffer.readerIndex();

      bufferValid = true;
   }

   private void createBody(final int initialMessageBufferSize)
   {
      buffer = HornetQBuffers.dynamicBuffer(initialMessageBufferSize);

      // There's a bug in netty which means a dynamic buffer won't resize until you write a byte
      buffer.writeByte((byte)0);

      int limit = PacketImpl.PACKET_HEADERS_SIZE + DataConstants.SIZE_INT;

      buffer.setIndex(limit, limit);
   }

   private void forceCopy()
   {
      // Must copy buffer before sending it

      buffer = buffer.copy(0, buffer.capacity());

      buffer.setIndex(0, endOfBodyPosition);

      if (bodyBuffer != null)
      {
         bodyBuffer.setBuffer(buffer);
      }

      bufferUsed = false;
   }

   // Inner classes -------------------------------------------------

   private final class DecodingContext implements BodyEncoder
   {
      private int lastPos = 0;

      public DecodingContext()
      {
      }

      public void open()
      {
      }

      public void close()
      {
      }

      public long getLargeBodySize()
      {
         return buffer.writerIndex();
      }

      public int encode(final ByteBuffer bufferRead) throws HornetQException
      {
         HornetQBuffer buffer = HornetQBuffers.wrappedBuffer(bufferRead);
         return encode(buffer, bufferRead.capacity());
      }

      public int encode(final HornetQBuffer bufferOut, final int size)
      {
         bufferOut.writeBytes(getWholeBuffer(), lastPos, size);
         lastPos += size;
         return size;
      }
   }

}
