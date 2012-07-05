package application.appFtsp;

import application.Application;
import application.Logger;
import sim.clock.Clock;
import sim.clock.ConstantDriftClock;
import sim.clock.Timer;
import sim.clock.TimerHandler;
import sim.node.Node;
import sim.node.Position;
import sim.radio.SimpleRadio;
import sim.simulator.Simulator;


public class FtspApp extends Application implements TimerHandler{

	public static final int LINE = 0;
	public static final int RING = 1;
	public static final int GRID = 2;
	
	private int PERIOD = 20000000;
	int NUMNODES = 20;
	Node[] nodes = null;
	
	Clock clock = new ConstantDriftClock(1.0);
	Timer timer = new Timer(clock,this);
	Logger logger;

	public FtspApp(int numNodes,String logFile,int topology) throws Exception {
		logger = new Logger(logFile);		
		this.NUMNODES = numNodes;
			
		createTopology(topology);
			
		for(int i=0;i<NUMNODES;i++){
			nodes[i].on();
		}
		
		clock.start();
		timer.startOneshot(PERIOD);
		
		run();
	}

	private void createTopology(int topology) {
//		nodes = new FtspNode[NUMNODES];	
//		nodes = new FtspNodeWithoutDiscontinuity[NUMNODES];
		nodes = new FtspNodeMinimumVariance[NUMNODES];
//		nodes = new FtspNodeAverage[NUMNODES];
//		nodes = new FtspNodeLSAverage[NUMNODES];
//		nodes = new FtspNodeMedian[NUMNODES];
		
		if(topology == LINE){
			for(int i=0;i<NUMNODES;i++){
//				nodes[i] = new FtspNode(i+1,new Position(i*5,i*5,0));
//				nodes[i] = new FtspNodeWithoutDiscontinuity(i+1,new Position(i*5,i*5,0));
				nodes[i] = new FtspNodeMinimumVariance(i+1,new Position(i*5,i*5,0));
//				nodes[i] = new FtspNodeAverage(i+1,new Position(i*5,i*5,0));
//				nodes[i] = new FtspNodeLSAverage(i+1,new Position(i*5,i*5,0));
//				nodes[i] = new FtspNodeMedian(i+1,new Position(i*5,i*5,0));
			}			
		}
		else if(topology == RING){
			
			double oneStep = 360.0 / NUMNODES;
			double radius = SimpleRadio.MAX_DISTANCE/Math.toRadians(oneStep); 
					
			for(int i = 0; i< NUMNODES;i++){
				Position pos = new Position(radius * Math.cos(Math.toRadians(i * oneStep)),
											radius * Math.sin(Math.toRadians(i * oneStep)),0);	
//				nodes[i] = new FtspNode(i+1,pos);
				nodes[i] = new FtspNodeMinimumVariance(i+1,pos);
			}			
		}
		else if(topology == GRID){
			
			int j = (int) Math.sqrt(NUMNODES);
			int id = 0;
			
			for(int i = 0;i<j;i++){
				for(int k = 0;k<j;k++){
//					nodes[id] = new FtspNode(id+1,new Position(k*10,i*10,0));
					nodes[id] = new FtspNodeMinimumVariance(id+1,new Position(k*10,i*10,0));
					id++;
				}				
			}
		}
	}

	@Override
	public void exit() {
		logger.close();		
	}

	@Override
	public void fireEvent(Timer timer) {
		log();
		timer.startOneshot((int) (PERIOD + ((Simulator.random.nextInt() % 4) + 1)*1000000));
	}

	private void log() {
		for(int i=0;i<nodes.length;i++){
			logger.log(nodes[i].toString());
		}
	}
}
