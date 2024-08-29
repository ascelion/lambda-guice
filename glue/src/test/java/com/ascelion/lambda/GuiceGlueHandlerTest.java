package com.ascelion.lambda;

import static com.amazonaws.services.lambda.runtime.serialization.events.LambdaEventSerializers.serializerFor;
import static java.lang.Thread.currentThread;
import static java.nio.charset.StandardCharsets.UTF_8;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.*;
import static uk.org.webcompere.systemstubs.SystemStubs.withEnvironmentVariable;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.serialization.PojoSerializer;
import com.ascelion.guice.request.RequestScoped;

import java.io.*;
import java.util.List;
import java.util.concurrent.Callable;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.NullInputStream;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

@ExtendWith(MockitoExtension.class)
@ExtendWith(SystemStubsExtension.class)
class GuiceGlueHandlerTest {

	static class HandlerCallable implements Callable<String> {
		@Override
		public String call() {
			return "HIHI";
		}
	}

	static class HandlerRunnable implements Runnable {
		interface Service {
			void run();
		}

		@Inject
		Service service;

		@Override
		public void run() {
			this.service.run();
		}
	}

	static class HandlerMethod {
		static class Service {
			@Inject
			LambdaRequest request;

			void run() throws IOException {
				IOUtils.write("HOHO", this.request.getOutput(), UTF_8);
			}
		}

		@Inject
		Service service;

		void invoke() throws IOException {
			this.service.run();
		}
	}

	static class HandlerWithRequestResponse {
		@RequestScoped
		@RequiredArgsConstructor
		@Getter
		public static class Request {
			private final String value;

			public Request() {
				this(null);
			}
		}

		@RequiredArgsConstructor
		@Getter
		public static class Response {
			private final String value;
		}

		static final PojoSerializer<Request> serializer = serializerFor(Request.class,
				currentThread().getContextClassLoader());

		@Inject
		Request request;

		Response invoke() {
			return new Response(this.request.getValue());
		}

		@Produces
		static Request toRequest(LambdaRequest request) {
			return serializer.fromJson(request.getInput());
		}
	}

	static class HandlerWithEvent {
		static final PojoSerializer<SQSEvent> serializer = serializerFor(SQSEvent.class,
				currentThread().getContextClassLoader());

		@Inject
		SQSEvent request;

		SQSEvent invoke() {
			final var buf = new ByteArrayOutputStream();

			serializer.toJson(this.request, buf);

			return serializer.fromJson(new ByteArrayInputStream(buf.toByteArray()));
		}
	}

	static class HandlerWithEventToRequest {
		@RequestScoped
		@RequiredArgsConstructor
		@Getter
		public static class Request {
			private final String value;

			public Request() {
				this(null);
			}
		}

		@RequiredArgsConstructor
		@Getter
		public static class Response {
			private final String region;
		}

		final PojoSerializer<SQSEvent> serializer = serializerFor(SQSEvent.class,
				currentThread().getContextClassLoader());

		@Inject
		Request request;

		Response invoke() {
			return new Response(this.request.getValue());
		}

		@Produces
		static Request toRequest6(SQSEvent event) {
			if (isEmpty(event.getRecords())) {
				return new Request();
			}

			return new Request(event.getRecords().get(0).getAwsRegion());
		}
	}

	static class HandlerWithEventToRequestAsParam {
		@RequestScoped
		@RequiredArgsConstructor
		@Getter
		public static class Request {
			private final String value;

			public Request() {
				this(null);
			}
		}

		@RequiredArgsConstructor
		@Getter
		public static class Response {
			private final String region;
		}

		final PojoSerializer<SQSEvent> serializer = serializerFor(SQSEvent.class,
				currentThread().getContextClassLoader());

		Response invoke(Request request) {
			return new Response(request.getValue());
		}

		@Produces
		static Request toRequest(SQSEvent event) {
			if (isEmpty(event.getRecords())) {
				return new Request();
			}

			return new Request(event.getRecords().get(0).getAwsRegion());
		}
	}

	private final ByteArrayOutputStream output = new ByteArrayOutputStream();

	@SystemStub
	private final EnvironmentVariables environ = withEnvironmentVariable(GuiceGlueHandler.GUICE_GLUE_HANDLER_ENV,
			HandlerCallable.class.getName());

	@Mock
	private Context context;

	@BeforeEach
	void setUp() {
		this.output.reset();
	}

	@Test
	void handlerCallable() {
		final var glue = new GuiceGlueHandler();

		assertThatNoException().isThrownBy(() -> glue.handleRequest(new NullInputStream(), this.output, this.context));

		final var text = StringUtils.toEncodedString(this.output.toByteArray(), UTF_8);

		assertThat(text).isEqualTo("HIHI");
	}

	@Test
	void handlerRunnable() {
		final var service = mock(HandlerRunnable.Service.class);
		final var glue = new GuiceGlueHandler(HandlerRunnable.class.getName(),
				bnd -> bnd.bind(HandlerRunnable.Service.class).toInstance(service));

		assertThatNoException().isThrownBy(() -> glue.handleRequest(new NullInputStream(), this.output, this.context));

		assertThat(this.output.toByteArray()).isEmpty();
		verify(service, times(1)).run();
	}

	@Test
	void handlerMethod() {
		final var glue = new GuiceGlueHandler(HandlerMethod.class.getName() + "::invoke");

		assertThatNoException().isThrownBy(() -> glue.handleRequest(new NullInputStream(), this.output, this.context));

		final var text = StringUtils.toEncodedString(this.output.toByteArray(), UTF_8);

		assertThat(text).isEqualTo("HOHO");
	}

	@Test
	void handlerWithRequestResponse() {
		final var glue = new GuiceGlueHandler(HandlerWithRequestResponse.class.getName() + "::invoke");
		final var iJson = new ByteArrayInputStream("""
				{
					"value": "HIHI"
				}
				""".getBytes(UTF_8));

		assertThatNoException().isThrownBy(() -> glue.handleRequest(iJson, this.output, this.context));

		final var oJson = StringUtils.toEncodedString(this.output.toByteArray(), UTF_8);

		assertThatJson(oJson).isObject().containsEntry("value", "HIHI");
	}

	@Test
	void handlerWithEvent() {
		final var glue = new GuiceGlueHandler(HandlerWithEvent.class.getName() + "::invoke");
		final var event = new SQSEvent();
		final var message = new SQSEvent.SQSMessage();

		message.setAwsRegion("eu-west-1");

		event.setRecords(List.of(message));

		final var inputBuf = new ByteArrayOutputStream();

		serializerFor(SQSEvent.class, currentThread().getContextClassLoader()).toJson(event, inputBuf);

		final var input = new ByteArrayInputStream(inputBuf.toByteArray());

		assertThatNoException().isThrownBy(() -> glue.handleRequest(input, this.output, this.context));

		final var iJson = StringUtils.toEncodedString(inputBuf.toByteArray(), UTF_8);
		final var oJson = StringUtils.toEncodedString(this.output.toByteArray(), UTF_8);

		assertThatJson(iJson).isEqualTo(oJson);
	}

	@Test
	void handlerWithEventToRequest() {
		final var glue = new GuiceGlueHandler(HandlerWithEventToRequest.class.getName() + "::invoke");
		final var event = new SQSEvent();
		final var message = new SQSEvent.SQSMessage();

		message.setAwsRegion("eu-east-1");

		event.setRecords(List.of(message));

		final var inputBuf = new ByteArrayOutputStream();

		serializerFor(SQSEvent.class, currentThread().getContextClassLoader()).toJson(event, inputBuf);

		final var input = new ByteArrayInputStream(inputBuf.toByteArray());

		assertThatNoException().isThrownBy(() -> glue.handleRequest(input, this.output, this.context));

		final var oJson = StringUtils.toEncodedString(this.output.toByteArray(), UTF_8);

		assertThatJson(oJson).isObject().containsEntry("region", "eu-east-1");
	}

	@Test
	void handlerWithEventToRequestAsParam() {
		final var glue = new GuiceGlueHandler(HandlerWithEventToRequestAsParam.class.getName() + "::invoke");
		final var event = new SQSEvent();
		final var message = new SQSEvent.SQSMessage();

		message.setAwsRegion("eu-east-1");

		event.setRecords(List.of(message));

		final var inputBuf = new ByteArrayOutputStream();

		serializerFor(SQSEvent.class, currentThread().getContextClassLoader()).toJson(event, inputBuf);

		final var input = new ByteArrayInputStream(inputBuf.toByteArray());

		assertThatNoException().isThrownBy(() -> glue.handleRequest(input, this.output, this.context));

		final var oJson = StringUtils.toEncodedString(this.output.toByteArray(), UTF_8);

		assertThatJson(oJson).isObject().containsEntry("region", "eu-east-1");
	}
}
