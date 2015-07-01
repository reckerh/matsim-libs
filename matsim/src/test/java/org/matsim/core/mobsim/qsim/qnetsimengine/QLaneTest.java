/* *********************************************************************** *
 * project: org.matsim.*
 * QueueLaneTest
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.lanes.data.v20.*;
import org.matsim.testcases.MatsimTestCase;

/**
 * Test for QueueLane capacity calculations
 *
 * @author dgrether
 */
public class QLaneTest extends MatsimTestCase {

  private Network initNetwork(Network network) {
		((NetworkImpl) network).setCapacityPeriod(3600.0);
		Node node1 = network.getFactory().createNode(Id.create(1, Node.class), new CoordImpl(0, 0));
		Node node2 = network.getFactory().createNode(Id.create(2, Node.class), new CoordImpl(1, 0));
		Node node3 = network.getFactory().createNode(Id.create(3, Node.class), new CoordImpl(2, 0));
		network.addNode(node1);
		network.addNode(node2);
		network.addNode(node3);
		Link l1 = network.getFactory().createLink(Id.create(1, Link.class), node1, node2);
		l1.setLength(1005.0);
		l1.setFreespeed(15.0);
		l1.setCapacity(1800.0);
		l1.setNumberOfLanes(2.0);
		network.addLink(l1);
		Link l2 = network.getFactory().createLink(Id.create(2, Link.class), node2, node3);
		network.addLink(l2);
		return network;
  }
  
	private LaneDefinitions20 createOneLane(ScenarioImpl scenario, int numberOfRepresentedLanes) {
		scenario.getConfig().scenario().setUseLanes(true);
		LaneDefinitions20 lanes = new LaneDefinitions20Impl();
		scenario.addScenarioElement( LaneDefinitions20.ELEMENT_NAME, lanes );
		LaneDefinitionsFactory20 builder = lanes.getFactory();
		//lanes for link 1
		LanesToLinkAssignment20 lanesForLink1 = builder.createLanesToLinkAssignment(Id.create(1, Link.class));
		Lane link1FirstLane = builder.createLane(Id.create("1.ol", Lane.class));
		link1FirstLane.addToLaneId(Id.create(1, Lane.class));
		link1FirstLane.setNumberOfRepresentedLanes(2.0);
		link1FirstLane.setStartsAtMeterFromLinkEnd(1005.0);
		link1FirstLane.setCapacityVehiclesPerHour(1800.0);
		lanesForLink1.addLane(link1FirstLane);
		
		Lane link1lane1 = builder.createLane(Id.create(1, Lane.class));
		link1lane1.addToLinkId(Id.create(2, Link.class));
		link1lane1.setStartsAtMeterFromLinkEnd(105.0);
		link1lane1.setNumberOfRepresentedLanes(numberOfRepresentedLanes);
		link1lane1.setCapacityVehiclesPerHour(numberOfRepresentedLanes * 900.0);
		lanesForLink1.addLane(link1lane1);
		lanes.addLanesToLinkAssignment(lanesForLink1);
		return lanes;
	}
  
	private LaneDefinitions20 createLanes(ScenarioImpl scenario) {
		scenario.getConfig().scenario().setUseLanes(true);
		LaneDefinitions20 lanes = new LaneDefinitions20Impl();
		scenario.addScenarioElement( LaneDefinitions20.ELEMENT_NAME, lanes );
		LaneDefinitionsFactory20 builder = lanes.getFactory();
		//lanes for link 1
		LanesToLinkAssignment20 lanesForLink1 = builder.createLanesToLinkAssignment(Id.create(1, Link.class));
		
		Lane link1FirstLane = builder.createLane(Id.create("1.ol", Lane.class));
		link1FirstLane.addToLaneId(Id.create(1, Lane.class));
		link1FirstLane.addToLaneId(Id.create(2, Lane.class));
		link1FirstLane.addToLaneId(Id.create(3, Lane.class));
		link1FirstLane.setNumberOfRepresentedLanes(2.0);
		link1FirstLane.setStartsAtMeterFromLinkEnd(1005.0);
		link1FirstLane.setCapacityVehiclesPerHour(1800.0);
		lanesForLink1.addLane(link1FirstLane);
		
		Lane link1lane1 = builder.createLane(Id.create(1, Lane.class));
		link1lane1.addToLinkId(Id.create(2, Link.class));
		link1lane1.setStartsAtMeterFromLinkEnd(105.0);
		link1lane1.setCapacityVehiclesPerHour(900.0);
		lanesForLink1.addLane(link1lane1);
		
		Lane link1lane2 = builder.createLane(Id.create(2, Lane.class));
		link1lane2.addToLinkId(Id.create(2, Link.class));
		link1lane2.setNumberOfRepresentedLanes(2);
		link1lane2.setStartsAtMeterFromLinkEnd(105.0);
		link1lane2.setCapacityVehiclesPerHour(1800.0);
		lanesForLink1.addLane(link1lane2);

		Lane link1lane3 = builder.createLane(Id.create(3, Lane.class));
		link1lane3.addToLinkId(Id.create(2, Link.class));
		link1lane3.setCapacityVehiclesPerHour(900.0);
		link1lane3.setStartsAtMeterFromLinkEnd(105.0);
		lanesForLink1.addLane(link1lane3);
		
		lanes.addLanesToLinkAssignment(lanesForLink1);
		return lanes;
	}
  	
  
	public void testCapacityWoLanes() {
		Config config = ConfigUtils.createConfig();
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
		
		this.initNetwork(scenario.getNetwork());

		QSim queueSim = (QSim) QSimUtils.createDefaultQSim(scenario, null);
		NetsimNetwork queueNetwork = queueSim.getNetsimNetwork();
		QLinkImpl ql = (QLinkImpl) queueNetwork.getNetsimLink(Id.create(1, Link.class));

		assertEquals(0.5, ql.getSimulatedFlowCapacity());
		assertEquals(268.0, ql.getSpaceCap());
	}
	
	public void testCapacityWithOneLaneOneLane() {
		Config config = ConfigUtils.createConfig();
		config.scenario().setUseLanes(true);
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
		this.initNetwork(scenario.getNetwork());
		this.createOneLane(scenario, 1);

		QSim queueSim = (QSim) QSimUtils.createDefaultQSim(scenario, null);
		NetsimNetwork queueNetwork = queueSim.getNetsimNetwork();
		QLinkLanesImpl ql = (QLinkLanesImpl) queueNetwork.getNetsimLink(Id.create(1, Link.class));

		assertEquals(0.5, ql.getSimulatedFlowCapacity());
		//900 m link, 2 lanes = 240 storage + 105 m lane, 1 lane = 14 storage
		assertEquals(254.0, ql.getSpaceCap());
		//check original lane
		QLaneI qlane = ql.getOriginalLane();
		assertNotNull(qlane);
//		assertTrue(qlane.isFirstLaneOnLink());
		assertEquals(0.5, qlane.getSimulatedFlowCapacity());
		assertEquals(240.0, qlane.getStorageCapacity());
		
		// check lane
		assertNotNull(ql.getToNodeQueueLanes());
		assertEquals(1, ql.getToNodeQueueLanes().size());
		qlane = ql.getToNodeQueueLanes().get(0);
		
		// link_no_of_lanes = 2 flow = 0.5 -> lane_flow = 0.5/2 * 1 = 0.25
		assertEquals(0.25, qlane.getSimulatedFlowCapacity());
		assertEquals(14.0, qlane.getStorageCapacity());
	}

	public void testCapacityWithOneLaneOneLaneTwoLanes() {
		Config config = ConfigUtils.createConfig();
		config.scenario().setUseLanes(true);
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);

		scenario.getConfig().scenario().setUseLanes(true);
		this.initNetwork(scenario.getNetwork());
		this.createOneLane(scenario, 2);

		QSim queueSim = (QSim) QSimUtils.createDefaultQSim(scenario, null);
		NetsimNetwork queueNetwork = queueSim.getNetsimNetwork();
		QLinkLanesImpl ql = (QLinkLanesImpl) queueNetwork.getNetsimLink(Id.create(1, Link.class));

		assertEquals(0.5, ql.getSimulatedFlowCapacity());
		//900 m link, 2 lanes = 240 storage + 105 m lane, 2 lanes = 28 storage
		assertEquals(268.0, ql.getSpaceCap());
		//check original lane
		QLaneI qlane = ql.getOriginalLane();
		assertNotNull(qlane);
//		assertTrue(qlane.isFirstLaneOnLink());
		assertEquals(0.5, qlane.getSimulatedFlowCapacity());
		assertEquals(240.0, qlane.getStorageCapacity());
		
		// check lane
		assertNotNull(ql.getToNodeQueueLanes());
		assertEquals(1, ql.getToNodeQueueLanes().size());
		qlane = ql.getToNodeQueueLanes().get(0);
		
		// link_no_of_lanes = 2 flow = 0.5 -> lane_flow = 0.5/2 * 2 = 0.5
		assertEquals(0.5, qlane.getSimulatedFlowCapacity());
		assertEquals(28.0, qlane.getStorageCapacity());
	}

	
	
	public void testCapacityWithLanes() {
		Config config = ConfigUtils.createConfig();
		config.scenario().setUseLanes(true);
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
		this.initNetwork(scenario.getNetwork());
		this.createLanes(scenario);

		QSim queueSim = (QSim) QSimUtils.createDefaultQSim(scenario, null);
		NetsimNetwork queueNetwork = queueSim.getNetsimNetwork();
		QLinkLanesImpl ql = (QLinkLanesImpl) queueNetwork.getNetsimLink(Id.create(1, Link.class));

		assertEquals(0.5, ql.getSimulatedFlowCapacity());
		//240 link + 2 * 14 + 1 * 28 = 
		assertEquals(296.0, ql.getSpaceCap());
		double totalStorageCapacity = 0.0;
		//check original lane
		QLaneI qlane = ql.getOriginalLane();
		assertNotNull(qlane);
//		assertTrue(qlane.isFirstLaneOnLink());
		assertEquals(0.5, qlane.getSimulatedFlowCapacity());
		assertEquals(240.0, qlane.getStorageCapacity());
		totalStorageCapacity += qlane.getStorageCapacity();
		// check lanes
		assertNotNull(ql.getToNodeQueueLanes());
		assertEquals(3, ql.getToNodeQueueLanes().size());
		double totalFlowCapacity = 0.0;
		for (QLaneI qll : ql.getToNodeQueueLanes()) {
			if (((QueueWithBuffer)qll).getId().equals(Id.create(2, Lane.class))) {
				assertEquals(0.5, qll.getSimulatedFlowCapacity());
				assertEquals(28.0, qll.getStorageCapacity());
			}
			else {
				assertEquals(0.25, qll.getSimulatedFlowCapacity());
				assertEquals(14.0, qll.getStorageCapacity());
			}
			totalStorageCapacity += qll.getStorageCapacity();
			totalFlowCapacity += qll.getSimulatedFlowCapacity();
		}
		assertEquals(ql.getSpaceCap(), totalStorageCapacity);
		assertEquals(1.0, totalFlowCapacity);
	}
	
	
}
