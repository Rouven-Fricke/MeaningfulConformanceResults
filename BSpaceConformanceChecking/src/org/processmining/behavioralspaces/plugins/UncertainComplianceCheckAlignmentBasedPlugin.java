package org.processmining.behavioralspaces.plugins;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.collections.ListUtils;
import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.math.plot.utils.Array;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.acceptingpetrinet.models.impl.AcceptingPetriNetFactory;
import org.processmining.behavioralspaces.algorithms.TraceToBSpaceTranslator;
import org.processmining.behavioralspaces.evaluation.SingleModelEvaluationResult;
import org.processmining.behavioralspaces.matcher.EventActivityMappings;
import org.processmining.behavioralspaces.matcher.EventToActivityMapper;
import org.processmining.behavioralspaces.models.behavioralspace.BSpaceLog;
import org.processmining.behavioralspaces.models.behavioralspace.DeviationMatrix;
import org.processmining.behavioralspaces.models.behavioralspace.DeviationSet;
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
import org.processmining.plugins.astar.petrinet.PetrinetReplayerWithoutILP;
import org.processmining.plugins.connectionfactories.logpetrinet.EvClassLogPetrinetConnectionFactoryUI;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.dc.conf.ConformanceKit;
import org.processmining.plugins.dc.decomp.DCComponent;
import org.processmining.plugins.dc.decomp.DCComponents;
import org.processmining.plugins.dc.decomp.DCDecomposition;
import org.processmining.plugins.dc.decomp.DCDecompositionNode;
import org.processmining.plugins.dc.plugins.SESEDecompositionPlugin;
import org.processmining.plugins.log.XLogHelper;
import org.processmining.plugins.petrinet.replayer.PNLogReplayer;
import org.processmining.plugins.petrinet.replayer.algorithms.costbasedcomplete.CostBasedCompleteParam;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;

import nl.tue.astar.AStarException;

@Plugin(name = "00000 Behavioral space event-activity conformance using decomposed alignments", parameterLabels = {"PetriNet", "Log", "Event-activity mappings"}, 
returnLabels = { "Behavioral Space-based Event Log" }, returnTypes = { SingleModelEvaluationResult.class },  userAccessible = true)
public class UncertainComplianceCheckAlignmentBasedPlugin {

	
	public static String SER_FOLDER = "sers";
	
	public static boolean RECOMPUTE_ETAMS = false;
	
	Petrinet net;
	Marking iniM;
	Marking finalM;
	XLog log;
	EventActivityMappings etams;
	private TransEvClassMapping transEventMap;
	private XEventClassifier classifierMap;
	private DCDecomposition decomposition;
	private DCComponents components;
	
	
	private XEventClassifier eventClassifier = XLogInfoImpl.NAME_CLASSIFIER;
	private XEventClasses classes;
	
	//Stores the List of the HashMaps on the translation level and their non-compl components.
	//allUnambig/ambig/devSet are store Trace level information.
	private HashMap<Integer, List<HashMap<Integer, String>>> allUnambig = new HashMap<Integer, List<HashMap<Integer, String>>>();
	private HashMap<Integer, List<HashMap<Integer, String>>> allAmbig = new HashMap<Integer, List<HashMap<Integer, String>>>();

	private HashMap<Integer, List<HashMap<Integer, String>>> devSet = new HashMap<Integer, List<HashMap<Integer, String>>>(); 
	
	private List<HashMap<Integer, List<HashMap<Integer, String>>>> devList = new ArrayList<HashMap<Integer, List<HashMap<Integer, String>>>>();
	private List<Integer> deviatingTraces = new ArrayList<Integer>();
	
	private int traceNumber = 0;
	private List<String> listU = new ArrayList<String>();//list of unique unambiguous components
	private List<String> listA = new ArrayList<String>();// list of unique ambiguous components
	private List<DCComponent> listCompU = new ArrayList<DCComponent>();
	private List<DCComponent> listCompA = new ArrayList<DCComponent>();
	
	public enum ExecMode {
		NAIVE, EFFICIENT
	}
	

	@UITopiaVariant(affiliation = "VU University Amsterdam", author = "Han van der Aa", email = "j.h.vander.aa" + (char) 0x40 + "vu.nl")
	@PluginVariant(variantLabel = "benchmark single model", requiredParameterLabels = {0,1})
	public void exec(PluginContext context, Petrinet net, XLog log) throws Exception {
		
		XLogInfo logInfo = XLogInfoFactory.createLogInfo(log, eventClassifier);
		classes = logInfo.getEventClasses();

		boolean loadEtamFromSER = true;
		EventActivityMappings etams = EventToActivityMapper.obtainEtams(context, "input/etamsers/", net, log, loadEtamFromSER, EvaluationPlugin.MAX_MAPPING_TIME);		
		exec(context, net, log, etams, false);
		
	}
	
	@UITopiaVariant(affiliation = "VU University Amsterdam", author = "Han van der Aa", email = "j.h.vander.aa" + (char) 0x40 + "vu.nl")
	@PluginVariant(variantLabel = "benchmark single model", requiredParameterLabels = {0,1,2 })
	public BSpaceLog exec(PluginContext context, Petrinet net, XLog log, EventActivityMappings etams, boolean alsoRunNaive)  {
		
		this.net = net;
		this.log = log;
		this.etams = etams;
		
		XLogInfo logInfo = XLogInfoFactory.createLogInfo(log, eventClassifier);
		classes = logInfo.getEventClasses();
		
		SESEDecompositionPlugin decomposer = new SESEDecompositionPlugin();
		Object[] decompResult = decomposer.decompose(context, net);
		decomposition = (DCDecomposition) decompResult[0];
		components = (DCComponents) decompResult[1];
		
		
		
		double naiveTime = 0;
		if (alsoRunNaive) {
			System.out.println("Starting naive conformance check. Log size: " + log.size());
			long startTimeN = System.nanoTime();
			exec(context, net, log, ExecMode.NAIVE);
			long endTimeN = System.nanoTime();
			naiveTime = IOHelper.round((endTimeN - startTimeN) / 1000000000.0, 3);
			System.out.println("naive check took: " + naiveTime + " seconds");
		}
		
		

		
		System.out.println("Starting efficient conformance check. Log size: " + log.size());
		
		long startTimeE = System.nanoTime();
		BSpaceLog bspaceLog = exec(context, net, log, ExecMode.EFFICIENT);
		long endTimeE = System.nanoTime();
		double effTime = IOHelper.round((endTimeE - startTimeE) / 1000000000.0, 3);
		
		bspaceLog.setNaiveTime(naiveTime);
		bspaceLog.setEffTime(effTime);
		
		System.out.println("naive check took: " + naiveTime + " seconds");
		System.out.println("efficient check took: " + effTime + " seconds");
		
		bspaceLog.setDCComponents(components);
		return bspaceLog;
	}
	
	
	@UITopiaVariant(affiliation = "VU University Amsterdam", author = "Han van der Aa", email = "j.h.vander.aa" + (char) 0x40 + "vu.nl")
	@PluginVariant(variantLabel = "default", requiredParameterLabels = {0,1})
	public void execDefault(PluginContext context, Petrinet net, XLog log) throws Exception {
		
		this.net = net;
		this.log = log;
		
		etams = EventToActivityMapper.obtainEtams(context, SER_FOLDER, net, log, RECOMPUTE_ETAMS, AlignmentBasedCheckingBenchmarkPlugin.MAX_MAPPING_TIME);
				
		SESEDecompositionPlugin decomposer = new SESEDecompositionPlugin();
		Object[] decompResult = decomposer.decompose(context, net);
		decomposition = (DCDecomposition) decompResult[0];
		components = (DCComponents) decompResult[1];
		
		long startTimeE = System.nanoTime();
		exec(context, net, log, ExecMode.EFFICIENT);
		long endTimeE = System.nanoTime();
		long effTime = endTimeE - startTimeE;
		
		System.out.println("efficient check took: " + (effTime / 1000000000.0) + " seconds");
		
	}
	
		
	private BSpaceLog exec(PluginContext context, Petrinet net, XLog log, ExecMode mode) {
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
				System.out.println("component : " + comp.getName() + " " + comp.getTrans() + " by: " + actSet);

			}
			System.out.println("components affected by ambiguity: " + ambiguousComponents.size() + " " + ambiguousComponents);

			Set<String> childComponents = new HashSet<String>();
//			for (String compName : ambiguousComponents) {
//				childComponents.addAll(getChildComponents(decomposition, compName, new HashSet<String>()));
//			}
			ambiguousComponents.addAll(childComponents);
			
			unambiguousComponents = decomposition.getConformableNodesNames();
			unambiguousComponents.removeAll(ambiguousComponents);
			
			bspaceLog.setUnambigComponents(unambiguousComponents);
			
		}
		else {
			unambiguousComponents = new HashSet<String>();
			ambiguousComponents = decomposition.getConformableNodesNames();
		}
		System.out.println(decomposition.getConformableNodesNames());
		System.out.println("mode: " + mode);
		System.out.println("check once: " + unambiguousComponents.toString());
		System.out.println("repeat check: " + ambiguousComponents.toString());
		
		
		
		// time for compliance checking	
		AcceptingPetriNet acceptingNet = AcceptingPetriNetFactory.createAcceptingPetriNet(net);
		iniM = acceptingNet.getInitialMarking();
		finalM = (new ArrayList<Marking>(acceptingNet.getFinalMarkings())).get(0);

		//Get the Log Events / Petri net Transition mapping
		transEventMap = computeTransEventMapping(log, net);

		//Get the classifier used for the Log Events / Petri net Transition mapping
		classifierMap = XLogInfoImpl.NAME_CLASSIFIER;
		

		TraceToBSpaceTranslator translator = new TraceToBSpaceTranslator(etams);

		//get the sampled traces
		ArrayList<Integer> traceNumberList = RouvensPlaygroundPlugin.getTraceNumbers();


		for(int i = 0; i < traceNumberList.size(); i++) {
		//for(XTrace trace : traceList) {
		//for (int  i= 0; i<log.size();i+=500) {
			
			TraceBSpace tbs = translator.translateToBSpace(log.get(i));
			//TraceBSpace tbs = translator.translateToBSpace(trace);	
			bspaceLog.add(tbs);
			checkConformance(context, tbs, ambiguousComponents, unambiguousComponents, mode);
			this.traceNumber = traceNumberList.get(i);
			System.out.println(traceNumberList.get(i));

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
	

	private boolean checkConformance(PluginContext context, TraceBSpace bspace, Set<String> ambiguousComponents, Set<String> unambiguousComponents, ExecMode mode) {
		
		//Lists that store the different hashMaps to allow for the same translation to have multiple non-compl components.
		//Each HashMap stores one translation number and a non compl. component
		//Translation level
		List<HashMap<Integer, String>> aHashMapList = new ArrayList<HashMap<Integer, String>>();
		List<HashMap<Integer, String>> uHashMapList = new ArrayList<HashMap<Integer, String>>();
		List<HashMap<Integer, String>> totalDevsMapList = new ArrayList<HashMap<Integer, String>>();
		

		
		XLogInfo logInfo = XLogInfoFactory.createLogInfo(log, classifierMap);
		XLog tempLog = XLogHelper.initializeLog(log);
		
		tempLog.add(bspace.getTranslations().iterator().next());
		
		// instantiate replayer
		PNLogReplayer replayer = new PNLogReplayer();
		PetrinetReplayerWithoutILP replayerWithoutILP = new PetrinetReplayerWithoutILP();
		
		
		//Unambiguous components

		int unambigComps = 0;
		for (String compName : unambiguousComponents) {
			//System.out.println("Unambiguous CompName: " + compName);
			ConformanceKit kit; //Conformance for only the relevant component components.getComponent(compName)??
				try {
				kit = new ConformanceKit(components.getComponent(compName), iniM, finalM, tempLog, transEventMap, classifierMap);
				} catch (NullPointerException e) {
					for (XTraceTranslation tt : bspace.getTranslations()) {
						tt.setComponentCompliance(compName, true);
					}
					break;
				}
			
			CostBasedCompleteParam parameters;
				parameters = new CostBasedCompleteParam(logInfo.getEventClasses().getClasses(),
						transEventMap.getDummyEventClass(), kit.getNet().getTransitions(), 2, 5);
				parameters.getMapEvClass2Cost().remove(transEventMap.getDummyEventClass());
				parameters.getMapEvClass2Cost().put(transEventMap.getDummyEventClass(), 1);

				parameters.setGUIMode(false);
				parameters.setCreateConn(false);
				parameters.setInitialMarking(kit.getIniM());
				parameters.setFinalMarkings(new Marking[] {kit.getEndM()});
				parameters.setMaxNumOfStates(50);
				
//f�r jede UnambigComponent check compliance on the first translation of a trace. Assign compliance value to all
//translation of the trace, as it is unambiguous. Then, check for next unambigComp.				
			
			boolean compl = true;
			try {//Teil vorher auskommentiert
				PNRepResult res = replayer.replayLog(context, kit.getNet(), kit.getLog(), kit.getMap(), replayerWithoutILP, parameters);
				if (!res.isEmpty()) {
					double fit = (Double) res.getInfo().get(PNRepResult.TRACEFITNESS);//Fitness of the first translation of a trace
					compl = (fit >= 1.0);
					if(fit < 1) {
						if(!listU.contains(compName)) {
							listU.add(compName);//we only store the unique unambiguous comps in this list.
						}
						if(!listCompU.contains(components.getComponent(compName))) {
							listCompU.add(components.getComponent(compName));
						}
					}

					
				}
			} catch (AStarException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NullPointerException e) {
				
			}

			int uTransNo = 0;
			for (XTraceTranslation tt : bspace.getTranslations()) { //set the compliance of all translations according to the
			
				tt.setComponentCompliance(compName, compl);
				if(!compl) {
					unambigComps++;
					if(uTransNo == 0 && this.traceNumber == 5500) {//only print once, as its the same for all transl.
						System.out.println("Unambig: Trace No. "+ traceNumber + ", CompName: " + compName + " Compl: " + compl);
						for(XEvent e : tt.getOriginal()) {
							System.out.print(classes.getClassOf(e)+ ", ");	
						}
						System.out.println();
					}
					
					HashMap<Integer, String> nonCompUnambig = new HashMap<Integer,String>();
					nonCompUnambig.put(uTransNo, compName);//create a new HashMap for the noncompliant component and the translation number
					uHashMapList.add(nonCompUnambig);//always create a new HashMap per translation and add it to the List to allow for multiple non-compl components for the same translation
					totalDevsMapList.add(nonCompUnambig);
					if(!deviatingTraces.contains(this.traceNumber)) {
						deviatingTraces.add(this.traceNumber);
					}
					//System.out.println("Transition of an unambig noncompl comp?: " +components.getComponent(compName).getTrans());
					
				}
			
				uTransNo++;
			}
			this.allUnambig.put(this.traceNumber, uHashMapList);//two HashMaps to be able to output only unabmiguous/ambiguous and
			this.devSet.put(this.traceNumber, uHashMapList);//all non-compliant components together.
		}
		devList.add(allUnambig);

		
		//ambiguous Components
		for (String compName : ambiguousComponents) {
			for (XTraceTranslation tt : bspace.getTranslations()) {
				tt.setComponentCompliance(compName, true);
			}
		
			List<XTraceTranslation> sampler = new ArrayList<XTraceTranslation>(bspace.getTranslations());
			Collections.shuffle(sampler);
			List<XTraceTranslation> sample = sampler.subList(0,  Math.min(100, sampler.size() - 1));

			int aTransNo = 0;
			for (XTraceTranslation tt : sample) {
				
				boolean seenCompl = false;
				boolean seenNoncompl = false;
				tempLog.retainAll(new ArrayList<XTrace>());//Deletes all entries of the log and only creates a new empty ArrayList of XTraces?
				tempLog.add(tt);
				
				ConformanceKit kit;
					try {
					kit = new ConformanceKit(components.getComponent(compName), iniM, finalM, tempLog, transEventMap, classifierMap);
					//for each translation we check the conformance with a new ConformanceKit and the respective
					//component of the loop. Should be different for each translation.	
					} catch (NullPointerException e) {
						System.out.println("NullPointerException gecatched");
						tt.setComponentCompliance(compName, true);
						break;
					}

				CostBasedCompleteParam parameters;
					parameters = new CostBasedCompleteParam(logInfo.getEventClasses().getClasses(),
							transEventMap.getDummyEventClass(), kit.getNet().getTransitions(), 2, 5);
					parameters.getMapEvClass2Cost().remove(transEventMap.getDummyEventClass());
					parameters.getMapEvClass2Cost().put(transEventMap.getDummyEventClass(), 1);

					parameters.setGUIMode(false);
					parameters.setCreateConn(false);
					parameters.setInitialMarking(kit.getIniM());
					parameters.setFinalMarkings(new Marking[] {kit.getEndM()});
					parameters.setMaxNumOfStates(25);

				boolean compl = true;
				if (!kit.getLog().isEmpty()) {
					try {
						XEventClass evClassDummy = EvClassLogPetrinetConnectionFactoryUI.DUMMY;
						TransEvClassMapping newMap = new TransEvClassMapping(XLogInfoImpl.NAME_CLASSIFIER, evClassDummy);
						for (Transition t : kit.getMap().keySet()) {
							if (kit.getMap().get(t) != null) {
								newMap.put(t, kit.getMap().get(t));
							}
						}

						PNRepResult res = replayer.replayLog(context, kit.getNet(), kit.getLog(), newMap, replayerWithoutILP, parameters);
						if (!res.isEmpty()) {
							double fit = (Double) res.getInfo().get(PNRepResult.TRACEFITNESS);
							compl = (fit >= 1.0);
							if(fit < 1) {
								if(!listA.contains(compName)) {
									listA.add(compName); //get unique ambiguous noncompl comps.
									//listA.add(components.getComponent(compName)); //listA und listU mit DCComponents machen f�r unique Liste an Comps mit deren Transitionen
								}
								if(!listCompA.contains(components.getComponent(compName))) {
									listCompA.add(components.getComponent(compName));
								}
							}
							
						}
						tt.setComponentCompliance(compName, compl);
						
						//just for testing purposes, no further use, 5500 arbitrarily chosen
						if(this.traceNumber == 5500) {
							System.out.print("Ambig: Trace No. " + this.traceNumber + "Translation No. " + aTransNo + ", ");
							for(XEvent e : tt.getOriginal()) {
								System.out.print(classes.getClassOf(e)+ ", ");	
							}
							System.out.println();
						}
						
						if(!compl) {
							if(this.traceNumber == 5500) {
								//System.out.println("Ambig: Trace No. "+ traceNumber + "Translation No: " + aTransNo + ", CompName: " + compName + " Compl: " + compl);
							}
							
							HashMap<Integer, String> nonCompAmbig = new HashMap<Integer, String>();
							nonCompAmbig.put(aTransNo, compName);
							aHashMapList.add(nonCompAmbig);
							totalDevsMapList.add(nonCompAmbig);
							
							if(!deviatingTraces.contains(this.traceNumber)) {
								deviatingTraces.add(this.traceNumber);//get the numbers of the traces with fitness problems
							}
						}
						
						
						if (compl) {
							seenCompl = true;
						} else {
							seenNoncompl = true;
						}
						
						if (seenCompl && seenNoncompl) {
							return false;
						}
						
					} catch (AStarException e) {
						e.printStackTrace();
					} catch (NullPointerException e) {
						
					}
				}
				aTransNo++;
			}
			this.allAmbig.put(this.traceNumber, aHashMapList);
			this.devSet.put(this.traceNumber, aHashMapList);
		}
		devList.add(allAmbig);
		return true;
	}

public void printUnambiguousNonCompliantComps() {
	for(Map.Entry<Integer, List<HashMap<Integer, String>>> unambigNonComp : this.allUnambig.entrySet()) {
		int traceNo = unambigNonComp.getKey();
		List<HashMap<Integer, String>> traceLevelUnambigNonComp = unambigNonComp.getValue();
		for(int i = 0;i< traceLevelUnambigNonComp.size();i++) {
			boolean onlyFirstTranslation = true;
			for(Map.Entry<Integer, String> translationEntry : traceLevelUnambigNonComp.get(i).entrySet()) {
				String comp = translationEntry.getValue();
				if(onlyFirstTranslation) {
					System.out.println("TraceNo: " + traceNo + " Non compliant Unambig Comp: " + comp + "-> " + components.getComponent(comp).getTrans());
				}
			}
		}
	}
}

public void printAmbiguousNonCompliantComps() {
	for(Map.Entry<Integer, List<HashMap<Integer, String>>> ambigNonComp : this.allAmbig.entrySet()) {
		int traceNo = ambigNonComp.getKey();
		List<HashMap<Integer, String>> traceLevelAmbigNonComp = ambigNonComp.getValue();
		for(int i = 0;i<traceLevelAmbigNonComp.size();i++) {
			for(Map.Entry<Integer, String> translationEntry : traceLevelAmbigNonComp.get(i).entrySet()) {
				int translationNo = translationEntry.getKey();
				String comp = translationEntry.getValue();
				System.out.println("TraceNo: " + traceNo + " Translation No: " + translationNo + " Non compliant Ambig Comp: " + comp + "-> " + components.getComponent(comp).getTrans());
			}
		}
	}
	
}

public void printDeviationSets() {
	for(HashMap<Integer, List<HashMap<Integer, String>>> devSet : devList) {//HashMap allUnambig and HashMap allAmbig
		for(Map.Entry<Integer, List<HashMap<Integer, String>>> entry : devSet.entrySet()) {//iterate through entries of each HMap
			int traceNo = entry.getKey();
				for(HashMap<Integer, String> translList : entry.getValue()) {//Iterate through the list of HashMaps of a translNo and a non-compl Comp
					for(Map.Entry<Integer, String> translResult : translList.entrySet()) {//get the translNo and String of a single HashMap
						int translation = translResult.getKey();
						String deviationName =  translResult.getValue();
						System.out.println("Trace No. " + traceNo + " Translation No. " + translation + " Deviating Component: " + deviationName);
					}
				}
		}
	}
	System.out.println("Deviating Traces: " + deviatingTraces + " Size: " + deviatingTraces.size());
}

//use this method to get the dev comps of a specific trace
//not needed anymore, could be changed
/*private List<String> getDeviatingCompsOfTrace(int traceNo) {
	List<String> deviatingComps = new ArrayList<String>();
	for(HashMap<Integer, List<HashMap<Integer, String>>> devSet : devList) {
		for(Map.Entry<Integer, List<HashMap<Integer, String>>> entry : devSet.entrySet()) {
			int traceNumber = entry.getKey();
			if(traceNo == traceNumber) {
				for(HashMap<Integer, String> translList : entry.getValue()) {
					for(Map.Entry<Integer, String> translResult : translList.entrySet()) {
						String deviationName =  translResult.getValue();
						//System.out.println("Trace No. " + traceNo + " Translation No. " + translation + " Deviating Component: " + deviationName);
						if(!deviatingComps.contains(deviationName)) {
							deviatingComps.add(deviationName);
						}
					}
				}
			}
		}

	}
	//System.out.println("Deviating Comps for Trace: " + traceNo + ": " + deviatingComps);
	return deviatingComps;
}*/

//constructs a deviation Matrix over the full set of deviating components and orders them lexicographically 
public DeviationMatrix constructDeviationMatrix(DeviationSet[] ds, int traceNo) {
	//List<String> deviatingComps = this.getDeviatingCompsOfTrace(traceNo);
	List<String> deviatingComps = new ArrayList<String>();
	deviatingComps.addAll(getUniqueAmbigComps());
	deviatingComps.addAll(getUniqueUnambigComps());
	Collections.sort(deviatingComps);
	//String[][] matrixEntries =  new String[getDeviatingCompsOfTrace(traceNo).size()+1][getDeviatingCompsOfTrace(traceNo).size()+1];
	String[][] matrixEntries = new String[deviatingComps.size()+1][deviatingComps.size()+1];
	matrixEntries[0][0] = "Components";
	
	
	int ithComp = 1;
	int jthComp = 1;
	//vertically fill in the first column with the Comp Names
	//horizontally fill in the first row with the Comp Names
	for(String singleComp : deviatingComps) {
		matrixEntries[ithComp][0] = singleComp;
		matrixEntries[0][jthComp] = singleComp;
		jthComp++;
		ithComp++;
	}
	ithComp = 1;
	for(String singleComponent : deviatingComps) {
		Map<String, Integer> hm = new HashMap<String, Integer>();//Elements and their frequency of co-occurrence across all devSets
		
		//save the co-occurring deviating comps and their frequency of occurrence.
		// hashmap to store the frequency of element 
		for(DeviationSet devSet : ds) {
			getNumberedCoOccurrences2(singleComponent, hm, devSet);
		}
		
		jthComp = 1;
		TreeMap<String, Integer> sortedMap = sortbykey(hm); //sort both lists lexicographically to have a 0-diagonal matrix.
		System.out.print(" " + sortedMap.size() + " ");
		if(sortedMap.size() == 0) {//fill rows and columns with zero
			for(int j = 1; j<deviatingComps.size() + 1; j++) {
				matrixEntries[ithComp][j] = "0";//Row
				matrixEntries[j][deviatingComps.indexOf(singleComponent)+1] = "0";//Column
			}
		}
		for(Map.Entry<String, Integer> val : sortedMap.entrySet()) {
			
			//fill matrix
			if(matrixEntries[ithComp][jthComp] == null) {//nur wenn es vorher noch nicht mit 0 bef�llt wurde
				matrixEntries[ithComp][jthComp] = Integer.toString(val.getValue());
			}else {//wenn es mit 0 bef�llt wurde durchgehen, bis es zu einer leeren Stelle kommt
				//System.out.print("else bedingung" + ": " + matrixEntries[ithComp][jthComp]);
				while(matrixEntries[ithComp][jthComp] == "0" /*&& jthComp <= deviatingComps.size() +1*/) {
					jthComp++;
				}
				matrixEntries[ithComp][jthComp] = Integer.toString(val.getValue());
			}
			
			jthComp++;
		}
		
		System.out.println();
		
		ithComp++;
	}
	//fill in the missing values with 0s.
	for(int i = 1;i<deviatingComps.size()+1;i++) {
		 for(int j = 1;j < deviatingComps.size()+1;j++) {
			 if(matrixEntries[i][j] == null) {
				 matrixEntries[i][j] = "0";
			 }
		 }
	 }
	return new DeviationMatrix(matrixEntries);
}

//create a dummy full deviation matrix initialized with 0s (zeroes) so we can properly add the actual values.
public DeviationMatrix createInitialMatrix() {
	List<String> allComps = new ArrayList<String>();
	for(DCComponent str : getUniqueDCComps()) {
		allComps.add(str.getName());
		Collections.sort(allComps);
	}

	//initialize the matrix with the correct size
	String[][] matrixEntries = new String[allComps.size()+1][allComps.size()+1];
	matrixEntries[0][0] = "Components";
	
	int ithComp = 1;
	int jthComp = 1;
	//vertically fill in the first column with the Comp Names
	//horizontally fill in the first row with the Comp Names
	for(String singleComp : allComps) {
		matrixEntries[ithComp][0] = singleComp;
		matrixEntries[0][jthComp] = singleComp;
		jthComp++;
		ithComp++;
	}
	
	//initialize the rest of the Matrix with 0-Strings(!)
	//cast to int and back during adding ofc.
	for(int i = 1; i <=allComps.size(); i++) {
		for(int j = 1; j<= allComps.size();j++) {
			matrixEntries[i][j] = "0";
			System.out.print(matrixEntries[i][j]);
		}
		System.out.println();
	}
	
	return new DeviationMatrix(matrixEntries);

}



//maybe two methods, construct devset and getDevSet... and call construct() from getter.

public DeviationSet getSingleDevSet(int traceNo, int translationNo) {
	List<String> deviatingCompNames = new ArrayList<String>();
	double probability = 0.0;
	for(HashMap<Integer, List<HashMap<Integer, String>>> devSet : devList) {
		for(Map.Entry<Integer, List<HashMap<Integer, String>>> entry : devSet.entrySet()) {
			int traceNumber = entry.getKey();
			if(traceNumber == traceNo) {
				for(HashMap<Integer, String> translList : entry.getValue()) {//Iterate over the list of hashmaps
					for(Map.Entry<Integer, String> translResult : translList.entrySet()) {//get into each HashMap, Integer key is the number of the translation
						int translation = translResult.getKey();
						String deviationName =  translResult.getValue();

						if(translation == translationNo) {
							//System.out.println("Trace No. " + traceNumber + " Translation No. " + translation + " Deviating Component: " + deviationName);
							if(!deviatingCompNames.contains(deviationName)) {deviatingCompNames.add(deviationName);}
							
						}
					}
				}
			}
		}
		
		
	}
	
	double etamsSize = etams.size(); 
	probability = (1.0 / etamsSize);
	return new DeviationSet(deviatingCompNames, traceNo, translationNo, probability);//gets called with toString() methods in RouvensPlugin
}
	


public List<String> getUniqueAmbigComps() {
	 Collections.sort(listA);
	 return listA;
}

public List<String> getUniqueUnambigComps(){
	 Collections.sort(listU);
	 return listU;
}

public List<DCComponent> getUniqueDCComps() {
	
	List<DCComponent> list = new ArrayList<DCComponent>();
	list.addAll(listCompA);
	list.addAll(listCompU);
	
	return list;
}

public List<String> getAllNonConfComps(){
	List<String> list = new ArrayList<String>();
	list.addAll(listA);
	list.addAll(listU);
	return list;
}

//Function to sort map by Key 
private TreeMap<String, Integer> sortbykey(Map<String, Integer> map) { 
 // TreeMap to store values of HashMap 
 TreeMap<String, Integer> sorted = new TreeMap<>(); 

 // Copy all data from hashMap into TreeMap 
 sorted.putAll(map); 

 
 return sorted;
} 

private Map<String, Integer> getNumberedCoOccurrences2(String originalComp, Map<String, Integer> hm, DeviationSet ds){
	if(ds.getDevList().contains(originalComp)) {
		for(String comp : ds.getDevList()) {//for each comp of the whole list of non-compl comps
			if(!comp.equals(originalComp)) {
				Integer j = hm.get(comp); 
	            hm.put(comp, (j == null) ? 1 : j + 1); 
			}else if(comp.equals(originalComp)){
				hm.put(comp, 0);
			}
		}
	}
	List<String> allComps = this.getAllNonConfComps();
	for(String str : allComps) {
		if(!hm.containsKey(str)) {
			hm.put(str, 0);
		}
	}
	return hm;
}

	
}