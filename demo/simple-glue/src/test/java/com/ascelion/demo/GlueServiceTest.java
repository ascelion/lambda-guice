package com.ascelion.demo;

import static org.apache.commons.io.IOUtils.resourceToByteArray;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.*;

import com.amazonaws.services.lambda.runtime.Context;
import com.ascelion.guice.jupiter.GuiceBootExtension;
import com.ascelion.lambda.GuiceGlueHandler;

import java.io.*;

import jakarta.enterprise.inject.Produces;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@ExtendWith(GuiceBootExtension.class)
class GlueServiceTest {

	@Mock
	@Produces
	Context context;

	GuiceGlueHandler app;

	@BeforeEach
	void beforeEach() {
		this.app = new GuiceGlueHandler();
	}

	@Test
	void run() throws IOException {
		when(this.context.getAwsRequestId()).thenReturn("mocked request id");

		final var input = new ByteArrayInputStream(resourceToByteArray("/sqs-event.json"));
		final var output = new ByteArrayOutputStream();

		this.app.handleRequest(input, output, this.context);

		assertAll(
				() -> verify(this.context, times(1)).getAwsRequestId(),
				() -> {});
	}

}
