package com.ascelion.guice.test;

import static com.ascelion.guice.GuiceBoot.guiceInit;
import static com.ascelion.guice.internal.GuiceUtils.isAnnotatedWith;
import static org.apache.commons.lang3.reflect.FieldUtils.getAllFieldsList;
import static org.apache.commons.lang3.reflect.FieldUtils.getFieldsListWithAnnotation;
import static org.apache.commons.lang3.reflect.MethodUtils.getMethodsListWithAnnotation;

import com.ascelion.guice.internal.MemberProducer;
import com.google.inject.*;
import com.google.inject.Module;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import jakarta.enterprise.inject.Vetoed;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import uk.org.webcompere.systemstubs.resource.TestResource;

@Vetoed
@Slf4j
public final class GuiceMockStubsModule implements Module {
	private final Object instance;
	private final Class<?> type;
	private final List<Field> producerFields;
	private final List<Method> producerMethods;
	private final List<Field> stubs;
	private final Injector injector;

	public GuiceMockStubsModule(@NonNull Object instance, Class<? extends Annotation> stubAnnotation) {
		this.instance = instance;
		this.type = instance.getClass();

		this.producerFields = getFieldsListWithAnnotation(this.type, BindProducer.class);
		this.producerMethods = getMethodsListWithAnnotation(this.type, BindProducer.class, true, true);

		this.stubs = getAllFieldsList(instance.getClass()).stream()
				.filter(f -> isAnnotatedWith(f, stubAnnotation))
				.filter(f -> TestResource.class.isAssignableFrom(f.getType()))
				.toList();

		final var init = guiceInit(this.type)
				.exclude(this.type)
				.classes(this.type.getDeclaredClasses());

		this.producerFields.forEach(field -> init.classes(field.getType()));
		this.producerMethods.forEach(methods -> init.classes(methods.getReturnType()));

		getAllFieldsList(instance.getClass()).stream()
				.filter(f -> isAnnotatedWith(f, jakarta.inject.Inject.class, com.google.inject.Inject.class))
				.forEach(field -> init.classes(field.getType()));

		if (instance instanceof final Module m) {
			init.overrides(m);
		}

		init.overrides(this);

		this.injector = init.boot();
	}

	@Override
	public void configure(Binder bnd) {
		this.producerFields.forEach(field -> {
			new MemberProducer(field, () -> this.instance, () -> this.injector)
					.bind(bnd, Scopes.NO_SCOPE);
		});
		this.producerMethods.forEach(method -> {
			new MemberProducer(method, () -> this.instance, () -> this.injector)
					.bind(bnd, Scopes.NO_SCOPE);
		});
	}

	public void setUp() {
		this.stubs.forEach(this::setUp);

		this.injector.injectMembers(this.instance);
	}

	@SneakyThrows
	private void setUp(Field field) {
		field.setAccessible(true);

		final var ts = (TestResource) field.get(instance);

		if (ts != null) {
			ts.setup();
		}
	}
}
