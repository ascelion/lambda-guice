package com.ascelion.demo;

import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Service {
	@Inject
	private AppContext context;
	@Inject
	private AppRequest request;
	@Inject
	private AppRequestWrapper wrapper;

	public void proceed(int invocation) {
		LOG.info("Invocation {}, context {}, request {}, wrapper {}",
				invocation, this.context.value(), this.request, this.wrapper);
	}
}
