package org.processmining.behavioralspaces.parameters;

public class BenchmarkEvaluationParameters {
	
	public int logSize;
	public int traceLength;
	public int startIndex;
	public int endIndex;
	public int[] noiseLevels;
	public boolean multiThreaded;
	public boolean useSerializedLogs;
	public boolean useSerializedMappings;
	public boolean alsoRunNaive;
	public int maxMappingTime;
	public double mappingMultiplier;
	
	
	public BenchmarkEvaluationParameters(int logSize, int traceLength, int startIndex, int endIndex, int[] noiseLevels,
			boolean multiThreaded, boolean useSerializedLogs, boolean useSerializedMappings, boolean alsoRunNaive, int maxMappingTime, double mappingMultiplier) {
		super();
		this.logSize = logSize;
		this.traceLength = traceLength;
		this.startIndex = startIndex;
		this.endIndex = endIndex;
		this.noiseLevels = noiseLevels;
		this.multiThreaded = multiThreaded;
		this.useSerializedLogs = useSerializedLogs;
		this.useSerializedMappings = useSerializedMappings;
		this.alsoRunNaive = alsoRunNaive;
		this.maxMappingTime = maxMappingTime;
		this.mappingMultiplier = mappingMultiplier;
	}
	
	public BenchmarkEvaluationParameters(int logSize, int traceLength, int startIndex, int endIndex, int[] noiseLevels,
			boolean useSerializedLogs, boolean useSerializedMappings, boolean alsoRunNaive, int maxMappingTime, double mappingMultiplier) {
	super();
	this.logSize = logSize;
	this.traceLength = traceLength;
	this.startIndex = startIndex;
	this.endIndex = endIndex;
	this.noiseLevels = noiseLevels;
	this.useSerializedLogs = useSerializedLogs;
	this.useSerializedMappings = useSerializedMappings;
	this.alsoRunNaive = alsoRunNaive;
	this.maxMappingTime = maxMappingTime;
	this.mappingMultiplier = mappingMultiplier;
}
	
	

}