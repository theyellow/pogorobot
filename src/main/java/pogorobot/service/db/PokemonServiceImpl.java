/**
 Copyright 2020 Benjamin Marstaller
 
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

package pogorobot.service.db;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.OptimisticLockException;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import pogorobot.entities.PokemonWithSpawnpoint;
import pogorobot.entities.PokemonWithSpawnpoint_;
import pogorobot.entities.ProcessedPokemon;
import pogorobot.entities.SendMessages;
import pogorobot.service.db.repositories.ProcessedPokemonRepository;

@Service("pokemonService")
public class PokemonServiceImpl implements PokemonService {

	private static Logger logger = LoggerFactory.getLogger(PokemonService.class);

	@PersistenceContext
	@SuppressWarnings("squid:S3749")
	private EntityManager entityManager;

	@Autowired
	private ProcessedPokemonRepository processedPokemonDAO;

	@Override
	public Iterable<PokemonWithSpawnpoint> getAllPokemon() {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		return entityManager.createQuery(cb.createQuery(PokemonWithSpawnpoint.class)).getResultList();
	}

	@Override
	public void deletePokemon(PokemonWithSpawnpoint pokemon) {
		entityManager.remove(pokemon);
	}

	@Override
	@Transactional
	public List<String> retrieveProcessedPokemonEncounterIds() {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<ProcessedPokemon> retrieveProcessedMons = cb.createQuery(ProcessedPokemon.class);
		retrieveProcessedMons.from(ProcessedPokemon.class);
		List<ProcessedPokemon> processedPokemon = entityManager.createQuery(retrieveProcessedMons).getResultList();
		List<String> processedMons = processedPokemon.stream().map(processed -> processed.getEncounterId())
				.collect(Collectors.toList());
		return processedMons;
	}

	@Override
	@Transactional
	public List<ProcessedPokemon> retrieveProcessedPokemon() {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<ProcessedPokemon> retrieveProcessedMons = cb.createQuery(ProcessedPokemon.class);
		retrieveProcessedMons.from(ProcessedPokemon.class);
		List<ProcessedPokemon> processedPokemon = entityManager.createQuery(retrieveProcessedMons).getResultList();
		return processedPokemon;
	}

	@Override
	@Transactional
	public List<String> retrievePokemonWithSpawnpointEncounterIds() {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<PokemonWithSpawnpoint> retrieveSpawnpointsWithMon = cb.createQuery(PokemonWithSpawnpoint.class);
		retrieveSpawnpointsWithMon.from(PokemonWithSpawnpoint.class);

		List<PokemonWithSpawnpoint> livePokemon = entityManager.createQuery(retrieveSpawnpointsWithMon).getResultList();
		List<String> encounterIds = livePokemon.stream().map(pokemon -> pokemon.getEncounterId())
				.collect(Collectors.toList());
		return encounterIds;
	}

//	/**
//	 * Not used and also does nothing
//	 */
//	@Override
//	@Deprecated
//	public void deleteProcessedPokemonOnDatabase() {
//		List<String> encounterIds = retrievePokemonWithSpawnpointEncounterIds();
//		List<String> processedMons = retrieveProcessedPokemonEncounterIds();
//
//		int numberOfSavedPokemon = 0;
//
//		for (String encounterId : encounterIds) {
//			if (processedMons.remove(encounterId)) {
//				numberOfSavedPokemon++;
//			}
//		}
//		List<ProcessedPokemon> processedPokemon = retrieveProcessedPokemon();
//
//		List<Set<SendMessages>> processedMessages = processedPokemon.stream()
//				.map(processed -> processed.getChatsPokemonIsPosted()).collect(Collectors.toList());
//
//		StopWatch stopWatch = StopWatch.createStarted();
//		logger.info("Cleaning up processed Pokemon - to delete: " + processedMessages.size() + ", to save: "
//				+ numberOfSavedPokemon);
//		int numberOfDeleted = 0;
//		for (Set<SendMessages> mon : processedMessages) {
//			// processedPokemonDAO.deleteById(mon);
//			// numberOfDeleted++;
//		}
//		// for (Long mon : processedMons) {
//		// processedPokemonDAO.deleteById(mon);
//		// numberOfDeleted++;
//		// }
//		stopWatch.stop();
//		long time = stopWatch.getTime(TimeUnit.SECONDS);
//		logger.info("Cleaning up processed Pokemon - deleted: " + numberOfDeleted + "\nused time: " + time + " secs");
//		logger.info("Just a fake, no processedPokemon deleted on database - saved " + numberOfDeleted);
//	}

	@Override
	@Transactional(TxType.REQUIRED)
	public void cleanPokemonWithSpawnpointOnDatabase() {
		logger.info("start cleaning up PokemonWithSpawnpoint ");
		StopWatch stopWatch = StopWatch.createStarted();
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaDelete<PokemonWithSpawnpoint> deleteCriteria = cb
				.createCriteriaDelete(PokemonWithSpawnpoint.class);
		Root<PokemonWithSpawnpoint> from = deleteCriteria.from(PokemonWithSpawnpoint.class);
		Predicate spawnpointIdEqual = cb.lessThan(from.get(PokemonWithSpawnpoint_.disappearTime), System.currentTimeMillis() / 1000 - 180);
		deleteCriteria = deleteCriteria.where(spawnpointIdEqual);
		int deadPokemon = entityManager.createQuery(deleteCriteria).executeUpdate();
		stopWatch.stop();
		long time = stopWatch.getTime(TimeUnit.SECONDS);
		logger.info("cleaning up PokemonWithSpawnpoint - deleted: " + deadPokemon + " | used time: " + time + " secs");
	}

	// public void deletePokemon(String mon) {
	// CriteriaBuilder cb = entityManager.getCriteriaBuilder();
	// CriteriaDelete<ProcessedPokemon> delete =
	// cb.createCriteriaDelete(ProcessedPokemon.class);
	// Root<ProcessedPokemon> root = delete.from(ProcessedPokemon.class);
	// delete = delete.where(cb.equal(root.get("gymId"), mon));
	// int numberOfDeleted =
	// entityManager.createQuery(delete).executeUpdate();
	// return numberOfDeleted;
	// }

	@Override
	@Transactional(dontRollbackOn = OptimisticLockException.class, value =  TxType.REQUIRES_NEW)
	public PokemonWithSpawnpoint updateOrInsertPokemon(PokemonWithSpawnpoint pokemon) {
		if ("None".equalsIgnoreCase(pokemon.getSpawnpointId())) {
			pokemon.setSpawnpointId(String.valueOf(System.nanoTime()));
//			return pokemon;
		}
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<PokemonWithSpawnpoint> realQuery = cb.createQuery(PokemonWithSpawnpoint.class);

		// Root for pokemon-with-spawnpoint query
		Root<PokemonWithSpawnpoint> from = realQuery.from(PokemonWithSpawnpoint.class);

		// Good old times always had exact spawnpoint_ids from nia
		Predicate spawnpointIdEqual = cb.equal(from.get("spawnpointId"), pokemon.getSpawnpointId());

		// Today we'll check lat/lon by hand...
		Predicate latLonEqual = cb.and(cb.equal(from.get("latitude"), pokemon.getLatitude()),
				cb.equal(from.get("longitude"), pokemon.getLongitude()));

		// build query
		realQuery = realQuery.where(cb.or(spawnpointIdEqual, latLonEqual));

		boolean changedPokemon = false;
		// retrieve result
		List<PokemonWithSpawnpoint> resultList = entityManager.createQuery(realQuery).getResultList();
		if (resultList.isEmpty()) {
			entityManager.persist(pokemon);
			changedPokemon = true;
		} else {
			PokemonWithSpawnpoint dbPokemon = resultList.get(0);

			// if ("None".equalsIgnoreCase(dbPokemon.getSpawnpointId())) {
			//
			// dbPokemon.setSpawnpointId(String.valueOf(System.nanoTime()));
			// entityManager.persist(pokemon);
			// }
		if (pokemon.getVerified() != null && !pokemon.getVerified().equals(dbPokemon.getVerified())) {
			dbPokemon.setVerified(pokemon.getVerified());
			changedPokemon = true;
		}
		if (pokemon.getPokemonId() != null && !pokemon.getPokemonId().equals(dbPokemon.getPokemonId())) {
			dbPokemon.setPokemonId(pokemon.getPokemonId());
			changedPokemon = true;
		}
		if (pokemon.getLatitude() != null && !pokemon.getLatitude().equals(dbPokemon.getLatitude()) ) {
			dbPokemon.setLatitude(pokemon.getLatitude());
			changedPokemon = true;
		}
		if (pokemon.getLongitude() != null && !pokemon.getLongitude().equals(dbPokemon.getLongitude())) {
			dbPokemon.setLongitude(pokemon.getLongitude());
			changedPokemon = true;
		}
		if (pokemon.getDisappearTime() != null && pokemon.getDisappearTime() != 0 && !pokemon.getDisappearTime().equals(dbPokemon.getDisappearTime())) {
			dbPokemon.setDisappearTime(pokemon.getDisappearTime());
			changedPokemon = true;
		}
		if (pokemon.getSpawnStart() != null && !pokemon.getSpawnStart().equals(dbPokemon.getSpawnStart())) {
			dbPokemon.setSpawnStart(pokemon.getSpawnStart());
			changedPokemon = true;
		}
		if (pokemon.getSpawnEnd() != null && !pokemon.getSpawnEnd().equals(dbPokemon.getSpawnEnd())) {
			dbPokemon.setSpawnEnd(pokemon.getSpawnEnd());
			changedPokemon = true;
		}
		if (pokemon.getCpMultiplier() != null && pokemon.getCpMultiplier() != 0 && !pokemon.getCpMultiplier().equals(dbPokemon.getCpMultiplier())) {
			dbPokemon.setCpMultiplier(pokemon.getCpMultiplier());
			changedPokemon = true;
		}
		if (pokemon.getForm() != null && !pokemon.getForm().equals(dbPokemon.getForm())) {
			dbPokemon.setForm(pokemon.getForm());
			changedPokemon = true;
		}
		if (pokemon.getGender() != null && !pokemon.getGender().equals(dbPokemon.getGender())) {
			dbPokemon.setGender(pokemon.getGender());
			changedPokemon = true;
		}
		if (pokemon.getHeight() != null && !pokemon.getHeight().equals(dbPokemon.getHeight())) {
			dbPokemon.setHeight(pokemon.getHeight());
			changedPokemon = true;
		}
		if (pokemon.getWeight() != null && !pokemon.getWeight().equals(dbPokemon.getWeight())) {
			dbPokemon.setWeight(pokemon.getWeight());
			changedPokemon = true;
		}
		if (pokemon.getIndividualAttack() != null && !pokemon.getIndividualAttack().equals(dbPokemon.getIndividualAttack())) {
			dbPokemon.setIndividualAttack(pokemon.getIndividualAttack());
			changedPokemon = true;
		}
		if (pokemon.getIndividualDefense() != null && !pokemon.getIndividualDefense().equals(dbPokemon.getIndividualDefense())) {
			dbPokemon.setIndividualDefense(pokemon.getIndividualDefense());
			changedPokemon = true;
		}
		if (pokemon.getIndividualStamina() != null && !pokemon.getIndividualStamina().equals(dbPokemon.getIndividualStamina())) {
			dbPokemon.setIndividualStamina(pokemon.getIndividualStamina());
			changedPokemon = true;
		}
		if (pokemon.getMove1() != null && !pokemon.getMove1().equals(dbPokemon.getMove1())) {
			dbPokemon.setMove1(pokemon.getMove1());
			changedPokemon = true;
		}
		if (pokemon.getMove2() != null && !pokemon.getMove2().equals(dbPokemon.getMove2())) {
			dbPokemon.setMove2(pokemon.getMove2());
			changedPokemon = true;
		}
		if (changedPokemon) {
			if (pokemon.getTimeUntilHidden_ms() != null && !pokemon.getTimeUntilHidden_ms().equals(dbPokemon.getTimeUntilHidden_ms())) {
				dbPokemon.setTimeUntilHidden_ms(pokemon.getTimeUntilHidden_ms());
			}
			if (pokemon.getLastModified() != null && !pokemon.getLastModified().equals(dbPokemon.getLastModified())) {
				dbPokemon.setLastModified(pokemon.getLastModified());
			}
			if (pokemon.getSpawnpointId() != null && !pokemon.getSpawnpointId().equals(dbPokemon.getSpawnpointId())) {
				dbPokemon.setSpawnpointId(pokemon.getSpawnpointId());
			}
			if (pokemon.getEncounterId() != null && !pokemon.getEncounterId().equals(dbPokemon.getEncounterId())) {
				dbPokemon.setEncounterId(pokemon.getEncounterId());
			}
			if (pokemon.getSecondsUntilDespawn() != null && !pokemon.getSecondsUntilDespawn().equals(dbPokemon.getSecondsUntilDespawn()) ) {
				dbPokemon.setSecondsUntilDespawn(pokemon.getSecondsUntilDespawn());
			}
			if (pokemon.getPlayerLevel() != null && !pokemon.getPlayerLevel().equals(dbPokemon.getPlayerLevel())) {
				dbPokemon.setPlayerLevel(pokemon.getPlayerLevel());
			}
			// if (pokemon.getPokemonEncounterId() != null) {
			// dbPokemon.setPokemonEncounterId(pokemon.getPokemonEncounterId());
			// }
			try {
				pokemon = entityManager.merge(dbPokemon);
				entityManager.flush();
			} catch (OptimisticLockException ex) {
				logger.error("updateOrInsert caused OptimisticLockException: {}", ex.getMessage());
				String methodName = "updateOrInsert";
				logStacktraceForMethod(ex.getStackTrace(), methodName);
				if (ex.getCause() != null) {
					logger.error("caused by {}: {}", ex.getCause().getClass().getSimpleName(), ex.getCause().getMessage());
				}
				logger.error("Entity with problems {}: {}", ex.getEntity().getClass().getSimpleName(), ex.getEntity());
			}
		}
		}
		return pokemon;
	}

	private void logStacktraceForMethod(StackTraceElement[] stackTrace, String methodName) {
		boolean lastLineMatched = false;
		for (StackTraceElement stackTraceElement : stackTrace) {
			if (stackTraceElement.getMethodName().contains(methodName)) {
				logger.error("Stacktrace: {}", stackTraceElement.getClassName());
				lastLineMatched = true;
			} else if (lastLineMatched) {
				logger.error("Stacktrace: {}", stackTraceElement.getClassName());
				lastLineMatched = false;
			}
		}
	}

}
