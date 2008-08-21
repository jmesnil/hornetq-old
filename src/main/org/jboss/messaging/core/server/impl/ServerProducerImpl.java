/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005-2008, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.messaging.core.server.impl;

import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.messaging.core.logging.Logger;
import org.jboss.messaging.core.postoffice.FlowController;
import org.jboss.messaging.core.remoting.CommandManager;
import org.jboss.messaging.core.remoting.Packet;
import org.jboss.messaging.core.remoting.PacketDispatcher;
import org.jboss.messaging.core.remoting.impl.wireformat.ProducerFlowCreditMessage;
import org.jboss.messaging.core.server.ServerMessage;
import org.jboss.messaging.core.server.ServerProducer;
import org.jboss.messaging.core.server.ServerSession;
import org.jboss.messaging.util.SimpleString;

/**
 * 
 * A ServerProducerImpl
 * 
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 *
 */
public class ServerProducerImpl implements ServerProducer
{
	private static final Logger log = Logger.getLogger(ServerProducerImpl.class);
	
	private final long id;
	
	private final long clientTargetID;
	
	private final ServerSession session;
	
	private final SimpleString address;
	
	private final FlowController flowController;
	
	private final int windowSize;
	
	private volatile boolean waiting;
	
   private AtomicInteger creditsToSend = new AtomicInteger(0);
   
   private final CommandManager commandManager;
     	
	// Constructors ----------------------------------------------------------------
	
	public ServerProducerImpl(final ServerSession session, final long clientTargetID,
	                          final SimpleString address, 
			                    final FlowController flowController,
			                    final int windowSize,
			                    final PacketDispatcher dispatcher,
			                    final CommandManager commandManager) throws Exception
	{
		this.id = dispatcher.generateID();
		
		this.clientTargetID = clientTargetID;
      
		this.session = session;
		
		this.address = address;
		
		this.flowController = flowController;		
		
		this.windowSize = windowSize;
		
		this.commandManager = commandManager;
	}
	
	// ServerProducer implementation --------------------------------------------
	
	public long getID()
	{
		return id;
	}
	
	public void close() throws Exception
	{
		session.removeProducer(this);
	}
	
	public void send(final ServerMessage message) throws Exception
	{		
		if (this.address != null)
		{			
		   //Only do flow control with non anonymous producers
		   
			if (flowController != null)
		   {
			   int creds = creditsToSend.addAndGet(message.getEncodeSize());
			   
			   if (creds >= windowSize)
			   {
			      requestAndSendCredits();
			   }
			}
		}
		
		session.send(message);  		
	}
	
	public void requestAndSendCredits() throws Exception
	{	 
	   if (!waiting)
	   {
	      flowController.requestAndSendCredits(this, creditsToSend.get());
	   }
	}

	public void sendCredits(final int credits) throws Exception
	{
	   creditsToSend.addAndGet(-credits);
	   
		Packet packet = new ProducerFlowCreditMessage(credits);
		
		commandManager.sendCommandOneway(clientTargetID, packet);	
	}
	
	public void setWaiting(final boolean waiting)
	{
		this.waiting = waiting;
	}
	
	public boolean isWaiting()
	{
		return waiting;
	}
}
