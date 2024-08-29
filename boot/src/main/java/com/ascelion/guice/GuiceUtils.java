package com.ascelion.guice;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import io.github.classgraph.ClassInfo;
import jakarta.enterprise.inject.Vetoed;

public final class GuiceUtils {
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
		return ci.hasAnnotation(Vetoed.class);
	}

	public static boolean isAnnotatedWith(AnnotatedElement annotated, Class<? extends Annotation>... types) {
		return isAnnotatedWith(annotated, List.of(types));
	}

	public static boolean isAnnotatedWith(AnnotatedElement annotated, Collection<Class<? extends Annotation>> types) {
		return Stream.of(annotated.getAnnotations()).map(Annotation::annotationType).anyMatch(types::contains);
	}

	public static boolean isSingleton(AnnotatedElement annotated) {
		return isAnnotatedWith(annotated, jakarta.inject.Singleton.class, com.google.inject.Singleton.class);
	}
}
