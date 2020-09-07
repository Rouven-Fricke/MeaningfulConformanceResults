package org.processmining.behavioralspaces.models.behavioralspace;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.behavioralspaces.evaluation.ComplianceType;
import org.processmining.plugins.dc.decomp.DCComponents;
import org.processmining.plugins.dc.decomp.DCDecomposition;
import org.processmining.plugins.log.XLogHelper;
import org.processmining.plugins.log.abstraction.mapping.EventsToActivitiesMapping;


//The TBS log captures all interpretations (trace translations) of trace t according to the different mappings in etams
public class TraceBSpace implements Serializable {

	private static final long serialVersionUID = 1L;
	//	private String id;
	private XTrace trace;
	private Map<EventsToActivitiesMapping, XTraceTranslation> translationsMap;

	private int compliantTTs = 0;
	private int noncompliantTTs = 0;
	
	//neu
	private Set<Set<String>> ambiguousActSets;
	private DCDecomposition decomposition;
	private Set<String> unambigComponents;
	private DCComponents components;
	
	
	public TraceBSpace(XTrace trace) {
		super();
		this.trace = trace;
		translationsMap = new HashMap<EventsToActivitiesMapping, XTraceTranslation>();
	}

//	public String getId() {
//		return id;
//	}

	public XTrace getTrace() {
		return trace;
	}
	
	

	public int size() {
		return translationsMap.size();
	}
	
	
	public Map<EventsToActivitiesMapping, XTraceTranslation> getTranslationsMap() {
		return translationsMap;
	}

	public Collection<XTraceTranslation> getTranslations() {
		return translationsMap.values();
	}

	public XTraceTranslation getTranslation(EventsToActivitiesMapping mapping) {
		return translationsMap.get(mapping);
	}

	public void addTraceTranslation(XTraceTranslation translation) {
		translationsMap.put(translation.getMapping(), translation);
	}
	

	public double complianceDegree() {
		//return compliantTTs * 1.0 / (compliantTTs + noncompliantTTs);
		int compliant = 0;
		for (XTraceTranslation t : translationsMap.values()) {
			if (t.isCompliant()) {
				compliant++;
			}
		}
		return compliant * 1.0 / size();
	}
	
	public ComplianceType complianceType() {
		return ComplianceType.getType(complianceDegree());
	}
	
	public XLog translationsAsLog(XLog originalLog) {
		XLog tempLog = XLogHelper.initializeLog(originalLog);
		tempLog.addAll(getTranslations());
		return tempLog;
	}
	
	//neu
	public void setDCComponents(DCComponents components) {
		this.components = components;
	}
	
	public DCComponents getComponents() {
		return components;
	}
	
	public DCDecomposition getDecomposition() {
		return decomposition;
	}
	
	
}
