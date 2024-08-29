package com.ascelion.guice;

import static java.util.Comparator.nullsLast;
import static java.util.Optional.ofNullable;

import com.google.inject.Module;

import jakarta.annotation.Priority;

public interface ModulePriorities {
	int MODULE_PRIORITY_OFFSET = 100;

	int HIGHEST_MODULE_PRIORITY = Integer.MIN_VALUE + 100 * MODULE_PRIORITY_OFFSET;

	int SCOPE_MODULE_PRIORITY = -100 * MODULE_PRIORITY_OFFSET;
	int PROVIDER_MODULE_PRIORITY = 0;
	int APPLICATION_MODULE_PRIORITY = 100 * MODULE_PRIORITY_OFFSET;

	int LOWEST_MODULE_PRIORITY = Integer.MAX_VALUE - 100 * MODULE_PRIORITY_OFFSET;

	static int compareModules(Module m1, Module m2) {
		final var p1 = ofNullable(m1.getClass().getAnnotation(Priority.class))
				.map(Priority::value)
				.orElse(null);
		final var p2 = ofNullable(m2.getClass().getAnnotation(Priority.class))
				.map(Priority::value)
				.orElse(null);

		return nullsLast(Integer::compare).compare(p1, p2);
	}
}
