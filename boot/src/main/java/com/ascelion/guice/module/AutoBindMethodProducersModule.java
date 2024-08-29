package com.ascelion.guice.module;

import com.ascelion.guice.GuiceScan;
import com.ascelion.guice.internal.MemberProducer;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;

import java.lang.reflect.Method;
import java.util.Set;

import io.github.classgraph.ScanResult;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

@SuppressWarnings({ "rawtypes", "unchecked" })
@Slf4j
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class AutoBindMethodProducersModule extends AbstractModule {

	@Inject
	@GuiceScan
	private ScanResult scanned;

	@Inject
	@Named("beanTypes")
	private Set<String> beanTypes;

	@Override
	protected void configure() {
		final var classes = this.scanned.getClassesWithMethodAnnotation(Produces.class)
				.filter(it -> !this.beanTypes.contains(it.getName()));

		for (final var ci : classes) {
			final Class source = ci.loadClass();

			for (final var mi : ci.getMethodInfo().filter(it -> it.hasAnnotation(Produces.class))) {
				final Method method = mi.loadClassAndGetMethod();
				final Class target = method.getReturnType();

				new MemberProducer(method, getProvider(source), getProvider(Injector.class))
						.bind(binder());

				this.beanTypes.add(target.getName());
			}
		}
	}
}
