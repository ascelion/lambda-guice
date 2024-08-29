package com.ascelion.demo;

import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.utility.DockerImageName;

@TestInstance(Lifecycle.PER_CLASS)
@Slf4j
class ApplicationIT {
	static final String LOCAL_STACK_VERSION = System.getProperty("localstack.version", "3.6");
	static final DockerImageName LOCAL_STACK_IMAGE = DockerImageName
			.parse("localstack/localstack:" + LOCAL_STACK_VERSION);
	private LocalStackContainer container;

	@SuppressWarnings("resource")
	@BeforeAll
	void beforeAll() {
		this.container = new LocalStackContainer(LOCAL_STACK_IMAGE)
				.withFileSystemBind(System.getProperty("user.dir"), "/tmp/work", BindMode.READ_ONLY)
				.withServices(Service.LAMBDA, Service.SQS);

		this.container.start();
		this.container.followOutput(f -> LOG.debug("{}", f.getUtf8StringWithoutLineEnding()));

		exec("awslocal", "sqs", "create-queue", "--queue-name", "ascelion-demo");
	}

	@AfterAll
	void afterAll() {
		this.container.stop();
		this.container.close();
	}

	void exec(String... command) {
		LOG.atInfo().addArgument(() -> Stream.of(command).collect(joining(" "))).log("Running {}");

		try {
			final var result = this.container.execInContainer(command);

			LOG.info(result.getStdout());
			LOG.info(result.getStderr());

			if (result.getExitCode() != 0) {
				fail("Exit code " + result.getExitCode());
			}
		} catch (UnsupportedOperationException | IOException | InterruptedException e) {
			LOG.error("Execution failed", e);

			fail(e.getMessage());
		}
	}

	@Test
	void run() {
	}
}
