package application;

import sim.simulator.Simulator;

public abstract class Application {
	public static long MAXSECOND = 200000;
	
	public void run(){
		
		while(Simulator.getInstance().getSecond() < MAXSECOND){
			Simulator.getInstance().tick();
		}
		
		Simulator.getInstance().reset();
		
		exit();
	}
	
	public abstract void exit();
}