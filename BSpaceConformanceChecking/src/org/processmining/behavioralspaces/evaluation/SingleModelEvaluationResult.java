package org.processmining.behavioralspaces.evaluation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.behavioralspaces.matcher.EventActivityMappings;
import org.processmining.behavioralspaces.models.behavioralspace.BSpaceLog;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.analysis.NonFreeChoiceClustersSet;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.plugins.dc.decomp.DCComponent;
import org.processmining.plugins.petrinet.structuralanalysis.FreeChoiceAnalyzer;

public class SingleModelEvaluationResult {

	public String name;
	public Petrinet net;
	public XLog originalLog;
	public EventActivityMappings etams;
	public int noiseLevel;
	public ErrorType eType;
	public int[] complianceResults;
	public double effTime;
	public double naiveTime;
	public int ambigClasses;
	public int seses;
	public int unambigSeses;
	public BSpaceLog bSpaceLog; 

	
	
	public SingleModelEvaluationResult(String name, Petrinet net, int noiseLevel) {
		this.name = name;
		this.net = net;
		this.noiseLevel = noiseLevel;
		eType = ErrorType.NONE;
	}
	
	public void setError(ErrorType t) {
		this.eType = t;
	}
	
	public ErrorType getError() {
		return eType;
	}
	
	private int logSize() {
		if (originalLog == null) {
			return 0;
		}
		return originalLog.size();
	}
	
	public void setOriginalLog(XLog originalLog) {
		this.originalLog = originalLog;
	}

	public void setEtam(EventActivityMappings etams) {
		this.etams = etams;
	}
	
	public void setComplianceResults(int[] complianceResults) {
		this.complianceResults = complianceResults;
	}
	
	private int silentTransitions() {
		int n = 0;
		for (Transition t : net.getTransitions()) {
			if (t.isInvisible() || t.getLabel().isEmpty()) {
				n++;
			}
		}
		return n;
	}
	
	public void setBSpaceLog(BSpaceLog bsl) {
		this.bSpaceLog = bsl;
	}
		
	public boolean hasLoops() {
		if (originalLog == null) {
			return false;
		}
		for (XTrace t : originalLog) {
			if (t.size() > net.getTransitions().size()) {
				return true;
			}
		}
		return false;
	}



	public void setEffTime(double effTime) {
		this.effTime = effTime;
	}


	public void setNaiveTime(double naiveTime) {
		this.naiveTime = naiveTime;
	}
	
	
	public void setAmbigClasses(int ambigClasses) {
		this.ambigClasses = ambigClasses;
	}

	public void setSeses(int seses) {
		this.seses = seses;
	}

	public void setUnambigSeses(int unambigSeses) {
		this.unambigSeses = unambigSeses;
	}
	
	private int countXorSplits() {
		int res = 0;
	
		for (Place p : net.getPlaces()) {
			if (net.getOutEdges(p).size() > 1) {
				res++;
			}
		}
		return res;
	}
	
	private int countAndSplits() {
		int res = 0;
		for (Transition t : net.getTransitions()) {
			if (net.getOutEdges(t).size() > 1) {
				res++;
			}
		}
		return res;
	}
	
	private int duplicateTasks() {
		int res = 0;
		
		Set<String> seen = new HashSet<String>();
		for (Transition t : net.getTransitions()) {
			if (!t.getLabel().isEmpty()) {
				if (!seen.add(t.getLabel())) {
					res++;
				}
			}
		}
		return res;
	}
	
	private int countSkips() {
		int res = 0;
		for (Place p : net.getPlaces()) {
			boolean hasLabeled = false;
			boolean hasSilent = false;
			for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> e : net.getOutEdges(p)) {
				if (e.getTarget().getLabel().isEmpty() || ((Transition) e.getTarget()).isInvisible()) {
					hasSilent = true;
				} else {
					hasLabeled = true;
				}
			}
			if (hasLabeled && hasSilent) {
				res++;
			}
		}
		return res;
	}
	
	public int NFCSize() {
		NonFreeChoiceClustersSet clusters = FreeChoiceAnalyzer.getNFCClusters(net);
		int res = 0;
		for (SortedSet<PetrinetNode> cluster : clusters) {
			res = res + cluster.size();
		}
		return res;
	}
	
	private int unambigSize() {
		int res = 0;
		if (bSpaceLog == null) {
			 return 0;
		}
		for (DCComponent comp : bSpaceLog.getUnAmbiguousComponents()) {
			res += comp.getPlaces().size() + comp.getTrans().size();
		}
		return res;
	}
	
	private int seseSize() {
		if (bSpaceLog == null) {
			 return 0;
		}
		int res = 0;
		for (DCComponent comp : bSpaceLog.getComponents().getComponents().values()) {
			res += comp.getPlaces().size() + comp.getTrans().size();
		}
		return res;
	}

	public String[] toCSVLine() {
//				"modelname", "noise", "error", "places", "transitions", "silent", "xorsplit", "andsplit", "skips", "duplicate", 
//				"loops", "NFC size", "log size (original)",
//				"interpretations", "ambig event classes", "SESEs", "unambig. SESEs", unambig size, sese size
//				"compliant", "noncompl", "potentiallycompliant", "naive exec time (sec)", "eff exec time (sec)"
		
		Object[] objArray = new Object[]{name, noiseLevel, eType, net.getPlaces().size(), net.getTransitions().size(), silentTransitions(),
				countXorSplits(), countAndSplits(), countSkips(), duplicateTasks(), hasLoops(), NFCSize(),
				logSize()
		};
		List<Object> objList = new ArrayList<Object>(Arrays.asList(objArray));
		if (etams != null) {
			objList.add(etams.size());
			objList.add(ambigClasses);
			objList.add(seses);
			objList.add(unambigSeses);
			objList.add(unambigSize());
			objList.add(seseSize());

			if (complianceResults != null) {
				for (int i = 0; i < complianceResults.length; i++) {
					objList.add(complianceResults[i]);
				}
				objList.add(naiveTime);
				objList.add(effTime);
			}
		}
		String[] res = new String[objList.size()];
		
		for (int i = 0; i < res.length; i++) {
			res[i] = String.valueOf(objList.get(i));
		}
		
		return res;

	}
	
	
}
