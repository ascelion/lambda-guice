package com.ascelion.lambda;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.util.Collections.enumeration;

import java.io.IOException;
import java.net.*;
import java.util.Enumeration;
import java.util.LinkedHashSet;

public final class TestCaseClassLoader extends URLClassLoader {

	@SuppressWarnings("java:S3457")
	public static URLClassLoader forCase(String caseName, String resource) throws MalformedURLException {
		final ClassLoader cld = currentThread().getContextClassLoader();
		final String resName = format("cld-roots/%s/" + resource, caseName);
		final String fullUrl = cld.getResource(resName).toString();
		final String baseUrl = fullUrl.substring(0, fullUrl.length() - resource.length());

		return new TestCaseClassLoader(new URL(baseUrl), cld);
	}

	private TestCaseClassLoader(URL url, ClassLoader parent) {
		super(new URL[] { url }, parent);

		currentThread().setContextClassLoader(this);
	}

	@Override
	public URL getResource(String name) {
		final URL res = findResource(name);

		return res != null ? res : super.getResource(name);
	}

	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		final var all = new LinkedHashSet<URL>();

		final Enumeration<URL> en1 = findResources(name);

		while (en1.hasMoreElements()) {
			all.add(en1.nextElement());
		}

		final Enumeration<URL> en2 = super.getResources(name);

		while (en2.hasMoreElements()) {
			all.add(en2.nextElement());
		}

		return enumeration(all);
	}

	@Override
	public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		Class<?> clazz = findLoadedClass(name);

		if (clazz != null) {
			return clazz;
		}

		try {
			clazz = findClass(name);

			if (resolve) {
				resolveClass(clazz);
			}

			return clazz;
		} catch (final ClassNotFoundException e) {
			/* ignore */
		}

		return super.loadClass(name, resolve);
	}

	@Override
	public void close() throws IOException {
		final ClassLoader parent = getParent();

		super.close();

		currentThread().setContextClassLoader(parent);
	}
}
