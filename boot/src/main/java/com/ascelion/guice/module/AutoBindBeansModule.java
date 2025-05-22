package com.ascelion.guice.module;

import static com.ascelion.guice.ModulePriorities.MODULE_PRIORITY_OFFSET;
import static com.ascelion.guice.ModulePriorities.PROVIDER_MODULE_PRIORITY;

import com.ascelion.guice.internal.BootstrapContext;
import com.google.inject.AbstractModule;
import com.google.inject.Scope;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Priority(PROVIDER_MODULE_PRIORITY + 3 * MODULE_PRIORITY_OFFSET)
@SuppressWarnings({ "rawtypes" })
@Slf4j
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class AutoBindBeansModule extends AbstractModule {
	@Inject
	private BootstrapContext context;

	@Override
	protected void configure() {
		final var classes = this.context.getAllStandardClasses();

		for (final var ci : classes) {
			final Class target = ci.loadClass();
			final Scope scope = this.context.getScope(target);

			LOG.debug("Binding {} to itself in scope {}", target.getName(), scope);

			bind(target).in(scope);

			if (ci.getInterfaces().size() != 1) {
				continue;
			}

			final var itf = ci.getInterfaces().get(0);
			final Class itfc = itf.loadClass();

			if (Provider.class.isAssignableFrom(itfc)) {
				continue;
			}
			if (itfc.getTypeParameters().length != 0) {
				continue;
			}

			LOG.debug("Binding {} to {} in scope {}", itfc.getName(), target.getName(), scope);

			bind(itfc).to(target).in(scope);
		}
	}
}
