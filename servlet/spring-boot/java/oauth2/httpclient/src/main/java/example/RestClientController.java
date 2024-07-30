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

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestClient;

/**
 * @author Steve Riesenberg
 */
@Controller
public class RestClientController {

	private final RestClient restClient;

	public RestClientController(RestClient restClient) {
		this.restClient = restClient;
	}

	@GetMapping(value = {"/authenticated/messages", "/public/messages"})
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

}
