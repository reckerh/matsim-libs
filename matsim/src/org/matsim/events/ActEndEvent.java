/* *********************************************************************** *
 * project: org.matsim.*
 * ActEndEvent.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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

package org.matsim.events;

import org.matsim.network.Link;
import org.matsim.population.Act;
import org.matsim.population.Person;

public class ActEndEvent extends ActEvent {

	public static final String EVENT_TYPE = "actend";

	public ActEndEvent(final double time, final Person agent, final Link link, final Act act) {
		super(time, agent, link, act);
	}

	@Deprecated // String-based ctors discouraged
	public ActEndEvent(final double time, final String agentId, final String linkId, final String acttype) {
		super(time, agentId, linkId, acttype);
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	@Override
	public String toString() {
		return asString() + "8\t" + EVENT_TYPE + " " + this.acttype;
	}

}
