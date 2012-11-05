package application.appSelf;

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

public class SelfNode3 extends Node implements TimerHandler {

	private static final int BEACON_RATE = 30000000;

	LogicalClock3 logicalClock = new LogicalClock3();
	Timer timer0;

	SelfMessage outgoingMsg = new SelfMessage();
	ClockSpeedAdapter speedAdapter = new ClockSpeedAdapter();

	public SelfNode3(int id, Position position) {
		super(id, position);

		CLOCK = new ConstantDriftClock();

		/* to start clock with a random value */
		CLOCK.setValue(new UInt32(Math.abs(Simulator.random.nextInt())));

		MAC = new MicaMac(this);
		RADIO = new SimpleRadio(this, MAC);

		timer0 = new Timer(CLOCK, this);

		outgoingMsg.sequence = 0;

		System.out.println("Node:" + this.NODE_ID + ":" + (int)(CLOCK.getDrift()*1000000.0));
	}

	private void adjustClockSpeed(RadioPacket packet) {
		SelfMessage msg = (SelfMessage) packet.getPayload();
		speedAdapter.adjust(msg.nodeid, msg.hardwareClock, packet.getEventTime(), msg.rateMultiplier);
		logicalClock.rate = speedAdapter.getSpeed();
	}
	
	private void adjustClockOffset(RadioPacket packet) {
		SelfMessage msg = (SelfMessage) packet.getPayload();

		UInt32 neighborClock = msg.clock;
		UInt32 myClock = logicalClock.getValue(packet.getEventTime());

		int skew = myClock.subtract(neighborClock).toInteger();
		
		UInt32 offset = logicalClock.getOffset();
		offset = offset.add((int) -(skew *0.5));
		logicalClock.setOffset(offset);	
	}

	@Override
	public void receiveMessage(RadioPacket packet) {
		/* update logical clock */
//		logicalClock.update(packet.getEventTime());

		adjustClockSpeed(packet);
//		adjustClockOffset(packet);
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
		outgoingMsg.rateMultiplier = logicalClock.rate;

		RadioPacket packet = new RadioPacket(new SelfMessage(outgoingMsg));
		packet.setSender(this);
		packet.setEventTime(new UInt32(localTime));
		MAC.sendPacket(packet);
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
				+ Float.floatToIntBits((float) ((1.0 + speedAdapter.getSpeed()) * (1.0 + CLOCK.getDrift())));

		
//		s += " "
//				+ Float.floatToIntBits(speedAdapter.getSpeed());

		
		return s;
	}
}