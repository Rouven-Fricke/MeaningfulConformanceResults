package org.processmining.behavioralspaces.visualization;


import org.processmining.behavioralspaces.models.behavioralspace.DeviationMatrix;
import org.processmining.behavioralspaces.models.behavioralspace.DeviationSet;
import org.processmining.behavioralspaces.models.behavioralspace.MetricsResult;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import java.util.List;


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
	
	
	private Object parent;
	
	//Dot format String for GraphViz
	private String dotFormat = "";
	
	public GraphBuilder(DeviationMatrix dm){
		//super("Deviation Graph");
		this.dm = dm;
        //setup(dm);
    }
	
	
	//compSelection: depending on the selected mode: TopN, BottomN, usw.
	//nthComp: the component we select from the comboBox
	public void filterSettings(DeviationSet[] ds, DeviationMatrix dm, int start, int end, String mode, String nthComp, String relationFormat,
			double lowerBound, double upperBound) {
		//config();
		this.ds = ds;
		//List<MetricsResult> list = DeviationSet.buildHierarchy(ds);//we just call this method to get a list of all unique comps
		List<String> partition = new ArrayList<String>();
		//for(int i = list.size()-topN; i<list.size();i++) {
		System.out.println("Mode: " + mode);
		switch(mode) {
			case "TopN":
				partition = topNPartition(ds, start);
				break;
			case "BottomN":
				partition = bottomNPartition(ds, start);
				break;
			case "Specific Interval":
				partition = specifiedInterval(ds, start, end);
		}

		//build the dotFormat String
		if(relationFormat.equals("Single Component to the selected partition")) {
			singleCompToPartition(dm, partition, nthComp, lowerBound, upperBound);//build the dotFormat String
		}
		else if(relationFormat.equals("Relation among the components of the partition")) {
			relationAmongPartition(dm, partition, lowerBound, upperBound);
		}
		System.out.println("nthcomp: " + nthComp);
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
		this.ds = ds;
		List<MetricsResult> list = DeviationSet.buildHierarchy(ds);//we just call this method to get a list of all unique comps
		List<String> partition = new ArrayList<String>();
		for(int i = start; i<= end; i++) {
			partition.add(list.get(i).getCompName());
		}
		return partition;
	}
	
	//needed? yes!
	public List<String> specifiedComps(List<String> compNames){
		return compNames;
	}

	
	
	public void singleCompToPartition(DeviationMatrix dm, List<String> partition, String nthComp, double lowerBound, double upperBound) {
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
            			if(adjustedEdgeWeight == 0.0) {
            				color = " edge [color = red];"; 
            			}
            			if(adjustedEdgeWeight <=0.2 && adjustedEdgeWeight != 0.0) {
            				color = " edge [color = crimson];"; 
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
           				if(adjustedEdgeWeight > 0.8 && adjustedEdgeWeight < 1.0) {
           					color = " edge [color = blue4];";
           				}
           				if(adjustedEdgeWeight == 1.0) {
           					color = " edge [color = black];";
           				}
           				//graph.insertEdge(parent, null, adjustedEdgeWeight, v1, v2);
           				if(edgeWeightInInterval(adjustedEdgeWeight,lowerBound, upperBound)) {
           					str.append( color + preprocess(entries[i][0]) + "->" + preprocess(entries[0][j]) + "\n"
               						/*+ "[label = " + adjustedEdgeWeight + "]"*/);
           				}

            			}
            			}
            		}
            }
        }catch(IndexOutOfBoundsException e1) {
        	System.out.println("invalid input");
        	e1.printStackTrace();
        }
        dotFormat = str.toString();
	}
	
	public void relationAmongPartition(DeviationMatrix dm, List<String> partition, double lowerBound, double upperBound) {
		//config();
		String[][] entries = dm.getMatrixEntries();
		StringBuilder str = new StringBuilder();
        try{     
            for(int i = 1; i<entries.length; i++) {
            	if(partition.contains(entries[i][0])) {
            	//if(entries[i][0].equals(nthComp)){
            		//if(partition.contains(entries[i][0])) {
            		for(int j = 1; j< entries.length; j++) {
            			if(partition.contains(entries[0][j])) {
            			double edgeWeight  = Integer.parseInt(entries[i][j]);
            			
            			double totalNoOfDevsOfComp = dm.noOfDevs().get(entries[i][0]);
            			double adjustedEdgeWeight = edgeWeight / totalNoOfDevsOfComp; 
            			String color = " edge [color = blue]"; 
            			if(adjustedEdgeWeight == 0.0) {
            				color = " edge [color = red];"; 
            			}
            			if(adjustedEdgeWeight <=0.2 && adjustedEdgeWeight != 0.0) {
            				color = " edge [color = crimson];"; 
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
           				if(adjustedEdgeWeight > 0.8 && adjustedEdgeWeight < 1.0) {
           					color = " edge [color = blue4];";
           				}
           				if(adjustedEdgeWeight == 1.0) {
           					color = " edge [color = black];";
           				}
           				//graph.insertEdge(parent, null, adjustedEdgeWeight, v1, v2);
           				if(edgeWeightInInterval(adjustedEdgeWeight,lowerBound, upperBound)) {
           					str.append(color + preprocess(entries[i][0]) + "->" + preprocess(entries[0][j]) + "\n"
               						/*+ "[label = " + adjustedEdgeWeight + "]"*/);
           				}
            			}
            			}
            		}
            }
        }catch(IndexOutOfBoundsException e1) {
        	System.out.println("invalid input");
        	e1.printStackTrace();
        }
        dotFormat = str.toString();
	}
	
	
	
	
	public Dot getDot() throws IOException{
		if(!dotFormat.substring(0, 1).equals("di")) {
			dotFormat = "digraph G {concentrate=true" + dotFormat + "}";
		}
		InputStream targetStream = new ByteArrayInputStream(dotFormat.getBytes());
		Dot dot = new Dot(targetStream);
		//System.out.println("--------------------\n" + dot.toString());
		return dot;
	}
	
	public Dot setAndReturnDot(String str) throws IOException{
		InputStream targetStream  = new ByteArrayInputStream(str.getBytes());
		Dot dot = new Dot(targetStream);
		return dot;
	}
	
	private String preprocess(String str) {
		String preprocessed = "\""+str+"\"";
		return preprocessed;
	}
	
	private boolean edgeWeightInInterval(double edgeWeight, double lowerBound, double upperBound) {
		if(edgeWeight >= lowerBound && edgeWeight <= upperBound) {
			return true;
		}
		return false;
	}
	
	//additional method to build a digraph string for graphviz
	
	//add the deviations of all comps that map to the same transition
	//construct the matrix with only the unique transitions. Edge weights will be the added occurrences.

}