package org.processmining.behavioralspaces.plugins;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.swing.JComponent;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.in.XParser;
import org.deckfour.xes.in.XesXmlParser;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XEventImpl;
import org.deckfour.xes.model.impl.XLogImpl;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.acceptingpetrinet.models.impl.AcceptingPetriNetFactory;
import org.processmining.behavioralspaces.algorithms.TraceToBSpaceTranslator;
import org.processmining.behavioralspaces.alignmentbased.AlignmentBasedChecker;
import org.processmining.behavioralspaces.evaluation.BSpaceComplianceEvaluator;
import org.processmining.behavioralspaces.matcher.EventActivityMappings;
import org.processmining.behavioralspaces.matcher.EventToActivityMapper;
import org.processmining.behavioralspaces.models.behavioralspace.BSpaceLog;
import org.processmining.behavioralspaces.models.behavioralspace.DeviationSet;
import org.processmining.behavioralspaces.models.behavioralspace.TraceBSpace;
import org.processmining.behavioralspaces.models.behavioralspace.XTraceTranslation;
import org.processmining.behavioralspaces.utils.BSpaceUtils;
import org.processmining.behavioralspaces.utils.IOHelper;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.connectionfactories.logpetrinet.EvClassLogPetrinetConnectionFactoryUI;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.dc.conn.DCDecompositionPetrinetConnection;
import org.processmining.plugins.dc.decomp.DCComponents;
import org.processmining.plugins.dc.decomp.DCDecomposition;
import org.processmining.plugins.dc.plugins.KPartitioningPlugin;
import org.processmining.plugins.dc.plugins.SESEDecompositionPlugin;
import org.processmining.plugins.petrinet.replayer.algorithms.IPNReplayParameter;
import org.processmining.plugins.petrinet.replayer.algorithms.costbasedprefix.CostBasedPrefixAlg;
import org.processmining.plugins.petrinet.replayer.algorithms.costbasedprefix.CostBasedPrefixParam;
import org.processmining.plugins.petrinet.replayer.algorithms.costbasedprefix.CostBasedPrefixUI;
import org.processmining.plugins.petrinet.replayer.algorithms.syncproduct.SyncProductAlg;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.petrinet.replayresult.StepTypes;
import org.processmining.plugins.replayer.replayresult.SyncReplayResult;

import gnu.trove.map.TObjectIntMap;
import nl.tue.alignment.Progress;
import nl.tue.alignment.Replayer;
import nl.tue.alignment.ReplayerParameters;
import nl.tue.alignment.ReplayerParameters.Default;
import nl.tue.alignment.TraceReplayTask;
import nl.tue.alignment.Utils;
import nl.tue.alignment.TraceReplayTask.TraceReplayResult;
import nl.tue.alignment.algorithms.ReplayAlgorithm;
import nl.tue.alignment.algorithms.ReplayAlgorithm.Debug;
import nl.tue.alignment.algorithms.implementations.AStarLargeLP;
import nl.tue.alignment.algorithms.syncproduct.SyncProduct;
import nl.tue.astar.AStarThread;

@Plugin(name = "Rouven's uncertain conformance checking testbed", parameterLabels = {}, 
returnLabels = { "Meaningful conformance results" }, returnTypes = {String.class },  userAccessible = true)
public class RouvensPlaygroundPlugin {

	Map<List<String>, Double> fitnessMap;
	
	private DCComponents components;
	private DCDecomposition decomposition;
	private int i = 0;
	@UITopiaVariant(affiliation = "University of Mannheim", author = "Rouven Fricke", email = "rfricke@mail.uni-mannheim.de")
	@PluginVariant(variantLabel = "Testbed for Rouven", requiredParameterLabels = {})
	public String exec(PluginContext context) throws Exception {
		
		// Step 0: load a Petri net and a corresponding event log, plus initalize stuff
		String caseName =  "Artificial - Review - Large";//"Artificial - Loan Process";//"BPIC15_1";//"Artificial - Claims"; //"Artificial - Loan Process - Partial";//"Hospital_log";//"Artificial - Claims";//"Road_Traffic_Fines_Management_Process";//"Artificial - Repair";////"Road_Traffic_Fines_Management_Process";//
		String netPath = "input/mwe/" + caseName + ".pnml";
		String logPath = "input/mwe/" + caseName + ".xes";
		Petrinet net = loadPetrinet(context, netPath);
		AcceptingPetriNet acceptingNet = AcceptingPetriNetFactory.createAcceptingPetriNet(net);
		Marking initMarking = acceptingNet.getInitialMarking();
		Marking finalMarking = getFinalMarking(net);
		
		XLog log = loadLog(logPath);
		XEventClassifier eventClassifier = XLogInfoImpl.NAME_CLASSIFIER;
		XLogInfo logInfo = XLogInfoFactory.createLogInfo(log, eventClassifier);
		XEventClasses classes = logInfo.getEventClasses();
		XAttributeMap logAttributes = log.getAttributes();
		TransEvClassMapping transEventMap = computeTransEventMapping(log, net); // not the same as the etams mappings created in Step 1
		
		//decomposition
		SESEDecompositionPlugin decomposer = new SESEDecompositionPlugin();
		Object[] decompResult = decomposer.decompose(context, net);
		decomposition = (DCDecomposition) decompResult[0];
		components = (DCComponents) decompResult[1];
		
		// Step 1: establish the possible event-to-activity mappings (leave parameters as is for the moment)
		boolean loadEtamFromSER = true;
		//EventActivityMappings etams = EventToActivityMapper.obtainEtams(context, "input/etamsers/", caseName, net, log, loadEtamFromSER, EvaluationPlugin.MAX_MAPPING_TIME);
		EventActivityMappings etams = EventToActivityMapper.obtainEtams(context, "input/etamsers/", net, log, loadEtamFromSER, EvaluationPlugin.MAX_MAPPING_TIME);		

		// Step 2: Perform conformance checks
		
		Default replayParameters = new ReplayerParameters.Default(2, Debug.NONE);
		
		Replayer replayer = new Replayer(replayParameters, net, initMarking, finalMarking, classes, transEventMap, false);
		
		fitnessMap = new HashMap<>(); // store previously seen traces
		//TraceReplayTask... pro Trace
		TraceToBSpaceTranslator translator = new TraceToBSpaceTranslator(etams);
		double fitSum = 0;
		//int i = 0;
		// Loop over traces in event log
		//for(XTrace t : log) {
		for (int i=0; i<log.size();i+=4) {
			
			// The TBS log captures all interpretations (trace translations) of trace t according to the different mappings in etams
			//TraceBSpace tbs = translator.translateToBSpace(t);
			TraceBSpace tbs = translator.translateToBSpace(log.get(i));
			XLog tbsLog = tbs.translationsAsLog(log);
			double traceFitness = 0.0;
			for (XTrace interpretation : tbsLog) {
				//double intFit = computeTraceFitness(replayer, logAttributes, interpretation);
				// for now just calculate traceFitness as the average fitness value per interpretation, no probabilities considered
				//traceFitness = traceFitness + intFit / (tbsLog.size()/4);
			}
			if(i % 50 == 0) {
				System.out.println(i + " Traces done");
			}
			i++;
			fitSum += traceFitness;
		}
		
		UncertainComplianceCheckAlignmentBasedPlugin compPlugin = new UncertainComplianceCheckAlignmentBasedPlugin();
		compPlugin.exec(context, net, log);
		/*for(Set<String> actSets : compPlugin.exec(context, net, log, etams, false).getAmbiguousActSets()) {
			for(String str : actSets) {
				System.out.println("Ambiguous Activity: " + str);
			}
		}*/
		//compPlugin.printAmbiguousNonCompliantComps();
		
		//compPlugin.printUnambiguousNonCompliantComps();
		//compPlugin.printDeviationSets();
		
		DeviationSet ds[] = new DeviationSet[etams.size()];
		for(int i = 0; i< etams.size();i++) {
			ds[i] = compPlugin.getSingleDevSet(5500, i);//TODO: impelement a method getDevSets(traceNo) 
			System.out.println(compPlugin.getSingleDevSet(5500, i).toString());
		}
		compPlugin.constructDeviationMatrix(ds, 5500).showDeviationMatrix();
		DeviationSet.constructConnectivityMetric(ds, 5500);
		//DeviationSet.createDevDistr(ds);
		System.out.println("\nUnique Ambiguous non-compliant Components: "+ compPlugin.getUniqueAmbigComps());
		System.out.println("\n Unique Unambiguous non-compliant Components: " + compPlugin.getUniqueUnambigComps());
		//System.out.println("Average trace fitness: " + fitSum / (log.size()));
		
		return "Plugin completed";
		
	}
		
	
	private double computeTraceFitness(Replayer replayer, XAttributeMap logAttributes, XTrace trace) {
		List<String> traceLabelList = BSpaceUtils.traceToLabelList(trace);
		if (fitnessMap.containsKey(traceLabelList)) {
			return fitnessMap.get(traceLabelList);
		}
		
		XLog log2=new XLogImpl(logAttributes);
		log2.add(trace);
		try {
			
			PNRepResult pnrresult  = replayer.computePNRepResult(Progress.INVISIBLE, log2);
		
			//System.out.println(replayer.getEventClass(trace.get(0)));
			double fitness = (double) pnrresult.getInfo().get(PNRepResult.TRACEFITNESS);
			fitnessMap.put(traceLabelList, fitness);
			

			//print the trace
			/*for(String s: traceLabelList) {
				System.out.print(s + ", ");
			}*/

			
			
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0.0;
	}
	

	private Marking getFinalMarking(PetrinetGraph net) {
		Marking finalMarking = new Marking();

		for (Place p : net.getPlaces()) {
			if (net.getOutEdges(p).isEmpty())
				finalMarking.add(p);
			
		}

		return finalMarking;
	}

	private Petrinet loadPetrinet(PluginContext context, String filepath) {
		Petrinet net = null;
		try {
			net = IOHelper.importPNML(context, filepath);
		} catch (Exception e) {
			System.err.println("FAILED TO LOAD MODEL FROM: " + filepath);
		}
		return net;
	}
	
	private XLog loadLog(String filepath) {
		XLog log = null;
		XFactory  factory = XFactoryRegistry.instance().currentDefault();
		XParser parser = new XesXmlParser(factory);

		File logFile = new File(filepath);
		try {
			log = parser.parse(logFile).get(0);
//			System.out.println("Loaded log with: " + log.size() + " traces.");
		} catch (Exception e) {
			System.err.println("FAILED TO LOAD LOG FROM: " + logFile);
		}
		return log;
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
	
	/*public void writeEdgeTraversed1(ReplayAlgorithm algorithm, int fromMarking, int transition, int toMarking,
			String extra) {
		StringBuilder b = new StringBuilder();
		b.append("i" + algorithm.getIterationNumber());
		b.append("m");
		b.append(fromMarking);
		b.append(" -> ");
		b.append("i" + algorithm.getIterationNumber());
		b.append("m");
		b.append(toMarking);
		b.append(" [");
		if (transition >= 0) {
			b.append("label=<<b>");
			//				b.append("t");
			//				b.append(transition);
			//				b.append("<br/>");
			b.append(algorithm.getNet().getTransitionLabel(transition));
			b.append("<br/>");
			b.append(algorithm.getNet().getCost(transition));
			b.append("</b>>");
			if (algorithm.getNet().getTypeOf(transition) == SyncProduct.SYNC_MOVE) {
				b.append(",fontcolor=forestgreen");
			} else if (algorithm.getNet().getTypeOf(transition) == SyncProduct.MODEL_MOVE) {
				b.append(",fontcolor=darkorchid1");
			} else if (algorithm.getNet().getTypeOf(transition) == SyncProduct.LOG_MOVE) {
				b.append(",fontcolor=goldenrod2");
			} else if (algorithm.getNet().getTypeOf(transition) == SyncProduct.TAU_MOVE) {
				b.append(",fontcolor=honeydew4");
			}
		}
		if (!extra.isEmpty()) {
			b.append(extra);
		}

		b.append("];");
		System.out.println(b);
	}*/
}
