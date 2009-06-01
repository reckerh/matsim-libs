/* *********************************************************************** *
 * project: org.matsim.*																															*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.api.basic.v01;

import org.matsim.api.basic.v01.network.BasicNetwork;
import org.matsim.api.basic.v01.population.BasicPopulation;
import org.matsim.core.config.Config;
/**
 * @author dgrether
 */
public interface BasicScenario {

	public BasicNetwork getNetwork() ;

	public BasicPopulation getPopulation() ;

	public Config getConfig();

	public Id createId(String string);

	public Coord createCoord(double x, double y);

}