package application.appRate;

import sim.type.Register;

public class FloodingMessage {
	
	public int nodeid = -1;	
	public Register clock = new Register();

	public int rootid = -1;	
	public Register rootClock = new Register();
	public float rootRate;	
	public int sequence = -1;
	
	public FloodingMessage(int nodeid,int rootid,Register clock,Register rootClock,float rootRate,int sequence){
		this.nodeid = nodeid;
		this.rootid = rootid;
		this.clock = new Register(clock);
		this.rootClock = new Register(rootClock);
		this.rootRate = rootRate;
		this.sequence = sequence;
	}
	
	public FloodingMessage(FloodingMessage msg){
		this.nodeid = msg.nodeid;
		this.rootid = msg.rootid;
		this.clock = new Register(msg.clock);
		this.rootClock = new Register(msg.rootClock);
		this.sequence = msg.sequence;
		this.rootRate = msg.rootRate;
	}

	public FloodingMessage() {
	
	}
}
