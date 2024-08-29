package com.ascelion.guice.internal;

import com.ascelion.guice.GuiceScan;
import com.google.inject.Scope;
import com.google.inject.Scopes;

import java.lang.annotation.Annotation;
import java.util.*;

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
}
