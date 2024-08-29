package com.ascelion.demo;

import static com.ascelion.demo.Application.INVOCATIONS;
import static org.mockito.Mockito.*;

import com.ascelion.guice.jupiter.GuiceBootExtension;
import com.ascelion.guice.test.BindProducer;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@ExtendWith(GuiceBootExtension.class)
class ApplicationTest {

	@BindProducer
	static AppContext ctx = mock(AppContext.class);

	@Inject
	Application app;

	@BeforeAll
	static void setUpClass() {
		when(ctx.value()).thenReturn(-1);
	}

	@Test
	void run() {
		verify(ctx, times(INVOCATIONS.size())).value();
	}
}
