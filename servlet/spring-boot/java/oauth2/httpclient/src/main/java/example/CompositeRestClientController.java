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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.function.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.security.oauth2.client.web.function.client.OAuth2ClientHttpRequestInterceptor.ClientRegistrationIdResolver;
import org.springframework.security.oauth2.client.web.function.client.RequestAttributeClientRegistrationIdResolver;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestClient;

import static org.springframework.security.oauth2.client.web.function.client.OAuth2ClientHttpRequestInterceptor.authorizationFailureHandler;

/**
 * @author Steve Riesenberg
 */
@Controller
public class CompositeRestClientController {

	private final RestClient restClient;

	public CompositeRestClientController(OAuth2AuthorizedClientManager authorizedClientManager,
			OAuth2AuthorizedClientRepository authorizedClientRepository,
			@Value("${messages.base-url}") String baseUrl) {

		OAuth2ClientHttpRequestInterceptor requestInterceptor = new OAuth2ClientHttpRequestInterceptor(
				authorizedClientManager, clientRegistrationIdResolver(null));
		requestInterceptor.setAuthorizationFailureHandler(authorizationFailureHandler(authorizedClientRepository));
		this.restClient = RestClient.builder().baseUrl(baseUrl).requestInterceptor(requestInterceptor).build();
	}

	@GetMapping({ "/composite/messages", "/public/composite/messages" })
	public String getMessages(Model model) {
		// @formatter:off
		Message[] messages = this.restClient.get()
			.uri("/messages")
			.retrieve()
			.body(Message[].class);
		// @formatter:on
		model.addAttribute("messages", messages);
		return "messages";
	}

	/**
	 * This demonstrates a composite {@link ClientRegistrationIdResolver} that tries the
	 * following ways of resolving a {@code clientRegistrationId}:
	 * <ol>
	 * <li>delegate to {@link RequestAttributeClientRegistrationIdResolver}</li>
	 * <li>use (optional) default {@code clientRegistrationId}</li>
	 * <li>use the {@code clientRegistrationId} from OAuth 2.0 or OpenID Connect 1.0
	 * Login</li>
	 * </ol>
	 * @param defaultClientRegistrationId the default clientRegistrationId to use, or null
	 * to fall back to using OAuth 2.0 or OpenID Connect 1.0 Login
	 */
	private static ClientRegistrationIdResolver clientRegistrationIdResolver(String defaultClientRegistrationId) {
		RequestAttributeClientRegistrationIdResolver delegate = new RequestAttributeClientRegistrationIdResolver();
		SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();
		return (request) -> {
			String clientRegistrationId = delegate.resolve(request);
			if (clientRegistrationId == null) {
				clientRegistrationId = defaultClientRegistrationId;
			}
			Authentication authentication = securityContextHolderStrategy.getContext().getAuthentication();
			if (clientRegistrationId == null && authentication instanceof OAuth2AuthenticationToken principal) {
				return principal.getAuthorizedClientRegistrationId();
			}
			return clientRegistrationId;
		};
	}

	public record Message(String message) {
	}

}
