package sim.topology;

import sim.configuration.AreaConfiguration;
import sim.node.NodeFactory;
import sim.node.Position;
import sim.radio.SimpleRadio;

public class Line2D extends Topology{
	private double dx;
	private double dy; 
	private double previousPositionX;
	private double previousPositionY;
	
	/* (non-Javadoc)
	 * @see sinalgo.models.DistributionModel#initialize()
	 */
	public void initialize() {
		dy = 0;
		AreaConfiguration.dimX = (NodeFactory.numNodes+1)*SimpleRadio.MAX_DISTANCE;				
		dx = SimpleRadio.MAX_DISTANCE;
		
		previousPositionX = 0;
		previousPositionY = AreaConfiguration.dimY / 2;
	}
	
	/* (non-Javadoc)
	 * @see models.DistributionModel#getNextPosition()
	 */
	public Position getNextPosition() {
		previousPositionX += dx;
		previousPositionY += dy;
		return new Position(previousPositionX, previousPositionY, 0);
	}


}
