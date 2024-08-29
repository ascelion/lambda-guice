package com.ascelion.demo;

import static com.amazonaws.services.lambda.runtime.serialization.events.LambdaEventSerializers.serializerFor;
import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.fail;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.TestcontainersConfiguration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.LogType;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

@TestInstance(Lifecycle.PER_CLASS)
@Slf4j
class ApplicationIT {
	static final String LAMBDA_FUNCTION = "simple-aws";
	static final String LAMBDA_HANDLER = Application.class.getName() + "::handleRequest";
	static final String LOCAL_STACK_VERSION = System.getProperty("localstack.version", "3.6");
	static final DockerImageName LOCAL_STACK_IMAGE = DockerImageName
			.parse("localstack/localstack:" + LOCAL_STACK_VERSION);
	static final String REUSE_CF = TestcontainersConfiguration.getInstance()
			.getEnvVarOrUserProperty("testcontainers.reuse.enable", "");
	static final String REGION = "eu-central-1";

	private LocalStackContainer container;

	@SuppressWarnings("resource")
	@BeforeAll
	void beforeAll() {
		this.container = new LocalStackContainer(LOCAL_STACK_IMAGE)
				.withReuse(false /* "true".equals(REUSE_CF) */)
				.withFileSystemBind(System.getProperty("user.dir"), "/tmp/work", BindMode.READ_ONLY)
				.withEnv("LAMBDA_FUNCTION", LAMBDA_FUNCTION)
				.withServices(Service.LAMBDA, Service.SQS);

		this.container.start();
		this.container.followOutput(f -> LOG.debug("{}", f.getUtf8StringWithoutLineEnding()));

		exec("awslocal", "sqs", "create-queue", "--queue-name", LAMBDA_FUNCTION, "--region", REGION);
		exec("awslocal", "lambda", "create-function",
				"--function-name", LAMBDA_FUNCTION, "--region", REGION,
				"--role", "arn:aws:iam::000000000000:role/" + LAMBDA_FUNCTION,
				"--zip-file", "fileb:///tmp/work/build/distributions/" + LAMBDA_FUNCTION + ".zip",
				"--handler", LAMBDA_HANDLER,
				"--runtime", "java17",
				"--memory-size", "256",
				"--timeout", "30000",
				"--publish");
		exec("awslocal", "lambda", "wait", "function-active-v2",
				"--function-name", LAMBDA_FUNCTION, "--region", REGION);
		exec("awslocal", "lambda", "create-event-source-mapping",
				"--event-source-arn", format("arn:aws:sqs:%s:000000000000:%s", REGION, LAMBDA_FUNCTION),
				"--function-name", LAMBDA_FUNCTION, "--region", REGION);
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

			LOG.info(result.getStdout());
			LOG.info(result.getStderr());

			if (result.getExitCode() != 0) {
				fail("Exit code " + result.getExitCode());
			}
		} catch (IOException | InterruptedException e) {
			LOG.error("Execution failed", e);

			fail(e.getMessage());
		}
	}

	@Test
	void sqsBatch() {
		final var sqsClient = SqsClient.builder()
				.endpointOverride(this.container.getEndpointOverride(LocalStackContainer.Service.SQS))
				.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
						this.container.getAccessKey(), this.container.getSecretKey())))
				.region(Region.of(REGION))
				.build();

		final GetQueueUrlResponse urlResponse = sqsClient
				.getQueueUrl(GetQueueUrlRequest.builder().queueName(LAMBDA_FUNCTION).build());

		final var request = SendMessageRequest.builder()
				.queueUrl(urlResponse.queueUrl())
				.messageBody("HIHI")
				.build();

		final var response = sqsClient.sendMessage(request);

		LOG.info("{}", response);
	}

	@Test
	void lambdaInvoke() {
		final var awsClient = LambdaClient.builder()
				.endpointOverride(this.container.getEndpointOverride(LocalStackContainer.Service.LAMBDA))
				.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
						this.container.getAccessKey(), this.container.getSecretKey())))
				.region(Region.of(REGION))
				.build();
		final var event = new SQSEvent();
		final var message = new SQSEvent.SQSMessage();

		message.setAwsRegion("eu-east-1");

		event.setRecords(List.of(message));

		final var inputBuf = new ByteArrayOutputStream();

		serializerFor(SQSEvent.class, currentThread().getContextClassLoader()).toJson(event, inputBuf);

		final var request = InvokeRequest.builder()
				.functionName(LAMBDA_FUNCTION)
				.logType(LogType.TAIL)
				.payload(SdkBytes.fromByteArrayUnsafe(inputBuf.toByteArray()))
				.build();

		final var response = awsClient.invoke(request);

		LOG.info("{}", response);
	}
}
