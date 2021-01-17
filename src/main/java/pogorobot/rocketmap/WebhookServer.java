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
import java.util.concurrent.Executor;

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
import pogorobot.rocketmap.messages.IncomingEvent;
import pogorobot.service.MessageContentProcessor;

@RestController
public class WebhookServer {

	private static final int PERIOD = 1;

	@Autowired
	private MessageContentProcessor messageContentProcessor;

	private ConcurrentLinkedQueue<EventMessage<?>> eventQueue = new ConcurrentLinkedQueue<>();


	private Logger logger = LoggerFactory.getLogger(this.getClass());

	private class ThreadPerTaskExecutor implements Executor {

		@Override
		public void execute(Runnable r) {
			new Thread(r).start();
		}

	}

	Runnable messagePoller = new Runnable() {

		Timer messageSendTimer = new Timer("Webhook-MessagePoller",true);

		@Override
		public void run() {
			// messageSendTimer = new Timer(true);
			messageSendTimer.schedule(new MessageSenderTask(), 0, PERIOD);
		}

	};

	public WebhookServer() {
		Executor executor = new ThreadPerTaskExecutor();
		executor.execute(messagePoller);
	}

	@RequestMapping(value = "/", method = RequestMethod.POST)
	@ResponseBody
	public HttpStatus readFromWebhook(@RequestBody List<IncomingEvent<EventMessage<?>>> messages) {
		messages.stream().map((event) -> event.getMessage()).forEach((message) -> {
//			logger.debug("message: " + message.toString());
			eventQueue.add(message);
		});
		return HttpStatus.OK;
	}
	
	// /**
	// * Temporary turn on for debugging raw webhook data
	// *
	// * @param messages
	// * @return
	// */
	// public HttpStatus readFromWebhook(@RequestBody String messages) {
	// if (messages.contains("invasion")) {
	// logger.info(messages.toString());
	// }
	// return HttpStatus.OK;
	// }


	private final class MessageSenderTask extends TimerTask {

		private synchronized <T> void processContent(EventMessage<T> message) {
			if (message != null) {
				messageContentProcessor.processContent(message);
			}
		}

		@Override
		public void run() {
			// while (!eventQueue.isEmpty()) {
				EventMessage<?> eventMessage = eventQueue.poll();
				if (eventMessage != null) {
					logger.debug("processing next message: {}", eventMessage);
					processContent(eventMessage);

				}
			// }
			// else {
			// logger.debug("incoming queue empty - wait a period of {} ms", PERIOD * 10);
			// try {
			// Thread.sleep(PERIOD * 9L);
			// } catch (InterruptedException e) {
			// logger.warn(
			// "MessageSenderTask got interupted while sleeping - interupt message sender
			// task of webhook");
			// Thread.currentThread().interrupt();
			// }
			// }
			// }
		}
	}

}
