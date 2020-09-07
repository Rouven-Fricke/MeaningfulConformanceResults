package org.processmining.behavioralspaces.models.behavioralspace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	
	
	public static void createDevDistr(DeviationSet[] ds) {
		//first: construct a list of all deviations across all deviation sets in the DeviationSet[] array
		List<String> compList = new ArrayList<String>();
		
		//fill the list with all deviations
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
		System.out.println(noOfTotalDevs);
		//second: get the occurrences of the individual non-conf comps.
		 for (Map.Entry<String, Integer> val : hm.entrySet()) {
			 double devDistr = val.getValue() / noOfTotalDevs;
	            System.out.println("Element " + val.getKey() + " "
	                               + "occurs"
	                               + ": " + val.getValue() + " times"
	                               + " Deviation Distribution = " + devDistr); 
	        } 
	}
	
	public List<String> getDevList(){
		return this.deviatingCompNames;
	}
	
	
	public Map<String, Integer> getNumberedCoOccurrences(String originalComp, Map<String, Integer> hm){
		if(this.getDevList().contains(originalComp)) {
			for(String i : this.getDevList()) {
				if(!i.equals(originalComp)) {
					Integer j = hm.get(i); 
		            hm.put(i, (j == null) ? 1 : j + 1); 
				}else if(i.equals(originalComp)){
					hm.put(i, 0);
				}
			}
		}
		return hm;
	}
	
	public static void constructConnectivityMetric(DeviationSet[] ds, int traceNo) {
		HashMap<String, Double> connectivityOfComp = new HashMap<String, Double>();
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
			System.out.println("Single Component: " + singleComponent + " " + noOfCoOccurringComponents + " " + deviationsOfComp);
			connectivityOfComp.put(singleComponent, connectivity);
		}
		for(Map.Entry<String, Double> entry : connectivityOfComp.entrySet()) {
			System.out.println("Component: " + entry.getKey() + " Connectivity: " + entry.getValue());
		}
		
	}
	
	public String toString() {
		
		return "Deviation Set for Trace No: " + traceNo + " TranslationNo: " + translationNo + " List of deviating comps: " + deviatingCompNames
				+ " with probability: " + probability;
	}
}
