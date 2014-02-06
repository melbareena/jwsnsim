package application;

import sim.configuration.MobilityConfiguration;
import sim.configuration.NodeConfiguration;
import sim.topology.Line2D;

public class Main {

	public static void main(String[] args) {
		sample();				
	}
	
	static void sample(){
		/* create nodes */
		NodeConfiguration.createNodes("application.appPI.PINode", 100, new Line2D());
		
		/* start simulation */
		new Simulation("logFile.txt",20000);
	}
	
	static void mobilitySample(){
		MobilityConfiguration.mobility = true;
		
		/* create nodes */
		NodeConfiguration.createNodes("application.appPI.PINode", 100, new Line2D());
		
		/* start simulation */
		new Simulation("logFile.txt",20000);
	}
	
//	private static void diameterSimulations(String className) {
//		System.out.println(className);
//		try {
//			for (int i = 10; i <= 100; i += 10) {
//				for (int j = 1; j <= 5; j++) {
//					System.out.println("Diamater:" + i + " Counter:" + j);
//					new Simulation(className,i,className 
//							+"_diameter:" + i 
//							+"_count:" + j +".txt", Simulation.LINE, 0);					
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
}
