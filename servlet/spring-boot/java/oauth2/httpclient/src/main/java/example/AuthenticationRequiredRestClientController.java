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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.function.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.security.oauth2.client.web.function.client.OAuth2ClientHttpRequestInterceptor.ClientRegistrationIdResolver;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestClient;

import static org.springframework.security.oauth2.client.web.function.client.OAuth2ClientHttpRequestInterceptor.authorizationFailureHandler;

/**
 * @author Steve Riesenberg
 */
@Controller
public class AuthenticationRequiredRestClientController {

	private final RestClient restClient;

	public AuthenticationRequiredRestClientController(OAuth2AuthorizedClientManager authorizedClientManager,
			OAuth2AuthorizedClientRepository authorizedClientRepository,
			@Value("${mockwebserver.url}") String baseUrl) {

		OAuth2ClientHttpRequestInterceptor requestInterceptor = new OAuth2ClientHttpRequestInterceptor(
				authorizedClientManager, clientRegistrationIdResolver());
		requestInterceptor.setAuthorizationFailureHandler(authorizationFailureHandler(authorizedClientRepository));
		this.restClient = RestClient.builder().baseUrl(baseUrl).requestInterceptor(requestInterceptor).build();
	}

	@GetMapping("/public/authentication-required/messages")
	public String getMessages(Model model) {
		// @formatter:off
		Message[] messages = this.restClient.get()
			.uri("/api/v1/messages")
			.retrieve()
			.body(Message[].class);
		// @formatter:on
		model.addAttribute("messages", messages);
		return "messages";
	}

	public record Message(String message) {
	}

	/**
	 * This demonstrates a {@link ClientRegistrationIdResolver} that requires
	 * authentication using OAuth 2.0 or Open ID Connect 1.0. If the user is not logged
	 * in, they are sent to the login page prior to obtaining an access token.
	 */
	private static ClientRegistrationIdResolver clientRegistrationIdResolver() {
		SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();
		return (request) -> {
			Authentication authentication = securityContextHolderStrategy.getContext().getAuthentication();
			if (authentication instanceof OAuth2AuthenticationToken principal) {
				return principal.getAuthorizedClientRegistrationId();
			}
			if (authentication instanceof AnonymousAuthenticationToken) {
				throw new AccessDeniedException("Authentication is required");
			}
			throw new IllegalStateException("OAuth 2.0 or OpenID Connect 1.0 Login is required");
		};
	}

}
