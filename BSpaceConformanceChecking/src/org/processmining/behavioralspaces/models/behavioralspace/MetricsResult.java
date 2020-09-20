package org.processmining.behavioralspaces.models.behavioralspace;

//use a easily extendable class that collects all the values of the metrics we have as a new data structure
//calculated in the DeviationSet class (DevDistr, Connectivity, (Accumulated Probability))
//and construct a triple that can than be compared by multiple attributes by a new Comparator Implementation
public class MetricsResult {
	private String compName;
	private double devDistr;
	private double connectivity;
	
	public MetricsResult() {}
	
	public MetricsResult(String compName, double devDistr, double connectivity) {
		this.compName = compName;
		this.devDistr = devDistr;
		this.connectivity = connectivity;
	}
	
	public void setCompName(String compName){
		this.compName = compName;
	}
	
	public void setDevDistr(double devDistr) {
		this.devDistr = devDistr;
	}
	
	public void setConnectivity(double connectivity) {
		this.connectivity = connectivity;
	}
	
	public double getDevDistr() {
		return this.devDistr;
	}
	
	public double getConnectivity() {
		return this.connectivity;
	}
	
	public String getCompName() {
		return this.compName;
	}
	
	public String toString() {
		return "Component: " + compName + " Deviation Distribution: " + devDistr + " Connectivity: " + connectivity;
	}
}
