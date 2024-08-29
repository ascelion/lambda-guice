package com.ascelion.lambda;

import static com.amazonaws.services.lambda.runtime.serialization.events.LambdaEventSerializers.serializerFor;
import static com.ascelion.guice.GuiceBoot.guiceInit;
import static java.lang.Thread.currentThread;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.IOUtils.write;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.lambda.runtime.serialization.PojoSerializer;
import com.ascelion.guice.request.RequestScope;
import com.google.inject.*;
import com.google.inject.Module;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.stream.Stream;

import jakarta.enterprise.inject.Vetoed;
import lombok.extern.slf4j.Slf4j;

@Vetoed
@Slf4j
public final class GuiceGlueHandler implements RequestStreamHandler {

	public static final String ENTRY_POINT_PROPERTY = "guice.glue.entry-point";

	private final Object handler;
	private final Method method;
	private final PojoSerializer serializer;

	@Inject
	private Injector injector;
	@Inject
	private RequestScope scope;

	public GuiceGlueHandler() {
		this(null);
	}

	public GuiceGlueHandler(String handler, Module... modules) {
		final var ept = EntryPoint.build(handler);

		LOG.info("Using entry point {}", ept);

		guiceInit(ept.clazz).modules(modules).boot(this);

		this.handler = this.injector.getInstance(ept.clazz);
		this.method = ept.method;

		final var returnType = this.method.getReturnType();

		if (returnType == String.class) {
			this.serializer = new StringSerializer();
		} else {
			this.serializer = serializerFor(returnType, currentThread().getContextClassLoader());
		}
	}

	@Override
	public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
		try {
			final var request = new LambdaRequestImpl(input, output, context);
			final var response = this.scope.activate()
					.seed(LambdaRequest.class, request)
					.proceed(this::call);

			if (response instanceof final String s) {
				write(s, output, UTF_8);
			} else if (response != null) {
				this.serializer.toJson(response, output);
			}
		} catch (final IOException e) {
			throw e;
		} catch (final Exception e) {
			throw new GuiceGlueException("Invocation failure", e);
		}
	}

	private Object call() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		final Object[] args = Stream.of(this.method.getParameterTypes())
				.map(this.injector::getProvider)
				.map(Provider::get)
				.toArray();

		this.method.setAccessible(true);

		return this.method.invoke(this.handler, args);
	}
}
