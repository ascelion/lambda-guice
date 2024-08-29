package com.ascelion.lambda;

import static com.ascelion.guice.internal.GuiceUtils.externalConfiguration;
import static com.ascelion.lambda.GuiceGlueHandler.ENTRY_POINT_PROPERTY;
import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.io.IOUtils.resourceToString;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
final class EntryPoint {

	static final String RESOURCE_NAME = "META-INF/services/" + GuiceGlueHandler.class.getName();

	private static final Method CALLABLE;
	private static final Method RUNNABLE;

	static {
		try {
			CALLABLE = Callable.class.getMethod("call");
			RUNNABLE = Runnable.class.getMethod("run");
		} catch (NoSuchMethodException | SecurityException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	static EntryPoint build(String handler) {
		final EntryPointBuilder bld = builder();

		parseHandlerName(bld, handler);

		return bld.build();
	}

	private static EntryPointBuilder builder() {
		return new EntryPointBuilder();
	}

	private static Method selectMethod(Class<?> clazz, String methodName, String handler) {
		List<Method> methods = Stream.of(clazz.getDeclaredMethods())
				.filter(m -> !m.isBridge() && !m.isSynthetic())
				.toList();

		if (methodName != null) {
			methods = methods.stream()
					.filter(m -> m.getName().equals(methodName))
					.toList();
		} else if (Callable.class.isAssignableFrom(clazz)) {
			return CALLABLE;
		} else if (Runnable.class.isAssignableFrom(clazz)) {
			return RUNNABLE;
		}

		if (methods.isEmpty()) {
			throw new GuiceGlueException("Cannot find any entry point for handler " + handler);
		}

		if (methods.size() > 1) {
			final var mList = methods;

			LOG.atError()
					.addArgument(handler)
					.addArgument(() -> mList.stream().map(Object::toString).collect(joining("\n")))
					.log("Too many candidates for handler (), found\n{}");

			throw new GuiceGlueException("Expecting only one method " + handler);
		}

		return methods.get(0);
	}

	private static String handlerName() {
		return externalConfiguration(ENTRY_POINT_PROPERTY).orElseGet(EntryPoint::readFromServices);
	}

	private static String readFromServices() {
		try {
			return trimToEmpty(resourceToString(RESOURCE_NAME, UTF_8, currentThread().getContextClassLoader()));
		} catch (final IOException e) {
			throw new GuiceGlueException("Cannot read " + RESOURCE_NAME, e);
		}
	}

	private static void parseHandlerName(EntryPointBuilder bld, String handler) {
		if (isEmpty(handler)) {
			handler = handlerName();
		}

		final int colonLoc = handler.lastIndexOf("::");
		final String className;
		String methodName;

		if (colonLoc < 0) {
			className = handler;
			methodName = null;
		} else {
			className = handler.substring(0, colonLoc);
			methodName = trimToEmpty(handler.substring(colonLoc + 2));
		}

		if (className.isEmpty()) {
			throw new GuiceGlueException("Missing class name: " + handler);
		}
		if (methodName != null && methodName.isEmpty()) {
			throw new GuiceGlueException("Missing method name: " + handler);
		}

		final Class<?> clazz;

		try {
			bld.clazz(currentThread().getContextClassLoader().loadClass(className));
		} catch (final ClassNotFoundException e) {
			throw new GuiceGlueException("Cannot instantiate handler " + handler, e);
		}

		bld.method(selectMethod(bld.clazz, methodName, handler));
	}

	final Class<?> clazz;
	final Method method;

	private final String description;

	@Builder
	private EntryPoint(Class<?> clazz, Method method) {
		this.clazz = clazz;
		this.method = method;
		this.description = format("%s::%s(%s)", this.clazz.getSimpleName(), this.method.getName(),
				Stream.of(this.method.getParameterTypes())
						.map(Class::getSimpleName)
						.collect(joining(",")));
	}

	@Override
	public String toString() {
		return this.description;
	}
}
