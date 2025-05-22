package com.ascelion.guice.internal;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.trimToNull;

import com.google.inject.BindingAnnotation;
import com.google.inject.ScopeAnnotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import io.github.classgraph.ClassInfo;
import jakarta.enterprise.inject.Vetoed;
import jakarta.inject.Qualifier;
import lombok.NonNull;

public final class GuiceUtils {
	public static final String GENERATED_OUTPUT_DIRECTORY_PARAM = "generated.output.directory";

	private static final Class<? extends Annotation>[] SINGLETONS = new Class[] {
			jakarta.inject.Singleton.class,
			com.google.inject.Singleton.class,
	};

	private GuiceUtils() {
		throw new UnsupportedOperationException();
	}

	public static Class<? extends Annotation>[] singletons() {
		return SINGLETONS.clone();
	}

	public static boolean isSingleton(ClassInfo ci) {
		return ci.hasAnnotation(jakarta.inject.Singleton.class)
				|| ci.hasAnnotation(com.google.inject.Singleton.class);
	}

	public static boolean isVetoed(ClassInfo ci) {
		return ci.hasAnnotation(Vetoed.class)
				|| ci.isInnerClass() && ci.getOuterClasses().stream().anyMatch(si -> si.hasAnnotation(Vetoed.class));
	}

	public static boolean isAnnotatedWith(AnnotatedElement annotated, Class<? extends Annotation>... types) {
		return isAnnotatedWith(annotated, List.of(types));
	}

	public static boolean isAnnotatedWith(AnnotatedElement annotated, Collection<Class<? extends Annotation>> types) {
		return Stream.of(annotated.getAnnotations()).map(Annotation::annotationType).anyMatch(types::contains);
	}

	public static Optional<Class<? extends Annotation>> scopeAnnotation(AnnotatedElement annotated) {
		if (annotated.isAnnotationPresent(jakarta.inject.Singleton.class)) {
			return Optional.of(com.google.inject.Singleton.class);
		}

		return (Optional) Stream.of(annotated.getAnnotations())
				.map(Annotation::annotationType)
				.filter(it -> it.isAnnotationPresent(ScopeAnnotation.class))
				.findFirst();
	}

	public static Annotation getBindingAnnotation(AnnotatedElement annotated) {
		for (final var a : annotated.getAnnotations()) {
			if (a.annotationType().isAnnotationPresent(BindingAnnotation.class)) {
				return a;
			}
			if (a.annotationType().isAnnotationPresent(Qualifier.class)) {
				return a;
			}
		}

		return null;
	}

	/**
	 * Retrieves a configuration value from {@code System.env} then from {@code System.properties}, following <a href=
	 * "https://docs.spring.io/spring-boot/reference/features/external-config.html#features.external-config.typesafe-configuration-properties.relaxed-binding.environment-variables">Spring
	 * binding conventions</a>.
	 * <p>
	 * When both environment and system property are defined, the environment variable wins.
	 * </p>
	 */
	public static Optional<String> externalConfiguration(String name) {
		return ofNullable(trimToNull(System.getenv(configurationEnvName(name))))
				.or(() -> ofNullable(trimToNull(System.getProperty(configurationPropertyName(name)))));
	}

	public static String configurationEnvName(@NonNull String name) {
		return name.toUpperCase().replace('.', '_').replace("-", "");
	}

	public static String configurationPropertyName(String name) {
		return name.toLowerCase().replace('_', '.');
	}

	private static final Pattern CAMEL_CASE = Pattern.compile("([0-9a-z])([A-Z])");

	public static String toKebabCase(String value) {
		return CAMEL_CASE.matcher(value).replaceAll("$1-$2").toLowerCase();
	}

	public static Optional<String> tryExternalConfiguration(String name) {
		if (name.startsWith("{") && name.startsWith("}")) {
			return externalConfiguration(name.substring(1, name.length() - 2));
		} else {
			return Optional.of(name);
		}
	}

}
