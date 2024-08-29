package com.ascelion.demo;

import static com.ascelion.guice.GuiceBoot.guiceBoot;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.ascelion.guice.GuiceScan;
import com.ascelion.guice.request.RequestScope;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.IntStream;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@GuiceScan(
		packages = RequestApp.class,
		classes = {
				RequestScope.class,
		})
@Slf4j
public class RequestApp {

	static final List<Integer> INVOCATIONS = IntStream.range(0, 100)
			.mapToObj(Integer::valueOf)
			.toList();

	public static void main(String[] args) {
		guiceBoot(RequestApp.class);
	}

	@Inject
	private ExecutorService executor;

	@Inject
	private RequestScope scope;

	@Inject
	private Service service;

	@PostConstruct
	void start() throws InterruptedException {
		INVOCATIONS.forEach(n -> this.executor.submit(() -> invocation(n)));

		this.executor.shutdown();
		this.executor.awaitTermination(5, SECONDS);
	}

	private void invocation(int n) {
		this.scope.activate()
				.seed(AppRequest.class, new AppRequest("value " + n))
				.seed(AppContext.class, () -> n)
				.proceed(() -> this.service.proceed(n));
	}
}
