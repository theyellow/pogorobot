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
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

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

	private static final int PERIOD = 50;

	@Autowired
	private MessageContentProcessor messageContentProcessor;

	private ConcurrentLinkedQueue<EventMessage<?>> eventQueue = new ConcurrentLinkedQueue<>();

	private final Timer messageSendTimer = new Timer(true);

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	public WebhookServer() {
		messageSendTimer.schedule(new MessageSenderTask(), 0, PERIOD);
	}

	@RequestMapping(value = "/", method = RequestMethod.POST)
	@ResponseBody
	public HttpStatus readFromWebhook(@RequestBody List<RocketmapEvent<EventMessage<?>>> messages) {
		messages.stream().map((event) -> event.getMessage()).forEach((message) -> {
			logger.debug("message: " + message.toString());
			eventQueue.add(message);
		});
		return HttpStatus.OK;
	}

	private synchronized <T> void processContent(EventMessage<T> message) {
		if (message != null) {
			messageContentProcessor.processContent(message);
		}
	}

	private final class MessageSenderTask extends TimerTask {
		@Override
		public void run() {
			while (true) {
				EventMessage<?> eventMessage = eventQueue.poll();
				processContent(eventMessage);
				if (eventMessage == null) {
					logger.debug("incoming queue empty - waiting a second");
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						logger.info("MessageSenderTask got interupted while sleeping");
					}
				}
			}
		}
	}

}
