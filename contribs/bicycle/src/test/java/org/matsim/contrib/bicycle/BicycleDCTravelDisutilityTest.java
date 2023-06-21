package org.matsim.contrib.bicycle;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonScoreEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import static org.junit.Assert.*;

public class BicycleDCTravelDisutilityTest {

	//define some vars as private in the class so they don't need to be passed all the time
	private double time;
	private Link link1;
	private Person person1;
	private Vehicle veh1;
	private Config config;
	private Scenario scenario;
	private EventsManager manager;
	private BicycleConfigGroup bikeConfig;
	private PlanCalcScoreConfigGroup cnScoringGroup;
	private PlansCalcRouteConfigGroup plansCalcRouteConfigGroup;
	private TravelTime timeCalculator;
	private double normalization;
	private BicycleDCTravelDisutility testClass;

	@Test
	public void dummyTest() {

		//prepare dummy classes
		timeCalculator = new TestTravelTime();

		// prepare non-scenario-related values
		normalization = 1.;
		time = 60.;

		// prepare scenario
		config = ConfigUtils.createConfig();

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
		config.addModule(bikeConfig);

		cnScoringGroup = config.planCalcScore();
		PlanCalcScoreConfigGroup.ModeParams bicycleParams = cnScoringGroup.getOrCreateModeParams(BicycleConfigGroup.GROUP_NAME);
		bicycleParams.setMonetaryDistanceRate(-0.1);
		bicycleParams.setMarginalUtilityOfDistance(-0.1);
		bicycleParams.setMarginalUtilityOfTraveling(-0.1);
		cnScoringGroup.setMarginalUtilityOfMoney(1.);
		cnScoringGroup.setPerforming_utils_hr(8.);

		plansCalcRouteConfigGroup = config.plansCalcRoute();
		plansCalcRouteConfigGroup.setRoutingRandomness(0.);

		scenario = ScenarioUtils.createScenario(config);

		// create network
		var node1 = scenario.getNetwork().getFactory().createNode(Id.createNodeId("node1"), new Coord(0, 0));
		var node2 = scenario.getNetwork().getFactory().createNode(Id.createNodeId("node2"), new Coord(0, 100));
		link1 = scenario.getNetwork().getFactory().createLink(Id.createLinkId("link1"), node1, node2);
		link1.getAttributes().putAttribute(BicycleUtils.WAY_TYPE, "nonsense");
		link1.getAttributes().putAttribute(BicycleUtils.SURFACE, "nonsense");
		link1.getAttributes().putAttribute(BicycleUtils.CYCLEWAY, "nonsense");
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
		var handler = new BicycleDCTravelDisutilityTest.Handler();
		manager = EventsUtils.createEventsManager();
		manager.addHandler(handler);


		testClass = new BicycleDCTravelDisutility(bikeConfig, cnScoringGroup, plansCalcRouteConfigGroup, timeCalculator, normalization, scenario, manager);
		//try{setUp();}catch (java.lang.Exception exception){};
		double res = testClass.getLinkTravelDisutility(link1, time, person1, veh1);
		double expRes = (
				2 * time * (-(bicycleParams.getMarginalUtilityOfTraveling() / 3600.) + cnScoringGroup.getPerforming_utils_hr() / 3600.) +
						link1.getLength() * (-(bicycleParams.getMonetaryDistanceRate() * cnScoringGroup.getMarginalUtilityOfMoney()) -
								bicycleParams.getMarginalUtilityOfDistance()) +
						(-((-1. + -2. + -3.) / 10.))
		);
		assertEquals(res, expRes, 0.0001);


	}

	//prepare Guice
	//@Before does not work here as testClass is still null before -> I call setup manually, I do not know if using the injections in the constructor
	//will work in practice anyways
	public void setUp() throws Exception {
		Injector injector = Guice.createInjector(new Module() {
			@Override
			public void configure(Binder binder) {
				binder.bind(Scenario.class).toInstance(scenario);
				binder.bind(EventsManager.class).toInstance(manager);
			}
		});
		injector.injectMembers(testClass);
	}

	static class TestTravelTime implements TravelTime {

		@Override
		public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
			return time;
		}
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
