package org.processmining.behavioralspaces.plugins;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.processmining.behavioralspaces.alignmentbased.BenchMarkComplianceSingleModelAndLog;
import org.processmining.behavioralspaces.evaluation.ErrorType;
import org.processmining.behavioralspaces.evaluation.SingleModelEvaluationResult;
import org.processmining.behavioralspaces.io.ResultsWriter;
import org.processmining.behavioralspaces.parameters.BenchmarkEvaluationParameters;
import org.processmining.behavioralspaces.utils.IOHelper;
import org.processmining.contexts.cli.CLIPluginContext;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;


@Plugin(name = "Benchmarking behavioral space event-activity conformance", parameterLabels = {"Benchmark parameters"}, 
returnLabels = { "Behavioral Space-based Event Log" }, returnTypes = { SingleModelEvaluationResult.class },  userAccessible = true)
public class AlignmentBasedCheckingBenchmarkPlugin {
	
	
		public static  boolean TEST_RUN = false;
		public static List<String> TEST_SET = Arrays.asList();
		
		public static boolean RUN_MULTI_THREADED = false;
		public static boolean ALSO_RUN_NAIVE = false;
		
		public static int[] NOISE_LEVELS = new int[]{0};
		public static int MODEL_RANGE_START = 0;
		public static int MODEL_RANGE_END = 5;
		public static int LOG_SIZE = 100;
		public static int TRACE_LENGTH = 30;
		public static int MAX_MAPPING_TIME = 5 * 60 * 1000; 
		public static double MAPPING_MULTIPLIER = 0.001;
		
		public static boolean APPEND_RESULTS_OUTPUT = false;
		
		
		public static final String BASE_FOLDER = "input/";
		public static final String MODEL_FOLDER = BASE_FOLDER + "modelsthomas/";
		public static final String LOG_FOLDER = BASE_FOLDER + "logs/";
		public static final String ETAM_SER_FOLDER = BASE_FOLDER + "etamsers/";
		public static final String RESULTS_FOLDER = "output/alignmentbasedresults/";

		public static boolean USE_SERIALIZED_LOGS = false;
		public static boolean USE_SERIALIZED_ETAMS = false;
		
		Map<String, Petrinet> netMap;
		PluginContext context;
		ResultsWriter writer;
		BenchmarkEvaluationParameters parameters;
		
		@UITopiaVariant(affiliation = "Humboldt University of Berlin", author = "Han van der Aa", email = "han.van.der.aa" + (char) 0x40 + "hu-berlin.de")
		@PluginVariant(variantLabel = "benchmark collection", requiredParameterLabels = {})
		public void exec(UIPluginContext context) throws Exception {
			this.context = context;

			parameters = new BenchmarkEvaluationParameters(LOG_SIZE, TRACE_LENGTH, MODEL_RANGE_START, MODEL_RANGE_END,
					NOISE_LEVELS, RUN_MULTI_THREADED, USE_SERIALIZED_LOGS, USE_SERIALIZED_ETAMS, ALSO_RUN_NAIVE, MAX_MAPPING_TIME, MAPPING_MULTIPLIER);
			
			
			loadModels();

			if (parameters.multiThreaded) {
				execMultiThreaded();
			} else {
				execSingleThreaded();
			}
		}
		
		@UITopiaVariant(affiliation = "Humboldt University of Berlin", author = "Han van der Aa", email = "han.van.der.aa" + (char) 0x40 + "hu-berlin.de")
		@PluginVariant(variantLabel = "benchmark collection", requiredParameterLabels = {0})
		public void execCLI(CLIPluginContext cliContext, BenchmarkEvaluationParameters parameters) {
			this.context = cliContext;
			this.parameters = parameters;
			loadModels();

			if (parameters.multiThreaded) {
				execMultiThreaded();
			} else {
				execSingleThreaded();
			}
//			return null;
		}
		
		private void execSingleThreaded() {
			String outfile = RESULTS_FOLDER + "results " + System.currentTimeMillis() +  ".csv";
			writer = new ResultsWriter(outfile, APPEND_RESULTS_OUTPUT);

			int i = 0;
			for (String netName : netMap.keySet()) {
				if (TEST_RUN && !TEST_SET.contains(netName)) {
					continue;
				}
				boolean stop = false;
				for (int noise : parameters.noiseLevels) {
					if (!stop) {
						BenchMarkComplianceSingleModelAndLog modelLogRunner = new BenchMarkComplianceSingleModelAndLog(context, netMap.get(netName), noise, parameters);	
						modelLogRunner.run();
						writer.addModelResult(modelLogRunner.getResults().toCSVLine());
						ErrorType error = modelLogRunner.getResults().getError(); 
						if (error == ErrorType.DEADLOCK || error == ErrorType.NOETAMSOLUTION) {
							stop = true;
						}
						System.gc();
					}
				}
				i++;
				System.out.println(i + " models done");
			}	
			writer.close();
		}

		private void execMultiThreaded() {
			String outfile = RESULTS_FOLDER + "results " + System.currentTimeMillis() +  ".csv";
			writer = new ResultsWriter(outfile, APPEND_RESULTS_OUTPUT);
			final Integer[] progress = new Integer[2];
			progress[0] = 0;
			progress[1] = 0;
			netMap.keySet().stream()
			.parallel()
			.forEach(name->{
				progress[0]++;
				runModelForNoiseLevels(netMap.get(name));
				progress[1]++;
				System.out.println(progress[1] + " models done, " + (progress[0] - progress[1]) + " running");
			});
			writer.close();
			System.out.println("Done");
		}
		
	
		private void runModelForNoiseLevels(Petrinet net) {
			boolean stop = false;
			for (int noise : parameters.noiseLevels) {
				if (!stop) {
					BenchMarkComplianceSingleModelAndLog modelLogRunner = new BenchMarkComplianceSingleModelAndLog(context, net, noise, parameters);
					modelLogRunner.run();
					writer.addModelResult(modelLogRunner.getResults().toCSVLine());
					System.out.println("Results written for: " + net.getLabel() + " noise: " + noise);
					ErrorType error = modelLogRunner.getResults().getError(); 
					if (error == ErrorType.DEADLOCK || error == ErrorType.NOETAMSOLUTION) {
						stop = true;
					}
//					System.gc();
				}
			}
		}

	
	
	private Map<String, Petrinet> loadModels() {
		netMap = new HashMap<String, Petrinet>();
		int i = 0;
		for (File pnmlFile : IOHelper.getFilesWithExtension(new File(MODEL_FOLDER), ".pnml") ) {
			Petrinet net = null;
			try {
				net = IOHelper.importPNML(context, pnmlFile.getAbsolutePath());
			} catch (Exception e) {
				System.err.println("FAILED TO LOAD MODEL FROM: " + pnmlFile.getAbsolutePath());
			}
			if (net != null) {
				if ((parameters.startIndex <= i) && (i <= parameters.endIndex)) {
					netMap.put(IOHelper.getFileName(pnmlFile), net);
				}
			}
			i++;
		}
		System.out.println("Loaded " + netMap.size() + " models from " + MODEL_FOLDER);
		return netMap;
	}
	
	
//	
	
	
}