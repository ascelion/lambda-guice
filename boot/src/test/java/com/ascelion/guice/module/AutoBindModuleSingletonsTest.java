package com.ascelion.guice.module;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Slf4j
class AutoBindModuleSingletonsTest extends AbstractAutoModuleTest {

	@com.google.inject.Singleton
	static class Singleton1 {

		int count;

		@PostConstruct
		public void init() {
			this.count++;
		}
	}

	interface Singleton2 {
		int getCount();

		Singleton1 getSingleton1();

		void init();
	}

	@com.google.inject.Singleton
	static class Singleton2Impl implements Singleton2 {

		@Getter
		int count;

		@Inject
		@Getter
		Singleton1 singleton1;

		@Override
		@PostConstruct
		public void init() {
			this.count++;
		}
	}

	@jakarta.inject.Singleton
	static class Singleton3 {

		int count;

		@Inject
		Singleton2 singleton2;

		@PostConstruct
		void init() {
			this.count++;
		}
	}

	static class Singleton4 {
		int count;

		@Inject
		Singleton3 singleton3;

		Singleton4(int count) {
			this.count = count;
		}

		@PostConstruct
		void init() {
			this.count++;
		}
	}

	static class Singleton4Provider implements Provider<Singleton4> {
		int count;

		@Override
		@com.google.inject.Singleton
		public Singleton4 get() {
			return new Singleton4(this.count);
		}

		@PostConstruct
		void init() {
			this.count++;
		}
	}

	static class Singleton5 {
		int count;

		Singleton4 singleton4;

		@Inject
		Singleton3 singleton3;

		Singleton5(int count, Singleton4 singleton4) {
			this.count = count;
			this.singleton4 = singleton4;
		}

		@PostConstruct
		void init() {
			this.count++;
		}
	}

	static class Singleton5Producer {
		int count;

		@Produces
		@jakarta.inject.Singleton
		Singleton5 create(Singleton4 singleton4) {
			return new Singleton5(this.count, singleton4);
		}

		Singleton5 dontCreate() {
			throw new UnsupportedOperationException();
		}

		@PostConstruct
		void init() {
			this.count++;
		}
	}

	@jakarta.inject.Singleton
	static class Cycleton1 {
		@Inject
		Cycleton2 pair;

		int count;

		@PostConstruct
		void init() {
			this.count++;
		}
	}

	@com.google.inject.Singleton
	static class Cycleton2 {
		@Inject
		Cycleton1 pair;

		int count;

		@PostConstruct
		void init() {
			this.count++;
		}
	}

	@ParameterizedTest
	@CsvSource({
			"false,false,false",
			"false,false,true",
			"false,true,false",
			"false,true,true",
			"true,false,false",
			"true,false,true",
			"true,true,false",
			"true,true,true",
	})
	void instance(boolean explicitBinding, boolean childInjector, boolean postConstruct) {
		final var inj = createInjector(explicitBinding, childInjector, postConstruct);

		final var singleton1 = inj.getInstance(Singleton1.class);
		final var singleton2 = inj.getInstance(Singleton2.class);
		final var singleton3 = inj.getInstance(Singleton3.class);
		final var singleton4 = inj.getInstance(Singleton4.class);
		final var singleton5 = inj.getInstance(Singleton5.class);

		final var cycleton1 = inj.getInstance(Cycleton1.class);
		final var cycleton2 = inj.getInstance(Cycleton2.class);

		final var count1 = postConstruct ? 1 : 0;
		final var count2 = postConstruct ? 2 : 0;

		assertAll(
				() -> assertThat(singleton1).extracting("count").isEqualTo(count1),

				() -> assertThat(singleton2).extracting("count").isEqualTo(count1),
				() -> assertThat(singleton2).extracting("singleton1").isSameAs(singleton1),
				() -> assertThat(singleton2).extracting("singleton1.count").isEqualTo(count1),

				() -> assertThat(singleton3).extracting("count").isEqualTo(count1),
				() -> assertThat(singleton3).extracting("singleton2").isSameAs(singleton2),
				() -> assertThat(singleton3).extracting("singleton2.count").isEqualTo(count1),
				() -> assertThat(singleton3).extracting("singleton2.singleton1.count").isEqualTo(count1),

				() -> assertThat(singleton4).extracting("count").isEqualTo(count2),
				() -> assertThat(singleton4).extracting("singleton3").isSameAs(singleton3),
				() -> assertThat(singleton4).extracting("singleton3.count").isEqualTo(count1),
				() -> assertThat(singleton4).extracting("singleton3.singleton2.count").isEqualTo(count1),
				() -> assertThat(singleton4).extracting("singleton3.singleton2.singleton1.count").isEqualTo(count1),

				() -> assertThat(singleton5).extracting("count").isEqualTo(count2),
				() -> assertThat(singleton5).extracting("singleton4").isSameAs(singleton4),
				() -> assertThat(singleton5).extracting("singleton4.count").isEqualTo(count2),
				() -> assertThat(singleton5).extracting("singleton4.singleton3.count").isEqualTo(count1),
				() -> assertThat(singleton5).extracting("singleton4.singleton3.singleton2.count").isEqualTo(count1),
				() -> assertThat(singleton5).extracting("singleton4.singleton3.singleton2.singleton1.count")
						.isEqualTo(count1),

				() -> assertThat(cycleton1).extracting("count").isEqualTo(count1),
				() -> assertThat(cycleton1).extracting("pair").isSameAs(cycleton2),
				() -> assertThat(cycleton1).extracting("pair.count").isEqualTo(count1),
				() -> assertThat(cycleton1).extracting("pair.pair").isSameAs(cycleton1),

				() -> assertThat(cycleton2).extracting("count").isEqualTo(count1),
				() -> assertThat(cycleton2).extracting("pair").isSameAs(cycleton1),
				() -> assertThat(cycleton2).extracting("pair.count").isEqualTo(count1),
				() -> assertThat(cycleton2).extracting("pair.pair").isSameAs(cycleton2),

				() -> {});
	}

	static class Shell {
		@Inject
		Singleton1 singleton1;
		@Inject
		Singleton2 singleton2;
		@Inject
		Singleton3 singleton3;
		@Inject
		Singleton4 singleton4;
		@Inject
		Singleton5 singleton51;
		@Inject
		Singleton5 singleton52;
	}

	@ParameterizedTest
	@CsvSource({
			"false,false,false,false",
			"false,false,false,true",
			"false,false,true,false",
			"false,false,true,true",
			"false,true,false,false",
			"false,true,false,true",
			"false,true,true,false",
			"false,true,true,true",
			"true,false,false,false",
			"true,false,false,true",
			"true,false,true,false",
			"true,false,true,true",
			"true,true,false,false",
			"true,true,false,true",
			"true,true,true,false",
			"true,true,true,true",
	})
	void nested(boolean instantiate, boolean explicitBinding, boolean childInjector, boolean postConstruct) {
		final var inj = createInjector(explicitBinding, childInjector, postConstruct);

		final Shell shell;

		if (instantiate) {
			shell = injectMembers(inj, new Shell());
		} else {
			shell = inj.getInstance(Shell.class);
		}

		final var count1 = postConstruct ? 1 : 0;
		final var count2 = postConstruct ? 2 : 0;

		assertAll(
				() -> assertThat(shell).extracting("singleton1.count").isEqualTo(count1),

				() -> assertThat(shell).extracting("singleton2.count").isEqualTo(count1),
				() -> assertThat(shell).extracting("singleton2.singleton1").isSameAs(shell.singleton1),
				() -> assertThat(shell).extracting("singleton2.singleton1.count").isEqualTo(count1),

				() -> assertThat(shell).extracting("singleton3.count").isEqualTo(count1),
				() -> assertThat(shell).extracting("singleton3.singleton2").isSameAs(shell.singleton2),
				() -> assertThat(shell).extracting("singleton3.singleton2.count").isEqualTo(count1),
				() -> assertThat(shell).extracting("singleton3.singleton2.singleton1.count").isEqualTo(count1),

				() -> assertThat(shell).extracting("singleton4.count").isEqualTo(count2),
				() -> assertThat(shell).extracting("singleton4.singleton3").isSameAs(shell.singleton3),
				() -> assertThat(shell).extracting("singleton4.singleton3.count").isEqualTo(count1),
				() -> assertThat(shell).extracting("singleton4.singleton3.singleton2.count").isEqualTo(count1),
				() -> assertThat(shell).extracting("singleton4.singleton3.singleton2.singleton1.count")
						.isEqualTo(count1),

				() -> assertThat(shell).extracting("singleton51.count").isEqualTo(count2),
				() -> assertThat(shell).extracting("singleton51.singleton4").isSameAs(shell.singleton4),
				() -> assertThat(shell).extracting("singleton51.singleton4.count").isEqualTo(count2),
				() -> assertThat(shell).extracting("singleton51.singleton4.singleton3.count").isEqualTo(count1),
				() -> assertThat(shell).extracting("singleton51.singleton4.singleton3.singleton2.count")
						.isEqualTo(count1),
				() -> assertThat(shell).extracting("singleton51.singleton4.singleton3.singleton2.singleton1.count")
						.isEqualTo(count1),

				() -> assertThat(shell.singleton51).isSameAs(shell.singleton52),

				() -> {});
	}
}
