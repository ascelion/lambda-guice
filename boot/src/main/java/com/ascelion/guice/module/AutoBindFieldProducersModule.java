package com.ascelion.guice.module;

import static com.ascelion.guice.ModulePriorities.MODULE_PRIORITY_OFFSET;
import static com.ascelion.guice.ModulePriorities.PROVIDER_MODULE_PRIORITY;

import com.ascelion.guice.internal.BootstrapContext;
import com.ascelion.guice.internal.MemberProducer;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;

import java.lang.reflect.Field;

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
public class AutoBindFieldProducersModule extends AbstractModule {

	@Inject
	private BootstrapContext context;

	@Override
	protected void configure() {
		final var classes = this.context.getScanned().getClassesWithFieldAnnotation(Produces.class)
				.filter(it -> !this.context.containsBean(it));

		for (final var ci : classes) {
			final Class source = ci.loadClass();

			for (final var fi : ci.getFieldInfo().filter(it -> it.hasAnnotation(Produces.class))) {
				final Field field = fi.loadClassAndGetField();
				final Class target = field.getType();

				new MemberProducer(field, getProvider(source), getProvider(Injector.class))
						.bind(binder());

				this.context.addBean(target);
			}
		}
	}
}
