package com.ascelion.guice;

import com.ascelion.guice.internal.BootstrapContext;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Scopes;

import java.util.*;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import jakarta.enterprise.inject.Vetoed;
import lombok.NonNull;

public class GuiceInit {
	private final Set<String> classes = new HashSet<>();
	private final Set<String> packages = new HashSet<>();
	private final List<Module> modules = new ArrayList<>();
	private final List<Module> overrides = new ArrayList<>();
	private final Set<String> excluded = new HashSet<>();

	GuiceInit() {
		this.classes.add("@");
	}

	public GuiceInit excluded(Class<?>... classes) {
		for (final var c : classes) {
			this.excluded.add(c.getName());
		}

		return this;
	}

	public GuiceInit classes(Class<?>... classes) {
		for (final var it : classes) {
			if (isEligible(it) && this.classes.add(it.getName())) {
				process(it);
			}
		}

		return this;
	}

	private void process(final Class<?> type) {
		final var a = type.getAnnotation(GuiceScan.class);

		if (a != null) {
			classes(a.value());
			classes(a.classes());
			classNames(a.classNames());
			packages(a.packages());
			packageNames(a.packageNames());
			excluded(a.excluded());
		}

		for (final var c : type.getDeclaredClasses()) {
			classes(c);
		}
	}

	public GuiceInit classNames(String... classNames) {
		this.classes.addAll(Set.of(classNames));

		return this;
	}

	public GuiceInit packages(Class<?>... packages) {
		for (final var it : packages) {
			this.packages.add(it.getPackageName());
		}

		return this;
	}

	public GuiceInit packageNames(String... packageNames) {
		this.packages.addAll(Set.of(packageNames));

		return this;
	}

	public GuiceInit modules(Module... modules) {
		this.modules.addAll(List.of(modules));

		return this;
	}

	public GuiceInit overrides(Module... overrides) {
		this.overrides.addAll(List.of(overrides));

		return this;
	}

	public ScanResult scan() {
		return new ClassGraph()
				.rejectClasses(this.excluded.toArray(String[]::new))
				.acceptClasses(this.classes.toArray(String[]::new))
				.acceptPackages(this.packages.toArray(String[]::new))
				.enableAllInfo().scan();
	}

	public Injector boot() {
		if (this.classes.size() == 1 && this.packages.isEmpty()) {
			throw new IllegalStateException("No scanning information provided");
		}

		return new GuiceBoot(this, this.modules, this.overrides).construct(Injector.class);
	}

	public <T> T boot(@NonNull Class<T> type) {
		if (Injector.class.isAssignableFrom(type)) {
			throw new IllegalArgumentException("Invalid type, use GuiceBoot.init(...).boot() to create an injector");
		}

		classes(type);

		return new GuiceBoot(this, this.modules, this.overrides).construct(type);
	}

	public <T> T boot(@NonNull T instance) {
		if (instance instanceof Injector) {
			throw new IllegalArgumentException("Invalid type, use GuiceBoot.init(...).boot() to create an injector");
		}

		classes(instance.getClass());

		boot().injectMembers(instance);

		return instance;
	}

	public Module buildInitModule() {
		return bnd -> {
			bnd.bind(ScanResult.class)
					.annotatedWith(GuiceScan.class)
					.toProvider(this::scan)
					.in(Scopes.SINGLETON);
			bnd.bind(BootstrapContext.class)
					.in(Scopes.SINGLETON);
		};
	}

	private static boolean isEligible(Class<?> c) {
		if (c.isAnnotationPresent(Vetoed.class)) {
			return false;
		}
		if (c.isAnnotation()) {
			return false;
		}
		if (c.isAnonymousClass()) {
			return false;
		}
		if (c.isEnum()) {
			return false;
		}
		if (c.isInterface()) {
			return false;
		}
		if (c.isHidden()) {
			return false;
		}

		if (c.isRecord()) {
			return false;
		}
		if (c.isSynthetic()) {
			return false;
		}

		return true;
	}
}
