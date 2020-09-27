package org.processmining.behavioralspaces.visualization;

import javax.swing.JComponent;
import javax.swing.JFrame;

import org.processmining.behavioralspaces.models.behavioralspace.DeviationMatrix;
import org.processmining.behavioralspaces.models.behavioralspace.DeviationSet;
import org.processmining.behavioralspaces.models.behavioralspace.MetricsResult;

import java.awt.Component;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;
import com.mxgraph.view.mxStylesheet;
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.util.mxConstants;


import org.processmining.plugins.graphviz.dot.*;
import org.processmining.plugins.graphviz.visualisation.DotVisualisation;


public class GraphBuilder 
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private DeviationMatrix dm;
	private DeviationSet[] ds;
	
	private mxGraph graph;
	private Object parent;
	
	//Dot format String for GraphViz
	private String dotFormat;
	
	public GraphBuilder(DeviationMatrix dm){
		//super("Deviation Graph");
		this.dm = dm;
        //setup(dm);
    }
	
	
	//compSelection: depending on the selected mode: TopN, BottomN, usw.
	public void filterSettings(DeviationSet[] ds, DeviationMatrix dm, int compSelection, String mode, String nthComp) {
		//config();
		this.ds = ds;
		List<MetricsResult> list = DeviationSet.buildHierarchy(ds);//we just call this method to get a list of all unique comps
		List<String> partition = new ArrayList<String>();
		//for(int i = list.size()-topN; i<list.size();i++) {
		System.out.println("Mode: " + mode);
		switch(mode) {
			case "TopN":
				partition = topNPartition(ds, compSelection);
				break;
			case "BottomN":
				partition = bottomNPartition(ds, compSelection);
				break;
		}

		singleCompToPartition(dm, partition, nthComp);//build the dotFormat String
	}
	
	public List<String> topNPartition(DeviationSet[] ds, int noOfComps){
		this.ds = ds;
		List<MetricsResult> list = DeviationSet.buildHierarchy(ds);//we just call this method to get a list of all unique comps
		List<String> partition = new ArrayList<String>();
		for(int i = 0; i< noOfComps; i++) {
			partition.add(list.get(i).getCompName());
		}
		return partition;
	}
	
	public List<String> bottomNPartition(DeviationSet[] ds, int noOfComps){
		this.ds = ds;
		List<MetricsResult> list = DeviationSet.buildHierarchy(ds);//we just call this method to get a list of all unique comps
		List<String> partition = new ArrayList<String>();
		for(int i = list.size()-1; i>=list.size()-noOfComps; i--) {
			partition.add(list.get(i).getCompName());
		}
		return partition;
	}
	
	public List<String> specifiedInterval(DeviationSet[] ds, int start, int end){
		return new ArrayList<String>();
	}

	
	public void singleCompToPartition(DeviationMatrix dm, List<String> partition, String nthComp) {
		//config();
		String[][] entries = dm.getMatrixEntries();
		StringBuilder str = new StringBuilder();
        try{     
            for(int i = 1; i<entries.length; i++) {
            	//if( entries[i][0] == partition.get(nthComp)) {
            	if(entries[i][0].equals(nthComp)){
            		//if(partition.contains(entries[i][0])) {
            		for(int j = 1; j< entries.length; j++) {
            			if(partition.contains(entries[0][j])) {
            			double edgeWeight  = Integer.parseInt(entries[i][j]);
            			
            			double totalNoOfDevsOfComp = dm.noOfDevs().get(entries[i][0]);
            			double adjustedEdgeWeight = edgeWeight / totalNoOfDevsOfComp; 
            			String color = " edge [color = blue]"; 
            			if(adjustedEdgeWeight <=0.2) {
            				color = " edge [color = red];"; 
            			}
            			if(adjustedEdgeWeight > 0.2 && adjustedEdgeWeight <= 0.4) {
            				color = " edge [color = orange];";
            			}
            			if(adjustedEdgeWeight > 0.4 && adjustedEdgeWeight <= 0.6) {
            				color = " edge [color = green];";
            			}
           				if(adjustedEdgeWeight > 0.6 && adjustedEdgeWeight <= 0.8) {
           					color = " edge [color = blue];"; 
           				}
           				if(adjustedEdgeWeight >0.8 && adjustedEdgeWeight <= 1.0) {
           					color = " edge [color = black];";
           				}
           				//graph.insertEdge(parent, null, adjustedEdgeWeight, v1, v2);
           				str.append(preprocess(entries[i][0]) + "->" + preprocess(entries[0][j]) + color + "\n"
           						/*+ "[label = " + adjustedEdgeWeight + "]"*/);

            			}
            			}
            		}
            		//}
            }
        }catch(IndexOutOfBoundsException e1) {
        	System.out.println("invalid input");
        	e1.printStackTrace();
        }
        
        finally
        {
            //graph.getModel().endUpdate();
        }
        dotFormat = str.toString();
	}
	
	private void createDotGraph(String dotFormat,String fileName)
	{
	    GraphViz gv=new GraphViz();
	    gv.addln(gv.start_graph());
	    gv.add(dotFormat);
	    gv.addln(gv.end_graph());
	   // String type = "gif";
	    String type = "png";
	  // gv.increaseDpi();
	    gv.decreaseDpi();
	    gv.decreaseDpi();
	    File out = new File(fileName+"."+ type);
	    
	    gv.writeGraphToFile(gv.getGraph(gv.getDotSource(), type), out);
	}
	public JFrame runGraphViz() throws IOException {
	    createDotGraph(dotFormat, "DotGraph");
	    //TODO dotFormat ver�ndern zu einem validen dot format
	    //get it in a JFrame
	    DotFileBuilder dfb = new DotFileBuilder("C:\\Users\\rouma\\git\\MeaningfulConformanceResults\\BSpaceConformanceChecking\\DotGraph.png");
	    return dfb.showPNG();
	}
	
	public void refreshImage() {
		System.out.println("==================== NEW DOT STRING ==============");
		createDotGraph(dotFormat, "DotGraphNEU");
	}
	
	public Dot getDot() throws IOException{
		dotFormat = "digraph G {" + dotFormat + "}";
		InputStream targetStream = new ByteArrayInputStream(dotFormat.getBytes());
		Dot dot = new Dot(targetStream);
		System.out.println("--------------------\n" + dot.toString());
		return dot;
	}
	
	private String preprocess(String str) {
		String preprocessed = "\""+str+"\"";
		return preprocessed;
	}
	
	//additional method to build a digraph string for graphviz
	
	//add the deviations of all comps that map to the same transition
	//construct the matrix with only the unique transitions. Edge weights will be the added occurrences.

}