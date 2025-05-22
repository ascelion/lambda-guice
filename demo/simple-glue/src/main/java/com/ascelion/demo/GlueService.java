package com.ascelion.demo;

import static com.amazonaws.services.lambda.runtime.serialization.events.LambdaEventSerializers.serializerFor;
import static java.lang.Thread.currentThread;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.reducing;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.serialization.PojoSerializer;
import com.ascelion.guice.request.RequestScoped;

import java.math.BigDecimal;
import java.util.stream.Stream;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class GlueService {
	@RequestScoped
	@RequiredArgsConstructor
	@Getter
	@ToString
	public static class Request {
		private final BigDecimal[] operands;

		public Request() {
			this(new BigDecimal[0]);
		}
	}

	static final PojoSerializer<Request> serializer = serializerFor(
			Request.class, currentThread().getContextClassLoader());

	@Produces
	static Request toRequest(APIGatewayProxyRequestEvent event) {
		return ofNullable(event)
				.map(APIGatewayProxyRequestEvent::getBody)
				.map(StringUtils::trimToNull)
				.map(serializer::fromJson)
				.orElseGet(Request::new);
	}

	@Inject
	private Request request;

	BigDecimal proceed() {
		LOG.info("Got {}", this.request);

		return Stream.of(this.request.getOperands()).collect(reducing(BigDecimal.ZERO, BigDecimal::add));
	}
}
