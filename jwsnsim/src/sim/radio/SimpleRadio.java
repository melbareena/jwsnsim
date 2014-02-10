/*
 * Copyright (c) 2003, Vanderbilt University
 * All rights reserved.
 *
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose, without fee, and without written agreement is
 * hereby granted, provided that the above copyright notice, the following
 * two paragraphs and the author appear in all copies of this software.
 * 
 * IN NO EVENT SHALL THE VANDERBILT UNIVERSITY BE LIABLE TO ANY PARTY FOR
 * DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT
 * OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF THE VANDERBILT
 * UNIVERSITY HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * THE VANDERBILT UNIVERSITY SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE.  THE SOFTWARE PROVIDED HEREUNDER IS
 * ON AN "AS IS" BASIS, AND THE VANDERBILT UNIVERSITY HAS NO OBLIGATION TO
 * PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 *
 * Author: Gyorgy Balogh, Gabor Pap, Miklos Maroti
 * Date last modified: 02/09/04
 */

package sim.radio;

import sim.configuration.TransmissionConfiguration;
import sim.node.Node;
import sim.node.NodeFactory;
import sim.node.Position;
import sim.simulator.Event;
import sim.simulator.EventObserver;
import sim.type.UInt32;

/**
 * This radio model determines a quotient q = s / (i+n) between the received
 * signal and the sum of the ambient background noise n and the interference caused
 * by all concurrent transmissions. The transmission succeeds if q > beta, where beta
 * is a small constant. 
 * 
 * This model assumes that the intensity of an electric signal decays exponentially 
 * with the distance from the sender. This decrease is parameterized by the path-loss 
 * exponent alpha: Intensity(r) = sendPower/r^alpha. The value of alpha is often chosen
 * in the range between 2 and 6.
 * 
 * To the interference caused by concurrent transmissions, we add an ambient noise 
 * level N.
 * 
 */
public class SimpleRadio extends Radio implements EventObserver{	
	
	/** owner node */
	protected Node node;

	/** The vector of the neighboring nodes. */
	protected Node[] neighbors; 
	

	
	protected RadioPacket packetToTransmit = null;
	protected RadioPacket receivingPacket = null;

	/**	Signal stregth of transmitting or parent node.  */
	private double signalStrength = 0;

	/**	Noise generated by other nodes. */
	private double noiseStrength = 0;	

	/** State variable, true if the last received message got corrupted by noise */
	protected boolean   corrupted        = false;

	protected boolean receiving = false;
	protected boolean transmitting = false;

	public static int    sendTransmissionTime          = 960;
	
	private Event endTransmissionEvent = new Event(this);
	
	private RadioListener listener;	

	public SimpleRadio(Node node,RadioListener listener){
		this.node = node;
		this.listener= listener;
	}
	
	public void on() {		
		for(int i=0; i<NodeFactory.nodes.length;i++){
			((SimpleRadio)(NodeFactory.nodes[i].getRadio())).updateNeighborhood();
		}
	}

	public void updateNeighborhood() {
  
        Node[] neighbors = new Node[NodeFactory.numNodes];

        int i = 0;
        
        
        for (int j = 0; j<NodeFactory.numNodes; j++){
        	Node node1 = NodeFactory.nodes[j];
        	
        	if(node1.isRunning()){
				double distance = node.getPosition().distanceTo(node1.getPosition());
				if( distance <= TransmissionConfiguration.MAX_DISTANCE && node != node1){
					neighbors[i] = node1;                   
					i++;
				}				
			}
        }
        
		this.neighbors = new Node[i];
        System.arraycopy( neighbors, 0,this.neighbors, 0, i );
        
//        System.out.println("---------------------------------");
//        System.out.print(node.getID() + "'s neighbors: ");
//        for (int j = 0; j < this.neighbors.length; j++) {
//        	System.out.print(this.neighbors[j].getID() + " ");
//		}
//        System.out.println("");
//        System.out.println("---------------------------------");
        
	}
	


	public void beginTransmission(RadioPacket packet){	
		
		packetToTransmit = packet;
		setTransmissionTimestamp();
		packet.setIntensity(intensity);
		
		transmitting = true;
		
		int i = neighbors.length;
		while( --i >= 0 ){
			neighbors[i].getRadio().receptionBegin(packet);
		}
		
		endTransmissionEvent.register(sendTransmissionTime);
		listener.radioTransmissionBegin();
	}

	private void setTransmissionTimestamp() {	
		UInt32 age = node.getClock().getValue();
		age = age.subtract(packetToTransmit.getEventTime());
//		System.out.println("Packet age " + age.getValue());
		packetToTransmit.setEventTime(age);
	}
	
	public void endTransmission(){	
		int i = neighbors.length;
		while( --i >= 0 )
			neighbors[i].getRadio().receptionEnd(packetToTransmit);
		
		packetToTransmit = null;
		transmitting = false;
		listener.radioTransmissionEnd();
	}
	
	protected double getNoise(RadioPacket packet) {
		Position pos = packet.getSender().getPosition();
		double distance = pos.distanceTo(node.getPosition());
		double poweredDistance = Math.pow(distance, TransmissionConfiguration.alpha);
		return packet.getIntensity() / poweredDistance;
	}
	
	public void receptionBegin(RadioPacket packet) {
		
        if(receiving){
			noiseStrength += getNoise(packet);
			
            if(isMessageCorrupted())
                corrupted = true;
        } else{
            if(!transmitting){
                // start receiving
            	receivingPacket = new RadioPacket((RadioPacket)packet);
            	setReceptionTimestamp();
                receiving		= true;
                corrupted     	= false;
                signalStrength 	= getNoise(packet);
                noiseStrength = TransmissionConfiguration.ambientNoise;
                
                listener.radioReceptionBegin();
            }
            else{
                noiseStrength += getNoise(packet);
            }
        }
	}

	private void setReceptionTimestamp() {
		UInt32 timestamp = node.getClock().getValue();
		receivingPacket.setTimestamp(timestamp);
//		System.out.println("Receiving age: " + receivingPacket.getEventTime().getValue());
		timestamp  = timestamp.subtract(receivingPacket.getEventTime());
		receivingPacket.setEventTime(timestamp);
	}

	/**
	 * Calls the {@link Mica2NodeNonCSMA#removeNoise} method. See also 
	 * {@link Node#receptionEnd} for more information.
	 */
	public void receptionEnd(RadioPacket packet) {
		
		if(receivingPacket != null && receivingPacket.equals(packet)){            
            receiving = false;
            
            if(!corrupted){
            	node.getMAC().receivePacket(receivingPacket);
            }            
            else
            	System.out.println("Corruption!");
            
            receivingPacket = null;
            signalStrength = 0;
            noiseStrength -= TransmissionConfiguration.ambientNoise;
            listener.radioReceptionEnd();                        
        }
		else{
			noiseStrength -= getNoise(packet);
		}
	}
	
	public boolean isMessageCorrupted( ){
		return signalStrength < TransmissionConfiguration.beta * noiseStrength; 
	}

	@Override
	public void signal(Event event) {
		if( event == endTransmissionEvent){						
			endTransmission();
		}		
	}

	@Override
	protected boolean isChannelFree() {
		
		return true;
	}

	@Override
	public Node[] getNeighbors() {
		// TODO Auto-generated method stub
		return neighbors;
	}
}

