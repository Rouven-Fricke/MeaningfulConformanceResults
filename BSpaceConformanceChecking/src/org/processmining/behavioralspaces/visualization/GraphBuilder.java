package org.processmining.behavioralspaces.visualization;

import javax.swing.JFrame;

import org.processmining.behavioralspaces.models.behavioralspace.DeviationMatrix;

import java.util.HashMap;
import java.util.Hashtable;
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


	public GraphBuilder(DeviationMatrix dm){
		super("Deviation Graph");
		this.dm = dm;
        setup(dm);
    }
	
	public void setup(DeviationMatrix dm) {
		
		String[][] entries = dm.getMatrixEntries();
		
		mxGraph graph = new mxGraph();
        Object parent = graph.getDefaultParent();

        mxStylesheet stylesheet = graph.getStylesheet();
        Hashtable<String, Object> style = new Hashtable<>();
        style.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_RECTANGLE);
        style.put(mxConstants.STYLE_FONTCOLOR, "#774400");
        style.put(mxConstants.STYLE_FILLCOLOR, "#FFFFFF");
        style.put(mxConstants.STYLE_STROKECOLOR, "#000000");
        //style.put(mxConstants.STYLE_ROUNDED, "true");
        stylesheet.putCellStyle("ROUNDED", style);

        graph.getModel().beginUpdate();
        try
        {
            
        	Object[] vertices = new Object[entries.length-1];
        	//create a mapping between vertices and compNames
        	HashMap<String, Object> vertexToStringMapping = new HashMap<String, Object>();
        	for(int i = 0;i<entries.length;i++) {
        		for(int j = 1; j< entries.length; j++) {
        			vertices[j-1] = (Object) entries[0][j];
        			
        			vertexToStringMapping.put(entries[0][j], graph.insertVertex(parent, null, vertices[j-1].toString(), 20, 20, 100, 40, "ROUNDED"));
        		}
        	}
        	
        	for(int i = 1; i<entries.length; i++) {
        		for(int j = 1; j< entries.length; j++) {
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
	
	//additional method to build a digraph string for graphviz
    
	public void run() {
		setup(dm);
	}
   
    
    /*public static void main (String[] args)
    {
    	GraphBuilder frame = new GraphBuilder(this.dm);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 320);
        frame.setVisible(true);
    }*/
}