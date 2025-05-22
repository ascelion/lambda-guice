package com.ascelion.s3;

import static com.ascelion.guice.internal.GuiceUtils.GENERATED_OUTPUT_DIRECTORY_PARAM;
import static com.ascelion.guice.internal.GuiceUtils.configurationEnvName;
import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assumptions.assumingThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.org.webcompere.systemstubs.SystemStubs.withEnvironmentVariable;

import com.ascelion.guice.jupiter.GuiceBootExtension;
import com.ascelion.guice.test.BindProducer;
import com.google.inject.Inject;
import com.google.inject.Injector;

import java.lang.reflect.*;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

import lombok.SneakyThrows;
import org.apache.commons.lang3.reflect.InheritanceUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.model.S3Request.Builder;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

@ExtendWith(MockitoExtension.class)
@ExtendWith(SystemStubsExtension.class)
@ExtendWith(GuiceBootExtension.class)
class S3GlueModuleTest {
	private static final String TEST_BUCKET = "client-bucket";

	static class Service1 {
		private final S3Client cs1;

		@Inject
		@BucketName("cs2")
		private S3Client cs2;

		private S3Client cs3;

		@Inject
		@BucketName(TEST_BUCKET)
		private S3Client client;

		@Inject
		Service1(@BucketName("CS1") S3Client cs1) {
			this.cs1 = cs1;
		}

		@Inject
		void setCs3(@BucketName("cs3") S3Client cs3) {
			this.cs3 = cs3;
		}
	}

	@SystemStub
	private final EnvironmentVariables environ = withEnvironmentVariable(
			configurationEnvName(GENERATED_OUTPUT_DIRECTORY_PARAM), "build/generated/proxies");

	@Inject
	private Injector injector;

	@BindProducer
	@Mock
	private S3Client s3Client;

	@Test
	void instance() {
		final var service = this.injector.getInstance(Service1.class);

		assertAll(
				() -> assertThat(service).isNotNull().hasNoNullFieldsOrProperties(),
				() -> assertThat(service.cs1).isNotNull()
						.extracting(S3GlueModule.BUCKET_FIELD, S3GlueModule.DELEGATE_FIELD)
						.contains("CS1", this.s3Client),
				() -> assertThat(service.cs2).isNotNull()
						.extracting(S3GlueModule.BUCKET_FIELD, S3GlueModule.DELEGATE_FIELD)
						.contains("cs2", this.s3Client),
				() -> assertThat(service.cs3).isNotNull()
						.extracting(S3GlueModule.BUCKET_FIELD, S3GlueModule.DELEGATE_FIELD)
						.contains("cs3", this.s3Client),
				() -> {});
	}

	@Test
	void client() {
		final var service = this.injector.getInstance(Service1.class);
		final var transformer = (ResponseTransformer<GetObjectResponse, String>) (response, stream) -> null;
		final var path = Path.of(".");

		service.client.getObject(bld -> {});
		service.client.getObject(bld -> {}, path);
		service.client.getObject(bld -> {}, transformer);

		service.client.getObject(GetObjectRequest.Builder::build);
		service.client.getObject(GetObjectRequest.Builder::build, path);
		service.client.getObject(GetObjectRequest.Builder::build, transformer);

		final var requestArg = ArgumentCaptor.forClass(GetObjectRequest.class);
		final var pathArg = ArgumentCaptor.forClass(Path.class);
		final var transformerArg = ArgumentCaptor.forClass(ResponseTransformer.class);

		verify(this.s3Client, times(2)).getObject(requestArg.capture());
		verify(this.s3Client, times(2)).getObject(requestArg.capture(), pathArg.capture());
		verify(this.s3Client, times(2)).getObject(requestArg.capture(), transformerArg.capture());

		assertThat(requestArg.getAllValues())
				.hasSize(6)
				.allMatch(arg -> TEST_BUCKET.equals(arg.bucket()));
		assertThat(pathArg.getAllValues())
				.hasSize(2)
				.allSatisfy(arg -> assertThat(arg).isSameAs(path));
		assertThat(transformerArg.getAllValues())
				.hasSize(2)
				.allSatisfy(arg -> assertThat(arg).isSameAs(transformer));
	}

	static List<Arguments> clientMethods() {
		final Map<String, List<Method>> methods = Stream.of(S3Client.class.getDeclaredMethods())
				.filter(m -> !Modifier.isStatic(m.getModifiers()))
				.collect(groupingBy(S3GlueModuleTest::methodKey, TreeMap::new, toList()));
		final var arguments = new ArrayList<Arguments>();

		for (final var ent : methods.entrySet()) {
			final Method met = ent.getValue().stream()
					.max(comparingInt(m -> InheritanceUtils.distance(m.getReturnType(), Object.class)))
					.get();

			arguments.add(Arguments.of(met, ent.getKey()));
		}

		return arguments;
	}

	static String methodKey(Method method) {
		return format("%s(%s)", method.getName(),
				Stream.of(method.getParameterTypes()).map(Class::getSimpleName).collect(joining(",")));
	}

	@ParameterizedTest(name = "{1}")
	@MethodSource("clientMethods")
	void clientMethods(Method method, String caseName) {
		final int[] count = new int[1];
		final String[] bucket = new String[1];

		this.s3Client = (S3Client) Proxy.newProxyInstance(currentThread().getContextClassLoader(),
				new Class[] { S3Client.class }, (InvocationHandler) (proxy, m, args) -> {
					count[0]++;

					if (args == null || args.length < 1) {
						return null;
					}

					if (args[0] instanceof final S3Request req) {
						bucket[0] = req.getValueForField(S3GlueModule.BUCKET_SDK_FIELD, String.class).orElse(null);
					}

					return null;
				});

		final Object[] arguments = Stream.of(method.getGenericParameterTypes()).map(this::buildArgument).toArray();

		final var service = this.injector.getInstance(Service1.class);

		assertThatNoException().isThrownBy(() -> {
			method.invoke(service.client, arguments);
		});
		assertThat(count[0]).isEqualTo(1);
		assumingThat(bucket[0] != null, () -> assertThat(bucket[0]).isEqualTo(TEST_BUCKET));
	}

	@SneakyThrows
	private Object buildArgument(Type type) {
		if (type instanceof final Class cls) {
			if (S3Request.class.isAssignableFrom(cls)) {
				final S3Request.Builder bld = (Builder) cls.getMethod("builder").invoke(null);

				return bld.build();
			}
			if (Path.class.isAssignableFrom(cls)) {
				return Path.of(".");
			}
		}
		if (type instanceof final ParameterizedType ptype) {
			if (Consumer.class == ptype.getRawType()) {
				return new Consumer<S3Request.Builder>() {

					@Override
					public void accept(Builder t) {
						t.build();
					}
				};
			}
			if (ResponseTransformer.class == ptype.getRawType()) {
				return null;
			}
		}

		return null;
	}

}
