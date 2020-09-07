package org.processmining.behavioralspaces.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;

public class BSpaceUtils {

	
	public static boolean hasLoops(XLog log) {
		for (XTrace t : log) {
			if (hasRepeatedActivities(t)) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean hasRepeatedActivities(XTrace t) {
		Set<String> labels = new HashSet<String>();
		for (XEvent e : t) {
			String eventClass = extractName(e);
			if (!labels.add(eventClass)) {
				return true;
			}
		}
		return false;
	}
	
	public static String extractName(XEvent e) {
		return XConceptExtension.instance().extractName(e);
	}
	
	public static List<String> getActivities(Petrinet net) {
		List<String> result = new ArrayList<String>();
		for (Transition t : net.getTransitions()) {
			if (!t.getLabel().isEmpty()) {
				result.add(t.getLabel());
			}
		}
		return result;
	}
	
//	public static String traceToCharString
	
	public static String traceCommaSepString(XTrace trace) {
		String res = "";
		for (XEvent e : trace) {
			if (res.isEmpty()) {
				res = extractName(e);
			} else {
				res = res + "," +extractName(e);
			}
		}
		return res;
	}
	
	public static List<String> traceToLabelList(XTrace trace) {
		List<String> res = new ArrayList<String>();
		Iterator<XEvent> eventIter = trace.iterator();
		while (eventIter.hasNext()) {
			XEvent xEvent = eventIter.next();
			res.add(XConceptExtension.instance().extractName(xEvent));
		}
		return res;
	}
	
}
