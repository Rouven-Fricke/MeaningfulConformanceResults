package org.processmining.behavioralspaces.models.behavioralspace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.lang3.tuple.Triple;
import org.processmining.behavioralspaces.plugins.UncertainComplianceCheckAlignmentBasedPlugin;
import org.processmining.plugins.dc.decomp.DCComponent;

public class DeviationSet {
	private List<String> deviatingCompNames = new ArrayList<String>();
	private List<DCComponent> deviatingDCComps = new ArrayList<DCComponent>();
	
	private int traceNo;
	private int translationNo;
	private double probability;
	
	UncertainComplianceCheckAlignmentBasedPlugin compPlugin = new UncertainComplianceCheckAlignmentBasedPlugin();
	
	public DeviationSet(List<String> deviatingCompNames, int traceNo, int translationNo, double probability) {
		this.deviatingCompNames = deviatingCompNames;
		this.traceNo = traceNo;
		this.translationNo = translationNo;
		this.probability = probability;
		
	}
	
	public DeviationSet setDeviationSet(List<String> deviatingCompNames, int traceNo, int translationNo, double probability) {
		
		return new DeviationSet(deviatingCompNames, traceNo, translationNo, probability);
	}
	
	
	public static HashMap<String, Double> createDevDistr(DeviationSet[] ds) {
		//first: construct a list of all deviations across all deviation sets in the DeviationSet[] array
		List<String> compList = new ArrayList<String>();
		
		//fill the list (multiset) with all deviations
		for(DeviationSet devSet : ds) {
			for(String str : devSet.deviatingCompNames) {
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
		//TreeMap<String, Double> sortedMap = sortbykey(hm);
		System.out.println(noOfTotalDevs);
		//second: get the occurrences of the individual non-conf comps.
		HashMap<String, Double> unsortedMap = new HashMap<String, Double>();
		 for (Map.Entry<String, Integer> val : hm.entrySet()) {
			 double devDistr = val.getValue() / noOfTotalDevs;
			 unsortedMap.put(val.getKey(), devDistr);
	            /*System.out.println("Element " + val.getKey() + " "
	                               + "occurs"
	                               + ": " + val.getValue() + " times"
	                               + " Deviation Distribution = " + devDistr); 
	                               */
		 }
		
		 //sort the components in descending order
		 List<Entry<String, Double>> sortedEntries = sortValues(unsortedMap);
		 double tester = 0.0;
		 for(Entry<String, Double> entry : sortedEntries) {
			 System.out.println("Element " + entry.getKey() + " Deviation Distribution: " + entry.getValue());
			 tester += entry.getValue();
		 }
		 System.out.println(tester);
		 return unsortedMap;
	}
	
	public List<String> getDevList(){
		return this.deviatingCompNames;
	}
	
	//muss evtl geändert werden zur ganzen Liste an non-compl components
	public Map<String, Integer> getNumberedCoOccurrences(String originalComp, Map<String, Integer> hm){
		if(this.getDevList().contains(originalComp)) {
			for(String comp : this.getDevList()) {//for each comp of the whole list of non-compl comps
				if(!comp.equals(originalComp)) {
					Integer j = hm.get(comp); 
		            hm.put(comp, (j == null) ? 1 : j + 1); 
				}else if(comp.equals(originalComp)){
					hm.put(comp, 0);
				}
			}
		}//hier noch was einfügen??? Dürfen nicht nur über this.getDevList gehen sondern müssen über alle non-compl comps gehen.
		return hm;
	}
	
	public static HashMap<String, Double> constructConnectivityMetric(DeviationSet[] ds) {
		Map<String, Double> connectivityOfComp = new HashMap<String, Double>();
		List<String> deviatingComps = new ArrayList<String>();
		//get a list of all unique deviating comps for a trace to get the connectivity of each one of them.
		for(DeviationSet devSet : ds) {
			for(String str : devSet.deviatingCompNames) {
				if(!deviatingComps.contains(str)) {
					deviatingComps.add(str);
				}
			}
		}
		
		//compute connectivity for each comp
		for(String singleComponent : deviatingComps) {
			double deviationsOfComp = 0.0;
			Map<String, Integer> hm = new HashMap<String, Integer>();
			for(DeviationSet devSet : ds) {
				devSet.getNumberedCoOccurrences(singleComponent, hm);
				if(devSet.deviatingCompNames.contains(singleComponent)) {//will only work if a fragment is false at most 1 time per trace
					deviationsOfComp++;
				}
		
			}
			
			double noOfCoOccurringComponents = 0.0;
			for(Map.Entry<String, Integer> val : hm.entrySet()) {
				if(val.getKey() != singleComponent) {
					noOfCoOccurringComponents++;
				}
			}
			double connectivity = noOfCoOccurringComponents / deviationsOfComp;
			//System.out.println("Single Component: " + singleComponent + " " + noOfCoOccurringComponents + " " + deviationsOfComp);
			connectivityOfComp.put(singleComponent, connectivity);
		}
		
		HashMap<String, Double> unsortedMap = new HashMap<String, Double>();
		//for(Map.Entry<String, Double> entry : connectivityOfComp.entrySet()) {
		for(Map.Entry<String, Double> entry : connectivityOfComp.entrySet()) {
			if(entry.getKey().equals("No deviating comp for this trace")) {
				continue;
			}
			//System.out.println("Component: " + entry.getKey() + " Connectivity: " + entry.getValue());
			unsortedMap.put(entry.getKey(), entry.getValue());
		}
		
		 List<Entry<String, Double>> sortedEntries = sortValues(unsortedMap);
		 for(Entry<String, Double> entry : sortedEntries) {
			 System.out.println("Element " + entry.getKey() + " Connectivity: " + entry.getValue());
		 }
		 return unsortedMap;
	}
	
	public void buildHierarchy(DeviationSet[] ds) {
		//Step 1: get a list of all the metrics results(name, dd, conn)
		List<MetricsResult> resultList = new ArrayList<>();
		HashMap<String, Double> devDistrResults = createDevDistr(ds);
		HashMap<String, Double> connectivityResults = constructConnectivityMetric(ds);
		for(int i= 0; i< devDistrResults.size(); i++) {
			//get the name and the devDistr
			//then use connectivityResults.get(key) mit namen als key to fill in the third value of the metrics result object
		}
		
		//Step 2: compare
	}
	
	public int size() {
		return this.deviatingCompNames.size();
	}
	
	public String toString() {
		
		return "Deviation Set for Trace No: " + traceNo + " TranslationNo: " + translationNo + " List of deviating comps: " + deviatingCompNames
				+ " with probability: " + probability;
	}
	
	//Function to sort map by Key 
	private static TreeMap<String, Double> sortbykey(Map<String, Double> map) { 
	 // TreeMap to store values of HashMap 
	 TreeMap<String, Double> sorted = new TreeMap<>(); 

	 // Copy all data from hashMap into TreeMap 
	 sorted.putAll(map); 

	 // Display the TreeMap which is naturally sorted 
	 /*for (Map.Entry<String, Integer> entry : sorted.entrySet())  
	     System.out.println("Key = " + entry.getKey() +  
	                  ", Value = " + entry.getValue());    */    
	 return sorted;
	}
	
	private static List<Entry<String, Double>> sortValues(Map<String, Double> unsortedMap){
		List<Entry<String, Double>> sortedEntries = new ArrayList<Entry<String, Double>>(unsortedMap.entrySet());
		 Collections.sort(sortedEntries, 
		            new Comparator<Entry<String,Double>>() {
		                @Override
		                public int compare(Entry<String,Double> e1, Entry<String,Double> e2) {
		                    return e2.getValue().compareTo(e1.getValue());
		                }
		            }
		    );
		 return sortedEntries;
	}
}
