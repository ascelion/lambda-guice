package com.ascelion.guice;

import static java.util.Optional.ofNullable;

import com.google.inject.*;
import com.google.inject.Module;

import java.util.*;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import jakarta.enterprise.inject.Vetoed;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Named;
import lombok.RequiredArgsConstructor;

public class GuiceInit {
	@RequiredArgsConstructor
	static class NamedLiteral extends AnnotationLiteral<Named> implements Named {
		final String value;

		@Override
		public String value() {
			return this.value;
		}
	}

	static class StringSetLiteral extends TypeLiteral<Set<String>> {
	}

	private final Set<String> classes = new HashSet<>();
	private final Set<String> packages = new HashSet<>();
	private final List<Module> modules = new ArrayList<>();
	private final List<Module> overrides = new ArrayList<>();
	private final Set<String> excluded = new TreeSet<>();

	GuiceInit() {
		this.classes.add("@");
	}

	public GuiceInit exclude(Class<?>... classes) {
		for (final var c : classes) {
			this.excluded.add(c.getName());
		}

		return this;
	}

	public GuiceInit classes(Class<?>... classes) {
		for (final var c : classes) {
			if (c.isAnnotationPresent(Vetoed.class)) {
				continue;
			}

			if (this.classes.add(c.getName())) {
				ofNullable(c.getAnnotation(GuiceScan.class))
						.ifPresent(a -> {
							classes(a.classes());
							packages(a.packages());
						});
			}
		}

		return this;
	}

	public GuiceInit packages(Class<?>... classes) {
		for (final var c : classes) {
			this.packages.add(c.getPackageName());
		}

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

	public <T> T boot(Class<T> type) {
		if (Injector.class.isAssignableFrom(type)) {
			throw new IllegalArgumentException("Invalid type, use GuiceBoot.init(...).boot() to create an injector");
		}

		classes(type);

		return new GuiceBoot(this, this.modules, this.overrides).construct(type);
	}

	public Module buildInitModule() {
		return bnd -> {
			bnd.bind(ScanResult.class)
					.annotatedWith(GuiceScan.class)
					.toProvider(this::scan)
					.in(Scopes.SINGLETON);
			bnd.bind(new StringSetLiteral())
					.annotatedWith(new NamedLiteral("beanTypes"))
					.toInstance(new TreeSet<>());
		};
	}
}
