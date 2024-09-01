package com.ascelion.demo;

import static com.ascelion.guice.GuiceBoot.guiceBoot;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.ascelion.guice.GuiceScan;
import com.ascelion.guice.request.RequestScope;

import java.util.List;

import jakarta.inject.Inject;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@GuiceScan(
		packages = SimpleAws.class,
		classes = {
				RequestScope.class,
		})
@AllArgsConstructor(onConstructor_ = @Inject)
@Slf4j
public class SimpleAws implements RequestHandler<SQSEvent, String[]> {

	@Inject
	private Service service;

	@Inject
	private RequestScope scope;

	public SimpleAws() {
		guiceBoot(this);
	}

	@Override
	public String[] handleRequest(SQSEvent input, Context context) {
		try {
			final var response = this.scope.activate()
					.seed(Context.class, context)
					.seed(SQSEvent.class, input)
					.proceed(this.service::proceed);

			LOG.info("Returning {}", List.of(response));

			return response;
		} catch (final Exception e) {
			LOG.error("Invocation exception", e);

			return new String[] { e.getMessage() };
		}
	}
}
