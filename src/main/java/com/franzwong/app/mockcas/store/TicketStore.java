package com.franzwong.app.mockcas.store;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.inject.Singleton;

@Singleton
public class TicketStore {
	
	private Map<String, Set<Service>> ticketMap;
	
	private Map<String, String> userMap;
	
	public TicketStore() {
		ticketMap = new HashMap<>();
		userMap = new HashMap<>();
	}
	
	public void addTicketGrantingTicket(String ticketGrantingTicketId, String userName) {
		ticketMap.put(ticketGrantingTicketId, new HashSet<Service>());
		userMap.put(ticketGrantingTicketId, userName);
	}
	
	public String getUserName(String ticketGrantingTicketId) {
		return userMap.get(ticketGrantingTicketId);
	}
	
	public void addServiceTicket(String ticketGrantingTicketId, String serviceId, String serviceTicketId) {
		Set<Service> set = ticketMap.get(ticketGrantingTicketId);
		set.add(new Service(serviceId, serviceTicketId));
	}
	
	public String getTicketGrantingTicketByServiceTicket(String serviceTicketId) {
		for (Map.Entry<String, Set<Service>> entry : ticketMap.entrySet()) {
			for (Service service : entry.getValue()) {
				if (serviceTicketId.equals(service.getServiceTicketId())) {
					return entry.getKey();
				}
			}
		}
		
		return null;
	}
	
	public Set<Service> getServices(String ticketGrantingTicketId) {
		Set<Service> services = ticketMap.get(ticketGrantingTicketId);
		
		if (null == services) {
			return null;
		}
		
		return new HashSet<Service>(services);
	}
	
	public void destroyTicketGrantingTicket(String ticketGrantingTicketId) {
		System.out.println("ticketMap:" + ticketMap.size());
		System.out.println("userMap:" + userMap.size());
		
		ticketMap.remove(ticketGrantingTicketId);
		userMap.remove(ticketGrantingTicketId);
	}
}
