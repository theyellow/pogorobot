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

import pogorobot.entities.Raid;
import pogorobot.entities.RaidWithGym;
import pogorobot.events.EventMessage;

public class RocketmapRaid implements EventMessage<Raid> {

	private String gym_id;
	private Double latitude;
	private Double longitude;
	private Long spawn;
	private Long start;
	private Long end;
	private Long level;
	private Long pokemon_id;
	private Long cp;
	private String name;
	private String url;
	private String move_1;
	private String move_2;
	private String ex_raid_eligible;
	private String sponsor_id;

	public String getEx_raid_eligible() {
		return ex_raid_eligible;
	}

	public void setEx_raid_eligible(String ex_raid_eligible) {
		this.ex_raid_eligible = ex_raid_eligible;
	}

	public String getSponsor_id() {
		return sponsor_id;
	}

	public void setSponsor_id(String sponsor_id) {
		this.sponsor_id = sponsor_id;
	}

	public final String getUrl() {
		return url;
	}

	public final void setUrl(String url) {
		this.url = url;
	}

	public final String getName() {
		return name;
	}

	public final void setName(String name) {
		this.name = name;
	}

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

	public final Long getSpawn() {
		return spawn;
	}

	public final void setSpawn(Long spawn) {
		this.spawn = spawn;
	}

	public final Long getStart() {
		return start;
	}

	public final void setStart(Long start) {
		this.start = start;
	}

	public final Long getEnd() {
		return end;
	}

	public final void setEnd(Long end) {
		this.end = end;
	}

	public final Long getLevel() {
		return level;
	}

	public final void setLevel(Long level) {
		this.level = level;
	}

	public final Long getPokemon_id() {
		return pokemon_id;
	}

	public final void setPokemon_id(Long pokemon_id) {
		this.pokemon_id = pokemon_id;
	}

	public final Long getCp() {
		return cp;
	}

	public final void setCp(Long cp) {
		this.cp = cp;
	}

	public final String getMove_1() {
		return move_1;
	}

	public final void setMove_1(String move_1) {
		this.move_1 = move_1;
	}

	public final String getMove_2() {
		return move_2;
	}

	public final void setMove_2(String move_2) {
		this.move_2 = move_2;
	}

	@Override
	public Raid transformToEntity() {
		Raid raid = new RaidWithGym(gym_id);
		// raid.setCp(cp);
//		raid.setGymId(gym_id);
		raid.setRaidLevel(level);
		raid.setMove1(move_1);
		raid.setMove2(move_2);
		raid.setPokemonId(pokemon_id);
		raid.setSpawn(spawn);
		raid.setStart(start);
		raid.setEnd(end);
		raid.setExRaidEligible(ex_raid_eligible);
		raid.setSponsorId(sponsor_id);
		return raid;
	}

	@Override
	public String toString() {
		return "RocketmapRaid [" + (gym_id != null ? "gym_id=" + gym_id + ", " : "")
				+ (latitude != null ? "latitude=" + latitude + ", " : "")
				+ (longitude != null ? "longitude=" + longitude + ", " : "")
				+ (spawn != null ? "spawn=" + spawn + ", " : "") + (start != null ? "start=" + start + ", " : "")
				+ (end != null ? "end=" + end + ", " : "") + (level != null ? "level=" + level + ", " : "")
				+ (pokemon_id != null ? "pokemon_id=" + pokemon_id + ", " : "") + (cp != null ? "cp=" + cp + ", " : "")
				+ (name != null ? "name=" + name + ", " : "") + (url != null ? "url=" + url + ", " : "")
				+ (move_1 != null ? "move_1=" + move_1 + ", " : "") + (move_2 != null ? "move_2=" + move_2 + ", " : "")
				+ (ex_raid_eligible != null ? "ex_raid_eligible=" + ex_raid_eligible + ", " : "")
				+ (sponsor_id != null ? "sponsor_id=" + sponsor_id : "") + "]";
	}


}
