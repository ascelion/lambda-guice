package com.ascelion.guice.module;

import static com.ascelion.guice.GuiceUtils.isSingleton;
import static java.lang.reflect.Modifier.isStatic;

import com.ascelion.guice.GuiceScan;
import com.google.inject.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.stream.Stream;

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

				if (isSingleton(target) || isSingleton(method)) {
					LOG.debug("Binding singleton {} to provider {}.{}", target.getName(), ci.getName(), mi.getName());

					bind(target).toProvider(producerMethod(source, method)).in(Scopes.SINGLETON);
				} else {
					LOG.debug("Binding {} to provider {}.{}", target.getName(), ci.getName(), mi.getName());

					bind(target).toProvider(producerMethod(source, method));
				}

				this.beanTypes.add(target.getName());
			}
		}
	}

	private Provider<?> producerMethod(Class<?> source, Method method) {
		final Provider sourceP = isStatic(method.getModifiers())
				? () -> null
				: getProvider(source);
		final Provider[] argsP = Stream.of(method.getParameterTypes()).map(this::getProvider).toArray(Provider[]::new);

		return () -> {
			method.setAccessible(true);

			try {
				return method.invoke(sourceP.get(), Stream.of(argsP).map(Provider::get).toArray());
			} catch (IllegalAccessException | InvocationTargetException e) {
				throw new ProvisionException("Cannot invoke producer method", e);
			}
		};
	}
}
