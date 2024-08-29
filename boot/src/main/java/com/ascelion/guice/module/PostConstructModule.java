package com.ascelion.guice.module;

import static com.ascelion.guice.ModulePriorities.MODULE_PRIORITY_OFFSET;
import static com.ascelion.guice.ModulePriorities.SCOPE_MODULE_PRIORITY;
import static java.lang.System.identityHashCode;
import static org.apache.commons.lang3.reflect.MethodUtils.getMethodsListWithAnnotation;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Priority(SCOPE_MODULE_PRIORITY - MODULE_PRIORITY_OFFSET)
@SuppressWarnings({ "rawtypes" })
@Slf4j
public class PostConstructModule implements Module {
	static class PostConstructInjectionListener<T> implements InjectionListener<T> {
		private final Class<T> rawType;
		private final List<Method> methods;

		@SuppressWarnings("java:S3011")
		PostConstructInjectionListener(Class<T> type) {
			this.rawType = type;

			this.methods = getMethodsListWithAnnotation(this.rawType, PostConstruct.class, true, true);
		}

		@Override
		public void afterInjection(T injectee) {
			LOG.trace("Invoking post-construct method(s) on @{} of type {}", identityHashCode(injectee), this.rawType);

			this.methods.forEach(m -> invoke(m, injectee));
		}

		@SneakyThrows
		private void invoke(Method method, T injectee) {
			method.setAccessible(true);
			method.invoke(injectee);
		}
	}

	@SuppressWarnings("rawtypes")
	private final Map<Class, InjectionListener> listeners = new ConcurrentHashMap<>();

	@Override
	public void configure(Binder bnd) {
		LOG.debug("Configuring post-construct listeners");

		bnd.bindListener(Matchers.any(), this::hear);
	}

	private <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
		final var rawType = type.getRawType();
		final var pcil = listener(rawType);

		if (pcil.methods.isEmpty()) {
			return;
		}

		LOG.trace("Registering listener for {}", rawType.getName());

		encounter.register(pcil);
	}

	private <T> PostConstructInjectionListener<T> listener(Class<T> type) {
		return (PostConstructInjectionListener<T>) this.listeners
				.computeIfAbsent(type, PostConstructInjectionListener::new);
	}
}
