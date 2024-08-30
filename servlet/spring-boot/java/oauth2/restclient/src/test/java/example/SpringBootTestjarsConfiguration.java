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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.aether.repository.RemoteRepository;
import testjars.resourceServer.TestOAuth2ResourceServerApplication;

import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.experimental.boot.server.exec.CommonsExecWebServer;
import org.springframework.experimental.boot.server.exec.CommonsExecWebServerFactoryBean;
import org.springframework.experimental.boot.server.exec.MavenClasspathEntry;
import org.springframework.experimental.boot.test.context.DynamicProperty;
import org.springframework.experimental.boot.test.context.EnableDynamicProperty;
import org.springframework.experimental.boot.test.context.OAuth2ClientProviderIssuerUri;

@EnableDynamicProperty
@TestConfiguration(proxyBeanMethods = false)
public class SpringBootTestjarsConfiguration {

	@Bean
	@OAuth2ClientProviderIssuerUri
	static CommonsExecWebServerFactoryBean authorizationServer() {
		// @formatter:off
		return CommonsExecWebServerFactoryBean.builder()
			.defaultSpringBootApplicationMain()
			.classpath((classpath) -> classpath
				.entries(springBootStarter("oauth2-authorization-server"))
			);
		// @formatter:on
	}

	@Bean
	@DependsOn("authorizationServer")
	@DynamicProperty(name = "messages.base-url", value = "'http://localhost:' + port")
	static CommonsExecWebServerFactoryBean resourceServer(CommonsExecWebServer authorizationServer) {
		String issuer = "http://127.0.0.1:" + authorizationServer.getPort();
		// @formatter:off
		return CommonsExecWebServerFactoryBean.builder()
			.mainClass(TestOAuth2ResourceServerApplication.class.getName())
			.classpath((classpath) -> classpath
				.entries(springBootStarter("web"))
				.entries(springBootStarter("oauth2-resource-server"))
				.recursive(TestOAuth2ResourceServerApplication.class)
			)
			.addSystemProperties(Map.of("auth-server.issuer-uri", issuer));
		// @formatter:on
	}

	/**
	 * Resolve a Spring Boot starter with snapshots.
	 *
	 * @param starterName the name of the starter
	 * @return the {@link MavenClasspathEntry}
	 */
	static MavenClasspathEntry springBootStarter(String starterName) {
		List<RemoteRepository> repositories = new ArrayList<>();
		repositories.add(new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2").build());
		repositories.add(new RemoteRepository.Builder("spring-milestone", "default", "https://repo.spring.io/milestone").build());
		repositories.add(new RemoteRepository.Builder("spring-snapshot", "default", "https://repo.spring.io/snapshot").build());
		String artifactName = "org.springframework.boot:spring-boot-starter-%s:%s".formatted(starterName, SpringBootVersion.getVersion());
		return new MavenClasspathEntry(artifactName, repositories);
	}

}
