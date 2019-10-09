/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
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

package org.matsim.contrib.freight.carrier;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.freight.FreightConfigGroup;
import org.matsim.contrib.freight.controler.CarrierModule;
import org.matsim.contrib.freight.mobsim.DistanceScoringFunctionFactoryForTests;
import org.matsim.contrib.freight.mobsim.StrategyManagerFactoryForTests;
import org.matsim.contrib.freight.replanning.CarrierPlanStrategyManagerFactory;
import org.matsim.contrib.freight.scoring.CarrierScoringFunctionFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

public class CarrierModuleTest {

    Controler controler;

    FreightConfigGroup freightConfigGroup;

    @Rule
    public MatsimTestUtils testUtils = new MatsimTestUtils();

    @Before
    public void setUp(){
        Config config = ConfigUtils.createConfig() ;
        PlanCalcScoreConfigGroup.ActivityParams workParams = new PlanCalcScoreConfigGroup.ActivityParams("w");
        workParams.setTypicalDuration(60 * 60 * 8);
        config.planCalcScore().addActivityParams(workParams);
        PlanCalcScoreConfigGroup.ActivityParams homeParams = new PlanCalcScoreConfigGroup.ActivityParams("h");
        homeParams.setTypicalDuration(16 * 60 * 60);
        config.planCalcScore().addActivityParams(homeParams);
        config.global().setCoordinateSystem("EPSG:32632");
        config.controler().setFirstIteration(0);
        config.controler().setLastIteration(0);
        config.controler().setOutputDirectory(testUtils.getOutputDirectory());
        config.network().setInputFile( testUtils.getClassInputDirectory() + "network.xml" );
        config.plans().setInputFile( testUtils.getClassInputDirectory() + "plans100.xml" );

        freightConfigGroup = new FreightConfigGroup();
        freightConfigGroup.setCarriersFile( testUtils.getClassInputDirectory() + "carrierPlansEquils.xml");
        freightConfigGroup.setCarriersVehicleTypesFile( testUtils.getClassInputDirectory() + "vehicleTypes.xml");

        Scenario scenario = ScenarioUtils.loadScenario( config );
        controler = new Controler(scenario);
        controler.getConfig().controler().setWriteEventsInterval(1);
        controler.getConfig().controler().setCreateGraphs(false);
    }

    @Test
    public void test_ConstructorWOParameters(){
        controler.addOverridingModule(new CarrierModule());
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bind(CarrierPlanStrategyManagerFactory.class).to(StrategyManagerFactoryForTests.class).asEagerSingleton();
                bind(CarrierScoringFunctionFactory.class).to(DistanceScoringFunctionFactoryForTests.class).asEagerSingleton();
            }
        });
        controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
        controler.run();
    }

    @Test
    public void test_ConstructorWithOneParameter(){
        Carriers carriers = new Carriers();
        new CarrierPlanXmlReader( carriers ).readFile( testUtils.getClassInputDirectory() + "carrierPlansEquils.xml" );
        CarrierVehicleTypes carrierVehicleTypes = new CarrierVehicleTypes();
        new CarrierVehicleTypeReader(carrierVehicleTypes).readFile( testUtils.getClassInputDirectory() + "vehicleTypes.xml");
        new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(carrierVehicleTypes);
        controler.addOverridingModule(new CarrierModule(carriers));
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bind(CarrierPlanStrategyManagerFactory.class).to(StrategyManagerFactoryForTests.class).asEagerSingleton();
                bind(CarrierScoringFunctionFactory.class).to(DistanceScoringFunctionFactoryForTests.class).asEagerSingleton();
            }
        });
        controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
        controler.run();
    }

    @Test
    public void test_ConstructorWithThreeParameters(){
        Carriers carriers = new Carriers();
        new CarrierPlanXmlReader( carriers ).readFile( testUtils.getClassInputDirectory() + "carrierPlansEquils.xml" );
        CarrierVehicleTypes carrierVehicleTypes = new CarrierVehicleTypes();
        new CarrierVehicleTypeReader(carrierVehicleTypes).readFile( testUtils.getClassInputDirectory() + "vehicleTypes.xml");
        new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(carrierVehicleTypes);
        controler.addOverridingModule(new CarrierModule(carriers, new StrategyManagerFactoryForTests(), new DistanceScoringFunctionFactoryForTests()));
        controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
        controler.run();
    }

}
