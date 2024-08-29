package com.ascelion.guice;

import com.google.inject.*;
import com.google.inject.Module;
import com.google.inject.util.Modules;

import java.lang.reflect.Method;
import java.util.*;
import java.util.ServiceLoader.Provider;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class GuiceBoot {

	@Getter(lazy = true, value = AccessLevel.PRIVATE)
	private final Injector injector = createInjector();

	private final GuiceInit init;
	private final List<Module> modules;
	private final List<Module> overrides;

	public static <T> T guiceBoot(Class<T> type) {
		if (Injector.class.isAssignableFrom(type)) {
			throw new IllegalArgumentException("Invalid type, use GuiceBoot.init(...).boot() to create an injector");
		}

		return guiceInit(type).boot(type);
	}

	public static GuiceInit guiceInit(Class<?>... classes) {
		return new GuiceInit().classes(classes);
	}

	<T> T construct(Class<T> type) {
		return getInjector().getInstance(type);
	}

	<T> T construct(T instance, Injector inj) {
		inj.injectMembers(instance);

		return instance;
	}

	private Injector createInjector() {
		final var first = Guice.createInjector(Stage.PRODUCTION,
				Binder::requireExplicitBindings,
				this.init.buildInitModule());

		final var allModules = new ArrayList<>(loadModules(first));

		allModules.addAll(this.modules);

		return first.createChildInjector(Modules.override(allModules).with(this.overrides));
	}

	private List<Module> loadModules(Injector root) {
		return ServiceLoader.load(Module.class).stream()
				.map(Provider::get)
				.map(mod -> construct(mod, root))
				.toList();
	}

	@SneakyThrows
	private static void invoke(Method method, Object instance) {
		method.invoke(instance);
	}
}
