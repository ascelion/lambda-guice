package com.ascelion.guice;

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
				() -> assertThat(GuiceBoot.boot(App.class))
						.isInstanceOf(App.class),
				() -> assertThat(GuiceBoot.init(getClass()).classes(getClass().getDeclaredClasses()).boot(App.class))
						.isInstanceOf(App.class),
				() -> assertThat(GuiceBoot.init(getClass()).boot())
						.isInstanceOf(Injector.class),

				() -> assertThatExceptionOfType(IllegalArgumentException.class)
						.isThrownBy(() -> GuiceBoot.boot(Injector.class)),
				() -> assertThatExceptionOfType(IllegalArgumentException.class)
						.isThrownBy(() -> GuiceBoot.init().boot(Injector.class)),
				() -> assertThatExceptionOfType(IllegalArgumentException.class)
						.isThrownBy(() -> GuiceBoot.init(getClass()).boot(Injector.class)),
				() -> assertThatExceptionOfType(IllegalStateException.class)
						.isThrownBy(() -> GuiceBoot.init().boot()),

				() -> {});
	}

}
