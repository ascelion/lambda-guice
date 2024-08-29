package com.ascelion.guice.internal;

import com.ascelion.guice.GuiceScan;
import com.google.inject.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.*;
import java.util.stream.Stream;

import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import jakarta.inject.Inject;
import lombok.Getter;

public final class BootstrapContext {

	@Getter
	private final ScanResult scanned;
	private final Set<String> beans = new TreeSet<>();
	private final Map<Class<? extends Annotation>, Scope> scopes = new IdentityHashMap<>();

	@Inject
	public BootstrapContext(@GuiceScan ScanResult scanned) {
		this.scanned = scanned;
		this.scopes.put(com.google.inject.Singleton.class, Scopes.SINGLETON);
	}

	public void addBean(ClassInfo ci) {
		this.beans.add(ci.getName());
	}

	public void addBean(Class cl) {
		this.beans.add(cl.getName());
	}

	public boolean containsBean(ClassInfo ci) {
		return this.beans.contains(ci.getName());
	}

	public boolean containsBean(Class cl) {
		return this.beans.contains(cl.getName());
	}

	public void addScope(Scope scope, Class<? extends Annotation> annotation) {
		this.scopes.put(annotation, scope);
	}

	public Scope getScope(AnnotatedElement... annotated) {
		return Stream.of(annotated)
				.map(GuiceUtils::scopeAnnotation)
				.filter(Optional::isPresent)
				.map(Optional::get)
				.map(this::requireScope)
				.findFirst()
				.orElse(Scopes.NO_SCOPE);
	}

	private Scope requireScope(Class<? extends Annotation> annotation) {
		return this.scopes.computeIfAbsent(annotation, a -> {
			throw new ProvisionException("Cannot find scope for @" + a.getName());
		});
	}
}
