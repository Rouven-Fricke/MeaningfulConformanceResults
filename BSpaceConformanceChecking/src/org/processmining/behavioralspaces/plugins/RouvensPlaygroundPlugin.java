package org.processmining.behavioralspaces.plugins;

import java.io.File;
import java.util.ArrayList;
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
import org.processmining.behavioralspaces.alignmentbased.BenchMarkComplianceSingleModelAndLog;
import org.processmining.behavioralspaces.evaluation.BSpaceComplianceEvaluator;
import org.processmining.behavioralspaces.matcher.EventActivityMappings;
import org.processmining.behavioralspaces.matcher.EventToActivityMapper;
import org.processmining.behavioralspaces.models.behavioralspace.BSpaceLog;
import org.processmining.behavioralspaces.models.behavioralspace.DeviationMatrix;
import org.processmining.behavioralspaces.models.behavioralspace.DeviationSet;
import org.processmining.behavioralspaces.models.behavioralspace.MetricsResult;
import org.processmining.behavioralspaces.models.behavioralspace.TraceBSpace;
import org.processmining.behavioralspaces.models.behavioralspace.XTraceTranslation;
import org.processmining.behavioralspaces.parameters.BenchmarkEvaluationParameters;
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
	private int traceNo = 0;
	private UncertainComplianceCheckAlignmentBasedPlugin compPlugin;
	@UITopiaVariant(affiliation = "University of Mannheim", author = "Rouven Fricke", email = "rfricke@mail.uni-mannheim.de")
	@PluginVariant(variantLabel = "Testbed for Rouven", requiredParameterLabels = {})
	public String exec(PluginContext context) throws Exception {
		
		// Step 0: load a Petri net and a corresponding event log, plus initalize stuff
		String caseName ="Artificial - Review - Large";//"Artificial - Loan Process";////BPIC15_1";//"Artificial - Repair"; //"Road_Traffic_Fines_Management_Process"; ////"Artificial - Claims"; //"Artificial - Claims";//"Hospital_log";//"Artificial - Claims";/"Road_Traffic_Fines_Management_Process";//
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
		
		
		//noise insertion
		/*int[] noiseLevel = {40};
		BenchmarkEvaluationParameters noiseParam = new BenchmarkEvaluationParameters(log.size(), 14, 1,14, noiseLevel , true, true, false, Integer.MAX_VALUE,0.05);
		BenchMarkComplianceSingleModelAndLog noiseInsertionPlugin = new BenchMarkComplianceSingleModelAndLog(context, net, 40, noiseParam);
		noiseInsertionPlugin.run();
		*/
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
		
		
		compPlugin = new UncertainComplianceCheckAlignmentBasedPlugin();
		compPlugin.exec(context, net, log);
		
		//get a list of all necessary devSet Arrays (Trace level) to construct matrices
		List<DeviationSet[]> devSetList = new ArrayList<DeviationSet[]>();
		List<DeviationMatrix> devMatrixList = new ArrayList<DeviationMatrix>();
		
		for (int i=0; i<log.size();i+=500) {
			DeviationSet ds[] = getDeviationSetsOfATrace(i, etams);//new DeviationSet[etams.size()];
			/*for(int j = 0; j<etams.size(); j++) {
				ds[j] = compPlugin.getSingleDevSet(i, j);//create a deviation set for each trace
				System.out.println(compPlugin.getSingleDevSet(j, j).toString());

			}*/
			devSetList.add(ds);	//and add the deviation set for the specific trace to the list
			DeviationMatrix matrix = compPlugin.constructDeviationMatrix(ds, i);//create a deviation matrix with the dev sets array
			devMatrixList.add(matrix);//add it to the list of deviation matrices
			
			// The TBS log captures all interpretations (trace translations) of trace t according to the different mappings in etams
			//TraceBSpace tbs = translator.translateToBSpace(t);
			TraceBSpace tbs = translator.translateToBSpace(log.get(i));
			XLog tbsLog = tbs.translationsAsLog(log);
			double traceFitness = 0.0;
			for (XTrace interpretation : tbsLog) {
//				double intFit = computeTraceFitness(replayer, logAttributes, interpretation);
				//for now just calculate traceFitness as the average fitness value per interpretation, no probabilities considered
//				traceFitness = traceFitness + intFit / (tbsLog.size()/4);
			}
			if(i % 50 == 0) {
				System.out.println(i + " Traces done");
			}
			//i++;
			fitSum += traceFitness;
		}
		
		
		//for(Set<String> actSets : compPlugin.exec(context, net, log, etams, false).getAmbiguousActSets()) {
		//	for(String str : actSets) {
		//		System.out.println("Ambiguous Activity: " + str);
		//	}
		//}
		//compPlugin.printAmbiguousNonCompliantComps();
		
		//compPlugin.printUnambiguousNonCompliantComps();
		//compPlugin.printDeviationSets(); //Unformatted
		
		/*DeviationSet ds1[] = new DeviationSet[etams.size()];
		DeviationSet ds2[] = new DeviationSet[etams.size()];
		for(int i = 0; i< etams.size();i++) {
			ds1[i] = compPlugin.getSingleDevSet(5500, i);//TODO: impelement a method getDevSets(traceNo) 
			System.out.println(compPlugin.getSingleDevSet(5500, i).toString());
			ds2[i] = compPlugin.getSingleDevSet(6000, i);
			//System.out.println(compPlugin.getSingleDevSet(6000, i));
		}
		DeviationMatrix m1 = compPlugin.constructDeviationMatrix(ds1, 5500);
		//DeviationMatrix m2 = compPlugin.constructDeviationMatrix(ds2, 6000);
		m1.showDeviationMatrix();
		System.out.println();
		//m2.showDeviationMatrix();
		//System.out.println();
		//m1.addMatrix(m2).showDeviationMatrix();*/
		DeviationMatrix resultsMatrix = compPlugin.createInitialMatrix();
		for(DeviationMatrix dm : devMatrixList) {
			resultsMatrix = resultsMatrix.addMatrix(dm);
			//dm.showDeviationMatrix();
		}
		//resultsMatrix = devMatrixList.get(1).addMatrix(devMatrixList.get(2));
		resultsMatrix.showDeviationMatrix();
		
		
		
		
		int count = 0;
		DeviationSet[] allDevSets = new DeviationSet[devSetList.size() * etams.size()];
		for(DeviationSet[] ds: devSetList) {
			for(int i=0; i<ds.length; i++) {
				allDevSets[count] = ds[i];
				count++;
			}
		}
	   
		
		DeviationSet.createDevDistr(allDevSets);
		DeviationSet.constructConnectivityMetric(allDevSets);
		System.out.println("\nUnique Ambiguous non-compliant Components: "+ compPlugin.getUniqueAmbigComps());
		System.out.println("\n Unique Unambiguous non-compliant Components: " + compPlugin.getUniqueUnambigComps());
		//compPlugin.createFullMatrix().showDeviationMatrix();
		System.out.println("Average trace fitness: FitSum: " + fitSum  + " log size" + (log.size()));
		
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
			return fitness;

			
			
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
	
	private DeviationSet[] getDeviationSetsOfATrace(int traceNo, EventActivityMappings etams) {
		DeviationSet ds[] = new DeviationSet[etams.size()];
		for(int j = 0; j<etams.size(); j++) {
			ds[j] = compPlugin.getSingleDevSet(traceNo, j);//create a deviation set for each trace
			System.out.println(compPlugin.getSingleDevSet(j, j).toString());
		}
		return ds;
	}
	
	
}
