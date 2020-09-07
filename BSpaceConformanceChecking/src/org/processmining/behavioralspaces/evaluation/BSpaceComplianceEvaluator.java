package org.processmining.behavioralspaces.evaluation;

import org.processmining.behavioralspaces.models.behavioralspace.BSpaceLog;
import org.processmining.behavioralspaces.models.behavioralspace.TraceBSpace;
import org.processmining.behavioralspaces.models.behavioralspace.XTraceTranslation;

public class BSpaceComplianceEvaluator {

	
	public static int[] evaluate(BSpaceLog bsl) {
		int[] res = new int[3];
		
		for (TraceBSpace tbs : bsl) {
			ComplianceType cType = checkComplianceType(tbs);
			if (cType == ComplianceType.COMPLIANT) {
				res[0]++;
			}
			if (cType == ComplianceType.NONCOMPLIANT) {
				res[1]++;
			}
			if (cType == ComplianceType.POTENTIALLYCOMPLIANT) {
				res[2]++;
			}
		}
		return res;
	}
	
	
	private static ComplianceType checkComplianceType(TraceBSpace tbs) {
		int compl = 0;
		int noncompl = 0;
		
		for (XTraceTranslation xtt : tbs.getTranslations()) {
			if (xtt.isCompliant()) {
				compl++;
			} else {
				noncompl++;
			}
		}
		
		double degree = compl * 1.0 / (compl + noncompl);
		return ComplianceType.getType(degree);
	}
}
