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

package org.matsim.contrib.transEnergySim.controllers;

import java.util.LinkedList;

import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.Wait2LinkEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.PersonEntersVehicleEvent;
import org.matsim.core.api.experimental.events.PersonLeavesVehicleEvent;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.Wait2LinkEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;

public class EventHandlerGroup implements ActivityStartEventHandler, AgentArrivalEventHandler,
AgentDepartureEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, Wait2LinkEventHandler {

	protected LinkedList<EventHandler> handler = new LinkedList<EventHandler>();

	public void addHandler(EventHandler handler) {
		this.handler.add(handler);
	}

	@Override
	public void reset(int iteration) {
		for (EventHandler h : handler) {
			h.reset(iteration);
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		for (EventHandler h : handler) {
			if (h instanceof LinkLeaveEventHandler) {
				((LinkLeaveEventHandler) h).handleEvent(event);
			}
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		for (EventHandler h : handler) {
			if (h instanceof LinkEnterEventHandler) {
				((LinkEnterEventHandler) h).handleEvent(event);
			}
		}
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		for (EventHandler h : handler) {
			if (h instanceof AgentDepartureEventHandler) {
				((AgentDepartureEventHandler) h).handleEvent(event);
			}
		}
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		for (EventHandler h : handler) {
			if (h instanceof AgentArrivalEventHandler) {
				((AgentArrivalEventHandler) h).handleEvent(event);
			}
		}
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		for (EventHandler h : handler) {
			if (h instanceof ActivityStartEventHandler) {
				((ActivityStartEventHandler) h).handleEvent(event);
			}
		}
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		for (EventHandler h : handler) {
			if (h instanceof PersonLeavesVehicleEventHandler) {
				((PersonLeavesVehicleEventHandler) h).handleEvent(event);
			}
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		for (EventHandler h : handler) {
			if (h instanceof PersonEntersVehicleEventHandler) {
				((PersonEntersVehicleEventHandler) h).handleEvent(event);
			}
		}
		
	}

	@Override
	public void handleEvent(Wait2LinkEvent event) {
		for (EventHandler h : handler) {
			if (h instanceof Wait2LinkEventHandler) {
				((Wait2LinkEventHandler) h).handleEvent(event);
			}
		}		
	}


}
