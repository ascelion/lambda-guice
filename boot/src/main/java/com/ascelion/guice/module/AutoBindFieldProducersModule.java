package com.ascelion.guice.module;

import static com.ascelion.guice.GuiceUtils.isSingleton;
import static java.lang.reflect.Modifier.isStatic;

import com.ascelion.guice.GuiceScan;
import com.google.inject.*;

import java.lang.reflect.Field;
import java.util.Set;

import io.github.classgraph.ScanResult;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

@SuppressWarnings({ "rawtypes", "unchecked" })
@Slf4j
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class AutoBindFieldProducersModule extends AbstractModule {

	@Inject
	@GuiceScan
	private ScanResult scanned;

	@Inject
	@Named("beanTypes")
	private Set<String> beanTypes;

	@Override
	protected void configure() {
		final var classes = this.scanned.getClassesWithFieldAnnotation(Produces.class)
				.filter(it -> !this.beanTypes.contains(it.getName()));

		for (final var ci : classes) {
			final Class source = ci.loadClass();

			for (final var fi : ci.getFieldInfo().filter(it -> it.hasAnnotation(Produces.class))) {
				final Field field = fi.loadClassAndGetField();
				final Class target = field.getType();

				if (isSingleton(target) || isSingleton(field)) {
					LOG.debug("Binding singleton {} to provider {}.{}", target.getName(), ci.getName(), fi.getName());

					bind(target).toProvider(readProducerField(source, field)).in(Scopes.SINGLETON);
				} else {
					LOG.debug("Binding {} to provider {}.{}", target.getName(), ci.getName(), fi.getName());

					bind(target).toProvider(readProducerField(source, field));
				}

				this.beanTypes.add(target.getName());
			}
		}
	}

	private Provider<?> readProducerField(Class<?> sourceType, Field field) {
		final Provider sourceP = isStatic(field.getModifiers())
				? () -> null
				: getProvider(sourceType);

		return () -> {
			field.setAccessible(true);

			try {
				return field.get(sourceP.get());
			} catch (IllegalArgumentException | IllegalAccessException e) {
				throw new ProvisionException("Cannot read producer field", e);
			}
		};
	}
}
