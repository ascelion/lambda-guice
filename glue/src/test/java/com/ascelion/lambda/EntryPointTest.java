package com.ascelion.lambda;

import static com.ascelion.lambda.EntryPoint.RESOURCE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.IOException;
import java.util.concurrent.Callable;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class EntryPointTest {
	static class HandlerWithMethod1 {
		void invoke() {
			throw new UnsupportedOperationException();
		}
	}

	static class HandlerWithMethod2 {
		void invoke1() {
			throw new UnsupportedOperationException();
		}

		void invoke2() {
			throw new UnsupportedOperationException();
		}
	}

	static class HandlerCallable implements Callable<Void> {
		@Override
		public Void call() throws Exception {
			throw new UnsupportedOperationException();
		}
	}

	static class HandlerRunnable implements Runnable {

		@Override
		public void run() {
			throw new UnsupportedOperationException();
		}
	}

	static class HandlerCallableAndRunnable implements Callable<Void>, Runnable {
		@Override
		public Void call() throws Exception {
			throw new UnsupportedOperationException();
		}

		@Override
		public void run() {
			throw new UnsupportedOperationException();
		}
	}

	@ParameterizedTest
	@CsvSource({
			"HandlerWithMethod1,invoke",
			"HandlerWithMethod2,invoke2",
			"HandlerCallable,call",
			"HandlerRunnable,run",
			"HandlerCallableAndRunnable,call"
	})
	void goodHandles(String className, String methodName) throws IOException {
		try (final var cld = TestCaseClassLoader.forCase(className, RESOURCE_NAME)) {
			final EntryPoint info = EntryPoint.build(null);

			assertThat(info.clazz.getSimpleName()).isEqualTo(className);
			assertThat(info.method.getName()).isEqualTo(methodName);
		}
	}

	static class WithSameMethod {
		void invoke(int n) {
			throw new UnsupportedOperationException();
		}

		void invoke(String s) {
			throw new UnsupportedOperationException();
		}
	}

	static class WithNoMethod {
	}

	static class WithTwoMethods {
		void invoke1(int n) {
			throw new UnsupportedOperationException();
		}

		void invoke2(String s) {
			throw new UnsupportedOperationException();
		}
	}

	@ParameterizedTest
	@CsvSource({
			"WithNoMethod,any entry point",
			"WithSameMethod::,method name",
			"WithSameMethod,only one method",
			"WithSameMethod::invoke,only one method",
			"WithTwoMethods,only one method",
	})
	void badHandles1(String handler, String message) {
		final var hndl = handler.startsWith("::")
				? handler
				: getClass().getName() + "$" + handler;

		assertThatExceptionOfType(GuiceGlueException.class)
				.isThrownBy(() -> EntryPoint.build(hndl)).withMessageContaining(message);
	}

	@SuppressWarnings("java:S5778")
	@ParameterizedTest
	@CsvSource({
			"Nothing,instantiate",
			"::invoke,class name",
			"::,class name",
			"'',read META-INF",
	})
	void badHandles2(String handler, String message) {
		assertThatExceptionOfType(GuiceGlueException.class)
				.isThrownBy(() -> {
					try (final var cld = TestCaseClassLoader.forCase("no-service", "service-missing-marker")) {
						EntryPoint.build(handler);
					}
				}).withMessageContaining(message)

		;
	}
}
