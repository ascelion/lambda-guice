package com.ascelion.demo;

import static java.lang.Math.E;
import static java.lang.Math.PI;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.within;

import java.io.*;
import java.math.BigDecimal;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.EnabledService;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.TestcontainersConfiguration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.*;
import software.amazon.awssdk.services.lambda.model.Runtime;

@TestInstance(Lifecycle.PER_CLASS)
@Slf4j
class GlueServiceIT {
	static final String LOCAL_STACK_VERSION = System.getProperty("localstack.version", "3.6");
	static final DockerImageName LOCAL_STACK_IMAGE = DockerImageName
			.parse("localstack/localstack:" + LOCAL_STACK_VERSION);
	static final String REUSE_CF = TestcontainersConfiguration.getInstance()
			.getEnvVarOrUserProperty("testcontainers.reuse.enable", "");

	static final String LAMBDA_HANDLER = com.ascelion.lambda.GuiceGlueHandler.class.getName() + "::handleRequest";
	static final Region LAMBDA_REGION = Region.EU_CENTRAL_1;
	static final String LAMBDA_FUNCTION = "simple-glue";

	static final String REQUEST_JSON = """
			{
				"operands": [
					3.141592653589793,
					2.718281828459045
				]
			}
			""";

	private LocalStackContainer container;

	@SuppressWarnings("resource")
	@BeforeAll
	void beforeAll() {
		this.container = new LocalStackContainer(LOCAL_STACK_IMAGE)
				.withReuse("true".equals(REUSE_CF))
				.withEnv("DEPLOY_VERBOSE", "true")
				.withServices(Service.CLOUDWATCHLOGS, Service.LAMBDA, Service.SQS);

		this.container.start();
		this.container.followOutput(f -> LOG.info("{}", f.getUtf8StringWithoutLineEnding()));
	}

	<B extends AwsClientBuilder<B, C>, C> B configure(B bld, EnabledService service) {
		return bld
				.endpointOverride(this.container.getEndpointOverride(service))
				.credentialsProvider(
						StaticCredentialsProvider.create(
								AwsBasicCredentials.create(
										this.container.getAccessKey(),
										this.container.getSecretKey())))
				.region(LAMBDA_REGION);
	}

	LambdaClient deployLambda(String artifactId) {
		final var zipFile = format("build/distributions/%s.zip", artifactId);
		final SdkBytes zipData;

		try (final var fis = new FileInputStream(zipFile)) {
			zipData = SdkBytes.fromInputStream(fis);
		} catch (final IOException e) {
			LOG.error(zipFile, e);

			throw new UncheckedIOException(e);
		}

		final var client = configure(LambdaClient.builder(), Service.LAMBDA).build();
		final boolean found = client.listFunctions().functions().stream()
				.anyMatch(fc -> artifactId.equals(fc.functionName()));

		final SdkHttpResponse response;

		if (found) {
			response = client.updateFunctionCode(bld -> bld
					.functionName(artifactId)
					.zipFile(zipData)).sdkHttpResponse();
		} else {
			response = client.createFunction(bld -> bld
					.functionName(artifactId)
					.code(FunctionCode.builder().zipFile(zipData).build())
					.role(format("arn:aws:iam::000000000000:role/%s", artifactId))
					.handler(LAMBDA_HANDLER)
					.runtime(Runtime.JAVA17)
					.memorySize(256)
					.timeout(30)).sdkHttpResponse();
		}

		LOG.info("Deploying {}: status = {}, message = {}", artifactId, response.statusCode(), response.statusText());

		client.waiter().waitUntilFunctionActive(bld -> bld.functionName(artifactId));

		return client;
	}

	@Test
	void run() {
		final var client = deployLambda(LAMBDA_FUNCTION);
		final var request = InvokeRequest.builder()
				.functionName(LAMBDA_FUNCTION)
				.logType(LogType.TAIL)
				.payload(SdkBytes.fromString(REQUEST_JSON, UTF_8))
				.build();

		final var response = client.invoke(request);
		final var payload = response.payload().asUtf8String();

		LOG.info("payload {}", payload);

		assertThatJson(payload)
				.isNumber()
				.isCloseTo(BigDecimal.valueOf(PI + E), within(BigDecimal.valueOf(1E-10)));
	}
}
