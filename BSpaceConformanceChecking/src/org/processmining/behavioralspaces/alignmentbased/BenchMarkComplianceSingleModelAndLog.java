package org.processmining.behavioralspaces.alignmentbased;

import java.io.File;
import java.io.IOException;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.in.XParser;
import org.deckfour.xes.in.XesXmlParser;
import org.deckfour.xes.model.XLog;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.acceptingpetrinet.models.impl.AcceptingPetriNetFactory;
import org.processmining.behavioralspaces.evaluation.BSpaceComplianceEvaluator;
import org.processmining.behavioralspaces.evaluation.ErrorType;
import org.processmining.behavioralspaces.evaluation.SingleModelEvaluationResult;
import org.processmining.behavioralspaces.matcher.EventActivityMappings;
import org.processmining.behavioralspaces.matcher.PositionbasedMapper;
import org.processmining.behavioralspaces.models.behavioralspace.BSpaceLog;
import org.processmining.behavioralspaces.parameters.BenchmarkEvaluationParameters;
import org.processmining.behavioralspaces.plugins.AlignmentBasedCheckingBenchmarkPlugin;
import org.processmining.behavioralspaces.plugins.UncertainComplianceCheckAlignmentBasedPlugin;
import org.processmining.behavioralspaces.plugins.UncertainConformanceCheckingPlugin;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.TimeUnit;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.models.semantics.petrinet.StochasticNetSemantics;
import org.processmining.models.semantics.petrinet.impl.EfficientStochasticNetSemanticsImpl;
import org.processmining.plugins.evaluation.NoiseUtils;
import org.processmining.plugins.log.exporting.ExportLogXes;
import org.processmining.plugins.stochasticpetrinet.simulator.PNSimulator;
import org.processmining.plugins.stochasticpetrinet.simulator.PNSimulatorConfig;

public class BenchMarkComplianceSingleModelAndLog {

	private PluginContext context;
	private Petrinet net;
	private BenchmarkEvaluationParameters parameters;
	private int noise;
	private SingleModelEvaluationResult results;
	
	public BenchMarkComplianceSingleModelAndLog(PluginContext context, Petrinet net, int noise, BenchmarkEvaluationParameters parameters) {
		this.context = context;
		this.net = net;
		this.noise = noise;
		this.parameters = parameters;
		results = new SingleModelEvaluationResult(net.getLabel(), net, noise);
	}
	
	
	public void run() {
		computeResults();
	}
	
	public SingleModelEvaluationResult getResults() {
		return results;
	}
	
	
	private SingleModelEvaluationResult computeResults() {
		System.out.println("Starting run for: " + net.getLabel() + " noise level:" + noise);

		if (hasDeadlock(context, net)) {
			results.setError(ErrorType.DEADLOCK);
			System.out.println("Model has a deadlock. Skipping");
			return results;
		}
		
		
		XLog original = obtainLog(context, net);
		XLog log = original;

		results.setOriginalLog(original);


		
		
		if (log == null) {
			results.setError(ErrorType.NO_LOG);
			return results;
		}
		
		if (results.hasLoops()) {
			System.out.println("Loop detected");
//			results.setError(ErrorType.LOOPS);
//			return results;
		}
		

		if (noise > 0) {
			try {
				log = NoiseUtils.introduceNoise(log, noise, null);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				results.setError(ErrorType.NOISE_INSTERTION_PROBLEM);
				return results;
				
			}
			XConceptExtension.instance().assignName(log, "Simulated_" + net.getLabel() + "_" + log.size() + "Traces_noiseLevel-" + noise);
		}

		PositionbasedMapper mapper = new PositionbasedMapper(original, log);
		EventActivityMappings etams = mapper.obtainMappings(noise * parameters.mappingMultiplier);
		System.out.println("ETAMS size: " + etams.size());
		
		
//		EventActivityMappings etams = EventToActivityMapper.obtainEtams(context, AlignmentBasedCheckingBenchmarkPlugin.ETAM_SER_FOLDER, net, log, parameters.useSerializedMappings, parameters.maxMappingTime);
		if (etams.isEmpty()) {
			System.out.println("No solutions found for " + net.getLabel() + " skipping model" );
			results.setError(ErrorType.NOETAMSOLUTION);
			return results;
		}
		if (etams.size() == 1) {
			System.out.println("No ambiguity found in " + net.getLabel() + " skipping model" );
			results.setError(ErrorType.SINGLESOLUTION);
			return results;
		}

		System.out.println("Number of interpretations generated: " + etams.size());
		results.setEtam(etams);

		System.out.println("Starting evaluation for bspacelog.");

		UncertainComplianceCheckAlignmentBasedPlugin plugin = new UncertainComplianceCheckAlignmentBasedPlugin();
		BSpaceLog bspacelog;
		try {
			bspacelog = plugin.exec(context, net, log, etams, parameters.alsoRunNaive);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			results.setError(ErrorType.ALIGNMENT_PROBLEM);
			return results;
		}
		
		results.setBSpaceLog(bspacelog);

		results.setAmbigClasses(bspacelog.getAmbiguousActSets().size());
		results.setSeses(bspacelog.getDecomposition().getConformableNodesNames().size());
		results.setUnambigSeses(bspacelog.getUnambigComponentsNames().size());

		results.setEffTime(bspacelog.getEffTime());
		results.setNaiveTime(bspacelog.getNaiveTime());

		int[] evalResult = BSpaceComplianceEvaluator.evaluate(bspacelog);

		results.setComplianceResults(evalResult);
		return results;
	}
	
	
	/*  public SingleModelEvaluationResult computeResults() {
		System.out.println("Starting run for: " + net.getLabel() + " noise level:" + noise);

		if (hasDeadlock(context, net) && results.NFCSize() == 0) {
			results.setError(ErrorType.DEADLOCK);
			System.out.println("Model has a deadlock. Skipping");
			return results;
		}

		XLog original = obtainLog(context, net);
		XLog log = original;

		results.setOriginalLog(original);
		
		if (log == null) {
			results.setError(ErrorType.NO_LOG);
			return results;
		}
		
		if (noise > 0) {
			try {
				log = NoiseUtils.introduceNoise(log, noise, null);
			} catch (Exception e) {
				e.printStackTrace();
				results.setError(ErrorType.NOISE_INSTERTION_PROBLEM);
				return results;
				
			}
			XConceptExtension.instance().assignName(log, "Simulated_" + net.getLabel() + "_" + log.size() + "Traces_noiseLevel-" + noise);
		}

		PositionbasedMapper mapper = new PositionbasedMapper(original, log);
//		EventActivityMappings etams = mapper.obtainMappings();
		EventActivityMappings etams = mapper.obtainMappings(noise * parameters.mappingMultiplier);
		System.out.println("ETAMS size: " + etams.size());
//		
		
//		EventActivityMappings etams = EventToActivityMapper.obtainEtams(context, AlignmentBasedCheckingBenchmarkPlugin.ETAM_SER_FOLDER, net, log, parameters.useSerializedMappings, parameters.maxMappingTime);
		if (etams.isEmpty()) {
			System.out.println("No solutions found for " + net.getLabel() + " skipping model" );
			results.setError(ErrorType.NOETAMSOLUTION);
			return results;
		}
		if (etams.size() == 1) {
			System.out.println("No ambiguity found in " + net.getLabel() + " skipping model" );
			results.setError(ErrorType.SINGLESOLUTION);
			return results;
		}

//		System.out.println("Number of interpretations generated: " + etams.size());
		results.setEtam(etams);

//		System.out.println("Starting evaluation for bspacelog.");

		UncertainConformanceCheckingPlugin plugin = new UncertainConformanceCheckingPlugin();
		BSpaceLog bspacelog;
		try {
			bspacelog = plugin.exec(context, net, log, etams, parameters.alsoRunNaive);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			results.setError(ErrorType.ALIGNMENT_PROBLEM);
			return results;
		}
		
		results.setBSpaceLog(bspacelog);

//		results.setAmbigClasses(bspacelog.getAmbiguousActSets().size());
//		results.setSeses(bspacelog.getDecomposition().getConformableNodesNames().size());
//		results.setUnambigSeses(bspacelog.getUnambigComponentsNames().size());

//		results.setEffTime(bspacelog.getEffTime());
//		results.setNaiveTime(bspacelog.getNaiveTime());

		int[] evalResult = BSpaceComplianceEvaluator.evaluate(bspacelog);

		results.setComplianceResults(evalResult);
		
		System.out.println("Compl traces: " + evalResult[0] + " noncompl: " + evalResult[1] + " potential: " + evalResult[2]);
		return results;
	}
	 */
	
	
	private XLog obtainLog(PluginContext context, Petrinet net) {
		XLog log = null;
		if (parameters.useSerializedLogs) {
			XFactory  factory = XFactoryRegistry.instance().currentDefault();
			XParser parser = new XesXmlParser(factory);

			File logFile = new File(AlignmentBasedCheckingBenchmarkPlugin.LOG_FOLDER + net.getLabel() + ".xes");
			try {
				log = parser.parse(logFile).get(0);
				System.out.println("Loaded log with: " + log.size() + " traces.");
			} catch (Exception e) {
				System.err.println("FAILED TO LOAD LOG FROM: " + logFile);
			}
		}
		if (log == null) {
			System.out.println("GENERATING LOG FOR: " + net.getLabel() );
			log = generateLogForNet(context, net);
			if (log != null) {
				String outpath = AlignmentBasedCheckingBenchmarkPlugin.LOG_FOLDER + net.getLabel() + ".xes";
				try {
					ExportLogXes.export(log, new File(outpath));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} 
		}
		System.out.println("Completed Log generation for: " + net.getLabel() );
		return log;
	}
	
	private XLog generateLogForNet(PluginContext context, Petrinet net) {
		AcceptingPetriNet acceptingNet = AcceptingPetriNetFactory.createAcceptingPetriNet(net);
		Marking initialMarking = acceptingNet.getInitialMarking();
		PNSimulator simulator = new PNSimulator();
		PNSimulatorConfig simConfig = new PNSimulatorConfig(parameters.logSize, TimeUnit.MINUTES, 1, 1, parameters.traceLength);
		simConfig.setDeterministicBoundedStateSpaceExploration(false);
		
		StochasticNetSemantics semantics = new EfficientStochasticNetSemanticsImpl();
		semantics.initialize(net.getTransitions(), initialMarking);
		XLog log = null;
		try {
			log = simulator.simulate(null, net, semantics, simConfig, initialMarking);
//			System.out.println("number of different traces in originally generated log: " +log.size());
			
		} catch (Exception e) {
			System.out.println(e);
			System.out.println("FAILED TO GENERATE LOG FOR" + net.getLabel());
		} catch (OutOfMemoryError er) {
			System.out.println(er);
			System.out.println("FAILED TO GENERATE LOG FOR" + net.getLabel());
		}
		
		return log;
	}
	
	
	private boolean hasDeadlock(PluginContext context, Petrinet net) {
//		System.out.println("Checking for deadlocks.");
		AcceptingPetriNet acceptingNet = AcceptingPetriNetFactory.createAcceptingPetriNet(net);
		Marking initialMarking = acceptingNet.getInitialMarking();
		PNSimulator simulator = new PNSimulator();
		PNSimulatorConfig simConfig = new PNSimulatorConfig(1000, TimeUnit.MINUTES, 1, 1, 10);
		simConfig.setDeterministicBoundedStateSpaceExploration(true);
		
		StochasticNetSemantics semantics = new EfficientStochasticNetSemanticsImpl();
		
//		Marking finalMarking = StochasticNetUtils.getFinalMarking(context, net);
		
		semantics.initialize(net.getTransitions(), initialMarking);
		XLog log = null;
		try {
			log = simulator.simulate(null, net, semantics, simConfig, initialMarking);
//			System.out.println("number of different traces in originally generated log: " +log.size());
			
		} catch (Exception e) {
//			System.out.println(e);
//			System.out.println("FAILED TO GENERATE LOG FOR" + net.getLabel());
			return true;
		} catch (OutOfMemoryError er) {
			System.out.println("FAILED TO GENERATE LOG FOR" + net.getLabel());
		}
		
		return false;
	}

	
}