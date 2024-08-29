package com.ascelion.guice.module;

import static com.ascelion.guice.ModulePriorities.MODULE_PRIORITY_OFFSET;
import static com.ascelion.guice.ModulePriorities.PROVIDER_MODULE_PRIORITY;
import static com.ascelion.guice.internal.GuiceUtils.isSingleton;
import static com.ascelion.guice.internal.GuiceUtils.isVetoed;

import com.ascelion.guice.internal.BootstrapContext;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

import io.github.classgraph.ClassInfo;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Priority(PROVIDER_MODULE_PRIORITY + 2 * MODULE_PRIORITY_OFFSET)
@SuppressWarnings({ "rawtypes" })
@Slf4j
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class AutoBindBeansModule extends AbstractModule {

	@Inject
	private BootstrapContext context;

	@Override
	protected void configure() {
		final var classes = this.context.getScanned().getAllClasses()
				.filter(it -> !this.context.containsBean(it))
				.filter(ClassInfo::isStandardClass)
				.filter(ci -> !isVetoed(ci))
				.filter(this::hasEligibleConstructor);

		for (final var ci : classes) {
			// TODO: what about custom scopes?
			final Class target = ci.loadClass();

			if (isSingleton(target)) {
				LOG.debug("Binding singleton {} to itself", target.getName());

				bind(target).in(Scopes.SINGLETON);
			} else {
				LOG.debug("Binding {} to itself", target.getName());

				bind(target);
			}

			this.context.addBean(target);

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

			if (isSingleton(target)) {
				LOG.debug("Binding {} to singleton {}", itfc.getName(), target.getName());

				bind(itfc).to(target).in(Scopes.SINGLETON);
			} else {
				LOG.debug("Binding {} to {}", itfc.getName(), target.getName());

				bind(itfc).to(target);
			}
		}
	}

	private boolean hasEligibleConstructor(ClassInfo ci) {
		return hasSimpleConstructor(ci) || hasInjectConstructor(ci);
	}

	private boolean hasSimpleConstructor(ClassInfo ci) {
		return ci.getDeclaredConstructorInfo()
				.filter(mi -> mi.getParameterInfo().length == 0)
				.size() == 1;
	}

	private boolean hasInjectConstructor(ClassInfo ci) {
		return ci.getDeclaredConstructorInfo()
				.filter(mi -> mi.hasAnnotation(jakarta.inject.Inject.class)
						|| mi.hasAnnotation(com.google.inject.Inject.class))
				.size() == 1;
	}
}
