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
import java.util.Set;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;

import org.springframework.data.jpa.domain.AbstractPersistable;

@Entity
public class Filter extends AbstractPersistable<Long> {

	private static final long serialVersionUID = -1842789670931226752L;

	private FilterType filterType;
	
	@OneToOne(mappedBy = "userFilter")
	private User owner;
	
	@OneToOne(mappedBy = "groupFilter")
	private UserGroup group;

	@ElementCollection
	private List<User> receivers;
	
	@ElementCollection
	private List<UserGroup> receiveFromGroups;

	private Double minIV;

	private Boolean onlyWithIV;

	private Integer minWP;
	
	private Double latitude;
	
	private Double longitude;

	private Double radius;

	private Double radiusPokemon;

	private Double radiusRaids;

	private Double radiusIV;
	
	@ElementCollection(fetch = FetchType.EAGER)
	private List<Integer> pokemons;
	
	// @ElementCollection
	// private List<Gym> gyms;

	@ElementCollection
	private List<Integer> gymPokemons;

	@ElementCollection
	private List<Integer> raidPokemon;
	
	@ManyToMany
	@JoinTable(name = "filter_geofences_mon")
	private Set<Geofence> geofences;

	@ManyToMany
	@JoinTable(name = "filter_geofences_raid")
	private Set<Geofence> raidGeofences;

	@ManyToMany
	@JoinTable(name = "filter_geofences_iv")
	private Set<Geofence> ivGeofences;

	private Integer raidLevel;


	public Filter() {
		this(null);
	}
	
	public Filter(Long id) {
		this.setId(id);
	}

	public Double getLatitude() {
		return latitude;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	public Double getLongitude() {
		return longitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	public Double getRadius() {
		return radius;
	}

	public void setRadius(Double radius) {
		this.radius = radius;
	}

	public List<Integer> getPokemons() {
		return pokemons;
	}

	public void setPokemons(List<Integer> pokemons) {
		this.pokemons = pokemons;
	}

	// public List<Gym> getGyms() {
	// return gyms;
	// }
	//
	// public void setGyms(List<Gym> gyms) {
	// this.gyms = gyms;
	// }

	public List<Integer> getGymPokemons() {
		return gymPokemons;
	}

	public void setGymPokemons(List<Integer> gymPokemons) {
		this.gymPokemons = gymPokemons;
	}
	
	public FilterType getFilterType() {
		return filterType;
	}
	
	public void setFilterType(FilterType filterType) {
		this.filterType = filterType;
	}

	@Override
	public String toString() {
		return "Filter [" + (filterType != null ? "filterType=" + filterType + ", " : "")
				+ (receivers != null ? "receivers=" + receivers + ", " : "")
				+ (receiveFromGroups != null ? "groups=" + receiveFromGroups + ", " : "")
				+ (latitude != null ? "latitude=" + latitude + ", " : "")
				+ (longitude != null ? "longitude=" + longitude + ", " : "")
				+ (radius != null ? "radius=" + radius + ", " : "")
				+ (pokemons != null ? "pokemons=" + pokemons + ", " : "")
				// + (gyms != null ? "gyms=" + gyms + ", " : "")
				+ (gymPokemons != null ? "gymPokemons=" + gymPokemons : "") + "]";
	}

	public User getOwner() {
		return owner;
	}

	public void setOwner(User owner) {
		this.owner = owner;
		if (owner != null && owner.getUserFilter() == null) {
			owner.setUserFilter(this);
		}
	}

	public List<User> getReceivers() {
		return receivers;
	}

	public void setReceivers(List<User> receivers) {
		this.receivers = receivers;
	}

	public UserGroup getGroup() {
		return group;
	}

	public void setGroup(UserGroup group) {
		this.group = group;
	}

	public List<UserGroup> getReceiveFromGroups() {
		return receiveFromGroups;
	}

	public void setReceiveFromGroups(List<UserGroup> receiveFromGroups) {
		this.receiveFromGroups = receiveFromGroups;
	}

	public List<Integer> getRaidPokemon() {
		return raidPokemon;
	}

	public void setRaidPokemon(List<Integer> raidPokemon) {
		this.raidPokemon = raidPokemon;
	}

	public Integer getRaidLevel() {
		return raidLevel;
	}

	public void setRaidLevel(Integer raidLevel) {
		this.raidLevel = raidLevel;
	}

	public Set<Geofence> getGeofences() {
		return geofences;
	}

	public void setGeofences(Set<Geofence> geofences) {
		this.geofences = geofences;
	}

	public Set<Geofence> getRaidGeofences() {
		return raidGeofences;
	}

	public void setRaidGeofences(Set<Geofence> raidGeofences) {
		this.raidGeofences = raidGeofences;
	}

	public Double getMinIV() {
		return minIV;
	}

	public void setMinIV(Double minIV) {
		this.minIV = minIV;
	}

	public Integer getMinWP() {
		return minWP;
	}

	public void setMinWP(Integer minWP) {
		this.minWP = minWP;
	}

	public Boolean getOnlyWithIV() {
		return onlyWithIV;
	}

	public void setOnlyWithIV(Boolean onlyWithIV) {
		this.onlyWithIV = onlyWithIV;
	}

	public Double getRadiusPokemon() {
		return radiusPokemon;
	}

	public void setRadiusPokemon(Double radiusPokemon) {
		this.radiusPokemon = radiusPokemon;
	}

	public Double getRadiusRaids() {
		return radiusRaids;
	}

	public void setRadiusRaids(Double radiusRaids) {
		this.radiusRaids = radiusRaids;
	}

	public Double getRadiusIV() {
		return radiusIV;
	}

	public void setRadiusIV(Double radiusIV) {
		this.radiusIV = radiusIV;
	}

	public Set<Geofence> getIvGeofences() {
		return ivGeofences;
	}

	public void setIvGeofences(Set<Geofence> ivGeofences) {
		this.ivGeofences = ivGeofences;
	}

}