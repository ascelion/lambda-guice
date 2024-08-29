package com.ascelion.demo;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.*;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.ascelion.guice.jupiter.GuiceBootExtension;

import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@ExtendWith(GuiceBootExtension.class)
class ApplicationTest {

	@Mock
	Context context;

	@Mock
	SQSEvent event;

	@Inject
	Application app;

	@Test
	void run() {
		when(this.context.getAwsRequestId()).thenReturn("mocked request id");

		this.app.handleRequest(this.event, this.context);

		assertAll(
				() -> verify(this.context, times(1)).getAwsRequestId(),
				() -> verify(this.event, times(1)).getRecords(),
				() -> {});
	}

}
