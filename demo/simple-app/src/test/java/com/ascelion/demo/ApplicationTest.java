package com.ascelion.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.ascelion.guice.jupiter.GuiceBootExtension;
import com.ascelion.guice.test.BindProducer;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GuiceBootExtension.class)
class ApplicationTest {

	@BindProducer
	@Arguments
	@Singleton
	final String[] args1 = { "test", "arguments" };

	@BindProducer
	@Arguments
	@Singleton
	final List<String> args2 = List.of(this.args1);

	@Inject
	Application app;

	@Test
	void run() {
		assertThat(this.app).extracting("service.args1").isEqualTo(this.args1);
		assertThat(this.app).extracting("service.args2").isEqualTo(this.args2);
	}
}
