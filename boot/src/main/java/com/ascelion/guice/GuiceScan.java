package com.ascelion.guice;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target({ TYPE, FIELD, METHOD, CONSTRUCTOR, PARAMETER })
@BindingAnnotation
public @interface GuiceScan {
	Class<?>[] classes() default {};

	Class<?>[] packages() default {};
}
