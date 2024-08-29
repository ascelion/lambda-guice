package com.ascelion.demo;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.ascelion.guice.request.RequestScoped;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

@Singleton
class AwsEvents {

	@Produces
	@RequestScoped
	Context context() {
		throw new UnsupportedOperationException("Context must be set explicitly in scope");
	}

	@Produces
	@RequestScoped
	SQSEvent sqsEvent() {
		throw new UnsupportedOperationException("Event must be set explicitly in scope");
	}
}
