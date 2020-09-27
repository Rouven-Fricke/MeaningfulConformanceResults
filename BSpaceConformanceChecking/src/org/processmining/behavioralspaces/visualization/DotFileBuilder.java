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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.processmining.behavioralspaces.models.behavioralspace.DeviationMatrix;
import org.processmining.behavioralspaces.models.behavioralspace.DeviationSet;
import org.processmining.behavioralspaces.models.behavioralspace.MetricsResult;
import org.processmining.behavioralspaces.plugins.RouvensPlaygroundPlugin;
import org.processmining.plugins.graphviz.dot.Dot;
import org.processmining.plugins.graphviz.visualisation.DotPanel;
import org.processmining.plugins.graphviz.visualisation.DotVisualisation;



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
		List<String> componentsInPartition = gb.topNPartition(allDevSets, resultsMatrix.getMatrixEntries().length-1);
		gb.filterSettings(allDevSets, resultsMatrix, 10, "topN", "B0");
		Dot dot = gb.getDot();
		DotPanel dp = new DotPanel(dot);
		dp.setVisible(true);
		jp.add(dp, BorderLayout.SOUTH);
		
		//JPanel for TextFields
		JPanel textPanel = new JPanel(new GridLayout(1,0));
		textPanel.setLayout(new GridLayout(3,1));
		JTextField jtf = new JTextField("Current config: " + "TopN = 10" + " Single Component to the selected partition" , 20);
		Color backgroundGray = new Color(245,245,245);
		jtf.setBackground(backgroundGray);
		jtf.setEditable(false);
		JTextField jtf2 = new JTextField("1",20);
		
		String[] modes = {"Single Component to the selected partition", "Relation among the components of the partition", 
				"Single Component to all components", "Components of the partition to the universe"};
		JComboBox<String> jcb = new JComboBox<String>(modes);
		
		String[] comps = new String[componentsInPartition.size()];
		comps = componentsInPartition.toArray(comps);
		JComboBox<String> jcbComps = new JComboBox<String>(comps);
	
		final DefaultComboBoxModel<String> listInListener = new DefaultComboBoxModel<String>(comps);
	    JComboBox<String> comboBox = new JComboBox<String>(listInListener);
	    textPanel.add(comboBox);
		
		
		
		jcbComps.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent arg0) {
				System.out.println(jcbComps.getSelectedIndex() + " " + jcbComps.getSelectedItem());
				
			}
			
		});
		jcb.addItemListener(new ItemListener(){
			@Override
			public void itemStateChanged(ItemEvent arg0) {
				System.out.println(jcb.getSelectedItem());
				
			}
			
		});
		String[] partition = {"TopN", "BottomN", "Specific Interval", "Specific Components"};
		JComboBox<String> jcbPartition = new JComboBox<String>(partition);
		jcbPartition.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				System.out.println(jcbPartition.getSelectedItem());
			}
			
		});
		JButton but = new JButton("Run with specifications");
		textPanel.add(jtf);
		textPanel.add(jtf2);
		textPanel.add(jcb);
		textPanel.add(jcbPartition);
		textPanel.add(jcbComps);
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
		    String name = (String) jcbComps.getSelectedItem(); //get the form of the partition, i.e. topN, bottomN,...
		    // Method call String.valueOf(jcbPartition.getSelectedItem()) gets the desired type of relation among the partition
			gb.filterSettings(allDevSets, resultsMatrix, sizeOfPartition, String.valueOf(jcbPartition.getSelectedItem()), name); //calls setup() to construct dotFormat String
			System.out.println(jcbPartition.getSelectedItem()+"----------");
			try {
				System.out.println("################EventListener###############");
				jp.remove(dp);
				jp.removeAll();
				
				List<String> componentsInPartition = gb.topNPartition(allDevSets, sizeOfPartition);
				String[] comps = new String[componentsInPartition.size()];
				comps = componentsInPartition.toArray(comps);
				listInListener.removeAllElements();
				for(String str : comps) {
					listInListener.addElement(str);
				}
				
				
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


