package com.ascelion.guice.module;

import static com.ascelion.guice.GuiceUtils.isSingleton;
import static com.ascelion.guice.GuiceUtils.isVetoed;

import com.ascelion.guice.GuiceScan;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

import java.util.Set;

import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import jakarta.inject.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

@SuppressWarnings({ "rawtypes", "unchecked" })
@Slf4j
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class AutoBindBeansModule extends AbstractModule {

	@Inject
	@GuiceScan
	private ScanResult scanned;

	@Inject
	@Named("beanTypes")
	private Set<String> beanTypes;

	@Override
	protected void configure() {
		final var classes = this.scanned.getAllClasses()
				.filter(it -> !this.beanTypes.contains(it.getName()))
				.filter(ci -> !isVetoed(ci))
				.filter(ClassInfo::isStandardClass);

		for (final var ci : classes) {
			// TODO: what about custom scopes?
			final Class target = ci.loadClass();

			if (isSingleton(target)) {
				LOG.debug("Binding singleton {} to iself", target.getName());

				bind(target).in(Scopes.SINGLETON);
			} else {
				LOG.debug("Binding {} to iself", target.getName());

				bind(target);
			}

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
}
