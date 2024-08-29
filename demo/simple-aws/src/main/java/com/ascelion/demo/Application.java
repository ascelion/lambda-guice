package com.ascelion.demo;

import static com.ascelion.guice.GuiceBoot.guiceBoot;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.ascelion.guice.GuiceScan;
import com.ascelion.guice.request.RequestScope;

import jakarta.inject.Inject;
import lombok.AllArgsConstructor;

@GuiceScan(
		packages = Application.class,
		classes = {
				RequestScope.class,
		})
@AllArgsConstructor(onConstructor_ = @Inject)
public class Application implements RequestHandler<SQSEvent, Void> {

	@Inject
	private Service service;

	@Inject
	private RequestScope scope;

	public Application() {
		guiceBoot(this);
	}

	@Override
	public Void handleRequest(SQSEvent input, Context context) {
		this.scope.activate()
				.seed(Context.class, context)
				.seed(SQSEvent.class, input)
				.proceed(this.service::proceed);

		return null;
	}
}
