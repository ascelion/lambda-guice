package com.ascelion.guice.module;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assumptions.assumingThat;

import com.google.inject.Injector;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Slf4j
class AutoBindModuleServicesTest extends AbstractAutoModuleTest {

	static class Service1 {

		int count;

		@PostConstruct
		public void init() {
			this.count++;
		}
	}

	interface Service2 {
		int getCount();

		Service1 getService1();

		void init();
	}

	static class Service2Impl implements Service2 {

		@Getter
		int count;

		@Inject
		@Getter
		Service1 service1;

		@Override
		@PostConstruct
		public void init() {
			this.count++;
		}
	}

	static class Service3 {

		int count;

		@Inject
		Service2 service2;

		@PostConstruct
		void init() {
			this.count++;
		}
	}

	static class Service4 {
		int count;

		@Inject
		Service3 service3;

		Service4(int count) {
			this.count = count;
		}

		@PostConstruct
		void init() {
			this.count++;
		}
	}

	static class Service4Provider implements Provider<Service4> {
		int count;

		@Override
		public Service4 get() {
			return new Service4(this.count);
		}

		@PostConstruct
		void init() {
			this.count++;
		}
	}

	static class Service5 {
		int count;

		Service4 service4;

		@Inject
		Service3 service3;

		Service5(int count, Service4 service4) {
			this.count = count;
			this.service4 = service4;
		}

		@PostConstruct
		void init() {
			this.count++;
		}
	}

	static class Service5Producer {
		int count;

		@Produces
		Service5 create(Service4 service4) {
			return new Service5(this.count, service4);
		}

		Service5 dontCreate() {
			throw new UnsupportedOperationException();
		}

		@PostConstruct
		void init() {
			this.count++;
		}
	}

	static class Cyclic1 {
		@Inject
		Cyclic2 pair;

		int count;

		@PostConstruct
		void init() {
			this.count++;
		}
	}

	static class Cyclic2 {
		@Inject
		Cyclic1 pair;

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

		final var service1 = inj.getInstance(Service1.class);
		final var service2 = inj.getInstance(Service2.class);
		final var service31 = inj.getInstance(Service3.class);
		final var service32 = new Service3();

		inj.injectMembers(service32);

		final var service4 = inj.getInstance(Service4.class);
		final var service5 = inj.getInstance(Service5.class);

		final var cyclic1 = inj.getInstance(Cyclic1.class);
		final var cyclic2 = inj.getInstance(Cyclic2.class);

		final var count1 = postConstruct ? 1 : 0;
		final var count2 = postConstruct ? 2 : 0;

		assertAll(
				() -> assertThat(service1).extracting("count").isEqualTo(count1),

				() -> assertThat(service2).extracting("count").isEqualTo(count1),
				() -> assertThat(service2).extracting("service1.count").isEqualTo(count1),

				() -> assertThat(service31).extracting("count").isEqualTo(count1),
				() -> assertThat(service31).extracting("service2.count").isEqualTo(count1),
				() -> assertThat(service31).extracting("service2.service1.count").isEqualTo(count1),

				() -> assertThat(service32).extracting("count").isEqualTo(count1),
				() -> assertThat(service32).extracting("service2.count").isEqualTo(count1),
				() -> assertThat(service32).extracting("service2.service1.count").isEqualTo(count1),

				() -> assertThat(service4).extracting("count").isEqualTo(count2),
				() -> assertThat(service4).extracting("service3.count").isEqualTo(count1),
				() -> assertThat(service4).extracting("service3.service2.count").isEqualTo(count1),
				() -> assertThat(service4).extracting("service3.service2.service1.count").isEqualTo(count1),

				() -> assertThat(service5).extracting("count").isEqualTo(count2),
				() -> assertThat(service5).extracting("service4.count").isEqualTo(count2),
				() -> assertThat(service5).extracting("service4.service3.count").isEqualTo(count1),
				() -> assertThat(service5).extracting("service4.service3.service2.count").isEqualTo(count1),
				() -> assertThat(service5).extracting("service4.service3.service2.service1.count").isEqualTo(count1),

				() -> assertThat(cyclic1).extracting("count").isEqualTo(count1),
				() -> assertThat(cyclic1).extracting("pair.count").isEqualTo(count1),
				() -> assertThat(cyclic1).extracting("pair.pair").isSameAs(cyclic1),

				() -> assertThat(cyclic2).extracting("count").isEqualTo(count1),
				() -> assertThat(cyclic2).extracting("pair.count").isEqualTo(count1),
				() -> assertThat(cyclic2).extracting("pair.pair").isSameAs(cyclic2),

				() -> {});
	}

	static class Shell {
		@Inject
		Injector injector;

		@Inject
		Service1 service1;
		@Inject
		Service2 service2;
		@Inject
		Service3 service31;
		Service3 service32;
		@Inject
		Service4 service4;
		@Inject
		Service5 service5;

		@PostConstruct
		void init() {
			this.service32 = injectMembers(this.injector, new Service3());
		}
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
				() -> assertThat(shell).extracting("service1.count").isEqualTo(count1),

				() -> assertThat(shell).extracting("service2.count").isEqualTo(count1),
				() -> assertThat(shell).extracting("service2.service1.count").isEqualTo(count1),

				() -> assertThat(shell).extracting("service31.count").isEqualTo(count1),
				() -> assertThat(shell).extracting("service31.service2.count").isEqualTo(count1),
				() -> assertThat(shell).extracting("service31.service2.service1.count").isEqualTo(count1),

				() -> assumingThat(postConstruct, () -> {
					assertAll(
							() -> assertThat(shell).extracting("service32.count").isEqualTo(count1),
							() -> assertThat(shell).extracting("service32.service2.count").isEqualTo(count1),
							() -> assertThat(shell).extracting("service32.service2.service1.count").isEqualTo(count1),

							() -> {});
				}),

				() -> assumingThat(!postConstruct, () -> {
					assertThat(shell).extracting("service32").isNull();
				}),

				() -> assertThat(shell).extracting("service4.count").isEqualTo(count2),
				() -> assertThat(shell).extracting("service4.service3.count").isEqualTo(count1),
				() -> assertThat(shell).extracting("service4.service3.service2.count").isEqualTo(count1),
				() -> assertThat(shell).extracting("service4.service3.service2.service1.count").isEqualTo(count1),

				() -> assertThat(shell).extracting("service5.count").isEqualTo(count2),
				() -> assertThat(shell).extracting("service5.service4.count").isEqualTo(count2),
				() -> assertThat(shell).extracting("service5.service4.service3.count").isEqualTo(count1),
				() -> assertThat(shell).extracting("service5.service4.service3.service2.count").isEqualTo(count1),
				() -> assertThat(shell).extracting("service5.service4.service3.service2.service1.count")
						.isEqualTo(count1),

				() -> {});
	}
}
