package org.processmining.behavioralspaces.algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeLiteralImpl;
import org.processmining.behavioralspaces.matcher.EventActivityMappings;
import org.processmining.behavioralspaces.models.behavioralspace.TraceBSpace;
import org.processmining.behavioralspaces.models.behavioralspace.XTraceTranslation;
import org.processmining.behavioralspaces.utils.BSpaceUtils;
import org.processmining.plugins.dc.decomp.DCComponents;
import org.processmining.plugins.log.abstraction.mapping.EventsToActivitiesMapping;


// only works with simple mappings
public class TraceToBSpaceTranslator {

	private EventActivityMappings maps;
		
	public TraceToBSpaceTranslator(EventActivityMappings maps) {
		this.maps = maps;
	}
	
	public TraceBSpace translateToBSpace(XTrace original) {
		TraceBSpace bspace = new TraceBSpace(original);

		List<EventsToActivitiesMapping> sampler = new ArrayList<EventsToActivitiesMapping>(maps);
		Collections.shuffle(sampler);
		List<EventsToActivitiesMapping> sample = sampler.subList(0,  Math.min(100, sampler.size()));
//		
		
		Set<String> traceStrings = new HashSet<String>();
		for (EventsToActivitiesMapping mapping : sample) {

			
			XTraceTranslation tt = translate(original, mapping);
			if (!traceStrings.contains(tt.getCharseq())) {
				bspace.addTraceTranslation(tt);
				traceStrings.add(tt.getCharseq());
			}
	
		}
//		System.out.println("Bspace size " + bspace.size());
		return bspace;
	}
	
	
	
	public XTraceTranslation translate(XTrace original, EventsToActivitiesMapping mapping) {
		
		XTraceTranslation translation = new XTraceTranslation(original, mapping);
		StringBuilder charseq = new StringBuilder();
		
		for (XEvent event : original) {
			XEvent translatedEvent = translateEvent(event, mapping);
			translation.add(translatedEvent);
			charseq.append(BSpaceUtils.extractName(translatedEvent) + ",");
		}
		translation.setCharseq(charseq.toString());
		return translation;
	}

	private XEvent translateEvent(XEvent originalEvent, EventsToActivitiesMapping mapping) {
		XEvent newEvent = (XEvent) originalEvent.clone();
		String eventName = BSpaceUtils.extractName(originalEvent);
		String activityName = getMappedActivity(eventName, mapping);
		
		newEvent.getAttributes().put("source", new XAttributeLiteralImpl("source", eventName));
		XConceptExtension.instance().assignName(newEvent, activityName);
		
		return newEvent;
	}
	


	private String getMappedActivity(String eventClass, EventsToActivitiesMapping mapping) {
		List<String> activities = new ArrayList<String>(mapping.getRelatedActivities(eventClass));
		if (activities.isEmpty()) {
			return eventClass;
		}
		// this only works with 1:1 mappings of course
		return activities.get(0);
	}
	
	
	
}
