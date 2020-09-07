package org.processmining.behavioralspaces.evaluation;

public enum ComplianceType {

	COMPLIANT, NONCOMPLIANT, POTENTIALLYCOMPLIANT;
	
	public static ComplianceType getType(double score) {
		if (score == 1.0) {
			return COMPLIANT;
		}
		if (score > 0.0) {
			return POTENTIALLYCOMPLIANT;
		}
		return NONCOMPLIANT;
	}
}
