package com.ascelion.guice.module;

import static com.ascelion.guice.ModulePriorities.MODULE_PRIORITY_OFFSET;
import static com.ascelion.guice.ModulePriorities.PROVIDER_MODULE_PRIORITY;

import com.ascelion.guice.internal.BootstrapContext;
import com.ascelion.guice.internal.MemberProducer;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;

import java.lang.reflect.Method;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Priority(PROVIDER_MODULE_PRIORITY + MODULE_PRIORITY_OFFSET)
@SuppressWarnings({ "rawtypes" })
@Slf4j
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class AutoBindMethodProducersModule extends AbstractModule {

	@Inject
	private BootstrapContext context;

	@Override
	protected void configure() {
		final var classes = this.context.getScanned()
				.getClassesWithMethodAnnotation(Produces.class)
				.filter(it -> !this.context.containsBean(it));

		for (final var ci : classes) {
			final Class source = ci.loadClass();

			for (final var mi : ci.getMethodInfo().filter(it -> it.hasAnnotation(Produces.class))) {
				final Method method = mi.loadClassAndGetMethod();
				final Class target = method.getReturnType();

				new MemberProducer(method, getProvider(source), getProvider(Injector.class))
						.bind(binder(), this.context.getScope(method, target));

				this.context.addBean(target);
			}
		}
	}
}
