package application.appGradientDescent;

import sim.type.UInt32;

public class GDMessage {
	public int nodeid = -1;
	public UInt32 clock = new UInt32();
	public int rootid = -1;
	public int sequence = -1;
	
	public GDMessage(int nodeid,int rootid,UInt32 clock,int sequence){
		this.nodeid = nodeid;
		this.rootid = rootid;
		this.clock = new UInt32(clock);
		this.sequence = sequence;
	}
	
	public GDMessage(GDMessage msg){
		this.nodeid = msg.nodeid;
		this.rootid = msg.rootid;
		this.clock = new UInt32(msg.clock);
		this.sequence = msg.sequence;
	}

	public GDMessage() {
	
	}
}