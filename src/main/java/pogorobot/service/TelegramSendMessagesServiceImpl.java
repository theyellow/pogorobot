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
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

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
import pogorobot.entities.Gym;
import pogorobot.entities.PokemonWithSpawnpoint;
import pogorobot.entities.Raid;
import pogorobot.entities.SendMessages;
import pogorobot.entities.User;
import pogorobot.entities.UserGroup;
import pogorobot.events.EventMessage;
import pogorobot.service.db.repositories.UserRepository;
import pogorobot.telegram.PogoBot;
import pogorobot.telegram.util.SendMessageAnswer;
import pogorobot.telegram.util.Type;

@Service("telegramSendMessagesService")
public class TelegramSendMessagesServiceImpl implements TelegramSendMessagesService {

	private static Logger logger = LoggerFactory.getLogger(TelegramSendMessagesService.class);

	@Autowired
	private PogoBot pogoBot;

	@Autowired
	private TelegramTextService telegramTextService;


	@Autowired
	private TelegramKeyboardService telegramKeyboardService;


	private static Iterator<Integer> sendMessageIterator = new Iterator<Integer>() {

		private int i;

		@Override
		public boolean hasNext() {
			return i < Integer.MAX_VALUE - 1;
		}

		@Override
		public Integer next() {
			if (hasNext()) {
				i++;
			} else {
				i = Integer.MIN_VALUE + 1;
				logger.warn("internal id map of send messages was full, reiterate from beginning.");
				if (i == 0) {
					// not reachable, because of hack above
					throw new NoSuchElementException(
							"iterator for map of sendMessages and internal id mapping is full.");
				}
			}
			return i;
		}
	};

	@Override
	public SendMessageAnswer sendMonMessage(PokemonWithSpawnpoint pokemon, String chatId,
			Integer possibleMessageIdToUpdate)
			throws FileNotFoundException, TelegramApiException, InterruptedException, DecoderException {
		return sendStandardMessage(pokemon, null, null, chatId, possibleMessageIdToUpdate);
	}

	@Override
	public SendMessageAnswer sendMonMessage(PokemonWithSpawnpoint pokemon, String chatId,
			SortedSet<EventWithSubscribers> eventWithSubscribers)
			throws FileNotFoundException, TelegramApiException, InterruptedException, DecoderException {
		return sendStandardMessage(pokemon, null, eventWithSubscribers, chatId, null);
	}

	@Override
	public SendMessageAnswer sendRaidMessage(Gym gym, String chatId,
			SortedSet<EventWithSubscribers> eventWithSubscribers, Integer possibleMessageIdToUpdate)
			throws FileNotFoundException, TelegramApiException, InterruptedException, DecoderException {
		return sendStandardMessage(null, gym, eventWithSubscribers, chatId, possibleMessageIdToUpdate);
	}

	@Override
	public SendMessageAnswer sendEggMessage(String chatId, Gym gym, String level,
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
		// SendMessageAnswer answer = sendMessages(chatId, url, latitude, longitude,
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
						"errorCode " + x.getErrorCode() + " - "
								+ (x.getApiResponse() != null ? x.getApiResponse() : x.getMessage())
								+ (x.getParameters() != null ? " - parameter: " + x.getParameters().toString() : ""),
						x.getCause());
			} else if (e instanceof TelegramApiValidationException) {
				TelegramApiValidationException x = (TelegramApiValidationException) e;
				logger.error("method: " + x.getMethod() + " - " + x.getObject() + " - error: " + x.toString(),
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

	@Override
	public Message sendMessageTimed(Long chatId, PartialBotApiMethod<Message> message) {
		Message result = null;
		try {
			if (message instanceof SendSticker) {
				result = pogoBot.execute((SendSticker) message);
			} else if (message != null) {
				pogoBot.executeTimed(chatId, (BotApiMethod<Message>) message);
			}
		} catch (TelegramApiException e) {
			if (e instanceof TelegramApiRequestException) {
				TelegramApiRequestException x = (TelegramApiRequestException) e;
				logger.error(
						"errorCode " + x.getErrorCode() + " - "
								+ (x.getApiResponse() != null ? x.getApiResponse() : x.getMessage())
								+ (x.getParameters() != null ? " - parameter: " + x.getParameters().toString() : ""),
						x.getCause());
			} else if (e instanceof TelegramApiValidationException) {
				TelegramApiValidationException x = (TelegramApiValidationException) e;
				logger.error("method: " + x.getMethod() + " - " + x.getObject() + " - error: " + x.toString(),
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
					logger.error("BotApiMethod - error in method: " + myMessage.getMethod());

				}
			}
		}
		if (null != result) {
			logger.info("Got result " + result);
		}
		return result;
	}

	private SendMessageAnswer sendAllMessagesForEventInternally(SendSticker stickerMessage,
			BotApiMethod<? extends Serializable> message, SendLocation location, boolean isGroupMessage)
			throws InterruptedException, TelegramApiException {
		SendMessageAnswer answer = new SendMessageAnswer();
		if (stickerMessage != null) {
			Thread.sleep(100);
			Message sendMessage = sendMessageTimed(Long.valueOf(stickerMessage.getChatId()),stickerMessage);
			answer.setLocationAnswer(sendMessage.getMessageId());
			Thread.sleep(100);
		}
		Semaphore mutex = new Semaphore(1);
		if (message instanceof SendMessage) {
			SendMessage sendMessage = (SendMessage) message;
			sendMessage.enableMarkdown(true);
			ReplyKeyboard originalKeyboard = sendMessage.getReplyMarkup();
			ReplyKeyboard replyMarkup = originalKeyboard == null && !isGroupMessage
					&& !(originalKeyboard instanceof InlineKeyboardMarkup)
							? telegramKeyboardService.getSettingsKeyboard(true)
							: originalKeyboard;
			sendMessage.setReplyMarkup(replyMarkup);
			Integer next = sendMessageIterator.next();
			pogoBot.putSendMessages(next, 0);
			pogoBot.sendTimed(Long.valueOf(sendMessage.getChatId()), sendMessage, next, mutex);
			mutex.acquire();
			// waitUntilPosted(next);
			Integer sendMessagesInternalId = pogoBot.getSendMessages(next);
			if (sendMessagesInternalId == null || sendMessagesInternalId == Integer.MIN_VALUE
					|| sendMessagesInternalId == Integer.MAX_VALUE) {
				pogoBot.removeSendMessage(next);
			} else {
				answer.setMainMessageAnswer(sendMessagesInternalId);
				pogoBot.removeSendMessage(next);
			}
			mutex.release();
		} else if (message instanceof EditMessageText) {
			EditMessageText editMessage = ((EditMessageText) message);
			editMessage.enableMarkdown(true);
			Integer next = sendMessageIterator.next();
			pogoBot.putSendMessages(next, 0);
			pogoBot.sendTimed(Long.valueOf(editMessage.getChatId()), editMessage, next, mutex);
			mutex.acquire();
			Integer sendMessagesInternalId = pogoBot.getSendMessages(next);
			if (sendMessagesInternalId == null || sendMessagesInternalId == Integer.MIN_VALUE
				|| sendMessagesInternalId == Integer.MAX_VALUE) {
				pogoBot.removeSendMessage(next);
			} else {
				answer.setMainMessageAnswer(sendMessagesInternalId);
				pogoBot.removeSendMessage(next);
			}
			mutex.release();
		} else if (message instanceof DeleteMessage) {
			Integer sendMessagesInternalId = null;
			DeleteMessage deleteMessage = (DeleteMessage) message;
//			Semaphore mutex = new Semaphore(1);
			try {
				// pogoBot.execute(deleteMessage);
				// Hack for not mixing up ids:
				// TODO: cleanup
				Integer next = sendMessageIterator.next() - Integer.MIN_VALUE + 2;
				pogoBot.putSendMessages(next, 0);
				pogoBot.sendTimed(Long.valueOf(deleteMessage.getChatId()), deleteMessage, next, mutex);
				logger.info("waiting delete message " + deleteMessage.getMessageId() + " for chat '" + deleteMessage.getChatId() + "'");
				// waitUntilPosted(next);
				boolean acquired = mutex.tryAcquire(1000, TimeUnit.MILLISECONDS);
				if (acquired) {
					sendMessagesInternalId = pogoBot.getSendMessages(next);
					if (sendMessagesInternalId == null || sendMessagesInternalId == Integer.MIN_VALUE 
							|| sendMessagesInternalId == Integer.MAX_VALUE) {
						pogoBot.removeSendMessage(next);
					} else {
						answer.setMainMessageAnswer(sendMessagesInternalId);
						pogoBot.removeSendMessage(next);
					}
					mutex.release();
				}
			} catch (Exception e) {
				logger.error("delete message " + deleteMessage.getMessageId() + " failed in chat: '" + deleteMessage.getChatId() + "'", e.getCause());
				logger.error("message: " + e.getMessage(), e);
				if (e instanceof TelegramApiException) {
					throw (TelegramApiException) e;
				} else if (e instanceof Exception) {
					throw new TelegramApiException(e);
				}
			}
		}
		if (location != null) {
			if (!isGroupMessage) {
				location.setReplyMarkup(telegramKeyboardService.getSettingsKeyboard(true));
			}
			Integer next = sendMessageIterator.next();
			pogoBot.putSendMessages(next, 0);
			pogoBot.sendTimed(Long.valueOf(location.getChatId()), location, next, mutex);
			mutex.acquire();
			Integer sendMessagesInternalId = pogoBot.getSendMessages(next);
			if (sendMessagesInternalId == null || sendMessagesInternalId == Integer.MIN_VALUE 
					|| sendMessagesInternalId == Integer.MAX_VALUE) {
				pogoBot.removeSendMessage(next);
			} else {
				answer.setMainMessageAnswer(sendMessagesInternalId);
				pogoBot.removeSendMessage(next);
			}
			mutex.release();
		}
		return answer;
	}

	private ConcurrentLinkedQueue<EventMessage<?>> eventQueue = new ConcurrentLinkedQueue<>();

	// private void waitUntilPosted(Integer next) {
	// while (next != Integer.MIN_VALUE && next != Integer.MAX_VALUE &&
	// pogoBot.getSendMessages(next) != null
	// && pogoBot.getSendMessages(next) == 0) {
	//
	// try {
	// Thread.sleep(123L);
	// } catch (InterruptedException e) {
	// logger.warn("wait until posted message got interupted - shutting down this
	// thread -> "
	// + Thread.currentThread().getName());
	// Thread.currentThread().interrupt();
	// }
	// }
	// }

	@Override
	public SendMessageAnswer sendStandardMessage(PokemonWithSpawnpoint pokemon, Gym fullGym,
			SortedSet<EventWithSubscribers> eventWithSubscribers, String chatId, Integer possibleMessageIdToUpdate)
			throws FileNotFoundException, TelegramApiException, InterruptedException, DecoderException {

		// TODO: Refactor with this raidPokemon and call to different methods for
		// monsters and egg/raids
		Type type = null;
		if (pokemon == null && fullGym != null) {
			type = Type.RAID;
		} else if (pokemon != null && fullGym == null) {
			type = Type.POKEMON;
		} else {
			logger.warn("tried to send mon-message without mon or gym...");
			return null;
		}

		Double latitude = type.equals(Type.RAID) ? fullGym.getLatitude() : pokemon.getLatitude();
		Double longitude = type.equals(Type.RAID) ? fullGym.getLongitude() : pokemon.getLongitude();
		Raid raid = type.equals(Type.RAID) ? fullGym.getRaid() : null;

		// Meanings (if it's a raid, for monsters it's always positive):
		// idForSticker > 0 -> monster-sticker
		// idForSticker < 0 -> egg-sticker
		// idForSticker = 0 -> shouldn't happen
		int stickerId = 0;
		if (type.equals(Type.RAID)) {
			Long pokemonId = raid.getPokemonId();
			boolean isEgg = pokemonId == null || pokemonId <= 0L;
			logger.debug("get sticker for " + (isEgg ? "egg" : "raid") + " - look for id " + pokemonId);
			stickerId = isEgg ? -1 * raid.getRaidLevel().intValue() : pokemonId.intValue();
		} else {
			logger.debug("get sticker for pokemon");
			stickerId = pokemon.getPokemonId().intValue();
		}

		String gymId = fullGym == null ? null : fullGym.getGymId();
		String messageText = "";
		if (type.equals(Type.RAID)) {
			String raidMessagePokemonText = telegramTextService.getRaidMessagePokemonText(fullGym);
			String participantsText = telegramTextService.getParticipantsText(eventWithSubscribers);
			messageText = raidMessagePokemonText + participantsText;
			logger.debug("Created text for " + chatId + ": Raid: " + raidMessagePokemonText);
		} else {
			messageText = telegramTextService.createPokemonMessageWithIVText(pokemon);
			String pokemonName = telegramTextService.getPokemonName(pokemon.getPokemonId().toString());
			logger.debug("Created text for " + chatId + ": Mon " + pokemonName);
		}
		String stUrl = telegramTextService.getStickerUrl(stickerId);
		return sendMessages(chatId, stUrl, latitude, longitude, messageText, type.equals(Type.POKEMON), eventWithSubscribers, gymId,
				possibleMessageIdToUpdate);
	}

	// private String getThreeDigitFormattedPokemonId(int pokemonInt) {
	// return pokemonInt >= 100 ? pokemonInt + "" : (pokemonInt >= 10 ? "0" +
	// pokemonInt : "00" + pokemonInt);
	// }

	private SendMessageAnswer sendMessages(String chatId, String stickerUrl, Double latitude, Double longitude,
			String messageText, boolean forMonsters, SortedSet<EventWithSubscribers> eventWithSubscribers, String gymId,
			Integer possibleMessageIdToUpdate) throws InterruptedException, TelegramApiException, DecoderException {
		boolean showStickers = PogoBot.getConfiguration().getShowStickers();
		boolean showRaidStickers = PogoBot.getConfiguration().getShowRaidStickers();
		SendSticker stickerMessage = forMonsters && showStickers || !forMonsters && showRaidStickers
				? createStickerMessage(chatId, stickerUrl)
				: null;
		logger.debug("sticker-end: " + stickerUrl);
		BotApiMethod<? extends Serializable> message = null;
		if (forMonsters) {
			message = createMessageForChat(messageText, chatId, possibleMessageIdToUpdate);
		} else {
			BotApiMethod<? extends Serializable> messageForChat = createMessageForChat(messageText, chatId,
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
				logger.info("couldn't disable webpage-preview for " + message.toString());
			}
		} else {
			if (message instanceof SendMessage) {
				((SendMessage) message).disableWebPagePreview();
			} else if (message instanceof EditMessageText) {
				((EditMessageText) message).disableWebPagePreview();
			} else {
				logger.info("couldn't disable webpage-preview for " + message.toString());
			}

		}
	}

	private BotApiMethod<? extends Serializable> createMessageForChat(String chatText, String chatId,
			Integer possibleMessageIdToUpdate) {
		if (possibleMessageIdToUpdate != null && possibleMessageIdToUpdate != 0) {
			EditMessageText message = updateMessageForChat(chatText, chatId, possibleMessageIdToUpdate);
			message.enableMarkdown(true);
			logger.debug("created edit message for " + chatId + " with messageIdToUpdate " + possibleMessageIdToUpdate);
			return message;
		} else {
			SendMessage message = new SendMessage(chatId, chatText);
			message.enableMarkdown(true);
			logger.debug("created new message for " + chatId);
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
		
		// TODO: reenable stickers
//		stickerMessage.setSticker(stickerUrl);
		return stickerMessage;
	}

	private SendLocation createLocationMessage(String chatId, Double latitude, Double longitude) {
		SendLocation location = new SendLocation();
		location.setChatId(chatId);
		location.disableNotification();
		location.setLatitude(latitude);
		location.setLongitude(longitude);
		return location;
	}

	@Override
	public String deleteMessagesOnTelegram(SendMessages sendMessagesClone, long timeDifference) {
		String errorsWhileDeleting = "";
		// TelegramApiException possibleException = null;
		Long chatId = sendMessagesClone.getGroupChatId();
		Integer locationId = sendMessagesClone.getLocationId();
		Integer stickerId = sendMessagesClone.getStickerId();
		Integer messageId = sendMessagesClone.getMessageId();
		try {
			errorsWhileDeleting = deleteMessagesOnTelegram(chatId, locationId, stickerId, messageId, timeDifference);
		} catch (TelegramApiException e) {
			// possibleException = e;
			logger.warn("some messages in chats couldn't be deleted, thrown Exception was: " + e.getMessage());
		}
		return errorsWhileDeleting;
	}

	private String deleteMessagesOnTelegram(Long chatId, Integer locationId, Integer stickerId, Integer messageId,
			Long timeDifference) throws TelegramApiException {
		boolean success = true;
		TelegramApiException possibleException = null;
		String errorsWhileDeleting = "";
		// Now try to delete all:
		if (chatId != null && chatId != 0) {
			boolean deletedMessage = true;
			if (locationId != null) {
				try {
					deletedMessage = deleteMessage(chatId, locationId);
				} catch (TelegramApiException e) {
					possibleException = e;
					deletedMessage = false;
					errorsWhileDeleting += ("location " + locationId + " in chat " + chatId + "\n  -> " + e.toString()
							+ "\n");
					// success = false;
				}
				if (!deletedMessage) {
					logger.info("Can't delete location in chat " + chatId + " and location " + locationId);
				}
			}
			deletedMessage = true;
			if (stickerId != null) {

				try {
					deletedMessage = deleteMessage(chatId, stickerId);
				} catch (TelegramApiException e) {
					possibleException = e;
					deletedMessage = false;
					errorsWhileDeleting += ("sticker " + stickerId + " in chat " + chatId + "\n  -> " + e.toString()
							+ "\n");
					// success = false;
				}
				if (!deletedMessage) {
					logger.info("Can't delete sticker in chat " + chatId + " and sticker " + stickerId);
				}
			}
			deletedMessage = true;
			if (messageId != null) {
				try {
					if (messageId != 0) {
						deletedMessage = deleteMessage(chatId, messageId);
					}
				} catch (TelegramApiException e) {
					possibleException = e;
					deletedMessage = false;
					errorsWhileDeleting += ("message " + messageId + " in chat " + chatId + "\n  -> " + e.toString()
							+ "\n");
					success = false;
				}
			}
			if (!deletedMessage) {
				logger.info("Can't delete message in chat " + chatId + " and message " + messageId);
			}
		}

		if (!success) {
			logger.warn(
					errorsWhileDeleting + "some standard messages in chats couldn't be deleted, thrown Exception is: "
							+ possibleException.getMessage());
			// TODO: what kind of error handling is best here? (We also want to dele
			// throw possibleException;
		} else {
			long differenceInMinutes = timeDifference / 60;
			if (differenceInMinutes > 1) {
				logger.warn("deleted message on database, difference is " + differenceInMinutes + " minutes");
			} else {
				logger.info("deleted message on database, difference is " + differenceInMinutes + " minutes");
			}
		}

		if (possibleException != null) {
			logger.warn("some messages in chats couldn't be deleted, thrown Exception was: "
					+ possibleException.getMessage());
		}

		// if (possibleException != null) {
		// throw possibleException;
		// }
		return errorsWhileDeleting;
	}

	// private boolean deleteMessage(Long chatId, Integer locationId) throws
	// TelegramApiException {
	// // TODO Auto-generated method stub
	// return false;
	// }

	@Override
	public boolean deleteMessage(Long groupChatId, Integer messageId) throws TelegramApiException {
		DeleteMessage deleteMessage;
		if (messageId != null) {
			deleteMessage = new DeleteMessage(Long.toString(groupChatId), messageId);
			SendMessageAnswer sendMessageAnswer = null;
			try {
				sendMessageAnswer = sendAllMessagesForEventInternally(null, deleteMessage, null, true);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				logger.warn("InterruptedException: Errors while sending deleteMessage to telegram. Sytem threw ", e1.getLocalizedMessage());
			} catch (TelegramApiException tae) {
				// TODO Auto-generated catch block
				throw tae;
			}
			if (sendMessageAnswer != null && sendMessageAnswer.getMainMessageAnswer() != null)
			{
				logger.info("deleteMessage in telegram gave true with mainAnswer: " + sendMessageAnswer.getMainMessageAnswer());
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	@Autowired
	private UserRepository userDAO;

	@Override
	public void sendMessageToRecipient(User user, String userMessage) {
		String chatId = user.getChatId() == null ? user.getTelegramId() : user.getChatId();
		SendMessage sendMessage = new SendMessage(chatId, userMessage);
		sendMessageTimed(Long.valueOf(chatId), sendMessage);
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



}
