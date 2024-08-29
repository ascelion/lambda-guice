package com.ascelion.guice.module;

import static com.ascelion.guice.GuiceBoot.guiceInit;
import static java.util.Collections.sort;

import com.ascelion.guice.ModulePriorities;
import com.ascelion.guice.internal.BootstrapContext;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

import java.util.ArrayList;
import java.util.List;

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

			all.add(injectMembers(inj0, new AutoBindBeansModule()));
			all.add(injectMembers(inj0, new AutoBindClassProvidersModule()));
			all.add(injectMembers(inj0, new AutoBindFieldProducersModule()));
			all.add(injectMembers(inj0, new AutoBindMethodProducersModule()));
			all.add(injectMembers(inj0, new AutoBindScopeModule()));

			if (postConstruct) {
				all.add(injectMembers(inj0, new PostConstructModule()));
			}

			sort(all, ModulePriorities::compareModules);

			inj = inj0.createChildInjector(all);
		} else {
			final BootstrapContext ctx = new BootstrapContext(ini.scan());
			final List<Module> all = new ArrayList<>();

			all.add(strict);
			all.add(new AutoBindBeansModule(ctx));
			all.add(new AutoBindClassProvidersModule(ctx));
			all.add(new AutoBindFieldProducersModule(ctx));
			all.add(new AutoBindMethodProducersModule(ctx));
			all.add(new AutoBindScopeModule(ctx));

			if (postConstruct) {
				all.add(new PostConstructModule());
			}

			sort(all, ModulePriorities::compareModules);

			inj = Guice.createInjector(all);
		}

		return inj;
	}

	static <T> T injectMembers(Injector inj, T t) {
		inj.injectMembers(t);

		return t;
	}

}
