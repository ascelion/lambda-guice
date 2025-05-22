package com.ascelion.s3;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.enterprise.util.AnnotationLiteral;
import lombok.RequiredArgsConstructor;

@Retention(RUNTIME)
@Target({ FIELD, PARAMETER })
@BindingAnnotation
public @interface BucketName {
	@RequiredArgsConstructor
	public static final class Literal extends AnnotationLiteral<BucketName> implements BucketName {
		private final String value;

		@Override
		public String value() {
			return this.value;
		}
	}

	String value();
}
