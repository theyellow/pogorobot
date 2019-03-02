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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.google.maps.GeoApiContext;
import com.google.maps.GeoApiContext.Builder;
import com.google.maps.GeocodingApiRequest;
import com.google.maps.errors.ApiException;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;

import pogorobot.entities.Gym;
import pogorobot.entities.GymPokemon;
import pogorobot.entities.Raid;
import pogorobot.entities.RaidAtGymEvent;
import pogorobot.entities.RaidWithGym;
import pogorobot.repositories.GymPokemonRepository;
import pogorobot.repositories.GymRepository;
import pogorobot.telegram.config.StandardConfiguration;

@Service("gymService")
public class GymServiceImpl implements GymService {

	Logger logger = LoggerFactory.getLogger(this.getClass().getInterfaces()[0]);

	@PersistenceContext
	private EntityManager entityManager;
	
	@Autowired
	@Qualifier("standard")
	private StandardConfiguration standardConfiguration;

	@Autowired
	private GymPokemonRepository gymPokemonDao;

	@Autowired
	private GymRepository gymRepository;

	@Override
	public Iterable<Gym> getAllGym() {
		return gymRepository.findAll();
	}

	@Override
	public void deleteGym(Gym gym) {
		entityManager.remove(gym);
		entityManager.flush();
	}

	@Override
	@Transactional
	public Gym updateOrInsertGym(Gym gym) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Gym> query = cb.createQuery(Gym.class);
		query = query.where(cb.equal(query.from(Gym.class).get("gymId"), gym.getGymId()));
		List<Gym> resultList = entityManager.createQuery(query).getResultList();
		
		if (resultList.isEmpty()) {
			entityManager.persist(gym);
		} else {
			Gym oldGym = resultList.get(0);
			if (gym.getEnabled() != null) {
				oldGym.setEnabled(gym.getEnabled());
			}
			if (gym.getDescription() != null) {
				oldGym.setDescription(gym.getDescription());
			}
			if (gym.getLastModified() != null) {
				oldGym.setLastModified(gym.getLastModified());
			}
			if (gym.getLatitude() != null) {
				oldGym.setLatitude(gym.getLatitude());
			}
			if (gym.getLongitude() != null) {
				oldGym.setLongitude(gym.getLongitude());
			}
			if (gym.getName() != null) {
				oldGym.setName(gym.getName());
			}
			if (gym.getOccupiedSince() != null) {
				oldGym.setOccupiedSince(gym.getOccupiedSince());
			}
			if (gym.getPokemon() != null && !gym.getPokemon().isEmpty()) {
				List<GymPokemon> temp = new ArrayList<>();
				for (GymPokemon singlePokemon : gym.getPokemon()) {
					entityManager.persist(singlePokemon);
					temp.add(singlePokemon);
				}
				oldGym.setPokemon(temp);
			}
			if (gym.getRaidActiveUntil() != null) {
				oldGym.setRaidActiveUntil(gym.getRaidActiveUntil());
			}
			if (gym.getSlotsAvailable() != null) {
				oldGym.setSlotsAvailable(gym.getSlotsAvailable());
			}
			if (gym.getTeamId() != null) {
				oldGym.setTeamId(gym.getTeamId());
			}
			if (gym.getUrl() != null) {
				oldGym.setUrl(gym.getUrl());
			}
			if (gym.getRaid() != null) {
				oldGym.setRaid(gym.getRaid());
			}
			// if (gym.getEgg() != null) {
			// oldGym.setEgg(gym.getEgg());
			// }
			if (oldGym.getAddress() == null || oldGym.getAddress().isEmpty()) {
				// Reenable this (googlemaps-api update never done...)
				// oldGym.setAddress(createAddressFromGeo(oldGym.getLatitude(),
				// oldGym.getLongitude()));
			}
			gym = entityManager.merge(oldGym);
		}
		entityManager.flush();
		return gym;
	}

	@Override
	@Transactional(TxType.REQUIRED)
	public RaidAtGymEvent updateOrInsertRaidWithGymEvent(RaidAtGymEvent raidEvent) {
		if (null == raidEvent || null == raidEvent.getGymId() || null == raidEvent.getEnd()) {
			return null;
		}
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<RaidAtGymEvent> query = cb.createQuery(RaidAtGymEvent.class);
		Root<RaidAtGymEvent> from = query.from(RaidAtGymEvent.class);
		Path<Object> pathToGymId = from.get("gymId");
		Path<Object> pathToId = from.get("id");
		query = query
				.where(cb.or(cb.equal(pathToGymId, raidEvent.getGymId()), cb.equal(pathToId, raidEvent.getGymId())));
		List<RaidAtGymEvent> resultList = entityManager.createQuery(query).getResultList();

		if (resultList.isEmpty()) {
			String id = raidEvent.getId() != null ? raidEvent.getId() : raidEvent.getGymId();
			raidEvent.setId(id);
			raidEvent.getEventsWithSubscribers().stream().forEach(x -> entityManager.persist(x));
			entityManager.persist(raidEvent);
		} else {
			RaidAtGymEvent oldEvent = resultList.get(0);
			if (raidEvent.getEventsWithSubscribers() != null && raidEvent.getEventsWithSubscribers().size() > 0) {
				logger.warn("Events get overwritten! with {}", raidEvent.getEventsWithSubscribers());
				oldEvent.setEventsWithSubscribers(raidEvent.getEventsWithSubscribers());
			}
			if (raidEvent.getEnd() != null) {
				oldEvent.setEnd(raidEvent.getEnd());
			}
			if (raidEvent.getGymId() != null) {
				oldEvent.setGymId(raidEvent.getGymId());
			}
			if (raidEvent.getStart() != null) {
				oldEvent.setStart(raidEvent.getStart());
			}
			if (raidEvent.getPokemonId() != null) {
				oldEvent.setPokemonId(raidEvent.getPokemonId());
			}
			if (oldEvent.getId() == null) {
				String id = raidEvent.getId() != null ? raidEvent.getId() : raidEvent.getGymId();
				oldEvent.setId(id);
			}
			oldEvent.getEventsWithSubscribers().stream().forEach(x -> entityManager.persist(x));
			raidEvent = entityManager.merge(oldEvent);
		}
		entityManager.flush();
		return raidEvent;
	}

	// TODO: Find a replacement for googlemaps-api-key
	private String createAddressFromGeo(Double latitude, Double longitude) {
		// TODO: rework for getting suburb-names
		String result = "";
		GeoApiContext context = new Builder().apiKey(standardConfiguration.getGmapsKey()).disableRetries().build();
		
		LatLng latlng = new LatLng(latitude, longitude);
		GeocodingApiRequest request = new GeocodingApiRequest(context).latlng(latlng);
		try {
			GeocodingResult[] results = request.await();
			System.out.println(Arrays.toString(results));
			int i = 0;
			for (GeocodingResult geocodingResult : results) {
				i++;
				if (i == 2) {
					break;
				}
				System.out.println("Adress: " + geocodingResult.formattedAddress);
				result += geocodingResult.formattedAddress != null ?  geocodingResult.formattedAddress + " " : result + " ";
			}
		} catch (ApiException | InterruptedException | IOException e) {
			logger.error(e.getMessage(), e);
		}
		result = result.replaceAll(", Germany", "");
		return result;
	}

	@Override
	@Transactional(TxType.REQUIRES_NEW)
	public Gym updateOrInsertGymWithRaid(RaidAtGymEvent raidAtGymEvent) {
		// update start and end if somethings missing
		Raid raid = new RaidWithGym(raidAtGymEvent.getGymId());
		raid.setStart(raidAtGymEvent.getStart());
		raid.setEnd(raidAtGymEvent.getEnd());
		raid.setPokemonId(raidAtGymEvent.getPokemonId());
		raid.setRaidLevel(raidAtGymEvent.getLevel());
		if (raid.getEnd() != null) {
			if (raid.getStart() == null) {
				raid.setStart(raid.getEnd() - RAID_DURATION * 60);
			}
		} else if (raid.getStart() != null) {
			if (raid.getEnd() == null) {
				raid.setEnd(raid.getStart() + RAID_DURATION * 60);
			}
		} else {
			logger.warn("Couldn't update start/end of raid -> both are null!");
		}

		// later we use this 'magic number' -1 in sendRaidIfFiltersMatch(...) in
		// TelegramServiceImpl
		if (raid.getPokemonId() == null || raid.getPokemonId().equals(0L)) {
			raid.setPokemonId(-1L);
		}

		Gym gym = new Gym();
		gym.setGymId(raidAtGymEvent.getGymId());
		gym.setRaid(raid);
		gym.setLatitude(raidAtGymEvent.getLatitude());
		gym.setLongitude(raidAtGymEvent.getLongitude());
		gym = updateOrInsertGym(gym);
		entityManager.flush();

		// hope this will work...
		RaidAtGymEvent updatedOrInsertedRaidWithGymEvent = updateOrInsertRaidWithGymEvent(raidAtGymEvent);
		if (null == updatedOrInsertedRaidWithGymEvent) {
			logger.warn("Couldn't update/insert RaidAtGymEvent " + raidAtGymEvent.toString());
		}
		return gym;
	}

	@Override
	public Gym getGym(String gymId) {
		Gym gym = null;
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Gym> query = cb.createQuery(Gym.class);
		query = query.where(cb.equal(query.from(Gym.class).get("gymId"), gymId));
		List<Gym> resultList = entityManager.createQuery(query).getResultList();
		if (!resultList.isEmpty()) {
			gym = resultList.get(0);
		}
		return gym;
	}

	@Override
	@Transactional
	public void deleteOldGymPokemonOnDatabase() {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<GymPokemon> retrieveSpawnpointsWithMon = cb.createQuery(GymPokemon.class);
		retrieveSpawnpointsWithMon.from(GymPokemon.class);

		List<GymPokemon> allGymPokemon = entityManager.createQuery(retrieveSpawnpointsWithMon).getResultList();
		List<Long> allGymMonIds = allGymPokemon.stream().map(pokemon -> pokemon.getId()).collect(Collectors.toList());

		CriteriaQuery<Gym> retrieveProcessedMons = cb.createQuery(Gym.class);
		retrieveProcessedMons.from(Gym.class);
		List<Gym> allGym = entityManager.createQuery(retrieveProcessedMons).getResultList();
		List<List<GymPokemon>> activeMons = allGym.stream().map(processed -> processed.getPokemon())
				.collect(Collectors.toList());
		int numberOfSavedPokemon = 0;
		for (List<GymPokemon> list : activeMons) {
			List<Long> monsInGym = list.stream().map(gymMon -> gymMon.getId()).collect(Collectors.toList());
			allGymMonIds.removeAll(monsInGym);
			numberOfSavedPokemon = numberOfSavedPokemon + monsInGym.size();
		}
		StopWatch stopwatch = StopWatch.createStarted();
		logger.info("Cleaning up old GymPokemon - to delete: " + allGymMonIds.size() + ", to save: "
				+ numberOfSavedPokemon);
		int numberOfDeleted = 0;
		for (Long mon : allGymMonIds) {
			gymPokemonDao.deleteById(mon);
			numberOfDeleted++;
		}
		stopwatch.stop();
		long seconds = stopwatch.getTime(TimeUnit.SECONDS);
		long ending = stopwatch.getTime(TimeUnit.MILLISECONDS) % 100;

		logger.info(
				"Cleaning up old GymPokemon - deleted: " + numberOfDeleted + "\nused time: " + seconds + "," + ending
						+ " secs");
	}

	@Override
	public Gym getGymByInternalId(Long id) {
		return gymRepository.findById(id).orElse(null);
	}
}
