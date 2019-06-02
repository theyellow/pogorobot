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

package pogorobot.service.db;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import pogorobot.entities.PokemonWithSpawnpoint;
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
	public void deleteProcessedPokemonOnDatabase() {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<PokemonWithSpawnpoint> retrieveSpawnpointsWithMon = cb.createQuery(PokemonWithSpawnpoint.class);
		retrieveSpawnpointsWithMon.from(PokemonWithSpawnpoint.class);

		List<PokemonWithSpawnpoint> livePokemon = entityManager.createQuery(retrieveSpawnpointsWithMon).getResultList();
		List<String> encounterIds = livePokemon.stream().map(pokemon -> pokemon.getEncounterId())
				.collect(Collectors.toList());
		CriteriaQuery<ProcessedPokemon> retrieveProcessedMons = cb.createQuery(ProcessedPokemon.class);
		retrieveProcessedMons.from(ProcessedPokemon.class);
		List<ProcessedPokemon> processedPokemon = entityManager.createQuery(retrieveProcessedMons).getResultList();
		List<String> processedMons = processedPokemon.stream().map(processed -> processed.getEncounterId())
				.collect(Collectors.toList());

		int numberOfSavedPokemon = 0;

		for (String encounterId : encounterIds) {
			if (processedMons.remove(encounterId)) {
				numberOfSavedPokemon++;
			}
		}
		List<Set<SendMessages>> processedMessages = processedPokemon.stream()
				.map(processed -> processed.getChatsPokemonIsPosted()).collect(Collectors.toList());

		StopWatch stopWatch = StopWatch.createStarted();
		logger.info("Cleaning up processed Pokemon - to delete: " + processedMessages.size() + ", to save: "
				+ numberOfSavedPokemon);
		int numberOfDeleted = 0;
		for (Set<SendMessages> mon : processedMessages) {
			// processedPokemonDAO.deleteById(mon);
			// numberOfDeleted++;
		}
		// for (Long mon : processedMons) {
		// processedPokemonDAO.deleteById(mon);
		// numberOfDeleted++;
		// }
		stopWatch.stop();
		long time = stopWatch.getTime(TimeUnit.SECONDS);
		logger.info("Cleaning up processed Pokemon - deleted: " + numberOfDeleted + "\nused time: " + time + " secs");
		logger.info("Just a fake, no processedPokemon deleted on database - saved " + numberOfDeleted);
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
	@Transactional(TxType.REQUIRES_NEW)
	public PokemonWithSpawnpoint updateOrInsertPokemon(PokemonWithSpawnpoint pokemon) {
		if ("None".equalsIgnoreCase(pokemon.getSpawnpointId())) {
			pokemon.setSpawnpointId(String.valueOf(System.nanoTime()));
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

		// retrieve result
		List<PokemonWithSpawnpoint> resultList = entityManager.createQuery(realQuery).getResultList();
		if (resultList.isEmpty()) {
			entityManager.persist(pokemon);
		} else {
			PokemonWithSpawnpoint dbPokemon = resultList.get(0);

			// if ("None".equalsIgnoreCase(dbPokemon.getSpawnpointId())) {
			//
			// dbPokemon.setSpawnpointId(String.valueOf(System.nanoTime()));
			// entityManager.persist(pokemon);
			// }
		if (pokemon.getVerified() != null) {
			dbPokemon.setVerified(pokemon.getVerified());
		}
		if (pokemon.getSpawnpointId() != null) {
			dbPokemon.setSpawnpointId(pokemon.getSpawnpointId());
		}
		if (pokemon.getPokemonId() != null) {
			dbPokemon.setPokemonId(pokemon.getPokemonId());
		}
		if (pokemon.getEncounterId() != null) {
			dbPokemon.setEncounterId(pokemon.getEncounterId());
		}
		if (pokemon.getLatitude() != null) {
			dbPokemon.setLatitude(pokemon.getLatitude());
		}
		if (pokemon.getLongitude() != null) {
			dbPokemon.setLongitude(pokemon.getLongitude());
		}
		if (pokemon.getDisappearTime() != null && pokemon.getDisappearTime() != 0) {
			dbPokemon.setDisappearTime(pokemon.getDisappearTime());
		}
		if (pokemon.getSecondsUntilDespawn() != null) {
			dbPokemon.setSecondsUntilDespawn(pokemon.getSecondsUntilDespawn());
		}
		if (pokemon.getSpawnStart() != null) {
			dbPokemon.setSpawnStart(pokemon.getSpawnStart());
		}
		if (pokemon.getSpawnEnd() != null) {
			dbPokemon.setSpawnEnd(pokemon.getSpawnEnd());
		}
		if (pokemon.getTimeUntilHidden_ms() != null) {
			dbPokemon.setTimeUntilHidden_ms(pokemon.getTimeUntilHidden_ms());
		}
		if (pokemon.getLastModified() != null) {
			dbPokemon.setLastModified(pokemon.getLastModified());
		}
		if (pokemon.getCpMultiplier() != null && pokemon.getCpMultiplier() != 0) {
			dbPokemon.setCpMultiplier(pokemon.getCpMultiplier());
		}
		if (pokemon.getForm() != null) {
			dbPokemon.setForm(pokemon.getForm());
		}
		if (pokemon.getGender() != null) {
			dbPokemon.setGender(pokemon.getGender());
		}
		if (pokemon.getHeight() != null) {
			dbPokemon.setHeight(pokemon.getHeight());
		}
		if (pokemon.getWeight() != null) {
			dbPokemon.setWeight(pokemon.getWeight());
		}
		if (pokemon.getIndividualAttack() != null) {
			dbPokemon.setIndividualAttack(pokemon.getIndividualAttack());
		}
		if (pokemon.getIndividualDefense() != null) {
			dbPokemon.setIndividualDefense(pokemon.getIndividualDefense());
		}
		if (pokemon.getIndividualStamina() != null) {
			dbPokemon.setIndividualStamina(pokemon.getIndividualStamina());
		}
		if (pokemon.getMove1() != null) {
			dbPokemon.setMove1(pokemon.getMove1());
		}
		if (pokemon.getMove2() != null) {
			dbPokemon.setMove2(pokemon.getMove2());
		}
		if (pokemon.getPlayerLevel() != null) {
			dbPokemon.setPlayerLevel(pokemon.getPlayerLevel());
		}
		// if (pokemon.getPokemonEncounterId() != null) {
		// dbPokemon.setPokemonEncounterId(pokemon.getPokemonEncounterId());
		// }
		pokemon = entityManager.merge(dbPokemon);
		}
		entityManager.flush();
		return pokemon;
	}

}
