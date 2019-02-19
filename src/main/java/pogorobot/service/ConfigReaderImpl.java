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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import pogorobot.entities.Filter;
import pogorobot.entities.FilterType;
import pogorobot.entities.Geofence;
import pogorobot.entities.MessageConfigElement;
import pogorobot.entities.UserGroup;
import pogorobot.repositories.FilterRepository;
import pogorobot.repositories.GeofenceRepository;
import pogorobot.repositories.UserGroupRepository;

@Service("configReader")
public class ConfigReaderImpl implements ConfigReader {

	Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private UserGroupRepository userGroupDAO;

	@Autowired
	private FilterRepository filterDAO;

	@Autowired
	private FilterService filterService;

	@Autowired
	private GeofenceRepository geofenceDAO;

	@Override
	public Map<MessageConfigElement, JSONObject> getMessageTemplate() throws IOException {
		return parseMessageTemplate("dts.json");
	}

	private Map<MessageConfigElement, JSONObject> parseMessageTemplate(String fileName) {
		// get dts.json
		BufferedReader bufferedFileReader = getBufferedFileReader(fileName);
		if (bufferedFileReader == null) {
			return new HashMap<>();
		}
		JSONObject jsonObject = getJson(bufferedFileReader);
		Map<MessageConfigElement, JSONObject> configMap = parseConfigFile(jsonObject);
		// parse for easy usage
		return configMap;
	}

	private Map<MessageConfigElement, JSONObject> parseConfigFile(JSONObject jsonObject) {
		Map<MessageConfigElement, JSONObject> result = new HashMap<>();
		addMessageConfigElement(result, MessageConfigElement.CONFIG_ELEMENT_MONSTER, jsonObject);
		addMessageConfigElement(result, MessageConfigElement.CONFIG_ELEMENT_MONSTER_NOIV, jsonObject);
		addMessageConfigElement(result, MessageConfigElement.CONFIG_ELEMENT_RAID, jsonObject);
		addMessageConfigElement(result, MessageConfigElement.CONFIG_ELEMENT_EGG, jsonObject);
		// addMessageConfigElement(result,
		// MessageConfigElement.CONFIG_ELEMENT_QUEST_SIMPLE, jsonObject);
		// addMessageConfigElement(result,
		// MessageConfigElement.CONFIG_ELEMENT_QUEST_APPLE_MONSTER, jsonObject);
		// addMessageConfigElement(result,
		// MessageConfigElement.CONFIG_ELEMENT_QUEST_GOOGLE_MONSTER, jsonObject);
		return result;
	}

	private void addMessageConfigElement(Map<MessageConfigElement, JSONObject> result,
			MessageConfigElement configElement, JSONObject jsonObject) {
		Object value = jsonObject.get(configElement.getPath());
		if (value != null) {
			result.put(configElement, new JSONObject(value.toString()));
		}
	}

	private JSONObject getJson(BufferedReader jsonFileReader) {
		JSONTokener tokener = new JSONTokener(jsonFileReader);
		JSONObject json = new JSONObject(tokener);
		return json;
	}

	@Override
	public void updateGeofences() throws IOException {
		List<Geofence> geofences = getGeofences();
		Iterable<Geofence> alreadyInstalled = geofenceDAO.findAll();
		List<Geofence> fencesToSave = new ArrayList<>();
		for (Geofence newFence : geofences) {
			int size = fencesToSave.size();
			for (Geofence installedFence : alreadyInstalled) {
				if (installedFence.getGeofenceName().equals(newFence.getGeofenceName())) {
					installedFence.setPolygon(newFence.getPolygon());
					fencesToSave.add(installedFence);
				}
			}
			if (size == fencesToSave.size()) {
				fencesToSave.add(newFence);
			}
		}
		geofenceDAO.saveAll(fencesToSave);
	}

	@Override
	public void updateGroupsWithIds() throws IOException {
		String fileName = "groupchatid.txt";
		Map<String, List<Long>> groupIds = parseGroupConfigFile(fileName, x -> Long.parseLong(x));
		updateUserGroups(groupIds);
	}

	@Override
	public void updateGroupFilterWithMons() throws IOException {
		List<Filter> newFilters = getFiltersWithGroupPokemonFromFile();
		updateFilterWithGroupsInternally(newFilters);
	}

	@Override
	public void updateGroupFilterWithIV() throws IOException {
		List<Filter> newFilters = getFiltersWithGroupIVFromFile();
		updateFilterWithGroupsInternally(newFilters);
	}

	@Override
	public void updateGroupFilterWithRaidMons() throws IOException {
		List<Filter> newFilters = getFiltersWithGroupRaidPokemonFromFile();
		updateFilterWithGroupsInternally(newFilters);
	}

	private void updateFilterWithGroupsInternally(List<Filter> newFilters) {
		List<Filter> groupFilters = filterService.getFiltersByType(FilterType.GROUP);
		List<UserGroup> userGroups = new ArrayList<>();
		userGroupDAO.findAll().forEach(x -> userGroups.add(x));
		for (Filter newFilter : newFilters) {
			boolean alreadyExisting = false;
			UserGroup group = newFilter.getGroup();
			for (Filter installedFilter : groupFilters) {
				UserGroup installedGroup = installedFilter.getGroup();
				if (installedGroup != null && installedGroup.getGroupName().equals(group.getGroupName())) {

					Double minIV = newFilter.getMinIV();
					if (minIV != null) {
						installedFilter.setMinIV(minIV);
					}
					List<Integer> raidPokemon = newFilter.getRaidPokemon();
					if (raidPokemon != null && !raidPokemon.isEmpty()) {
						installedFilter.setRaidPokemon(raidPokemon);
					}
					List<Integer> pokemon = newFilter.getPokemons();
					if (pokemon != null && !pokemon.isEmpty()) {
						installedFilter.setPokemons(pokemon);
					}

					Set<Geofence> geofences = newFilter.getGeofences();
					if (geofences != null) {
						Set<Geofence> geofencesNew = new TreeSet<>();
						for (Geofence geofence : geofences) {
							geofencesNew.add(geofence);
						}
						installedFilter.setGeofences(geofencesNew);
						installedFilter.setRaidGeofences(geofencesNew);
						installedFilter.setIvGeofences(geofencesNew);
					}

					Set<Geofence> ivGeofences = newFilter.getIvGeofences();
					if (ivGeofences != null) {
						Set<Geofence> geofencesNew = new TreeSet<>();
						for (Geofence geofence : ivGeofences) {
							geofencesNew.add(geofence);
						}
						installedFilter.setIvGeofences(geofencesNew);
					}

					Set<Geofence> raidGeofences = newFilter.getRaidGeofences();
					if (raidGeofences != null) {
						Set<Geofence> geofencesNew = new TreeSet<>();
						for (Geofence geofence : raidGeofences) {
							geofencesNew.add(geofence);
						}
						installedFilter.setRaidGeofences(geofencesNew);
					}
					filterDAO.save(installedFilter);
					alreadyExisting = true;
					break;
				}
			}
			if (!alreadyExisting) {
				String groupName = group.getGroupName();
				List<UserGroup> existingUserGroupWithGroupName = userGroups.stream()
						.filter(x -> x.getGroupName().equals(groupName)).collect(Collectors.toList());
				if (!existingUserGroupWithGroupName.isEmpty()) {
					newFilter.setGroup(existingUserGroupWithGroupName.get(0));
					newFilter = filterDAO.save(newFilter);
					existingUserGroupWithGroupName.get(0).setGroupFilter(newFilter);
					userGroupDAO.save(existingUserGroupWithGroupName.get(0));
				} else {
					group = userGroupDAO.save(group);
					newFilter.setGroup(group);
					group.setGroupFilter(newFilter);
				}
				filterDAO.save(newFilter);
			}
		}
	}

	@Override
	public void updateGroupFiltersWithGeofences() throws IOException {
		String fileName = "groupgeofences.txt";

		List<Geofence> existingFences = new ArrayList<>();
		geofenceDAO.findAll().forEach(x -> existingFences.add(x));
		Map<String, List<String>> groupIds = parseGroupIdFile(fileName);
		List<Filter> groupFilters = filterService.getFiltersByType(FilterType.GROUP);
		List<UserGroup> userGroups = new ArrayList<>();
		userGroupDAO.findAll().forEach(x -> userGroups.add(x));
		for (Entry<String, List<String>> groupWithFences : groupIds.entrySet()) {
			String groupName = groupWithFences.getKey();
			List<Geofence> existingChosenFences = getExistingChosenFences(existingFences, groupWithFences.getValue());
			boolean alreadyAdded = false;
			for (Filter existingFilter : groupFilters) {
				UserGroup group = existingFilter.getGroup();
				if (group != null && group.getGroupName().equals(groupName)) {
					existingFilter.setGeofences(new HashSet<>(existingChosenFences));
					existingFilter.setRaidGeofences(new HashSet<>(existingChosenFences));
					existingFilter.setIvGeofences(new HashSet<>(existingChosenFences));
					filterService.updateOrInsertFilter(existingFilter);
					alreadyAdded = true;
				}
			}

			if (!alreadyAdded) {
				Filter newFilter = new Filter();
				newFilter.setFilterType(FilterType.GROUP);
				newFilter.setGeofences(new HashSet<>(existingChosenFences));
				newFilter.setRaidGeofences(new HashSet<>(existingChosenFences));
				newFilter.setIvGeofences(new HashSet<>(existingChosenFences));
				for (UserGroup existingGroup : userGroups) {
					if (existingGroup.getGroupName().equals(groupName)) {
						newFilter.setGroup(existingGroup);
						Filter filter = filterService.updateOrInsertFilter(newFilter);
						existingGroup.setGroupFilter(filter);
						userGroupDAO.save(existingGroup);
						break;
					}
				}
			}
		}
	}

	private List<Geofence> getExistingChosenFences(List<Geofence> existingFences, List<String> newFences) {
		return existingFences.stream().filter(x -> newFences.contains(x.getGeofenceName()))
				.collect(Collectors.toList());
	}

	private void updateUserGroups(Map<String, List<Long>> groupIds) {
		List<UserGroup> userGroups = new ArrayList<>();
		userGroupDAO.findAll().forEach(x -> userGroups.add(x));
		for (Entry<String, List<Long>> entry : groupIds.entrySet()) {
			boolean changed = false;
			String key = entry.getKey();
			List<Long> value = entry.getValue();
			if (value == null || value.size() != 1) {
				logger.debug("No value or too many values for " + key + "   ---->  " + value);
			} else {
				for (UserGroup userGroup : userGroups) {
					if (userGroup.getGroupName().equals(key)) {
						userGroup.setChatId(value.get(0));
						userGroupDAO.save(userGroup);
						changed = true;
						break;
					} else if (userGroup.getChatId().equals(value.get(0))) {
						userGroup.setGroupName(key);
						userGroupDAO.save(userGroup);
						changed = true;
						break;
					}
				}
				if (!changed) {
					UserGroup newEntity = new UserGroup();
					newEntity.setGroupName(key);
					newEntity.setChatId(value.get(0));
					userGroupDAO.save(newEntity);
				}
			}
		}
	}

	private List<Geofence> getGeofences() throws IOException {
		List<Geofence> result = new ArrayList<>();
		Map<String, List<Double>> resultContent = new HashMap<>();
		parseGroupConfigFile("geofences.txt", x -> x).entrySet().stream().forEach(
				geofenceEntry -> resultContent.put(geofenceEntry.getKey(), splitCoordinates(geofenceEntry.getValue())));
		for (Entry<String, List<Double>> geofence2 : resultContent.entrySet()) {
			Geofence fence = new Geofence();
			fence.setGeofenceName(geofence2.getKey());
			fence.setPolygon(geofence2.getValue());
			result.add(fence);
		}
		return result;
	}

	private List<Double> splitCoordinates(List<String> coordinates) {
		List<Double> allCoordsFlat = new ArrayList<>();
		for (String coordinate : coordinates) {
			String[] splittedCoordinates = coordinate.split(",");
			if (splittedCoordinates.length == 2) {
				Double latitude = Double.valueOf(splittedCoordinates[0]);
				Double longitude = Double.valueOf(splittedCoordinates[1]);
				allCoordsFlat.add(latitude);
				allCoordsFlat.add(longitude);
			}
		}
		return allCoordsFlat;
	}

	private List<Filter> getFiltersWithGroupPokemonFromFile() throws IOException {
		List<Filter> result = new ArrayList<>();
		String fileName = "groupmonsters.txt";
		Map<String, List<Integer>> groupPokemon = parseGroupConfigFile(fileName, x -> Integer.valueOf(x));
		for (Entry<String, List<Integer>> groupMons : groupPokemon.entrySet()) {
			Filter filter = new Filter();
			filter.setFilterType(FilterType.GROUP);
			filter.setPokemons(groupMons.getValue());
			UserGroup group = new UserGroup();
			group.setGroupName(groupMons.getKey());
			group.setGroupFilter(filter);
			filter.setGroup(group);
			result.add(filter);
		}
		return result;
	}

	private List<Filter> getFiltersWithGroupIVFromFile() throws IOException {
		String fileName = "groupiv.txt";
		List<Filter> filters = new ArrayList<>();
		Map<String, List<Double>> parsedIVs = parseGroupConfigFile(fileName, x -> Double.valueOf(x));
		for (Entry<String, List<Double>> ivGroup : parsedIVs.entrySet()) {
			Filter filter = new Filter();
			filter.setFilterType(FilterType.GROUP);
			if (ivGroup.getValue() != null) {
				Double minIV = ivGroup.getValue().get(0);
				filter.setMinIV(minIV);
			}
			UserGroup group = new UserGroup();
			group.setGroupName(ivGroup.getKey());
			group.setGroupFilter(filter);
			filter.setGroup(group);
			filters.add(filter);

		}
		return filters;
	}

	private Map<String, List<String>> parseGroupIdFile(String fileName) throws IOException {
		Map<String, List<String>> result = new HashMap<>();
		BufferedReader bufferedReader = getBufferedFileReader(fileName);
		if (bufferedReader == null) {
			return result;
		}
		String readLine = bufferedReader.readLine();
		String key = null;
		List<String> content = new ArrayList<>();
		while (readLine != null) {
			if (readLine.startsWith("[") && readLine.endsWith("]")) {
				if (key != null) {
					result.put(key, content);
					content = new ArrayList<>();
				}
				key = readLine.substring(1, readLine.length() - 1);
			} else if (readLine.isEmpty()) {
				System.out.println("Skip empty line");
			} else {
				content.add(readLine.trim());
			}
			readLine = bufferedReader.readLine();
			if (null == readLine && key != null) {
				result.put(key, content);
			}
		}
		return result;
	}

	private <T> Map<String, List<T>> parseGroupConfigFile(String fileName, Function<String, T> function)
			throws IOException {
		Map<String, List<T>> result = new HashMap<>();
		BufferedReader bufferedReader = getBufferedFileReader(fileName);
		if (bufferedReader == null) {
			return result;
		}
		String readLine = bufferedReader.readLine().trim();
		List<String> groupNames = new ArrayList<>();
		List<T> valuePerFilter = new ArrayList<>();
		List<List<T>> allValues = new ArrayList<>();
		while (readLine != null) {
			if (readLine.startsWith("[") && readLine.endsWith("]")) {
				if (!valuePerFilter.isEmpty()) {
					allValues.add(valuePerFilter);
				}
				groupNames.add(readLine.substring(1, readLine.length() - 1));
				valuePerFilter = new ArrayList<>();
			} else if (readLine.isEmpty()) {
				// logger.debug("Empty line in " + fileName);
			} else {
				String content = readLine.trim();
				T value = function.apply(content);
				valuePerFilter.add(value);
			}
			readLine = bufferedReader.readLine();
			if (readLine == null) {
				allValues.add(valuePerFilter);
			}
		}
		Function<Integer, Map<String, List<T>>> resultPutter = createMapPutter(result, groupNames, allValues);
		for (int i = 0; i < groupNames.size(); i++) {
			resultPutter.apply(i);
		}
		return result;
	}

	private <T> Function<Integer, Map<String, List<T>>> createMapPutter(Map<String, List<T>> result,
			List<String> groupNames, List<List<T>> allIv) {
		return x -> {
			List<T> list = result.put(groupNames.get(x), allIv.get(x));
			if (list == null) {
				logger.debug("no values found for group " + groupNames.get(x));
			}
			return result;
		};
	}

	private List<Filter> getFiltersWithGroupRaidPokemonFromFile() throws IOException {
		List<Filter> result = new ArrayList<>();
		String fileName = "groupraidmonsters.txt";
		Map<String, List<Integer>> groups = parseGroupConfigFile(fileName, x -> Integer.valueOf(x.trim()));
		for (Entry<String, List<Integer>> groupRaidPokemon : groups.entrySet()) {
			Filter filter = new Filter();
			filter.setFilterType(FilterType.GROUP);
			filter.setRaidPokemon(groupRaidPokemon.getValue());
			UserGroup group = new UserGroup();
			group.setGroupName(groupRaidPokemon.getKey());
			group.setGroupFilter(filter);
			filter.setGroup(group);
			result.add(filter);
		}
		return result;
	}

	private BufferedReader getBufferedFileReader(String fileName) {
		BufferedReader bufferedReader = null;

		// ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		InputStream fileInputStream = null;

		// Remove prefix "file:" from property
		fileName = System.getProperty("ext.properties.dir").substring(5) + File.separator + fileName;
		try {
			fileInputStream = new FileInputStream(fileName);
		} catch (FileNotFoundException e1) {
			logger.warn("No resource could be found for " + fileName + " ...");
		}
		// classLoader.getResourceAsStream(fileName);
		if (fileInputStream != null) {
			InputStreamReader fileStreamReader = new InputStreamReader(fileInputStream);
			bufferedReader = new BufferedReader(fileStreamReader);
		} else {
			try {
				bufferedReader = new BufferedReader(new FileReader(fileName));
			} catch (FileNotFoundException e) {
				logger.warn("No resource could be found for " + fileName + " ...");
			}
		}
		if (bufferedReader != null) {
			logger.info("Get filereader for " + fileName + " ...");
		}
		return bufferedReader;
	}

}