package org.processmining.behavioralspaces.visualization;

import java.awt.*;

import javax.swing.*;

public class PresenterFrame extends JFrame{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	Container c;
	JLabel caption;
	
	public PresenterFrame() {
		c = getContentPane();
		c.setLayout(new FlowLayout());
		caption = new JLabel("Label text");
		c.add(caption);
		
	}
}
