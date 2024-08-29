package com.ascelion.demo;

import static com.ascelion.guice.GuiceBoot.guiceInit;

import com.ascelion.guice.GuiceScan;

import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@GuiceScan(classes = {
		Producers.class,
		Service.class,
})
public class SimpleApp {
	public static void main(String[] args) {
		guiceInit(SimpleApp.class)
				.modules(new ArgumentsModule(args))
				.boot(SimpleApp.class)
				.start();
	}

	@Inject
	Service service;

	void start() {
		this.service.run();
	}
}
