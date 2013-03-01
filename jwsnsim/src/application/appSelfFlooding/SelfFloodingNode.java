package application.appSelfFlooding;

import fr.irit.smac.util.avt.Feedback;
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

public class SelfFloodingNode extends Node implements TimerHandler {

	private static final int BEACON_RATE = 30000000;  
	private static final int TOLERANCE = 1;

	LogicalClock logicalClock = new LogicalClock();
	Timer timer0;

	RadioPacket processedMsg = null;
	SelfFloodingMessage outgoingMsg = new SelfFloodingMessage();
    
	public SelfFloodingNode(int id, Position position) {
		super(id, position);

		CLOCK = new ConstantDriftClock();
		MAC = new MicaMac(this);
		RADIO = new SimpleRadio(this, MAC);

		timer0 = new Timer(CLOCK, this);
		
	
		outgoingMsg.sequence = 0;
		outgoingMsg.rootid = NODE_ID;
		outgoingMsg.nodeid = NODE_ID;
	}
	
	int calculateSkew(RadioPacket packet) {
		SelfFloodingMessage msg = (SelfFloodingMessage) packet.getPayload();

		UInt32 neighborClock = msg.clock;
		UInt32 myClock = logicalClock.getValue(packet.getEventTime());

		return myClock.subtract(neighborClock).toInteger();
	}
	

	private void adjustClock(RadioPacket packet) {
		logicalClock.update(packet.getEventTime());
		SelfFloodingMessage msg = (SelfFloodingMessage)packet.getPayload();

		if( msg.rootid < outgoingMsg.rootid) {
			outgoingMsg.rootid = msg.rootid;
			outgoingMsg.sequence = msg.sequence;
		} else if (outgoingMsg.rootid == msg.rootid && (msg.sequence - outgoingMsg.sequence) > 0) {
			outgoingMsg.sequence = msg.sequence;
		}
		else {
			return;
		}
	
		int skew = calculateSkew(packet);
		logicalClock.setValue(msg.clock, packet.getEventTime());
		
		if (skew > TOLERANCE) {
//			logicalClock.rate.adjustValue(Feedback.LOWER);
			logicalClock.rate.adjustValue(AvtSimple.FEEDBACK_LOWER);
		} else if (skew < -TOLERANCE) {
//			logicalClock.rate.adjustValue(Feedback.GREATER);
			logicalClock.rate.adjustValue(AvtSimple.FEEDBACK_GREATER);
		} else {
//			logicalClock.rate.adjustValue(Feedback.GOOD);
			logicalClock.rate.adjustValue(AvtSimple.FEEDBACK_GOOD);
		}
	}
	
	void processMsg() {
		adjustClock(processedMsg);
	}

	@Override
	public void receiveMessage(RadioPacket packet) {
		processedMsg = packet;
		processMsg();
	}

	@Override
	public void fireEvent(Timer timer) {
		sendMsg();
	}

	private void sendMsg() {
		UInt32 localTime, globalTime;
		
		localTime = CLOCK.getValue();
		globalTime = logicalClock.getValue(localTime);
		
		if( outgoingMsg.rootid == NODE_ID ) {
			outgoingMsg.clock = new UInt32(localTime);
		}
		else{
			outgoingMsg.clock = new UInt32(globalTime);	
		}
		
		RadioPacket packet = new RadioPacket(new SelfFloodingMessage(outgoingMsg));
		packet.setSender(this);
		packet.setEventTime(new UInt32(localTime));
		MAC.sendPacket(packet);	
		
		if (outgoingMsg.rootid == NODE_ID)
			++outgoingMsg.sequence;
	}

	@Override
	public void on() throws Exception {
		super.on();
		timer0.startPeriodic(BEACON_RATE+((Simulator.random.nextInt() % 100) + 1)*10000);
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
//		System.out.println("" + NODE_ID + " "
//				+ (1.0 + (double) logicalClock.rate.getValue())
//				* (1.0 + CLOCK.getDrift()));

		return s;
	}
}