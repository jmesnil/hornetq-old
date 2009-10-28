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

package org.hornetq.core.remoting.impl.wireformat;

import org.hornetq.core.remoting.spi.HornetQBuffer;
import org.hornetq.utils.DataConstants;
import org.hornetq.utils.SimpleString;

/**
 * A ReplicationPageWrite
 *
 * @author <mailto:clebert.suconic@jboss.org">Clebert Suconic</a>
 *
 *
 */
public class ReplicationPageEventMessage extends PacketImpl
{

   private int pageNumber;

   private SimpleString storeName;

   /**
    * True = delete page, False = close page
    */
   private boolean isDelete;

   // Constants -----------------------------------------------------

   // Attributes ----------------------------------------------------

   // Static --------------------------------------------------------

   // Constructors --------------------------------------------------

   public ReplicationPageEventMessage()
   {
      super(REPLICATION_PAGE_EVENT);
   }

   public ReplicationPageEventMessage(final SimpleString storeName, final int pageNumber, final boolean isDelete)
   {
      this();
      this.pageNumber = pageNumber;
      this.isDelete = isDelete;
      this.storeName = storeName;
   }

   // Public --------------------------------------------------------

   @Override
   public int getRequiredBufferSize()
   {
      return BASIC_PACKET_SIZE + DataConstants.SIZE_INT + storeName.sizeof() + DataConstants.SIZE_BOOLEAN;

   }

   @Override
   public void encodeBody(final HornetQBuffer buffer)
   {
      buffer.writeSimpleString(storeName);
      buffer.writeInt(pageNumber);
      buffer.writeBoolean(isDelete);
   }

   @Override
   public void decodeBody(final HornetQBuffer buffer)
   {
      storeName = buffer.readSimpleString();
      pageNumber = buffer.readInt();
      isDelete = buffer.readBoolean();
   }

   /**
    * @return the pageNumber
    */
   public int getPageNumber()
   {
      return pageNumber;
   }

   /**
    * @return the storeName
    */
   public SimpleString getStoreName()
   {
      return storeName;
   }

   /**
    * @return the isDelete
    */
   public boolean isDelete()
   {
      return isDelete;
   }

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------

}