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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.oauth2.client.OAuth2AuthorizationFailureHandler;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor.ClientRegistrationIdResolver;
import org.springframework.security.oauth2.client.web.client.RequestAttributeClientRegistrationIdResolver;
import org.springframework.web.client.RestClient;

import static org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor.authorizationFailureHandler;

/**
 * @author Steve Riesenberg
 */
@Configuration
public class RestClientConfiguration {

	private static final String CLIENT_REGISTRATION_ID = "messaging-client";

	@Bean
	public RestClient restClient(OAuth2AuthorizedClientManager authorizedClientManager,
			OAuth2AuthorizedClientRepository authorizedClientRepository,
			@Value("${messages.base-url}") String baseUrl) {

		/*
		 * The following is the default ClientRegistrationIdResolver in
		 * OAuth2ClientHttpRequestInterceptor, or uncomment one of the other resolvers to
		 * try a different strategy for resolving the clientRegistrationId.
		 */
		ClientRegistrationIdResolver clientRegistrationIdResolver = new RequestAttributeClientRegistrationIdResolver();
//		ClientRegistrationIdResolver clientRegistrationIdResolver =
//			compositeClientRegistrationIdResolver(CLIENT_REGISTRATION_ID);
//		ClientRegistrationIdResolver clientRegistrationIdResolver =
//			authenticationRequiredClientRegistrationIdResolver();

		OAuth2AuthorizationFailureHandler authorizationFailureHandler = authorizationFailureHandler(
				authorizedClientRepository);
		OAuth2ClientHttpRequestInterceptor requestInterceptor = new OAuth2ClientHttpRequestInterceptor(
				authorizedClientManager, clientRegistrationIdResolver);
		requestInterceptor.setAuthorizationFailureHandler(authorizationFailureHandler);

		return RestClient.builder().baseUrl(baseUrl).requestInterceptor(requestInterceptor).build();
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
	private static ClientRegistrationIdResolver compositeClientRegistrationIdResolver(
			String defaultClientRegistrationId) {
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

	/**
	 * This demonstrates a {@link ClientRegistrationIdResolver} that requires
	 * authentication using OAuth 2.0 or Open ID Connect 1.0. If the user is not logged
	 * in, they are sent to the login page prior to obtaining an access token.
	 */
	private static ClientRegistrationIdResolver authenticationRequiredClientRegistrationIdResolver() {
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
