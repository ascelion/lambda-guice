package com.ascelion.guice.test;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.reflect.FieldUtils.getAllFieldsList;
import static org.apache.commons.lang3.reflect.FieldUtils.getFieldsListWithAnnotation;
import static org.apache.commons.lang3.reflect.MethodUtils.getMethodsListWithAnnotation;

import com.ascelion.guice.GuiceBoot;
import com.ascelion.guice.GuiceUtils;
import com.google.inject.*;
import com.google.inject.Module;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Supplier;

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
				.filter(f -> GuiceUtils.isAnnotatedWith(f, stubAnnotation))
				.filter(f -> TestResource.class.isAssignableFrom(f.getType()))
				.toList();

		final var init = GuiceBoot.init(this.type)
				.classes(this.type.getDeclaredClasses());

		this.producerFields.forEach(field -> init.classes(field.getType()));
		this.producerMethods.forEach(methods -> init.classes(methods.getReturnType()));

		getAllFieldsList(instance.getClass()).stream()
				.filter(f -> GuiceUtils.isAnnotatedWith(f, jakarta.inject.Inject.class, com.google.inject.Inject.class))
				.forEach(field -> init.classes(field.getType()));

		if (instance instanceof final Module m) {
			init.modules(m);
		}

		init.overrides(this);

		this.injector = init.boot();
	}

	@Override
	public void configure(Binder bnd) {
		this.producerFields.forEach(field -> {
			LOG.atTrace().addArgument(() -> fieldInfo(field)).log("Binding {}");

			bnd.bind(field.getType()).toProvider((Provider) () -> fromField(field));
		});
		this.producerMethods.forEach(method -> {
			LOG.atTrace().addArgument(() -> methodInfo(method)).log("Binding {}");

			bnd.bind(method.getReturnType()).toProvider((Provider) () -> fromMethod(method));
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

	@SneakyThrows
	private Object fromField(Field field) {
		field.setAccessible(true);

		final var bean = nonNull(field.get(instance), () -> fieldInfo(field));

		this.injector.injectMembers(bean);

		return bean;
	}

	@SneakyThrows
	private Object fromMethod(Method method) {
		method.setAccessible(true);

		final var bean = nonNull(method.invoke(this.instance), () -> methodInfo(method));

		this.injector.injectMembers(bean);

		return bean;
	}

	private static String fieldInfo(Field field) {
		return format("field %s.%s", field.getDeclaringClass().getSimpleName(), field.getName());
	}

	private static String methodInfo(Method method) {
		return format("method %s.%s()", method.getDeclaringClass().getSimpleName(), method.getName());
	}

	private Object nonNull(Object value, Supplier<String> info) {
		if (value == null) {
			throw new ProvisionException(format("Bound %s returned null, make sure Mockito "
					+ "or any other provider is initialised first", info.get()));
		}

		return value;
	}
}
