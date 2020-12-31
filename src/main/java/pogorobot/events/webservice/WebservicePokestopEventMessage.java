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

import pogorobot.entities.Gym;
import pogorobot.events.EventMessage;

public class WebservicePokestopEventMessage implements EventMessage<Gym> {

	private String gym_id;
	private Double latitude;
	private Double longitude;
	private Boolean enabled;
	private Long team_id;
	private String name;
	private String description;
	private String url;
	private Long occupied_since;
	private Long last_modified;
	private Long guard_pokemon_id;
	private Long total_cp;
	private Long slots_available;
	private Double lowest_pokemon_motivation;
	private Long raid_active_until;

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

	@Override
	public Gym transformToEntity() {
		Gym gym = new Gym();
		gym.setDescription(description);
		gym.setEnabled(enabled);
		gym.setGymId(gym_id);
		gym.setPokestop(true);
		gym.setLastModified(last_modified);
		gym.setLatitude(latitude);
		gym.setLongitude(longitude);
		gym.setSlotsAvailable(slots_available);
		gym.setName(name);
		gym.setOccupiedSince(occupied_since);
		gym.setRaidActiveUntil(raid_active_until);
		gym.setUrl(url);
		gym.setTeamId(team_id);
		return gym;
	}

	@Override
	public String toString() {
		return "RocketmapGym [" + (gym_id != null ? "gym_id=" + gym_id + ", " : "")
				+ (latitude != null ? "latitude=" + latitude + ", " : "")
				+ (longitude != null ? "longitude=" + longitude + ", " : "")
				+ (enabled != null ? "enabled=" + enabled + ", " : "")
				+ (team_id != null ? "team_id=" + team_id + ", " : "") + (name != null ? "name=" + name + ", " : "")
				+ (description != null ? "description=" + description + ", " : "")
				+ (url != null ? "url=" + url + ", " : "")
				+ (occupied_since != null ? "occupied_since=" + occupied_since + ", " : "")
				+ (last_modified != null ? "last_modified=" + last_modified + ", " : "")
				+ (guard_pokemon_id != null ? "guard_pokemon_id=" + guard_pokemon_id + ", " : "")
				+ (total_cp != null ? "total_cp=" + total_cp + ", " : "")
				+ (slots_available != null ? "slots_available=" + slots_available + ", " : "")
				+ (lowest_pokemon_motivation != null ? "lowest_pokemon_motivation=" + lowest_pokemon_motivation + ", "
						: "")
				+ (raid_active_until != null ? "raid_active_until=" + raid_active_until : "") + "]";
	}


}
