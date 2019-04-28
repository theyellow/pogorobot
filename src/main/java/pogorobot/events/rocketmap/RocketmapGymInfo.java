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

package pogorobot.events.rocketmap;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pogorobot.entities.Gym;
import pogorobot.entities.GymPokemon;
import pogorobot.events.EventMessage;

public class RocketmapGymInfo implements EventMessage<Gym> {

	private Logger logger = LoggerFactory.getLogger(RocketmapGymInfo.class);

	private String id;
	private String name;
	private String description;
	private String url;
	private Boolean park;
	private Boolean ex_raid_eligible;
	private Double latitude;
	private Double longitude;
	private Long team;
	private List<RocketmapGymPokemon> pokemon;

	public final String getId() {
		return id;
	}

	public final void setId(String id) {
		this.id = id;
	}

	public Boolean getPark() {
		return park;
	}

	public void setPark(Boolean park) {
		this.park = park;
	}

	public Boolean getEx_raid_eligible() {
		return ex_raid_eligible;
	}

	public void setEx_raid_eligible(Boolean ex_raid_eligible) {
		this.ex_raid_eligible = ex_raid_eligible;
	}

	public final String getName() {
		return name;
	}

	public final void setName(String name) {
		this.name = name;
	}

	public final String getDescription() {
		return description;
	}

	public final void setDescription(String description) {
		this.description = description;
	}

	public final String getUrl() {
		return url;
	}

	public final void setUrl(String url) {
		this.url = url;
	}

	public final Double getLatitude() {
		return latitude;
	}

	public final void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	public final Double getLongitude() {
		return longitude;
	}

	public final void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	public final Long getTeam() {
		return team;
	}

	public final void setTeam(Long team) {
		this.team = team;
	}

	public final List<RocketmapGymPokemon> getPokemon() {
		return pokemon;
	}

	public final void setPokemon(List<RocketmapGymPokemon> pokemon) {
		this.pokemon = pokemon;
	}

	@Override
	public Gym transformToEntity() {
		Gym gym = new Gym();
		gym.setGymId(id);
		gym.setLatitude(latitude);
		gym.setLongitude(longitude);
		gym.setName(name);
		gym.setTeamId(team);

		Boolean exraidEglible;
		if (park != null) {
			exraidEglible = park;
		}
		exraidEglible = ex_raid_eligible != null ? ex_raid_eligible : park;

		if (exraidEglible != null) {
			if (ex_raid_eligible) {
				logger.debug("exraid_eglible found in gym-info: {}", ex_raid_eligible);
			}
			gym.setExraidEglible(exraidEglible);
		}
		if (pokemon != null && pokemon.size() > 0) {
			List<GymPokemon> pokemonList = pokemon.stream().map((x -> x.transformToEntity()))
					.collect(Collectors.toList());
			gym.setPokemon(pokemonList);
		} else {
			gym.setPokemon(new ArrayList<>());
		}
		gym.setUrl(url);
		return gym;
	}

	@Override
	public String toString() {
		return "RocketmapGymInfo [" + (logger != null ? "logger=" + logger + ", " : "")
				+ (id != null ? "id=" + id + ", " : "") + (name != null ? "name=" + name + ", " : "")
				+ (description != null ? "description=" + description + ", " : "")
				+ (url != null ? "url=" + url + ", " : "") + (park != null ? "park=" + park + ", " : "")
				+ (ex_raid_eligible != null ? "ex_raid_eligible=" + ex_raid_eligible + ", " : "")
				+ (latitude != null ? "latitude=" + latitude + ", " : "")
				+ (longitude != null ? "longitude=" + longitude + ", " : "")
				+ (team != null ? "team=" + team + ", " : "") + (pokemon != null ? "pokemon=" + pokemon : "") + "]";
	}




}
