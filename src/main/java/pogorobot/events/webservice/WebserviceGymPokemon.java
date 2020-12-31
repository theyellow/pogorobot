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

import pogorobot.entities.GymPokemon;
import pogorobot.events.EventMessage;

public class WebserviceGymPokemon implements EventMessage<GymPokemon> {

	private String trainer_name;
	private Long trainer_level;
	private Long pokemon_uid;
	private Long pokemon_id;
	private Long cp;
	private Long cp_decayed;
	private Double cp_multiplier;
	private Double additional_cp_multiplier;
	private Long stamina_max;
	private Long stamina;
	private Long iv_attack;
	private Long iv_defense;
	private Long iv_stamina;
	private String move_1;
	private String move_2;
	private Double weight;
	private Double height;
	private Boolean verified;
	private Long gender;
	private String form;
	private String costume;
	private Long num_upgrades;
	private Long deployment_time;

	public final String getTrainer_name() {
		return trainer_name;
	}

	public final void setTrainer_name(String trainer_name) {
		this.trainer_name = trainer_name;
	}

	public final Long getTrainer_level() {
		return trainer_level;
	}

	public final void setTrainer_level(Long trainer_level) {
		this.trainer_level = trainer_level;
	}

	public final Long getPokemon_uid() {
		return pokemon_uid;
	}

	public final void setPokemon_uid(Long pokemon_uid) {
		this.pokemon_uid = pokemon_uid;
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

	public final Long getCp_decayed() {
		return cp_decayed;
	}

	public final void setCp_decayed(Long cp_decayed) {
		this.cp_decayed = cp_decayed;
	}

	public final Double getCp_multiplier() {
		return cp_multiplier;
	}

	public final void setCp_multiplier(Double cp_multiplier) {
		this.cp_multiplier = cp_multiplier;
	}

	public final Double getAdditional_cp_multiplier() {
		return additional_cp_multiplier;
	}

	public final void setAdditional_cp_multiplier(Double additional_cp_multiplier) {
		this.additional_cp_multiplier = additional_cp_multiplier;
	}

	public final Long getStamina_max() {
		return stamina_max;
	}

	public final void setStamina_max(Long stamina_max) {
		this.stamina_max = stamina_max;
	}

	public final Long getStamina() {
		return stamina;
	}

	public final void setStamina(Long stamina) {
		this.stamina = stamina;
	}

	public final Long getIv_attack() {
		return iv_attack;
	}

	public final void setIv_attack(Long iv_attack) {
		this.iv_attack = iv_attack;
	}

	public final Long getIv_defense() {
		return iv_defense;
	}

	public final void setIv_defense(Long iv_defense) {
		this.iv_defense = iv_defense;
	}

	public final Long getIv_stamina() {
		return iv_stamina;
	}

	public final void setIv_stamina(Long iv_stamina) {
		this.iv_stamina = iv_stamina;
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

	public final Boolean getVerified() {
		return verified;
	}

	public final void setVerified(Boolean verified) {
		this.verified = verified;
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

	public final Long getNum_upgrades() {
		return num_upgrades;
	}

	public final void setNum_upgrades(Long num_upgrades) {
		this.num_upgrades = num_upgrades;
	}

	public final Long getDeployment_time() {
		return deployment_time;
	}

	public final void setDeployment_time(Long deployment_time) {
		this.deployment_time = deployment_time;
	}

	@Override
	public GymPokemon transformToEntity() {
		GymPokemon pokemon = new GymPokemon();
		pokemon.setPokemonId(pokemon_id);
		pokemon.setTrainerLevel(trainer_level);
		pokemon.setTrainerName(trainer_name);
		return pokemon;
	}

	public final String getCostume() {
		return costume;
	}

	public final void setCostume(String costume) {
		this.costume = costume;
	}

	@Override
	public String toString() {
		return "WebserviceGymPokemon [" + (trainer_name != null ? "trainer_name=" + trainer_name + ", " : "")
				+ (trainer_level != null ? "trainer_level=" + trainer_level + ", " : "")
				+ (pokemon_uid != null ? "pokemon_uid=" + pokemon_uid + ", " : "")
				+ (pokemon_id != null ? "pokemon_id=" + pokemon_id + ", " : "") + (cp != null ? "cp=" + cp + ", " : "")
				+ (cp_decayed != null ? "cp_decayed=" + cp_decayed + ", " : "")
				+ (cp_multiplier != null ? "cp_multiplier=" + cp_multiplier + ", " : "")
				+ (additional_cp_multiplier != null ? "additional_cp_multiplier=" + additional_cp_multiplier + ", "
						: "")
				+ (stamina_max != null ? "stamina_max=" + stamina_max + ", " : "")
				+ (stamina != null ? "stamina=" + stamina + ", " : "")
				+ (iv_attack != null ? "iv_attack=" + iv_attack + ", " : "")
				+ (iv_defense != null ? "iv_defense=" + iv_defense + ", " : "")
				+ (iv_stamina != null ? "iv_stamina=" + iv_stamina + ", " : "")
				+ (move_1 != null ? "move_1=" + move_1 + ", " : "") + (move_2 != null ? "move_2=" + move_2 + ", " : "")
				+ (weight != null ? "weight=" + weight + ", " : "") + (height != null ? "height=" + height + ", " : "")
				+ (verified != null ? "verified=" + verified + ", " : "")
				+ (gender != null ? "gender=" + gender + ", " : "") + (form != null ? "form=" + form + ", " : "")
				+ (costume != null ? "costume=" + costume + ", " : "")
				+ (num_upgrades != null ? "num_upgrades=" + num_upgrades + ", " : "")
				+ (deployment_time != null ? "deployment_time=" + deployment_time : "") + "]";
	}

}
