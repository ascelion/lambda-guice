package com.ascelion.guice.request;

import static com.ascelion.guice.GuiceBoot.guiceInit;
import static org.assertj.core.api.Assertions.assertThatException;

import com.google.inject.Injector;

import lombok.*;
import org.junit.jupiter.api.Test;

class RequestScopeTest {
	@RequestScoped
	@NoArgsConstructor
	@AllArgsConstructor
	@Getter
	static class Request1 {
		private String value;
	}

	@Test
	void withInterface() {
		final Injector inj = guiceInit(RequestScope.class).boot();

		assertThatException().isThrownBy(() -> inj.getInstance(Request1.class));
	}
}
