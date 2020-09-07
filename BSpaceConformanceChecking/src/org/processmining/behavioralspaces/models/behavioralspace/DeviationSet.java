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
		//first: construct a list of all deviations across all deviation set in the DeviationSet[] array
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
	
	public String toString() {
		
		return "Deviation Set for Trace No: " + traceNo + " TranslationNo: " + translationNo + " List of deviating comps: " + deviatingCompNames
				+ " with probability: " + probability;
	}
}
