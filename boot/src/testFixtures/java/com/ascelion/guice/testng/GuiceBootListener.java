package com.ascelion.guice.testng;

import com.ascelion.guice.test.GuiceMockStubsModule;

import java.util.IdentityHashMap;
import java.util.Map;

import org.testng.*;
import uk.org.webcompere.systemstubs.testng.SystemStub;

public class GuiceBootListener implements IInvokedMethodListener {
	private final Map<Object, GuiceMockStubsModule> mocked = new IdentityHashMap<>();

	@Override
	public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
		this.mocked.computeIfAbsent(testResult.getInstance(), i -> new GuiceMockStubsModule(i, SystemStub.class))
				.setUp();
	}
}
