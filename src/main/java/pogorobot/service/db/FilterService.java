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

/**
 * 
 */
package pogorobot.service.db;

import java.util.List;
import java.util.Set;

import com.bbn.openmap.geo.GeoArray;

import pogorobot.entities.Filter;
import pogorobot.entities.FilterType;
import pogorobot.entities.Geofence;
import pogorobot.entities.Gym;
import pogorobot.entities.User;
import pogorobot.events.EventMessage;
import pogorobot.events.rocketmap.RocketmapEgg;
import pogorobot.events.rocketmap.RocketmapGym;
import pogorobot.events.rocketmap.RocketmapPokemon;
import pogorobot.telegram.util.Type;

public interface FilterService {

	Filter updateOrInsertFilter(Filter filter);
	
	void deleteFilter(Filter filter);
	
	List<Filter> getAllFilters();
	
	List<Filter> getFiltersByType(FilterType type);

	List<Filter> getFiltersForEvent(EventMessage<?> message);

	List<Filter> getFiltersForUsers(List<User> users);

	List<Integer> getFilterRaidsForTelegramId(String telegramId);
	
	Filter addPokemonToUserfilter(String telegramId, int pokemonId);

	Filter removePokemonFromUserfilter(String telegramId, int pokemonId);

	Filter removeRaidPokemonFromUserfilter(String telegramId, int pokemonId);

	boolean processGymFilter(Filter filter, RocketmapGym gym);

	boolean processEggFilter(Filter filter, RocketmapEgg egg);

	boolean processPokemonFilter(Filter filter, RocketmapPokemon pokemon);

	Filter createFilter(List<User> receivers, List<Integer> pokemon, List<Gym> gyms,
			Double latitude, Double longitude, Double radius, Integer raidLevel);

	Filter addRaidPokemonToUserfilter(String telegramId, int pokemonId);

	List<Integer> getAllRaidPokemon();

	Filter setUserRaidLevel(Long telegramId, String level);

	Geofence getGeofenceByName(String geofenceName);

	GeoArray getGeoArrayForGeofence(String geofenceName);

	Set<Geofence> getGeofencesForTelegramId(Long telegramId);

	boolean isPointInOneGeofenceOfTelegramId(double latitude, double longitude, Long telegramId);

	boolean isPointInGeofence(double latitude, double longitude, String geofenceName);

	boolean isPointInOneGeofenceOfFilterByType(double latitude, double longitude, Filter filter, Type pokemonFence);

	boolean isPointInOneOfManyGeofences(double latitude, double longitude, Set<Geofence> geofences);

	boolean isDistanceNearby(double latitude, double longitude, Double userLatitude, Double userLongitude,
			Double radius);

	// Filter setUserradius(Long telegramId, String radius);

	Set<Geofence> getAllGeofences();

	List<Geofence> getGeofencesForUser(User user, Type pokemon);

	void addGeofenceToUserfilter(String telegramId, String geofenceName, Type pokemonFilter);

	void removeGeofenceFromUserfilter(String telegramId, String geofenceName, Type pokemonFilter);

	Filter setUserMinIv(Long telegramId, String minIv);

	Filter setUserPokemonRadius(Long telegramId, String radius);

	Filter setUserRaidRadius(Long telegramId, String radius);

	Filter setUserIvRadius(Long telegramId, String radius);

	Double calculateDistanceInKilometer(Double latitude, Double longitude, Double latitude2, Double longitude2);

}