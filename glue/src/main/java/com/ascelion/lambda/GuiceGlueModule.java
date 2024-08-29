package com.ascelion.lambda;

import static com.amazonaws.services.lambda.runtime.serialization.events.LambdaEventSerializers.isLambdaSupportedEvent;
import static com.amazonaws.services.lambda.runtime.serialization.events.LambdaEventSerializers.serializerFor;
import static java.lang.Thread.currentThread;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.ascelion.guice.GuiceScan;
import com.ascelion.guice.ModulePriorities;
import com.ascelion.guice.internal.BootstrapContext;
import com.ascelion.guice.request.RequestScope;
import com.ascelion.guice.request.RequestScoped;
import com.google.inject.*;

import io.github.classgraph.ClassInfo;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Priority(ModulePriorities.PROVIDER_MODULE_PRIORITY)
@GuiceScan(packageNames = GuiceGlueModule.LAMBDA_EVENTS_PACKAGE, classes = {
		RequestScope.class,
}, excluded = APIGatewayProxyResponseEvent.class)
@Slf4j
public final class GuiceGlueModule extends AbstractModule {
	static final String LAMBDA_EVENTS_PACKAGE = "com.amazonaws.services.lambda.runtime.events";

	@Inject
	private BootstrapContext context;

	@Override
	protected void configure() {
		this.context.getScanned()
				.getAllStandardClasses()
				.filter(ci -> LAMBDA_EVENTS_PACKAGE.equals(ci.getPackageName())
						|| ci.getPackageName().startsWith(LAMBDA_EVENTS_PACKAGE + "."))
				.stream()
				.map(ClassInfo::loadClass)
				.forEach(this::bindProviderFor);
	}

	private <T> void bindProviderFor(Class<T> cl) {
		if (isLambdaSupportedEvent(cl.getName())) {
			LOG.debug("Binding {} to lambda in scope Scopes.REQUEST", cl.getName());

			bind(cl).toProvider(createProvider(cl)).in(RequestScoped.class);
		}

		this.context.addBean(cl);
	}

	private <T> Provider<T> createProvider(Class<T> cl) {
		final var injectorP = getProvider(Injector.class);
		final var serializer = serializerFor(cl, currentThread().getContextClassLoader());

		return () -> {
			final LambdaRequest request = injectorP.get().getInstance(LambdaRequest.class);

			return serializer.fromJson(request.getInput());
		};
	}

	@Provides
	@RequestScoped
	LambdaRequest lambdaRequest() {
		throw new UnsupportedOperationException("No bean of type LambdaRequest request attached to scope REQUEST");
	}

	@Provides
	@RequestScoped
	Context lambdaContext(LambdaRequest request) {
		return request.getContext();
	}
}
