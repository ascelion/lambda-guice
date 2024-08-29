package com.ascelion.lambda;

import static com.amazonaws.services.lambda.runtime.serialization.events.LambdaEventSerializers.serializerFor;
import static com.ascelion.guice.GuiceBoot.guiceInit;
import static java.lang.Thread.currentThread;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.IOUtils.resourceToString;
import static org.apache.commons.lang3.StringUtils.trimToNull;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.lambda.runtime.api.client.HandlerInfo;
import com.amazonaws.services.lambda.runtime.api.client.HandlerInfo.InvalidHandlerException;
import com.amazonaws.services.lambda.runtime.serialization.PojoSerializer;
import com.ascelion.guice.request.RequestScope;
import com.google.inject.*;
import com.google.inject.Module;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import jakarta.enterprise.inject.Vetoed;

@Vetoed
public final class GuiceGlueHandler implements RequestStreamHandler {

	public static final String GUICE_GLUE_HANDLER_ENV = "GUICE_GLUE_HANDLER";
	public static final String GUICE_GLUE_HANDLER_PRP = GUICE_GLUE_HANDLER_ENV.toLowerCase().replace('_', '.');

	private static final String RESOURCE_NAME = "META-INF/services/"
			+ GuiceGlueHandler.class.getName().replace('.', '/');

	private final Object handler;
	private final Method method;
	private final PojoSerializer serializer;

	@Inject
	private Injector injector;
	@Inject
	private RequestScope scope;

	public GuiceGlueHandler() {
		this(handlerName());
	}

	public GuiceGlueHandler(String handler, Module... modules) {
		final var hi = parseHandlerInfo(handler);

		guiceInit(hi.clazz).modules(modules).boot(this);

		this.handler = this.injector.getInstance(hi.clazz);

		final List<Method> methods = Stream.of(hi.clazz.getDeclaredMethods())
				.filter(m -> !m.isBridge() && !m.isSynthetic())
				.filter(m -> m.getName().equals(hi.methodName))
				.toList();

		switch (methods.size()) {
			case 1:
			break;

			case 0: {
				throw new GuiceGlueException("No such method " + handler);
			}

			default:
				throw new GuiceGlueException("Expecting only one method " + handler);
		}

		this.method = methods.get(0);

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

			final Object response = this.scope.activate()
					.seed(LambdaRequest.class, request)
					.proceed(this::call);

			if (response != null) {
				this.serializer.toJson(response, output);
			}
		} catch (final IOException e) {
			throw e;
		} catch (final Exception e) {
			throw new GuiceGlueException(RESOURCE_NAME, e);
		}
	}

	private Object call() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		final Object[] args = Stream.of(this.method.getParameterTypes())
				.map(this.injector::getProvider)
				.map(Provider::get)
				.toArray();

		return this.method.invoke(this.handler, args);
	}

	private static String handlerName() {
		return Stream.of(
				trimToNull(System.getenv(GUICE_GLUE_HANDLER_ENV)),
				trimToNull(System.getProperty(GUICE_GLUE_HANDLER_PRP)))
				.filter(Objects::nonNull)
				.findFirst()
				.orElseGet(GuiceGlueHandler::readFromServices);
	}

	private static HandlerInfo parseHandlerInfo(final String handler) {
		final HandlerInfo hi;

		try {
			hi = HandlerInfo.fromString(handler, currentThread().getContextClassLoader());
		} catch (ClassNotFoundException | NoClassDefFoundError | InvalidHandlerException e) {
			throw new GuiceGlueException("Cannot use handler " + handler, e);
		}

		if (hi.methodName == null) {
			if (Callable.class.isAssignableFrom(hi.clazz)) {
				return new HandlerInfo(hi.clazz, "call");
			}
			if (Runnable.class.isAssignableFrom(hi.clazz)) {
				return new HandlerInfo(hi.clazz, "run");
			}

			throw new GuiceGlueException("Method name must be specified");
		}

		return hi;
	}

	private static String readFromServices() {
		try {
			return resourceToString(RESOURCE_NAME, UTF_8, currentThread().getContextClassLoader());
		} catch (final IOException e) {
			throw new GuiceGlueException("Cannot read " + RESOURCE_NAME, e);
		}
	}
}
