/**
 * JBoss, the OpenSource J2EE WebOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.messaging.core.message;



import org.jboss.messaging.core.MessageReference;
import org.jboss.messaging.core.Message;

import java.io.Serializable;
import java.util.Iterator;

/**
 * A simple MessageReference implementation.
 *
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 * @version <tt>$Revision$</tt>
 */
public class MessageReferenceSupport extends RoutableSupport implements MessageReference
{
   // Attributes ----------------------------------------------------

   protected Serializable storeID;

   // Constructors --------------------------------------------------

   /**
    * @param messageID
    */
   public MessageReferenceSupport(Serializable messageID)
   {
      super(messageID);
   }

   public MessageReferenceSupport(Serializable messageID, boolean reliable,
                                  long expirationTime, Serializable storeID)
   {
      super(messageID, reliable, expirationTime);
      this.storeID = storeID;
   }

   /**
    * Creates a reference based on a given message.
    */
   public MessageReferenceSupport(Message m, Serializable storeID)
   {
      this(m.getMessageID(), m.isReliable(), m.getExpiration(), storeID);
      for(Iterator i = m.getHeaderNames().iterator(); i.hasNext(); )
      {
         String name = (String)i.next();
         putHeader(name, m.getHeader(name));
      }
   }

   // Message implementation ----------------------------------------

   public Serializable getStoreID()
   {
      return storeID;
   }

   // Public --------------------------------------------------------

   public boolean equals(Object o)
   {
      if (this == o)
      {
         return true;
      }
      if (!(o instanceof MessageReferenceSupport))
      {
         return false;
      }
      MessageReferenceSupport that = (MessageReferenceSupport)o;
      if (messageID == null)
      {
         return that.messageID == null;
      }
      return messageID.equals(that.messageID);
   }

   public int hashCode()
   {
      if (messageID == null)
      {
         return 0;
      }
      return messageID.hashCode();
   }

   public String toString()
   {
      return "MRef["+messageID+"]";
   }
}
