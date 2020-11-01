package org.processmining.behavioralspaces.models.behavioralspace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;

public class DeviationMatrix {
	private  int M;             // number of rows
    private  int N;             // number of columns
    private  String[][] data;   // M-by-N array
    
    //for the graphBuilder provide a deviation set 
    private DeviationSet[] ds;

    // create M-by-N matrix of 0's
    public DeviationMatrix(int M, int N) {
        this.M = M;
        this.N = N;
        data = new String[M][N];
    }

    // copy constructor
    private DeviationMatrix(DeviationMatrix A) { 
    	this(A.data); 
    }
    
    // create matrix based on 2d array
    public DeviationMatrix(String[][] data) {
        M = data.length;
        N = data[0].length;
        this.data = new String[M][N];
        for (int i = 0; i < M; i++)
            for (int j = 0; j < N; j++)
                this.data[i][j] = data[i][j];    
            	//this.data[i][j] = data[Integer.toString(i)][Integer.toString(j)];
    }

    public void showDeviationMatrix() {
    	System.out.println();
        for (int i = 0; i < M; i++) {
            for (int j = 0; j < N; j++) { 
                
            	//System.out.print(String.format("%1$4s", data[i][j])); used only for integer
            	if(j == 0) {
            		System.out.print(StringUtils.leftPad(data[i][j], 30));
            	}
            	else {
            		System.out.print(StringUtils.leftPad(data[i][j], 30));
            	}
            }
            
            System.out.println();
        }
    }
    
    //for the third view in the plugin. Already attach some markup.
    public String getVisualRepresentationOfMatrix() {
    	StringBuilder builder = new StringBuilder();
    	builder.append("<table style=\"width:100%\">");
    	for (int i = 0; i < M; i++) {
    		 builder.append("<tr>");
             for (int j = 0; j < N; j++) { 
                 
            	 if(i == 0) {
            		builder.append("<th>");
             		builder.append(data[i][j]);
             		builder.append("</th>");
             		

             	}
             	else {
             		builder.append("<td>");
             		builder.append(data[i][j]);
             		builder.append("</td>");
             		
             	}
             }
             builder.append("</tr>");
         
    	 }
    	builder.append("</table>");
    	 return builder.toString(); 
    }
    
    public DeviationMatrix addMatrix(DeviationMatrix dm) {
    	String[][] firstMatrix = this.data;
    	String[][] secondMatrix = dm.data;
    	
    	//Matrix with added elements
    	DeviationMatrix resMatrix = new DeviationMatrix(N, N);
    	
    	//fill in  the first column and row with the non-compl comp names
    	String[] compNames = new String[N-1];
    
    	for(int i = 1; i<=compNames.length; i++) {
    		compNames[i-1] = this.data[i][0];
    	}
    	//label the rows and columns
    	resMatrix.data[0][0] = "Components";
    	int ithComp = 1;
    	for(String singleComp : compNames) {
    		resMatrix.data[ithComp][0] = singleComp;
    		resMatrix.data[0][ithComp] = singleComp;
    		ithComp++;
    	}
    	

    	for(int i = 1; i<this.N; i++) {
    		for(int j = 1; j< this.N; j++) {
    			int sum = Integer.parseInt(firstMatrix[i][j]) + Integer.parseInt(secondMatrix[i][j]);
    			resMatrix.data[i][j] = String.valueOf(sum);
    		}
    	}
    	return resMatrix;
    }
    
    public List<String> computeMatrixMeasures(DeviationSet[] ds) {
    	List<String> resultList = new ArrayList<String>(); //used in the visualization to get a list of strings about the measures
    	this.ds = ds;//for the GraphBuilder helper method noOfDevs
    	//could put the first part that counts the occurrences of the single comps in a separate method as we use it twice.
    	List<String> compList = new ArrayList<String>();
		//fill the list (multiset) with all deviations
		for(DeviationSet devSet : ds) {
			for(String str : devSet.getDevList()) {
				compList.add(str);
			}
		}
		double noOfTotalDevs = compList.size(); //total number of deviations dev(trace, Model)
		// hashmap to store the frequency of element 
        Map<String, Integer> hm = new HashMap<String, Integer>();
		for(String i : compList) {
			Integer j = hm.get(i); 
            hm.put(i, (j == null) ? 1 : j + 1); 
		}
		
		TreeMap<String, Integer> sortedMap = sortbykey(hm);
		//second: get the occurrences of the individual non-conf comps.
		LinkedHashMap<String, Double> mapToSort = new LinkedHashMap<String, Double>();//ensure the order remains alphabetic over time
		 for (Map.Entry<String, Integer> val : sortedMap.entrySet()) {//loop through by alphabet
			 double devDistr = val.getValue() / noOfTotalDevs;
			 mapToSort.put(val.getKey(), devDistr); //Component and how often it deviates in total              
		 }
		 
		
		 String[][] matrixEntries = this.getMatrixEntries();
		 for(Map.Entry<String, Integer> entry : sortedMap.entrySet()) {
			 List<String> guaranteedCoOccurrences = new ArrayList<String>();
			 List<String> exclusiveness = new ArrayList<String>();

			 for(int i = 1; i< matrixEntries.length; i++) {
				 if(matrixEntries[i][0].equals(entry.getKey())) {//only for the respective row of the matrix
					 //get max
					 int max = -1;
					 for(int j = 1;j<matrixEntries.length;j++) {
						 if(Integer.parseInt(matrixEntries[i][j]) > max) {
							 max = Integer.parseInt(matrixEntries[i][j]);
						 }
					 }
					 for(int j = 1; j< matrixEntries.length; j++) {
						 if(Double.parseDouble(matrixEntries[i][j]) /  max == 1.0) {
							 guaranteedCoOccurrences.add(matrixEntries[0][j]);
						 }
						 if(Double.parseDouble(matrixEntries[i][j]) / max <= 0.2) {
							 exclusiveness.add(matrixEntries[0][j]);
						 }
						 
					 }
				 }
			 }

			 String stringForTwoMeasures = "Element " + entry.getKey() + " co-occurs certainly with: ";
			 System.out.print("Element " + entry.getKey() + " co-occurs certainly with: ");
			 int i = 0;
			 for(String str : guaranteedCoOccurrences) {
				 if(i < guaranteedCoOccurrences.size()) {
					 stringForTwoMeasures+= str + ", ";
				 }else {
					 stringForTwoMeasures += str; 
				 }
				 System.out.print(str + ", ");
				 i++;
			 }
			 resultList.add(stringForTwoMeasures);
			 System.out.println();
			 
			 stringForTwoMeasures = "Element " + entry.getKey() + " is (almost/in more than 80% of all traces) exclusive to: ";
			 System.out.print("Element " + entry.getKey() + " is (almost/in more than 80% of all traces) exclusive to: ");
			 i = 0;
			 for(String str : exclusiveness) {
				 if(i < exclusiveness.size()) {
					 stringForTwoMeasures += str + ", ";
				 }else {
					 stringForTwoMeasures += str;
				 }
				 System.out.print(str + ", ");
				 i++;
			 }
			 resultList.add(stringForTwoMeasures);
			 System.out.println();
		 }
		 return resultList;
    }
    
    public String[][] getMatrixEntries(){
    	return this.data;
    }
   

  //Function to sort map by Key 
  	private static TreeMap<String, Integer> sortbykey(Map<String, Integer> map) { 
  	 // TreeMap to store values of HashMap 
  	 TreeMap<String, Integer> sorted = new TreeMap<>(); 

  	 // Copy all data from hashMap into TreeMap 
  	 sorted.putAll(map); 

  	 // Display the TreeMap which is naturally sorted 

  	 return sorted;
  	}
  	
  	public LinkedHashMap<String, Integer> noOfDevs() {
  	//could put the first part that counts the occurrences of the single comps in a separate method as we use it twice.
    	List<String> compList = new ArrayList<String>();
		//fill the list (multiset) with all deviations
		for(DeviationSet devSet : this.ds) {
			for(String str : devSet.getDevList()) {
				compList.add(str);
			}
		}
		// hashmap to store the frequency of element 
        Map<String, Integer> hm = new HashMap<String, Integer>();
		for(String i : compList) {
			Integer j = hm.get(i); 
            hm.put(i, (j == null) ? 1 : j + 1); 
		}
		
		TreeMap<String, Integer> sortedMap = sortbykey(hm);
		//second: get the occurrences of the individual non-conf comps.
		LinkedHashMap<String, Integer> mapToSort = new LinkedHashMap<String,Integer>();//ensure the order remains alphabetic over time
		 for (Map.Entry<String, Integer> val : sortedMap.entrySet()) {//loop through by alphabet
			
			String[][] matrix = this.getMatrixEntries();
			for(int i = 1;i< matrix.length; i++) {
				if(matrix[i][0].equals(val.getKey())) { //only go into the correct column
					int max = -1;
					for(int j = 1; j < matrix.length; j++) {
						if(Integer.parseInt(matrix[i][j]) > max) {
							max = Integer.parseInt(matrix[i][j]);
						}
					}
					mapToSort.put(val.getKey(), max);
				}
			}
		 }
		return mapToSort;
  	}
}
