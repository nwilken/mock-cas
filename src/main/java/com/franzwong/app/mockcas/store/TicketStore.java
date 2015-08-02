package com.franzwong.app.mockcas.store;

import java.util.HashMap;
import java.util.Map;

import com.google.inject.Singleton;

@Singleton
public class TicketStore {
	private Map<String, String> map;
	
	public TicketStore() {
		map = new HashMap<>();
	}
	
	public void put(String ticketId, String value) {
		map.put(ticketId, value);
	}
	
	public String get(String ticketId) {
		return map.get(ticketId);
	}
}
