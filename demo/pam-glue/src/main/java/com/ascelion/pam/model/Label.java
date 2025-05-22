package com.ascelion.pam.model;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import software.amazon.awssdk.enhanced.dynamodb.extensions.annotations.DynamoDbAtomicCounter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

@Builder
@DynamoDbImmutable(builder = Label.LabelBuilder.class)
@Getter
public class Label {
	private final String id;
	private final long count;
	private final List<String> images;

	@DynamoDbPartitionKey
	@DynamoDbAttribute("label")
	public String getId() {
		return this.id;
	}

	@DynamoDbAtomicCounter(startValue = 1)
	public long getCount() {
		return this.count;
	}
}
