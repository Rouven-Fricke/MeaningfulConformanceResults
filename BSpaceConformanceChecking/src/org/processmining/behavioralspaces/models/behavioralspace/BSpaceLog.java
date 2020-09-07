package org.processmining.behavioralspaces.models.behavioralspace;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.deckfour.xes.model.XLog;
import org.processmining.behavioralspaces.matcher.EventActivityMappings;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.plugins.dc.decomp.DCComponent;
import org.processmining.plugins.dc.decomp.DCComponents;
import org.processmining.plugins.dc.decomp.DCDecomposition;

public class BSpaceLog extends ArrayList<TraceBSpace> {


	private static final long serialVersionUID = 1L;
	private Petrinet net;
	private EventActivityMappings mappings;
	private XLog originalLog;
	private double naiveTime;
	private double effTime;
	private Set<Set<String>> ambiguousActSets;
	private DCDecomposition decomposition;
	private Set<String> unambigComponents;
	private DCComponents components;
	
	public BSpaceLog(Petrinet net, XLog originalLog, EventActivityMappings mappings) {
		super();
		this.net = net;
		this.mappings = mappings;
		this.originalLog = originalLog;
	}
	
	public String getName() {
		return net.getLabel();
	}

	public Petrinet getPetrinet() {
		return net;
	}
	
	public EventActivityMappings getMappings() {
		return mappings;
	}

	public XLog getOriginalLog() {
		return originalLog;
	}
	
	public void setDCComponents(DCComponents components) {
		this.components = components;
	}
		
	public String toString() {
		return "BSpaceLog for " + net.getLabel() + " " + super.toString();
	}
	
	public void setNaiveTime(double time) {
		this.naiveTime = time;
	}
	
	public void setEffTime(double time) {
		this.effTime = time;
	}

	public double getNaiveTime() {
		return naiveTime;
	}

	public double getEffTime() {
		return effTime;
	}

	public Set<Set<String>> getAmbiguousActSets() {
		return ambiguousActSets;
	}

	public void setAmbiguousActSets(Set<Set<String>> ambiguousActSets) {
		this.ambiguousActSets = ambiguousActSets;
	}

	public DCDecomposition getDecomposition() {
		return decomposition;
	}

	public void setDecomposition(DCDecomposition decomposition) {
		this.decomposition = decomposition;
	}

	public Set<String> getUnambigComponentsNames() {
		return unambigComponents;
	}
	
	public Set<DCComponent> getUnAmbiguousComponents() {
		Set<DCComponent> res = new HashSet<DCComponent>();
		for (String name : unambigComponents) {
			res.add(components.getComponent(name));
		}
		return res;
	}
	
	public DCComponents getComponents() {
		return components;
	}

	public void setUnambigComponents(Set<String> unambigComponents) {
		this.unambigComponents = unambigComponents;
	}

	public boolean hasCorrectMapping() {
		return mappings.containsCorrectMapping();
	}
	
	
	
	
}
