package org.processmining.behavioralspaces.plugins;

import java.io.File;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;

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
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XLogImpl;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.acceptingpetrinet.models.impl.AcceptingPetriNetFactory;
import org.processmining.behavioralspaces.algorithms.TraceToBSpaceTranslator;
import org.processmining.behavioralspaces.matcher.EventActivityMappings;
import org.processmining.behavioralspaces.matcher.EventToActivityMapper;
import org.processmining.behavioralspaces.models.behavioralspace.DeviationMatrix;
import org.processmining.behavioralspaces.models.behavioralspace.DeviationSet;
import org.processmining.behavioralspaces.models.behavioralspace.TraceBSpace;
import org.processmining.behavioralspaces.utils.BSpaceUtils;
import org.processmining.behavioralspaces.utils.IOHelper;
import org.processmining.behavioralspaces.visualization.DotFileBuilder;
import org.processmining.behavioralspaces.plugins.PresenterFrame;
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
import org.processmining.plugins.dc.decomp.DCComponents;
import org.processmining.plugins.dc.decomp.DCDecomposition;
import org.processmining.plugins.dc.plugins.SESEDecompositionPlugin;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import nl.tue.alignment.Progress;
import nl.tue.alignment.Replayer;
import nl.tue.alignment.ReplayerParameters;
import nl.tue.alignment.ReplayerParameters.Default;
import nl.tue.alignment.algorithms.ReplayAlgorithm.Debug;

@Plugin(name = "Rouven's uncertain conformance checking testbed", parameterLabels = {"Dot"}, 
returnLabels = { "Meaningful conformance results" }, returnTypes = {DotFileBuilder.class },  userAccessible = true)
public class RouvensPlaygroundPlugin {

	Map<List<String>, Double> fitnessMap;
	
	private DCComponents components;
	private DCDecomposition decomposition;

	private UncertainComplianceCheckAlignmentBasedPlugin compPlugin;
	private static DeviationMatrix resultsMatrix;
	private static DeviationSet[] allDevSets;

	private static ArrayList<XTrace> traceSet;
	private static ArrayList<Integer> traceNumberList;
	
	@UITopiaVariant(affiliation = "University of Mannheim", author = "Rouven Fricke", email = "rfricke@mail.uni-mannheim.de")
	@PluginVariant(variantLabel = "Testbed for Rouven", requiredParameterLabels = {})
	public DotFileBuilder exec(PluginContext context) throws Exception {
		
		// Step 0: load a Petri net and a corresponding event log, plus initalize stuff
		//specify the name of the log and petri net as the caseName.
		String caseName ="Road_Traffic_Fines_Management_Process"; //"Artificial - Review - Large";//"BPI_Challenge_2012";// "bpi_challenge_2013_incidents";//"Artificial - Claims";//"BPIC15_1";//"Hospital_log";//"Artificial - Repair"; //"Artificial - Loan Process";"Artificial - Claims";//"Artificial - Claims";/"Road_Traffic_Fines_Management_Process";//
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
		this.decomposition = (DCDecomposition) decompResult[0];
		this.components = (DCComponents) decompResult[1];
		
		// Step 1: establish the possible event-to-activity mappings (leave parameters as is for the moment)
		boolean loadEtamFromSER = true;
		//EventActivityMappings etams = EventToActivityMapper.obtainEtams(context, "input/etamsers/", caseName, net, log, loadEtamFromSER, EvaluationPlugin.MAX_MAPPING_TIME);
		EventActivityMappings etams = EventToActivityMapper.obtainEtams(context, "input/etamsers/", net, log, loadEtamFromSER, EvaluationPlugin.MAX_MAPPING_TIME);		

		// Step 2: Perform conformance checks
		
		Default replayParameters = new ReplayerParameters.Default(2, Debug.NONE);
		
		Replayer replayer = new Replayer(replayParameters, net, initMarking, finalMarking, classes, transEventMap, false);
		
		fitnessMap = new HashMap<>(); // store previously seen traces
		//TraceReplayTask... pro Trace
		traceSet = new ArrayList<XTrace>();
		//ArrayList<Integer> traceNumberList = computeNeededTraces(context, caseName, netPath, logPath, net, acceptingNet, initMarking, finalMarking, log);
		ArrayList<Integer> traceNumberList = computeNeededTraces(context, caseName, netPath, logPath, replayer, log, net, logAttributes);
		for(int i : traceNumberList) {
			System.out.println("========= TraceNo: " + i + " ==========");
			
		}
		
		
		compPlugin = new UncertainComplianceCheckAlignmentBasedPlugin();
		compPlugin.exec(context, net, log);
		
		//get a list of all necessary devSet Arrays (Trace level) to construct matrices
		List<DeviationSet[]> devSetList = new ArrayList<DeviationSet[]>();
		List<DeviationMatrix> devMatrixList = new ArrayList<DeviationMatrix>();
		

		for(int i = 0; i<traceNumberList.size();i++) {
		//for (int i=0; i<log.size();i+=500) {
		//while(enoughTraces == false) {

			
			//DeviationSet ds[] = getDeviationSetsOfATrace(i, etams);
			DeviationSet ds[] = getDeviationSetsOfATrace(traceNumberList.get(i), etams);//new DeviationSet[etams.size()];

			devSetList.add(ds);	//and add the deviation set for the specific trace to the list; for each trace a different dev matrix
			DeviationMatrix matrix = compPlugin.constructDeviationMatrix(ds, traceNumberList.get(i));//create a deviation matrix with the dev sets array
			//DeviationMatrix matrix = compPlugin.constructDeviationMatrix(ds, i);
			devMatrixList.add(matrix);//add it to the list of deviation matrices
			
			// The TBS log captures all interpretations (trace translations) of trace t according to the different mappings in etams
			//TraceBSpace tbs = translator.translateToBSpace(t);
			/*TraceBSpace tbs = translator.translateToBSpace(log.get(traceNumberList.get(i)));
			XLog tbsLog = tbs.translationsAsLog(log);
			double traceFitness = 0.0;
			for (XTrace interpretation : tbsLog) {
				double intFit = computeTraceFitness(replayer, logAttributes, interpretation);
				//for now just calculate traceFitness as the average fitness value per interpretation, no probabilities considered
				traceFitness = traceFitness + intFit / (tbsLog.size());
			}
			
			fitSum += traceFitness;
			
			traceCounter++;*/
			
			//i++;
			
		}
		//compPlugin.printAmbiguousNonCompliantComps();
		//compPlugin.printUnambiguousNonCompliantComps();
		//compPlugin.printDeviationSets(); //Unformatted
		
		resultsMatrix = compPlugin.createInitialMatrix();
		
		for(DeviationMatrix dm : devMatrixList) {
			resultsMatrix = resultsMatrix.addMatrix(dm);
		}
		System.out.println("resultsMatrix");
		resultsMatrix.showDeviationMatrix();
		
		//get all the DeviationSets of the whole log 
		//reduce/flatten the list of arrays to a single array.
		int count = 0;
		allDevSets = new DeviationSet[devSetList.size() * etams.size()];
		for(DeviationSet[] ds: devSetList) {
			for(int i=0; i<ds.length; i++) {
				allDevSets[count] = ds[i];
				count++;
			}
		}
		//add boundary values for exclusiveness in the function call?
		resultsMatrix.computeMatrixMeasures(allDevSets);
		DeviationSet.createDevDistr(allDevSets);
		DeviationSet.constructConnectivityMetric(allDevSets);
		DeviationSet.buildHierarchy(allDevSets);
		System.out.println("\nUnique Ambiguous non-compliant Components: "+ compPlugin.getUniqueAmbigComps());
		System.out.println("\n Unique Unambiguous non-compliant Components: " + compPlugin.getUniqueUnambigComps());
//		for(DCComponent dc : compPlugin.getUniqueDCComps()) {
//			System.out.println("Component: " + dc.getName() + " Transitions: " + dc.getTrans());
//		}
		//System.out.println("Average trace fitness: FitSum: " + fitSum / (log.size()/traceSet.size())   + " log size " + traceCounter);
		
		//Das alles in DotFileBuilder machen! RouvensPlaygroundPlugin Instanz erstellen?
		//GraphBuilder gb = new GraphBuilder(resultsMatrix);
		//gb.runGraphViz();
		//gb.filterSettings(allDevSets, resultsMatrix, 10, 0, "topN", resultsMatrix.getMatrixEntries()[0][1],"Single Component to the selected partition", 0.0, 1.0);
		
		PresenterFrame presenter = new PresenterFrame();
		return presenter.visualize(context, new DotFileBuilder());
	}
	
	public static ArrayList<XTrace> getUsedTraces(){
		return traceSet;
	}
	
	private ArrayList<Integer> computeNeededTraces(PluginContext context, String caseName, String netPath, String logPath, Replayer replayer, XLog log, Petrinet net
			,XAttributeMap logAttributes){
		// Step 1: establish the possible event-to-activity mappings (leave parameters as is for the moment)
		boolean loadEtamFromSER = true;
		//EventActivityMappings etams = EventToActivityMapper.obtainEtams(context, "input/etamsers/", caseName, net, log, loadEtamFromSER, EvaluationPlugin.MAX_MAPPING_TIME);
		EventActivityMappings etams = EventToActivityMapper.obtainEtams(context, "input/etamsers/", net, log, loadEtamFromSER, EvaluationPlugin.MAX_MAPPING_TIME);		

		// Step 2: Perform conformance checks
		
		fitnessMap = new HashMap<>(); // store previously seen traces
		//TraceReplayTask... pro Trace
		TraceToBSpaceTranslator translator = new TraceToBSpaceTranslator(etams);
		double fitSum = 0;

		// Loop over traces in event log
		boolean enoughTraces = false;
		ArrayList<Double> fitValuesList = new ArrayList<Double>();
		int traceCounter = 0;
		traceNumberList = new ArrayList<Integer>();
		System.out.println("in neuer methode vor while");
		while(enoughTraces == false) {
			Random rn = new Random();
			int answer = rn.nextInt(log.size()) + 1;
			
			if(!traceSet.contains(log.get(answer))) {
			traceSet.add(log.get(answer));
			traceNumberList.add(answer);
			traceCounter++;
			// The TBS log captures all interpretations (trace translations) of trace t according to the different mappings in etams
			//TraceBSpace tbs = translator.translateToBSpace(t);
			TraceBSpace tbs = translator.translateToBSpace(log.get(answer));
			XLog tbsLog = tbs.translationsAsLog(log);
			double traceFitness = 0.0;
			for (XTrace interpretation : tbsLog) {
				double intFit = computeTraceFitness(replayer, logAttributes, interpretation);
				//for now just calculate traceFitness as the average fitness value per interpretation, no probabilities considered
				traceFitness = traceFitness + intFit / (tbsLog.size());
			}
			fitSum += traceFitness;
			System.out.println("Tracefitness: " + traceFitness + " ~ Avg: " + fitSum / traceCounter + " " + traceCounter);
			fitValuesList.add(fitSum / traceCounter );
			System.out.println(traceCounter);
			if(fitValuesList.size() > 20) {
				for(int count = fitValuesList.size()-11; count < fitValuesList.size()-1; count++) {
					if(Math.abs(fitValuesList.get(count) - fitValuesList.get(count + 1)) <= 0.01) {
						System.out.print(Math.abs(fitValuesList.get(count) - fitValuesList.get(count + 1)) + " ");
						System.out.println("Current Fitness: " + fitValuesList.get(count) + " " + fitValuesList.get(count + 1) + " " +  (Math.abs(fitValuesList.get(count) - fitValuesList.get(count + 1)) <= 0.01));
						enoughTraces = true;
					}
					else {
						System.out.print(Math.abs(fitValuesList.get(count) - fitValuesList.get(count + 1)) + " ");
						System.out.println("Current Fitness: " + fitSum / traceCounter + " " + (Math.abs(fitValuesList.get(count) - fitValuesList.get(count + 1)) <= 0.01));
						enoughTraces = false;
						break;
					}
				}
			}
			
		}
			if(log.size() < 20 && traceSet.size() == log.size()) {
				System.out.println("JAAAAAAAAAAAAAAAAAAAAAAA");
				enoughTraces = true;
			}
		}
		
		// Loop over traces in event log
		System.out.println("in neuer Methode nach while");
		System.out.println("Average trace fitness: " + fitSum / traceCounter + " " + traceCounter);
		return traceNumberList;
	}
	
	public static DeviationMatrix getResultsMatrix() {
		return resultsMatrix;
	}
	public static ArrayList<Integer> getTraceNumbers(){
		return traceNumberList;
	}
	
	public static DeviationSet[] getDevSetArray() {
		return allDevSets;
	}
	
	private double computeTraceFitness(Replayer replayer, XAttributeMap logAttributes, XTrace trace) {
		List<String> traceLabelList = BSpaceUtils.traceToLabelList(trace);
		if (fitnessMap.containsKey(traceLabelList)) {
			return fitnessMap.get(traceLabelList);
		}
		
		XLog log2 = new XLogImpl(logAttributes);
		log2.add(trace);
		try {
			PNRepResult pnrresult  = replayer.computePNRepResult(Progress.INVISIBLE, log2);
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
			//System.out.println(compPlugin.getSingleDevSet(j, j).toString());
		}
		return ds;
	}
	
	
}
