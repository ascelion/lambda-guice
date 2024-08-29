package com.ascelion.guice.jupiter;

import com.ascelion.guice.test.GuiceMockStubsModule;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;

public class GuiceBootExtension implements BeforeEachCallback {
	private static final Namespace ASC_GUICE = Namespace.create("com.ascelion.guice");

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		context.getStore(ASC_GUICE)
				.getOrComputeIfAbsent(context.getRequiredTestInstance(),
						i -> new GuiceMockStubsModule(i, SystemStub.class), GuiceMockStubsModule.class)
				.setUp();
	}
}
