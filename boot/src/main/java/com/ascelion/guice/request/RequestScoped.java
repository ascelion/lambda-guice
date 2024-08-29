package com.ascelion.guice.request;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.google.inject.ScopeAnnotation;

import java.lang.annotation.*;

@Target({ TYPE, METHOD, FIELD })
@Retention(RUNTIME)
@ScopeAnnotation
@Inherited
public @interface RequestScoped {
}
