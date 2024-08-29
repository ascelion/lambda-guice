package com.ascelion.demo;

import jakarta.inject.Inject;
import lombok.*;

@RequiredArgsConstructor(onConstructor_ = @Inject)
@Getter
@ToString
public class AppRequestWrapper {

	private final AppRequest request;
}
