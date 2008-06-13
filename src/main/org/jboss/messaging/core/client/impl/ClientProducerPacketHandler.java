package org.jboss.messaging.core.client.impl;

import org.jboss.messaging.core.logging.Logger;
import org.jboss.messaging.core.remoting.Packet;
import org.jboss.messaging.core.remoting.PacketHandler;
import org.jboss.messaging.core.remoting.PacketReturner;
import org.jboss.messaging.core.remoting.impl.wireformat.EmptyPacket;
import org.jboss.messaging.core.remoting.impl.wireformat.ProducerFlowCreditMessage;

/**
 * 
 * A ClientProducerPacketHandler
 * 
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 *
 */
public class ClientProducerPacketHandler implements PacketHandler
{
   private static final Logger log = Logger.getLogger(ClientProducerPacketHandler.class);

   private final ClientProducerInternal clientProducer;

   private final long producerID;

   public ClientProducerPacketHandler(final ClientProducerInternal clientProducer, final long producerID)
   {
      this.clientProducer = clientProducer;
      
      this.producerID = producerID;
   }

   public long getID()
   {
      return producerID;
   }

   public void handle(final Packet packet, final PacketReturner sender)
   {    
      byte type = packet.getType();
      
      if (type == EmptyPacket.PROD_RECEIVETOKENS)
      {
         ProducerFlowCreditMessage message = (ProducerFlowCreditMessage) packet;
         
         try
         {
            clientProducer.receiveCredits(message.getTokens());
         }
         catch (Exception e)
         {
            log.error("Failed to handle packet " + packet, e);
         }
      }
      else
      {
      	throw new IllegalStateException("Invalid packet: " + type);
      }      
   }

   @Override
   public String toString()
   {
      return "ClientProducerPacketHandler[id=" + producerID + "]";
   }
   
   public boolean equals(Object other)
   {
      if (other instanceof ClientProducerPacketHandler == false)
      {
         return false;
      }
            
      ClientProducerPacketHandler r = (ClientProducerPacketHandler)other;
      
      return r.producerID == this.producerID;     
   }
}