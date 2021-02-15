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

package pogorobot.service;

import java.io.FileNotFoundException;
import java.util.SortedSet;

import org.apache.commons.codec.DecoderException;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import pogorobot.entities.EventWithSubscribers;
import pogorobot.entities.Filter;
import pogorobot.entities.Gym;
import pogorobot.entities.PokemonWithSpawnpoint;
import pogorobot.entities.SendMessages;
import pogorobot.entities.User;
import pogorobot.entities.UserGroup;
import pogorobot.telegram.util.SendMessageAnswer;

public interface TelegramSendMessagesService {

	public void sendMessageToRecipient(User userId, String message);

	public boolean sendMessageToAll(String message);

	public boolean sendMessageToGroup(UserGroup groupId, String message);

	public boolean sendMessageToFilteredPeople(Filter filter, String message);

	SendMessageAnswer sendEggMessage(String chatId, Gym fullGym, String string,
			SortedSet<EventWithSubscribers> eventWithSubscribers, Integer possibleMessageIdToUpdate)
			throws FileNotFoundException, TelegramApiException, InterruptedException, DecoderException;

	SendMessageAnswer sendRaidMessage(Gym fullGym, String chatId, SortedSet<EventWithSubscribers> eventWithSubscribers,
			Integer possibleMessageIdToUpdate)
			throws FileNotFoundException, TelegramApiException, InterruptedException, DecoderException;

	SendMessageAnswer sendMonMessage(PokemonWithSpawnpoint pokemon, String chatId, Integer possibleMessageIdToUpdate)
			throws FileNotFoundException, TelegramApiException, InterruptedException, DecoderException;


	Message sendMessageTimed(Long chatId, PartialBotApiMethod<Message> message);

	// void cleanupSendMessage() throws TelegramApiException;
	// String getPokemonName(String pokemon);
	//
	// JSONObject getJsonPokemons();

	boolean deleteMessage(Long groupChatId, Integer messageId) throws TelegramApiException;

	SendMessageAnswer sendStandardMessage(PokemonWithSpawnpoint pokemon, Gym fullGym,
			SortedSet<EventWithSubscribers> eventWithSubscribers, String chatId, Integer possibleMessageIdToUpdate)
			throws FileNotFoundException, TelegramApiException, InterruptedException, DecoderException;

	String deleteMessagesOnTelegram(SendMessages sendMessagesClone, long timeDifference);

}
