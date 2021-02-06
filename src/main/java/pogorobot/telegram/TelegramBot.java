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

package pogorobot.telegram;

import java.io.Serializable;
import java.util.concurrent.Semaphore;

import org.telegram.telegrambots.extensions.bots.commandbot.commands.ICommandRegistry;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendSticker;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.LongPollingBot;

public interface TelegramBot extends LongPollingBot, ICommandRegistry {

	// Something like destructor
	void finish();

	/**
	 * 
	 * This method must be called instead of all calls to sendMessage(),
	 * editMessageText(), sendChatAction() etc... for performing time-based sends
	 * obeying the basic Telegram limits (no more 30 msgs per second in different
	 * chats, no more 1 msg per second in any single chat). The method can be safely
	 * called from multiple threads. Order of sends to any given chat is guaranteed
	 * to remain the same as order of calls. Sends to different chats can be
	 * out-of-order depending on timing.<br>
	 * Example of call: <br>
	 * <br>
	 * <code>
	 * SendMessage sendMessageRequest = new SendMessage();<br>
	 * sendMessageRequest.setChatId(chatId);<br>
	 * sendMessageRequest.setParseMode("HTML");<br>
	 * sendMessageRequest.setText(text);<br>
	 * sendMessageRequest.setReplyMarkup(replyMarkup); <br>
	 * sendTimed(chatId, sendMessageRequest); // now use this instead of sendMessage() API method<br>
	 * </code>
	 **/
	void sendTimed(Long chatId, PartialBotApiMethod<? extends Serializable> messageRequest);

	/**
	 * When time of actual send comes this callback is called with the same
	 * parameters as in call to sendTimed().
	 * 
	 * @param chatId
	 * @param messageRequest
	 */
	void sendMessageCallback(Long chatId, PartialBotApiMethod<? extends Serializable> messageRequest);

	void processNonCommandUpdate(Update update);

	@Override
	String getBotToken();

	void sendTimed(Long chatId, PartialBotApiMethod<? extends Serializable> messageRequest, Integer updateId, Semaphore mutex);

	Integer putSendMessages(Integer internalId, Integer postedMessageId);

	Integer getSendMessages(Integer internalId);

	void removeSendMessage(Integer next);

	Message executeSendSticker(SendSticker method) throws TelegramApiException;

}