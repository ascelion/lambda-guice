package com.ascelion.dynamodb;

import static com.ascelion.guice.ModulePriorities.PROVIDER_MODULE_PRIORITY;
import static com.ascelion.guice.internal.GuiceUtils.toKebabCase;
import static io.leangen.geantyref.GenericTypeReflector.getExactFieldType;
import static io.leangen.geantyref.GenericTypeReflector.getExactParameterTypes;
import static java.util.Optional.ofNullable;

import com.ascelion.guice.internal.BootstrapContext;
import com.ascelion.guice.internal.GuiceUtils;
import com.google.inject.*;
import com.google.inject.Key;

import java.lang.reflect.ParameterizedType;
import java.util.HashSet;
import java.util.Set;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Priority(PROVIDER_MODULE_PRIORITY)
@Slf4j
public class DynamoDbModule extends AbstractModule {
	@Inject
	private BootstrapContext context;

	@Override
	protected void configure() {
		final Set<ParameterizedType> tables = new HashSet<>();
		final var injections = this.context.getClassesWithInjectedType(DynamoDbTable.class);

		for (final var injection : injections) {
			for (final var executable : injection.getExecutables()) {
				for (final var type : getExactParameterTypes(executable, injection.getType())) {
					if (type instanceof final ParameterizedType pt && pt.getRawType() == DynamoDbTable.class) {
						tables.add(pt);
					}
				}

			}
			for (final var field : injection.getFields()) {
				final var genType = (ParameterizedType) getExactFieldType(field, injection.getType());

				tables.add(genType);
			}
		}

		for (final var table : tables) {
			LOG.debug("Binding {} to Scopes.SINGLETON", table);

			bind(Key.get(table)).toProvider(createProvider(table)).in(Scopes.SINGLETON);
		}
	}

	private Provider createProvider(ParameterizedType type) {
		final var entType = (Class<?>) type.getActualTypeArguments()[0];
		final var cltProvider = getProvider(DynamoDbEnhancedClient.class);

		return () -> {
			final var clt = cltProvider.get();
			final var schema = TableSchema.fromClass(entType);
			final var tableName = tableName(entType);

			return clt.table(tableName, schema);
		};
	}

	private static String tableName(final Class<?> entType) {
		return ofNullable(entType.getAnnotation(DynamoDbTableName.class))
				.map(DynamoDbTableName::value)
				.map(StringUtils::trimToNull)
				.flatMap(GuiceUtils::tryExternalConfiguration)
				.orElseGet(() -> toKebabCase(entType.getSimpleName()));
	}

	@Provides
	static DynamoDbClient dynamoDbClient(Region region) {
		return DynamoDbClient.builder()
				.region(region)
				.credentialsProvider(EnvironmentVariableCredentialsProvider.create())
				.build();
	}

	@Provides
	static DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient ddbClient) {
		return DynamoDbEnhancedClient.builder().dynamoDbClient(ddbClient).build();
	}
}
