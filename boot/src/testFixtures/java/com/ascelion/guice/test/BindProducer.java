package com.ascelion.guice.test;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.*;

@Target({ FIELD, METHOD })
@Retention(RUNTIME)
@Documented
public @interface BindProducer {
}
