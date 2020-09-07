package org.processmining.behavioralspaces.plugins;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.acceptingpetrinet.models.impl.AcceptingPetriNetFactory;
import org.processmining.behavioralspaces.algorithms.TraceToBSpaceTranslator;
import org.processmining.behavioralspaces.alignmentbased.AlignmentBasedChecker;
import org.processmining.behavioralspaces.evaluation.SingleModelEvaluationResult;
import org.processmining.behavioralspaces.matcher.EventActivityMappings;
import org.processmining.behavioralspaces.matcher.EventToActivityMapper;
import org.processmining.behavioralspaces.models.behavioralspace.BSpaceLog;
import org.processmining.behavioralspaces.models.behavioralspace.TraceBSpace;
import org.processmining.behavioralspaces.models.behavioralspace.XTraceTranslation;
import org.processmining.behavioralspaces.utils.IOHelper;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.connectionfactories.logpetrinet.EvClassLogPetrinetConnectionFactoryUI;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.dc.decomp.DCComponent;
import org.processmining.plugins.dc.decomp.DCComponents;
import org.processmining.plugins.dc.decomp.DCDecomposition;
import org.processmining.plugins.dc.decomp.DCDecompositionNode;
import org.processmining.plugins.dc.plugins.SESEDecompositionPlugin;

@Plugin(name = "00000 Behavioral space event-activity conformance using decomposed alignments", parameterLabels = {"PetriNet", "Log", "Event-activity mappings"}, 
returnLabels = { "Behavioral Space-based Event Log" }, returnTypes = { SingleModelEvaluationResult.class },  userAccessible = true)
public class UncertainConformanceCheckingPlugin {

	
	public static String SER_FOLDER = "sers";
	
	public static boolean RECOMPUTE_ETAMS = false;
	
//	Petrinet net;
//	Marking iniM;
//	Marking finalM;
//	XLog log;
//	EventActivityMappings etams;
	private XEventClassifier classifierMap;
	private DCDecomposition decomposition;
	private DCComponents components;
	
	public enum ExecMode {
		NAIVE, EFFICIENT
	}
	

	@UITopiaVariant(affiliation = "VU University Amsterdam", author = "Han van der Aa", email = "j.h.vander.aa" + (char) 0x40 + "vu.nl")
	@PluginVariant(variantLabel = "benchmark single model", requiredParameterLabels = {0,1})
	public void exec(PluginContext context, Petrinet net, XLog log) throws Exception {
		
		EventActivityMappings etams = EventToActivityMapper.obtainEtams(context, SER_FOLDER, net.getLabel(), net, log, RECOMPUTE_ETAMS, EvaluationPlugin.MAX_MAPPING_TIME);
				
		exec(context, net, log, etams, true);
		
	}
	
	@UITopiaVariant(affiliation = "VU University Amsterdam", author = "Han van der Aa", email = "j.h.vander.aa" + (char) 0x40 + "vu.nl")
	@PluginVariant(variantLabel = "benchmark single model", requiredParameterLabels = {0,1,2 })
	public BSpaceLog exec(PluginContext context, Petrinet net, XLog log, EventActivityMappings etams, boolean alsoRunNaive)  {

		
		//decomposition
		SESEDecompositionPlugin decomposer = new SESEDecompositionPlugin();
		Object[] decompResult = decomposer.decompose(context, net);
		decomposition = (DCDecomposition) decompResult[0];
		components = (DCComponents) decompResult[1];
		
		
		
		long startTimeE = System.nanoTime();
		BSpaceLog bspaceLog = exec(context, net, log, etams, ExecMode.EFFICIENT);
		long endTimeE = System.nanoTime();
		double effTime = IOHelper.round((endTimeE - startTimeE) / 1000000000.0, 3);
		
		bspaceLog.setNaiveTime(0);
		bspaceLog.setEffTime(effTime);
		
		bspaceLog.setDCComponents(components);
		return bspaceLog;
	}
	
		
	private BSpaceLog exec(PluginContext context, Petrinet net, XLog log, EventActivityMappings etams, ExecMode mode) {
		BSpaceLog bspaceLog = new BSpaceLog(net, log, etams);
		bspaceLog.setDecomposition(decomposition);
		
		Set<String> unambiguousComponents;
		Set<String> ambiguousComponents;
		if (mode == ExecMode.EFFICIENT) {
			ambiguousComponents = new HashSet<String>();
			Set<Set<String>> ambiguousActivitySets = etams.computeAmbiguousActivitySets();
			bspaceLog.setAmbiguousActSets(ambiguousActivitySets);
			for (Set<String> actSet : ambiguousActivitySets) {
				DCComponent comp =  findSmallestSESEWithActivities(actSet);
				ambiguousComponents.add(comp.getName());
				//vorher auskommentiert
				System.out.println("component : " + comp.getName() + " " + comp.getTrans() + " by: " + actSet);

			}
			//vorher auskommentiert
			System.out.println("components affected by ambiguity: " + ambiguousComponents);

			Set<String> childComponents = new HashSet<String>();
			for (String compName : ambiguousComponents) {
				childComponents.addAll(getChildComponents(decomposition, compName, new HashSet<String>()));
			}
			ambiguousComponents.addAll(childComponents);
			
			unambiguousComponents = decomposition.getConformableNodesNames();
			unambiguousComponents.removeAll(ambiguousComponents);
			
			bspaceLog.setUnambigComponents(unambiguousComponents);
			
		}
		else {
			unambiguousComponents = new HashSet<String>();
			ambiguousComponents = decomposition.getConformableNodesNames();
		}
		
//		System.out.println("mode: " + mode);
//		System.out.println("check once: " + unambiguousComponents.toString());
//		System.out.println("repeat check: " + ambiguousComponents.toString());
//		
//		
		
		// time for conformance checking	
		AcceptingPetriNet acceptingNet = AcceptingPetriNetFactory.createAcceptingPetriNet(net);
		Marking iniM = acceptingNet.getInitialMarking();
		Marking finalM = (new ArrayList<Marking>(acceptingNet.getFinalMarkings())).get(0);

		//Get the Log Events / Petri net Transition mapping
		TransEvClassMapping transEventMap = computeTransEventMapping(log, net);

		//Get the classifier used for the Log Events / Petri net Transition mapping
		classifierMap = XLogInfoImpl.NAME_CLASSIFIER;
		

		TraceToBSpaceTranslator translator = new TraceToBSpaceTranslator(etams);
		int i = 0;
		for (XTrace t : log) {
			TraceBSpace tbs = translator.translateToBSpace(t);
			bspaceLog.add(tbs);
//			checkConformance(context, tbs, ambiguousComponents, unambiguousComponents, mode);
			XLog tbsLog = tbs.translationsAsLog(log);
			AlignmentBasedChecker checker = new AlignmentBasedChecker(net, tbsLog, context);
			for (XTraceTranslation tt : tbs.getTranslations()) {
				tt.setCompliance(checker.isConformant(tt));
//				System.out.println(tt);
			}
			i++;
			if (i % 50 == 0) {
				System.out.println(i + " traces done.");
			}
		}

		return bspaceLog;
	}
	
	private DCComponent findSmallestSESEWithActivities(Set<String> activities) {
		return findSmallestSESE(activities, decomposition.getRoot());
	}
	
	private DCComponent findSmallestSESE(Set<String> activities, DCDecompositionNode current) {
		for (DCDecompositionNode child : current.getChildren()) {
			DCComponent comp = components.getComponent(child.getName());
			if (componentContainsTransitions(comp, activities)) {
				return findSmallestSESE(activities, child);
			}
		}
		return components.getComponent(current.getName());
	}
	
	private boolean componentContainsTransitions(DCComponent component, Set<String> activities) {
		Set<String> missing = new HashSet<String>(activities);
		for (Transition t : component.getTrans()) {
			missing.remove(t.getLabel());
		}
		return missing.isEmpty();
	}
	
	private TransEvClassMapping computeTransEventMapping(XLog log, Petrinet net) {
		XEventClass evClassDummy = EvClassLogPetrinetConnectionFactoryUI.DUMMY;
		TransEvClassMapping mapping = new TransEvClassMapping(XLogInfoImpl.NAME_CLASSIFIER, evClassDummy);
		XEventClasses ecLog = XLogInfoFactory.createLogInfo(log, XLogInfoImpl.NAME_CLASSIFIER).getEventClasses();
		for (Transition t : net.getTransitions()) {
			XEventClass eventClass = ecLog.getByIdentity(t.getLabel());
			if (eventClass != null) {
				mapping.put(t, eventClass);
			}

		}
		return mapping;
	}
	
	
	
	
private Set<String> getChildComponents(DCDecomposition decomp, String compName, Set<String> names) {
	for (DCDecompositionNode child : decomp.getNode(compName).getChildren()) {
		names.add(child.getName());
		if (!child.getChildren().isEmpty()) {
			names.addAll(getChildComponents(decomp, child.getName(), new HashSet<String>()));
		}
	}
	return names;
}


	
}
