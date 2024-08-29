package com.ascelion.demo;

import java.math.BigDecimal;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Service {

	@Inject
	@Arguments
	String[] args1;
	@Inject
	@Arguments
	List<String> args2;

	@Inject
	@Named("PI")
	BigDecimal pi;

	@PostConstruct
	void init() {
		LOG.info("Args1: {}", (Object) this.args1);
		LOG.info("Args2: {}", this.args2);
	}

	public void run() {
		LOG.info("PI: {}", this.pi);
	}
}
