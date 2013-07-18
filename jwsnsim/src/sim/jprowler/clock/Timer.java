package sim.jprowler.clock;

import sim.jprowler.Event;

/**
 * Simulates Timer which is built on a hardware clock.
 * 
 * @author K. Sinan YILDIRIM
 */
public class Timer {
	
	/** Indicates if timer is periodic */
	private boolean periodic = false;
	/** The period of the timer */
	private long period = 0;
	
	/** The hardware clock on which the timer is built */
	private Clock clock;
	
	/** System event which will be used for timer events */
	Event event = null;
	
	public Timer(Clock clock,Event timerEvent){
		this.clock = clock;
		event = timerEvent;
	}
	
	private int convert(double ticks) {
		long result = (long) (ticks/(1.0 + clock.getDrift()));
		return (int)result;
	}
	
	/**
	 * Starts a one shot timer which will fire when the hardware clock 
	 * progressed given amount of clock ticks.
	 * 
	 * @param ticks
	 */
	public void startOneshot(int ticks){
		
		if(ticks > 0){
			periodic = false;	
			period = convert(ticks);
			
			if(period == 0){
				period = 1;
			}
					
			event.register((int) period);			
		}
	}
	
	/**
	 * Starts a periodic timer which will fire every time the hardware clock 
	 * progressed given amount of clock ticks.
	 * 
	 * @param ticks
	 */
	public void startPeriodic(int ticks){
		
		if(ticks > 0){
			periodic = true;	
			period = convert(ticks);	
			
			if(period == 0){
				period = 1;
			}
				
			event.register((int) period);			
		}
	}
	
	public void stop(){
		event.unregister();
	}

	public int getPeriod() {
		return (int) period;
	}
}
