package org.processmining.behavioralspaces.matcher;

import java.io.File;

import org.deckfour.xes.model.XLog;
import org.processmining.behavioralspaces.utils.IOHelper;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.plugins.log.abstraction.mapping.constraints.ConstraintBasedMappingPlugin;
import org.processmining.plugins.log.abstraction.mapping.constraints.ConstraintMappingSettings;
import org.processmining.plugins.log.abstraction.mapping.constraints.ConstraintMappingSettings.CARDINALITY;
import org.processmining.plugins.log.abstraction.mapping.constraints.ConstraintMappingSettings.ConstraintGenerationType;
import org.processmining.plugins.log.abstraction.mapping.constraints.EventToActivitiesMappingResult;

public class EventToActivityMapper {

	
	public static EventActivityMappings obtainEtams(PluginContext context, Petrinet net, XLog log) {
		EventActivityMappings etams = null;
		System.out.println("Generating ETAMs for " + net.getLabel());
		try {
			EventToActivitiesMappingResult etamRes = ConstraintBasedMappingPlugin.suggestEventMappings(context, log, net, 
					new ConstraintMappingSettings());
			etams = new EventActivityMappings(etamRes.getEtamChoice().getPotentialMappings());
			System.out.println("Created ETAMs for " + net.getLabel() + " with " + etams.size() + " interpretations");
			System.gc();
		} catch (Error e) {
			System.out.println("No Mapping created for " + net.getLabel());
		} catch (Exception e) {
			System.out.println("No Mapping created for " + net.getLabel());
		}
		if (etams == null) {
			etams = new EventActivityMappings();
		}
		return etams;
	}
	
	public static EventActivityMappings obtainEtams(PluginContext context, String folder, Petrinet net, XLog log, boolean useSerialized, int maxMappingTime) {
		String netName = net.getLabel();
		EventActivityMappings etams = null;
		String serfilepath = folder + netName + ".ser";
		if (useSerialized) {
			File file = new File(serfilepath);
			etams = (EventActivityMappings) IOHelper.deserializeObject(file.getAbsolutePath());
			System.out.println("Loaded ETAMS for " + netName + " with " + etams.size() + " interpretations");
		}
		if (etams == null) {
			try {
				ConstraintMappingSettings settings = new ConstraintMappingSettings();
				settings.setMaxSolutionTime(maxMappingTime);
				settings.setProfileCompTablePath("output/tempFiles/" + netName + ".csv");
				settings.setActivtyEventCardinality(CARDINALITY.ONE_TO_ONE_MAPPING);
				settings.setConstraintGenerationType(ConstraintGenerationType.BEHAVIORAL_RELATIONS);
				settings.setUseDeclareForModelBehavior(false);

				EventToActivitiesMappingResult etamRes = ConstraintBasedMappingPlugin.suggestEventMappings(context, log, net, 
						settings);
				etams = new EventActivityMappings(etamRes.getEtamChoice().getPotentialMappings());
				System.out.println("Created ETAMS for " + netName + " with " + etams.size() + " interpretations");
				System.gc();
			} catch (Error e) {
				System.out.println("No Mapping created for " + netName);
			} catch (Exception e) {
				System.out.println("No Mapping created for " + netName);
			}
		}
		if (etams == null) {
			etams = new EventActivityMappings();
		}
		IOHelper.serializeObject(serfilepath, etams);
		return etams;
	}
	
	public static EventActivityMappings obtainEtams(PluginContext context, String folder, String netName, Petrinet net, XLog log, boolean useSerialized, int maxMappingTime) {
		EventActivityMappings etams = null;
		String serfilepath = folder + netName + ".ser";
		if (useSerialized) {
			File file = new File(serfilepath);
			if (file.exists()) {
				etams = (EventActivityMappings) IOHelper.deserializeObject(file.getAbsolutePath());
				System.out.println("Loaded ETAMS for " + netName + " with " + etams.size() + " interpretations");
			}
		}
		if (etams == null) {
			try {
				ConstraintMappingSettings settings = new ConstraintMappingSettings();
				settings.setMaxSolutionTime(maxMappingTime);
				settings.setProfileCompTablePath("output/tempFiles/" + netName + ".csv");
				settings.setActivtyEventCardinality(CARDINALITY.ONE_TO_ONE_MAPPING);
				settings.setConstraintGenerationType(ConstraintGenerationType.BEHAVIORAL_RELATIONS);
				settings.setUseDeclareForModelBehavior(false);

				EventToActivitiesMappingResult etamRes = ConstraintBasedMappingPlugin.suggestEventMappings(context, log, net, 
						settings);
				etams = new EventActivityMappings(etamRes.getEtamChoice().getPotentialMappings());
				System.out.println("Created ETAMS for " + netName + " with " + etams.size() + " interpretations");
				System.gc();
			} catch (Error e) {
				System.err.println(e);
				System.out.println("No Mapping created for " + netName);
			} catch (Exception e) {
				System.out.println("No Mapping created for " + netName);
			}
		}
		if (etams == null) {
			etams = new EventActivityMappings();
		}
		IOHelper.serializeObject(serfilepath, etams);
		return etams;
	}
	
}