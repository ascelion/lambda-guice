package com.ascelion.guice;

import static java.lang.Thread.currentThread;
import static java.util.Collections.sort;

import com.google.inject.*;
import com.google.inject.Module;
import com.google.inject.util.Modules;

import java.lang.reflect.Method;
import java.util.*;

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
		return guiceInit().boot(type);
	}

	public static <T> T guiceBoot(T instance) {
		return guiceInit().boot(instance);
	}

	public static GuiceInit guiceInit(Class<?>... classes) {
		return new GuiceInit().classes(classes);
	}

	<T> T construct(Class<T> type) {
		return getInjector().getInstance(type);
	}

	private Injector createInjector() {
		final var allModules = new ArrayList<>(loadModules());

		allModules.addAll(this.modules);

		visitModules(allModules);

		final var ovrModules = new ArrayList<>(this.overrides);

		visitModules(ovrModules);

		final var first = Guice.createInjector(Stage.PRODUCTION,
				Binder::requireExplicitBindings,
				this.init.buildInitModule());

		allModules.forEach(first::injectMembers);
		ovrModules.forEach(first::injectMembers);

		return first.createChildInjector(Modules.override(allModules).with(ovrModules));
	}

	private void visitModules(final java.util.ArrayList<com.google.inject.Module> modules) {
		sort(modules, ModulePriorities::compareModules);

		for (final var it : modules) {
			final var cl = it.getClass();

			this.init.classes(cl).excluded(cl);
		}
	}

	private List<Module> loadModules() {
		return ServiceLoader.load(Module.class, currentThread().getContextClassLoader())
				.stream()
				.map(ServiceLoader.Provider::get)
				.toList();
	}

	@SneakyThrows
	private static void invoke(Method method, Object instance) {
		method.invoke(instance);
	}
}
