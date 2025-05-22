package com.ascelion.dynamodb;

import static com.ascelion.guice.GuiceBoot.guiceInit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.google.inject.Injector;

import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@ExtendWith(MockitoExtension.class)
class DynamoDbModuleTest {

	@DynamoDbBean
	public static class MyTestEntity1 {
	}

	@DynamoDbBean
	@DynamoDbTableName("my-table-name")
	public static class MyTestEntity2 {
	}

	static class App {
		DynamoDbTable<MyTestEntity1> table11;

		@Inject
		DynamoDbTable<MyTestEntity1> table12;

		@Inject
		DynamoDbTable<MyTestEntity2> table2;

		@Inject
		App(DynamoDbTable<MyTestEntity1> table11) {
			this.table11 = table11;
		}
	}

	static class FieldApp<T> {
		@Inject
		DynamoDbTable<T> table;
	}

	static class FieldApp1 extends FieldApp<MyTestEntity1> {
	}

	static class FieldApp2 extends FieldApp<MyTestEntity2> {
	}

	static class MethodApp<T> {
		DynamoDbTable<T> table;

		@Inject
		void setTable(DynamoDbTable<T> table) {
			this.table = table;
		}
	}

	static class MethodApp1 extends MethodApp<MyTestEntity1> {
	}

	static class MethodApp2 extends MethodApp<MyTestEntity2> {
	}

	@RequiredArgsConstructor
	static class ConstructorApp<T> {
		final DynamoDbTable<T> table;
	}

	static class ConstructorApp1 extends ConstructorApp<MyTestEntity1> {
		@Inject
		ConstructorApp1(DynamoDbTable<MyTestEntity1> table) {
			super(table);
		}
	}

	static class ConstructorApp2 extends ConstructorApp<MyTestEntity2> {
		@Inject
		ConstructorApp2(DynamoDbTable<MyTestEntity2> table) {
			super(table);
		}
	}

	@Mock(answer = Answers.RETURNS_SELF)
	private DynamoDbClient ddbClient;

	@Test
	void simpleInject() {
		final var inj = injector(App.class);
		final var app = inj.getInstance(App.class);

		assertAll(
				() -> assertThat(app).isNotNull().hasNoNullFieldsOrProperties(),
				() -> assertThat(app.table11).isSameAs(app.table12),
				() -> assertThat(app.table11).extracting(DynamoDbTable::tableName).isEqualTo("my-test-entity1"),
				() -> assertThat(app.table2).extracting(DynamoDbTable::tableName).isEqualTo("my-table-name"),

				() -> {});
	}

	@Test
	void genericFieldInject() {
		final var inj = injector(FieldApp1.class, FieldApp2.class);
		final var app1 = inj.getInstance(FieldApp1.class);
		final var app2 = inj.getInstance(FieldApp2.class);

		assertAll(
				() -> assertThat(app1).isNotNull().hasNoNullFieldsOrProperties(),
				() -> assertThat(app2).isNotNull().hasNoNullFieldsOrProperties(),
				() -> assertThat(app1.table).extracting(DynamoDbTable::tableName).isEqualTo("my-test-entity1"),
				() -> assertThat(app2.table).extracting(DynamoDbTable::tableName).isEqualTo("my-table-name"),

				() -> {});
	}

	@Test
	void genericMethodInject() {
		final var inj = injector(MethodApp1.class, MethodApp2.class);
		final var app1 = inj.getInstance(MethodApp1.class);
		final var app2 = inj.getInstance(MethodApp2.class);

		assertAll(
				() -> assertThat(app1).isNotNull().hasNoNullFieldsOrProperties(),
				() -> assertThat(app2).isNotNull().hasNoNullFieldsOrProperties(),
				() -> assertThat(app1.table).extracting(DynamoDbTable::tableName).isEqualTo("my-test-entity1"),
				() -> assertThat(app2.table).extracting(DynamoDbTable::tableName).isEqualTo("my-table-name"),

				() -> {});
	}

	@Test
	void genericConstructorInject() {
		final var inj = injector(ConstructorApp1.class, ConstructorApp2.class);
		final var app1 = inj.getInstance(ConstructorApp1.class);
		final var app2 = inj.getInstance(ConstructorApp2.class);

		assertAll(
				() -> assertThat(app1).isNotNull().hasNoNullFieldsOrProperties(),
				() -> assertThat(app2).isNotNull().hasNoNullFieldsOrProperties(),
				() -> assertThat(app1.table).extracting(DynamoDbTable::tableName).isEqualTo("my-test-entity1"),
				() -> assertThat(app2.table).extracting(DynamoDbTable::tableName).isEqualTo("my-table-name"),

				() -> {});
	}

	private Injector injector(Class... classes) {
		return guiceInit(classes)
				.overrides(bnd -> {
					bnd.bind(Region.class).toInstance(Region.EU_CENTRAL_1);
					bnd.bind(DynamoDbClient.class).toInstance(this.ddbClient);
				})
				.boot();

	}
}
