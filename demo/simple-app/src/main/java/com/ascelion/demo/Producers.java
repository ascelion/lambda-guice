package com.ascelion.demo;

import java.math.BigDecimal;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

public class Producers {
	@Produces
	@Singleton
	@Named("E")
	static final BigDecimal E = new BigDecimal("2.718281828459045235360287471");

	@Produces
	@Singleton
	@Named("PI")
	static final BigDecimal PI = new BigDecimal("3.141592653589793238462643383");

	@Produces
	@Singleton
	@Named("PHI")
	static final BigDecimal PHI = new BigDecimal("1.618033988749894848204586834");
}
