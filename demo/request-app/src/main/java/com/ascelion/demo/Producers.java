package com.ascelion.demo;

import com.ascelion.guice.request.RequestScoped;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

final class Producers {

	@Produces
	@Singleton
	ExecutorService executorService() {
		return Executors.newFixedThreadPool(4);
	}

	@Produces
	@RequestScoped
	AppRequestWrapper wrapper(AppRequest request) {
		return new AppRequestWrapper(request);
	}

	@Produces
	@RequestScoped
	AppContext context() {
		throw new UnsupportedOperationException();
	}
}
