package com.ascelion.s3;

import static com.ascelion.guice.ModulePriorities.PROVIDER_MODULE_PRIORITY;
import static com.ascelion.guice.internal.GuiceUtils.*;
import static java.lang.Thread.currentThread;
import static net.bytebuddy.matcher.ElementMatchers.*;

import com.ascelion.guice.internal.BootstrapContext;
import com.google.inject.*;

import java.io.IOException;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.CreationException;
import jakarta.enterprise.inject.Vetoed;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.implementation.*;
import net.bytebuddy.implementation.bind.annotation.*;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Request;

@Priority(PROVIDER_MODULE_PRIORITY)
@Slf4j
public class S3GlueModule extends AbstractModule {
	static final String BUCKET_SDK_FIELD = "Bucket";
	static final String BUCKET_FIELD = "bucket";
	static final String DELEGATE_FIELD = "delegate";

	private static final Constructor<?> OBJECT_CT = Object.class.getConstructors()[0];

	private final String generatedDirectory = externalConfiguration(GENERATED_OUTPUT_DIRECTORY_PARAM).orElse(null);

	@Inject
	private BootstrapContext context;

	@Override
	protected void configure() {
		final Set<BucketName> names = new HashSet<>();
		final var injections = this.context.getClassesWithInjectedType(S3Client.class);

		for (final var injection : injections) {
			for (final var executable : injection.getExecutables()) {
				for (final var param : executable.getParameters()) {
					if (param.getType() != S3Client.class) {
						continue;
					}

					collectBucketName(names, param);
				}
			}
			for (final var field : injection.getFields()) {
				collectBucketName(names, field);
			}
		}

		for (final var name : names) {
			LOG.debug("Binding S3Client for bucket {} to Scopes.SINGLETON", name);

			bind(Key.get(S3Client.class, name))
					.toProvider(createProvider(name.value()));
		}
	}

	private void collectBucketName(Collection<BucketName> names, final AnnotatedElement element) {
		final BucketName name = element.getAnnotation(BucketName.class);

		if (name == null) {
			throw new IllegalStateException("Cannot find annotation @BucketName on " + element);
		}

		names.add(name);
	}

	private Provider<S3Client> createProvider(String name) {
		final var injectorP = getProvider(Injector.class);

		final var unloaded = new ByteBuddy()
				.with(new NamingStrategy.Suffixing("Proxy"))
				.subclass(S3Client.class)
				.annotateType(Vetoed.Literal.INSTANCE)

				.defineField(DELEGATE_FIELD, S3Client.class, Modifier.PRIVATE | Modifier.FINAL)
				.defineField(BUCKET_FIELD, String.class, Modifier.PRIVATE | Modifier.FINAL)

				.defineConstructor(Modifier.PUBLIC)
				.withParameters(S3Client.class, String.class)
				.intercept(MethodCall.invoke(OBJECT_CT)
						.andThen(FieldAccessor.ofField(DELEGATE_FIELD).setsArgumentAt(0))
						.andThen(FieldAccessor.ofField(BUCKET_FIELD).setsArgumentAt(1)))

				.method(isDeclaredBy(S3Client.class).and(takesArgument(0, isSubTypeOf(S3Request.class))))
				.intercept(MethodDelegation.to(this))

				.method(isDeclaredBy(S3Client.class).and(takesArguments(0)))
				.intercept(MethodDelegation.toField(DELEGATE_FIELD))

				.make();

		if (this.generatedDirectory != null) {
			final var path = Path.of(this.generatedDirectory,
					unloaded.getTypeDescription().getInternalName() + ".class");

			try {
				path.getParent().toFile().mkdirs();

				Files.write(path, unloaded.getBytes());
			} catch (final IOException e) {
				LOG.warn("{}: {}", path, e);
			}
		}

		final Class<? extends S3Client> loaded = unloaded
				.load(currentThread().getContextClassLoader())
				.getLoaded();

		final Constructor<? extends S3Client> constructor;

		try {
			constructor = loaded.getConstructor(S3Client.class, String.class);
		} catch (NoSuchMethodException | SecurityException e) {
			throw new CreationException("Cannot find constructor of S3ClientService", e);
		}

		return () -> {
			final Injector injector = injectorP.get();
			final S3Client client = injector.getInstance(S3Client.class);

			try {
				return constructor.newInstance(client, tryExternalConfiguration(name).orElse(name));
			} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
				throw new ProvisionException("Cannot create instance of S3ClientService", e);
			}
		};
	}

	@SuppressWarnings("java:S112")
	@RuntimeType
	public Object intercept(
			@FieldValue(S3GlueModule.DELEGATE_FIELD) S3Client delegate,
			@FieldValue(S3GlueModule.BUCKET_FIELD) String bucketName,
			@Origin Method method,
			@AllArguments Object[] args)
			throws Throwable {

		final S3Request request = (S3Request) args[0];

		args[0] = request.sdkFields().stream()
				.filter(f -> BUCKET_SDK_FIELD.equals(f.memberName()))
				.findAny()
				.map(f -> rebuild(request, f, bucketName))
				.orElse(request);

		return method.invoke(delegate, args);
	}

	private static <R extends S3Request> R rebuild(R request, SdkField<?> f, String bucketName) {
		final var bld = request.toBuilder();

		f.set(bld, bucketName);

		return (R) bld.build();
	}

	@Provides
	static S3Client s3Client(Region region) {
		return S3Client.builder()
				.region(region)
				.credentialsProvider(EnvironmentVariableCredentialsProvider.create())
				.build();
	}
}
