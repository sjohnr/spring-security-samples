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

package example;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.function.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

/**
 * @author Steve Riesenberg
 */
@Configuration
public class RestClientConfiguration {

	private static final String CLIENT_REGISTRATION_ID = "messaging-client";

	private final String baseUrl;

	public RestClientConfiguration(@Value("${mockwebserver.url}") String baseUrl) {
		this.baseUrl = baseUrl;
	}

	@Bean
	public OAuth2AuthorizedClientManager authorizedClientManager(
			ClientRegistrationRepository clientRegistrationRepository,
			OAuth2AuthorizedClientService authorizedClientService) {

		return new AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepository,
				authorizedClientService);
	}

	@Bean
	public RestClient restClient(RestClient.Builder builder, OAuth2AuthorizedClientManager authorizedClientManager,
			OAuth2AuthorizedClientRepository authorizedClientRepository) {

		OAuth2ClientHttpRequestInterceptor requestInterceptor = new OAuth2ClientHttpRequestInterceptor(
				authorizedClientManager);
		requestInterceptor.setDefaultClientRegistrationId(CLIENT_REGISTRATION_ID);
		requestInterceptor.setAuthorizedClientRepository(authorizedClientRepository);

		return builder.baseUrl(this.baseUrl).requestInterceptor(requestInterceptor).build();
	}

}
