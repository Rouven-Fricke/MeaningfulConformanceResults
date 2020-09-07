package org.processmining.behavioralspaces.alignmentbased;

import java.util.HashMap;

public class TranslationComplianceResult extends HashMap<String, Boolean>{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	
	public boolean isFullyCompliant() {
		for (Boolean b : this.values()) {
			if (!b) {
				return false;
			}
		} 
		return true;
	}
	
}
