package pogorobot.service.db;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import pogorobot.entities.PokemonWithSpawnpoint;
import pogorobot.entities.ProcessedPokemon;
import pogorobot.entities.ProcessedPokemon_;
import pogorobot.entities.ProcessedRaids;
import pogorobot.entities.ProcessedRaids_;
import pogorobot.entities.RaidAtGymEvent;
import pogorobot.entities.SendMessages;
import pogorobot.entities.SendMessages_;
import pogorobot.service.db.repositories.ProcessedPokemonRepository;
import pogorobot.service.db.repositories.ProcessedRaidRepository;
import pogorobot.service.db.repositories.RaidAtGymEventRepository;
import pogorobot.service.db.repositories.SendMessagesRepository;
import pogorobot.telegram.util.SendMessageAnswer;

@Repository("processedElementsServiceRepository")
public class ProcessedElementsServiceRepositoryImpl implements ProcessedElementsServiceRepository {

	private static Logger logger = LoggerFactory.getLogger(ProcessedElementsServiceRepository.class);

	@Autowired
	private EventWithSubscribersService eventWithSubscribersService;

	@Autowired
	private SendMessagesRepository sendMessagesRepository;

	@Autowired
	private RaidAtGymEventRepository raidAtGymEventRepository;

	@Autowired
	private ProcessedRaidRepository processedRaidRepository;

	@Autowired
	private ProcessedPokemonRepository processedPokemonRepository;

	@PersistenceContext
	private EntityManager entityManager;


	@Override
	@Transactional
	public List<SendMessages> retrievePostedMessagesForGymId(String gymId) {
		List<SendMessages> result = new ArrayList<>();
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		CriteriaQuery<ProcessedRaids> queryProcessedRaids = criteriaBuilder.createQuery(ProcessedRaids.class);

		Root<ProcessedRaids> processedRaid = queryProcessedRaids.from(ProcessedRaids.class);
		queryProcessedRaids = queryProcessedRaids.where(criteriaBuilder.equal(processedRaid.get("gymId"), gymId));
		
		List<ProcessedRaids> processeRaidsForGym = entityManager.createQuery(queryProcessedRaids).getResultList();
		
		for (ProcessedRaids processedRaids : processeRaidsForGym) {
			Set<SendMessages> groupsRaidIsPosted = processedRaids.getGroupsRaidIsPosted();
			logger.debug("found " + groupsRaidIsPosted.size());
			for (SendMessages message : groupsRaidIsPosted) {
				if (message.getMessageId() != null && message.getGroupChatId() != null) {
					result.add(message);
				}
			}
		}
		
		return result;
	}

	/**
	 * Deletes on database ProcessedPokemon, ProcessedRaids, SendMessages and Raids
	 * and RaidEvents itself if endTime is before now.
	 * 
	 * @param nowInSeconds
	 */
	@Override
	@Transactional(TxType.REQUIRES_NEW)
	public void cleanupSendMessage(List<SendMessages> messagesWithTimeOver, long nowInSeconds) {

		// Delete sendMessages and ProcessedRaids or ProcessedPokemon
		cleanupSendMessagesAndProcessedElementsOnDatabase(nowInSeconds, messagesWithTimeOver);

		// Delete old not posted processed pokemon:
		deleteNonPostedMonsterOnDatabase();

		// Delete old raid-events if time is over:
		deleteOldRaidsOnDatabase(nowInSeconds);

		// Delete not posted processed raids:
		deleteNonPostedProcessedRaidsOnDatabase();
	}

	/**
	 * Deletes all non posted raids from ProcessedRaids.
	 * 
	 * @return number of deleted entities.
	 */
	@Override
	@Transactional
	public int deleteNonPostedProcessedRaidsOnDatabase() {
		// int size;
		// List<Long> raidsToDelete = new ArrayList<>();

		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		// Subquery<ProcessedRaids> subquery = select.subquery(ProcessedRaids.class);
		CriteriaQuery<ProcessedRaids> subquery = criteriaBuilder.createQuery(ProcessedRaids.class);
		// SetJoin<ProcessedRaids, SendMessages> join =
		// processedRaids.joinSet("groupsRaidIsPosted");
		Root<ProcessedRaids> subqueryRoot = subquery.from(ProcessedRaids.class);

		// subquery.select(subqueryRoot.get("id"));

		Expression<Boolean> sendMessages = criteriaBuilder
				.isEmpty(subqueryRoot.get(ProcessedRaids_.groupsRaidIsPosted));
		subquery = subquery.where(sendMessages);

		// processedRaids.in

		List<ProcessedRaids> emptySendMessages = entityManager.createQuery(subquery).getResultList();
		int deleted = emptySendMessages.size();
		emptySendMessages.forEach(raid -> {
			processedRaidRepository.deleteById(raid.getId());
		});

		// SetAttribute
		// SetJoin<ProcessedRaids, SendMessages> fetch =
		// Predicate notNull = join.isNotNull();
		// join = join.on(notNull);
		// Set<Fetch<SendMessages, ?>> fetches = join.getFetches();
		// logger.info("Fetches: " + fetches.size());
		// for (Fetch<SendMessages, ?> fetch : fetches) {
		// logger.info("Fetch " + fetch.toString());
		// }
		// CriteriaDelete<ProcessedRaids> delete =
		// criteriaBuilder.createCriteriaDelete(ProcessedRaids.class);
		//
		// Root<ProcessedRaids> from = delete.from(ProcessedRaids.class);
		// select = select.where(criteriaBuilder.in().value(sendMessages));
		// ));
		// entityManager.joinTransaction();
		// int deleted = entityManager.createQuery(delete).executeUpdate();
		// logger.info("deleted {} unsend raids from ProcessedRaids", deleted);
		// logger.info("Raidlist {}", resultList);
		// for (ProcessedRaids processedRaid : resultList) {
		// processedRaidRepository.deleteById(processedRaid.getId());
		// }
		// Path<Long> endTimeRaid = join.get("endTime");
		// fetch.fetch("id");
		// SetAttribute<ProcessedRaids, SendMessages> attribute = ProcessedRaids
		// Path<Set<SendMessages>> sendMessages =
		// processedRaids.get("groupsRaidIsPosted");
		// Path<ProcessedPokemon> monForEndTime = processedRaids.get("owningPokemon");
		// Path<Long> endTimeMon = monForEndTime.get("endTime");
		// querySendMessages = querySendMessages
		// .where(criter
		// querySendMessages.where(notNull).
		// processedRaidRepository.findAll().forEach(processedRaid -> {
		//
		// if (!processedRaid.isSomewherePosted()) {
		// raidsToDelete.add(processedRaid.getId());
		// }
		// });
		//
		// raidsToDelete.stream().forEach(id -> processedRaidRepository.deleteById(id));
		// size = raidsToDelete.size();
		// if (size > 0) {
		// logger.info("deleted {} processed raids ", size);
		// // logger.debug("deleted {} processed raids.", size);
		// }
		return deleted;
	}

	/**
	 * Deletes old RaidAtGymEvent entries that are after <i>nowInSeconds</i>.
	 * 
	 */
	@Override
	@Transactional
	public void deleteOldRaidsOnDatabase(long nowInSeconds) {
		Iterable<RaidAtGymEvent> allEvents = raidAtGymEventRepository.findAll();
		List<RaidAtGymEvent> deletedEvents = new ArrayList<>();
		StringBuilder gymIdBuilder = new StringBuilder();
		allEvents.forEach(raidEvent -> {
			if (raidEvent.getEnd() < nowInSeconds) {
				eventWithSubscribersService.deleteEvent(raidEvent.getGymId());
				deletedEvents.add(raidEvent);
				gymIdBuilder.append(raidEvent.getGymId() + " ");
			}
		});
		if (!deletedEvents.isEmpty()) {
			String deletedEventRaids = gymIdBuilder.toString();
			logger.debug("deleted {} event at following gyms: {}", deletedEvents.size(), deletedEventRaids);
		}
	}

	/**
	 * Deletes all ProcessedPokemon entries that are not posted.
	 * 
	 */
	@Transactional
	public void deleteNonPostedMonsterOnDatabase() {
		List<Long> monsterToDelete = new ArrayList<>();
		processedPokemonRepository.findAll().forEach(processedMonster -> {
			if (!processedMonster.isSomewherePosted()) {
				monsterToDelete.add(processedMonster.getId());
			}
		});
		monsterToDelete.stream().forEach(id -> processedPokemonRepository.deleteById(id));
		int size = monsterToDelete.size();
		if (size > 0) {
			logger.debug("deleted {} processed monsters.", size);
		}
	}

	@Override
	@Transactional
	public List<SendMessages> retrievePostedMonsterMessagesWithTimeOver(Long nowInSeconds) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<SendMessages> select = cb.createQuery(SendMessages.class);

		Root<SendMessages> from = select.from(SendMessages.class);
		Join<SendMessages, ProcessedPokemon> owningPokemon = from.join(SendMessages_.owningPokemon);
		Path<Long> endTimeOfMonster = owningPokemon.get(ProcessedPokemon_.endTime);

		select = select.where(cb.lessThan(endTimeOfMonster, nowInSeconds));

		TypedQuery<SendMessages> query = entityManager.createQuery(select);
		List<SendMessages> processedMonsterMessages = query.getResultList();

		int size = processedMonsterMessages.size();
		if (size > 0) {
			logger.warn("got {} processed monster for deletion", size);
		} else {
			logger.info("got {} processed monster for deletion", size);
		}
		return processedMonsterMessages;
	}

	@Override
	@Transactional
	public List<SendMessages> retrievePostedRaidMessagesWithTimeOver(Long nowInSeconds) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<SendMessages> select = cb.createQuery(SendMessages.class);

		Root<SendMessages> from = select.from(SendMessages.class);
		Join<SendMessages, ProcessedRaids> owningRaids = from.join(SendMessages_.owningRaid);

		Path<Long> endTimeOfRaid = owningRaids.get(ProcessedRaids_.endTime);

		TypedQuery<SendMessages> query = entityManager
				.createQuery(select.where(cb.lessThan(endTimeOfRaid, nowInSeconds)));
		List<SendMessages> processedRaidsMessages = query.getResultList();

		int size = processedRaidsMessages.size();
		if (size > 0) {
			logger.warn("got {} processed raids for deletion", size);
		} else {
			logger.info("got {} processed raids for deletion", size);
		}
		return processedRaidsMessages;
	}

	@Transactional
	public void deleteProcessedElement(SendMessages sendMessages) {
		ProcessedRaids owningRaid = sendMessages.getOwningRaid();
		ProcessedPokemon owningMon = sendMessages.getOwningPokemon();
		if (owningRaid != null) {
			owningRaid = processedRaidRepository.findById(owningRaid.getId()).orElse(null);
			if (owningRaid == null) {
				logger.warn("could not find owning raid in table");
				return;
			}
			owningRaid.removeFromGroupsRaidIsPosted(sendMessages);
			processedRaidRepository.save(owningRaid);
		} else if (owningMon != null) {
			owningMon = processedPokemonRepository.findById(owningMon.getId()).orElse(null);
			if (owningMon == null) {
				logger.warn("could not find owning monster in table");
				return;
			}
			owningMon.removeFromChatsPokemonIsPosted(sendMessages);
			processedPokemonRepository.save(owningMon);
		}
	}

	/**
	 * Cleans database entries of SendMessages and ProcessedPokemon, ProcessedRaids
	 * before <i>nowInSeconds</i>. Returns SendMessages to clean on Telegram.
	 * 
	 * @param nowInSeconds
	 *            before this moment all entries get deleted.
	 * @param all
	 * 
	 */
	@Transactional
	public void cleanupSendMessagesAndProcessedElementsOnDatabase(long nowInSeconds,
			List<SendMessages> all) {
		ProcessedRaids owningRaid;
		ProcessedPokemon owningMon;
		logger.info("retrieved {} messages", all.size());
		for (SendMessages sendMessages : all) {
			owningRaid = sendMessages.getOwningRaid();
			owningMon = sendMessages.getOwningPokemon();
			if (owningMon != null && owningRaid != null) {
				logger.error("there is no owning raid or monster for message {}", sendMessages.toString());
				continue;
			}
			deleteProcessedElement(sendMessages);
			sendMessagesRepository.delete(sendMessages);
		}
	}

	@Override
	@Transactional
	public ProcessedPokemon updateProcessedMonster(ProcessedPokemon processedPokemon, SendMessageAnswer answer,
			String chatId) {
		Set<SendMessages> chatsPokemonIsPosted = processedPokemon.getChatsPokemonIsPosted();

		if (chatsPokemonIsPosted == null) {
			chatsPokemonIsPosted = new HashSet<>();
		}
		SendMessages sentMessage = new SendMessages();
		sentMessage.setGroupChatId(Long.valueOf(chatId));
		if (answer != null) {
			logger.debug("now we have future while sending to group :) The main-message is "
					+ answer.getMainMessageAnswer());
			Integer mainMessageAnswer = answer.getMainMessageAnswer();
			if (mainMessageAnswer != null) {
				if (mainMessageAnswer == 2147483647) {
					logger.warn("In chat {} the answer was: 2147483647 (a special prime) - "
							+ "Don't save SendMessages and ProcessedPokemon", chatId);
				} else {
					sentMessage.setMessageId(mainMessageAnswer);
				}
			}
			Integer stickerAnswer = answer.getStickerAnswer();
			if (stickerAnswer != null) {
				sentMessage.setStickerId(stickerAnswer);
			}
			Integer locationAnswer = answer.getLocationAnswer();
			if (locationAnswer != null) {
				sentMessage.setLocationId(locationAnswer);
			}
		} else {
			logger.debug("got no real answer for monster encounter {} in chat {}", processedPokemon.getEncounterId(),
					chatId);
		}

		if (sentMessage.getMessageId() != null && sentMessage.getGroupChatId() != null) {
			processedPokemon.addToChatsPokemonIsPosted(sentMessage);
			processedPokemon = processedPokemonRepository.save(processedPokemon);
		}
		return processedPokemon;
	}

	@Override
	@Transactional
	public List<Set<SendMessages>> retrievePossibleMessageIdToUpdate(PokemonWithSpawnpoint pokemon, boolean deepScan,
			List<Long> updatedChats, List<SendMessages> sendMessageAnswers) {
		List<Set<SendMessages>> possibleMessageIdToUpdate = new ArrayList<>();
		List<ProcessedPokemon> processedPokemon = processedPokemonRepository
				.findByEncounterId(pokemon.getEncounterId());
		logger.debug(" processed pokemon for this encounter", processedPokemon.size());
		if (processedPokemon != null) {
			logger.debug("pokemon already encountered with encounterId " + pokemon.getEncounterId() + " , pokemon: "
					+ pokemon);
			if (pokemon.getWeight() != null && pokemon.getDisappearTime() != null) {
				deepScan = true;
				logger.debug("it's a detailed mon-scan, so check iv-filter again and update message...");

				if (!processedPokemon.isEmpty()) {
					processedPokemon.forEach(x -> {
						Set<SendMessages> chatsPokemonIsPosted = x.getChatsPokemonIsPosted();
						logger.debug(chatsPokemonIsPosted.size() + " chats pokemon is posted");
						chatsPokemonIsPosted.forEach(y -> {
							sendMessageAnswers.add(y);
							// SendMessageAnswer updateMonsterMessage = updateMonsterMessage(pokemon, y,
							// true);
							// if (updateMonsterMessage != null) {
							// updatedChats.add(y.getGroupChatId());
							// }
						});
						possibleMessageIdToUpdate.add(chatsPokemonIsPosted);

						// return chatsPokemonIsPosted;
					});

					// collect(Collectors.toList());
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
		return possibleMessageIdToUpdate;
		// return null;
	}

}
