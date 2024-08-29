package com.ascelion.demo;

import com.google.inject.*;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ArgumentsModule extends AbstractModule {

	static final class StringList extends TypeLiteral<List<String>> {
	}

	private final String[] args;

	public ArgumentsModule(String... args) {
		this.args = args.clone();
	}

	@Override
	public void configure() {
		LOG.trace("Binding arguments");

		bind(new StringList())
				.annotatedWith(Arguments.class)
				.toProvider(() -> List.of(this.args)).in(Scopes.SINGLETON);
		bind(String[].class)
				.annotatedWith(Arguments.class)
				.toProvider(this.args::clone).in(Scopes.SINGLETON);
	}
}
