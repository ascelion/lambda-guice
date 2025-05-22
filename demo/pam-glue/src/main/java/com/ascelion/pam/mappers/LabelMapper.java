package com.ascelion.pam.mappers;

import com.ascelion.pam.dto.LabelInfo;
import com.ascelion.pam.model.Label;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface LabelMapper {
	LabelInfo toLabelInfo(Label source);
}
