/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.anhorni.surprice;

import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioImpl;

import playground.anhorni.surprice.scoring.LaggedScoringFunctionFactory;

public class DayControler extends Controler {
	
	private AgentMemories memories = new AgentMemories();
	
	public DayControler(final ScenarioImpl scenario) {
		super(scenario);	
		super.setOverwriteFiles(true);
	} 
	
	public DayControler(final Config config, AgentMemories memories) {
		super(config);	
		super.setOverwriteFiles(true);
		this.memories = memories;
	} 
	
	public DayControler(final String configFile) {
		super(configFile);	
		super.setOverwriteFiles(true);
	}
	
	protected void setUp() {
	    super.setUp();	           
	  	LaggedScoringFunctionFactory scoringFunctionFactory = new LaggedScoringFunctionFactory(this, this.config.planCalcScore(), this.network, this.memories);	  		
	  	this.setScoringFunctionFactory(scoringFunctionFactory);
	}
}
