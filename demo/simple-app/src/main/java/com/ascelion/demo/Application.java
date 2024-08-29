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
public class Application {
	public static void main(String[] args) {
		guiceInit(Application.class)
				.modules(new ArgumentsModule(args))
				.boot(Application.class)
				.start();
	}

	@Inject
	Service service;

	void start() {
		this.service.run();
	}
}
