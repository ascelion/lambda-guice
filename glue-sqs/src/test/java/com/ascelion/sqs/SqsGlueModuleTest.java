package com.ascelion.sqs;

import com.ascelion.guice.jupiter.GuiceBootExtension;
import com.ascelion.guice.test.BindProducer;
import com.google.inject.Injector;

import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsClient;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

@ExtendWith(MockitoExtension.class)
@ExtendWith(SystemStubsExtension.class)
@ExtendWith(GuiceBootExtension.class)
class SqsGlueModuleTest {

	@RequiredArgsConstructor(onConstructor_ = @Inject)
	static class Service {
		private final SqsClient client;
	}

	@BindProducer
	@Mock
	private SqsClient client;

	@Inject
	private Injector injector;

	@Test
	void run() {

	}

}
