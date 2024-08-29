package com.ascelion.demo;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;

import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class Service {
	@Inject
	private Context context;

	@Inject
	private SQSEvent request;

	public void proceed() {
		LOG.info("Got request {} in context {}", this.request, this.context.getAwsRequestId());
	}
}
