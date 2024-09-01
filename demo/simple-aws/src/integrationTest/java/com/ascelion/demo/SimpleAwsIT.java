package com.ascelion.demo;

import static java.util.stream.Collectors.joining;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.apache.commons.io.IOUtils.resourceToByteArray;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.EnabledService;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.TestcontainersConfiguration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.LogType;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@TestInstance(Lifecycle.PER_CLASS)
@Slf4j
class SimpleAwsIT {
	static final String LOCAL_STACK_VERSION = System.getProperty("localstack.version", "3.6");
	static final DockerImageName LOCAL_STACK_IMAGE = DockerImageName
			.parse("localstack/localstack:" + LOCAL_STACK_VERSION);
	static final String REUSE_CF = TestcontainersConfiguration.getInstance()
			.getEnvVarOrUserProperty("testcontainers.reuse.enable", "");

	static final String LAMBDA_HANDLER = SimpleAws.class.getName() + "::handleRequest";
	static final String LAMBDA_FUNCTION = "simple-aws";
	static final Region LAMBDA_REGION = Region.EU_CENTRAL_1;

	static final String DEPLOY_SCRIPT = """
			#!/bin/bash

			set -e
			set -o pipefail

			lambda_handler=${1?missing lambda handler as first argument}
			lambda_function=${2?missing lambda function as second argument}
			# the default is waiting for its region
			lambda_region=${3:-eu-east-1}

			${DEPLOY_VERBOSE:-false} && set -x

			awslocal sqs create-queue \
					--region ${lambda_region} \
					--queue-name ${lambda_function}

			if awslocal lambda update-function-code \
					--region ${lambda_region} \
					--function-name ${lambda_function} \
					--zip-file fileb:///tmp/work/build/distributions/${lambda_function}.zip; then

				awslocal lambda wait function-updated-v2 \
						--region ${lambda_region} \
						--function-name ${lambda_function}

			else
				awslocal lambda create-function \
						--region ${lambda_region} \
						--function-name ${lambda_function} \
						--zip-file fileb:///tmp/work/build/distributions/${lambda_function}.zip \
						--role arn:aws:iam::000000000000:role/${lambda_function} \
						--handler ${lambda_handler} \
						--runtime java17 \
						--memory-size 256 \
						--timeout 30000 \
						--publish

				awslocal lambda wait function-active-v2 \
						--function-name ${lambda_function} \
						--region ${lambda_region}

				awslocal lambda create-event-source-mapping \
						--event-source-arn arn:aws:sqs:${lambda_region}:000000000000:${lambda_function} \
						--function-name ${lambda_function} \
						--region ${lambda_region}
			fi
			""";

	private LocalStackContainer container;

	@SuppressWarnings("resource")
	@BeforeAll
	void beforeAll() {
		this.container = new LocalStackContainer(LOCAL_STACK_IMAGE)
				.withReuse("true".equals(REUSE_CF))
				.withFileSystemBind(System.getProperty("user.dir"), "/tmp/work", BindMode.READ_ONLY)
				.withCopyToContainer(Transferable.of(DEPLOY_SCRIPT, 0700), "/tmp/deploy")
				.withEnv("DEPLOY_VERBOSE", "true")
				.withServices(Service.CLOUDWATCHLOGS, Service.LAMBDA, Service.SQS);

		this.container.start();
		this.container.followOutput(f -> LOG.info("{}", f.getUtf8StringWithoutLineEnding()));

		exec("/tmp/deploy", LAMBDA_HANDLER, LAMBDA_FUNCTION, LAMBDA_REGION.id());
	}

	@AfterAll
	void afterAll() {
		if (!this.container.isShouldBeReused()) {
			this.container.stop();
			this.container.close();
		}
	}

	void exec(String... command) {
		LOG.atInfo().addArgument(() -> Stream.of(command).collect(joining(" "))).log("Running {}");

		try {
			final var result = this.container.execInContainer(command);

			final String stdout = result.getStdout();
			if (isNotEmpty(stdout)) {
				LOG.info("\n{}", stdout);
			}

			final String stderr = result.getStderr();
			if (isNotEmpty(stderr)) {
				LOG.error("\n{}", stderr);
			}

			assertThat(result.getExitCode()).isZero();
		} catch (IOException | InterruptedException e) {
			LOG.error("Execution failed", e);

			fail(e.getMessage());
		}
	}

	@Test
	void sqsBatch() {
		final var client = configure(SqsClient.builder(), LocalStackContainer.Service.SQS).build();

		final var queueUrl = client
				.getQueueUrl(GetQueueUrlRequest.builder().queueName(LAMBDA_FUNCTION).build())
				.queueUrl();

		final var request = SendMessageRequest.builder()
				.queueUrl(queueUrl)
				.messageBody("HIHI")
				.build();

		final var response = client.sendMessage(request);

		LOG.info("{}", response);
	}

	@Test
	void lambdaInvoke() throws IOException {
		final var client = configure(LambdaClient.builder(), LocalStackContainer.Service.LAMBDA).build();
		final var request = InvokeRequest.builder()
				.functionName(LAMBDA_FUNCTION)
				.logType(LogType.TAIL)
				.payload(SdkBytes.fromByteArrayUnsafe(resourceToByteArray("/sqs-event.json")))
				.build();

		final var response = client.invoke(request);
		final var payload = response.payload().asUtf8String();

		LOG.info("payload {}", payload);

		assertThatJson(payload).isArray().containsExactly("message-1", "message-2");
	}

	private <B extends AwsClientBuilder<B, C>, C> B configure(B bld, EnabledService service) {
		return bld
				.endpointOverride(this.container.getEndpointOverride(service))
				.credentialsProvider(
						StaticCredentialsProvider.create(
								AwsBasicCredentials.create(
										this.container.getAccessKey(),
										this.container.getSecretKey())))
				.region(LAMBDA_REGION);
	}
}
