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

package pogorobot.events.telegrambot;

import pogorobot.entities.RaidAtGymEvent;
import pogorobot.entities.RaidWithGym;
import pogorobot.events.EventMessage;

public class IncomingManualRaid implements EventMessage<RaidAtGymEvent> {

	private String gymId;
	private Double latitude;
	private Double longitude;
	private Long spawn;
	private Long start;
	private Long end;
	private Long level;
	private Long pokemonId;
	private Long cp;
	private String move_1;
	private String move_2;

	public final String getGymId() {
		return gymId;
	}

	public final void setGymId(String gym_id) {
		this.gymId = gym_id;
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

	public final Long getPokemonId() {
		return pokemonId;
	}

	public final void setPokemonId(Long pokemonId) {
		this.pokemonId = pokemonId;
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
	public RaidAtGymEvent transformToEntity() {
		RaidWithGym raid = new RaidWithGym(gymId);
		raid.setPokemonId(pokemonId);
		// raid.setCp(cp);
		// raid.setGymId(gymId);
		// raid.setLatitude(latitude);
		// raid.setLongitude(longitude);
		raid.setRaidLevel(level);
		raid.setMove1(move_1);
		raid.setMove2(move_2);
		raid.setSpawn(spawn);
		raid.setStart(start);
		raid.setEnd(end);
		RaidAtGymEvent raidAtGym = new RaidAtGymEvent(raid);
		return raidAtGym;
	}

	@Override
	public String toString() {
		return "IncomingManualRaid [" + (gymId != null ? "gymId=" + gymId + ", " : "")
				+ (latitude != null ? "latitude=" + latitude + ", " : "")
				+ (longitude != null ? "longitude=" + longitude + ", " : "")
				+ (spawn != null ? "spawn=" + spawn + ", " : "") + (start != null ? "start=" + start + ", " : "")
				+ (end != null ? "end=" + end + ", " : "") + (level != null ? "level=" + level + ", " : "")
				+ (pokemonId != null ? "pokemonId=" + pokemonId + ", " : "") + (cp != null ? "cp=" + cp + ", " : "")
				+ (move_1 != null ? "move_1=" + move_1 + ", " : "") + (move_2 != null ? "move_2=" + move_2 : "") + "]";
	}

}
