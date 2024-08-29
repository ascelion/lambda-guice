package com.ascelion.guice.module;

import static com.ascelion.guice.ModulePriorities.SCOPE_MODULE_PRIORITY;
import static com.ascelion.guice.internal.GuiceUtils.scopeAnnotation;
import static java.util.Comparator.comparing;

import com.ascelion.guice.internal.BootstrapContext;
import com.google.inject.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.stream.Stream;

import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.CreationException;
import jakarta.inject.Inject;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Priority(SCOPE_MODULE_PRIORITY)
@Slf4j
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class AutoBindScopeModule extends AbstractModule {

	@Inject
	private BootstrapContext context;

	@Override
	protected void configure() {
		final ClassInfoList classes = this.context.getScanned()
				.getClassesImplementing(Scope.class);

		for (final ClassInfo ci : classes) {
			final Class<Scope> cl = ci.loadClass(Scope.class);
			final var sa = scopeAnnotation(cl);

			if (sa.isPresent()) {
				bindScope(sa.get(), cl);
			} else {
				LOG.warn("Skipping auto registration of scope " + ci.getName());
			}
		}
	}

	private void bindScope(Class<? extends Annotation> annotation, Class<Scope> type) {
		final Scope scope = instantiate(type);

		LOG.debug("Binding @{} to scope {}", annotation.getName(), scope);
		bindScope(annotation, scope);

		LOG.debug("Binding {} to itself", type.getName());
		bind(type).toInstance(scope);

		this.context.addBean(type);
		this.context.addScope(scope, annotation);
	}

	private Scope instantiate(Class<Scope> type) {
		final Constructor<Scope> ct = (Constructor<Scope>) Stream.of(type.getDeclaredConstructors())
				.filter(c -> c.getParameterCount() == 0
						|| c.getParameterCount() == 1 && isInjectSupplier(c.getGenericParameterTypes()[0]))
				.sorted(comparing(Constructor<?>::getParameterCount).reversed())
				.findFirst()
				.orElseThrow();

		try {
			return ct.newInstance(
					ct.getParameterCount() == 0 ? new Object[0] : new Object[] { getProvider(Injector.class) });
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
			LOG.atError()
					.setCause(e)
					.addArgument(type.getName())
					.log("Cannot instantiate scope {}", e);

			throw new CreationException("Cannot instantiate scope " + type.getName(), e);
		}
	}

	private boolean isInjectSupplier(Type type) {
		if ((type instanceof final ParameterizedType pt) && (pt.getActualTypeArguments()[0] == Injector.class)) {
			return true;
		}

		return false;
	}
}
