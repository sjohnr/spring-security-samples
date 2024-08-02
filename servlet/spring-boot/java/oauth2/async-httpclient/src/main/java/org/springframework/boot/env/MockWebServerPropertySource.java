/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.env;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * A {@link PropertySource} that makes the dynamic <code>mockwebserver.url</code> property
 * available in the {@link org.springframework.core.env.Environment}.
 *
 * @author Steve Riesenberg
 */
public class MockWebServerPropertySource extends PropertySource<MockWebServer> implements DisposableBean {

	private static final String PROPERTY_SOURCE_NAME = "mockwebserver";

	private final Map<String, Object> properties = new HashMap<>();

	public MockWebServerPropertySource() {
		super(PROPERTY_SOURCE_NAME, createMockWebServer());
		initialize();
	}

	private void initialize() {
		MockWebServer mockWebServer = getSource();
		String baseUrl = getBaseUrl(mockWebServer);
		this.properties.put(PROPERTY_SOURCE_NAME.concat(".url"), baseUrl);
	}

	@Override
	public Object getProperty(String name) {
		return this.properties.get(name);
	}

	@Override
	public void destroy() throws Exception {
		getSource().shutdown();
	}

	private static MockWebServer createMockWebServer() {
		MockWebServer mockWebServer = new MockWebServer();
		mockWebServer.setDispatcher(new ClassPathDispatcher());
		try {
			mockWebServer.start();
		}
		catch (IOException ex) {
			throw new RuntimeException("Unable to start MockWebServer", ex);
		}

		return mockWebServer;
	}

	private static String getBaseUrl(MockWebServer mockWebServer) {
		String url = mockWebServer.url("").url().toExternalForm();
		return StringUtils.trimTrailingCharacter(url, '/');
	}

	private static final class ClassPathDispatcher extends Dispatcher {

		@Override
		public MockResponse dispatch(RecordedRequest recordedRequest) {
			String path = recordedRequest.getPath();
			if (path != null) {
				try {
					return new MockResponse().setResponseCode(200)
						.setHeader("Content-Type", "application/json")
						.setBody(readResource(path));
				}
				catch (IOException ignored) {
					ignored.printStackTrace();
				}
			}
			return new MockResponse().setResponseCode(404);
		}

		private static String readResource(String path) throws IOException {
			if (path.startsWith("/")) {
				path = StringUtils.trimLeadingCharacter(path, '/');
			}

			return new ClassPathResource("responses/" + path + ".json").getContentAsString(StandardCharsets.UTF_8);
		}

	}

}
