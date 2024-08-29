package com.ascelion.guice;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Named;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class NamedLiteral extends AnnotationLiteral<Named> implements Named {
	private final String value;

	@Override
	public String value() {
		return this.value;
	}
}
