package org.processmining.behavioralspaces.plugins;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

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
	
	
	private HashMap<Integer, List<HashMap<Integer, String>>> allUnambig = new HashMap<Integer, List<HashMap<Integer, String>>>();
	private HashMap<Integer, List<HashMap<Integer, String>>> allAmbig = new HashMap<Integer, List<HashMap<Integer, String>>>();
	//check, if devSet is needed somewhere???
	private HashMap<Integer, List<HashMap<Integer, String>>> devSet = new HashMap<Integer, List<HashMap<Integer, String>>>(); 
	
	private List<HashMap<Integer, List<HashMap<Integer, String>>>> devList = new ArrayList<HashMap<Integer, List<HashMap<Integer, String>>>>();
	private List<Integer> deviatingTraces = new ArrayList<Integer>();
	//private HashMap<Integer, HashMap<Integer,String>> allUnambig = new HashMap<Integer, HashMap<Integer,String>>();
	//private HashMap<Integer, HashMap<Integer,String>> allAmbig = new HashMap<Integer, HashMap<Integer,String>>();
	private int traceNumber = 0;
	private List<String> listU = new ArrayList<String>();
	private List<String> listA = new ArrayList<String>();
	
	public enum ExecMode {
		NAIVE, EFFICIENT
	}
	

	@UITopiaVariant(affiliation = "VU University Amsterdam", author = "Han van der Aa", email = "j.h.vander.aa" + (char) 0x40 + "vu.nl")
	@PluginVariant(variantLabel = "benchmark single model", requiredParameterLabels = {0,1})
	public void exec(PluginContext context, Petrinet net, XLog log) throws Exception {
		
		XLogInfo logInfo = XLogInfoFactory.createLogInfo(log, eventClassifier);
		classes = logInfo.getEventClasses();

		boolean loadEtamFromSER = true;
		//etams = EventToActivityMapper.obtainEtams(context, SER_FOLDER, net, log, RECOMPUTE_ETAMS, AlignmentBasedCheckingBenchmarkPlugin.MAX_MAPPING_TIME);
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
		System.out.println("repeat check: " + ambiguousComponents.toString());//why only one ambiguous component?
		
		
		
		// time for compliance checking	
		AcceptingPetriNet acceptingNet = AcceptingPetriNetFactory.createAcceptingPetriNet(net);
		iniM = acceptingNet.getInitialMarking();
		finalM = (new ArrayList<Marking>(acceptingNet.getFinalMarkings())).get(0);

		//Get the Log Events / Petri net Transition mapping
		transEventMap = computeTransEventMapping(log, net);

		//Get the classifier used for the Log Events / Petri net Transition mapping
		classifierMap = XLogInfoImpl.NAME_CLASSIFIER;
		

		TraceToBSpaceTranslator translator = new TraceToBSpaceTranslator(etams);
		
		
		for (int  i= 0; i<log.size();i+=500) {
		//int i  = 0;
		//for(XTrace t : log) {
			
			TraceBSpace tbs = translator.translateToBSpace(log.get(i));
			//TraceBSpace tbs = translator.translateToBSpace(t);	
			bspaceLog.add(tbs);
			checkConformance(context, tbs, ambiguousComponents, unambiguousComponents, mode);
			this.traceNumber = i;
			
			//i++;
		
			/*if (i % 5000 == 0) {
					System.out.println(i + "TRACES checked for: " + net.getLabel() + " " + etams.size() + " interpretations");
			}*/
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
		
		//Lists that store the different hashMaps to allow for the same translation to have multiple !compl components.
		List<HashMap<Integer, String>> aHashMapList = new ArrayList<HashMap<Integer, String>>();
		List<HashMap<Integer, String>> uHashMapList = new ArrayList<HashMap<Integer, String>>();
		List<HashMap<Integer, String>> totalDevsMapList = new ArrayList<HashMap<Integer, String>>();
		
		//HashMap<Integer, String> nonCompUnambig = new HashMap<Integer,String>();
		//HashMap<Integer, String> nonCompAmbig = new HashMap<Integer, String>();
		
		XLogInfo logInfo = XLogInfoFactory.createLogInfo(log, classifierMap);
		XLog tempLog = XLogHelper.initializeLog(log);
		
		tempLog.add(bspace.getTranslations().iterator().next());//TempLog only the log of the first translation??? 
		//Test only with the first translations are all the others are the same for unambiguous comps?->Probably yes.
		
		// instantiate replayer
		PNLogReplayer replayer = new PNLogReplayer();
		PetrinetReplayerWithoutILP replayerWithoutILP = new PetrinetReplayerWithoutILP();
		
		
		//Unambiguous components
		int compIt = 0;
		int unambigComps = 0;
		for (String compName : unambiguousComponents) {
			//System.out.println("Unambiguous CompName: " + compName);
			ConformanceKit kit; //Conformance for only the relevant component components.getComponent(compName)??
				try {
				kit = new ConformanceKit(components.getComponent(compName), iniM, finalM, tempLog, transEventMap, classifierMap);
				} catch (NullPointerException e) {//nur bei NullPointerException
					System.out.println("NULLPOINTER EXCEPTION--------------------------------------------");
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
				
//				parameters.setNumThreads(numThreads);
				
//für jede UnambigComponent check compliance on the first translation of a trace. Assign compliance value to all
//translation of the trace, as it is unambiguous. Then, check for next unambigComp.				
			
			boolean compl = true;
			try {//Teil vorher auskommentiert
				PNRepResult res = replayer.replayLog(context, kit.getNet(), kit.getLog(), kit.getMap(), replayerWithoutILP, parameters);
				if (!res.isEmpty()) {
					double fit = (Double) res.getInfo().get(PNRepResult.TRACEFITNESS);//Fitness of the first translation of a trace
					compl = (fit >= 1.0);
					if(fit < 1) {
						//System.out.println(res.getInfo().get(PNRepResult.TRACEFITNESS) + " " + compName + " " + components.getComponent(compName).getTrans() + " " + compl);
						if(!listU.contains(compName)) {
							listU.add(compName);
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
			//first one as it is an unambiguous component.
			//XTraceTranslation tt = bspace.getTranslations().iterator().next();	
				//System.out.print("Unambig: Trace No. " + i + ": ");
				if(compIt == 0) {
					/*for(XEvent e : tt.getOriginal()) {
						System.out.print(classes.getClassOf(e)+ ", ");
					}*/
					System.out.println();
					for(int l = 0;l<tt.getMapping().size();l++) {
						//System.out.println(l + ". Mapping EventList: " + tt.getMapping().get(l) + " - " + tt.getMapping().getActivities());
					}
				}
				
			
				tt.setComponentCompliance(compName, compl);
				if(!compl) {
					boolean unambigCompIsNonCompl = true;
					unambigComps++;
					System.out.println("Unambig: Trace No. "+ traceNumber + ", CompName: " + compName + " Compl: " + compl);
					HashMap<Integer, String> nonCompUnambig = new HashMap<Integer,String>();
					
					nonCompUnambig.put(uTransNo, compName);//why are unambiguous noncompliant components across traces always the same, including the same transition??
					uHashMapList.add(nonCompUnambig);
					totalDevsMapList.add(nonCompUnambig);
					if(!deviatingTraces.contains(this.traceNumber)) {
						deviatingTraces.add(this.traceNumber);
					}
					//System.out.println("Transition of an unambig noncompl comp?: " +components.getComponent(compName).getTrans());
					
				}
			
				compIt++;
				uTransNo++;
			}
			this.allUnambig.put(this.traceNumber, uHashMapList);
			this.devSet.put(this.traceNumber, uHashMapList);
		}
		devList.add(allUnambig);

		
		//ambiguous Components
		for (String compName : ambiguousComponents) {
			//System.out.println("GRÖßE:" + ambiguousComponents.size() +"Ambig. CompName: " + compName);
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
							double fit = (Double) res.getInfo().get(PNRepResult.TRACEFITNESS);//was anderes als TRACEFITNESS? MOVELOG; MODEK?
							compl = (fit >= 1.0);
							if(fit < 1) {
								if(!listA.contains(compName)) {
									listA.add(compName);
								}
							}
							
							/*for(Map.Entry<String, Object> info : res.getInfo().entrySet()) {
								System.out.println("STring: " + info.getKey() + " Obj: " + info.getValue());
							}*/
						}
						tt.setComponentCompliance(compName, compl);
						
						if(aTransNo == 0) {
							//System.out.print("Ambig: Trace No. " + j + ", ");
							for(XEvent e : tt.getOriginal()) {
								//System.out.print(classes.getClassOf(e)+ ", ");	
							}
							
						}
						if(!compl) {
							//System.out.println("Ambig. Component: " + compName + " Compliant?: "+ compl + " for Translation No. " + k);
							System.out.println("Ambig: Trace No. "+ traceNumber + "Translation No: " + aTransNo + ", CompName: " + compName + " Compl: " + compl);
							HashMap<Integer, String> nonCompAmbig = new HashMap<Integer, String>();
							
							nonCompAmbig.put(aTransNo, compName);
							aHashMapList.add(nonCompAmbig);
							totalDevsMapList.add(nonCompAmbig);
							
							if(!deviatingTraces.contains(this.traceNumber)) {
								deviatingTraces.add(this.traceNumber);
							}
						}
						
						aTransNo++;
						if (compl) {
							seenCompl = true;
						} else {
							seenNoncompl = true;
						}
						
						if (seenCompl && seenNoncompl) {
							return false;
						}
						
					} catch (AStarException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (NullPointerException e) {
						
					}
				}
			}
			//System.out.println("Total translations: " + bspace.getTranslations().size());
			this.allAmbig.put(this.traceNumber, aHashMapList);
			this.devSet.put(this.traceNumber, aHashMapList);
		}
		devList.add(allAmbig);
		//System.out.println(unambigComps + "Unambig non-compliant components");
	
		
		return true;
	}

public void printUnambiguousNonCompliantComps() {
	for(Map.Entry<Integer, List<HashMap<Integer, String>>> unambigNonComp : this.allUnambig.entrySet()) {
		int traceNo = unambigNonComp.getKey();
		List<HashMap<Integer, String>> traceLevelUnambigNonComp = unambigNonComp.getValue();
		for(int i = 0;i< traceLevelUnambigNonComp.size();i++) {
			boolean onlyFirstTranslation = true;
			for(Map.Entry<Integer, String> translationEntry : traceLevelUnambigNonComp.get(i).entrySet()) {
				int translationNo = translationEntry.getKey();
				String comp = translationEntry.getValue();
				if(onlyFirstTranslation) {
					System.out.println("TraceNo: " + traceNo + " Non compliant Unambig Comp: " + comp + "-> " + components.getComponent(comp).getTrans());
				}
			}
		}
	}
}

//Delete restricting if stmts.
public void printDeviationSets() {
	for(HashMap<Integer, List<HashMap<Integer, String>>> devSet : devList) {
		for(Map.Entry<Integer, List<HashMap<Integer, String>>> entry : devSet.entrySet()) {
			int traceNo = entry.getKey();
				for(HashMap<Integer, String> translList : entry.getValue()) {
					for(Map.Entry<Integer, String> translResult : translList.entrySet()) {
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
private List<String> getDeviatingCompsOfTrace(int traceNo) {
	List<String> deviatingComps = new ArrayList<String>();
	for(HashMap<Integer, List<HashMap<Integer, String>>> devSet : devList) {
		for(Map.Entry<Integer, List<HashMap<Integer, String>>> entry : devSet.entrySet()) {
			int traceNumber = entry.getKey();
			if(traceNo == traceNumber) {
				for(HashMap<Integer, String> translList : entry.getValue()) {
					for(Map.Entry<Integer, String> translResult : translList.entrySet()) {
						int translation = translResult.getKey();
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
	System.out.println("Deviating Comps for Trace: " + traceNo + ": " + deviatingComps);
	return deviatingComps;
}

//change return type to DeviationMatrix after having programmed that class
//ggf. also add the trace number as additional input for the method!
public DeviationMatrix constructDeviationMatrix(DeviationSet[] ds, int traceNo) {
	List<String> deviatingComps = this.getDeviatingCompsOfTrace(traceNo);
	String[][] matrixEntries =  new String[getDeviatingCompsOfTrace(traceNo).size()+1][getDeviatingCompsOfTrace(traceNo).size()+1];
	matrixEntries[0][0] = "Components";
	boolean topRowFilled = false;
	int k = 1;
	/*for(String str : ds[0].getDevList()) {
		System.out.println(str + "STR");
		matrixEntries[0][k] = str;
		k++;
	}*/
	int ithComp = 1;
	for(String singleComponent : deviatingComps) {
		Map<String, Integer> hm = new HashMap<String, Integer>();//Elements and their frequency of co-occurrence across all devSets
		
		for(DeviationSet devSet : ds) {
			if(devSet.getDevList().contains(singleComponent)) {
				//save the co-occurring deviating comps and their frequency of occurrence.
				// hashmap to store the frequency of element 
				for(String i : devSet.getDevList()) {
					if(!i.equals(singleComponent)) {
						Integer j = hm.get(i); 
			            hm.put(i, (j == null) ? 1 : j + 1); 
					}else if(i.equals(singleComponent)){
						hm.put(i, 0);
					}
					
				}
			}
			
		}
		System.out.print("Element " + singleComponent + " co-occurs: ");
		int jthComp = 1;
		matrixEntries[ithComp][0] = singleComponent;
		int counter = 1;
		for (Map.Entry<String, Integer> val : hm.entrySet()) {
			if(!topRowFilled) {
				matrixEntries[0][counter] = val.getKey();
			}
			//plus also fill the deviation matrix
			System.out.print(val.getValue() + " times with " + val.getKey() + ", ");
			
			//fill matrix
			matrixEntries[ithComp][jthComp] = Integer.toString(val.getValue());
			jthComp++;
			counter++;
		}
		topRowFilled = true;
		System.out.println();
		
		ithComp++;
	}
	return new DeviationMatrix(matrixEntries);
}


//maybe two methods, construct devset and getDevSet... and call construct() from getter.
//morgen sofort dann das DeviationSet befüllen.
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
	if(deviatingCompNames.size() == 0) {
		System.out.println("no deviating comp for this trace translation");
		deviatingCompNames.add("No deviating comp for this trace");
	}
	double etamsSize = etams.size(); 
	probability = (1.0 / etamsSize);
	return new DeviationSet(deviatingCompNames, traceNo, translationNo, probability);
}
	
public void printAmbiguousNonCompliantComps() {
	for(Map.Entry<Integer, List<HashMap<Integer, String>>> ambigNonComp : this.allAmbig.entrySet()) {
		int traceNo = ambigNonComp.getKey();
		List<HashMap<Integer, String>> traceLevelAmbigNonComp = ambigNonComp.getValue();
		for(int i = 0;i<traceLevelAmbigNonComp.size();i++) {
			boolean onlyFirstTranslation = true;
			for(Map.Entry<Integer, String> translationEntry : traceLevelAmbigNonComp.get(i).entrySet()) {
				int translationNo = translationEntry.getKey();
				String comp = translationEntry.getValue();
				if(onlyFirstTranslation) {
					System.out.println("TraceNo: " + traceNo + " Translation No: " + translationNo + " Non compliant Ambig Comp: " + comp /*+ "-> " + components.getComponent(comp).getTrans()*/);
				}
			}
		}
	}
	
}

public List<String> getUniqueAmbigComps() {
	return listA;

}

public List<String> getUniqueUnambigComps(){
	return listU;
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