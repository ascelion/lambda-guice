package com.ascelion.lambda;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.amazonaws.services.lambda.runtime.serialization.PojoSerializer;

import java.io.*;

import org.apache.commons.io.IOUtils;

final class StringSerializer implements PojoSerializer<String> {

	@Override
	public String fromJson(InputStream input) {
		try {
			return IOUtils.toString(input, UTF_8);
		} catch (final IOException e) {
			throw new GuiceGlueException("Cannot read from input", e);
		}
	}

	@Override
	public String fromJson(String input) {
		return input;
	}

	@Override
	public void toJson(String value, OutputStream output) {
		try {
			IOUtils.write(value, output, UTF_8);
		} catch (final IOException e) {
			throw new GuiceGlueException("Cannot write to output", e);
		}
	}

}
