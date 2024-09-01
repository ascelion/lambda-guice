package com.ascelion.demo;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;

import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class Service {
	@Inject
	private Context context;

	@Inject
	private SQSEvent event;

	public String[] proceed() {
		final var records = this.event.getRecords();

		LOG.info("Got event with id {} as {}", this.context.getAwsRequestId(), records);

		return records.stream().map(SQSMessage::getBody).toArray(String[]::new);
	}
}
