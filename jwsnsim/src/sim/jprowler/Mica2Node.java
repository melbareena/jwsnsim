/*
 * Copyright (c) 2002, Vanderbilt University
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
package sim.jprowler;

import sim.jprowler.UInt32;
import sim.jprowler.clock.Clock;

/**
 * This class represents a mote and all its properties important from the
 * simulation point of view. The MAC layer specific constant are all defined and
 * used here.
 * 
 * @author Gyorgy Balogh, Gabor Pap, Miklos Maroti
 */
public class Mica2Node extends Node {
	/**
	 * In this simulation not messages but references to motes are passed. All
	 * this means is that the Mica2Node has to hold the information on the
	 * sender application which runs on this very mote.
	 */
	protected Protocol senderApplication = null;

	 /**
	 * This node is the one that sent the last message or the one this node is
	 * receiving a message from right now. It is mainly used for display
	 purposes,
	 * as you know this information is not embedded into any TinyOS message.
	 */
	 protected Node senderNode = null;

	/**
	 * This is the message being sent, on reception it is extracted and the
	 * message part is forwarded to the appropriate application, see
	 * {@link Protocol#receiveMessage}.
	 */
	protected RadioPacket sentPacket = null;
	
	protected RadioPacket receivedPacket = null;

	// //////////////////////////////
	// STATE VARIABLES
	// //////////////////////////////

	/**
	 * State variable, true if radio is in sending mode, this means it has a one
	 * message long buffer, which is full and the Node is trying to transmit its
	 * content.
	 */
	protected boolean sending = false;

	/** State variable, true if the radio is transmitting a message right now. */
	protected boolean transmitting = false;

	/** State variable, true if the radio is in receiving mode */
	protected boolean receiving = false;

	/** State variable, true if the last received message got corrupted by noise */
	protected boolean corrupted = false;

	/**
	 * State variable, true if radio failed to transmit a message do to high
	 * radio traffic, this means it has to retry it later, which is done using
	 * the {@link Mica2Node#generateBackOffTime} function.
	 */
	protected boolean sendingPostponed = false;

	// //////////////////////////////
	// MAC layer specific constants
	// //////////////////////////////

	/** The constant component of the time spent waiting before a transmission. */
	public static int sendMinWaitingTime = 200*25;

	/** The variable component of the time spent waiting before a transmission. */
	public static int sendRandomWaitingTime = 128*25;

	/** The constant component of the backoff time. */
	public static int sendMinBackOffTime = 100*25;

	/** The variable component of the backoff time. */
	public static int sendRandomBackOffTime = 30*25;

	/** The time of one transmission in 1/{@link Simulator#ONE_SECOND} second. */
	public static int sendTransmissionTime = 960;

	// //////////////////////////////
	// EVENTS
	// //////////////////////////////

	/**
	 * Every mote has to test the radio traffic before transmitting a message,
	 * if there is to much traffic this event remains a test and the mote
	 * repeats it later, if there is no significant traffic this event initiates
	 * message transmission and posts a {@link Mica2Node#EndTransmissionEvent}
	 * event.
	 */
	private TestChannelEvent testChannelEvent = new TestChannelEvent();

	/**
	 * Signals the end of a transmission.
	 */
	private EndTransmissionEvent endTransmissionEvent = new EndTransmissionEvent();

	// //////////////////////////////
	// Noise and signal
	// //////////////////////////////

	/** Signal stregth of transmitting or parent node. */
	private double signalStrength = 0;

	/** Noise generated by other nodes. */
	private double noiseStrength = 0;

	/**
	 * The constant self noise level. See either the {@link Mica2Node#calcSNR}
	 * or the {@link Mica2Node#isChannelFree} function.
	 */
	public double noiseVariance = 0.025;

	/**
	 * The maximum noise level that is allowed on sending. This is actually a
	 * multiplicator of the {@link Mica2Node#noiseVariance}.
	 */
	public double maxAllowedNoiseOnSending = 5;

	/** The minimum signal to noise ratio required to spot a message in the air. */
	public double receivingStartSNR = 4.0;

	/**
	 * The maximum signal to noise ratio below which a message is marked
	 * corrupted.
	 */
	public double corruptionSNR = 2.0;

	/**
	 * Inner class TestChannelEvent. Represents a test event, this happens when
	 * the mote listens for radio traffic to decide about transmission.
	 */
	class TestChannelEvent extends Event {

		/**
		 * If the radio channel is clear it begins the transmission process,
		 * otherwise generates a backoff and restarts testing later. It also
		 * adds noise to the radio channel if the channel is free.
		 */
		public void execute() {
			if (isChannelFree(noiseStrength)) {
				// start transmitting
				transmitting = true;
				setEventTime(Mica2Node.this.sentPacket);
				beginTransmission(1, Mica2Node.this);
				endTransmissionEvent.register(sendTransmissionTime);
			} else {
				// test again
				this.register(generateBackOffTime());
			}
		}

		private void setEventTime(RadioPacket packet) {
			UInt32 age = Mica2Node.this.getClock().getValue();
			age = age.subtract(packet.getEventTime());
			packet.setEventTime(age);		
		}
	}

	/**
	 * Inner class EndTransmissionEvent. Represents the end of a transmission.
	 */
	class EndTransmissionEvent extends Event {
		/**
		 * Removes the noise generated by the transmission and sets the state
		 * variables accordingly.
		 */
		public void execute() {
			transmitting = false;
			sending = false;
			endTransmission();
			senderApplication.sendMessageDone();
		}
	}

	/**
	 * Parameterized constructor, it set both the {@link Simulator} in which
	 * this mote exists and the {@link RadioModel} which is used by this mote.
	 * 
	 * @param sim
	 *            the Simulator in which the mote exists
	 * @param radioModel
	 *            the RadioModel used on this mote
	 */
	public Mica2Node(Simulator sim, RadioModel radioModel,Clock clock) {
		super(sim, radioModel,clock);
	}

	/**
	 * Calls the {@link Mica2Node#addNoise} method. See also
	 * {@link Node#receptionBegin} for more information.
	 */
	protected void receptionBegin(double strength, Object stream) {
		addNoise(strength, stream);
	}

	/**
	 * Calls the {@link Mica2Node#removeNoise} method. See also
	 * {@link Node#receptionEnd} for more information.
	 */
	protected void receptionEnd(double strength, Object stream) {
		removeNoise(strength, stream);
	}

	/**
	 * Sends out a radio message. If the node is in receiving mode the sending
	 * is postponed until the receive is finished. This method behaves exactly
	 * like the SendMsg.send command in TinyOS.
	 * 
	 * @param packet
	 *            the message to be sent
	 * @param app
	 *            the application sending the message
	 * @return If the node is in sending state it returns false otherwise true.
	 */
	public boolean sendMessage(RadioPacket packet, Protocol app) {
		if (sending){
			System.out.println("FALSE "+Mica2Node.this.id);
			return false;
		}
		else {
			sending = true;
			transmitting = false;

			this.sentPacket = packet;
			senderApplication = app;

			if (receiving) {
				sendingPostponed = true;
			} else {
				sendingPostponed = false;
				testChannelEvent.register(generateWaitingTime());
			}
			return true;
		}
	}

	/**
	 * Generates a waiting time, adding a random variable time to a constant
	 * minimum.
	 * 
	 * @return returns the waiting time in milliseconds
	 */
	public static int generateWaitingTime() {
		return sendMinWaitingTime
				+ (int) (Simulator.random.nextDouble() * sendRandomWaitingTime);
	}

	/**
	 * Generates a backoff time, adding a random variable time to a constant
	 * minimum.
	 * 
	 * @return returns the backoff time in milliseconds
	 */
	protected static int generateBackOffTime() {
		return sendMinBackOffTime
				+ (int) (Simulator.random.nextDouble() * sendRandomBackOffTime);
	}

	/**
	 * Tells if the transmitting media is free of transmissions based on the
	 * noise level.
	 * 
	 * @param noiseStrength
	 *            the level of noise right before transmission
	 * @return returns true if the channel is free
	 */
	protected boolean isChannelFree(double noiseStrength) {
		return noiseStrength < maxAllowedNoiseOnSending * noiseVariance;
	}

	/**
	 * Tells if the transmitting media is free of transmissions based on the
	 * noise level.
	 * 
	 * @param signal
	 *            the signal strength
	 * @param noise
	 *            the noise level
	 * @return returns true if the message is corrupted
	 */
	public boolean isMessageCorrupted(double signal, double noise) {
		return calcSNR(signal, noise) < corruptionSNR;
	}

	/**
	 * Inner function for calculating the signal noise ratio the following way: <br>
	 * signal / (noiseVariance + noise).
	 * 
	 * @param signal
	 *            the signal strength
	 * @param noise
	 *            the noise level
	 * @return returns the SNR
	 */
	protected double calcSNR(double signal, double noise) {
		return signal / (noiseVariance + noise);
	}

	/**
	 * Tells if the incomming message signal is corrupted by another signal.
	 * 
	 * @param signal
	 *            the signal strength of the incomming message
	 * @param noise
	 *            the noise level
	 * @return returns true if the message is corrupted
	 */
	public boolean isReceivable(double signal, double noise) {
		return calcSNR(signal, noise) > receivingStartSNR;
	}

	/**
	 * Adds the noice generated by other motes, and breaks up a transmission if
	 * the noise level is too high. Also checks if the noise is low enough to
	 * hear incomming messages or not.
	 * 
	 * @param level
	 *            the level of noise
	 * @param stream
	 *            a reference to the incomming message
	 */
	protected void addNoise(double level, Object stream) {
		if (receiving) {
			noiseStrength += level;
			if (isMessageCorrupted(signalStrength, noiseStrength))
				corrupted = true;
		} else {
			if (!transmitting && isReceivable(level, noiseStrength)) {
				// start receiving
				senderNode = (Node) stream;
				receiving = true;	
				
				receivedPacket = ((Mica2Node)senderNode).sentPacket.clone();
				setReceptionTimestamp(receivedPacket);
				
				corrupted = false;
				signalStrength = level;
			} else {
				System.out.println("Transmitting "+transmitting +" Receivable " +  isReceivable(level, noiseStrength));
				noiseStrength += level;
				System.out.println(noiseStrength);
			}
		}
	}

	private void setReceptionTimestamp(RadioPacket packet) {
			UInt32 timestamp = getClock().getValue();
			packet.setTimestamp(timestamp);
			timestamp  = timestamp.subtract(packet.getEventTime());
			packet.setEventTime(timestamp);
	}

	/**
	 * Removes the noise, if a transmission is over, though if the source is the
	 * sender of the message being transmitted there is some post processing
	 * accordingly, the addressed application is notified about the incomming
	 * message.
	 * 
	 * @param stream
	 *            a reference to the incomming messagethe incomming message
	 * @param level
	 *            the level of noise
	 */
	protected void removeNoise(double level, Object stream) {
		if (senderNode == stream) {
			receiving = false;
//			System.out.println("Receiving finished"+Mica2Node.this.id);
			if (!corrupted) {
				this.getApplication().receiveMessage(receivedPacket);
			}
			else{
				System.out.println("Corrupted");
			}

			signalStrength = 0;
			
			senderNode = null;
			receivedPacket = null;
			
			if (sendingPostponed) {
				sendingPostponed = false;
				testChannelEvent.register(generateWaitingTime());
			}
		} else {
			noiseStrength -= level;
		}
	}
}
