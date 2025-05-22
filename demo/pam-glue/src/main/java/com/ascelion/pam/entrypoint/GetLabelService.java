package com.ascelion.pam.entrypoint;

import com.ascelion.pam.dto.LabelInfo;
import com.ascelion.pam.mappers.LabelMapper;
import com.ascelion.pam.model.Label;

import java.util.List;

import jakarta.inject.Inject;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;

public class GetLabelService {

	@Inject
	private DynamoDbTable<Label> table;

	@Inject
	private LabelMapper mapper;

	List<LabelInfo> get() {
		return this.table.scan().items().stream()
				.map(this.mapper::toLabelInfo)
				.toList();
	}
}
