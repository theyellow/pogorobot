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

import java.util.List;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;

import org.springframework.data.jpa.domain.AbstractPersistable;

@Entity
public class Gym extends AbstractPersistable<Long> {

	private static final long serialVersionUID = -5526899994303851549L;

	// @Id
	@Column(length = 64)
	private String gymId;
	private Double latitude;
	private Double longitude;
	private Boolean enabled;
	private Long teamId;
	@Column(length = 512)
	private String name;
	@Column(length = 2048)
	private String description;
	@Column(length = 2048)
	private String url;
	private Long occupiedSince;
	private Long lastModified;
	private Long slotsAvailable;
	private Long raidActiveUntil;
	@Column(length=2048)
	private String address;
	
	@Embedded
	private Raid raid;
	
	// @Embedded
	// private Raid egg;

	public Gym() {
		this(null);
	}

	public Gym(Long id) {
		this.setId(id);
	}

	@ElementCollection()
	private List<GymPokemon> pokemon;

	public final String getGymId() {
		return gymId;
	}

	public final void setGymId(String gymId) {
		this.gymId = gymId;
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

	public final Long getTeamId() {
		return teamId;
	}

	public final void setTeamId(Long teamId) {
		this.teamId = teamId;
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

	public final Long getOccupiedSince() {
		return occupiedSince;
	}

	public final void setOccupiedSince(Long occupiedSince) {
		this.occupiedSince = occupiedSince;
	}

	public final Long getLastModified() {
		return lastModified;
	}

	public final void setLastModified(Long lastModified) {
		this.lastModified = lastModified;
	}

	public final Long getSlotsAvailable() {
		return slotsAvailable;
	}

	public final void setSlotsAvailable(Long slotsAvailable) {
		this.slotsAvailable = slotsAvailable;
	}

	public final Long getRaidActiveUntil() {
		return raidActiveUntil;
	}

	public final void setRaidActiveUntil(Long raidActiveUntil) {
		this.raidActiveUntil = raidActiveUntil;
	}

	public final List<GymPokemon> getPokemon() {
		return pokemon;
	}

	public final void setPokemon(List<GymPokemon> pokemon) {
		this.pokemon = pokemon;
	}

	@Override
	public String toString() {
		return "Gym [" + (getId() != null ? "gymId=" + getId() + ", " : "")
				+ (latitude != null ? "latitude=" + latitude + ", " : "")
				+ (longitude != null ? "longitude=" + longitude + ", " : "")
				+ (enabled != null ? "enabled=" + enabled + ", " : "")
				+ (teamId != null ? "teamId=" + teamId + ", " : "") + (name != null ? "name=" + name + ", " : "")
				+ (description != null ? "description=" + description + ", " : "")
				+ (url != null ? "url=" + url + ", " : "")
				+ (occupiedSince != null ? "occupiedSince=" + occupiedSince + ", " : "")
				+ (lastModified != null ? "lastModified=" + lastModified + ", " : "")
				+ (slotsAvailable != null ? "slotsAvailable=" + slotsAvailable + ", " : "")
				+ (raidActiveUntil != null ? "raidActiveUntil=" + raidActiveUntil + ", " : "")
				+ (pokemon != null ? "pokemon=" + pokemon : "") + "]";
	}

	public Raid getRaid() {
		return raid;
	}

	public void setRaid(Raid raid) {
		this.raid = raid;
	}

	// public Raid getEgg() {
	// return egg;
	// }
	//
	// public void setEgg(Raid egg) {
	// this.egg = egg;
	// }

	public final String getAddress() {
		return address;
	}

	public final void setAddress(String address) {
		this.address = address;
	}
}