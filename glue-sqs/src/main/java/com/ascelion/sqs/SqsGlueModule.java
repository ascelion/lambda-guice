package com.ascelion.sqs;

import static com.ascelion.guice.ModulePriorities.PROVIDER_MODULE_PRIORITY;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import jakarta.annotation.Priority;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

@Priority(PROVIDER_MODULE_PRIORITY)
@Slf4j
public class SqsGlueModule extends AbstractModule {
	@Provides
	static SqsClient sqsClient(Region region) {
		return SqsClient.builder()
				.region(region)
				.credentialsProvider(EnvironmentVariableCredentialsProvider.create())
				.build();
	}
}
