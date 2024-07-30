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
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.function.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestClient;

/**
 * @author Steve Riesenberg
 */
@Controller
public class PerRequestRestClientController {

	private final OAuth2AuthorizedClientManager authorizedClientManager;

	private final OAuth2AuthorizedClientRepository authorizedClientRepository;

	private final RestClient restClient;

	public PerRequestRestClientController(OAuth2AuthorizedClientManager authorizedClientManager,
			OAuth2AuthorizedClientRepository authorizedClientRepository,
			@Value("${mockwebserver.url}") String baseUrl) {

		this.authorizedClientManager = authorizedClientManager;
		this.authorizedClientRepository = authorizedClientRepository;
		this.restClient = RestClient.create(baseUrl);
	}

	@GetMapping(value = {"/authenticated/per-request/messages", "/public/per-request/messages"})
	public String getMessages(Authentication authentication, Model model) {
		OAuth2ClientHttpRequestInterceptor requestInterceptor = createRequestInterceptor(authentication);
		// @formatter:off
		Message[] messages = this.restClient.get()
			.uri("/api/v1/messages")
			.httpRequest(requestInterceptor.httpRequest())
			.retrieve()
			.onStatus(requestInterceptor.errorHandler())
			.body(Message[].class);
		// @formatter:on
		model.addAttribute("messages", messages);
		return "messages";
	}

	private OAuth2ClientHttpRequestInterceptor createRequestInterceptor(Authentication authentication) {
		String clientRegistrationId;
		if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
			clientRegistrationId = "messaging-client";
		}
		else {
			clientRegistrationId = "login-client";
		}

		OAuth2ClientHttpRequestInterceptor requestInterceptor = new OAuth2ClientHttpRequestInterceptor(
			this.authorizedClientManager, clientRegistrationId);
		requestInterceptor.setAuthorizedClientRepository(this.authorizedClientRepository);

		return requestInterceptor;
	}

	public record Message(String message) {
	}

}
