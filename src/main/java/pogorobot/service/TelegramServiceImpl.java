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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.apache.commons.codec.DecoderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

import pogorobot.entities.EventWithSubscribers;
import pogorobot.entities.Filter;
import pogorobot.entities.FilterType;
import pogorobot.entities.Gym;
import pogorobot.entities.PokemonWithSpawnpoint;
import pogorobot.entities.ProcessedPokemon;
import pogorobot.entities.ProcessedRaids;
import pogorobot.entities.RaidAtGymEvent;
import pogorobot.entities.SendMessages;
import pogorobot.entities.User;
import pogorobot.entities.UserGroup;
import pogorobot.service.db.EventWithSubscribersService;
import pogorobot.service.db.FilterService;
import pogorobot.service.db.GymService;
import pogorobot.service.db.UserService;
import pogorobot.service.db.repositories.FilterRepository;
import pogorobot.service.db.repositories.ProcessedPokemonRepository;
import pogorobot.service.db.repositories.ProcessedRaidRepository;
import pogorobot.service.db.repositories.RaidAtGymEventRepository;
import pogorobot.service.db.repositories.UserGroupRepository;
import pogorobot.telegram.util.SendMessageAnswer;

@Service("telegramService")
public class TelegramServiceImpl implements TelegramService {

	private static final String API_RESPONSE = "API response: ";

	private static final String GOT_INTERRUPTED = "got interrupted";

	private static Logger logger = LoggerFactory.getLogger(TelegramService.class);

	@Autowired
	private ProcessedPokemonRepository processedPokemonDAO;

	@Autowired
	private ProcessedRaidRepository processedRaidRepository;

	@Autowired
	private RaidAtGymEventRepository raidAtGymEventDAO;

	@Autowired
	private UserService userService;

	@Autowired
	private TelegramSendMessagesService telegramSendMessagesService;

	@Autowired
	private TelegramTextService telegramTextService;

	@Autowired
	private FilterService filterService;

	@Autowired
	private GymService gymService;

	@Autowired
	private EventWithSubscribersService eventWithSubscribersService;

	@Autowired
	private FilterRepository filterDAO;

	@Autowired
	private UserGroupRepository userGroupRepository;

	@Override
	@Transactional(TxType.REQUIRES_NEW)
	public void triggerPokemonMessages(PokemonWithSpawnpoint pokemon) {
		if (pokemon.getPokemonId() != null) {
			boolean deepScan = false;

			List<ProcessedPokemon> processedPokemon = processedPokemonDAO.findByEncounterId(pokemon.getEncounterId());
			ProcessedPokemon processedMon = null;
			List<Long> updatedChats = new ArrayList<>();
			if (processedPokemon != null) {
				logger.debug("pokemon already encountered with encounterId " + pokemon.getEncounterId() + " , pokemon: "
						+ pokemon);
				if (pokemon.getWeight() != null && pokemon.getDisappearTime() != null) {
					deepScan = true;
					logger.debug("it's a detailed mon-scan, so check iv-filter again and update message...");
					if (!processedPokemon.isEmpty()) {
						List<Set<SendMessages>> possibleMessageIdToUpdate = processedPokemon.stream()
								.map(x -> x.getChatsPokemonIsPosted()).collect(Collectors.toList());
						for (Set<SendMessages> set : possibleMessageIdToUpdate) {
							set.stream().forEach(x -> {
								SendMessageAnswer updateMonsterMessage = updateMonsterMessage(pokemon, x, true);
								if (updateMonsterMessage != null) {
									updatedChats.add(x.getGroupChatId());
								}
							});
						}
						// return;
					}
					// if (processedMon == null) {
					// ProcessedPokemon newPokemon = new ProcessedPokemon();
					// newPokemon.setEncounterId(pokemon.getEncounterId());
					// newPokemon.setEndTime(pokemon.getDisappearTime());
					// processedMon = processedPokemonDAO.save(newPokemon);
					// }
				} else {
					logger.debug("but it's no detail-scan");
				}
			}

			if (processedMon == null) {
				ProcessedPokemon newPokemon = new ProcessedPokemon();
				newPokemon.setEncounterId(pokemon.getEncounterId());
				newPokemon.setEndTime(pokemon.getDisappearTime());
				processedMon = processedPokemonDAO.save(newPokemon);
			}

			// Process all users:
			for (User user : userService.getAllUsers()) {
				if (user.isShowPokemonMessages()) {
					String chatId = user.getChatId() == null ? user.getTelegramId() : user.getChatId();
					Long userFilter = user.getUserFilter().getId();
					if (!updatedChats.contains(Long.valueOf(chatId))) {
						CompletableFuture<SendMessageAnswer> monsterFuture = sendPokemonIfFilterMatch(pokemon, chatId,
								userFilter, deepScan, null);
						SendMessageAnswer answer = getFutureAnswer(monsterFuture);
						updateProcessedMonster(processedMon, answer, chatId);
					} else {
						logger.debug("message was already posted (and perhaps edited), no reposting necessary");
					}

				}
			}
			// Workaround to get info about deep-scanning param to group-filters
			final boolean onlyDeep = deepScan;

			// Process all groups
			final ProcessedPokemon processedMonFinal = processedMon;

			// Retrieve groupfilter information from db
			Map<Long, String> usergroupFilters = new HashMap<>();
			userGroupRepository.findAll().iterator().forEachRemaining(group -> {
				Long groupFilterId = group.getGroupFilter().getId();
				String chatId = String.valueOf(group.getChatId());

				usergroupFilters.put(groupFilterId, chatId);
			});

			// Send messages to all matching groupfilters
			usergroupFilters.forEach((groupfilterId, chatId) -> {
				if (!updatedChats.contains(Long.valueOf(chatId))) {
					logger.debug("chat {} will be tested with monster encounter {}", chatId, pokemon.getEncounterId());
					CompletableFuture<SendMessageAnswer> monsterFuture = sendPokemonIfFilterMatch(pokemon, chatId,
							groupfilterId, onlyDeep, null);
					if (monsterFuture != null) {
						SendMessageAnswer answer = getFutureAnswer(monsterFuture);
						updateProcessedMonster(processedMonFinal, answer, chatId);
					} else {
						logger.debug("got no future for monster encounter {} in chat {}, filter didn't match?",
								pokemon.getEncounterId(), chatId);
					}
				} else {
					logger.debug("message was already posted (and perhaps edited), no reposting necessary");
				}
			});

			// Special logging for iv
			logIvs(pokemon);

		} else {
			logger.debug("No mon-id found");
		}
	}

	private void logIvs(PokemonWithSpawnpoint pokemon) {
		String individualAttack = pokemon.getIndividualAttack();
		String individualDefense = pokemon.getIndividualDefense();
		String individualStamina = pokemon.getIndividualStamina();
		boolean ivsGiven = individualAttack != null && individualDefense != null && individualStamina != null
				&& !individualAttack.isEmpty() && !individualDefense.isEmpty() && !individualStamina.isEmpty();
		if (ivsGiven) {
			Double ivs = telegramTextService.calculateIVs(Integer.valueOf(individualAttack),
					Integer.valueOf(individualDefense), Integer.valueOf(individualStamina));

			if (ivs != null) {
				Logger ivLogger = LoggerFactory.getLogger("iv");
				Date now = new Date();
				DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd; HH:mm:ss");
				String ivString = ivs.toString() + "0";
				ivString = ivString.substring(0, ivString.indexOf(".") + 2);
				ivLogger.debug("{}; {}; {}; {}; {}", dateFormat.format(now), ivString, pokemon.getPokemonId(),
						pokemon.getLatitude(), pokemon.getLongitude());
			}
		}
	}

	private SendMessageAnswer updateMonsterMessage(PokemonWithSpawnpoint pokemon, SendMessages sendMessage,
			boolean deepScan) {

		logger.info(
				"trigger updateMonsterMessage-editmessage future from triggerMonsterMessages for processed monster ");
		Long chatId = sendMessage.getGroupChatId();
		CompletableFuture<SendMessageAnswer> future = startSendMonsterFuture(pokemon, chatId.toString(),
				sendMessage.getMessageId());
		SendMessageAnswer answer = getFutureAnswer(future);

		// TODO: This code is possibly missing now
		// if (answer != null && (answer.getLocationAnswer() != null ||
		// answer.getStickerAnswer() != null
		// || answer.getMainMessageAnswer() != null)) {
		// updateProcessedMonster(processedPokemon, answer);
		// }

		return answer;
	}

	private CompletableFuture<SendMessageAnswer> sendPokemonIfFilterMatch(PokemonWithSpawnpoint pokemon, String chatId,
			Long filterId, boolean onlyDeepScan, Integer possibleMessageIdToUpdate) {
		if (filterId == null) {
			logger.warn("could not find filter without id");
			return null;
		}

		logger.debug("begin filter analyze of filter {}", filterId);
		Filter filter = filterDAO.findById(filterId).orElse(null);
		if (filter == null) {
			logger.warn("could not find filter {}", filterId);
			return null;
		}
		CompletableFuture<SendMessageAnswer> monsterFuture = null;
		boolean withIv = pokemon.getIndividualAttack() != null && !pokemon.getIndividualAttack().isEmpty();
		Double radiusPokemon = filter.getRadiusPokemon();
		if (withIv) {
			logger.debug("ivs given, begin...");
			Double minIV = filter.getMinIV();
			Double maxIV = filter.getMaxIV();
			if (minIV != null) {
				logger.debug("begin analyze IV for {}", filter.getId());
				Integer attack = Integer.valueOf(pokemon.getIndividualAttack());
				Integer defense = Integer.valueOf(pokemon.getIndividualDefense());
				Integer stamina = Integer.valueOf(pokemon.getIndividualStamina());

				Double calculatedIVs = telegramTextService.calculateIVs(attack, defense, stamina);
				boolean ivmatch = minIV <= calculatedIVs;
				if (maxIV != null) {
					ivmatch = ivmatch && maxIV >= calculatedIVs;
				}
				if (ivmatch) {
					Double latitude = filter.getLatitude();
					Double longitude = filter.getLongitude();
					Double monLatitude = pokemon.getLatitude();
					Double monLongitude = pokemon.getLongitude();
					Double radiusIV = filter.getRadiusIV() == null ? radiusPokemon : filter.getRadiusIV();
					if (radiusIV != null && radiusPokemon != null && radiusIV < radiusPokemon) {
						radiusIV = radiusPokemon;
					}
					// boolean nearby = filterService.isDistanceNearby(monLatitude, monLongitude,
					// latitude, longitude,
					// radiusIV);
					if (filterService.isDistanceNearby(monLatitude, monLongitude, latitude, longitude, radiusIV)
							|| filterService.isPointInOneOfManyGeofences(monLatitude, monLongitude,
									filter.getIvGeofences())
					// || filterService.isPointInOneOfManyGeofences(monLatitude, monLongitude,
					// filter.getGeofences())
					) {
						logger.debug("start creating new future to send mon {} to {}", pokemon.getPokemonId(), chatId);
						monsterFuture = startSendMonsterFuture(pokemon, chatId, possibleMessageIdToUpdate);
						return monsterFuture;
					} else {
						logger.info("pokemon {} isn't nearby or in a chosen iv area for filter {}",
								pokemon.getPokemonId(), filter.getId());
					}
				} else {
					logger.debug("iv didn't match for pokemon {} and filter {} : min iv is {} , calculated iv {}",
							pokemon.getPokemonId(), filter.getId(), minIV, calculatedIVs);
				}
			} else {
				logger.debug("no min iv given in filter {}", filter.getId());
			}
		} else {
			logger.debug("no iv scanning for filter {} because no iv given for pokemon {} at spawnpoint {}",
					filter.getId(), pokemon.getPokemonId(), pokemon.getSpawnpointId());
		}
		if (filter.getPokemons().contains(pokemon.getPokemonId().intValue())) {
			logger.debug("begin of pokemon-search by area/nearby");
			if (filter.getOnlyWithIV() != null && filter.getOnlyWithIV()) {
				logger.debug("only-iv filtering stops sending message to {}", chatId);
				return null;
			}
			Double latitude = filter.getLatitude();
			Double longitude = filter.getLongitude();
			Double monLatitude = pokemon.getLatitude();
			Double monLongitude = pokemon.getLongitude();
			Double radius = radiusPokemon;

			logger.debug("begin looking for nearby or geofence");
			boolean nearby = filterService.isDistanceNearby(monLatitude, monLongitude, latitude, longitude, radius);
			if (nearby || filterService.isPointInOneOfManyGeofences(monLatitude, monLongitude, filter.getGeofences())) {
				logger.debug("pokemon {} will be send to {}", pokemon.getPokemonId(), chatId);
				monsterFuture = startSendMonsterFuture(pokemon, chatId, possibleMessageIdToUpdate);
				return monsterFuture;
			}
		}
		// else {
		// String msg = "no nearby- or area-search for filter " + filter.getId() + "
		// because ";
		//
		// if (onlyDeepScan &&
		// filter.getPokemons().contains(pokemon.getPokemonId().intValue())) {
		// msg += " a deep inspection (pokemon 'spawned 2nd time' with iv-details) of iv
		// was happening";
		// monsterFuture = startSendMonsterFuture(pokemon, chatId,
		// possibleMessageIdToUpdate);
		// } else {
		// msg += " pokemon " + pokemon.getPokemonId() + " is not in list";
		// }
		// logger.debug(msg);
		// }
		return monsterFuture;
	}

	private CompletableFuture<SendMessageAnswer> startSendMonsterFuture(PokemonWithSpawnpoint pokemon, String chatId,
			Integer possibleMessageIdToUpdate) {
		return startNewMessageFuture(null, null, chatId, null, possibleMessageIdToUpdate, pokemon, "pokemon");
	}

	@Override
	@Transactional(TxType.REQUIRES_NEW)
	public void triggerRaidMessages(RaidAtGymEvent event) {
		String gymId = event.getGymId();
		Gym gym = gymService.getGym(gymId);
		Long end = event.getEnd();

		// TODO: Shouldn't this be at another place?
		// String quickMove = gym.getRaid().getMove1();
		// quickMove = quickMove != null ? quickMove : "Unbekannt";
		// String chargeMove = gym.getRaid().getMove2();
		// chargeMove = chargeMove != null ? chargeMove : "Unbekannt";

		if (gymId != null && end != null) {
			Gym fullGym = gymService.getGym(gymId);
			Long level = event.getLevel();
			SortedSet<EventWithSubscribers> eventsWithSubscribers;
			if (event.hasEventWithSubscribers()) {
				eventsWithSubscribers = event.getEventsWithSubscribers();
			} else {
				eventsWithSubscribers = eventWithSubscribersService.getSubscribersForRaid(gymId);
			}

			Long pokemonIdLong = event.getPokemonId();
			int pokemonId = pokemonIdLong == null ? -1 : pokemonIdLong.intValue();
			List<ProcessedRaids> processedRaids = processedRaidRepository.findByGymId(gymId);
			boolean alreadyPosted = false;

			// 1st we look if it was already posted, so we need to update instead of
			// resend..
			if (processedRaids != null) {
				boolean sendOnlyUpdate = false;
				logger.debug("got event at gym that has " + processedRaids.size() + " entries");
				for (ProcessedRaids processedRaid : processedRaids) {

					Set<SendMessages> sendMessages = null;
					// List<ProcessedRaids> processedGymIds =
					// processedRaidRepository.findByGymId(gym.getGymId());
					// if (!processedGymIds.isEmpty() && processedGymIds.size() == 1) {
					// ProcessedRaids processedRaid = processedGymIds.get(0);
					sendMessages = processedRaid.getGroupsRaidIsPosted();

					// Needed trigger to initialize resultSet:
					logger.debug("there are " + sendMessages.size() + " chats where this raid is posted");
					Map<Integer, Long> messages = new HashMap<>();
					for (SendMessages sendMessage : sendMessages) {
						messages.put(sendMessage.getMessageId(), sendMessage.getGroupChatId());
					}
					for (Entry<Integer, Long> sendMessage : messages.entrySet()) {
						sendOnlyUpdate = true;
						Integer messageId = sendMessage.getKey();
						Long groupChatId = sendMessage.getValue();
						if (null == groupChatId) {
							logger.warn("can't edit message " + messageId + " of unknown chat");
							continue;
						}

						// magic number: pokemonId -1 means "egg"
						boolean raidMessage = !(pokemonId == -1);
						CompletableFuture<SendMessageAnswer> future = null;
						if (raidMessage) {
							logger.info("trigger raid-editmessage future from triggerRaidMessages for processed raid "
									+ processedRaid.getId());
							future = startNewRaidMessageFuture(gym, groupChatId.toString(), eventsWithSubscribers,
									messageId);
						} else {
							logger.info("trigger egg-editmessage future from triggerRaidMessages for processed raid "
									+ processedRaid.getId());
							future = startNewEggMessageFuture(gym, gym.getRaid().getRaidLevel(), groupChatId.toString(),
									eventsWithSubscribers, messageId);
						}
						SendMessageAnswer answer = getFutureAnswer(future);
						if (answer != null && (answer.getLocationAnswer() != null || answer.getStickerAnswer() != null
								|| answer.getMainMessageAnswer() != null)) {
							updateProcessedRaid(processedRaid, answer, groupChatId);
						}
						// }
						// }

					}

					// if (processedRaid.getEndTime().compareTo(end) == 0) {
					// logger.info("Raid already sent: " + processedRaid.getId());
					// alreadyPosted = true;
					// }
				}
				if (sendOnlyUpdate) {
					return;
				}
			} else {
				logger.info("this event wasn't processed before");
			}
			ProcessedRaids processedRaid = new ProcessedRaids(gymId, end);
			logger.info("new level-" + event.getLevel() + " raid at gym: " + gymId + " , mon: " + event.getPokemonId()
					+ ", end " + telegramTextService.formatTimeFromSeconds(end));
			processedRaid = processedRaidRepository.save(processedRaid);

			for (Filter filter : filterService.getFiltersByType(FilterType.GROUP)) {
				try {
					CompletableFuture<SendMessageAnswer> raidFuture = sendOrUpdateRaidIfFiltersMatch(fullGym, level,
							filter, pokemonId, filter.getGroup().getChatId().toString(), eventsWithSubscribers);
					SendMessageAnswer answer = getFutureAnswer(raidFuture);

					if (answer != null && (answer.getLocationAnswer() != null
							|| answer.getStickerAnswer() != null
							|| answer.getMainMessageAnswer() != null)) {
						updateProcessedRaid(processedRaid, answer, filter.getGroup().getChatId());
					}
				} catch (NullPointerException ex) {
					logger.warn("error sending raid for filter " + filter.getId() + " for group " + filter.getGroup(),
							ex);
				}
			}

			for (User user : userService.getAllUsers()) {
				if (user.isShowRaidMessages()) {
					Filter userFilter = filterDAO.findById(user.getUserFilter().getId()).orElse(null);
					String chatId = user.getChatId() == null ? user.getTelegramId() : user.getChatId();
					CompletableFuture<SendMessageAnswer> raidFuture = sendOrUpdateRaidIfFiltersMatch(fullGym, level,
							userFilter, pokemonId, chatId, eventsWithSubscribers);
					SendMessageAnswer answer = getFutureAnswer(raidFuture);
					if (answer != null && (answer.getLocationAnswer() != null
							|| answer.getStickerAnswer() != null
							|| answer.getMainMessageAnswer() != null)) {
						updateProcessedRaid(processedRaid, answer, Long.valueOf(chatId));
					}
				}
			}
		} else {
			logger.info("no message send...(gymId or end of raid missing)");
		}
	}

	private CompletableFuture<SendMessageAnswer> sendOrUpdateRaidIfFiltersMatch(Gym gym, Long level, Filter filter,
			int pokemonId, String chatId, SortedSet<EventWithSubscribers> eventWithSubscribers) {
		List<Integer> raidPokemon = filter.getRaidPokemon();
		CompletableFuture<SendMessageAnswer> future = null;

		if ((filter.getRaidLevel() != null && filter.getRaidLevel() <= level)
				|| (raidPokemon != null && raidPokemon.contains(pokemonId)) || (filter.getAllExRaidsInArea() != null
						&& filter.getAllExRaidsInArea() && null != gym.getExraidEglible() && gym.getExraidEglible())) {
			Double latitude = gym.getLatitude();
			Double longitude = gym.getLongitude();
			boolean gymCoordsGiven = latitude != null && longitude != null;
			boolean geoGiven = filter.getRadius() != null && filter.getLatitude() != null
					&& filter.getLongitude() != null && filter.getRadiusRaids() != null;
			boolean pointInOneGeofence = gymCoordsGiven
					? filterService.isPointInOneOfManyGeofences(latitude, longitude, filter.getRaidGeofences())
					: false;
			if (pointInOneGeofence || (gymCoordsGiven && geoGiven && filterService.isDistanceNearby(latitude, longitude,
					filter.getLatitude(), filter.getLongitude(), filter.getRadiusRaids()))) {

				// magic number: pokemonId -1 means "egg"
				boolean raidMessage = !(pokemonId == -1);
				if (raidMessage) {
					logger.info("sendOrUpdate raid, filter " + filter.getId() + " match");
					future = startNewRaidMessageFuture(gym, chatId, eventWithSubscribers, null);
				} else {
					future = startNewEggMessageFuture(gym, level, chatId, eventWithSubscribers, null);
				}
			}
		}
		return future;
	}

	private CompletableFuture<SendMessageAnswer> startNewRaidMessageFuture(Gym fullGym, String chatId,
			SortedSet<EventWithSubscribers> eventWithSubscribers, Integer possibleMessageIdToUpdate) {
		return startNewMessageFuture(fullGym, null, chatId, eventWithSubscribers, possibleMessageIdToUpdate, null,
				"raid");
	}

	private ProcessedRaids updateProcessedRaid(ProcessedRaids processedRaid, SendMessageAnswer answer, Long chatId) {
		Set<SendMessages> sentMessages = processedRaid.getGroupsRaidIsPosted();

		if (sentMessages == null) {
			sentMessages = new HashSet<>();
		}
		SendMessages sentMessage = new SendMessages();

		sentMessage.setGroupChatId(chatId);
		// sentMessage.setGroupChatId(answer.getChatId());

		Integer mainMessageAnswer = answer.getMainMessageAnswer();
		if (mainMessageAnswer != null) {
			sentMessage.setMessageId(mainMessageAnswer);
		}
		Integer stickerAnswer = answer.getStickerAnswer();
		if (stickerAnswer != null) {
			sentMessage.setStickerId(stickerAnswer);
		}
		Integer locationAnswer = answer.getLocationAnswer();
		if (locationAnswer != null) {
			sentMessage.setLocationId(locationAnswer);
		}

		processedRaid.addToGroupsRaidIsPosted(sentMessage);
		processedRaid = processedRaidRepository.save(processedRaid);
		return processedRaid;

	}

	private ProcessedPokemon updateProcessedMonster(ProcessedPokemon processedPokemon, SendMessageAnswer answer,
			String chatId) {
		Set<SendMessages> chatsPokemonIsPosted = processedPokemon.getChatsPokemonIsPosted();

		if (chatsPokemonIsPosted == null) {
			chatsPokemonIsPosted = new HashSet<>();
		}
		SendMessages e = new SendMessages();
		e.setGroupChatId(Long.valueOf(chatId));
		if (answer != null) {
			logger.debug("now we have future while sending to group :) The main-message is "
					+ answer.getMainMessageAnswer());
			Integer mainMessageAnswer = answer.getMainMessageAnswer();
			if (mainMessageAnswer != null) {
				e.setMessageId(mainMessageAnswer);
			}
			Integer stickerAnswer = answer.getStickerAnswer();
			if (stickerAnswer != null) {
				e.setStickerId(stickerAnswer);
			}
			Integer locationAnswer = answer.getLocationAnswer();
			if (locationAnswer != null) {
				e.setLocationId(locationAnswer);
			}
		} else {
			logger.debug("got no real answer for monster encounter {} in chat {}", processedPokemon.getEncounterId(),
					chatId);
		}

		if (e.getMessageId() != null && e.getGroupChatId() != null) {
			processedPokemon.addToChatsPokemonIsPosted(e);
			processedPokemon = processedPokemonDAO.save(processedPokemon);
		}
		return processedPokemon;
	}

	private SendMessageAnswer getFutureAnswer(CompletableFuture<SendMessageAnswer> future) {
		if (future != null) {
			try {
				SendMessageAnswer answer = future.get();
				if (answer != null && answer.getMainMessageAnswer() != null) {
					Integer mainMessageAnswer = answer.getMainMessageAnswer();
					Iterable<UserGroup> allUserGroups = userGroupRepository.findAll();
					Map<Long, String> groups = new HashMap<>();
					allUserGroups.forEach(x -> groups.put(x.getChatId(), x.getGroupName().toString()));
					String chatId = groups.get(answer.getChatId());
					chatId = chatId != null ? chatId : "" + answer.getChatId();
					logger.info("wrote message with messageId {} in chat '{}'", mainMessageAnswer,
							chatId);
				} else {
					logger.info("no answer from future...");
				}
				return answer;
			} catch (ExecutionException e) {
				logger.error("error while triggering egg or raid message. ", e.getCause());
			} catch (InterruptedException e) {
				logger.warn(GOT_INTERRUPTED + " in getFutureAnswer");
				Thread.currentThread().interrupt();
			}
		} else {
			logger.debug("nothing to do, no future. returning null");
		}
		return null;
	}

	private CompletableFuture<SendMessageAnswer> startNewMessageFuture(Gym gym, Long level, String chatId,
			SortedSet<EventWithSubscribers> eventWithSubscribers, Integer possibleMessageIdToUpdate,
			PokemonWithSpawnpoint pokemon, String type) {
		CompletableFuture<SendMessageAnswer> future = CompletableFuture.supplyAsync(() -> {
			try {
				if ("egg".equals(type)) {
					return telegramSendMessagesService.sendStandardMessage(null, gym, eventWithSubscribers, chatId,
							possibleMessageIdToUpdate);
					// return telegramSendMessagesService.sendEggMessage(chatId, gym,
					// level.toString(),
					// eventWithSubscribers, possibleMessageIdToUpdate);
				} else if ("raid".equals(type)) {
					return telegramSendMessagesService.sendStandardMessage(null, gym, eventWithSubscribers, chatId,
							possibleMessageIdToUpdate);
					// return telegramSendMessagesService.sendRaidMessage(gym, chatId,
					// eventWithSubscribers,
					// possibleMessageIdToUpdate);
				} else if ("pokemon".equals(type)) {
					logger.debug("now start sending pokemon " + pokemon.getPokemonId());
					return telegramSendMessagesService.sendStandardMessage(pokemon, null, eventWithSubscribers, chatId,
							possibleMessageIdToUpdate);
					// return telegramSendMessagesService.sendMonMessage(pokemon, chatId,
					// possibleMessageIdToUpdate);
				}
			} catch (FileNotFoundException | DecoderException e) {
				logger.error(e.getMessage(), e);
			} catch (InterruptedException e) {
				logger.warn(GOT_INTERRUPTED, " with type " + type);
				Thread.currentThread().interrupt();
			} catch (TelegramApiException e) {
				TelegramApiRequestException e1 = (TelegramApiRequestException) e;
				logger.error(API_RESPONSE + e1.getApiResponse());
				if (null != e1.getParameters()) {
					logger.error("parameters: " + e1.getParameters().toString());
				}
				logger.error(e.getMessage(), e);
			}
			return null;
		});
		return future;
	}

	private CompletableFuture<SendMessageAnswer> startNewEggMessageFuture(Gym gym, Long level, String chatId,
			SortedSet<EventWithSubscribers> eventWithSubscribers, Integer possibleMessageIdToUpdate) {
		return startNewMessageFuture(gym, level, chatId, eventWithSubscribers, possibleMessageIdToUpdate, null, "egg");
	}

	/**
	 * Returns time in format 'hh:mm' with regards to local timezone
	 */
	@Override
	public String getLocaleTime(long timeInMillis) {
		// get default calendar instance (to get default timezone and daylight savings
		Calendar calendar = GregorianCalendar.getInstance();
		calendar.setTimeInMillis(timeInMillis);
		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		int minute = calendar.get(Calendar.MINUTE);
		// First set minute of result:
		String minuteWithTwoDigits = minute <= 9 ? "0" + minute : String.valueOf(minute);
		String result = hour + ":" + minuteWithTwoDigits;
		return result;
	}

}
