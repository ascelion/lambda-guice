package com.ascelion.guice.internal;

import static com.ascelion.guice.internal.GuiceUtils.getBindingAnnotation;
import static com.ascelion.guice.internal.GuiceUtils.isSingleton;
import static java.lang.reflect.Modifier.isStatic;

import com.google.inject.*;

import java.lang.reflect.*;
import java.util.function.Function;
import java.util.stream.Stream;

import jakarta.inject.Provider;
import lombok.extern.slf4j.Slf4j;

@SuppressWarnings("rawtypes")
@Slf4j
public class MemberProducer<M extends Member> implements Provider {

	private interface Invoker<M extends Member> {
		Object get(M member) throws Exception;
	}

	public interface Lookup extends Function<Class<Object>, Provider<Object>> {
		default <T> Provider<T> get(Class<T> t) {
			return apply((Class) t);
		}
	}

	private final M element;
	private final Invoker<M> invoker;
	private final Provider<Injector> injector;

	public MemberProducer(Provider<?> instance, M member, Lookup lookup) {
		this.element = member;

		if (isStatic(member.getModifiers())) {
			instance = () -> null;
		}

		this.invoker = toInvoker(member, instance, lookup);
		this.injector = lookup.get(Injector.class);
	}

	public void bind(Binder bnd) {
		final var scope = isSingleton((AnnotatedElement) this.element) ? Scopes.SINGLETON : Scopes.NO_SCOPE;
		final var type = toType(this.element);
		final var annotation = getBindingAnnotation((AnnotatedElement) this.element);

		if (annotation != null) {
			LOG.atTrace()
					.setMessage("Binding {} to {} producer @{} {}.{}")
					.addArgument(type)
					.addArgument(() -> this.element.getClass().getSimpleName().toLowerCase())
					.addArgument(() -> annotation.annotationType().getSimpleName())
					.addArgument(() -> this.element.getDeclaringClass().getSimpleName())
					.addArgument(() -> this.element.getName())
					.log();

			bnd.bind(Key.get(type, annotation)).toProvider(this).in(scope);
		} else {
			LOG.atTrace()
					.setMessage("Binding {} to {} producer {}.{}")
					.addArgument(type)
					.addArgument(() -> this.element.getClass().getSimpleName().toLowerCase())
					.addArgument(() -> this.element.getDeclaringClass().getSimpleName())
					.addArgument(() -> this.element.getName())
					.log();

			bnd.bind(Key.get(type)).toProvider(this).in(scope);
		}
	}

	@Override
	public Object get() {
		final Object object;

		try {
			((AccessibleObject) this.element).setAccessible(true);

			object = this.invoker.get(this.element);
		} catch (final Exception e) {
			throw new ProvisionException("", e);
		}

		if (object != null) {
			this.injector.get().injectMembers(object);
		}

		return object;
	}

	private static <M extends Member> Invoker<M> toInvoker(M element, Provider<?> instance, Lookup lookup) {
		if (element instanceof final Field f) {
			return e -> f.get(instance.get());
		}
		if (element instanceof final Method m) {
			final Class<?>[] types = m.getParameterTypes();
			final Provider<?>[] providers = Stream.of(types).map(lookup::get).toArray(Provider[]::new);

			return e -> m.invoke(instance.get(), Stream.of(providers).map(Provider::get).toArray());
		}

		throw new IllegalArgumentException("Unexpected type " + element.getClass().getName());
	}

	private static <M extends Member> Type toType(M element) {
		if (element instanceof final Field f) {
			return f.getGenericType();
		}
		if (element instanceof final Method m) {
			return m.getGenericReturnType();
		}

		throw new IllegalArgumentException("Unexpected type " + element.getClass().getName());
	}
}
