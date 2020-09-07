package org.processmining.behavioralspaces.plugins;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashMap;
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
public class EvaluationPlugin {
	
	
		public static  boolean TEST_RUN = false;
		public static List<String> TEST_SET = Arrays.asList();
		public static boolean ALSO_RUN_NAIVE = false;
		
		public static int[] NOISE_LEVELS = new int[]{50};
		public static int MODEL_RANGE_START = 0;
		public static int MODEL_RANGE_END = 10;
		public static int LOG_SIZE = 200;
		public static int TRACE_LENGTH = 50;
		public static int MAX_MAPPING_TIME = 3 * 60 * 1000; 
		public static double MAPPING_MULTIPLIER = 0.03;
		
		public static boolean APPEND_RESULTS_OUTPUT = false;
		
		
		public static final String BASE_FOLDER = "input/";
		public static final String MODEL_FOLDER = BASE_FOLDER + "testmodels/";
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
					NOISE_LEVELS, USE_SERIALIZED_LOGS, USE_SERIALIZED_ETAMS, ALSO_RUN_NAIVE, MAX_MAPPING_TIME, MAPPING_MULTIPLIER);
			
			
			loadModels();
			execSingleThreaded();
		}
		
		@UITopiaVariant(affiliation = "Humboldt University of Berlin", author = "Han van der Aa", email = "han.van.der.aa" + (char) 0x40 + "hu-berlin.de")
		@PluginVariant(variantLabel = "benchmark collection", requiredParameterLabels = {0})
		public void execCLI(CLIPluginContext cliContext, BenchmarkEvaluationParameters parameters) {
			this.context = cliContext;
			this.parameters = parameters;
			loadModels();

			execSingleThreaded();
		}
		
		private void execSingleThreaded() {
			String outfile = RESULTS_FOLDER + "results " + System.currentTimeMillis() +  ".csv";
			writer = new ResultsWriter(outfile, APPEND_RESULTS_OUTPUT);

			int i = 1;
			for (String netName : netMap.keySet()) {
				if (TEST_RUN && !TEST_SET.contains(netName)) {
					continue;
				}
				boolean stop = false;
				int j = 1;
				for (int noise : parameters.noiseLevels) {
					if (!stop) {
						BenchMarkComplianceSingleModelAndLog modelLogRunner = new BenchMarkComplianceSingleModelAndLog(context, netMap.get(netName), noise, parameters);	
						//modelLogRunner.computeResults();
						modelLogRunner.run();//oder public Methode computeResults() aufrufen (auskommentiert)
						writer.addModelResult(modelLogRunner.getResults().toCSVLine());
						ErrorType error = modelLogRunner.getResults().getError();
						if (error == ErrorType.DEADLOCK || error == ErrorType.NOETAMSOLUTION) {
							stop = true;
						}
						System.gc();
						System.out.println("model: " + i + " noise level: " + j + " done.");
						j++;
					}
				}
				i++;

			}	
			writer.close();
		}

	
	
	private Map<String, Petrinet> loadModels() {
		netMap = new LinkedHashMap<String, Petrinet>();
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
