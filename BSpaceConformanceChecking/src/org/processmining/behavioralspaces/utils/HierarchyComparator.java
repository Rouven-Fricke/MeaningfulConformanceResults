package org.processmining.behavioralspaces.utils;

import java.util.Comparator;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.tuple.Triple;
import org.processmining.behavioralspaces.models.behavioralspace.MetricsResult;
 
public class HierarchyComparator implements Comparator<MetricsResult> {
	
	@Override
	public int compare(MetricsResult r1, MetricsResult r2) {
		return new CompareToBuilder()
				.append(r1.getDevDistr(), r2.getDevDistr())
				.append(r1.getConnectivity(), r2.getConnectivity()).toComparison();
	}
}
