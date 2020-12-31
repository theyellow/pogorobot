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

package pogorobot.events.webservice;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pogorobot.entities.Gym;
import pogorobot.events.EventMessage;

public class WebserviceGym implements EventMessage<Gym> {

	private Logger logger = LoggerFactory.getLogger(WebserviceGym.class);

	private String gym_id;
	private Double latitude;
	private Double longitude;
	private Boolean enabled;
	private Long team_id;
	private String move_1;
	private String move_2;
	private String name;
	private String gym_name;
	private String description;
	private String url;
	private Long occupied_since;
	private Long last_modified;
	private Long guard_pokemon_id;
	private Long total_cp;
	private Long slots_available;
	private Double lowest_pokemon_motivation;
	private Long raid_active_until;
	private String ex_raid_eligible;
	private String sponsor_id;

	public final String getGym_id() {
		return gym_id;
	}

	public final void setGym_id(String gym_id) {
		this.gym_id = gym_id;
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

	public final Boolean getEnabled() {
		return enabled;
	}

	public final void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public final Long getTeam_id() {
		return team_id;
	}

	public final void setTeam_id(Long team_id) {
		this.team_id = team_id;
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

	public final Long getOccupied_since() {
		return occupied_since;
	}

	public final void setOccupied_since(Long occupied_since) {
		this.occupied_since = occupied_since;
	}

	public final Long getLast_modified() {
		return last_modified;
	}

	public final void setLast_modified(Long last_modified) {
		this.last_modified = last_modified;
	}

	public final Long getGuard_pokemon_id() {
		return guard_pokemon_id;
	}

	public final void setGuard_pokemon_id(Long guard_pokemon_id) {
		this.guard_pokemon_id = guard_pokemon_id;
	}

	public String getMove_1() {
		return move_1;
	}

	public void setMove_1(String move_1) {
		this.move_1 = move_1;
	}

	public String getMove_2() {
		return move_2;
	}

	public void setMove_2(String move_2) {
		this.move_2 = move_2;
	}

	public final Long getTotal_cp() {
		return total_cp;
	}

	public final void setTotal_cp(Long total_cp) {
		this.total_cp = total_cp;
	}

	public final Long getSlots_available() {
		return slots_available;
	}

	public final void setSlots_available(Long slots_available) {
		this.slots_available = slots_available;
	}

	public final Double getLowest_pokemon_motivation() {
		return lowest_pokemon_motivation;
	}

	public final void setLowest_pokemon_motivation(Double lowest_pokemon_motivation) {
		this.lowest_pokemon_motivation = lowest_pokemon_motivation;
	}

	public final Long getRaid_active_until() {
		return raid_active_until;
	}

	public final void setRaid_active_until(Long raid_active_until) {
		this.raid_active_until = raid_active_until;
	}

	public String getEx_raid_eligible() {
		return ex_raid_eligible;
	}

	public void setEx_raid_eligible(String ex_raid_eligible) {
		this.ex_raid_eligible = ex_raid_eligible;
	}

	public final String getSponsor_id() {
		return sponsor_id;
	}

	public final void setSponsor_id(String sponsor_id) {
		this.sponsor_id = sponsor_id;
	}

	public String getGym_name() {
		return gym_name;
	}

	public void setGym_name(String gym_name) {
		this.gym_name = gym_name;
	}

	@Override
	public Gym transformToEntity() {
		Gym gym = new Gym();
		gym.setDescription(description);
		gym.setEnabled(enabled);
		gym.setGymId(gym_id);
		gym.setPokestop(gym_id == null || gym_id.isEmpty());
		gym.setLastModified(last_modified);
		gym.setLatitude(latitude);
		gym.setLongitude(longitude);
		gym.setSlotsAvailable(slots_available);
		gym.setName(name);
		if (StringUtils.isNotEmpty(gym_name)) {
			gym.setName(gym_name);
		}
		gym.setOccupiedSince(occupied_since);
		gym.setRaidActiveUntil(raid_active_until);
		gym.setUrl(url);
		gym.setTeamId(team_id);
		boolean exraidEglibleExists = StringUtils.isNotEmpty(sponsor_id) || StringUtils.isNotEmpty(ex_raid_eligible);
		if (exraidEglibleExists) {
			if ("true".equals(ex_raid_eligible) || "true".equals(sponsor_id)) {
				gym.setExraidEglible(true);
			} else {
				gym.setExraidEglible(false);
			}
			logger.info("Found something in exraid-tags for '{}': 'sponsor_id={}'  | 'ex_raid_eligible={}'", name,
					sponsor_id,
					ex_raid_eligible);
		}
		return gym;
	}

	@Override
	public String toString() {
		return "RocketmapGym [" + (logger != null ? "logger=" + logger + ", " : "")
				+ (gym_id != null ? "gym_id=" + gym_id + ", " : "")
				+ (latitude != null ? "latitude=" + latitude + ", " : "")
				+ (longitude != null ? "longitude=" + longitude + ", " : "")
				+ (enabled != null ? "enabled=" + enabled + ", " : "")
				+ (team_id != null ? "team_id=" + team_id + ", " : "")
				+ (move_1 != null ? "move_1=" + move_1 + ", " : "") + (move_2 != null ? "move_2=" + move_2 + ", " : "")
				+ (name != null ? "name=" + name + ", " : "")
				+ (description != null ? "description=" + description + ", " : "")
				+ (url != null ? "url=" + url + ", " : "")
				+ (occupied_since != null ? "occupied_since=" + occupied_since + ", " : "")
				+ (last_modified != null ? "last_modified=" + last_modified + ", " : "")
				+ (guard_pokemon_id != null ? "guard_pokemon_id=" + guard_pokemon_id + ", " : "")
				+ (total_cp != null ? "total_cp=" + total_cp + ", " : "")
				+ (slots_available != null ? "slots_available=" + slots_available + ", " : "")
				+ (lowest_pokemon_motivation != null ? "lowest_pokemon_motivation=" + lowest_pokemon_motivation + ", "
						: "")
				+ (raid_active_until != null ? "raid_active_until=" + raid_active_until + ", " : "")
				+ (ex_raid_eligible != null ? "ex_raid_eligible=" + ex_raid_eligible + ", " : "")
				+ (sponsor_id != null ? "sponsor_id=" + sponsor_id : "") + "]";
	}

}
