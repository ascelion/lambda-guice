package com.ascelion.guice.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.ascelion.guice.jupiter.GuiceBootExtension;
import com.google.inject.Singleton;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import lombok.Getter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@ExtendWith(GuiceBootExtension.class)
class GuiceBootJupiterTest {
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

		Service5 dontCcreate() {
			throw new UnsupportedOperationException();
		}

		@PostConstruct
		void init() {
			this.count++;
		}
	}

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

	@Singleton
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

	@Singleton
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
		@Singleton
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
		@Singleton
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

	@Inject
	Service1 service1;
	@Inject
	Service2 service2;
	@Inject
	Service3 service3;
	@Inject
	Service4 service4;
	@Inject
	Service5 service5;

	@Inject
	Singleton1 singleton1;
	@Inject
	Singleton2 singleton2;
	@Inject
	Singleton3 singleton3;
	@Inject
	Singleton4 singleton4;
	@Inject
	Singleton5 singleton5;

	@Test
	void verifyServices() {
		assertAll(
				() -> assertThat(this.service1).extracting("count").isEqualTo(1),

				() -> assertThat(this.service2).extracting("count").isEqualTo(1),
				() -> assertThat(this.service2).extracting("service1.count").isEqualTo(1),

				() -> assertThat(this.service3).extracting("count").isEqualTo(1),
				() -> assertThat(this.service3).extracting("service2.count").isEqualTo(1),
				() -> assertThat(this.service3).extracting("service2.service1.count").isEqualTo(1),

				() -> assertThat(this.service4).extracting("count").isEqualTo(2),
				() -> assertThat(this.service4).extracting("service3.count").isEqualTo(1),
				() -> assertThat(this.service4).extracting("service3.service2.count").isEqualTo(1),
				() -> assertThat(this.service4).extracting("service3.service2.service1.count").isEqualTo(1),

				() -> assertThat(this.service5).extracting("count").isEqualTo(2),
				() -> assertThat(this.service5).extracting("service4.count").isEqualTo(2),
				() -> assertThat(this.service5).extracting("service4.service3.count").isEqualTo(1),
				() -> assertThat(this.service5).extracting("service4.service3.service2.count").isEqualTo(1),
				() -> assertThat(this.service5).extracting("service4.service3.service2.service1.count")
						.isEqualTo(1),

				() -> {});
	}

	@Test
	void verifySingletons() {
		assertAll(
				() -> assertThat(this.singleton1).extracting("count").isEqualTo(1),

				() -> assertThat(this.singleton2).extracting("count").isEqualTo(1),
				() -> assertThat(this.singleton2).extracting("singleton1").isSameAs(this.singleton1),
				() -> assertThat(this.singleton2).extracting("singleton1.count").isEqualTo(1),

				() -> assertThat(this.singleton3).extracting("count").isEqualTo(1),
				() -> assertThat(this.singleton3).extracting("singleton2").isSameAs(this.singleton2),
				() -> assertThat(this.singleton3).extracting("singleton2.count").isEqualTo(1),
				() -> assertThat(this.singleton3).extracting("singleton2.singleton1.count").isEqualTo(1),

				() -> assertThat(this.singleton4).extracting("count").isEqualTo(2),
				() -> assertThat(this.singleton4).extracting("singleton3").isSameAs(this.singleton3),
				() -> assertThat(this.singleton4).extracting("singleton3.count").isEqualTo(1),
				() -> assertThat(this.singleton4).extracting("singleton3.singleton2.count").isEqualTo(1),
				() -> assertThat(this.singleton4).extracting("singleton3.singleton2.singleton1.count")
						.isEqualTo(1),

				() -> assertThat(this.singleton5).extracting("count").isEqualTo(2),
				() -> assertThat(this.singleton5).extracting("singleton4").isSameAs(this.singleton4),
				() -> assertThat(this.singleton5).extracting("singleton4.count").isEqualTo(2),
				() -> assertThat(this.singleton5).extracting("singleton4.singleton3.count").isEqualTo(1),
				() -> assertThat(this.singleton5).extracting("singleton4.singleton3.singleton2.count")
						.isEqualTo(1),
				() -> assertThat(this.singleton5).extracting("singleton4.singleton3.singleton2.singleton1.count")
						.isEqualTo(1),

				() -> {});
	}

}
