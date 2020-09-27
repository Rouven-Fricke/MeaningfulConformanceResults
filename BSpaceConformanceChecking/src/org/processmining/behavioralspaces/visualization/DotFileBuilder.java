package org.processmining.behavioralspaces.visualization;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.processmining.behavioralspaces.models.behavioralspace.DeviationMatrix;
import org.processmining.behavioralspaces.models.behavioralspace.DeviationSet;
import org.processmining.behavioralspaces.plugins.RouvensPlaygroundPlugin;
import org.processmining.plugins.graphviz.dot.Dot;
import org.processmining.plugins.graphviz.visualisation.DotPanel;
import org.processmining.plugins.graphviz.visualisation.DotVisualisation;

import static java.awt.GridBagConstraints.*;


public class DotFileBuilder extends JFrame	{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final String filePath =  "C:\\Users\\rouma\\git\\MeaningfulConformanceResults\\BSpaceConformanceChecking\\DotGraph.png";
	private Container c;
	private GridBagConstraints constraints = new GridBagConstraints();
	public DotFileBuilder (String filePath) throws IOException{
		c = this.getContentPane();
		c.setLayout(new BorderLayout());
		if (filePath == null ) {
			//filePath= "C:\\Users\\rouma\\git\\MeaningfulConformanceResults\\BSpaceConformanceChecking\\DotGraph.png";
	      }
		//filePath= "C:\\Users\\rouma\\git\\MeaningfulConformanceResults\\BSpaceConformanceChecking\\DotGraph.png";
		
		
		JPanel jp= new JPanel(new GridLayout(1,0)); 
		jp.setBackground(Color.WHITE); 
		ImageIcon icon = new ImageIcon(filePath); 
		jp.setPreferredSize(new Dimension(icon.getIconHeight(), icon.getIconWidth()+100));
		JLabel label = new JLabel(); 
		label.setIcon(icon); 
		//jp.add(label, BorderLayout.NORTH);
		
		DeviationMatrix resultsMatrix = RouvensPlaygroundPlugin.getResultsMatrix();
		DeviationSet[] allDevSets = RouvensPlaygroundPlugin.getDevSetArray();
		GraphBuilder gb = new GraphBuilder(resultsMatrix);
		gb.filterSettings(allDevSets, resultsMatrix, 10);
		Dot dot = gb.getDot();
		DotPanel dp = new DotPanel(dot);
		dp.setVisible(true);
		jp.add(dp, BorderLayout.SOUTH);
		
		//JPanel for TextFields
		JPanel textPanel = new JPanel(new GridLayout(1,0));
		textPanel.setLayout(new GridLayout(3,1));
		JTextField jtf = new JTextField("Enter arguments for a new filtering", 20);
		JTextField jtf2 = new JTextField("Comma separated args",20);
		JRadioButton jrb = new JRadioButton("Show relations among members of the partition");
		JRadioButton jrb2 = new JRadioButton("Show relations of partition to all components");
		JButton but = new JButton("Get args");
		
		textPanel.add(jtf);
		textPanel.add(jtf2);
		textPanel.add(jrb);
		textPanel.add(jrb2);
		textPanel.add(but);
		but.addActionListener(new ActionListener()
		{
		  public void actionPerformed(ActionEvent e)
		  {
			
			jp.remove(dp);
			jp.revalidate();

		    // display/center the jdialog when the button is pressed
		    int sizeOfPartition = Integer.parseInt(jtf2.getText());
		    DeviationMatrix resultsMatrix = RouvensPlaygroundPlugin.getResultsMatrix();
		    DeviationSet[] allDevSets = RouvensPlaygroundPlugin.getDevSetArray();
			gb.filterSettings(allDevSets, resultsMatrix, sizeOfPartition); //calls setup() to construct dotFormat String

			try {
				System.out.println("################EventListener###############");
				jp.remove(dp);
				jp.removeAll();
				Dot dot = gb.getDot();
				DotPanel dp = new DotPanel(dot);
				dp.setVisible(true);
				jp.add(dp);
				jp.revalidate();
				jp.repaint();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			File f = new File("C:\\Users\\rouma\\git\\MeaningfulConformanceResults\\BSpaceConformanceChecking\\DotGraphNEU.png");
			f.delete();
			gb.refreshImage();
			
			ImageIcon icon = new ImageIcon("C:\\Users\\rouma\\git\\MeaningfulConformanceResults\\BSpaceConformanceChecking\\DotGraphNEU.png"); 
			
			label.setIcon(icon); 
			jp.setPreferredSize(new Dimension(icon.getIconHeight(), icon.getIconWidth()+100));
			//jp.add(label, BorderLayout.NORTH);
			//c.add(jp, BorderLayout.CENTER);
			
			//jp.add(dp);
			revalidate();
            repaint();
            setVisible(true);
		  }
		});
		
		jp.revalidate();
		System.out.println("hinter listener");
		
		c.add(jp, BorderLayout.CENTER);
		c.add(textPanel, BorderLayout.NORTH);
		c.setSize(new Dimension(600, icon.getIconWidth()+100));

	}
	 
	public JFrame showPNG() throws IOException {
		DotFileBuilder dfb = new DotFileBuilder(filePath);
		dfb.setSize(800,300);
		dfb.setVisible(true);
		return dfb; 
	}
}


