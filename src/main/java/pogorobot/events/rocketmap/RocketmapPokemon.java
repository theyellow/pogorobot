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

import pogorobot.entities.PokemonWithSpawnpoint;
import pogorobot.events.EventMessage;

public class RocketmapPokemon implements EventMessage<PokemonWithSpawnpoint> {

	private String spawnpoint_id;
	private String encounter_id;
	private Long pokemon_id;
	private Double latitude;
	private Double longitude;
	private Long disappear_time;
	private Long time_until_hidden_ms;
	private Long last_modified_time;
	private Long seconds_until_despawn;
	private Long spawn_start;
	private Long spawn_end;
	private Long gender;
	private String form;
	private String individual_attack;
	private String individual_defense;
	private String individual_stamina;
	private String cp;
	private Double cp_multiplier;
	private String move_1;
	private String move_2;
	private String pokemon_level;
	private Double weight;
	private Double height;
	private String costume_id;
	private String costume;
	private Long player_level;
	private Boolean verified;
	private Integer weather_boosted_condition;

	public final String getSpawnpoint_id() {
		return spawnpoint_id;
	}

	public final void setSpawnpoint_id(String spawnpoint_id) {
		this.spawnpoint_id = spawnpoint_id;
	}

	public final String getEncounter_id() {
		return encounter_id;
	}

	public final void setEncounter_id(String encounter_id) {
		this.encounter_id = encounter_id;
	}

	public final Long getPokemon_id() {
		return pokemon_id;
	}

	public final void setPokemon_id(Long pokemon_id) {
		this.pokemon_id = pokemon_id;
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

	public final Long getDisappear_time() {
		return disappear_time;
	}

	public final void setDisappear_time(Long disappear_time) {
		this.disappear_time = disappear_time;
	}

	public final Long getTime_until_hidden_ms() {
		return time_until_hidden_ms;
	}

	public final void setTime_until_hidden_ms(Long time_until_hidden_ms) {
		this.time_until_hidden_ms = time_until_hidden_ms;
	}

	public final Long getLast_modified_time() {
		return last_modified_time;
	}

	public final void setLast_modified_time(Long last_modified_time) {
		this.last_modified_time = last_modified_time;
	}

	public final Long getSeconds_until_despawn() {
		return seconds_until_despawn;
	}

	public final void setSeconds_until_despawn(Long seconds_until_despawn) {
		this.seconds_until_despawn = seconds_until_despawn;
	}

	public final Long getSpawn_start() {
		return spawn_start;
	}

	public final void setSpawn_start(Long spawn_start) {
		this.spawn_start = spawn_start;
	}

	public final Long getSpawn_end() {
		return spawn_end;
	}

	public final void setSpawn_end(Long spawn_end) {
		this.spawn_end = spawn_end;
	}

	public final Long getGender() {
		return gender;
	}

	public final void setGender(Long gender) {
		this.gender = gender;
	}

	public final String getForm() {
		return form;
	}

	public final void setForm(String form) {
		this.form = form;
	}

	public final String getIndividual_attack() {
		return individual_attack;
	}

	public final void setIndividual_attack(String individual_attack) {
		this.individual_attack = individual_attack;
	}

	public final String getIndividual_defense() {
		return individual_defense;
	}

	public final void setIndividual_defense(String individual_defense) {
		this.individual_defense = individual_defense;
	}

	public final String getIndividual_stamina() {
		return individual_stamina;
	}

	public final void setIndividual_stamina(String individual_stamina) {
		this.individual_stamina = individual_stamina;
	}

	public final Double getCp_multiplier() {
		return cp_multiplier;
	}

	public final void setCp_multiplier(Double cp_multiplier) {
		this.cp_multiplier = cp_multiplier;
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

	public final Double getWeight() {
		return weight;
	}

	public final void setWeight(Double weight) {
		this.weight = weight;
	}

	public final Double getHeight() {
		return height;
	}

	public final void setHeight(Double height) {
		this.height = height;
	}

	public final Long getPlayer_level() {
		return player_level;
	}

	public final void setPlayer_level(Long player_level) {
		this.player_level = player_level;
	}

	public final Boolean getVerified() {
		return verified;
	}

	public final void setVerified(Boolean verified) {
		this.verified = verified;
	}

	@Override
	public PokemonWithSpawnpoint transformToEntity() {
		PokemonWithSpawnpoint pokemon = new PokemonWithSpawnpoint();
		pokemon.setSpawnpointId(spawnpoint_id);
		pokemon.setPokemonId(pokemon_id);

		// Hack: misuse of field playerLevel with pokemon level
		// pokemon.setPlayerLevel(player_level);
		pokemon.setPlayerLevel(extractLongFromFloatString(pokemon_level));
		pokemon.setLatitude(latitude);
		pokemon.setLongitude(longitude);

		// Help a little bit to fill more fields...
		pokemon.setSecondsUntilDespawn(seconds_until_despawn != null ? seconds_until_despawn
				: disappear_time != null ? (disappear_time - System.currentTimeMillis() / 1000) : 0);
		pokemon.setSpawnStart(spawn_start != null ? spawn_start : System.currentTimeMillis() / 1000);
		pokemon.setSpawnEnd(spawn_end != null ? spawn_end : pokemon.getSpawnStart() + pokemon.getSecondsUntilDespawn());
		pokemon.setTimeUntilHidden_ms(
				time_until_hidden_ms != null ? time_until_hidden_ms : pokemon.getSecondsUntilDespawn() * 1000);
		pokemon.setDisappearTime(disappear_time != null ? disappear_time
				: System.currentTimeMillis() / 1000 + pokemon.getSecondsUntilDespawn());
		pokemon.setVerified(verified);
		pokemon.setCpMultiplier(cp_multiplier);
		pokemon.setEncounterId(encounter_id);
		pokemon.setForm(form);
		pokemon.setGender(gender);
		pokemon.setHeight(height);
		pokemon.setWeight(weight);
		pokemon.setCp(cp);
		pokemon.setCostumeId(costume_id);
		pokemon.setWeight(weight);
		pokemon.setIndividualAttack(individual_attack);
		pokemon.setIndividualDefense(individual_defense);
		pokemon.setIndividualStamina(individual_stamina);
		pokemon.setLastModified(last_modified_time);
		pokemon.setMove1(move_1);
		pokemon.setMove2(move_2);
		pokemon.setWeatherBoosted(weather_boosted_condition);
		return pokemon;
	}

	private Long extractLongFromFloatString(String floatString) {
		long returnValue = -1;
		if (floatString != null) {
			returnValue = Math.round(Float.valueOf(floatString)); 
		}
		return returnValue;
	}

	public final String getCp() {
		return cp;
	}

	public final void setCp(String cp) {
		this.cp = cp;
	}

	public final String getCostume_id() {
		return costume_id;
	}

	public final void setCostume_id(String costume_id) {
		this.costume_id = costume_id;
	}

	public final String getCostume() {
		return costume;
	}

	public final void setCostume(String costume) {
		this.costume = costume;
	}

	public Integer getWeather_boosted_condition() {
		return weather_boosted_condition;
	}

	public void setWeather_boosted_condition(Integer weather_boosted_condition) {
		this.weather_boosted_condition = weather_boosted_condition;
	}

	public String getPokemon_level() {
		return pokemon_level;
	}

	public void setPokemon_level(String pokemon_level) {
		this.pokemon_level = pokemon_level;
	}

	@Override
	public String toString() {
		return "RocketmapPokemon [" + (spawnpoint_id != null ? "spawnpoint_id=" + spawnpoint_id + ", " : "")
				+ (encounter_id != null ? "encounter_id=" + encounter_id + ", " : "")
				+ (pokemon_id != null ? "pokemon_id=" + pokemon_id + ", " : "")
				+ (latitude != null ? "latitude=" + latitude + ", " : "")
				+ (longitude != null ? "longitude=" + longitude + ", " : "")
				+ (disappear_time != null ? "disappear_time=" + disappear_time + ", " : "")
				+ (time_until_hidden_ms != null ? "time_until_hidden_ms=" + time_until_hidden_ms + ", " : "")
				+ (last_modified_time != null ? "last_modified_time=" + last_modified_time + ", " : "")
				+ (seconds_until_despawn != null ? "seconds_until_despawn=" + seconds_until_despawn + ", " : "")
				+ (spawn_start != null ? "spawn_start=" + spawn_start + ", " : "")
				+ (spawn_end != null ? "spawn_end=" + spawn_end + ", " : "")
				+ (gender != null ? "gender=" + gender + ", " : "") + (form != null ? "form=" + form + ", " : "")
				+ (individual_attack != null ? "individual_attack=" + individual_attack + ", " : "")
				+ (individual_defense != null ? "individual_defense=" + individual_defense + ", " : "")
				+ (individual_stamina != null ? "individual_stamina=" + individual_stamina + ", " : "")
				+ (cp != null ? "cp=" + cp + ", " : "")
				+ (cp_multiplier != null ? "cp_multiplier=" + cp_multiplier + ", " : "")
				+ (move_1 != null ? "move_1=" + move_1 + ", " : "") + (move_2 != null ? "move_2=" + move_2 + ", " : "")
				+ (pokemon_level != null ? "pokemon_level=" + pokemon_level + ", " : "")
				+ (weight != null ? "weight=" + weight + ", " : "") + (height != null ? "height=" + height + ", " : "")
				+ (costume_id != null ? "costume_id=" + costume_id + ", " : "")
				+ (costume != null ? "costume=" + costume + ", " : "")
				+ (player_level != null ? "player_level=" + player_level + ", " : "")
				+ (verified != null ? "verified=" + verified + ", " : "")
				+ (weather_boosted_condition != null ? "weather_boosted_condition=" + weather_boosted_condition : "")
				+ "]";
	}

}
