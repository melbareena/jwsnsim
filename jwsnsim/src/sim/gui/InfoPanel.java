package sim.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import sim.node.NodeFactory;
import sim.simulator.Event;
import sim.simulator.EventObserver;
import sim.simulator.Simulator;

public class InfoPanel extends JPanel implements EventObserver{
	
	JLabel numNodesCaption = new JLabel("Number of Nodes");
	JLabel maxSecondCaption = new JLabel("Simulation End Time");
	JLabel simulationSecondCaption = new JLabel("Simulation Second");
	
	
	JLabel numNodes = new JLabel("Number of Nodes");
	JLabel maxSecond = new JLabel("0");
	
	
	JLabel simulationSecond = new JLabel("0");
	JButton stopButton = new JButton("Exit");
	Event event = new Event(this);
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public InfoPanel(int w, int h) {
			
		stopButton.addActionListener(new ActionListener() {
		       public void actionPerformed(ActionEvent ae){
		           Simulator.getInstance().stopSimulation();
		           System.exit(0);
		       } 
	    });
		
		this.setSize(w,h);
		this.setPreferredSize(new Dimension(w, h));
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		numNodesCaption.setForeground(Color.RED);
		maxSecondCaption.setForeground(Color.RED);
		simulationSecondCaption.setForeground(Color.RED);
		
		add(numNodesCaption);
		add(numNodes);
		add(simulationSecondCaption);
		add(simulationSecond);
		add(maxSecondCaption);
		add(maxSecond);
		add(stopButton);
		event.register(1000000);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.Component#paint(java.awt.Graphics)
	 */
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		draw(g);
	}

	/**
	 * Draws the graph to a given graphics object.
	 * 
	 * @param g
	 *            The graphics to paint to
	 */
	private void draw(Graphics g) {
		numNodes.setText(""+NodeFactory.numNodes);	
		if(Simulator.getInstance().getSimulation()!=null){
			maxSecond.setText(""+Simulator.getInstance().getSimulation().MAXSECOND);	
		}		
		simulationSecond.setText(""+Simulator.getInstance().getSecond());		
	}

	@Override
	public void signal(Event event) {
		this.repaint();
		event.register(1000000);		
	}
}