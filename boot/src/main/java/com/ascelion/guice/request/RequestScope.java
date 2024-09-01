package com.ascelion.guice.request;

import static java.lang.Thread.currentThread;
import static java.util.Comparator.comparing;

import com.google.inject.*;

import java.lang.reflect.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import jakarta.enterprise.inject.Vetoed;
import jakarta.inject.Provider;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatchers;

@RequestScoped
@Slf4j
@RequiredArgsConstructor
public class RequestScope implements Scope {
	private static final ThreadLocal<Request> REQUESTS = new InheritableThreadLocal<>() {
		@Override
		protected Request childValue(Request parent) {
			return parent != null ? new Request(parent) : null;
		};
	};

	public static class Request {
		final Map<Key<?>, Object> values = new HashMap<>();

		Request() {
		}

		Request(@NonNull Request parent) {
			this.values.putAll(parent.values);
		}

		public <T> Request seed(Class<? extends T> type, T instance) {
			return seed(Key.get(type), instance);
		}

		public <T> Request seed(Key<? extends T> type, T o) {
			this.values.put(type, o);

			return this;
		}

		public void proceed(Runnable action) {
			try {
				action.run();
			} finally {
				REQUESTS.remove();
			}
		}

		public <T> T proceed(Callable<T> action) throws Exception {
			try {
				return action.call();
			} finally {
				REQUESTS.remove();
			}
		}

		<T> T get(Key<T> key, com.google.inject.Provider<T> unscoped) {
			if (!this.values.containsKey(key)) {
				LOG.trace("Unscoped {}", key);

				this.values.put(key, unscoped.get());
			}

			return (T) this.values.get(key);
		}
	}

	private final Map<Key<?>, Object> proxies = new ConcurrentHashMap<>();

	private final Provider<Injector> injectorP;

	public Request activate() {
		var request = REQUESTS.get();

		if (request != null) {
			throw new IllegalStateException("Nested scopes not allowed");
		}

		request = new Request();

		REQUESTS.set(request);

		return request;
	}

	@Override
	public String toString() {
		return "Scopes.REQUEST";
	}

	@Override
	public <T> com.google.inject.Provider<T> scope(Key<T> key, com.google.inject.Provider<T> unscoped) {
		return () -> {
			final var request = REQUESTS.get();

			if (request == null) {
				return (T) this.proxies.computeIfAbsent(key, k -> proxy((Key<T>) k, unscoped));
			}

			return request.get(key, unscoped);
		};
	}

	private <T> T proxy(Key<T> key, com.google.inject.Provider<T> unscoped) {
		final var type = key.getTypeLiteral().getRawType();
		final var unloaded = new ByteBuddy()
				.subclass(type)
				.name(type.getName() + "$Proxy")
				.annotateType(Vetoed.Literal.INSTANCE)
				.method(ElementMatchers.isDeclaredBy(type))
				.intercept(InvocationHandlerAdapter.of((proxy, method, args) -> scoped(key, method, args, unscoped)))
				.make();

//		try {
//			final var path = Path.of("build/generated/proxy-classes",
//					unloaded.getTypeDescription().getInternalName() + ".class");
//
//			path.getParent().toFile().mkdirs();
//
//			Files.write(path, unloaded.getBytes());
//		} catch (final IOException e) {
//			e.printStackTrace();
//		}

		final var loaded = unloaded
				.load(currentThread().getContextClassLoader())
				.getLoaded();

		final var types = Stream.of(type.getDeclaredConstructors())
				.filter(c -> c.getParameterCount() == 0
						|| c.getParameterCount() > 0 && (c.isAnnotationPresent(jakarta.inject.Inject.class)
								|| c.isAnnotationPresent(com.google.inject.Inject.class)))
				.sorted(comparing(Constructor<?>::getParameterCount).reversed())
				.map(Constructor::getParameterTypes)
				.findFirst()
				.orElse(new Class[0]);

		return (T) instantiate(loaded, types);
	}

	private <T> T instantiate(Class<T> loaded, java.lang.Class<?>[] types) {
		final Constructor<T> init;

		try {
			init = loaded.getDeclaredConstructor(types);
		} catch (final NoSuchMethodException e) {
			throw new ProvisionException("Cannot find any suitable constructor", e);
		}

		final var args = Stream.of(types)
				.map(this::getProvider)
				.map(Provider::get)
				.toArray();

		init.setAccessible(true);

		try {
			return init.newInstance(args);
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
			LOG.atError()
					.setCause(e)
					.addArgument(loaded.getName())
					.log("Cannot instantiate proxy type {}", e);

			throw new ProvisionException("Cannot invoke " + init);
		}
	}

	private <T> Provider<T> getProvider(Class<T> type) {
		return this.injectorP.get().getProvider(type);
	}

	private <T> Object scoped(Key<T> key, Method method, Object[] args, com.google.inject.Provider<T> unscoped)
			throws Throwable {
		final var request = REQUESTS.get();

		if (request == null) {
			throw new IllegalStateException("Scope is not active");
		}

		final var instance = request.get(key, unscoped);

		return method.invoke(instance, args);
	}

}
