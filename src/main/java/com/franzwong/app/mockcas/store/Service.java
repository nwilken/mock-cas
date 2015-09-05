package com.franzwong.app.mockcas.store;

public class Service {
	
	private String serviceId;
	
	private String serviceTicketId;
	
	public Service(String serviceId, String serviceTicketId) {
		this.setServiceId(serviceId);
		this.setServiceTicketId(serviceTicketId);
	}

	public String getServiceId() {
		return serviceId;
	}

	public void setServiceId(String serviceId) {
		this.serviceId = serviceId;
	}

	public String getServiceTicketId() {
		return serviceTicketId;
	}

	public void setServiceTicketId(String serviceTicketId) {
		this.serviceTicketId = serviceTicketId;
	}
}
