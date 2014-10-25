package application.appRate;

import hardware.Register32;
import application.regression.LeastSquares;
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

public class FloodingNode extends Node implements TimerHandler {

	private static final int MAX_NEIGHBORS = 8;
	
	private static final int BEACON_RATE = 30000000;  
	private static final int ROOT_TIMEOUT = 5;
	private static final int IGNORE_ROOT_MSG = 4;	
	private static final long NEIGHBOR_REMOVE = BEACON_RATE * 5; 

	Neighbor neighbors[] = new Neighbor[MAX_NEIGHBORS];
	int numNeighbors = 0;

	LeastSquares ls = new LeastSquares();
	Timer timer0;
	
	Register32 rootClock = new Register32();
	Register32 lastUpdate = new Register32();
	float rootRate = 0.0f;
	

	RadioPacket processedMsg = null;
	FloodingMessage outgoingMsg = new FloodingMessage();

	public FloodingNode(int id, Position position) {
		super(id, position);

		CLOCK = new ConstantDriftClock();
		MAC = new MicaMac(this);
		RADIO = new SimpleRadio(this, MAC);

		timer0 = new Timer(CLOCK, this);

		outgoingMsg.sequence = 0;
		outgoingMsg.rootid = NODE_ID;

		for (int i = 0; i < neighbors.length; i++) {
			neighbors[i] = new Neighbor();
		}
	}

	private int findNeighborSlot(int id) {
		for (int i = 0; i < neighbors.length; i++) {
			if ((neighbors[i].free == false) && (neighbors[i].id == id)) {
				return i;
			}
		}

		return -1;
	}
	
	private void updateNeighborhood(){
		int i;
		Register32 age;

		Register32 localTime = CLOCK.getValue();

		for (i = 0; i < MAX_NEIGHBORS; ++i) {
			age = new Register32(localTime);
			age = age.subtract(neighbors[i].timestamp);
			
			if(age.toLong() >= NEIGHBOR_REMOVE && neighbors[i].free == false) {
				neighbors[i].free = true;
				neighbors[i].clearTable();
			}
		}
	}

	private int getFreeSlot() {
		int i, freeItem = -1;

		for (i = 0; i < MAX_NEIGHBORS; ++i) {
			
			if(neighbors[i].free){
				freeItem = i;
			}
		}

		return freeItem;
	}

	private void addEntry(FloodingMessage msg, Register32 eventTime) {

		boolean found = false;
				
		updateNeighborhood();
		
		/* find and add neighbor */
		int index = findNeighborSlot(msg.nodeid);
		
		if(index >= 0){
			found = true;
		}
		else{
			index = getFreeSlot();
		}

		if (index >= 0) {		
			neighbors[index].free = false;
			neighbors[index].id = msg.nodeid;		
			neighbors[index].addNewEntry(msg.clock,eventTime);
			neighbors[index].timestamp = new Register32(eventTime);
			if(found){
				ls.calculate(neighbors[index].table, neighbors[index].tableEntries);
				neighbors[index].rate = ls.getSlope();
			}
			else{
				neighbors[index].rate = 0;
			}						
		}
	}

	void processMsg() {
		FloodingMessage msg = (FloodingMessage) processedMsg.getPayload();

		addEntry(msg, processedMsg.getEventTime());	
		
		if( msg.rootid < outgoingMsg.rootid){ 
//				&&
	            //after becoming the root, a node ignores messages that advertise the old root (it may take
	            //some time for all nodes to timeout and discard the old root) 
//	            !(heartBeats < IGNORE_ROOT_MSG && outgoingMsg.rootid == NODE_ID)){
			outgoingMsg.rootid = msg.rootid;
			outgoingMsg.sequence = msg.sequence;
			
			rootClock = new Register32(msg.rootClock);
			lastUpdate = new Register32(processedMsg.getEventTime());
			rootRate = 0;
		} else if (outgoingMsg.rootid == msg.rootid && (msg.sequence - outgoingMsg.sequence) > 0) {
			outgoingMsg.sequence = msg.sequence;
			
			rootClock = new Register32(msg.rootClock);
			lastUpdate = new Register32(processedMsg.getEventTime());
			int index = findNeighborSlot(msg.nodeid); 
			if( index != -1){
				rootRate = (neighbors[index].rate+1.0f)*(msg.rootRate+1.0f)-1.0f;
			}
			else{
				rootRate = 0.0f;
			}
		}
		else {
			return;
		}
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

		Register32 localTime = CLOCK.getValue();

		if( outgoingMsg.rootid == NODE_ID ) {
			rootClock = rootClock.add(localTime.subtract(lastUpdate));
			lastUpdate = new Register32(localTime);
		}
		
		outgoingMsg.nodeid = NODE_ID;
		outgoingMsg.clock = CLOCK.getValue();
		
		outgoingMsg.rootRate = rootRate;
		outgoingMsg.rootClock = local2Global(localTime);
		
		RadioPacket packet = new RadioPacket(new FloodingMessage(outgoingMsg));
		packet.setSender(this);
		packet.setEventTime(new Register32(localTime));
		MAC.sendPacket(packet);	

		if (outgoingMsg.rootid == NODE_ID)
			++outgoingMsg.sequence;
	}

	@Override
	public void on() throws Exception {
		super.on();
		timer0.startPeriodic(BEACON_RATE+((Distribution.getRandom().nextInt() % 100) + 1)*10000);
	}
	
	public Register32 local2Global(Register32 now) {
		int diff = now.subtract(lastUpdate).toInteger();
		diff += (int)(rootRate*(float)diff);
		return rootClock.add(diff);
	}

	public Register32 local2Global() {
		int diff = CLOCK.getValue().subtract(lastUpdate).toInteger();
		diff += (int)(rootRate*(float)diff);
		return rootClock.add(diff);
	}

	public String toString() {
		String s = "" + Simulator.getInstance().getSecond();

		s += " " + NODE_ID;
		s += " " + local2Global().toString();
//		s += " " + Float.floatToIntBits((1.0f+logicalClock.rate)*(float)(1.0f+CLOCK.getDrift()));
		s += " " + Float.floatToIntBits(rootRate);

		return s;
	}
}
