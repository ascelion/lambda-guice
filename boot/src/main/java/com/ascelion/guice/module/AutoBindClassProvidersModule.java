package com.ascelion.guice.module;

import static com.ascelion.guice.GuiceUtils.isSingleton;

import com.ascelion.guice.GuiceScan;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Set;

import io.github.classgraph.ScanResult;
import jakarta.enterprise.inject.CreationException;
import jakarta.inject.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

@SuppressWarnings({ "rawtypes", "unchecked" })
@Slf4j
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class AutoBindClassProvidersModule extends AbstractModule {

	@Inject
	@GuiceScan
	private ScanResult scanned;

	@Inject
	@Named("beanTypes")
	private Set<String> beanTypes;

	@Override
	protected void configure() {
		final var classes = this.scanned.getClassesImplementing(Provider.class)
				.filter(it -> !this.beanTypes.contains(it.getName()));

		for (final var ci : classes) {
			final Class source = ci.loadClass();
			final var ptype = (ParameterizedType) source.getGenericInterfaces()[0];
			final Class target = (Class) ptype.getActualTypeArguments()[0];

			final Method method;

			try {
				method = source.getMethod("get");
			} catch (NoSuchMethodException | SecurityException e) {
				LOG.atError().addArgument(source.getName()).setCause(e).log("Cannot find method {}#get()");

				throw new CreationException("Cannot find provider method", e);
			}

			if (isSingleton(target) || isSingleton(method)) {
				LOG.debug("Binding singleton {} to provider {}", target.getName(), source.getName());

				bind(target).toProvider(source).in(Scopes.SINGLETON);
			} else {
				LOG.debug("Binding {} to provider {}", target.getName(), source.getName());

				bind(target).toProvider(source);
			}

			this.beanTypes.add(target.getName());
		}
	}
}
