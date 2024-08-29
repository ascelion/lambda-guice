package com.ascelion.guice;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import org.junit.jupiter.api.Test;

class GuiceInitTest {

	static class Class1 {
	}

	static class Class2 {
	}

	@GuiceScan(classes = { Class1.class }, packages = jakarta.inject.Inject.class)
	static class Root1 {
	}

	@GuiceScan(classes = { Class2.class }, packages = com.google.inject.Inject.class)
	static class Root2 {
	}

	@Test
	void empty() {
		final ScanResult result = new GuiceInit()
				.scan();

		assertThat(result.getAllClasses())
				.isEmpty();
	}

	@Test
	void classes1() {
		final ScanResult result = new GuiceInit()
				.classes(Class1.class)
				.scan();

		assertThat(result.getAllClasses())
				.extracting(ClassInfo::getName)
				.containsExactly(Class1.class.getName());
	}

	@Test
	void classes21() {
		final ScanResult result = new GuiceInit()
				.classes(Class1.class, Class2.class)
				.scan();

		assertThat(result.getAllClasses())
				.extracting(ClassInfo::getName)
				.containsExactlyInAnyOrder(Class1.class.getName(), Class2.class.getName());
	}

	@Test
	void classes22() {
		final ScanResult result = new GuiceInit()
				.classes(Class1.class)
				.classes(Class2.class)
				.scan();

		assertThat(result.getAllClasses())
				.extracting(ClassInfo::getName)
				.containsExactlyInAnyOrder(Class1.class.getName(), Class2.class.getName());
	}

	@Test
	void packages1() {
		final ScanResult result = new GuiceInit()
				.packages(jakarta.inject.Inject.class)
				.scan();

		assertThat(result.getAllClasses())
				.extracting(ClassInfo::getPackageName)
				.isNotEmpty()
				.allMatch("jakarta.inject"::equals);
	}

	@Test
	void packages21() {
		final ScanResult result = new GuiceInit()
				.packages(jakarta.inject.Inject.class, com.google.inject.Inject.class)
				.scan();

		assertThat(result.getAllClasses())
				.extracting(ClassInfo::getPackageName)
				.isNotEmpty()
				.anyMatch(x -> "jakarta.inject".equals(x) || "com.google.inject".equals(x));
	}

	@Test
	void packages22() {
		final ScanResult result = new GuiceInit()
				.packages(jakarta.inject.Inject.class)
				.packages(com.google.inject.Inject.class)
				.scan();

		assertThat(result.getAllClasses())
				.extracting(ClassInfo::getPackageName)
				.isNotEmpty()
				.anyMatch(x -> "jakarta.inject".equals(x) || "com.google.inject".equals(x));
	}

	@Test
	void root1() {
		final ScanResult result = new GuiceInit()
				.classes(Root1.class)
				.scan();

		assertThat(result.getAllClasses())
				.extracting(ci -> (Class) ci.loadClass())
				.isNotEmpty()
				.contains(Root1.class, Class1.class, jakarta.inject.Singleton.class)
				.doesNotContain(getClass(), Root2.class, Class2.class, com.google.inject.Singleton.class);
	}

	@Test
	void root21() {
		final ScanResult result = new GuiceInit()
				.classes(Root1.class, Root2.class)
				.scan();

		assertThat(result.getAllClasses())
				.extracting(ci -> (Class) ci.loadClass())
				.isNotEmpty()
				.contains(Root1.class, Class1.class, jakarta.inject.Singleton.class)
				.contains(Root2.class, Class2.class, com.google.inject.Singleton.class)
				.doesNotContain(getClass());
	}

	@Test
	void root22() {
		final ScanResult result = new GuiceInit()
				.classes(Root1.class)
				.classes(Root2.class)
				.scan();

		assertThat(result.getAllClasses())
				.extracting(ci -> (Class) ci.loadClass())
				.isNotEmpty()
				.contains(Root1.class, Class1.class, jakarta.inject.Singleton.class)
				.contains(Root2.class, Class2.class, com.google.inject.Singleton.class)
				.doesNotContain(getClass());
	}
}
