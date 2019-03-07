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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaBuilder.In;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bbn.openmap.geo.Geo;
import com.bbn.openmap.geo.GeoArray;
import com.bbn.openmap.geo.Intersection;

import pogorobot.entities.EggWithGym;
import pogorobot.entities.Filter;
import pogorobot.entities.FilterType;
import pogorobot.entities.Geofence;
import pogorobot.entities.Gym;
import pogorobot.entities.PokemonWithSpawnpoint;
import pogorobot.entities.PossibleRaidPokemon;
import pogorobot.entities.RaidWithGym;
import pogorobot.entities.User;
import pogorobot.events.EventMessage;
import pogorobot.events.rocketmap.RocketmapEgg;
import pogorobot.events.rocketmap.RocketmapGym;
import pogorobot.events.rocketmap.RocketmapPokemon;
import pogorobot.repositories.FilterRepository;
import pogorobot.repositories.GeofenceRepository;
import pogorobot.repositories.PossibleRaidPokemonRepository;
import pogorobot.telegram.util.Type;

@Service("filterService")
public class FilterServiceImpl implements FilterService {

	private Logger logger = LoggerFactory.getLogger(this.getClass().getInterfaces()[0]);

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private FilterRepository filterDAO;

	@Autowired
	private GeofenceRepository geofenceDAO;

	@Autowired
	private UserService userService;

	@Autowired
	private PossibleRaidPokemonRepository raidPokemonRepository;

	@Override
	public List<Filter> getFiltersByType(FilterType type) {
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		CriteriaQuery<Filter> query = criteriaBuilder.createQuery(Filter.class);
		Root<Filter> root = query.from(Filter.class);
		query = query.where(criteriaBuilder.equal(root.get("filterType"), type));
		List<Filter> result = entityManager.createQuery(query).getResultList();
		return result;
	}

	@Override
	// @Query("SELECT Filter FROM Filter u inner join Filter_telegramIds m where
	// m.telegramIds in :telegramIds")
	public List<Filter> getFiltersForUsers(List<User> telegramIds) {
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		CriteriaQuery<Filter> query = criteriaBuilder.createQuery(Filter.class);
		Root<Filter> root = query.from(Filter.class);
		Expression<List<String>> path = root.get("receivers");
		In<Boolean> matchingTelegramIds = criteriaBuilder.in(path.in(telegramIds));
		List<Filter> result = entityManager.createQuery(query.where(matchingTelegramIds)).getResultList();
		return result;
	}

	@Override
	public boolean processGymFilter(Filter filter, RocketmapGym gym) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean processEggFilter(Filter filter, RocketmapEgg egg) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean processPokemonFilter(Filter filter, RocketmapPokemon pokemon) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<Filter> getFiltersForEvent(EventMessage<?> message) {
		Object entity = message.transformToEntity();
		if (entity instanceof EggWithGym) {
			CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
			CriteriaQuery<Filter> query = criteriaBuilder.createQuery(Filter.class);
			Root<Filter> root = query.from(Filter.class);
			query = query.where(criteriaBuilder.equal(root.get("gymIds"), ((EggWithGym) entity).getGymId()));
			return entityManager.createQuery(query).getResultList();
		} else if (entity instanceof RaidWithGym) {
			CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
			CriteriaQuery<Filter> query = criteriaBuilder.createQuery(Filter.class);
			Root<Filter> root = query.from(Filter.class);
			query = query.where(criteriaBuilder.equal(root.get("gymIds"), ((RaidWithGym) entity).getGymId()));
			return entityManager.createQuery(query).getResultList();
		} else if (entity instanceof PokemonWithSpawnpoint) {
			CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
			CriteriaQuery<Filter> query = criteriaBuilder.createQuery(Filter.class);
			Root<Filter> root = query.from(Filter.class);
			query = query.where(
					criteriaBuilder.equal(root.get("pokemons"), ((PokemonWithSpawnpoint) entity).getPokemonId()));
			return entityManager.createQuery(query).getResultList();
		}
		return new ArrayList<>();
	}

	@Override
	public Filter createFilter(List<User> receivers, List<Integer> pokemon, List<Gym> gyms, Double latitude,
			Double longitude, Double radius, Integer raidLevel) {
		Filter filter = new Filter();
		filter.setLatitude(latitude);
		filter.setLongitude(longitude);
		filter.setRadius(radius);
		// filter.setGyms(gyms);
		filter.setPokemons(pokemon);
		filter.setRaidLevel(raidLevel);
		return filter;
	}

	@Override
	@Transactional
	public Filter updateOrInsertFilter(Filter filter) {
		List<Filter> resultList = null;
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		CriteriaQuery<Filter> query = criteriaBuilder.createQuery(Filter.class);
		Root<Filter> from = query.from(Filter.class);
		if (filter.getOwner() != null) {
			query = query.where(
					criteriaBuilder.equal(from.join("owner").get("telegramId"), filter.getOwner().getTelegramId()));
		} else if (filter.getGroup() != null) {
			query = query.where(
					criteriaBuilder.equal(from.join("group").get("groupName"), filter.getGroup().getGroupName()));
		}
		resultList = entityManager.createQuery(query).getResultList();
		if (resultList.size() == 0) {
			entityManager.persist(filter);
		} else {
			Filter dbFilter = resultList.get(0);
			dbFilter.setGroup(filter.getGroup());
			dbFilter.setGymPokemons(filter.getGymPokemons());
			// dbFilter.setGyms(filter.getGyms());
			dbFilter.setLatitude(filter.getLatitude());
			dbFilter.setMinIV(filter.getMinIV());
			dbFilter.setMinWP(filter.getMinWP());
			dbFilter.setOnlyWithIV(filter.getOnlyWithIV());
			dbFilter.setGeofences(filter.getGeofences());
			dbFilter.setRaidGeofences(filter.getRaidGeofences());
			dbFilter.setIvGeofences(filter.getIvGeofences());
			dbFilter.setLongitude(filter.getLongitude());
			dbFilter.setRadius(filter.getRadius());
			dbFilter.setRaidLevel(filter.getRaidLevel());
			dbFilter.setOwner(filter.getOwner());
			dbFilter.setPokemons(filter.getPokemons());
			dbFilter.setRaidPokemon(filter.getRaidPokemon());
			dbFilter.setReceiveFromGroups(filter.getReceiveFromGroups());
			dbFilter.setReceivers(filter.getReceivers());
			dbFilter.setFilterType(getTypeOfFilter(dbFilter));
			dbFilter.setGroup(filter.getGroup());
			return entityManager.merge(dbFilter);
		}
		entityManager.flush();
		return filter;
	}

	private FilterType getTypeOfFilter(Filter filter) {
		FilterType result = filter.getFilterType();
		if (filter.getOwner() != null && filter.getGroup() != null) {
			result = FilterType.ALL;
		} else if (filter.getOwner() != null) {
			result = FilterType.USER;
		} else if (filter.getGroup() != null) {
			result = FilterType.GROUP;
		}
		return result;
	}

	@Override
	public void deleteFilter(Filter filter) {
		filterDAO.delete(filter);
	}

	@Override
	public List<Filter> getAllFilters() {
		List<Filter> result = new ArrayList<>();
		Iterable<Filter> all = filterDAO.findAll();
		for (Filter filter : all) {
			result.add(filter);
		}
		return result;
	}

	@Override
	public Filter addPokemonToUserfilter(String telegramId, int pokemonId) {
		User user = userService.getOrCreateUser(telegramId);
		List<Integer> pokemons = user.getUserFilter().getPokemons();
		pokemons.add(pokemonId);
		Collections.sort(pokemons);
		user = userService.updateOrInsertUser(user);
		return user.getUserFilter();
	}

	@Override
	@Transactional
	public Filter addRaidPokemonToUserfilter(String telegramId, int pokemonId) {
		User user = userService.getOrCreateUser(telegramId);
		List<Integer> raidPokemon = user.getUserFilter().getRaidPokemon();
		raidPokemon.add(pokemonId);
		Collections.sort(raidPokemon);
		user = userService.updateOrInsertUser(user);
		return user.getUserFilter();
	}

	@Override
	public Filter removePokemonFromUserfilter(String telegramId, int pokemonId) {
		User user = userService.getOrCreateUser(telegramId);
		List<Integer> pokemons = user.getUserFilter().getPokemons();
		pokemons.remove(pokemons.indexOf(pokemonId));
		Collections.sort(pokemons);
		user = userService.updateOrInsertUser(user);
		return user.getUserFilter();
	}

	@Override
	public List<Integer> getAllRaidPokemon() {
		List<Integer> raidPokemons = new ArrayList<>();
		List<PossibleRaidPokemon> pokemonToOrder = new ArrayList<>();
		raidPokemonRepository.findAll().forEach(pokemon -> pokemonToOrder.add(pokemon));
		raidPokemons = pokemonToOrder.stream().sorted((y, x) -> x.getLevel().compareTo(y.getLevel()))
				.map(x -> x.getPokemonId()).collect(Collectors.toList());
		return raidPokemons;
	}

	@Override
	@Transactional
	public List<Integer> getFilterRaidsForTelegramId(String telegramId) {
		User user = userService.getOrCreateUser(telegramId);
		Filter filter = user.getUserFilter();
		filter = entityManager.find(Filter.class, filter.getId());
		List<Integer> raidPokemon = filter.getRaidPokemon();
		raidPokemon.size();
		return raidPokemon;
	}

	@Override
	@Transactional
	public Filter removeRaidPokemonFromUserfilter(String telegramId, int pokemonId) {
		User user = userService.getOrCreateUser(telegramId);
		List<Integer> pokemons = user.getUserFilter().getRaidPokemon();
		pokemons.remove(pokemons.indexOf(pokemonId));
		user = userService.updateOrInsertUser(user);
		return user.getUserFilter();
	}

	@Override
	public Filter setUserRaidLevel(Long telegramId, String level) {
		User user = userService.getOrCreateUser(telegramId.toString());
		user.getUserFilter().setRaidLevel(Integer.valueOf(level));
		user = userService.updateOrInsertUser(user);
		return user.getUserFilter();
	}

	@Override
	@Transactional(TxType.REQUIRES_NEW)
	public Filter setUserMinIv(Long telegramId, String minIv) {
		User user = userService.getOrCreateUser(telegramId.toString());
		user.getUserFilter().setMinIV(Double.valueOf(minIv));
		user = userService.updateOrInsertUser(user);
		return user.getUserFilter();
	}

	// @Override
	// public Filter setUserradius(Long telegramId, String level) {
	// User user = userService.getOrCreateUser(telegramId.toString());
	// user.getUserFilter().setRadius(Double.valueOf(level));
	// user = userService.updateOrInsertUser(user);
	// return user.getUserFilter();
	// }

	@Override
	public Filter setUserPokemonRadius(Long telegramId, String radius) {
		User user = userService.getOrCreateUser(telegramId.toString());
		user.getUserFilter().setRadiusPokemon(Double.valueOf(radius));
		user = userService.updateOrInsertUser(user);
		return user.getUserFilter();
	}

	@Override
	public Filter setUserRaidRadius(Long telegramId, String radius) {
		User user = userService.getOrCreateUser(telegramId.toString());
		user.getUserFilter().setRadiusRaids(Double.valueOf(radius));
		user = userService.updateOrInsertUser(user);
		return user.getUserFilter();
	}

	@Override
	public Filter setUserIvRadius(Long telegramId, String radius) {
		User user = userService.getOrCreateUser(telegramId.toString());
		user.getUserFilter().setRadiusIV(Double.valueOf(radius));
		user = userService.updateOrInsertUser(user);
		return user.getUserFilter();
	}

	@Override
	public boolean isPointInGeofence(double latitude, double longitude, String geofenceName) {
		Geo geo = new Geo(latitude, longitude);
		GeoArray polygon = getGeoArrayForGeofence(geofenceName);
		return Intersection.isPointInPolygon(geo, polygon);
	}

	@Override
	@Deprecated
	public boolean isPointInOneGeofenceOfTelegramId(double latitude, double longitude, Long telegramId) {
		Set<Geofence> geofences = getGeofencesForTelegramId(telegramId);
		if (geofences == null || geofences.size() == 0) {
			return false;
		}
		Geo geo = new Geo(latitude, longitude);
		for (Geofence geofence : geofences) {
			List<Double> polygon = geofence.getPolygon();
			double[] a = new double[polygon.size()];
			for (int i = 0; i < polygon.size(); i++) {
				a[i] = polygon.get(i);
			}
			if (Intersection.isPointInPolygon(geo, GeoArray.Double.createFromLatLonDegrees(a))) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isPointInOneOfManyGeofences(double latitude, double longitude, Set<Geofence> geofences) {
		if (geofences == null || geofences.size() == 0) {
			return true;
		}
		Geo geo = new Geo(latitude, longitude);
		for (Geofence geofence : geofences) {
			List<Double> polygon = geofence.getPolygon();
			double[] a = new double[polygon.size()];
			for (int i = 0; i < polygon.size(); i++) {
				a[i] = polygon.get(i);
			}
			if (Intersection.isPointInPolygon(geo, GeoArray.Double.createFromLatLonDegrees(a))) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isDistanceNearby(double latitude, double longitude, Double latitudeCenter, Double longitudeCenter,
			Double radius) {
		if (radius == null || latitudeCenter == null || longitudeCenter == null) {
			logger.debug("Lat/Lon of center or radius null");
			return false;
		}
		Double distance = calculateDistanceInKilometer(latitude, longitude, latitudeCenter, longitudeCenter);
		logger.debug("Distance is " + distance + ", radius is " + radius);
		return distance < radius;
	}

	@Override
	public Double calculateDistanceInKilometer(Double latitude, Double longitude, Double latitudeCenter,
			Double longitudeCenter) {
		if (latitudeCenter == null || longitudeCenter == null) {
			logger.debug("Latitude or longitude of center are null");
			return null;
		}
		int EARTH_RADIUS = 6371;

		double dLat = Math.toRadians((latitudeCenter - latitude));
		double dLong = Math.toRadians((longitudeCenter - longitude));

		latitude = Math.toRadians(latitude);
		latitudeCenter = Math.toRadians(latitudeCenter);

		double a = haversin(dLat) + Math.cos(latitude) * Math.cos(latitudeCenter) * haversin(dLong);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

		return EARTH_RADIUS * c; // <-- d

	}

	public double haversin(double val) {
		return Math.pow(Math.sin(val / 2), 2);
	}

	@Override
	public boolean isPointInOneGeofenceOfFilterByType(double latitude, double longitude, Filter filter,
			Type pokemonFence) {
		Set<Geofence> geofences = null;
		switch (pokemonFence) {
		case POKEMON:
			geofences = filter.getGeofences();
			break;
		case RAID:
			geofences = filter.getRaidGeofences();
			break;
		case IV:
			geofences = filter.getIvGeofences();
			break;

		default:
			logger.debug("Can't find geofence " + pokemonFence);
			break;
		}
		if (geofences == null || geofences.size() == 0) {
			logger.debug("There are no geofences");
			return false;
		}
		Geo geo = new Geo(latitude, longitude);
		for (Geofence geofence : geofences) {
			List<Double> polygon = geofence.getPolygon();
			double[] a = new double[polygon.size()];
			for (int i = 0; i < polygon.size(); i++) {
				a[i] = polygon.get(i);
			}
			if (Intersection.isPointInPolygon(geo, GeoArray.Double.createFromLatLonDegrees(a))) {
				logger.debug(pokemonFence + "-filter matched for type ");
				return true;
			} else {
				logger.debug(pokemonFence + "-filter didn't match for type " + pokemonFence);
			}
		}
		return false;
	}

	@Override
	@Transactional
	public Set<Geofence> getGeofencesForTelegramId(Long telegramId) {
		User user = userService.getOrCreateUser(telegramId.toString());
		return user.getUserFilter().getGeofences();
	}

	@Override
	@Transactional
	public List<Geofence> getGeofencesForUser(User user, Type pokemon) {
		user = userService.getOrCreateUser(user.getTelegramId());
		Set<Geofence> geofences = null;
		switch (pokemon) {
		case POKEMON:
			geofences = user.getUserFilter().getGeofences();
			break;
		case RAID:
			geofences = user.getUserFilter().getRaidGeofences();
			break;
		case IV:
			geofences = user.getUserFilter().getIvGeofences();
			break;
		default:
			break;
		}
		geofences.size();
		return geofences.stream().collect(Collectors.toList());
	}

	@Override
	public GeoArray getGeoArrayForGeofence(String geofenceName) {
		Geofence poly = getGeofenceByName(geofenceName);
		List<Double> polygon = poly.getPolygon();
		double[] a = new double[polygon.size()];
		for (int i = 0; i < polygon.size(); i++) {
			a[i] = polygon.get(i);
		}
		return GeoArray.Double.createFromLatLonDegrees(a);
	}

	@Override
	@Transactional
	public Geofence getGeofenceByName(String geofenceName) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Geofence> criteriaQuery = cb.createQuery(Geofence.class);
		Root<Geofence> from = criteriaQuery.from(Geofence.class);
		criteriaQuery = criteriaQuery.where(cb.equal(from.get("geofenceName"), geofenceName));
		TypedQuery<Geofence> query = entityManager.createQuery(criteriaQuery);
		List<Geofence> resultList = query.getResultList();
		if (resultList.size() > 0) {
			return resultList.get(0);
		}
		return null;
	}

	@Override
	public Set<Geofence> getAllGeofences() {
		Set<Geofence> result = new HashSet<>();
		Iterable<Geofence> all = geofenceDAO.findAll();
		for (Geofence filter : all) {
			result.add(filter);
		}
		return result;
	}

	@Override
	@Transactional
	public void addGeofenceToUserfilter(String telegramId, String geofenceName, Type pokemonFilter) {
		Geofence geofence = getGeofenceByName(geofenceName);
		User user = userService.getOrCreateUser(telegramId);
		// System.out.println("Searched " + geofenceName + ". Found " +
		// geofence);
		Filter userFilter = user.getUserFilter();
		switch (pokemonFilter) {
		case POKEMON:
			userFilter.getGeofences().size();
			userFilter.getGeofences().add(geofence);
			break;
		case RAID:
			userFilter.getRaidGeofences().size();
			userFilter.getRaidGeofences().add(geofence);
			break;
		case IV:
			userFilter.getIvGeofences().size();
			userFilter.getIvGeofences().add(geofence);
			break;

		default:
			break;
		}
		entityManager.merge(userFilter);
		// filterDAO.save(userFilter);
	}

	@Override
	@Transactional
	public void removeGeofenceFromUserfilter(String telegramId, String geofenceName, Type pokemonFilter) {
		Geofence geofence = getGeofenceByName(geofenceName);
		User user = userService.getOrCreateUser(telegramId);
		Filter userFilter = user.getUserFilter();
		switch (pokemonFilter) {
		case POKEMON:
			userFilter.getGeofences().size();
			userFilter.getGeofences().remove(geofence);
			break;
		case RAID:
			userFilter.getRaidGeofences().size();
			userFilter.getRaidGeofences().remove(geofence);
			break;
		case IV:
			userFilter.getIvGeofences().size();
			userFilter.getIvGeofences().remove(geofence);
			break;
		default:
			break;
		}
		filterDAO.save(userFilter);
	}

}
