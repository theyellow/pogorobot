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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import org.telegram.telegrambots.meta.api.objects.Message;
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
import pogorobot.repositories.FilterRepository;
import pogorobot.repositories.ProcessedPokemonRepository;
import pogorobot.repositories.ProcessedRaidRepository;
import pogorobot.repositories.RaidAtGymEventRepository;
import pogorobot.repositories.UserGroupRepository;
import pogorobot.telegram.util.SendMessageAnswer;
import pogorobot.telegram.util.Type;

@Service("telegramService")
public class TelegramServiceImpl implements TelegramService {

	private static final String API_RESPONSE = "API-response: ";

	private static final String GOT_INTERRUPTED = "Got interrupted";

	Logger logger = LoggerFactory.getLogger(this.getClass().getInterfaces()[0]);

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
	@Transactional
	public synchronized void triggerPokemonMessages(PokemonWithSpawnpoint pokemon) {
		if (pokemon.getPokemonId() != null) // && pokemon.getVerified() deleted 'cause of RDRM
		{
			boolean deepScan = false;

			// TODO: remove pokemon messages and find a way to identify: (encounterId
			// possible?)

			List<ProcessedPokemon> processedPokemon = processedPokemonDAO.findByEncounterId(pokemon.getEncounterId());
			ProcessedPokemon processedMon = null;
			List<Long> updatedChats = new ArrayList<>();
			if (processedPokemon == null) {
				processedMon = processedPokemonDAO
						.save(new ProcessedPokemon(pokemon.getEncounterId(), pokemon.getDisappearTime()));

			} else {
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
				} else {
					logger.debug("but it's no detail-scan");
				}
			}

			if (processedMon == null) {
				processedMon = processedPokemonDAO
						.save(new ProcessedPokemon(pokemon.getEncounterId(), pokemon.getDisappearTime()));
			}

			// Process all users:
			for (User user : userService.getAllUsers()) {
				if (user.isShowPokemonMessages()) {
					String chatId = user.getChatId() == null ? user.getTelegramId() : user.getChatId();
					Filter userFilter = user.getUserFilter();
					if (!updatedChats.contains(Long.valueOf(chatId))) {
						CompletableFuture<SendMessageAnswer> monsterFuture = sendPokemonIfFilterMatch(pokemon, chatId,
								userFilter, deepScan, null);
						SendMessageAnswer answer = getFutureAnswer(monsterFuture);
						if (answer != null) {
							logger.debug("Now we have future while sending to person :) The main-messageId is "
									+ answer.getMainMessageAnswer().getMessageId());
							updateProcessedMonster(processedMon, answer);
						}
					} else {
						logger.debug("Message was already posted (and perhaps edited), no reposting necessary");
					}

				}
			}
			// Workaround to get info about deep-scanning param to group-filters
			final boolean onlyDeep = deepScan;

			// Process all groups
			final ProcessedPokemon processedMonFinal = processedMon;
			userGroupRepository.findAll().iterator().forEachRemaining(group -> {
				String chatId = String.valueOf(group.getChatId());
				Filter groupFilter = group.getGroupFilter();

				if (!updatedChats.contains(Long.valueOf(chatId))) {
					logger.debug("Chat {} will be tested with monster encounter {}", chatId, pokemon.getEncounterId());
					CompletableFuture<SendMessageAnswer> monsterFuture = sendPokemonIfFilterMatch(pokemon, chatId,
							groupFilter, onlyDeep, null);
					if (monsterFuture != null) {
						SendMessageAnswer answer = getFutureAnswer(monsterFuture);
						if (answer != null) {
							logger.debug("Now we have future while sending to group :) The main-message is "
									+ answer.getMainMessageAnswer());
							updateProcessedMonster(processedMonFinal, answer);
						} else {
							logger.debug("got no real answer for monster encounter {} in chat {}",
									pokemon.getEncounterId(), chatId);
						}
					} else {
						logger.debug("got no future for monster encounter {} in chat {}, filter didn't match?",
								pokemon.getEncounterId(), chatId);
					}
				} else {
					logger.debug("Message was already posted (and perhaps edited), no reposting necessary");
				}
			});

		} else {
			logger.debug("No mon-id found");
		}
	}

	private SendMessageAnswer updateMonsterMessage(PokemonWithSpawnpoint pokemon, SendMessages sendMessage,
			boolean deepScan) {

		logger.info(
				"trigger updateMonsterMessage-editmessage future from triggerMonsterMessages for processed monster ");
		CompletableFuture<SendMessageAnswer> future = startSendMonsterFuture(pokemon,
				sendMessage.getGroupChatId().toString(), sendMessage.getMessageId());
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
			Filter filter, boolean onlyDeepScan, Integer possibleMessageIdToUpdate) {
		Long id = filter.getId();
		logger.debug("begin filter analyze of {}", id);
		filter = filterDAO.findById(id).orElse(null);
		if (filter == null) {
			logger.warn("Could not find filter {}", id);
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
					boolean nearby = filterService.isDistanceNearby(monLatitude, monLongitude, latitude, longitude,
							radiusIV);
					if (nearby
							|| filterService.isPointInOneGeofenceOfFilterByType(monLatitude, monLongitude, filter,
									Type.IV)
							|| filterService.isPointInOneGeofenceOfFilterByType(monLatitude, monLongitude, filter,
									Type.POKEMON)) {

						logger.debug("start creating new future to send mon {} to {}", pokemon.getPokemonId(), chatId);
						monsterFuture = startSendMonsterFuture(pokemon, chatId, possibleMessageIdToUpdate);
						return monsterFuture;
					} else {
						logger.info("pokemon {} isn't nearby or in a chosen area for filter {}", pokemon.getPokemonId(),
								filter.getId());
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
		if (!onlyDeepScan && filter.getPokemons().contains(pokemon.getPokemonId().intValue())) {
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
			if (nearby || filterService.isPointInOneGeofenceOfFilterByType(monLatitude, monLongitude, filter,
					Type.POKEMON)) {

				logger.debug("pokemon {} will be send to {}", pokemon.getPokemonId(), chatId);
				monsterFuture = startSendMonsterFuture(pokemon, chatId, possibleMessageIdToUpdate);
				return monsterFuture;
			}
		} else {
			String msg = "no nearby- or area-search for filter " + filter.getId() + " because ";

			if (onlyDeepScan && filter.getPokemons().contains(pokemon.getPokemonId().intValue())) {
				msg += " a deep inspection (pokemon 'spawned 2nd time' with iv-details) of iv was happening";
				monsterFuture = startSendMonsterFuture(pokemon, chatId, possibleMessageIdToUpdate);
			} else {
				msg += " pokemon " + pokemon.getPokemonId() + " is not in list";
			}
			logger.debug(msg);
		}
		return monsterFuture;
	}

	private CompletableFuture<SendMessageAnswer> startSendMonsterFuture(PokemonWithSpawnpoint pokemon, String chatId,
			Integer possibleMessageIdToUpdate) {
		return startNewMessageFuture(null, null, chatId, null, possibleMessageIdToUpdate, pokemon, "pokemon");
	}

	@Override
	@Transactional(TxType.REQUIRES_NEW)
	public synchronized void triggerRaidMessages(RaidAtGymEvent event) {
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
			if (processedRaids != null) {
				boolean sendOnlyUpdate = false;
				logger.info("Got event at gym that has " + processedRaids.size() + " entries");
				for (ProcessedRaids processedRaid : processedRaids) {

					Set<SendMessages> groupsRaidIsPosted = null;
					// List<ProcessedRaids> processedGymIds =
					// processedRaidRepository.findByGymId(gym.getGymId());
					// if (!processedGymIds.isEmpty() && processedGymIds.size() == 1) {
					// ProcessedRaids processedRaid = processedGymIds.get(0);
					groupsRaidIsPosted = processedRaid.getGroupsRaidIsPosted();

					// Needed trigger to initialize resultSet:
					logger.info("There are " + groupsRaidIsPosted.size() + " chats where this raid is posted");

					for (SendMessages groupMessages : groupsRaidIsPosted) {
						sendOnlyUpdate = true;
						Long groupChatId = groupMessages.getGroupChatId();
						Integer messageId = groupMessages.getMessageId();

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
							updateProcessedRaid(processedRaid, answer);
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
				logger.info("This event wasn't processed before");
			}
			ProcessedRaids processedRaid = new ProcessedRaids(gymId, end);
			logger.info("New level-" + event.getLevel() + " raid at gym: " + gymId + " , mon: " + event.getPokemonId()
					+ ", end " + telegramTextService.formatTimeFromSeconds(end));
			processedRaid = processedRaidRepository.save(processedRaid);

			// 1st we look if it was already posted, so we need to update instead of
			// resend..

			//
			for (Filter filter : filterService.getFiltersByType(FilterType.GROUP)) {
				try {
					CompletableFuture<SendMessageAnswer> raidFuture = sendOrUpdateRaidIfFiltersMatch(fullGym, level,
							filter, pokemonId, filter.getGroup().getChatId().toString(), eventsWithSubscribers);
					SendMessageAnswer answer = getFutureAnswer(raidFuture);
					if (answer != null && (answer.getLocationAnswer() != null || answer.getStickerAnswer() != null
							|| answer.getMainMessageAnswer() != null)) {
						updateProcessedRaid(processedRaid, answer);
					}
				} catch (NullPointerException ex) {
					logger.warn("Error sending raid for filter " + filter.getId() + " for group " + filter.getGroup(),
							ex);
				}
			}

			for (User user : userService.getAllUsers()) {
				if (user.isShowRaidMessages()) {
					Filter userFilter = filterDAO.findById(user.getUserFilter().getId()).orElse(null);
					String chatId = user.getChatId() == null ? user.getTelegramId() : user.getChatId();
					CompletableFuture<SendMessageAnswer> raidFuture = sendOrUpdateRaidIfFiltersMatch(fullGym, level,
							userFilter, pokemonId, chatId, eventsWithSubscribers);
					getFutureAnswer(raidFuture);
				}
			}
		} else {
			logger.info("No message send...(gymId or end of raid missing)");
		}
	}

	private CompletableFuture<SendMessageAnswer> sendOrUpdateRaidIfFiltersMatch(Gym gym, Long level, Filter filter,
			int pokemonId, String chatId, SortedSet<EventWithSubscribers> eventWithSubscribers) {
		List<Integer> raidPokemon = filter.getRaidPokemon();
		CompletableFuture<SendMessageAnswer> future = null;

		// boolean sendOnlyUpdate = false;
		// Set<SendMessages> chatsPokemonIsPosted = null;
		// List<ProcessedRaids> processedGymIds =
		// processedRaidRepository.findByGymId(gym.getGymId());
		// if (!processedGymIds.isEmpty() && processedGymIds.size() == 1) {
		// ProcessedRaids processedRaid = processedGymIds.get(0);
		// chatsPokemonIsPosted = processedRaid.getGroupsRaidIsPosted();
		// sendOnlyUpdate = true;
		// for (SendMessages groupMessages : chatsPokemonIsPosted) {
		// Long groupChatId = groupMessages.getGroupChatId();
		// Integer messageId = groupMessages.getMessageId();
		//
		// // magic number: pokemonId -1 means "egg"
		// boolean raidMessage = !(pokemonId == -1);
		// if (raidMessage) {
		// future = startNewRaidMessageFuture(gym, chatId, eventWithSubscribers);
		// } else {
		// future = startNewEggMessageFuture(gym, level, chatId, eventWithSubscribers);
		// }
		// }
		// }

		if ((filter.getRaidLevel() != null && filter.getRaidLevel() <= level)
				|| (raidPokemon != null && raidPokemon.contains(pokemonId))) {
			Double latitude = gym.getLatitude();
			Double longitude = gym.getLongitude();
			boolean gymCoordsGiven = latitude != null && longitude != null;
			boolean geoGiven = filter.getRadius() != null && filter.getLatitude() != null
					&& filter.getLongitude() != null && filter.getRadiusRaids() != null;
			boolean pointInOneGeofence = gymCoordsGiven
					? filterService.isPointInOneGeofenceOfFilterByType(latitude, longitude, filter, Type.RAID)
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

	private ProcessedRaids updateProcessedRaid(ProcessedRaids processedRaid, SendMessageAnswer answer) {
		Set<SendMessages> groupsRaidIsPosted = processedRaid.getGroupsRaidIsPosted();

		if (groupsRaidIsPosted == null) {
			groupsRaidIsPosted = new HashSet<>();
		}
		SendMessages e = new SendMessages();
		Message mainMessageAnswer = answer.getMainMessageAnswer();
		if (mainMessageAnswer != null) {
			e.setGroupChatId(mainMessageAnswer.getChatId());
			e.setMessageId(mainMessageAnswer.getMessageId());
		}
		Message stickerAnswer = answer.getStickerAnswer();
		if (stickerAnswer != null) {
			e.setGroupChatId(stickerAnswer.getChatId());
			e.setStickerId(stickerAnswer.getMessageId());
		}
		Message locationAnswer = answer.getLocationAnswer();
		if (locationAnswer != null) {
			e.setGroupChatId(locationAnswer.getChatId());
			e.setLocationId(locationAnswer.getMessageId());
		}
		processedRaid.addToGroupsRaidIsPosted(e);
		processedRaid = processedRaidRepository.save(processedRaid);
		return processedRaid;

	}

	private ProcessedPokemon updateProcessedMonster(ProcessedPokemon processedPokemon, SendMessageAnswer answer) {
		Set<SendMessages> chatsPokemonIsPosted = processedPokemon.getChatsPokemonIsPosted();

		if (chatsPokemonIsPosted == null) {
			chatsPokemonIsPosted = new HashSet<>();
		}
		SendMessages e = new SendMessages();
		Message mainMessageAnswer = answer.getMainMessageAnswer();
		if (mainMessageAnswer != null) {
			e.setGroupChatId(mainMessageAnswer.getChatId());
			e.setMessageId(mainMessageAnswer.getMessageId());
		}
		Message stickerAnswer = answer.getStickerAnswer();
		if (stickerAnswer != null) {
			e.setGroupChatId(stickerAnswer.getChatId());
			e.setStickerId(stickerAnswer.getMessageId());
		}
		Message locationAnswer = answer.getLocationAnswer();
		if (locationAnswer != null) {
			e.setGroupChatId(locationAnswer.getChatId());
			e.setLocationId(locationAnswer.getMessageId());
		}
		processedPokemon.addToChatsPokemonIsPosted(e);
		processedPokemon = processedPokemonDAO.save(processedPokemon);
		return processedPokemon;
	}

	private SendMessageAnswer getFutureAnswer(CompletableFuture<SendMessageAnswer> future) {
		if (future != null) {
			try {
				SendMessageAnswer answer = future.get();
				if (answer != null && answer.getMainMessageAnswer() != null) {
					Iterable<UserGroup> allUserGroups = userGroupRepository.findAll();
					Map<Long, String> groups = new HashMap<>();
					allUserGroups.forEach(x -> groups.put(x.getChatId(), x.getGroupName().toString()));
					logger.info(this.getClass().getName() + " wrote message with messageId: "
							+ answer.getMainMessageAnswer().getMessageId() + " in chat '"
							+ groups.get(answer.getMainMessageAnswer().getChat().getId()) + "'");
				} else {
					logger.info("No answer from future...");
				}
				return answer;
			} catch (ExecutionException e) {
				logger.error("Error while triggering egg or raid message. ", e.getCause());
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
					return telegramSendMessagesService.sendEggMessage(chatId, gym, level.toString(),
							eventWithSubscribers, possibleMessageIdToUpdate);
				} else if ("raid".equals(type)) {
					return telegramSendMessagesService.sendRaidMessage(gym, chatId, eventWithSubscribers,
							possibleMessageIdToUpdate);
				} else if ("pokemon".equals(type)) {
					logger.debug("Now start sending pokemon " + pokemon.getPokemonId());
					return telegramSendMessagesService.sendMonMessage(pokemon, chatId, possibleMessageIdToUpdate);
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
		logger.info(calendar.toString());
		return result;
	}

}