/**
 Copyright 2019 Benjamin Marstaller
 
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at
 
    http://www.apache.org/licenses/LICENSE-2.0
 
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/

package pogorobot.rocketmap;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import pogorobot.events.EventMessage;
import pogorobot.rocketmap.messages.RocketmapEvent;
import pogorobot.service.MessageContentProcessor;

@RestController
public class WebhookServer {

	@Autowired
	private MessageContentProcessor messageContentProcessor;

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	// public HttpStatus readFromWebhook(@RequestBody String messages) {
	// System.out.println(messages.toString());
	// // messages.stream().map((event) -> event.getMessage()).forEach((message) ->
	// {
	// // processContent(message);
	// // });
	// return HttpStatus.OK;
	// }
	@RequestMapping(value = "/", method = RequestMethod.POST)
	@ResponseBody
	public HttpStatus readFromWebhook(@RequestBody List<RocketmapEvent<EventMessage<?>>> messages) {
		messages.stream().map((event) -> event.getMessage()).forEach((message) -> {
			logger.debug("Message: " + message.toString());
			processContent(message);
		});
		return HttpStatus.OK;
	}

	private synchronized <T> EventMessage<T> processContent(EventMessage<T> message) {
		return messageContentProcessor.processContent(message);
	}

}
