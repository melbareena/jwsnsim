package application.appPI;

import sim.clock.ConstantDriftClock;
import sim.clock.Timer;
import sim.clock.TimerHandler;
import sim.node.Node;
import sim.node.Position;
import sim.radio.MicaMac;
import sim.radio.RadioPacket;
import sim.radio.SimpleRadio;
import sim.simulator.Simulator;
import sim.statistics.Distribution;
import sim.type.UInt32;

public class PINode extends Node implements TimerHandler {

	private static final int BEACON_RATE = 30000000;
	private static final float MAX_PPM = 0.0001f;

	static int decreaseCount = 0;
	static int increaseCount = 0;

	LogicalClock logicalClock = new LogicalClock();

	Timer timer0;

	PIMessage outgoingMsg = new PIMessage();

	// public AvtSimple alpha = new AvtSimple(-0.0001f, 0.0001f, 0.0f,
	// 0.000000001f, 0.00001f);
	// public float alpha = 1.0f/(float)BEACON_RATE;

	public PINode(int id, Position position) {
		super(id, position);

		CLOCK = new ConstantDriftClock();

		/* to start clock with a random value */
		CLOCK.setValue(new UInt32(Math.abs(Distribution.getRandom().nextInt())));
		
		MAC = new MicaMac(this);
		RADIO = new SimpleRadio(this, MAC);

		timer0 = new Timer(CLOCK, this);

//		System.out.println("Node:" + this.NODE_ID + ":"
//				+ (int) (CLOCK.getDrift() * 1000000.0));
	}

	int calculateSkew(RadioPacket packet) {
		PIMessage msg = (PIMessage) packet.getPayload();

		UInt32 neighborClock = msg.clock;
		UInt32 myClock = logicalClock.getValue(packet.getEventTime());

		return neighborClock.subtract(myClock).toInteger();
	}
	
	private static final float BOUNDARY = 2.0f*MAX_PPM*(float)BEACON_RATE;
//	float K_max = 0.000004f/BOUNDARY;
//	float K_max = 1.0f/(10.0f*(float)BEACON_RATE);
	
//	float K_max = 0.000004f/BOUNDARY;
//	float K_max = (beta*beta)/120000000.0f;
	float K_max = 1.0f/(10.0f*(float)(BEACON_RATE));
//	float K_min = 1.5f/(10000.0f*(float)(BEACON_RATE));
//	float K_min = K_max;
	float K_i = K_max;
	
//	int lastDirection = 0;
//	
//	int numError = 0;
	
	private void algorithmPI(RadioPacket packet) {
		UInt32 updateTime = packet.getEventTime();
		logicalClock.update(updateTime);

		int skew = calculateSkew(packet);
		
		/*  initial offset compensation */ 
		if(Math.abs(skew) <= BOUNDARY){	
			
//			if(skew < -50) return; 
//			int currentDirection = 0;
//			
//			if(skew>0)
//				currentDirection = 1;
//			else if(skew <0)
//				currentDirection = -1;
//			else
//				currentDirection = 0;
//			
//			if(currentDirection == 0){
//				K_i /=3.0f;
//				if(K_i < K_min)
//					K_i = K_min;
//			}
//			else if(currentDirection == lastDirection){
//				K_i = K_i*2.0f;
//				if(K_i>K_max) K_i = K_max;
//			}
//			else {
//				K_i /=3.0f;
//				if(K_i < K_min)
//					K_i = K_min;
//			}
//				
//			lastDirection = currentDirection;

					
//			float x = BOUNDARY - Math.abs(skew);					
//			float K_i = x*K_max/BOUNDARY;
						
			logicalClock.rate += K_i*(float)skew;
//			logicalClock.rate += K_max*(float)skew;
			
			UInt32 myClock = logicalClock.getValue(packet.getEventTime());
			logicalClock.setValue(myClock.add(skew),updateTime);
		}
		else{
			if(skew > BOUNDARY){
				UInt32 myClock = logicalClock.getValue(packet.getEventTime());
				logicalClock.setValue(myClock.add(skew),updateTime);
			}			
		}
	}

	private void adjustClock(RadioPacket packet) {
		algorithmPI(packet);
	}

	@Override
	public void receiveMessage(RadioPacket packet) {
		adjustClock(packet);
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

		RadioPacket packet = new RadioPacket(new PIMessage(outgoingMsg));
		packet.setSender(this);
		packet.setEventTime(new UInt32(localTime));
		MAC.sendPacket(packet);
	}

	@Override
	public void on() throws Exception {
		super.on();
		timer0.startPeriodic(BEACON_RATE
				+ ((Distribution.getRandom().nextInt() % 100) + 1) * 10000);
	}

	public UInt32 local2Global() {
		return logicalClock.getValue(CLOCK.getValue());
	}

	public String toString() {
		String s = "" + Simulator.getInstance().getSecond();

		s += " " + NODE_ID;
		s += " " + local2Global().toString();
		s += " "
				// + Float.floatToIntBits((float) ((1.0 + logicalClock.rate
				// .getValue()) * (1.0 + CLOCK.getDrift())));
				// System.out.println(""
				// + NODE_ID
				// + " "
				// + (1.0 + (double) logicalClock.rate.getValue())
				// * (1.0 + CLOCK.getDrift()));
				+ Float.floatToIntBits((float) ((1.0 + logicalClock.rate) * (1.0 + CLOCK
						.getDrift())));
		// + Float.floatToIntBits(K_i);
		// System.out.println(""
		// + NODE_ID
		// + " "
		// + (1.0 + (double) logicalClock.rate)
		// * (1.0 + CLOCK.getDrift()));

		return s;
	}
}
