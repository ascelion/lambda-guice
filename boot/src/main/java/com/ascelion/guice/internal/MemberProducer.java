package com.ascelion.guice.internal;

import static com.ascelion.guice.internal.GuiceUtils.getBindingAnnotation;
import static com.ascelion.guice.internal.GuiceUtils.isSingleton;
import static java.lang.reflect.Modifier.isStatic;

import com.google.inject.*;

import java.lang.reflect.*;
import java.util.stream.Stream;

import jakarta.inject.Provider;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@SuppressWarnings("rawtypes")
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class MemberProducer implements Provider {

	private abstract static class Invoker<M extends Member> {
		final M member;
		final Type type;
		final Provider<?> instanceP;

		Invoker(M member, Type type, Provider<?> instanceP) {
			this.member = member;
			this.type = type;

			if (isStatic(member.getModifiers())) {
				this.instanceP = () -> null;
			} else {
				this.instanceP = instanceP;
			}
		}

		abstract Object get();
	}

	private static class FieldInvoker extends Invoker<Field> {
		FieldInvoker(Field field, Provider<?> instanceP) {
			super(field, field.getGenericType(), instanceP);
		}

		@Override
		Object get() {
			this.member.setAccessible(true);

			try {
				return this.member.get(this.instanceP.get());
			} catch (IllegalArgumentException | IllegalAccessException e) {
				throw new ProvisionException("Cannot invoke " + this.member, e);
			}
		}
	}

	private static class MethodInvoker extends Invoker<Method> {
		final Class<?>[] types;
		final Provider<Injector> injectorP;

		MethodInvoker(Method method, Provider<?> instanceP, Provider<Injector> injectorP) {
			super(method, method.getGenericReturnType(), instanceP);

			this.types = this.member.getParameterTypes();
			this.injectorP = injectorP;
		}

		@Override
		Object get() {
			final Injector inj = this.injectorP.get();

			final Object[] args = Stream.of(this.types)
					.map(inj::getProvider)
					.map(Provider::get)
					.toArray();

			this.member.setAccessible(true);

			try {
				return this.member.invoke(this.instanceP.get(), args);
			} catch (IllegalAccessException | InvocationTargetException e) {
				throw new ProvisionException("Cannot invoke " + this.member, e);
			}
		}
	}

	private final Provider<Injector> injectorP;
	private final Invoker<?> invoker;

	public MemberProducer(Field field, Provider<?> instanceP, Provider<Injector> injectorP) {
		this(injectorP, new FieldInvoker(field, instanceP));
	}

	public MemberProducer(Method method, Provider<?> instanceP, Provider<Injector> injectorP) {
		this(injectorP, new MethodInvoker(method, instanceP, injectorP));
	}

	public void bind(Binder bnd) {
		final var annotation = getBindingAnnotation((AnnotatedElement) this.invoker.member);
		final var scope = isSingleton((AnnotatedElement) this.invoker.member) ? Scopes.SINGLETON : Scopes.NO_SCOPE;

		if (annotation != null) {
			LOG.atTrace()
					.setMessage("Binding {} to {} producer @{} {}.{} in scope {}")
					.addArgument(this.invoker.type)
					.addArgument(() -> this.invoker.member.getClass().getSimpleName().toLowerCase())
					.addArgument(() -> annotation.annotationType().getSimpleName())
					.addArgument(() -> this.invoker.member.getDeclaringClass().getSimpleName())
					.addArgument(() -> this.invoker.member.getName())
					.addArgument(scope)
					.log();

			bnd.bind(Key.get(this.invoker.type, annotation)).toProvider(this).in(scope);
		} else {
			LOG.atTrace()
					.setMessage("Binding {} to {} producer {}.{} in scope {}")
					.addArgument(this.invoker.type)
					.addArgument(() -> this.invoker.member.getClass().getSimpleName().toLowerCase())
					.addArgument(() -> this.invoker.member.getDeclaringClass().getSimpleName())
					.addArgument(() -> this.invoker.member.getName())
					.addArgument(scope)
					.log();

			bnd.bind(Key.get(this.invoker.type)).toProvider(this).in(scope);
		}
	}

	@Override
	public Object get() {
		final var object = this.invoker.get();

		if (object != null) {
			this.injectorP.get().injectMembers(object);
		}

		return object;
	}
}
