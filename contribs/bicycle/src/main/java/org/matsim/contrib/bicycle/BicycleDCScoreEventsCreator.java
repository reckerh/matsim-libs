package org.matsim.contrib.bicycle;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.gbl.Gbl;
import org.matsim.vehicles.Vehicle;

import java.util.*;

 class BicycleDCScoreEventsCreator
		 implements VehicleEntersTrafficEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler,VehicleLeavesTrafficEventHandler
 {

	 private static final Logger log = LogManager.getLogger( BicycleScoreEventsCreator.class ) ;

	 //WTPs of ride time
	 //private final double wtpRideTime_min; //TODO: remove bike time scoring here (if it is already implemented somewhere else?)

	 //WTPs per minute of road type
	 private final double wtpSideRoad_min;

	 //WTPs per minute of bike infrastructure
	 private final double wtpBikeLane_min;
	 private final double wtpBikePath_min;
	 private final double wtpProtectedBikeLane_min;

	 //WTPs per minute of road surface
	 private final double wtpCobbled_min;
	 private final double wtpAsphalt_min;

	 //also implement a factor to adjust overall weight of bike WTPs to rest of simulation (like ASC but multiplicative)
	 private final double bikeScoreAdjustmentFactor;

	 //create other variables
	 private final double marginalUtilityOfUserDefinedNetworkAttribute_m;
	 private final String nameOfUserDefinedNetworkAttribute;
	 private final double userDefinedNetworkAttributeDefaultValue;
	 private final String bicycleMode;
	 private final Network network;
	 private final EventsManager eventsManager;

	 Vehicle2DriverEventHandler vehicle2driver = new Vehicle2DriverEventHandler();
	 private Map<Id<Vehicle>,Id<Link>> firstLinkIdMap = new LinkedHashMap<>();
	 private Map<Id<Vehicle>,Double> lastLinkEnterTime = new LinkedHashMap<>(); //track last link enter time of each veh. to calculate time spent on link
	 //should something like a synchronized map be used to prevent issues when using multiple threads?


	 //constructor
	 @Inject BicycleDCScoreEventsCreator( Scenario scenario, EventsManager eventsManager ) {
		 this.eventsManager = eventsManager;

		 this.network = scenario.getNetwork();

		 BicycleConfigGroup bicycleConfigGroup = ConfigUtils.addOrGetModule( scenario.getConfig(), BicycleConfigGroup.class );

		 //implement later: get wtps from config; for now, set WTPs by hand:
		 this.wtpSideRoad_min = 0.0261;
		 this.wtpBikeLane_min = 0.0804;
		 this.wtpBikePath_min = 0.0882;
		 this.wtpProtectedBikeLane_min = 0.1247;
		 this.wtpCobbled_min = 0.0122;
		 this.wtpAsphalt_min = 0.0454;
		 this.bikeScoreAdjustmentFactor = 1.;

		 //deprecate support for user-defined network attributes? (currently uses the functionality of BicycleUtilityUtils that is also used in default scoring)
		 this.marginalUtilityOfUserDefinedNetworkAttribute_m = bicycleConfigGroup.getMarginalUtilityOfUserDefinedNetworkAttribute_m();
		 this.nameOfUserDefinedNetworkAttribute = bicycleConfigGroup.getUserDefinedNetworkAttributeName();
		 this.userDefinedNetworkAttributeDefaultValue = bicycleConfigGroup.getUserDefinedNetworkAttributeDefaultValue();
		 this.bicycleMode = bicycleConfigGroup.getBicycleMode();
	 }

	 @Override public void reset( int iteration ){
		 vehicle2driver.reset( iteration );
	 }
	 @Override public void handleEvent( VehicleEntersTrafficEvent event ){
		 vehicle2driver.handleEvent( event );
		 // ---
		 this.firstLinkIdMap.put( event.getVehicleId(), event.getLinkId() );
		 this.lastLinkEnterTime.put(event.getVehicleId(), event.getTime());
	 }

	 @Override public void handleEvent( LinkEnterEvent event ){
		 this.lastLinkEnterTime.put(event.getVehicleId(), event.getTime());
	 }

	 @Override public void handleEvent( LinkLeaveEvent event ){
		 if ( vehicle2driver.getDriverOfVehicle( event.getVehicleId() ) != null ){
			 // can happen on first link.

			 Link link = network.getLinks().get( event.getLinkId() );
			 double amount = computeLinkBasedDCScore(link, lastLinkEnterTime.get(event.getVehicleId()), event.getTime(),
					 wtpSideRoad_min, wtpBikeLane_min, wtpBikePath_min, wtpProtectedBikeLane_min,
					 wtpCobbled_min, wtpAsphalt_min,
					 marginalUtilityOfUserDefinedNetworkAttribute_m, nameOfUserDefinedNetworkAttribute,
					 userDefinedNetworkAttributeDefaultValue);
			 final Id<Person> driverOfVehicle = vehicle2driver.getDriverOfVehicle( event.getVehicleId() );
			 Gbl.assertNotNull( driverOfVehicle );
			 this.eventsManager.processEvent( new PersonScoreEvent( event.getTime(), driverOfVehicle, amount, "bicycleAdditionalLinkScore" ) );
		 }
	 }
	 @Override public void handleEvent( VehicleLeavesTrafficEvent event ){
		 vehicle2driver.handleEvent( event );
		 // ---
		 if ( vehicle2driver.getDriverOfVehicle( event.getVehicleId() ) != null ){
			 if( !Objects.equals( this.firstLinkIdMap.get( event.getVehicleId() ), event.getLinkId() ) ){ //if driver did not enter and leave on the same link
				 Link link = network.getLinks().get( event.getLinkId() );
				 double amount = computeLinkBasedDCScore(link, lastLinkEnterTime.get(event.getVehicleId()), event.getTime(),
						 wtpSideRoad_min, wtpBikeLane_min, wtpBikePath_min, wtpProtectedBikeLane_min,
						 wtpCobbled_min, wtpAsphalt_min,
						 marginalUtilityOfUserDefinedNetworkAttribute_m, nameOfUserDefinedNetworkAttribute,
						 userDefinedNetworkAttributeDefaultValue);
				 final Id<Person> driverOfVehicle = vehicle2driver.getDriverOfVehicle( event.getVehicleId() );
				 Gbl.assertNotNull( driverOfVehicle );
				 this.eventsManager.processEvent( new PersonScoreEvent( event.getTime(), driverOfVehicle, amount, "bicycleAdditionalLinkScore" ) );
			 }
		 }
	 }



	 public double computeLinkBasedDCScore( Link link, double lastLinkEnterTime, double eventTime,
											double wtpSideRoad_min,
											double wtpBikeLane_min, double wtpBikePath_min, double wtpProtectedBikeLane_min,
											double wtpCobbled_min, double wtpAsphalt_min,
											double marginalUtilityOfUserDefinedNetworkAttribute_m,
											String nameOfUserDefinedNetworkAttribute, double userDefinedNetworkAttributeDefaultValue){

		 //get link attributes
		 String surface = (String) link.getAttributes().getAttribute(BicycleUtils.SURFACE);
		 String type = (String) link.getAttributes().getAttribute(BicycleUtils.WAY_TYPE);
		 String cyclewaytype = (String) link.getAttributes().getAttribute(BicycleUtils.CYCLEWAY);
		 //I don't think any of this is checked for presence of multiple attr (separated by ";") -> treated in WTP getters
		 double distance = link.getLength();

		 double traveledTime = (eventTime-lastLinkEnterTime)/60.; //TODO: look up unit of time (seconds, ...?) and adjust accordingly

		 //scoring of user-defined attributes
		 String userDefinedNetworkAttributeString;
		 double userDefinedNetworkAttributeScore = 0.;
		 if (nameOfUserDefinedNetworkAttribute != null) {
			 userDefinedNetworkAttributeString = BicycleUtils.getUserDefinedNetworkAttribute(link, nameOfUserDefinedNetworkAttribute);
			 double userDefinedNetworkAttributeFactor = BicycleUtilityUtils.getUserDefinedNetworkAttributeFactor(userDefinedNetworkAttributeString, userDefinedNetworkAttributeDefaultValue);
			 userDefinedNetworkAttributeScore = marginalUtilityOfUserDefinedNetworkAttribute_m * (1. - userDefinedNetworkAttributeFactor) * distance;
		 }

		 //Scoring of road type
		 double roadTypeScore = -(wtpSideRoad_min - getRoadTypeWTP(type, wtpSideRoad_min)) * traveledTime;

		 //scoring of bike infrastructure
		 double infrastructureScore = -(wtpProtectedBikeLane_min - getInfrastructureWTP(cyclewaytype, type, wtpBikeLane_min, wtpBikePath_min,
				 wtpProtectedBikeLane_min))*traveledTime;

		 //scoring of surface
		 double surfaceScore = -(wtpAsphalt_min - getSurfaceWTP(surface, type, wtpCobbled_min, wtpAsphalt_min)) * traveledTime;

		 //sum up and adjust the link-based score
		 double amount = (userDefinedNetworkAttributeScore + roadTypeScore + infrastructureScore + surfaceScore)*
				 bikeScoreAdjustmentFactor;
		 return amount;
	 }




	 public double getRoadTypeWTP(String type, double wtpSideRoad_min){
		 //see possible values of link-attribute BicycleUtils.WAY_TYPE (f.e. in BicycleUtilityUtils.getInfrastructureFactor) and think of scoring implementation

		 double roadTypeWTP = 0.;

		 if(type != null){
			 switch(type){
				 //for explanation of type (assumption: type = key:highway) values: https://wiki.openstreetmap.org/wiki/Key:highway
				 //things such as steps could be explicitly punished based on the OSM data, but not based on the DC experiment
				 case "motorway":
				 case "trunk":
				 case "primary":
				 case "secondary":
				 case "tertiary":
				 case "motorway_link":
				 case "trunk_link":
				 case "primary_link":
				 case "secondary_link":
				 case "tertiary_link": roadTypeWTP = 0.; break; //higher-function road network -> main Road -> reference category
				 case "minor":
				 case "unclassified":
				 case "residential":
				 case "living_street":
				 case "service":
				 case "pedestrian":
				 case "track":
				 case "footway":
				 case "path":
				 case "cycleway": roadTypeWTP = wtpSideRoad_min; break; //lower-function network -> side road
				 default: roadTypeWTP = 0.; break; //default is set to 0; may be justified due to possible not-bike-friendly untagged or left-out paths, but maybe too pessimistic
				 //TODO maybe change based on simulation results
			 }
		 }

		 return roadTypeWTP;

	 }

	 public double getInfrastructureWTP(String cyclewaytype, String type, double wtpBikeLane_min, double wtpBikePath_min, double wtpProtectedBikeLane_min){

		 double infrastructureWTP = 0.;



		 //assumption: cyclewaytype = key:cycleway; sadly, this does not cover all types of cycleways in OSM
		 //for (some) options in OSM see https://wiki.openstreetmap.org/wiki/Key:cycleway
		 //furthermore, no key:cycleway values exist for protected bike lanes; instead, they and bike paths are mapped as cycleway=track
		 //(or as cycleway = buffered_lane, but only by mistake and it could still refer to bike paths)
		 //shared busways (more common in DE) and shared car lanes (usually not explicitly marked in DE) not considered
		 //options were adjusted based on infrastructure found present in Stuttgart
		 if (cyclewaytype != null){
			switch(cyclewaytype){
				case "lane":
				case "yes":
				case "right":
				case "opposite":
				case "left":
				case "lane;opposite_lane":
				case "both":
				case "opposite_lane": infrastructureWTP = wtpBikeLane_min; break;
				case "track":
				case "track;opposite_track":
				case "track;opposite_lane":
				case "cyclestreet":
				case "opposite_track": infrastructureWTP = wtpProtectedBikeLane_min; break;
				default: infrastructureWTP = 0.; break;
			}
		 }

		 //road types that provide similar conditions to bike guidance separated from traffic are given the highest wtp
		 //assumption: type = key:highway
		 if (type != null){
			 switch(type){
				 case "pedestrian":
				 case "footway":
				 case "path":
				 case "cycleway": infrastructureWTP = wtpProtectedBikeLane_min; break; //lower-function network -> side road
				 default: infrastructureWTP = 0.; break;
			 }
		 }

		 return infrastructureWTP;

	 }

	 public double getSurfaceWTP(String surface, String type, double wtpCobbled_min, double wtpAsphalt_min){

		 double surfaceWTP = 0.;

		 if (surface != null) {
			 switch (surface) {
				 //for explanations of surface values see https://wiki.openstreetmap.org/wiki/Key:surface
				 case "excellent":
				 case "paved":
				 case "asphalt;paved":
				 case "asphalt": surfaceWTP = wtpAsphalt_min; break; //asphalt & paved both are assigned the asphalt WTP; paved assumed to be very even surface
				 case "cobblestone": surfaceWTP = wtpCobbled_min; break;
				 case "cobblestone (bad)": surfaceWTP = wtpCobbled_min; break;
				 case "sett": surfaceWTP = wtpCobbled_min; break;
				 case "grass_paver":
				 case "cobblestone;flattened":
				 case "cobblestone:flattened": surfaceWTP = wtpCobbled_min; break;
				 case "concrete": surfaceWTP = wtpAsphalt_min; break;
				 case "concrete:lanes": surfaceWTP = wtpAsphalt_min; break;
				 case "concrete_plates":
				 case "concrete:plates": surfaceWTP = wtpAsphalt_min; break;
				 case "paving_stones": surfaceWTP = wtpAsphalt_min; break; //relatively even -> asphalt WTP
				 case "paving_stones:35":
				 case "paving_stones:30": surfaceWTP = wtpAsphalt_min; break;
				 case "unpaved": surfaceWTP = 0.; break; //unpaved and subvalues are assigned WTP 0 (-> reference class)
				 case "compacted": surfaceWTP = 0.; break; //TODO: hier ggf. asphalt zuweisen, da eig. sehr gut befahrbar?
				 case "unhewn_cobblestone":
				 case "artificial_turf":
				 case "dirt":
				 case "earth": surfaceWTP = 0.; break;
				 case "fine_gravel": surfaceWTP = 0.; break; //TODO: hier ggf. asphalt zuweisen, da eig. sehr gut befahrbar?
				 case "gravel":
				 case "ground": surfaceWTP = 0.; break;
				 case "wood": //TODO: hier ggf. asphalt zuweisen, da eig. sehr gut befahrbar?
				 case "pebblestone":
				 case "sand": surfaceWTP = 0.; break;
				 case "bricks": surfaceWTP = wtpCobbled_min; break;
				 case "stone":
				 case "grass":
				 case "compressed": surfaceWTP = 0.; break; //TODO: compressed ggf. besser bewertet als stone und grass?
				 case "asphalt;paving_stones:35": surfaceWTP = wtpAsphalt_min; break;
				 case "paving_stones:3": surfaceWTP = wtpAsphalt_min; break;
				 default: surfaceWTP = 0.; break;
			 }
		 } else {
			 // For many primary and secondary roads, no surface is specified because they are by default assumed to be is asphalt.
			 // For tertiary roads street this is not true, e.g. Friesenstr. in Kreuzberg
			 if (type != null) {
				 if (type.equals("primary") || type.equals("primary_link") || type.equals("secondary") || type.equals("secondary_link") ||
				 type.equals("tertiary") || type.equals("tertiary_link") || type.equals("residential")
				 ) {
					 surfaceWTP = wtpAsphalt_min;
				 }
			 }
		 }

		 return surfaceWTP;

	 }

}
