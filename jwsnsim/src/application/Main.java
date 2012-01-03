package application;

import application.appFcsa.FloodingApp;
import application.appFtsp.FtspApp;
import application.appGtsp.GtspApp;
import application.appRateDetection.RateApp;

public class Main {
	
	public static void main(String[] args) {
		
		/*------------------------------------------*/
		//rateSimulations();
		/*------------------------------------------*/
		
		/*------------------------------------------*/
		//ftspSimulations();
		/*------------------------------------------*/	
			
		/*------------------------------------------*/
		//fcsaSimulations();
		/*------------------------------------------*/
		
		/*------------------------------------------*/
		gradientSimulations();
		/*------------------------------------------*/		
	}		

	public static void rateSimulations(){

		try{			
			new application.appRateDetection.RateApp(50,"RateConvergence.txt",RateApp.LINE);
		}
		catch (Exception e) {
			e.printStackTrace();
		}									
	}


	/**
	 * FTSP simulations 
	 * with least-squares and minimum variance slope estimator 
	 */
	public static void ftspSimulations(){

		try{
			
//			for (int i = 10; i <= 100; i+= 10) {
//				for(int j=1;j<=10;j++){
//					new application.appFtsp.FtspApp(i,"FtspSim"+i+"#"+j,FtspApp.LINE);
//					new application.appFtspMinimumVariance.FtspApp(i,"FtspSimMV"+i+"#"+j,FtspApp.LINE);
//					
//				}					
//			}
			new application.appFtsp.FtspApp(20,"LSAverage.txt",FtspApp.LINE);
//			new application.appRate.FloodingApp(40,"Rate.txt",FtspApp.LINE);
//			new application.appFcsa.FloodingApp(40,"Fcsa.txt",FloodingApp.LINE);
		}
		catch (Exception e) {
			e.printStackTrace();
		}									
	}
	
	/**
	 * Flooding Time Synchronization With Common Speed Agreement
	 */
	public static void fcsaSimulations(){
		try{
//			for(int j = 1;j<=10;j++)
//				for(int i = 10;i<=100;i+=10)
//					new application.appFlooding.FloodingApp(i,"FloodingSimLine"+i+"#"+j,FloodingApp.LINE);
			new application.appFcsa.FloodingApp(20,"FCSA.txt",FloodingApp.LINE);
		}
		catch (Exception e) {
			e.printStackTrace();
		}									
	}
	
	/**
	 * Flooding Time Synchronization With Common Speed Agreement
	 */
	public static void gradientSimulations(){
		try{
//			for(int j = 1;j<=10;j++)
//				for(int i = 10;i<=100;i+=10)
			//new application.appFcsa.FloodingApp(50,"FCSA.txt",FloodingApp.LINE);
			new application.appGradient.FloodingApp(20,"GFCSA.txt",FloodingApp.LINE);
//			new application.appGtsp.GtspApp(20,"Gtsp.txt",GtspApp.LINE);
		}
		catch (Exception e) {
			e.printStackTrace();
		}									
	}	
}
