package org.processmining.behavioralspaces.visualization;

import java.awt.BorderLayout;
import java.awt.CardLayout;
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
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
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
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;

import org.processmining.behavioralspaces.models.behavioralspace.DeviationMatrix;
import org.processmining.behavioralspaces.models.behavioralspace.DeviationSet;
import org.processmining.behavioralspaces.models.behavioralspace.MetricsResult;
import org.processmining.behavioralspaces.plugins.RouvensPlaygroundPlugin;
import org.processmining.plugins.graphviz.dot.Dot;
import org.processmining.plugins.graphviz.visualisation.DotPanel;
import org.processmining.plugins.graphviz.visualisation.DotVisualisation;



public class DotFileBuilder extends JPanel	{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final String filePath =  "C:\\Users\\rouma\\git\\MeaningfulConformanceResults\\BSpaceConformanceChecking\\DotGraph.png";
	private Container c;
	private CardLayout cardLayout;
	public DotFileBuilder () throws IOException{
		//c = this.getContentPane();
		this.setLayout(new BorderLayout());
		cardLayout = new CardLayout();
		JPanel cardLayoutPanel = new JPanel(cardLayout);
		
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
		//gb.filterSettings(allDevSets, resultsMatrix, 10, "topN", "B0");
		Dot dot = gb.getDot();
		dot.setStringValue("digraph G{ \"choose specifications and press run\" }");
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
		JTextField jtf2 = new JTextField("5",20);
		
		String[] modes = {"Single Component to the selected partition", "Relation among the components of the partition"};
		JComboBox<String> jcb = new JComboBox<String>(modes);
		
		String[] comps = new String[componentsInPartition.size()];
		comps = componentsInPartition.toArray(comps);
		JComboBox<String> jcbComps = new JComboBox<String>(comps);
		
		JTextField fieldToSpecifiyAnEdgeWeightThreshold = new JTextField("[0,1]");

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
		 String[] allCompNames = new String[resultsMatrix.getMatrixEntries().length-1];
		 JCheckBox[] checkBoxArray = new JCheckBox[allCompNames.length];
		
		 JButton closePopupMenuButton = new JButton("Close Popup and save selection");
		 final JPopupMenu popUpMenu = new JPopupMenu("Select desired components");
		 popUpMenu.add(closePopupMenuButton);
		 List<String> specifiedComponentNames = new ArrayList<String>();
		 closePopupMenuButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				specifiedComponentNames.clear();
				for(JCheckBox jcb : checkBoxArray) {
					if(jcb.isSelected()) {
						specifiedComponentNames.add(jcb.getText());
						System.out.println(jcb.getText());
					}
				}
				popUpMenu.setVisible(false);
			}
		 });

		 for(int i = 1; i<resultsMatrix.getMatrixEntries().length;i++) {
			 allCompNames[i-1] = resultsMatrix.getMatrixEntries()[i][0];
		     checkBoxArray[i-1] = new JCheckBox(resultsMatrix.getMatrixEntries()[i][0]);
		     popUpMenu.add(checkBoxArray[i-1]);
		 }
		
		JButton but = new JButton("Run with specifications");
		//textPanel.add(popUpMenu);
		//textPanel.add(jtf);
		textPanel.add(jtf2);
		textPanel.add(jcb);
		textPanel.add(jcbPartition);
		textPanel.add(jcbComps);
		textPanel.add(fieldToSpecifiyAnEdgeWeightThreshold);
		System.out.println(provideThresholdInterval(fieldToSpecifiyAnEdgeWeightThreshold)[0] + " " + provideThresholdInterval(fieldToSpecifiyAnEdgeWeightThreshold)[1] );
		textPanel.add(but);
		but.addActionListener(new ActionListener()
		{
		  public void actionPerformed(ActionEvent e) {
			jp.remove(dp);
			jp.revalidate();

		    // display/center the jdialog when the button is pressed
			int start = 0, end = 0;
			if(textIndicatesInterval(jtf2)) {
				String[] splitted = jtf2.getText().split("\\s*,\\s*");
		    	start = Integer.valueOf(splitted[0]);
		    	end = Integer.valueOf(splitted[1]);
			}
			else if(!textIndicatesInterval(jtf2)) {
				start = Integer.valueOf(jtf2.getText());
				end = 0;
			}
		    DeviationMatrix resultsMatrix = RouvensPlaygroundPlugin.getResultsMatrix();
		    DeviationSet[] allDevSets = RouvensPlaygroundPlugin.getDevSetArray();
		   
		   
		    Double[] intervalForMetrics = provideThresholdInterval(fieldToSpecifiyAnEdgeWeightThreshold);
		    double lowerBound = intervalForMetrics[0];
		    double upperBound = intervalForMetrics[1];
		    System.out.println(lowerBound + "   " + upperBound + " from field: " + fieldToSpecifiyAnEdgeWeightThreshold.getText());
		    if(jcbPartition.getSelectedItem().equals("Specific Components")) {
		    	popUpMenu.setVisible(true);
		    }
		    String name = (String) jcbComps.getSelectedItem(); //get the form of the partition, i.e. topN, bottomN,...
		    // Method call String.valueOf(jcbPartition.getSelectedItem()) gets the desired type of relation among the partition, i.e. the mode
		    String relationFormat = (String) jcb.getSelectedItem();//determines if we want to show only the graph for one component or for all components among the selected partition
			if(specifiedComponentNames.isEmpty()) {
		    	gb.filterSettings(allDevSets, resultsMatrix, start, end, String.valueOf(jcbPartition.getSelectedItem()), name, relationFormat,
		    			lowerBound, upperBound); //calls setup() to construct dotFormat String
			}else if(!specifiedComponentNames.isEmpty() && jcbPartition.getSelectedItem().equals("Specific Components")) {
				gb.relationAmongPartition(resultsMatrix, specifiedComponentNames, lowerBound, upperBound);
			}
		    System.out.println(jcbPartition.getSelectedItem()+"----------");
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
		popUpMenu.setVisible(false);
		jp.revalidate();
		
		JPanel lowerPanel = new JPanel();
		JTextPane jtp = new JTextPane();
		jtp.setContentType("text/html");
		jtp.setEditable(false);
		jtp.setBackground(new Color(224,224,224));
		jtp.setText("<html> <head>" + 
				"<style>" + 
				"body {" + 
				"  background-color: linen;" + 
				"}" + 
				"" + 
				"h1 {" + 
				"  color: maroon;" + 
				"  margin-left: 40px;" + 
				"}"
				+ ".boxed {\r\n" + 
				"  border: 1px solid black ;\r\n" + 
				"}" + 
				"</style>\r\n" + 
				"</head>\r\n" + 
				"<body>"
				+ "<div class=\"boxed\">\r\n"
				+ "<body>"
				+ "Edge Colors in relation to the edge weight w:<br /> "
				+ "<font color='red'> w &le; 0.2 = red; </font>"
				+ "<font color = 'orange'> 0.2 &lt; w &le; 0.4 = orange; </font>"
				+ "<font color = 'green'> 0.4 &lt; w &le; 0.6 = green </font> <br />"
				+ "<font color = 'blue'> 0.6 &lt; w &le; 0.8 = blue;  </font> "
				+ "<font color = 'black'> 0.8 &lt; w &le; 1.0 = black </font>"
				+ "</div>"
				+ "</body>"
				+ "</html>\\\"");
		
		
		cardLayoutPanel.add(jp, "Visualization Card");
		
		/***
		 *================================================== 
		 * Next card: Tables of the hierarchy, metrics, etc.
		 * =================================================
		 */
		JPanel metricsPanel = new JPanel(new GridLayout(0,2));
		JTextPane hierarchyPane = new JTextPane();
		hierarchyPane.setContentType("text/html");
		hierarchyPane.setEditable(false);
		hierarchyPane.setBackground(new Color(224,224,224));
		List<MetricsResult> hierarchyList = DeviationSet.buildHierarchy(allDevSets);
		String hierarchyString = "";
		for(MetricsResult res: hierarchyList) {
			hierarchyString += res.toString() + "<br />";
		}
		hierarchyPane.setText("<html> "
				+ "<head>"
				+ "<style>"
				+ ".boxed{"
				+ "border: 1px solid black ;"
				+ "}"
				+ "#id_div_comments p{\r\n" + 
				" word-wrap:break-word;\r\n" + 
				"}"
				+ "</style>"
				+ "</head>"
				+ "<body>"
				+ "<div class=\"boxed\">"
				+ hierarchyString
				+ "</div>"
				+ "</body>"
				+ "</html>");
		
		//computeMatrixMeasure change return type to string
		//make another pane with the measures either next to the other or in another card layout.
		JTextPane measuresPane = new JTextPane();
		measuresPane.setContentType("text/html");
		measuresPane.setEditable(false);
		measuresPane.setBackground(new Color(224,224,224));
		List<String> measuresList = resultsMatrix.computeMatrixMeasures(allDevSets);
		String stringFormattedForHTML = "";
		for(String str : measuresList) {
			stringFormattedForHTML += str + "<br/>";
		}
		measuresPane.setText("<html> "
				+ "<head>"
				+ "<style>"
				+ ".boxed{"
				+ "border: 1px solid black ;"
				+ "}"
				+ "#id_div_comments p{"
				+ "word-wrap:break-word;"
				+ "}"
				+ "</style>"
				+ "</head>"
				+ "<body>"
				+ "<div class=\"boxed\">"
				+ stringFormattedForHTML
				+"</div>"
				+ "</body>"
				+ "</html>");
		
		JScrollPane scroller = new JScrollPane();
		scroller.add(measuresPane);
		scroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
	    scroller.setViewportView(measuresPane);
	    scroller.getViewport().add(measuresPane);
	    //measuresPane.setScrollableWidth(scroller.ScrollableSizeHint.FIT );
		metricsPanel.add(scroller);
		metricsPanel.add(hierarchyPane);
		cardLayoutPanel.add(metricsPanel, "Metrics Card");
		
		JButton switchViewsButton = new JButton();
		switchViewsButton.setText("Switch to Metrics");
		switchViewsButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				CardLayout cardLayout = (CardLayout) cardLayoutPanel.getLayout();
				if(switchViewsButton.getText().equals("Switch to Metrics")) {
					switchViewsButton.setText("Switch to Visualization");
				}
				else {
					switchViewsButton.setText("Switch to Metrics");
				}
				cardLayout.next(cardLayoutPanel);
			}
		});
		lowerPanel.add(jtp);
		lowerPanel.add(switchViewsButton);
		
		//exchange with c. ...
		this.add(cardLayoutPanel, BorderLayout.CENTER);
		this.add(textPanel, BorderLayout.NORTH);
		this.add(lowerPanel, BorderLayout.SOUTH);
		this.setSize(new Dimension(600, icon.getIconWidth()+100));
	
	}
	 
	private boolean textIndicatesInterval(JTextField jtf) {
		String enteredText = jtf.getText();
		String  regex = "[0-9]+\\s*,\\s*[0-9]+";//any number, whitespaces* a comma (','), whitespaces*, any number.
		//check if we want to test an interval or just a single number
		if(enteredText.matches(regex)) {
			return true;
		}
		return false;
		
	}
	
	private Double[] provideThresholdInterval(JTextField fieldToSpecifiyAnEdgeWeightThreshold) {
		double start = 0, end = 0;
		String enteredText = fieldToSpecifiyAnEdgeWeightThreshold.getText();
		//String regex = "\\[[0-9]+\\s*,\\s*[0-9]+\\]";
		String regex = "\\[?[0-9]+([.][0-9])?\\s*,\\s*[0-9]+([.][0-9])?\\]?";

		if(enteredText.matches(regex)) {
			String toSplit = enteredText.replace("[", "");
			toSplit = toSplit.replace("]", "");
			String[] splitted = toSplit.split("\\s*,\\s*");
	    	start = Double.valueOf(splitted[0]);
	    	end = Double.valueOf(splitted[1]);
	    	
	    	Double[] startEndArray = {start, end};
			return startEndArray;
		}
		
    	Double[] arr = {0.0, 1.0};
    	return arr;
	}
	
	public DotFileBuilder showPNG() throws IOException {
		this.setSize(1000,800);
		this.setVisible(true);
		return this; 
	}
}


