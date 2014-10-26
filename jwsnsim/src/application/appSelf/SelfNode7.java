package application.appSelf;

import core.Simulator;
import hardware.Register32;
import hardware.clock.Timer;
import hardware.clock.TimerHandler;
import hardware.transceiver.Packet;
import hardware.transceiver.Transceiver;
import sim.clock.ConstantDriftClock;
import sim.node.Node;
import sim.node.Position;
import sim.radio.MicaMac;
import sim.statistics.Distribution;

public class SelfNode7 extends Node implements TimerHandler {

	private static final int BEACON_RATE = 30000000;

	LogicalClock7 logicalClock = new LogicalClock7();
	Timer timer0;

	SelfMessage7 outgoingMsg = new SelfMessage7();
	ClockSpeedAdapter7 speedAdapter = new ClockSpeedAdapter7();
	
	Register32 lastGlobalTime = new Register32();
	Register32 lastLocalTime = new Register32();

	public SelfNode7(int id, Position position) {
		super(id, position);

		CLOCK = new ConstantDriftClock();

		/* to start clock with a random value */
		CLOCK.setValue(new Register32(Math.abs(Distribution.getRandom().nextInt())));

		MAC = new MicaMac(this);
		RADIO = new Transceiver(this, MAC);

		timer0 = new Timer(CLOCK, this);

		outgoingMsg.sequence = 0;

		System.out.println("Node:" + this.NODE_ID + ":" + (int)(CLOCK.getDrift()*1000000.0));
	}

	private void adjustClockSpeed(Packet packet) {
		SelfMessage7 msg = (SelfMessage7) packet.getPayload();
		speedAdapter.adjust(msg.nodeid, msg.progress,msg.hardwareProgress, packet.getEventTime(),msg.rate);
		logicalClock.rate = speedAdapter.getSpeed();
	}

	@Override
	public void receiveMessage(Packet packet) {
		/* update logical clock */
		logicalClock.update(packet.getEventTime());

		adjustClockSpeed(packet);
	}

	@Override
	public void fireEvent(Timer timer) {
		sendMsg();
	}

	private void sendMsg() {
		Register32 localTime, globalTime;

		localTime = CLOCK.getValue();
		globalTime = logicalClock.getValue(localTime);

		outgoingMsg.nodeid = NODE_ID;
		outgoingMsg.clock = globalTime;
		outgoingMsg.hardwareClock = new Register32(localTime);
				
		outgoingMsg.progress = globalTime.subtract(lastGlobalTime);
		outgoingMsg.hardwareProgress = localTime.subtract(lastLocalTime);
		
		lastGlobalTime = new Register32(globalTime);
		lastLocalTime = new Register32(localTime);
				
		outgoingMsg.rate = logicalClock.rate;
		outgoingMsg.sequence++;		

		Packet packet = new Packet(new SelfMessage7(outgoingMsg));
		packet.setSender(this);
		packet.setEventTime(new Register32(localTime));
		MAC.sendPacket(packet);
	}

	@Override
	public void on() throws Exception {
		super.on();
		timer0.startPeriodic(BEACON_RATE
				+ ((Distribution.getRandom().nextInt() % 100) + 1) * 10000);
	}

	public Register32 local2Global() {
		return logicalClock.getValue(CLOCK.getValue());
	}

	public String toString() {
		String s = "" + Simulator.getInstance().getSecond();

		s += " " + NODE_ID;
		s += " " + local2Global().toString();
		s += " " + Float.floatToIntBits((1.0f+logicalClock.rate)*(float)(1.0f+CLOCK.getDrift()));
		System.out.println(""+NODE_ID+" "+(1.0+(double)logicalClock.rate)*(1.0+CLOCK.getDrift()));

		
		return s;
	}
}
