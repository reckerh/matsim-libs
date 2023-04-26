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

		 //get WTPs
		 this.wtpSideRoad_min = bicycleConfigGroup.getWtpSideRoad_min();
		 this.wtpBikeLane_min = bicycleConfigGroup.getWtpBikeLane_min();
		 this.wtpBikePath_min = bicycleConfigGroup.getWtpBikePath_min();
		 this.wtpProtectedBikeLane_min = bicycleConfigGroup.getWtpProtectedBikeLane_min();
		 this.wtpCobbled_min = bicycleConfigGroup.getWtpCobbled_min();
		 this.wtpAsphalt_min = bicycleConfigGroup.getWtpAsphalt_min();
		 this.bikeScoreAdjustmentFactor = bicycleConfigGroup.getWtpAdjustmentFactor();

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
			 double amount = computeLinkBasedDCScore(link, lastLinkEnterTime.get(event.getVehicleId()), event.getTime());
			 final Id<Person> driverOfVehicle = vehicle2driver.getDriverOfVehicle( event.getVehicleId() );
			 Gbl.assertNotNull( driverOfVehicle );
			 this.eventsManager.processEvent( new PersonScoreEvent( event.getTime(), driverOfVehicle, amount, "bicycleAdditionalDCLinkScore" ) );
		 }
	 }
	 @Override public void handleEvent( VehicleLeavesTrafficEvent event ){
		 vehicle2driver.handleEvent( event );
		 // ---
		 if ( vehicle2driver.getDriverOfVehicle( event.getVehicleId() ) != null ){
			 if( !Objects.equals( this.firstLinkIdMap.get( event.getVehicleId() ), event.getLinkId() ) ){ //if driver did not enter and leave on the same link
				 Link link = network.getLinks().get( event.getLinkId() );
				 double amount = computeLinkBasedDCScore(link, lastLinkEnterTime.get(event.getVehicleId()), event.getTime());
				 final Id<Person> driverOfVehicle = vehicle2driver.getDriverOfVehicle( event.getVehicleId() );
				 Gbl.assertNotNull( driverOfVehicle );
				 this.eventsManager.processEvent( new PersonScoreEvent( event.getTime(), driverOfVehicle, amount, "bicycleAdditionalDCLinkScore" ) );
			 }
		 }
	 }



	 public double computeLinkBasedDCScore( Link link, double lastLinkEnterTime, double eventTime){

		 //get link attributes
		 String surface = (String) link.getAttributes().getAttribute(BicycleUtils.SURFACE);
		 String type = (String) link.getAttributes().getAttribute(BicycleUtils.WAY_TYPE);
		 String cyclewaytype = (String) link.getAttributes().getAttribute(BicycleUtils.CYCLEWAY);
		 //I don't think any of this is checked for presence of multiple attr (separated by ";") -> treated in WTP getters
		 double distance = link.getLength();

		 //calculation of traveled time on link in minutes as WTPs are per minute
		 double traveledTime = (eventTime-lastLinkEnterTime)/60.; //TODO: look up unit of time (seconds, ...?) and adjust accordingly (so it is in minutes)

		 //scoring of user-defined attributes
		 String userDefinedNetworkAttributeString;
		 double userDefinedNetworkAttributeScore = 0.;
		 if (nameOfUserDefinedNetworkAttribute != null) {
			 userDefinedNetworkAttributeString = BicycleUtils.getUserDefinedNetworkAttribute(link, nameOfUserDefinedNetworkAttribute);
			 double userDefinedNetworkAttributeFactor = BicycleUtilityUtils.getUserDefinedNetworkAttributeFactor(userDefinedNetworkAttributeString, userDefinedNetworkAttributeDefaultValue);
			 userDefinedNetworkAttributeScore = marginalUtilityOfUserDefinedNetworkAttribute_m * (1. - userDefinedNetworkAttributeFactor) * distance;
		 }

		 //Scoring of road type
		 double roadTypeScore = -(wtpSideRoad_min - getRoadTypeWTP(type)) * traveledTime;

		 //scoring of bike infrastructure
		 double infrastructureScore = -(wtpProtectedBikeLane_min - getInfrastructureWTP(cyclewaytype, type))*traveledTime;

		 //scoring of surface
		 double surfaceScore = -(wtpAsphalt_min - getSurfaceWTP(surface, type)) * traveledTime;

		 //sum up and adjust the link-based score
		 double amount = (userDefinedNetworkAttributeScore + roadTypeScore + infrastructureScore + surfaceScore)*
				 bikeScoreAdjustmentFactor;
		 return amount;
	 }




	 public double getRoadTypeWTP(String type){
		 //see possible values of link-attribute BicycleUtils.WAY_TYPE (f.e. in BicycleUtilityUtils.getInfrastructureFactor) and think of scoring implementation

		 if(type != null){
			 return switch (type) {
				 //for explanation of type (assumption: type = key:highway) values: https://wiki.openstreetmap.org/wiki/Key:highway
				 //things such as steps could be explicitly punished based on the OSM data, but not based on the DC experiment
				 case "motorway", "trunk", "primary", "secondary", "tertiary", "motorway_link", "trunk_link", "primary_link", "secondary_link", "tertiary_link" ->
						 0.; //higher-function road network -> main Road -> reference category //-> TODO: Indentierung übernehmen oder automatisch von IntelliJ machen lassen
				 case "minor", "unclassified", "residential", "living_street", "service", "pedestrian", "track", "footway", "path", "cycleway" ->
						 wtpSideRoad_min; //lower-function network -> side road
				 default ->
						 0.; //default is set to 0; may be justified due to possible not-bike-friendly untagged or left-out paths, but maybe too pessimistic
				 //TODO maybe change based on simulation results
			 } ;
		 } else {
			 return 0;
		 }

	 }

	 public double getInfrastructureWTP(String cyclewaytype, String type){

		 double infrastructureWTP = 0.;



		 //assumption: cyclewaytype = key:cycleway; sadly, this does not cover all types of cycleways in OSM
		 //for (some) options in OSM see https://wiki.openstreetmap.org/wiki/Key:cycleway
		 //furthermore, no key:cycleway values exist for protected bike lanes; instead, they and bike paths are mapped as cycleway=track
		 //(or as cycleway = buffered_lane, but only by mistake and it could still refer to bike paths)
		 //shared busways (more common in DE) and shared car lanes (usually not explicitly marked in DE) not considered
		 //options were adjusted based on infrastructure found present in Stuttgart
		 if (cyclewaytype != null){
			 infrastructureWTP = switch (cyclewaytype) {
				 case "lane", "yes", "right", "opposite", "left", "lane;opposite_lane", "both", "opposite_lane" -> wtpBikeLane_min;
				 case "track", "track;opposite_track", "track;opposite_lane", "cyclestreet", "opposite_track" -> wtpProtectedBikeLane_min;
				 default -> 0.;
			 };
		 }

		 //road types that provide similar conditions to bike guidance separated from traffic are given the highest wtp
		 //assumption: type = key:highway
		 if (type != null){
			 infrastructureWTP = switch (type) {
				 case "pedestrian", "footway", "path", "cycleway" -> wtpProtectedBikeLane_min; //lower-function network -> side road
				 default -> 0.;
			 };
		 }

		 return infrastructureWTP;

	 }

	 public double getSurfaceWTP(String surface, String type){

		 double surfaceWTP = 0.;

		 if (surface != null) {
			 surfaceWTP = switch (surface) {
				 //for explanations of surface values see https://wiki.openstreetmap.org/wiki/Key:surface
				 case "excellent", "paved", "asphalt;paved", "asphalt", "concrete", "concrete:lanes", "concrete_plates",
						 "concrete:plates", "paving_stones", "paving_stones:3", "paving_stones:35", "paving_stones:30",
						 "asphalt;paving_stones:35" -> wtpAsphalt_min; //asphalt & paved both are assigned the asphalt WTP; paved assumed to be very even surface
				 case "cobblestone", "cobblestone (bad)", "sett", "grass_paver", "cobblestone;flattened", "cobblestone:flattened",
						 "bricks" -> wtpCobbled_min;
				 case "unpaved", "compacted", "unhewn_cobblestone", "artificial_turf", "dirt", "earth", "gravel", "ground",
						 "fine_gravel", "wood", "pebblestone", "sand", "stone", "grass", "compressed" -> 0.; //unpaved and subvalues are assigned WTP 0 (-> reference class)
				 default -> 0.;
			 };
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
