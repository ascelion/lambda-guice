package com.ascelion.demo;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;

import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GlueService {
	@Inject
	private Context context;

	@Inject
	private SQSEvent event;

	void proceed() {
		LOG.info("Got event with id {} as {}", this.context.getAwsRequestId(), this.event.getRecords());
	}
}
