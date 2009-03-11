package playground.ciarif.retailers;

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.controler.Controler;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Facility;

public class Retailer {
	private final Id id;
	private final Map<Id,Facility> facilities = new LinkedHashMap<Id,Facility>();
	private RetailerStrategy strategy;
		
	protected Retailer(final Id id, RetailerStrategy rs) { 
		this.id = id;
		this.strategy = rs;
	}

	public final Id getId() {
		return this.id;
	}

	public final boolean addFacility(Facility f) {
		if (f == null) { return false; }
		if (this.facilities.containsKey(f.getId())) { return false; }
		this.facilities.put(f.getId(),f);
		return true;
	}
	
	public final boolean addStrategy (Controler controler, String strategyName) {
			
		if (strategyName.equals(RandomRetailerStrategy.NAME)) {
			this.strategy = new RandomRetailerStrategy(controler.getNetwork());
			return true;
		}
		else if (strategyName.equals(MaxLinkRetailerStrategy.NAME)) {
			this.strategy = new MaxLinkRetailerStrategy (controler);
			return true;
		}
		else if (strategyName.equals(LogitMaxLinkRetailerStrategy.NAME)) {
			this.strategy = new LogitMaxLinkRetailerStrategy (controler);
			return true;
		}
		else if (strategyName.equals(CatchmentAreaRetailerStrategy.NAME)) {
			this.strategy = new CatchmentAreaRetailerStrategy (controler);
			return true;
		}
		else { return false; }
	}
	
	public final Facility getFacility(final Id facId) {
		return this.facilities.get(facId);
	}

	public final Map<Id,Facility> getFacilities() {
		return this.facilities;
	}

	public final Map<Id,Facility> runStrategy() {
		System.out.println("Strategy = " + strategy);
		System.out.println("Facilities = " + facilities);
		strategy.moveFacilities(this.facilities);
		return this.facilities;
	}
	

}
