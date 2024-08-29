package com.ascelion.guice.module;

import static com.ascelion.guice.GuiceBoot.guiceInit;
import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

class AutoBindProducersTest {
	static final String STRING_NAME = "string-42";
	static final String STRING_VALUE = "42";

	static class Service1 {
		@Inject
		@Named(STRING_NAME)
		String string42;
	}

	@RequiredArgsConstructor
	static class Service2 {
		final Service1 service1;

		@Inject
		@Named("string-42")
		String string42;
	}

	static class Service3 {
		final Service2 service2;
		final String string42;

		Service3(Service2 service2, String string42) {
			this.service2 = service2;
			this.string42 = string42;
		}
	}

	static class Producers {
		@Inject
		@Named(STRING_NAME)
		String string42;

		@Produces
		Service2 create(Service1 service1) {
			return new Service2(service1);
		}

		@Produces
		Service3 create(Service2 service2) {
			return new Service3(service2, "_" + this.string42);
		}
	}

	@Produces
	@Named(STRING_NAME)
	String string42 = STRING_VALUE;

	@Test
	void run() {
		final var inj = guiceInit(getClass()).classes(getClass().getDeclaredClasses()).boot();
		final var service1 = inj.getInstance(Service1.class);
		final var service2 = inj.getInstance(Service2.class);
		final var service3 = inj.getInstance(Service3.class);

		assertThat(service1).extracting("string42").isEqualTo(STRING_VALUE);

		assertThat(service2).extracting("string42").isEqualTo(STRING_VALUE);
		assertThat(service2).extracting("service1.string42").isEqualTo(STRING_VALUE);

		assertThat(service3).extracting("string42").isEqualTo("_" + STRING_VALUE);
		assertThat(service3).extracting("service2.string42").isEqualTo(STRING_VALUE);
		assertThat(service3).extracting("service2.service1.string42").isEqualTo(STRING_VALUE);
	}
}
