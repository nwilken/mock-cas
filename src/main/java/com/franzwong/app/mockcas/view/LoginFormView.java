package com.franzwong.app.mockcas.view;

import io.dropwizard.views.View;

public class LoginFormView extends View {
	
	private String service;

	public LoginFormView(String service) {
		super("/views/loginForm.ftl");
		
		setService(service);
	}

	public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
	}

}
