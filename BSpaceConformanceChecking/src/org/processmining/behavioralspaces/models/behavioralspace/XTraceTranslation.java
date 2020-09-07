package org.processmining.behavioralspaces.models.behavioralspace;

import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XTraceImpl;
import org.processmining.behavioralspaces.alignmentbased.TranslationComplianceResult;
import org.processmining.plugins.log.abstraction.mapping.EventsToActivitiesMapping;

public class XTraceTranslation extends XTraceImpl {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private XTrace original;
	private EventsToActivitiesMapping mapping;
	private boolean isCompliant;
	private TranslationComplianceResult complianceRes;
	private String charseq;
	
	public XTraceTranslation(XTrace original, EventsToActivitiesMapping mapping) {
		super(original.getAttributes());
		this.original = original;
		this.mapping = mapping;
		this.complianceRes = new TranslationComplianceResult();
	}

	public XTrace getOriginal() {
		return original;
	}

	public EventsToActivitiesMapping getMapping() {
		return mapping;
	}

	public void setCharseq(String charseq) {
		this.charseq = charseq;
	}
	
	public String getCharseq() {
		return charseq;
	}
	
	
	public void setCompliance(boolean isCompliant) {
		this.isCompliant = isCompliant;
	}
	
	public void setComponentCompliance(String componentName, boolean isCompliant) {
		complianceRes.put(componentName, isCompliant);
	}
	
	public boolean isCompliant() {
		 
		return isCompliant;
	}
	
	public boolean isComponentCompliant () {
		
		return complianceRes.isFullyCompliant();
	}
	
	public String toString() {
		return charseq;
	}
	

}
