package com.franzwong.app.mockcas;

import com.franzwong.app.mockcas.resource.CasResource;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewBundle;

public class MockCasApplication extends Application<MockCasConfiguration> {
	
	public static void main(String[] args) throws Exception {
		new MockCasApplication().run(args);
	}

	@Override
	public void initialize(Bootstrap<MockCasConfiguration> bootstrap) {
		bootstrap.addBundle(new ViewBundle<MockCasConfiguration>());
	}

	@Override
	public void run(final MockCasConfiguration config, Environment env) throws Exception {
		Injector injector = Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bind(MockCasConfiguration.class).toInstance(config);
			}
		});

		env.jersey().register(injector.getInstance(CasResource.class));
	}
}
