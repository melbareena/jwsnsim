package application.appSelf;

import java.util.Hashtable;

import sim.clock.ConstantDriftClock;
import sim.clock.Timer;
import sim.clock.TimerHandler;
import sim.node.Node;
import sim.node.Position;
import sim.radio.MicaMac;
import sim.radio.RadioPacket;
import sim.radio.SimpleRadio;
import sim.simulator.Simulator;
import sim.type.UInt32;

public class SelfNode2 extends Node implements TimerHandler {

	private static final int BEACON_RATE = 30000000;
	private static final double TOLERANCE = 1;

	LogicalClock logicalClock = new LogicalClock();
	Timer timer0;

	SelfMessage outgoingMsg = new SelfMessage();

	Averager averager = new Averager();
	
	class NeighborData{
		public UInt32 clock;
		public UInt32 timestamp;
		public float rate;
		
		public NeighborData(UInt32 clock, UInt32 timestamp,float rate){
			this.clock = new UInt32(clock);
			this.timestamp = new UInt32(timestamp);
			this.rate = rate;
		}
	}

	Hashtable<Integer, NeighborData> neighbors = new Hashtable<Integer, NeighborData>();
	
//	OffsetAvt offsetAvt = new OffsetAvt(0.00001f, 100000.0f);
	public AvtSimple skew_multiplier = new AvtSimple(0.50f, 1.0f, 0.95f, 0.05f, 0.20f);
	private int previousSkewPositive = 0;
	
	public SelfNode2(int id, Position position) {
		super(id, position);

		CLOCK = new ConstantDriftClock();

		/* to start clock with a random value */
		CLOCK.setValue(new UInt32(Math.abs(Simulator.random.nextInt())));

		MAC = new MicaMac(this);
		RADIO = new SimpleRadio(this, MAC);

		timer0 = new Timer(CLOCK, this);

		outgoingMsg.sequence = 0;

		System.out.println("Node:" + this.NODE_ID + ":" + CLOCK.getDrift());
	}

	int calculateProgressDifference(RadioPacket packet) {
		SelfMessage msg = (SelfMessage) packet.getPayload();
		NeighborData neighbor = neighbors.get(msg.nodeid);

		if(neighbor != null){
			UInt32 currentClock = msg.hardwareClock;
			UInt32 previousClock = neighbor.clock;
			UInt32 difference = currentClock.subtract(previousClock);
			UInt32 neighborProgress = difference.add(difference.multiply(neighbor.rate));
			
			UInt32 currentTimestamp = packet.getEventTime();
			UInt32 previousTimestamp = neighbor.timestamp;
			difference = currentTimestamp.subtract(previousTimestamp);
			UInt32 myProgress = difference.add(difference
					.multiply(logicalClock.rate.getValue()));
			
			return myProgress.subtract(neighborProgress).toInteger();
		}
		
		return 0;
	}

	private void adjustClockSpeed(RadioPacket packet) {

		int difference = calculateProgressDifference(packet);
//		System.out.println(this.NODE_ID + " " + difference);

		if (difference > TOLERANCE) {
			logicalClock.rate.adjustValue(AvtSimple.FEEDBACK_LOWER);
		} else if (difference < -TOLERANCE) {
			logicalClock.rate.adjustValue(AvtSimple.FEEDBACK_GREATER);
		} else {
			logicalClock.rate.adjustValue(AvtSimple.FEEDBACK_GOOD);
		}
	}
	
//	private void adjustClockOffset(RadioPacket packet) {
//		SelfMessage msg = (SelfMessage) packet.getPayload();
//
//		UInt32 neighborClock = msg.clock;
//		UInt32 myClock = logicalClock.getValue(packet.getEventTime());
//
//		double skew = myClock.subtract(neighborClock).toDouble();
//
//		if (previousSkewPositive == 0) {
//			if (skew > 0.0) {
//				skew_multiplier.adjustValue(AvtSimple.FEEDBACK_GREATER);
//				previousSkewPositive = 1;
//			} else if (skew < 0.0) {
//				skew_multiplier.adjustValue(AvtSimple.FEEDBACK_GREATER);
//				previousSkewPositive = -1;
//			} else {
//				skew_multiplier.adjustValue(AvtSimple.FEEDBACK_GOOD);
//				previousSkewPositive = 0;
//			}
//		} else if (previousSkewPositive == 1) {
//			if (skew > 0.0) { // positive
//				skew_multiplier.adjustValue(AvtSimple.FEEDBACK_GREATER);
//				previousSkewPositive = 1;
//			} else if (skew < 0.0) {
//				skew_multiplier.adjustValue(AvtSimple.FEEDBACK_LOWER);
//				previousSkewPositive = -1;
//			} else {
//				skew_multiplier.adjustValue(AvtSimple.FEEDBACK_GOOD);
//				previousSkewPositive = 0;
//			}
//		} else if (previousSkewPositive == -1) {
//			if (skew > 0.0) { // positive
//				skew_multiplier.adjustValue(AvtSimple.FEEDBACK_LOWER);
//				previousSkewPositive = 1;
//			} else if (skew < 0.0) { // negative
//				skew_multiplier.adjustValue(AvtSimple.FEEDBACK_GREATER);
//				previousSkewPositive = -1;
//			} else {
//				skew_multiplier.adjustValue(AvtSimple.FEEDBACK_GOOD);
//				previousSkewPositive = 0;
//			}
//		}
//		
//		UInt32 offset = logicalClock.getOffset();
//		offset = offset.add((int) -(skew * skew_multiplier.getValue()));
//		logicalClock.setOffset(offset);
//	}
	
//	private void adjustClockOffset(RadioPacket packet) {
//		SelfMessage msg = (SelfMessage) packet.getPayload();
//
//		UInt32 neighborClock = msg.clock;
//		UInt32 myClock = logicalClock.getValue(packet.getEventTime());
//
//		int skew = myClock.subtract(neighborClock).toInteger();
//				
//		if( skew < -1000 || skew > 1000){
//			logicalClock.setValue(msg.clock,packet.getEventTime());
//			logicalClock.setOffset(new UInt32());
//			
//			return;
//		}
//		
//		if (skew > TOLERANCE) {
//			offsetAvt.adjustValue(AvtSimple.FEEDBACK_LOWER);
//		} else if (skew < -TOLERANCE) {
//			offsetAvt.adjustValue(AvtSimple.FEEDBACK_GREATER);
//		} else {
//			offsetAvt.adjustValue(AvtSimple.FEEDBACK_GOOD);
//		}
//		
//		logicalClock.setOffset(offsetAvt.getValue());
//	}

	private void adjustClockOffset(RadioPacket packet) {
		SelfMessage msg = (SelfMessage) packet.getPayload();

		UInt32 neighborClock = msg.clock;
		UInt32 myClock = logicalClock.getValue(packet.getEventTime());

		int skew = myClock.subtract(neighborClock).toInteger();
		averager.update(skew);

		UInt32 offset = logicalClock.getOffset();
		offset = offset.add(-(int) (averager.getAverage() * 0.5));
		logicalClock.setOffset(offset);
	}

	@Override
	public void receiveMessage(RadioPacket packet) {
		/* update logical clock */
		logicalClock.update(packet.getEventTime());

		adjustClockSpeed(packet);
		adjustClockOffset(packet);

		/* store local receipt time */
		SelfMessage msg = (SelfMessage) packet.getPayload();
		neighbors.remove(msg.nodeid);
		neighbors.put(msg.nodeid, new NeighborData(msg.hardwareClock,packet.getEventTime(),msg.rateMultiplier));
	}

	@Override
	public void fireEvent(Timer timer) {
		sendMsg();
	}

	private void sendMsg() {
		UInt32 localTime, globalTime;

		localTime = CLOCK.getValue();
		globalTime = logicalClock.getValue(localTime);

		outgoingMsg.nodeid = NODE_ID;
		outgoingMsg.clock = globalTime;
		outgoingMsg.offset = logicalClock.getOffset();
		outgoingMsg.sequence++;

		outgoingMsg.hardwareClock = new UInt32(localTime);
		outgoingMsg.rateMultiplier = logicalClock.rate.getValue();

		RadioPacket packet = new RadioPacket(new SelfMessage(outgoingMsg));
		packet.setSender(this);
		packet.setEventTime(new UInt32(localTime));
		MAC.sendPacket(packet);

		averager = new Averager();
	}

	@Override
	public void on() throws Exception {
		super.on();
		timer0.startPeriodic(BEACON_RATE
				+ ((Simulator.random.nextInt() % 100) + 1) * 10000);
	}

	public UInt32 local2Global() {
		return logicalClock.getValue(CLOCK.getValue());
	}

	public String toString() {
		String s = "" + Simulator.getInstance().getSecond();

		s += " " + NODE_ID;
		s += " " + local2Global().toString();
		s += " "
				+ Float.floatToIntBits((float) ((1.0 + logicalClock.rate
						.getValue()) * (1.0 + CLOCK.getDrift())));
		
//		+ Float.floatToIntBits((float) logicalClock.rate.getDelta());

		return s;
	}
}
