package org.processmining.behavioralspaces.visualization;

import javax.swing.JFrame;

import org.processmining.behavioralspaces.models.behavioralspace.DeviationMatrix;
import org.processmining.behavioralspaces.models.behavioralspace.DeviationSet;
import org.processmining.behavioralspaces.models.behavioralspace.MetricsResult;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;
import com.mxgraph.view.mxStylesheet;
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.util.mxConstants;


public class GraphBuilder extends JFrame
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private DeviationMatrix dm;
	private DeviationSet[] ds;
	
	private mxGraph graph;
	private Object parent;
	
	public GraphBuilder(DeviationMatrix dm){
		super("Deviation Graph");
		this.dm = dm;
        //setup(dm);
    }
	
	public void config() {
		graph = new mxGraph();
        parent = graph.getDefaultParent();

        mxStylesheet stylesheet = graph.getStylesheet();
        Hashtable<String, Object> style = new Hashtable<>();
        style.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_RECTANGLE);
        style.put(mxConstants.STYLE_FONTCOLOR, "#774400");
        style.put(mxConstants.STYLE_FILLCOLOR, "#FFFFFF");
        style.put(mxConstants.STYLE_STROKECOLOR, "#000000");
        //style.put(mxConstants.STYLE_ROUNDED, "true");
        stylesheet.putCellStyle("ROUNDED", style);
        graph.getModel().beginUpdate();
	}
	
	public void guaranteedCoOccurrenceForTopN(DeviationSet[] ds, DeviationMatrix dm, int topN) {
		config();
		this.ds = ds;
		List<MetricsResult> list = DeviationSet.buildHierarchy(ds);
		List<String> top10 = new ArrayList<String>();
		for(int i = list.size()-3; i<list.size();i++) {
			top10.add(list.get(i).getCompName());
		}
//		for(int i=0;i<top10.size();i++) {
			//getGraphForSpecificComponent(ds, dm, top10.get(i));//create a graph for each of the specified comps
			setup(dm, top10);
//		}
		 this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	     this.setSize(400, 320);
	     this.setVisible(true);
	}
	
	//gets the graph for one particular component
	public void getGraphForSpecificComponent(DeviationSet[] ds, DeviationMatrix dm, String topI) {
		/*config();
		this.ds = ds;
		List<MetricsResult> list = DeviationSet.buildHierarchy(ds);
		List<String> top10 = new ArrayList<String>();
		for(int i = 0; i<10;i++) {
			top10.add(list.get(i).getCompName());
		}*/
		//List<MetricsResult> top10 = new ArrayList<MetricsResult>(list.subList(list.size() -topN, list.size())); //get first 10 entries
		String[][] entries = dm.getMatrixEntries();
		
		try {
			Object[] vertices = new Object[entries.length-1];
        	//create a mapping between vertices and compNames
        	HashMap<String, Object> vertexToStringMapping = new HashMap<String, Object>();
        	for(int i = 1;i<entries.length;i++) {
        		vertices[i-1] = (Object) entries[i][0];
        		Object obj =  graph.insertVertex(parent, null, vertices[i-1].toString(), 20, 20, 100, 40, "ROUNDED");
        		vertexToStringMapping.put(entries[i][0], obj);
        		
        	}
        	for(int i = 0;i<entries.length;i++) {
        		if(entries[i][0] == topI) {
        			for(int j = 1; j< entries.length; j++) {
            			if(i>=1) {
            				double edgeWeight = Integer.parseInt(entries[i][j]);	
            				//Get the number of times the component with the name of entries[i][0] deviates
            				double totalNoOfDevsOfComp = dm.noOfDevs().get(entries[i][0]);
                			double weightedEdgeWeight = edgeWeight / totalNoOfDevsOfComp; 
                			if(weightedEdgeWeight  == 1.0) {//only show guaranteed Co-Occurrence
                				Object v1 = (Object) vertexToStringMapping.get(entries[i][0]);//get the edges and their weight
                				Object v2 = (Object) vertexToStringMapping.get(entries[0][j]);
                				graph.insertEdge(parent, null, weightedEdgeWeight, v1, v2);                				
                			}
                			if(weightedEdgeWeight < 1.0  && weightedEdgeWeight > 0) {
                				System.out.print("removing cells: " + weightedEdgeWeight);
                				System.out.println(entries[0][j] + " " + j);
                				Object[] toRemove = {vertexToStringMapping.get(entries[0][j])};
                				graph.removeCells(toRemove);
                			}
            			}
        			}
        		}
        	}
		}
        finally{
        	graph.getModel().endUpdate();
        }

		new mxHierarchicalLayout(graph).execute(graph.getDefaultParent());

        mxGraphComponent graphComponent = new mxGraphComponent(graph);
        getContentPane().add(graphComponent);
	}

	
	public void setup(DeviationMatrix dm,List<String> topN) {
		config();
		String[][] entries = dm.getMatrixEntries();
		
        try{   
                
            	Object[] vertices = new Object[entries.length-1];
            	//create a mapping between vertices and compNames
            	HashMap<String, Object> vertexToStringMapping = new HashMap<String, Object>();
            	for(int i = 1;i<entries.length;i++) {
            		vertices[i-1] = (Object) entries[i][0];
            		Object obj =  graph.insertVertex(parent, null, vertices[i-1].toString(), 20, 20, 100, 40, "ROUNDED");
            		vertexToStringMapping.put(entries[i][0], obj);
            		
            	}
            	
            	for(int i = 1; i<entries.length; i++) {
            		if(topN.contains(entries[i][0])) {
            		for(int j = 1; j< entries.length; j++) {
            			if(topN.contains(entries[0][j])) {
            			double edgeWeight  = Integer.parseInt(entries[i][j]);
            			
            			Object v1 = (Object) vertexToStringMapping.get(entries[i][0]);
            			Object v2 = (Object) vertexToStringMapping.get(entries[0][j]);
            			
            			double totalNoOfDevsOfComp = dm.noOfDevs().get(entries[i][0]);
            			double weightedEdgeWeight = edgeWeight / totalNoOfDevsOfComp; 
            			if(edgeWeight / totalNoOfDevsOfComp  == 1.0) {
            				graph.insertEdge(parent, null, weightedEdgeWeight, v1, v2);
            			}
            			}
            		}
            		}
            	}	
            }
        
        finally
        {
            graph.getModel().endUpdate();
        }

        new mxHierarchicalLayout(graph).execute(graph.getDefaultParent());

        mxGraphComponent graphComponent = new mxGraphComponent(graph);
        getContentPane().add(graphComponent);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(400, 320);
        this.setVisible(true);
	}
	
	public static void createDotGraph(String dotFormat,String fileName)
	{
	    GraphViz gv=new GraphViz();
	    gv.addln(gv.start_graph());
	    gv.add(dotFormat);
	    gv.addln(gv.end_graph());
	   // String type = "gif";
	    String type = "gif";
	  // gv.increaseDpi();
	    gv.decreaseDpi();
	    gv.decreaseDpi();
	    File out = new File(fileName+"."+ type); 
	    System.out.println("Out.getAbsolutePaht: " +out.getAbsolutePath());
	    System.out.println(gv.getDotSource() + "getDotSource");
	    
	    gv.writeGraphToFile(gv.getGraph(gv.getDotSource(), type), out);
	}
	public void runGraphViz() {
		 //String dotFormat="1->2;1->3;1->4;4->5;4->6;6->7;5->7;3->8;3->6;8->7;2->8;2->5;";
		 String dotFormat = "www_jgrapht_org -> www_wikipedia_org; www_google_com -> www_jgrapht_org;"
		 		+ " www_google_com -> www_wikipedia_org; www_wikipedia_org -> www_google_com;";
			 
			 
			
	        createDotGraph(dotFormat, "DotGraph");
	}
	
	//additional method to build a digraph string for graphviz
	
	//add the deviations of all comps that map to the same transition
	//construct the matrix with only the unique transitions. Edge weights will be the added occurrences.
 
   
    
    /*public static void main (String[] args)
    {
    	GraphBuilder frame = new GraphBuilder(this.dm);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 320);
        frame.setVisible(true);
    }*/
}