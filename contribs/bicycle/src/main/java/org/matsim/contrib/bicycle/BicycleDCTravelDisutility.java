package org.matsim.contrib.bicycle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;
import java.util.Random;
import com.google.inject.Inject;

/**
 * @author reckerh
 * basically a copy of BicycleTravelDisutility (June 2023), but with adjustments to use logit-model-based scores as disutility basis;
 * another difference is that, for easier application and programming, I only use the output of the score-calculation-method and thus only randomly vary
 * the entire logit-model-based score
 */

public class BicycleDCTravelDisutility implements TravelDisutility {
	private static final Logger LOG = LogManager.getLogger(BicycleDCTravelDisutility.class);

	private final double marginalCostOfTime_s;
	private final double marginalCostOfDistance_m;

	private final double normalization;
	private final double sigma;
	private final Random random;
	private final TravelTime timeCalculator;

	//private final BicycleDCScoreEventsCreator scoreCreator;

	private double normalRndLink;
	private double logNormalRndDist;
	private double logNormalRndDCScore;
	private Person prevPerson;
	private BicycleDCScoreEventsCreator scoreCreator;

//	@Inject
//	Scenario scenario;
//
//	@Inject
//	EventsManager eventsManager;

	BicycleDCTravelDisutility(BicycleConfigGroup bicycleConfigGroup, PlanCalcScoreConfigGroup cnScoringGroup, PlansCalcRouteConfigGroup plansCalcRouteConfigGroup,
							  TravelTime timeCalculator, double normalization, Scenario scenario, EventsManager eventsManager) {

		final PlanCalcScoreConfigGroup.ModeParams bicycleParams = cnScoringGroup.getModes().get(bicycleConfigGroup.getBicycleMode());
		if (bicycleParams == null){
			throw new NullPointerException("Mode " + bicycleConfigGroup.getBicycleMode() + "is not part of the valid mode parameters " + cnScoringGroup.getModes().keySet());
		}

		this.marginalCostOfDistance_m = - (bicycleParams.getMonetaryDistanceRate() * cnScoringGroup.getMarginalUtilityOfMoney())
				- bicycleParams.getMarginalUtilityOfDistance();
		this.marginalCostOfTime_s = -(bicycleParams.getMarginalUtilityOfTraveling() / 3600.0) + cnScoringGroup.getPerforming_utils_hr() / 3600.0;

		this.timeCalculator = timeCalculator;

		this.normalization = normalization;
		this.sigma = plansCalcRouteConfigGroup.getRoutingRandomness();
		this.random = sigma != 0 ? MatsimRandom.getLocalInstance() : null;

		this.scoreCreator = new BicycleDCScoreEventsCreator(scenario, eventsManager);
	}

	@Override
	public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle){

		//BicycleDCScoreEventsCreator scoreCreator = new BicycleDCScoreEventsCreator(scenario, eventsManager);
		//I do not not if injection will work if I do this in the init and use an instance-wide variable
		//(it at least does not work when doing a unit-test), so I have to do this every time

		double travelTime = timeCalculator.getLinkTravelTime(link, time, person, vehicle);
		double distance = link.getLength();

		double travelTimeDisutility = marginalCostOfTime_s * travelTime;
		double distanceDisutility = marginalCostOfDistance_m * distance;

		double dcDisutility = -(scoreCreator.computeLinkBasedDCScore(link, 0., travelTime));
		//tt and dist have already had the sign of their marginal Costs altered so they are positive, the DCScore is usually negative so I have to make it positive by hand


		// randomize if applicable
		if ( sigma != 0.) {

			if (person == null) {
				throw new RuntimeException("you cannot use the randomizing travel disutility without person. If you need this without a person, set sigma to zero.");
			}

			normalRndLink = 0.05 * random.nextGaussian();
			// (copied comment:) yyyyyy are we sure that this is a good approach?  In high resolution networks, this leads to quirky detours ...  kai, sep'19
			if (person != prevPerson) {
				prevPerson = person;

				logNormalRndDist = Math.exp(sigma * random.nextGaussian());
				logNormalRndDCScore = Math.exp(sigma * random.nextGaussian());

				logNormalRndDist *= normalization;
				logNormalRndDCScore *= normalization;
			}

		} else {
			normalRndLink = 1.;
			logNormalRndDist = 1.;
			logNormalRndDCScore = 1.;
		}

		double disutility = (1 + normalRndLink) * travelTimeDisutility + logNormalRndDist * distanceDisutility + logNormalRndDCScore * dcDisutility;
		return disutility;

	}

	@Override
	public double getLinkMinimumTravelDisutility(Link link){
		return 0;
	}







}
