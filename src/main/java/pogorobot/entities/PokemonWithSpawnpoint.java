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

package pogorobot.entities;

import javax.persistence.Column;
import javax.persistence.Entity;

import org.springframework.data.jpa.domain.AbstractPersistable;

@Entity
public class PokemonWithSpawnpoint extends AbstractPersistable<Long> {

	private static final long serialVersionUID = -4272840495799677635L;

	@Column(unique = true)
	private String spawnpointId;
	private String encounterId;
	private Long pokemonId;
	private Double latitude;
	private Double longitude;
	private Long disappearTime;
	private Long timeUntilHidden_ms;
	private Long lastModified;
	private Long secondsUntilDespawn;
	private Long spawnStart;
	private Long spawnEnd;
	private Long gender;
	private String form;
	private String cp;
	private String individualAttack;
	private String individualDefense;
	private String individualStamina;
	private Double cpMultiplier;
	private String move1;
	private String move2;
	private Double weight;
	private Double height;
	private Long playerLevel;
	private Boolean verified;
	private Integer weatherBoosted;
	private String costumeId;

	public PokemonWithSpawnpoint() {
		this(null);
	}

	public PokemonWithSpawnpoint(Long id) {
		this.setId(id);
	}

	public final String getSpawnpointId() {
		return spawnpointId;
	}

	public final void setSpawnpointId(String spawnpointId) {
		this.spawnpointId = spawnpointId;
	}

	public final Long getPokemonId() {
		return pokemonId;
	}

	public final void setPokemonId(Long pokemonId) {
		this.pokemonId = pokemonId;
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

	public final Long getDisappearTime() {
		return disappearTime;
	}

	public final void setDisappearTime(Long disappearTime) {
		this.disappearTime = disappearTime;
	}

	public final Long getTimeUntilHidden_ms() {
		return timeUntilHidden_ms;
	}

	public final void setTimeUntilHidden_ms(Long timeUntilHidden_ms) {
		this.timeUntilHidden_ms = timeUntilHidden_ms;
	}

	public final Long getLastModified() {
		return lastModified;
	}

	public final void setLastModified(Long lastModified) {
		this.lastModified = lastModified;
	}

	public final Long getSecondsUntilDespawn() {
		return secondsUntilDespawn;
	}

	public final void setSecondsUntilDespawn(Long secondsUntilDespawn) {
		this.secondsUntilDespawn = secondsUntilDespawn;
	}

	public final Long getSpawnStart() {
		return spawnStart;
	}

	public final void setSpawnStart(Long spawnStart) {
		this.spawnStart = spawnStart;
	}

	public final Long getSpawnEnd() {
		return spawnEnd;
	}

	public final void setSpawnEnd(Long spawnEnd) {
		this.spawnEnd = spawnEnd;
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

	public final String getIndividualAttack() {
		return individualAttack;
	}

	public final void setIndividualAttack(String individualAttack) {
		this.individualAttack = individualAttack;
	}

	public final String getIndividualDefense() {
		return individualDefense;
	}

	public final void setIndividualDefense(String individualDefense) {
		this.individualDefense = individualDefense;
	}

	public final String getIndividualStamina() {
		return individualStamina;
	}

	public final void setIndividualStamina(String individualStamina) {
		this.individualStamina = individualStamina;
	}

	public final Double getCpMultiplier() {
		return cpMultiplier;
	}

	public final void setCpMultiplier(Double cpMultiplier) {
		this.cpMultiplier = cpMultiplier;
	}

	public final String getMove1() {
		return move1;
	}

	public final void setMove1(String move1) {
		this.move1 = move1;
	}

	public final String getMove2() {
		return move2;
	}

	public final void setMove2(String move2) {
		this.move2 = move2;
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

	public final Long getPlayerLevel() {
		return playerLevel;
	}

	public final void setPlayerLevel(Long playerLevel) {
		this.playerLevel = playerLevel;
	}

	public final Boolean getVerified() {
		return verified;
	}

	public final void setVerified(Boolean verified) {
		this.verified = verified;
	}

	public final String getEncounterId() {
		return encounterId;
	}

	public final void setEncounterId(String encounterId) {
		this.encounterId = encounterId;
	}

	public final String getCp() {
		return cp;
	}

	public final void setCp(String cp) {
		this.cp = cp;
	}

	public final String getCostumeId() {
		return costumeId;
	}

	public final void setCostumeId(String costumeId) {
		this.costumeId = costumeId;
	}

	public Integer getWeatherBoosted() {
		return weatherBoosted;
	}

	public void setWeatherBoosted(Integer weatherBoosted) {
		this.weatherBoosted = weatherBoosted;
	}

	@Override
	public String toString() {
		return "PokemonWithSpawnpoint [" + (spawnpointId != null ? "spawnpointId=" + spawnpointId + ", " : "")
				+ (encounterId != null ? "encounterId=" + encounterId + ", " : "")
				+ (pokemonId != null ? "pokemonId=" + pokemonId + ", " : "")
				+ (latitude != null ? "latitude=" + latitude + ", " : "")
				+ (longitude != null ? "longitude=" + longitude + ", " : "")
				+ (disappearTime != null ? "disappearTime=" + disappearTime + ", " : "")
				+ (timeUntilHidden_ms != null ? "timeUntilHidden_ms=" + timeUntilHidden_ms + ", " : "")
				+ (lastModified != null ? "lastModified=" + lastModified + ", " : "")
				+ (secondsUntilDespawn != null ? "secondsUntilDespawn=" + secondsUntilDespawn + ", " : "")
				+ (spawnStart != null ? "spawnStart=" + spawnStart + ", " : "")
				+ (spawnEnd != null ? "spawnEnd=" + spawnEnd + ", " : "")
				+ (gender != null ? "gender=" + gender + ", " : "") + (form != null ? "form=" + form + ", " : "")
				+ (cp != null ? "cp=" + cp + ", " : "")
				+ (individualAttack != null ? "individualAttack=" + individualAttack + ", " : "")
				+ (individualDefense != null ? "individualDefense=" + individualDefense + ", " : "")
				+ (individualStamina != null ? "individualStamina=" + individualStamina + ", " : "")
				+ (cpMultiplier != null ? "cpMultiplier=" + cpMultiplier + ", " : "")
				+ (move1 != null ? "move1=" + move1 + ", " : "") + (move2 != null ? "move2=" + move2 + ", " : "")
				+ (weight != null ? "weight=" + weight + ", " : "") + (height != null ? "height=" + height + ", " : "")
				+ (playerLevel != null ? "playerLevel=" + playerLevel + ", " : "")
				+ (verified != null ? "verified=" + verified + ", " : "")
				+ (weatherBoosted != null ? "weatherBoosted=" + weatherBoosted + ", " : "")
				+ (costumeId != null ? "costumeId=" + costumeId : "") + "]";
	}

}
