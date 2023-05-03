package org.matsim.contrib.bicycle;

import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.mobsim.qsim.qnetsimengine.ConfigurableQNetworkFactory;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import static org.junit.Assert.*;

public class BicycleDCScoreEventsCreatorTest {

	//define some vars as private in the class so they don't need to be passed all the time
	private Link link1;
	private Person person1;
	private Vehicle veh1;
	private BicycleConfigGroup bikeConfig;
	private BicycleDCScoreEventsCreator testClass;

	@Test
	public void dummyTest() {

		double expScoreSum = 0;

		// prepare scenario
		bikeConfig = new BicycleConfigGroup();
		bikeConfig.setWtpAdjustmentFactor(.1);
		bikeConfig.setWtpSideRoad_min(1.);
		bikeConfig.setWtpBikeLane_min(1.);
		bikeConfig.setWtpBikePath_min(2.);
		bikeConfig.setWtpProtectedBikeLane_min(3.);
		bikeConfig.setWtpCobbled_min(1.);
		bikeConfig.setWtpAsphalt_min(2.);
		bikeConfig.setMarginalUtilityOfUserDefinedNetworkAttribute_m(-0.01);
		bikeConfig.setUserDefinedNetworkAttributeName("myComfortAttr");
		bikeConfig.setUserDefinedNetworkAttributeDefaultValue(1.);
		var config = ConfigUtils.createConfig(bikeConfig);
		var scenario = ScenarioUtils.createScenario(config);

		// create network
		var node1 = scenario.getNetwork().getFactory().createNode(Id.createNodeId("node1"), new Coord(0, 0));
		var node2 = scenario.getNetwork().getFactory().createNode(Id.createNodeId("node2"), new Coord(0, 100));
		link1 = scenario.getNetwork().getFactory().createLink(Id.createLinkId("link1"), node1, node2);
		scenario.getNetwork().addNode(node1);
		scenario.getNetwork().addNode(node2);
		scenario.getNetwork().addLink(link1);

		//create population
		scenario.getPopulation().addPerson(PopulationUtils.getFactory().createPerson(Id.createPersonId("person1")));
		person1 = scenario.getPopulation().getPersons().get(Id.createPersonId("person1"));
		scenario.getVehicles().addVehicleType(VehicleUtils.createVehicleType(Id.create("bike", VehicleType.class)));
		scenario.getVehicles().addVehicle(VehicleUtils.createVehicle(Id.createVehicleId("person1"
		), scenario.getVehicles().getVehicleTypes().get(Id.create("bike", VehicleType.class))));
		veh1 = scenario.getVehicles().getVehicles().get(Id.createVehicleId("person1"));


		// prepare events manager
		var handler = new Handler();
		var manager = EventsUtils.createEventsManager();
		manager.addHandler(handler);

		// prepare scoring class
		testClass = new BicycleDCScoreEventsCreator(scenario, manager);

		// create and handle events, track expected score sum
		var link1Attributes = link1.getAttributes();

		//case: type NULL, surface NULL, cycleway type NULL
		expScoreSum += createTestEvents(0., 0., 60., 60., (-1. + -2. + -3.) / 10.);

		//case: type unknown, surface unknown, cycleway type unknown
		link1Attributes.putAttribute(BicycleUtils.WAY_TYPE, "nonsense");
		link1Attributes.putAttribute(BicycleUtils.SURFACE, "nonsense");
		link1Attributes.putAttribute(BicycleUtils.CYCLEWAY, "nonsense");
		expScoreSum += createTestEvents(0., 0., 60., 60., (-1. + -2. + -3.) / 10.);

		//case: type primary, surface NULL (-> assumed good due to primary), cycleway type unknown
		link1Attributes.putAttribute(BicycleUtils.WAY_TYPE, "primary");
		link1Attributes.removeAttribute(BicycleUtils.SURFACE);
		expScoreSum += createTestEvents(0., 0., 60., 60., (-1. + -0. + -3.) / 10.);

		//case: type motorway, excellent surface, unkown cycleway type
		link1Attributes.putAttribute(BicycleUtils.WAY_TYPE, "motorway");
		link1Attributes.putAttribute(BicycleUtils.SURFACE, "excellent");
		link1Attributes.putAttribute(BicycleUtils.CYCLEWAY, "nonsense");
		expScoreSum += createTestEvents(0., 0., 60., 60., (-1. + -0. + -3.) / 10.);

		//case: type path, surface cobbled, unknown cycleway type (implicit: pbl, due to type path)
		link1Attributes.putAttribute(BicycleUtils.WAY_TYPE, "path");
		link1Attributes.putAttribute(BicycleUtils.SURFACE, "cobblestone");
		link1Attributes.putAttribute(BicycleUtils.CYCLEWAY, "nonsense");
		expScoreSum += createTestEvents(0., 0., 60., 60., (0. + -1. + -0.) / 10.);

		//case: type footway, surface cobbled, cycleway type track (implicit: bike path, due to type footway)
		link1Attributes.putAttribute(BicycleUtils.WAY_TYPE, "footway");
		link1Attributes.putAttribute(BicycleUtils.SURFACE, "cobblestone");
		link1Attributes.putAttribute(BicycleUtils.CYCLEWAY, "track");
		expScoreSum += createTestEvents(0., 0., 60., 60., (0. + -1. + -1.) / 10.);

		//case: type residential, surface cobbled, cycleway type track
		link1Attributes.putAttribute(BicycleUtils.WAY_TYPE, "residential");
		link1Attributes.putAttribute(BicycleUtils.SURFACE, "cobblestone");
		link1Attributes.putAttribute(BicycleUtils.CYCLEWAY, "track");
		expScoreSum += createTestEvents(0., 0., 60., 60., (0. + -1. + 0.) / 10.);

		//case: type residential, surface cobbled, cycleway type lane
		link1Attributes.putAttribute(BicycleUtils.WAY_TYPE, "residential");
		link1Attributes.putAttribute(BicycleUtils.SURFACE, "cobblestone");
		link1Attributes.putAttribute(BicycleUtils.CYCLEWAY, "lane");
		expScoreSum += createTestEvents(0., 0., 60., 60., (0. + -1. + -2.) / 10.);

		//case: type residential, surface cobbled, cycleway type unknown but assumed lane due to lcn
		link1Attributes.putAttribute(BicycleUtils.WAY_TYPE, "residential");
		link1Attributes.putAttribute(BicycleUtils.SURFACE, "cobblestone");
		link1Attributes.putAttribute(BicycleUtils.CYCLEWAY, "nonsense");
		link1Attributes.putAttribute("lcn", "yes");
		expScoreSum += createTestEvents(0., 0., 60., 60., (0. + -1. + -2.) / 10.);

		//case: type residential, surface cobbled, cycleway type unknown but assumed lane due to lcn, userdefinedattr gives -1
		link1Attributes.putAttribute(BicycleUtils.WAY_TYPE, "residential");
		link1Attributes.putAttribute(BicycleUtils.SURFACE, "cobblestone");
		link1Attributes.putAttribute(BicycleUtils.CYCLEWAY, "nonsense");
		link1Attributes.putAttribute("lcn", "yes");
		link1Attributes.putAttribute("myComfortAttr", "0");
		expScoreSum += createTestEvents(0., 0., 60., 60., (0. + -1. + -2. + -1.) / 10.);

		// after all event handlers are called check the score
		System.out.println("expected Score sum: " + expScoreSum);
		assertEquals(expScoreSum, handler.accumulatedScore, 0.0001);
	}

	public double createTestEvents(double tet, double let, double llt, double tlt, double expScore) {

		//This class is not exactly "realistic" as it does both a enterTraffic/linkEnter and linkLeave/leaveTraffic
		//event for the same link (should normally not be the case)
		//my vehicleLeavesTrafficEvent, however, only scores if the driver did not enter and leave the traffic on the same link

		testClass.handleEvent(new VehicleEntersTrafficEvent(
				tet, person1.getId(), link1.getId(), veh1.getId(), bikeConfig.getBicycleMode(), 0.
		));

		testClass.handleEvent(new LinkEnterEvent(let, veh1.getId(), link1.getId()));

		testClass.handleEvent(new LinkLeaveEvent(llt, veh1.getId(), link1.getId()));

		testClass.handleEvent(new VehicleLeavesTrafficEvent(
				tlt, person1.getId(), link1.getId(), veh1.getId(), bikeConfig.getBicycleMode(), 1.
		));

		return expScore;
	}

	private static class Handler implements BasicEventHandler {

		private double accumulatedScore = 0;

		@Override
		public void handleEvent(Event event) {
			if (PersonScoreEvent.EVENT_TYPE.equals(event.getEventType())) {
				// do assertions about events here
				var scoreEvent = (PersonScoreEvent) event;
				accumulatedScore += scoreEvent.getAmount();
			}
		}
	}
}
