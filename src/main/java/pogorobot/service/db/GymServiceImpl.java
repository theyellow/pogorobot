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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
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

import org.apache.commons.lang3.StringUtils;
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

import pogorobot.entities.EventWithSubscribers;
import pogorobot.entities.Gym;
import pogorobot.entities.GymPokemon;
import pogorobot.entities.Raid;
import pogorobot.entities.RaidAtGymEvent;
import pogorobot.entities.RaidWithGym;
import pogorobot.service.db.repositories.GymPokemonRepository;
import pogorobot.service.db.repositories.GymRepository;
import pogorobot.telegram.config.StandardConfiguration;

@Service("gymService")
public class GymServiceImpl implements GymService {

	private static Logger logger = LoggerFactory.getLogger(GymService.class);

	@SuppressWarnings("squid:S3749")
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
	public boolean updateOrInsertGym(Gym gym) {
		// query existing gyms by lat/lon-equality
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Gym> gymCriteria = queryGymExisting(gym, cb);
		List<Gym> resultList = entityManager.createQuery(gymCriteria).getResultList();

		boolean changedGym = false;
		boolean unvisibleChange = true;

		if (resultList.isEmpty()) {
			logger.info("new gym/stop found");
			entityManager.persist(gym);
			changedGym = true;
			unvisibleChange = false;
		} else {
			Gym oldGym = null;
			if (resultList.size() > 1) {
				logger.warn("found " + resultList.size()
						+ " gyms/stops at this location! Delete unneccessary gyms on database - i'll try to guess the best match for updating on database");
				List<Gym> filteredStream = resultList.stream()
						.filter(x -> !(x.getGymId() == null && x.getName() == null)).collect(Collectors.toList());
				// Optional<Gym> optionalGym = filteredStream.findFirst();
				if (filteredStream == null || filteredStream.isEmpty()) {
					oldGym = resultList.get(0);
					logger.warn(
							"Found no gym/stop with name&id not null, take the first (with id " + oldGym.getId() + ")");
				} else {
					int bestGuessedCount = filteredStream.size();
					if (bestGuessedCount > 1) {
						logger.warn("found " + bestGuessedCount + " potential matches: " + filteredStream);
					}
					oldGym = filteredStream.get(0);
					logger.warn("Take match with id " + oldGym.getId());
				}
			} else {
				logger.debug("found a gym/stop at that location, going to update it");
				oldGym = resultList.get(0);
			}
			if (StringUtils.isNotEmpty(gym.getGymId())) {
				if (!gym.getGymId().equals(oldGym.getGymId())) {
					oldGym.setGymId(gym.getGymId());
					changedGym = true;
					unvisibleChange = true;
				}
			}

			if (gym.getLastModified() != null && gym.getLastModified() != 0L) {
				if (!gym.getLastModified().equals(oldGym.getLastModified())) {
					oldGym.setLastModified(gym.getLastModified());
					changedGym = true;
					unvisibleChange = true;
				}
			}
			if (gym.getLatitude() != null) {
				if (!gym.getLatitude().equals(oldGym.getLatitude())) {
					oldGym.setLatitude(gym.getLatitude());
					changedGym = true;
					unvisibleChange = true;
				}
			}
			if (gym.getLongitude() != null) {
				if (!gym.getLongitude().equals(oldGym.getLongitude())) {
					oldGym.setLongitude(gym.getLongitude());
					changedGym = true;
					unvisibleChange = true;
				}
			}
			if (gym.getOccupiedSince() != null) {
				if (!gym.getOccupiedSince().equals(oldGym.getOccupiedSince())) {
					oldGym.setOccupiedSince(gym.getOccupiedSince());
					changedGym = true;
					unvisibleChange = true;
				}
			}
			if (gym.getPokemon() != null && !gym.getPokemon().isEmpty()) {
				List<GymPokemon> temp = new ArrayList<>();
				for (GymPokemon singlePokemon : gym.getPokemon()) {
					temp.add(singlePokemon);
					entityManager.persist(singlePokemon);
				}
				oldGym.setPokemon(temp);
				changedGym = true;
				unvisibleChange = true;
			}
			if (gym.getTeamId() != null) {
				if (!gym.getTeamId().equals(oldGym.getTeamId())) {
					oldGym.setTeamId(gym.getTeamId());
					changedGym = true;
					unvisibleChange = true;
				}
			}
			if (gym.getSlotsAvailable() != null) {
				if (!gym.getSlotsAvailable().equals(oldGym.getSlotsAvailable())) {
					oldGym.setSlotsAvailable(gym.getSlotsAvailable());
					changedGym = true;
					unvisibleChange = true;
				}
			}
			if (gym.getExraidEglible() != null) {
				if (!gym.getExraidEglible().equals(oldGym.getExraidEglible())) {
					if (gym.getExraidEglible()) {
						logger.debug("found exraid gym :)");
					}
					oldGym.setExraidEglible(gym.getExraidEglible());
					changedGym = true;
					unvisibleChange = true;
				}
			}
			if (gym.getEnabled() != null) {
				if (!gym.getEnabled().equals(oldGym.getEnabled())) {
					oldGym.setEnabled(gym.getEnabled());
					changedGym = true;
					unvisibleChange = true;
				}
			}
			if (gym.getUrl() != null) {
				if (!gym.getUrl().equals(oldGym.getUrl())) {
					oldGym.setUrl(gym.getUrl());
					changedGym = true;
					unvisibleChange = true;
				}
			}
			if (gym.getDescription() != null && !gym.getDescription().equals("''")) {
				if (!gym.getDescription().equals(oldGym.getDescription())) {
					oldGym.setDescription(gym.getDescription());
					changedGym = true;
					unvisibleChange = true;
				}
			}
			if (gym.getRaid() != null) {
				if (!gym.getRaid().equals(oldGym.getRaid())) {
					logger.warn("Raid differs, so we update all posts");
					logger.warn("old raid: {}", oldGym.getRaid());
					logger.warn("new raid: {}", gym.getRaid());
					oldGym.setRaid(gym.getRaid());
					changedGym = true;
					unvisibleChange = false;
				}
			}
			if (StringUtils.isNotEmpty(gym.getName())) {
				if (!gym.getName().equals(oldGym.getName())) {
					oldGym.setName(gym.getName());
					changedGym = true;
					unvisibleChange = false;
				}
			}
			if (gym.getRaidActiveUntil() != null) {
				if (!gym.getRaidActiveUntil().equals(oldGym.getRaidActiveUntil())) {
					logger.warn("raid-active differs, so we update all posts");
					logger.warn("old raid-active: {}", oldGym.getRaidActiveUntil());
					logger.warn("new raid-active: {}", gym.getRaidActiveUntil());
					oldGym.setRaidActiveUntil(gym.getRaidActiveUntil());
					changedGym = true;
					unvisibleChange = false;
				}
			}
			if (oldGym.getAddress() == null || oldGym.getAddress().isEmpty()) {
				// Reenable this (googlemaps-api update never done...)
				// oldGym.setAddress(createAddressFromGeo(oldGym.getLatitude(),
				// oldGym.getLongitude()));
			}
			if (changedGym) {
				gym = entityManager.merge(oldGym);
			}
		}

		if (changedGym) {
			entityManager.flush();
		}
		return !unvisibleChange;
	}

	private CriteriaQuery<Gym> queryGymExisting(Gym gym, CriteriaBuilder cb) {
		CriteriaQuery<Gym> query = cb.createQuery(Gym.class);
		Root<Gym> from = query.from(Gym.class);
		Predicate latitudeEqual = cb.equal(from.get("latitude"), gym.getLatitude());
		Predicate longitudeEqual = cb.equal(from.get("longitude"), gym.getLongitude());
		Predicate gymIdEqualNotNull = cb.and(cb.isNotNull(from.get("gymId")), cb.notEqual(from.get("gymId"), ""),
				cb.equal(from.get("gymId"), gym.getGymId()));
		return query.where(cb.or(gymIdEqualNotNull, cb.and(latitudeEqual, longitudeEqual)));
	}

	private CriteriaQuery<RaidAtGymEvent> queryRaidEventExisting(RaidAtGymEvent raidAtGymEvent, CriteriaBuilder cb) {
		CriteriaQuery<RaidAtGymEvent> query = cb.createQuery(RaidAtGymEvent.class);
		Root<RaidAtGymEvent> from = query.from(RaidAtGymEvent.class);
		Predicate latitudeEqual = cb.equal(from.get("latitude"), raidAtGymEvent.getLatitude());
		Predicate longitudeEqual = cb.equal(from.get("longitude"), raidAtGymEvent.getLongitude());
		Predicate gymIdEqualNotNull = cb.and(cb.isNotNull(from.get("gymId")), cb.notEqual(from.get("gymId"), ""),
				cb.equal(from.get("gymId"), raidAtGymEvent.getGymId()));
		return query.where(cb.or(gymIdEqualNotNull, cb.and(latitudeEqual, longitudeEqual)));
	}

	@Override
	@Transactional(TxType.REQUIRED)
	public RaidAtGymEvent updateOrInsertRaidWithGymEvent(RaidAtGymEvent raidEvent) {
		if (null == raidEvent || null == raidEvent.getGymId() || null == raidEvent.getEnd()) {
			return null;
		}
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();

		// query existing gyms by lat/lon-equality
		// CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<RaidAtGymEvent> gymCriteria = queryRaidEventExisting(raidEvent, cb);
		List<RaidAtGymEvent> resultList = entityManager.createQuery(gymCriteria).getResultList();

		// CriteriaQuery<RaidAtGymEvent> query = cb.createQuery(RaidAtGymEvent.class);
		// Root<RaidAtGymEvent> from = query.from(RaidAtGymEvent.class);
		// Path<Object> pathToId = from.get("id");
		// Path<Object> pathToLatitude = from.get("latitude");
		// Path<Object> pathToLongitude = from.get("longitude");
		// query = query
		// .where(cb.or(cb.equal(pathToId, raidEvent.getId()),
		// cb.and(cb.equal(pathToLatitude, raidEvent.getLatitude()),
		// cb.equal(pathToLongitude, raidEvent.getLongitude()))));
		// List<RaidAtGymEvent> resultList =
		// entityManager.createQuery(query).getResultList();

		SortedSet<EventWithSubscribers> eventsWithSubscribers = null;
		boolean raidEventChanged = false;

		if (resultList.isEmpty()) {
			String id = raidEvent.getId() != null ? raidEvent.getId() : raidEvent.getGymId();
			raidEvent.setId(id);
			eventsWithSubscribers = raidEvent.getEventsWithSubscribers();
			logger.info("persist new raid with id " + id + " and " + eventsWithSubscribers.size() + " event-slots.");
			eventsWithSubscribers.stream().forEach(x -> entityManager.persist(x));
			entityManager.persist(raidEvent);
			raidEventChanged = true;
		} else {
			RaidAtGymEvent oldEvent = resultList.get(0);
			if (!oldEvent.hasEventWithSubscribers()) {
				logger.info("old event set is empty, use new one (and initialize if not existing)");
				eventsWithSubscribers = raidEvent.getEventsWithSubscribers();
				oldEvent.setEventsWithSubscribers(eventsWithSubscribers);
				raidEventChanged = true;
			}
			// if (oldEvent.getEventsWithSubscribers() == null) {
			// }
			// else
			// if (eventsWithSubscribers != null && eventsWithSubscribers.size() > 0) {
			// logger.warn("Events get overwritten! with {}", eventsWithSubscribers);
			// oldEvent.setEventsWithSubscribers(eventsWithSubscribers);
			// }
			if (raidEvent.getEnd() != null) {
				if (!raidEvent.getEnd().equals(oldEvent.getEnd())) {
					oldEvent.setEnd(raidEvent.getEnd());
					raidEventChanged = true;
				}
			}
			if (raidEvent.getGymId() != null) {
				if (!raidEvent.getGymId().equals(oldEvent.getGymId())) {
					oldEvent.setGymId(raidEvent.getGymId());
					raidEventChanged = true;
				}
			}
			if (raidEvent.getStart() != null) {
				if (!raidEvent.getStart().equals(oldEvent.getStart())) {
					oldEvent.setStart(raidEvent.getStart());
					raidEventChanged = true;
				}
			}
			if (raidEvent.getPokemonId() != null) {
				if (!raidEvent.getPokemonId().equals(oldEvent.getPokemonId())) {
					oldEvent.setPokemonId(raidEvent.getPokemonId());
					raidEventChanged = true;
				}
			}
			if (oldEvent.getId() == null) {
				String id = raidEvent.getId() != null ? raidEvent.getId() : raidEvent.getGymId();
				oldEvent.setId(id);
				raidEventChanged = true;
			}
			if (raidEventChanged) {
				oldEvent.getEventsWithSubscribers().stream().forEach(x -> entityManager.persist(x));
				raidEvent = entityManager.merge(oldEvent);
			}
		}
		if (raidEventChanged) {
			entityManager.flush();
		}
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
				logger.info("adress: " + geocodingResult.formattedAddress);
				result += geocodingResult.formattedAddress != null ? geocodingResult.formattedAddress + " "
						: result + " ";
			}
		} catch (ApiException | InterruptedException | IOException e) {
			if (e instanceof InterruptedException) {
				logger.error("interrupted while creating geoAdress", e);
				Thread.currentThread().interrupt();
			}
		}
		result = result.replaceAll(", Germany", "");
		return result;
	}

	@Override
	@Transactional(TxType.REQUIRES_NEW)
	public boolean updateOrInsertGymWithRaid(RaidAtGymEvent raidAtGymEvent) {
		Gym gym = transformRaidEventToGy(raidAtGymEvent);

		boolean changed = updateOrInsertGym(gym);

		// Think i don't need this:
		// entityManager.flush();

		raidAtGymEvent.setLatitude(gym.getLatitude());
		raidAtGymEvent.setLongitude(gym.getLongitude());
		raidAtGymEvent.setGymId(gym.getGymId());

		// hope this will work...
		RaidAtGymEvent updatedOrInsertedRaidWithGymEvent = updateOrInsertRaidWithGymEvent(raidAtGymEvent);
		if (null == updatedOrInsertedRaidWithGymEvent) {
			logger.warn("couldn't update/insert RaidAtGymEvent " + raidAtGymEvent.toString());
		}
		return changed;
	}

	private Gym transformRaidEventToGy(RaidAtGymEvent raidAtGymEvent) {
		// update start and end if somethings missing
		Raid raid = new RaidWithGym(raidAtGymEvent.getGymId());
		raid.setStart(raidAtGymEvent.getStart());
		raid.setEnd(raidAtGymEvent.getEnd());
		raid.setPokemonId(raidAtGymEvent.getPokemonId());
		raid.setRaidLevel(raidAtGymEvent.getLevel());

		// Workaround for bad data: fill start/end if end/start exists
		if (raid.getEnd() != null) {
			if (raid.getStart() == null) {
				raid.setStart(raid.getEnd() - RAID_DURATION * 60);
			}
		} else if (raid.getStart() != null) {
			if (raid.getEnd() == null) {
				raid.setEnd(raid.getStart() + RAID_DURATION * 60);
			}
		} else {
			logger.warn("couldn't update start/end of raid -> both are null!");
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
		logger.info("cleaning up old GymPokemon - to delete: " + allGymMonIds.size() + ", to save: "
				+ numberOfSavedPokemon);
		int numberOfDeleted = 0;
		for (Long mon : allGymMonIds) {
			gymPokemonDao.deleteById(mon);
			numberOfDeleted++;
		}
		stopwatch.stop();
		long seconds = stopwatch.getTime(TimeUnit.SECONDS);
		long ending = stopwatch.getTime(TimeUnit.MILLISECONDS) % 100;

		logger.info("cleaning up old GymPokemon - deleted: " + numberOfDeleted + "\nused time: " + seconds + ","
				+ ending + " secs");
	}

	@Override
	public Gym getGymByInternalId(Long id) {
		return gymRepository.findById(id).orElse(null);
	}
}
