package com.ascelion.demo;

import com.ascelion.guice.request.RequestScoped;

import lombok.*;

@RequestScoped
@NoArgsConstructor
@AllArgsConstructor
@Getter
@ToString
public class AppRequest {
	private String value;
}
