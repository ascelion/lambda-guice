package com.ascelion.guice.module;

import com.ascelion.guice.internal.BootstrapContext;
import com.ascelion.guice.internal.MemberProducer;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;

import java.lang.reflect.Method;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

@SuppressWarnings({ "rawtypes", "unchecked" })
@Slf4j
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class AutoBindMethodProducersModule extends AbstractModule {

	@Inject
	private BootstrapContext context;

	@Override
	protected void configure() {
		final var classes = this.context.getScanned().getClassesWithMethodAnnotation(Produces.class)
				.filter(it -> !this.context.containsBean(it));

		for (final var ci : classes) {
			final Class source = ci.loadClass();

			for (final var mi : ci.getMethodInfo().filter(it -> it.hasAnnotation(Produces.class))) {
				final Method method = mi.loadClassAndGetMethod();
				final Class target = method.getReturnType();

				new MemberProducer(method, getProvider(source), getProvider(Injector.class))
						.bind(binder());

				this.context.addBean(target);
			}
		}
	}
}
