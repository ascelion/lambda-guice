package com.ascelion.guice;

import static com.ascelion.guice.GuiceBoot.guiceBoot;
import static com.ascelion.guice.GuiceBoot.guiceInit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.google.inject.Injector;

import org.junit.jupiter.api.Test;

class GuiceBootTest {

	static class App {
	}

	@SuppressWarnings("java:S5778")
	@Test
	void run() {
		assertAll(
				() -> assertThat(guiceBoot(App.class))
						.isInstanceOf(App.class),
				() -> assertThat(guiceInit(getClass()).classes(getClass().getDeclaredClasses()).boot(App.class))
						.isInstanceOf(App.class),
				() -> assertThat(guiceInit(getClass()).boot())
						.isInstanceOf(Injector.class),

				() -> assertThatExceptionOfType(IllegalArgumentException.class)
						.isThrownBy(() -> guiceBoot(Injector.class)),
				() -> assertThatExceptionOfType(IllegalArgumentException.class)
						.isThrownBy(() -> guiceInit().boot(Injector.class)),
				() -> assertThatExceptionOfType(IllegalArgumentException.class)
						.isThrownBy(() -> guiceInit(getClass()).boot(Injector.class)),
				() -> assertThatExceptionOfType(IllegalStateException.class)
						.isThrownBy(() -> guiceInit().boot()),

				() -> {});
	}

}
