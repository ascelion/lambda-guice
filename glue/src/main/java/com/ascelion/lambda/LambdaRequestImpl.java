package com.ascelion.lambda;

import com.amazonaws.services.lambda.runtime.Context;

import java.io.InputStream;
import java.io.OutputStream;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
final class LambdaRequestImpl implements LambdaRequest {
	final InputStream input;
	final OutputStream output;
	final Context context;
}
