package com.ascelion.guice.module;

import static com.ascelion.guice.ModulePriorities.PROVIDER_MODULE_PRIORITY;

import com.ascelion.guice.internal.BootstrapContext;
import com.ascelion.guice.internal.MemberProducer;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.CreationException;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Priority(PROVIDER_MODULE_PRIORITY)
@SuppressWarnings({ "rawtypes" })
@Slf4j
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class AutoBindClassProvidersModule extends AbstractModule {

	@Inject
	private BootstrapContext context;

	@Override
	protected void configure() {
		final var classes = this.context.getScanned().getClassesImplementing(Provider.class)
				.filter(it -> !this.context.containsBean(it));

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

			new MemberProducer(method, getProvider(source), getProvider(Injector.class))
					.bind(binder());

			this.context.addBean(target);
		}
	}
}
