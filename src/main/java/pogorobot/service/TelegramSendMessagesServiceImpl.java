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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import javax.transaction.Transactional;

import org.apache.commons.codec.DecoderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendLocation;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendSticker;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiValidationException;

import pogorobot.entities.EventWithSubscribers;
import pogorobot.entities.Filter;
import pogorobot.entities.GroupMessages;
import pogorobot.entities.Gym;
import pogorobot.entities.PokemonWithSpawnpoint;
import pogorobot.entities.ProcessedRaids;
import pogorobot.entities.Raid;
import pogorobot.entities.RaidAtGymEvent;
import pogorobot.entities.User;
import pogorobot.entities.UserGroup;
import pogorobot.repositories.GroupMessagesRepository;
import pogorobot.repositories.ProcessedRaidRepository;
import pogorobot.repositories.RaidAtGymEventRepository;
import pogorobot.repositories.UserGroupRepository;
import pogorobot.repositories.UserRepository;
import pogorobot.telegram.PogoBot;
import pogorobot.telegram.util.SendRaidAnswer;

@Service("telegramSendMessagesService")
public class TelegramSendMessagesServiceImpl implements TelegramSendMessagesService {

	Logger logger = LoggerFactory.getLogger(this.getClass().getInterfaces()[0]);

	@Autowired
	private PogoBot pogoBot;

	@Autowired
	private TelegramTextService telegramTextService;

	@Autowired
	private GroupMessagesRepository groupMessagesRepository;

	@Autowired
	private UserGroupRepository userGroupRepository;

	@Autowired
	private RaidAtGymEventRepository raidAtGymEventRepository;

	@Autowired
	private ProcessedRaidRepository processedRaidRepository;

	@Autowired
	private TelegramKeyboardService telegramKeyboardService;

	@Autowired
	private EventWithSubscribersService eventWithSubscribersService;

	@Override
	public SendRaidAnswer sendMonMessage(PokemonWithSpawnpoint pokemon, String chatId)
			throws FileNotFoundException, TelegramApiException, InterruptedException, DecoderException {
		return sendStandardMessage(pokemon, null, null, chatId, null);
	}


	@Override
	public SendRaidAnswer sendMonMessage(PokemonWithSpawnpoint pokemon, String chatId,
			SortedSet<EventWithSubscribers> eventWithSubscribers)
			throws FileNotFoundException, TelegramApiException, InterruptedException, DecoderException {
		return sendStandardMessage(pokemon, null, eventWithSubscribers, chatId, null);
	}

	@Override
	public SendRaidAnswer sendRaidMessage(Gym gym, String chatId, SortedSet<EventWithSubscribers> eventWithSubscribers,
			Integer possibleMessageIdToUpdate)
			throws FileNotFoundException, TelegramApiException, InterruptedException, DecoderException {
		return sendStandardMessage(null, gym, eventWithSubscribers, chatId, possibleMessageIdToUpdate);
	}

	@Override
	public SendRaidAnswer sendEggMessage(String chatId, Gym gym, String level,
			SortedSet<EventWithSubscribers> eventWithSubscribers, Integer possibleMessageIdToUpdate)
			throws FileNotFoundException, TelegramApiException, InterruptedException, DecoderException {


		return sendStandardMessage(null, gym, eventWithSubscribers, chatId, possibleMessageIdToUpdate);

		// Long end = fullGym.getRaid().getEnd();
		// Double latitude = fullGym.getLatitude();
		// Double longitude = fullGym.getLongitude();
		// String pokemonFound = telegramTextService.createEggMessageText(fullGym, end,
		// level, latitude, longitude);
		// String url = telegramTextService.createDec() + "/eg" + "gs/" + level + ".we"
		// + "bp";
		// if (PogoBot.getConfiguration().getAlternativeStickers()) {
		// url = "";
		// }
		// SendRaidAnswer answer = sendMessages(chatId, url, latitude, longitude,
		// pokemonFound + telegramTextService.getParticipantsText(eventWithSubscribers),
		// false,
		// eventWithSubscribers,
		// fullGym.getGymId(), possibleMessageIdToUpdate);
		// logger.info("Sent to: " + chatId + ": Egg lvl. " + level);
		// logger.info("Answer: \nSticker:\n" + answer.getStickerAnswer() +
		// "\nMainMessage: \n"
		// + answer.getMainMessageAnswer() + "\nLocationMessage: \n" +
		// answer.getLocationAnswer());
		// return answer;
	}

	@Override
	public Message sendMessage(PartialBotApiMethod<Message> message) {
		Message result = null;
		try {
			Message sentMessage = null;
			if (message instanceof SendSticker) {
				sentMessage = pogoBot.execute((SendSticker) message);
			} else if (message != null) {
				sentMessage = pogoBot.execute((BotApiMethod<Message>) message);
			}
			if (null != sentMessage) {
				result = sentMessage;
			}
		} catch (TelegramApiException e) {
			if (e instanceof TelegramApiRequestException) {
				TelegramApiRequestException x = (TelegramApiRequestException) e;
				logger.error(
						"ErrorCode " + x.getErrorCode() + " - "
								+ (x.getApiResponse() != null ? x.getApiResponse() : x.getMessage())
								+ (x.getParameters() != null ? " - Parameter: " + x.getParameters().toString() : ""),
						x.getCause());
			} else if (e instanceof TelegramApiValidationException) {
				TelegramApiValidationException x = (TelegramApiValidationException) e;
				logger.error("Method: " + x.getMethod() + " - " + x.getObject() + " - Error: " + x.toString(),
						x.getCause());
			} else {
				logger.error("Unknown error: ", e);
			}
			if (message instanceof SendSticker) {
				SendSticker myMessage = (SendSticker) message;
				logger.error("Chat : " + myMessage.getChatId());
			} else {
				BotApiMethod<Message> myMessage = (BotApiMethod<Message>) message;
				if (message != null) {
					logger.error("Method : " + myMessage.getMethod());

				}
			}
		}
		return result;
	}

	private SendRaidAnswer sendAllMessagesForEventInternally(SendSticker stickerMessage,
			BotApiMethod<? extends Serializable> message,
			SendLocation location, boolean isGroupMessage) throws InterruptedException, TelegramApiException {
		Thread.sleep(100);
		SendRaidAnswer answer = new SendRaidAnswer();
		Message stickerAnswer = sendMessage(stickerMessage);
		if (stickerAnswer != null) {
			answer.setStickerAnswer(stickerAnswer);
		}
		Thread.sleep(100);
		if (message instanceof SendMessage) {
			SendMessage sendMessage = (SendMessage) message;
			sendMessage.enableMarkdown(true);
			ReplyKeyboard originalKeyboard = sendMessage.getReplyMarkup();
			ReplyKeyboard replyMarkup = originalKeyboard == null && !isGroupMessage
					&& !(originalKeyboard instanceof InlineKeyboardMarkup)
					? telegramKeyboardService.getSettingsKeyboard(true)
					: originalKeyboard;
			sendMessage.setReplyMarkup(replyMarkup);
			Message messageAnswer = sendMessage(sendMessage);
			if (messageAnswer != null) {
				answer.setMainMessageAnswer(messageAnswer);
			}
		} else if (message instanceof EditMessageText) {
			Serializable eventMessageAnswer = pogoBot.execute(((EditMessageText) message).enableMarkdown(true));
			if (eventMessageAnswer != null) {
				answer.setEventAnswer(eventMessageAnswer);
			}
		}
		Thread.sleep(100);
		if (location != null) {
			if (!isGroupMessage) {
				location.setReplyMarkup(telegramKeyboardService.getSettingsKeyboard(true));
			}
			Message locationAnswer = sendMessage(location);
			if (locationAnswer != null) {
				answer.setLocationAnswer(locationAnswer);
			}
		}
		return answer;
	}

	private SendRaidAnswer sendStandardMessage(PokemonWithSpawnpoint pokemon, Gym fullGym,
			SortedSet<EventWithSubscribers> eventWithSubscribers, String chatId, Integer possibleMessageIdToUpdate)
			throws FileNotFoundException, TelegramApiException, InterruptedException, DecoderException {

		// TODO: Refactor with this raidPokemon and call to different methods for
		// monsters and egg/raids
		Boolean raidPokemon = null;
		if (pokemon == null && fullGym != null) {
			raidPokemon = true;
		} else if (pokemon != null && fullGym == null) {
			raidPokemon = false;
		} else {
			logger.warn("Tried to send mon-message without mon or gym...");
			return null;
		}

		Double latitude = raidPokemon ? fullGym.getLatitude() : pokemon.getLatitude();
		Double longitude = raidPokemon ? fullGym.getLongitude() : pokemon.getLongitude();
		Raid raid = raidPokemon ? fullGym.getRaid() : null;

		// Meanings (if it's a raid, for monsters it's always positive):
		// idForSticker > 0 -> monster-sticker
		// idForSticker < 0 -> egg-sticker
		// idForSticker = 0 -> shouldn't happen
		int stickerId = 0;
		if (raidPokemon) {
			Long pokemonId = raid.getPokemonId();
			boolean isEgg = pokemonId == null || pokemonId <= 0L;
			logger.info("Get sticker for " + (isEgg ? "egg" : "raid") + " - look for id " + pokemonId);
			stickerId = isEgg ? -1 * raid.getRaidLevel().intValue() : pokemonId.intValue();
		} else {
			logger.info("Get sticker for pokemon");
			stickerId = pokemon.getPokemonId().intValue();
		}

		String gymId = fullGym == null ? null : fullGym.getGymId();
		String messageText = "";
		if (raidPokemon) {
			messageText = telegramTextService.getRaidMessagePokemonText(fullGym)
					+ telegramTextService.getParticipantsText(eventWithSubscribers);
		} else {
			messageText = telegramTextService.createPokemonMessageWithIVText(pokemon);
			String pokemonName = telegramTextService.getPokemonName(pokemon.getPokemonId().toString());
			logger.debug("Created text for " + chatId + ": Mon " + pokemonName);
		}
		String stUrl = telegramTextService.getStickerUrl(stickerId);
		return sendMessages(chatId, stUrl, latitude, longitude, messageText, !raidPokemon, eventWithSubscribers, gymId,
				possibleMessageIdToUpdate);
	}

	// private String getThreeDigitFormattedPokemonId(int pokemonInt) {
	// return pokemonInt >= 100 ? pokemonInt + "" : (pokemonInt >= 10 ? "0" +
	// pokemonInt : "00" + pokemonInt);
	// }

	private SendRaidAnswer sendMessages(String chatId, String stickerUrl, Double latitude, Double longitude,
			String messageText, boolean forMonsters, SortedSet<EventWithSubscribers> eventWithSubscribers, String gymId,
			Integer possibleMessageIdToUpdate)
			throws InterruptedException, TelegramApiException, DecoderException {
		boolean showStickers = PogoBot.getConfiguration().getShowStickers();
		boolean showRaidStickers = PogoBot.getConfiguration().getShowRaidStickers();
		SendSticker stickerMessage = forMonsters && showStickers || !forMonsters && showRaidStickers
				? createStickerMessage(chatId, stickerUrl)
				: null;
		logger.info("Sticker-end: " + stickerUrl);
		BotApiMethod<? extends Serializable> message = null;
		if (forMonsters) {
			message = createMessageForChat(messageText, chatId, possibleMessageIdToUpdate);
		} else {
			BotApiMethod<? extends Serializable> messageForChat = createMessageForChat(messageText,
					chatId,
					possibleMessageIdToUpdate);
			InlineKeyboardMarkup replyMarkup = telegramKeyboardService.getRaidSignupKeyboard(eventWithSubscribers,
					gymId);
			if (messageForChat instanceof EditMessageText) {
				// means we have a update raid and so we need a signup-keyboard
				((EditMessageText) messageForChat).setReplyMarkup(replyMarkup);
				((EditMessageText) messageForChat).enableMarkdown(true);
			} else if (messageForChat instanceof SendMessage) {
				// means we have a new raid and so we need a signup-keyboard
				((SendMessage) messageForChat).setReplyMarkup(replyMarkup);
				((SendMessage) messageForChat).enableMarkdown(true);
			}
			message = messageForChat;
		}
		boolean enableWebPagePreview = PogoBot.getConfiguration().getEnableWebPagePreview();
		boolean enableRaidWebPagePreview = PogoBot.getConfiguration().getEnableRaidWebPagePreview();
		setWebPagePreview(message, forMonsters ? enableWebPagePreview : enableRaidWebPagePreview);

		boolean showLocation = PogoBot.getConfiguration().getShowLocation();
		boolean showRaidLocation = PogoBot.getConfiguration().getShowRaidLocation();
		SendLocation location = forMonsters && showLocation || !forMonsters && showRaidLocation
				? createLocationMessage(chatId, latitude, longitude)
				: null;

		boolean isGroupMonsterMessage = gymId == null && possibleMessageIdToUpdate == null
				&& eventWithSubscribers == null;

		return sendAllMessagesForEventInternally(stickerMessage, message, location, isGroupMonsterMessage);
	}

	private void setWebPagePreview(BotApiMethod<? extends Serializable> message, boolean webPagePreview) {
		if (webPagePreview) {
			if (message instanceof SendMessage) {
				((SendMessage) message).enableWebPagePreview();
			} else if (message instanceof EditMessageText) {
				((EditMessageText) message).enableWebPagePreview();
			} else {
				logger.info("Couldn't disable webpage-preview for " + message.toString());
			}
		} else {
		if (message instanceof SendMessage) {
			((SendMessage) message).disableWebPagePreview();
		} else if (message instanceof EditMessageText) {
			((EditMessageText) message).disableWebPagePreview();
		} else {
			logger.info("Couldn't disable webpage-preview for " + message.toString());
		}

		}
	}

	private BotApiMethod<? extends Serializable> createMessageForChat(String pokemonFound, String chatId,
			Integer possibleMessageIdToUpdate) {
		if (possibleMessageIdToUpdate != null && possibleMessageIdToUpdate != 0) {
			EditMessageText messageForChat = updateMessageForChat(pokemonFound, chatId, possibleMessageIdToUpdate);
			messageForChat.enableMarkdown(true);
			return messageForChat;
		} else {
			SendMessage message = new SendMessage(chatId, pokemonFound);
			message.enableMarkdown(true);
			logger.debug("Created raid-message for " + chatId);
			return message;
		}
	}

	private EditMessageText updateMessageForChat(String newMessageText, String chatId,
			Integer possibleMessageIdToUpdate) {
		EditMessageText editMessage = new EditMessageText();
		editMessage.setChatId(chatId);
		editMessage.setMessageId(possibleMessageIdToUpdate);
		// editMessage.disableWebPagePreview();
		editMessage.enableMarkdown(true);
		editMessage.setText(newMessageText);
		return editMessage;
	}

	private SendSticker createStickerMessage(String chatId, String stickerUrl) {
		SendSticker stickerMessage = new SendSticker();
		stickerMessage.setChatId(chatId);
		stickerMessage.disableNotification();
		stickerMessage.setSticker(stickerUrl);
		return stickerMessage;
	}

	private SendLocation createLocationMessage(String chatId, Double latitude, Double longitude) {
		SendLocation location = new SendLocation();
		location.setChatId(chatId);
		location.disableNotification();
		location.setLatitude(latitude.floatValue());
		location.setLongitude(longitude.floatValue());
		return location;
	}

	@Autowired
	private UserRepository userDAO;

	@Override
	public Message sendMessageToRecipient(User user, String userMessage) {
		String chatId = user.getChatId() == null ? user.getTelegramId() : user.getChatId();
		SendMessage sendMessage = new SendMessage(chatId, userMessage);
		Message result = sendMessage(sendMessage);
		return result;
	}

	@Override
	public boolean sendMessageToAll(String message) {
		boolean result = false;
		for (User user : userDAO.findAll()) {
			sendMessageToRecipient(user, message);
		}
		result = true;
		return result;
	}

	@Override
	public boolean sendMessageToGroup(UserGroup group, String message) {
		boolean result = false;
		List<User> receivers = group.getGroupFilter().getReceivers();
		for (User user : receivers) {
			sendMessageToRecipient(user, message);
		}
		result = true;
		return result;
	}

	@Override
	public boolean sendMessageToFilteredPeople(Filter filter, String message) {
		boolean result = false;
		List<User> receivers = filter.getReceivers();
		if (receivers == null || receivers.isEmpty()) {
			receivers = new ArrayList<User>();
			receivers.add(filter.getOwner());
		}
		for (User user : receivers) {
			sendMessageToRecipient(user, message);
		}
		result = true;
		return result;
	}

	@Override
	@Transactional
	public void removeGroupRaidMessage() throws TelegramApiException {
		long nowInSecons = System.currentTimeMillis() / 1000;
		Iterable<GroupMessages> all = groupMessagesRepository.findAll();
		ProcessedRaids owningRaid = null;
		String errorsWhileDeleting = "";
		Iterable<UserGroup> allUserGroups = userGroupRepository.findAll();
		Map<Long, String> groups = new HashMap<>();
		allUserGroups.forEach(x -> groups.put(x.getChatId(), x.getGroupName().toString()));
		for (GroupMessages groupMessages : all) {
			owningRaid = groupMessages.getOwningRaid();
			Long endTime = null;
			if (owningRaid != null) {
				endTime = owningRaid.getEndTime();
			} else {
				logger.warn("There is no owning Raid for the message " + groupMessages.toString());
				return;
			}
			if (nowInSecons > endTime) {
				logger.info("Delete message - time difference in minutes is " + (nowInSecons - endTime) / 60);
				owningRaid = processedRaidRepository.findById(owningRaid.getId()).orElse(null);
				if (owningRaid == null) {
					logger.warn("Could not find owning raid in table");
					continue;
				}
				owningRaid.removeFromGroupsRaidIsPosted(groupMessages);
				processedRaidRepository.save(owningRaid);
				Long groupChatId = groupMessages.getGroupChatId();
				boolean success = true;
				TelegramApiException possibleException = null;
				if (groupChatId != null && groupChatId < 0) {
					try {
						Integer locationId = groupMessages.getLocationId();
						deleteMessage(groupChatId, locationId, groups.get(groupChatId));
					} catch (TelegramApiException e) {
						possibleException = e;
						errorsWhileDeleting += ("Location " + groupMessages.getLocationId() + " in chat " + groupChatId
								+ "\n  -> " + e.toString() + "\n");
						// success = false;
					}
					try {
						Integer stickerId = groupMessages.getStickerId();
						deleteMessage(groupChatId, stickerId, groups.get(groupChatId));
					} catch (TelegramApiException e) {
						possibleException = e;
						errorsWhileDeleting += ("Sticker " + groupMessages.getStickerId() + " in chat " + groupChatId
								+ "\n  -> " + e.toString() + "\n");
						// success = false;
					}
					try {
						Integer messageId = groupMessages.getMessageId();
						deleteMessage(groupChatId, messageId, groups.get(groupChatId));
					} catch (TelegramApiException e) {
						possibleException = e;
						errorsWhileDeleting += ("Message " + groupMessages.getMessageId() + " in chat " + groupChatId
								+ "\n  -> " + e.toString() + "\n");
						success = false;
					}
				}
				// eventWithSubscribersService.deleteEvent(owningRaid.getGymId());
				groupMessagesRepository.delete(groupMessages);
				if (!success) {
					logger.warn(errorsWhileDeleting
							+ "Some standard messages in chats couldn't be deleted, thrown Exception is: "
							+ possibleException.getMessage());
					// TODO: what kind of error handling is best here? (We also want to dele
					// throw possibleException;
				}
				if (possibleException != null) {
					logger.warn("Some messages in chats couldn't be deleted, thrown Exception was: "
							+ possibleException.getMessage());
				}
			}
		}

		Iterable<RaidAtGymEvent> allEvents = raidAtGymEventRepository.findAll();
		allEvents.forEach(raidEvent -> {
			if (raidEvent.getEnd() < nowInSecons) {
				eventWithSubscribersService.deleteEvent(raidEvent.getGymId());
			}
		});
	}

	@Override
	public void deleteMessage(Long groupChatId, Integer messageId, String groupName) throws TelegramApiException {
		DeleteMessage deleteMessage;
		if (messageId != null) {
			deleteMessage = new DeleteMessage(groupChatId, messageId);
			try {
				pogoBot.execute(deleteMessage);
				logger.info("Deleted message " + messageId + " in chat '" + groupName + "'");
			} catch (TelegramApiException e) {
				logger.error("Delete message " + messageId + " failed in chat: '" + groupName + "'", e.getCause());
				logger.error("Message: " + e.getMessage(), e);
				throw e;
			}
		}
	}


}
