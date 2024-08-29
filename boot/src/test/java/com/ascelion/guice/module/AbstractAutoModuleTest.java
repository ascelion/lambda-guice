package com.ascelion.guice.module;

import static com.ascelion.guice.GuiceBoot.guiceInit;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

import java.util.*;

public abstract class AbstractAutoModuleTest {
	Injector createInjector(boolean explicitBinding, boolean childInjector, boolean postConstruct) {
		final var ini = guiceInit(getClass().getDeclaredClasses());

		final Module strict = bnd -> {
			if (explicitBinding) {
				bnd.requireExplicitBindings();
			}
		};

		final Injector inj;

		if (childInjector) {
			final var inj0 = Guice.createInjector(strict, ini.buildInitModule());
			final List<Module> all = new ArrayList<>();

			all.add(injectMembers(inj0, new AutoBindClassProvidersModule()));
			all.add(injectMembers(inj0, new AutoBindMethodProducersModule()));
			all.add(injectMembers(inj0, new AutoBindFieldProducersModule()));
			all.add(injectMembers(inj0, new AutoBindBeansModule()));

			if (postConstruct) {
				all.add(injectMembers(inj0, new PostConstructModule()));
			}

			inj = inj0.createChildInjector(all);
		} else {
			final Set<String> set = new HashSet<>();
			final List<Module> all = new ArrayList<>();

			all.add(strict);
			all.add(new AutoBindClassProvidersModule(ini.scan(), set));
			all.add(new AutoBindMethodProducersModule(ini.scan(), set));
			all.add(new AutoBindFieldProducersModule(ini.scan(), set));
			all.add(new AutoBindBeansModule(ini.scan(), set));

			if (postConstruct) {
				all.add(new PostConstructModule());
			}

			inj = Guice.createInjector(all);
		}

		return inj;
	}

	static <T> T injectMembers(Injector inj, T t) {
		inj.injectMembers(t);

		return t;
	}

}
