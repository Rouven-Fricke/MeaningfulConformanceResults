package org.processmining.behavioralspaces.matcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.processmining.plugins.log.abstraction.mapping.EventMapping;
import org.processmining.plugins.log.abstraction.mapping.EventsToActivitiesMapping;
import org.processmining.plugins.log.abstraction.mapping.MapObject;

public class EventActivityMappings extends ArrayList<EventsToActivitiesMapping> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 566683317474286562L;
	private Map<String, Set<String>> eventToActivityMap;
	
	

	// only used for failed mapping attemps. don't use
	public EventActivityMappings() {
		super();
	}
	
	public EventActivityMappings(Collection<EventsToActivitiesMapping> col) {
		super(col);
		
	}

	public Set<Set<String>> computeAmbiguousActivitySets() {
		Set<Set<String>> result = new HashSet<Set<String>>();
		this.eventToActivityMap = new HashMap<String, Set<String>>();
		for (EventsToActivitiesMapping map : this) {
			for (EventMapping eventMapping : map) {
				for (String event : eventMapping.getEventNames()) {
					Set<String> activities;
					if (eventToActivityMap.containsKey(event)) {
						activities = eventToActivityMap.get(event);
					} else {
						activities = new HashSet<String>();
						eventToActivityMap.put(event, activities);
					}
					for (MapObject act : eventMapping.getActivities()) {
						activities.add(act.getName());
					}
				}
			}
		}
		for (Set<String> activitySet : eventToActivityMap.values()) {
			if (activitySet.size() > 1) {
				result.add(activitySet);
			}
		}
		return result;
		
	}
	
//	private String getMappedActivity(String eventClass) {
//		List<String> activities = new ArrayList<String>(mapping.getRelatedActivities(eventClass));
//		if (activities.isEmpty()) {
//			return eventClass;
//		}
//		// this only works with 1:1 mappings of course
//		return activities.get(0);
//	}
	
	// used for evaluation when correct mapping is given by events and activities with equal labels
	public boolean containsCorrectMapping() {
		for (EventsToActivitiesMapping map : this) {
			if (isCorrectMapping(map)) {
				return true;
			}
		}
		return false;
	}
	
	
	private boolean isCorrectMapping(EventsToActivitiesMapping map) {
		for (EventMapping eMap : map ) {
			for (String eventName : eMap.getEventNames()) {
				Set<String> activities;
				activities = new HashSet<String>();
				eventToActivityMap.put(eventName, activities);
				for (MapObject act : eMap.getActivities()) {
					activities.add(act.getName());
				}
				if (!activities.contains(eventName)) {
					return false;
				}
			}
		}
		return true;
	}
	
	

	
}
