package com.ascelion.lambda;

import com.amazonaws.services.lambda.runtime.Context;

import java.io.InputStream;
import java.io.OutputStream;

public interface LambdaRequest {
	InputStream getInput();

	OutputStream getOutput();

	Context getContext();
}
