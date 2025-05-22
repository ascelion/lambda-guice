package com.ascelion.pam.services;

import static org.apache.commons.lang3.StringUtils.trimToNull;

import com.ascelion.s3.BucketName;

import java.util.List;

import jakarta.inject.Inject;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Object;

public class S3Service {
	private final S3Client client;

	@Inject
	public S3Service(@BucketName("bucket") S3Client client) {
		this.client = client;
	}

	public List<String> list() {
		return this.client.listObjects(bld -> {}).contents().stream()
				.map(S3Object::key)
				.toList();
	}

	public boolean exists(String keyName) {
		return trimToNull(this.client.headObject(bld -> bld.key(keyName)).contentType()) != null;
	}

	public byte[] getBytes(String keyName) {
		return this.client.getObjectAsBytes(bld -> bld.key(keyName)).asByteArray();
	}

	public void putBytes(String keyName, byte[] data) {
		this.client.putObject(bld -> bld.key(keyName), RequestBody.fromBytes(data));
	}
}
