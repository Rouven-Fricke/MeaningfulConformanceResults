package org.processmining.behavioralspaces.matcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.plugins.log.XLogHelper;
import org.processmining.plugins.log.abstraction.mapping.EventMapping;
import org.processmining.plugins.log.abstraction.mapping.EventsToActivitiesMapping;

public class PositionbasedMapper {

	
	XLog originalLog;
	XLog noisyLog;
	Map<String, Double> posMapO;
	Map<String, Double> posMapN;
	Map<String, Set<Integer>> positionsO;
	Map<String, Set<Integer>> positionsN;
	
	public PositionbasedMapper(XLog originalLog, XLog noisyLog) {
		this.originalLog = originalLog;
		this.noisyLog = noisyLog;
		posMapO = new HashMap<String, Double>();
		posMapN = new HashMap<String, Double>();
		positionsO = new HashMap<String, Set<Integer>>();
		positionsN = new HashMap<String, Set<Integer>>();
		
		for (String eventClass : XLogHelper.extractEventClasses(originalLog, null)) {
			posMapO.put(eventClass, calculateAveragePos(eventClass, originalLog));
			positionsO.put(eventClass, computePositions(eventClass, originalLog));
		}
		
		for (String eventClass : XLogHelper.extractEventClasses(noisyLog, null)) {
			posMapN.put(eventClass, calculateAveragePos(eventClass, noisyLog));
			positionsN.put(eventClass, computePositions(eventClass, originalLog));
		}
		
		
	}
	
	private Set<Integer> computePositions(String eventClass, XLog log) {
		Set<Integer> intSet = new HashSet<Integer>();
		for (XTrace t : log) {
			int index = getFirstIndex(eventClass, t);
			if (index != -1) {
				intSet.add(index);
			}
		}
		return intSet;
	}
	
	public EventActivityMappings obtainMappings() {
		Map<String, Set<String>> matches = new HashMap<String, Set<String>>();

		for (String classO : positionsO.keySet()) {
			
			int pos = (new ArrayList<Integer>(positionsO.get(classO)).get(0));
			Set<String> classMatches = new HashSet<String>();
			for (String classN : positionsN.keySet()) {
				if (positionsN.get(classN).contains(pos)) {
					classMatches.add(classN);
				}
			}
			matches.put(classO, classMatches);
		}

		for (String classN : posMapN.keySet()) {
			if (!alreadyMatched(classN, matches)) {
				Set<String> classMatches = matches.get(bestMatch(classN));
				classMatches.add(classN);
			}
		}
		return computeCombinations(matches);
	}

	public EventActivityMappings obtainMappings(double range) {
		Map<String, Set<String>> matches = new HashMap<String, Set<String>>();
		
		for (String classO : posMapO.keySet()) {
			
			
			for (String classN : posMapN.keySet()) {
				if (positionalMatch(classO, classN, range)) {
					Set<String> classMatches = new HashSet<String>();
					if (matches.containsKey(classO)) {
						classMatches = matches.get(classO);
					}
					classMatches.add(classN);
					matches.put(classO, classMatches);
				}
			}
		}
		
		for (String classN : posMapN.keySet()) {
			if (!alreadyMatched(classN, matches)) {
				Set<String> classMatches = matches.get(bestMatch(classN));
				classMatches.add(classN);
			}
		}
		for (String classO : matches.keySet()) {
			if (matches.get(classO).size() > 1) {
//				System.out.println("Ambig match: " + classO + " --- " + matches.get(classO));
			}
		}
		return computeCombinations(matches);
	}
	
	private EventActivityMappings computeCombinations(Map<String, Set<String>> matches) {
		List<List<EventMapping>> mappingList = new ArrayList<List<EventMapping>>();
		mappingList.add(new ArrayList<EventMapping>());
		
		for (String classO : matches.keySet()) {
			List<List<EventMapping>> newList = new ArrayList<List<EventMapping>>();
			for (String classN : matches.get(classO)) {
				for (List<EventMapping> mapping : mappingList) {
					List<EventMapping> newMapping = new ArrayList<EventMapping>(mapping);
					newMapping.add(new EventMapping(classO, classN));
					if (!hasDoubleEventMatch(newMapping)) {
						newList.add(newMapping);
					}
				}
			}
			mappingList = new ArrayList<List<EventMapping>>(newList);
			if (mappingList.size() > 20000) {
				System.out.println("Mapping too large");
				return new EventActivityMappings();
			}
		}
		
//		Iterator<List<EventMapping>> iter = mappingList.iterator();
		List<EventsToActivitiesMapping> etamList = new ArrayList<EventsToActivitiesMapping>();
		
		for (List<EventMapping> mapping : mappingList) {
			EventsToActivitiesMapping etam = new EventsToActivitiesMapping();
			for (EventMapping eventMapping : mapping) {
				etam.add(eventMapping);
			}
			etamList.add(etam);
		}
		return new EventActivityMappings(etamList);
	}
	
	private boolean hasDoubleEventMatch(List<EventMapping> mapping) {
		Set<String> seenClasses = new HashSet<String>();
		for (EventMapping match : mapping) {
			if (!seenClasses.addAll(match.getEventNames())) {
				return true;
			}
		}
		return false;
	}
	
	private String bestMatch(String classN) {
		String match = null;
		double best = Integer.MAX_VALUE;
		for (String classO : posMapO.keySet()) {
			double dist = positionalDiff(classO, classN);
			if (dist < best) {
				best = dist;
				match = classO;
			}
		}
		return match;
	}
	
	private boolean alreadyMatched(String classN, Map<String, Set<String>> matches) {
		for (Set<String> classMatches : matches.values()) {
			if (classMatches.contains(classN)) {
				return true;
			}
		}
		return false;
	}
	
	private boolean positionalMatch(String classO, String classN, double range) {
		return (positionalDiff(classO, classN) <= range);
	}
	
	private double positionalDiff(String classO, String classN) {
		return Math.abs( posMapO.get(classO) - posMapO.get(classN));
	}
	
	private double calculateAveragePos(String eventClass, XLog log) {
		int sum = 0;
		int count = 0;
		for (XTrace trace : log) {
			int index = getFirstIndex(eventClass, trace);
			if (index != -1) {
				sum += index;
				count++;
			}
			
//			for (int index : getIndices(eventClass, trace)) {
//				sum += index;
//				count++;
//			}
		}
		return sum * 1.0 / count;
	}
	
	private List<Integer> getIndices(String eventClass, XTrace trace) {
		List<Integer> res = new ArrayList<Integer>();
		for (XEvent e : trace) {
			if (XConceptExtension.instance().extractName(e).equals(eventClass)) {
				res.add(trace.indexOf(e));
			}
		}
		return res;
	}
	
	private int getFirstIndex(String eventClass, XTrace trace) {
		for (XEvent e : trace) {
			if (XConceptExtension.instance().extractName(e).equals(eventClass)) {
				return trace.indexOf(e);
			}
		}
		return -1;
	}
	
	
}
