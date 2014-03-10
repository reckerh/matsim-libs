/* *********************************************************************** *
 * project: org.matsim.*
 * PtBoardCountsFixture.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.pt.counts;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.counts.algorithms.CountsComparisonAlgorithm;

public class PtBoardCountsFixture extends PtCountsFixture {

	public PtBoardCountsFixture() {
		super("inputBoardCountsFile");
	}

	@Override
	public CountsComparisonAlgorithm getCCA() {
		Map<Id, int[]> boards = new HashMap<Id, int[]>();
		int[] boardArray = new int[24];
		boardArray[8] = 65;
		boards.put(new IdImpl("stop1"), boardArray);
		this.oa.setBoards(boards);
		CountsComparisonAlgorithm cca = new CountsComparisonAlgorithm(new CountsComparisonAlgorithm.VolumesForId() {
			
			@Override
			public double[] getVolumesForStop(Id stopId) {
				return copyFromIntArray(oa.getBoardVolumesForStop(stopId));
			}
			
		}, counts, network, Double.parseDouble(config.findParam(MODULE_NAME, "countsScaleFactor")));
		cca.setDistanceFilter(Double.valueOf(config.findParam(MODULE_NAME,"distanceFilter")), config.findParam(MODULE_NAME,"distanceFilterCenterNode"));
		return cca;
	}
	

	private static double[] copyFromIntArray(int[] source) {
	    double[] dest = new double[source.length];
	    for(int i=0; i<source.length; i++) {
	        dest[i] = source[i];
	    }
	    return dest;
	}
}
