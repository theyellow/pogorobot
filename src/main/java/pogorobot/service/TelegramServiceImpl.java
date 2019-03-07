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
import pogorobot.entities.GroupMessages;
import pogorobot.entities.Gym;
import pogorobot.entities.PokemonWithSpawnpoint;
import pogorobot.entities.ProcessedPokemon;
import pogorobot.entities.ProcessedRaids;
import pogorobot.entities.RaidAtGymEvent;
import pogorobot.entities.User;
import pogorobot.entities.UserGroup;
import pogorobot.repositories.FilterRepository;
import pogorobot.repositories.ProcessedPokemonRepository;
import pogorobot.repositories.ProcessedRaidRepository;
import pogorobot.repositories.RaidAtGymEventRepository;
import pogorobot.repositories.UserGroupRepository;
import pogorobot.telegram.util.SendRaidAnswer;
import pogorobot.telegram.util.Type;

@Service("telegramService")
public class TelegramServiceImpl implements TelegramService {

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
	private FilterRepository filterDAO;

	@Autowired
	private UserGroupRepository userGroupRepository;

	@Override
	@Transactional
	public synchronized void triggerPokemonMessages(PokemonWithSpawnpoint pokemon) {
		if (pokemon.getPokemonId() != null) // && pokemon.getVerified() deleted 'cause of RDRM
		{
			boolean deepScan = false;
			ProcessedPokemon processedPokemon = processedPokemonDAO.findById(pokemon.getEncounterId()).orElse(null);
			if (processedPokemon == null) {
				processedPokemonDAO.save(new ProcessedPokemon(pokemon.getEncounterId()));
			} else {
				logger.debug("pokemon already encountered with encounterId " + pokemon.getEncounterId() + " , pokemon: "
						+ pokemon);
				if (pokemon.getWeight() != null && pokemon.getDisappearTime() != null) {
					deepScan = true;
					logger.debug("it's a detailed mon-scan, so check iv-filter again...");
				} else {
					logger.debug("but it's no detail-scan");
				}

			}
			// Process all users:
			for (User user : userService.getAllUsers()) {
				if (user.isShowPokemonMessages()) {
					String chatId = user.getChatId() == null ? user.getTelegramId() : user.getChatId();
					Filter userFilter = user.getUserFilter();
					CompletableFuture<SendRaidAnswer> monsterFuture = sendPokemonIfFilterMatch(pokemon, chatId,
							userFilter, deepScan);
					SendRaidAnswer answer = getFutureAnswer(monsterFuture);
					if (answer != null) {
						logger.debug("Now we have future while sending to person :) The main-messageId is "
								+ answer.getMainMessageAnswer().getMessageId());
					}
				}
			}
			// Workaround to get info about deep-scanning param to group-filters
			final boolean onlyDeep = deepScan;

			// Process all groups
			userGroupRepository.findAll().iterator().forEachRemaining(group -> {
				String chatId = String.valueOf(group.getChatId());
				Filter groupFilter = group.getGroupFilter();
				CompletableFuture<SendRaidAnswer> monsterFuture = sendPokemonIfFilterMatch(pokemon, chatId,
						groupFilter, onlyDeep);
				if (monsterFuture != null) {
					SendRaidAnswer answer = getFutureAnswer(monsterFuture);
					if (answer != null) {
						logger.debug("Now we have future while sending to group :) The main-messageId is "
								+ answer.getMainMessageAnswer().getMessageId());
					}
				}
			});

		} else {
			logger.debug("No mon-id found");
		}
	}

	private CompletableFuture<SendRaidAnswer> sendPokemonIfFilterMatch(PokemonWithSpawnpoint pokemon, String chatId,
			Filter filter, boolean onlyDeepScan) {
		Long id = filter.getId();
		logger.debug("begin filter analyze for filter " + id);
		filter = filterDAO.findById(id).orElse(null);
		CompletableFuture<SendRaidAnswer> monsterFuture = null;
		boolean withIv = pokemon.getIndividualAttack() != null && !pokemon.getIndividualAttack().isEmpty();
		Double radiusPokemon = filter.getRadiusPokemon();
		if (withIv) {
			// logger.debug("Begin calculating iv");
			Double minIV = filter.getMinIV();
			Double maxIV = filter.getMaxIV();
			if (minIV != null) {
				logger.debug("begin analyze IV for filter " + filter.getId());
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

						logger.debug(
								"start creating new future to send mon " + pokemon.getPokemonId() + " to " + chatId);
						monsterFuture = startSendMonsterFuture(pokemon, chatId);
						return monsterFuture;
					} else {
						logger.info("pokemon " + pokemon.getPokemonId()
								+ " isn't nearby or in a chosen area for filter "
								+ filter.getId());
					}
				} else {
					logger.debug("iv didn't match for pokemon " + pokemon.getPokemonId() + " and filter "
							+ filter.getId() + " : min iv is " + minIV + " , calculated iv " + calculatedIVs);
				}
			} else {
				logger.debug("no min iv given in filter " + filter.getId());
			}
		} else {
			logger.debug("no iv scanning for filter " + filter.getId() + " because no iv given for pokemon "
					+ pokemon.getPokemonId() + " at spawnpoint " + pokemon.getSpawnpointId());
		}
		if (!onlyDeepScan && filter.getPokemons().contains(pokemon.getPokemonId().intValue())) {
			logger.debug("begin of pokemon-search by area/nearby");
			if (filter.getOnlyWithIV() != null && filter.getOnlyWithIV()) {
				logger.debug("only-iv filtering stops sending message to " + chatId);
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

				logger.debug("pokemon " + pokemon.getPokemonId() + " will be send to " + chatId);
				monsterFuture = startSendMonsterFuture(pokemon, chatId);
				return monsterFuture;
			}
		} else {
			String msg = "no nearby- or area-search for filter " + filter.getId() + " because ";

			if (onlyDeepScan) {
				msg += " a deep inspection (pokemon 'spawned 2nd time' with iv-details) of iv was happening";
			} else {
				msg += " pokemon " + pokemon.getPokemonId() + " is not in list";
			}
			logger.debug(msg);
		}
		return monsterFuture;
	}

	private CompletableFuture<SendRaidAnswer> startSendMonsterFuture(PokemonWithSpawnpoint pokemon, String chatId) {
		CompletableFuture<SendRaidAnswer> future = CompletableFuture.supplyAsync(() -> {
			try {
				logger.debug("Now start sending pokemon " + pokemon.getPokemonId());
				return telegramSendMessagesService.sendMonMessage(pokemon, chatId);
			} catch (FileNotFoundException | TelegramApiException | InterruptedException | DecoderException e) {
				if (e instanceof TelegramApiRequestException) {
					TelegramApiRequestException e1 = (TelegramApiRequestException) e;
					logger.error("API-response: " + e1.getApiResponse());
					if (null != e1.getParameters()) {
						logger.error("parameters: " + e1.getParameters().toString());
					}
					logger.error(e.getMessage(), e);
				} else {
					logger.error(e.getMessage(), e);
				}
			}
			return null;
		});
		return future;
	}

	@Override
	@Transactional(TxType.REQUIRES_NEW)
	public synchronized void triggerRaidMessages(RaidAtGymEvent event) {
		String gymId = event.getGymId();
		Gym gym = gymService.getGym(gymId);
		Long end = event.getEnd();
		String quickMove = gym.getRaid().getMove1();
		quickMove = quickMove != null ? quickMove : "Unbekannt";
		String chargeMove = gym.getRaid().getMove2();
		chargeMove = chargeMove != null ? chargeMove : "Unbekannt";
		if (gymId != null && end != null) {
			Gym fullGym = gymService.getGym(gymId);
			Long level = event.getLevel();
			SortedSet<EventWithSubscribers> eventsWithSubscribers = event.getEventsWithSubscribers();

			Long pokemonIdLong = event.getPokemonId();
			int pokemonId = pokemonIdLong == null ? -1 : pokemonIdLong.intValue();
			List<ProcessedRaids> processedRaids = processedRaidRepository.findByGymId(gymId);
			boolean alreadyPosted = false;
			if (processedRaids != null) {
				boolean sendOnlyUpdate = false;

				for (ProcessedRaids processedRaid : processedRaids) {

					Set<GroupMessages> groupsRaidIsPosted = null;
					// List<ProcessedRaids> processedGymIds =
					// processedRaidRepository.findByGymId(gym.getGymId());
					// if (!processedGymIds.isEmpty() && processedGymIds.size() == 1) {
					// ProcessedRaids processedRaid = processedGymIds.get(0);
					groupsRaidIsPosted = processedRaid.getGroupsRaidIsPosted();
					for (GroupMessages groupMessages : groupsRaidIsPosted) {
						sendOnlyUpdate = true;
						Long groupChatId = groupMessages.getGroupChatId();
						Integer messageId = groupMessages.getMessageId();

						// magic number: pokemonId -1 means "egg"
						boolean raidMessage = !(pokemonId == -1);
						CompletableFuture<SendRaidAnswer> future = null;
						if (raidMessage) {
							future = startNewRaidMessageFuture(gym, groupChatId.toString(), eventsWithSubscribers,
									messageId);
						} else {
							future = startNewEggMessageFuture(gym, gym.getRaid().getRaidLevel(), groupChatId.toString(),
									eventsWithSubscribers,
									messageId);
						}
						SendRaidAnswer answer = getFutureAnswer(future);
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
					CompletableFuture<SendRaidAnswer> raidFuture = sendOrUpdateRaidIfFiltersMatch(fullGym, level,
							filter, pokemonId, filter.getGroup().getChatId().toString(), eventsWithSubscribers);
					SendRaidAnswer answer = getFutureAnswer(raidFuture);
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
					CompletableFuture<SendRaidAnswer> raidFuture = sendOrUpdateRaidIfFiltersMatch(fullGym, level,
							userFilter, pokemonId, chatId, eventsWithSubscribers);
					getFutureAnswer(raidFuture);
				}
			}
		} else {
			logger.info("No message send...(gymId or end of raid missing)");
		}
	}

	private CompletableFuture<SendRaidAnswer> sendOrUpdateRaidIfFiltersMatch(Gym gym, Long level, Filter filter,
			int pokemonId, String chatId, SortedSet<EventWithSubscribers> eventWithSubscribers) {
		List<Integer> raidPokemon = filter.getRaidPokemon();
		CompletableFuture<SendRaidAnswer> future = null;

		// boolean sendOnlyUpdate = false;
		// Set<GroupMessages> groupsRaidIsPosted = null;
		// List<ProcessedRaids> processedGymIds =
		// processedRaidRepository.findByGymId(gym.getGymId());
		// if (!processedGymIds.isEmpty() && processedGymIds.size() == 1) {
		// ProcessedRaids processedRaid = processedGymIds.get(0);
		// groupsRaidIsPosted = processedRaid.getGroupsRaidIsPosted();
		// sendOnlyUpdate = true;
		// for (GroupMessages groupMessages : groupsRaidIsPosted) {
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
					future = startNewRaidMessageFuture(gym, chatId, eventWithSubscribers, null);
				} else {
					future = startNewEggMessageFuture(gym, level, chatId, eventWithSubscribers, null);
				}
			}
		}
		return future;
	}

	private CompletableFuture<SendRaidAnswer> startNewRaidMessageFuture(Gym fullGym, String chatId,
			SortedSet<EventWithSubscribers> eventWithSubscribers, Integer possibleMessageIdToUpdate) {
		CompletableFuture<SendRaidAnswer> future = CompletableFuture.supplyAsync(() -> {
			try {
				return telegramSendMessagesService.sendRaidMessage(fullGym, chatId, eventWithSubscribers,
						possibleMessageIdToUpdate);
			} catch (FileNotFoundException | TelegramApiException | InterruptedException | DecoderException e) {
				if (e instanceof TelegramApiRequestException) {
					TelegramApiRequestException e1 = (TelegramApiRequestException) e;
					logger.error("API-response: " + e1.getApiResponse());
					if (null != e1.getParameters()) {
						logger.error("Telegram parameters: " + e1.getParameters().toString());
					}
				}
				logger.error(e.getMessage(), e);
				return null;
			}
		});
		return future;
	}

	private ProcessedRaids updateProcessedRaid(ProcessedRaids processedRaid, SendRaidAnswer answer) {
		Set<GroupMessages> groupsRaidIsPosted = processedRaid.getGroupsRaidIsPosted();

		if (groupsRaidIsPosted == null) {
			groupsRaidIsPosted = new HashSet<>();
		}
		GroupMessages e = new GroupMessages();
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

	private SendRaidAnswer getFutureAnswer(CompletableFuture<SendRaidAnswer> future) {
		if (future != null) {
			try {
				SendRaidAnswer answer = future.get();
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
			} catch (InterruptedException | ExecutionException e) {
				logger.error("Error while triggering egg or raid message. ", e.getCause());
			}
		} else {
			logger.debug("nothing to do, no future. returning null");
		}
		return null;
	}

	private CompletableFuture<SendRaidAnswer> startNewEggMessageFuture(Gym gym, Long level, String chatId,
			SortedSet<EventWithSubscribers> eventWithSubscribers, Integer possibleMessageIdToUpdate) {
		CompletableFuture<SendRaidAnswer> future = CompletableFuture.supplyAsync(() -> {
			try {
				return telegramSendMessagesService.sendEggMessage(chatId, gym, level.toString(), eventWithSubscribers,
						possibleMessageIdToUpdate);
			} catch (FileNotFoundException | TelegramApiException | InterruptedException | DecoderException e) {
				if (e instanceof TelegramApiRequestException) {
					TelegramApiRequestException e1 = (TelegramApiRequestException) e;
					logger.error("API-response: " + e1.getApiResponse());
					if (null != e1.getParameters()) {
						logger.error("parameters: " + e1.getParameters().toString());
					}
				}
				logger.error(e.getMessage(), e);
				return null;
			}
		});
		return future;
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