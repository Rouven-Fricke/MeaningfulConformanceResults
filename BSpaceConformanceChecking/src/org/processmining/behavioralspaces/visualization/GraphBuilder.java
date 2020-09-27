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
	
	
	
	public void filterSettings(DeviationSet[] ds, DeviationMatrix dm, int topN) {
		//config();
		this.ds = ds;
		List<MetricsResult> list = DeviationSet.buildHierarchy(ds);
		List<String> top10 = new ArrayList<String>();
		//for(int i = list.size()-topN; i<list.size();i++) {
		for(int i = list.size()-1; i>=topN; i--) {
			top10.add(list.get(i).getCompName());
		}
//		for(int i=0;i<top10.size();i++) {
			//getGraphForSpecificComponent(ds, dm, top10.get(i));//create a graph for each of the specified comps
			setup(dm, top10);//build the dotFormat String
//		}
	}

	
	public void setup(DeviationMatrix dm,List<String> topN) {
		//config();
		String[][] entries = dm.getMatrixEntries();
		StringBuilder str = new StringBuilder();
        try{     
            for(int i = 1; i<entries.length; i++) {
            	if( entries[i][0] == topN.get(5)) {
            	if(topN.contains(entries[i][0])) {
            		for(int j = 1; j< entries.length; j++) {
            			if(topN.contains(entries[0][j])) {
            			double edgeWeight  = Integer.parseInt(entries[i][j]);
            			
            			/*Object v1 = (Object) vertexToStringMapping.get(entries[i][0]);
            			Object v2 = (Object) vertexToStringMapping.get(entries[0][j]);
            			*/
            			
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
            		}
            		
            }
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
	    //TODO dotFormat verändern zu einem validen dot format
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