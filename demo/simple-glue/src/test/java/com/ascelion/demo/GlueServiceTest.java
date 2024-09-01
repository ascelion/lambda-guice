package com.ascelion.demo;

import static com.amazonaws.services.lambda.runtime.serialization.events.LambdaEventSerializers.serializerFor;
import static java.lang.Thread.currentThread;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import com.amazonaws.services.lambda.runtime.Context;
import com.ascelion.demo.GlueService.Request;
import com.ascelion.guice.jupiter.GuiceBootExtension;
import com.ascelion.lambda.GuiceGlueHandler;

import java.io.*;
import java.math.BigDecimal;

import jakarta.enterprise.inject.Produces;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@ExtendWith(GuiceBootExtension.class)
@Slf4j
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
		final var operands = new BigDecimal[] { BigDecimal.valueOf(Math.PI), BigDecimal.valueOf(Math.E) };
		final var inputBuf = new ByteArrayOutputStream();

		serializerFor(Request.class, currentThread().getContextClassLoader()).toJson(new Request(operands), inputBuf);

		final var input = new ByteArrayInputStream(inputBuf.toByteArray());
		final var output = new ByteArrayOutputStream();

		LOG.info("{}", new String(output.toByteArray(), UTF_8));

		this.app.handleRequest(input, output, this.context);

		final BigDecimal result = serializerFor(BigDecimal.class, currentThread().getContextClassLoader())
				.fromJson(new ByteArrayInputStream(output.toByteArray()));

		assertThat(result).isEqualTo(operands[0].add(operands[1]));
	}

}
