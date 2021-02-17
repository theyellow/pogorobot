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
import javax.persistence.Embeddable;

@Embeddable
public class Raid {


//	private Double latitude;
//	private Double longitude;
	private Long spawn;
	private Long start;
	@Column(name = "\"end\"")
	private Long end;
	private Long raidLevel;
	private Long pokemonId;
	// private Long cp;
	private String move1;
	private String move2;
	// private String ex_raid_eligible;
	private String sponsor_id;

	public Raid() {
	}

	// public Double getLatitude() {
//		return latitude;
//	}
//
	// public void setLatitude(Double latitude) {
//		this.latitude = latitude;
//	}
//
	// public Double getLongitude() {
//		return longitude;
//	}
//
	// public void setLongitude(Double longitude) {
//		this.longitude = longitude;
//	}

	public Long getSpawn() {
		return spawn;
	}

	public void setSpawn(Long spawn) {
		this.spawn = spawn;
	}

	public Long getStart() {
		return start;
	}

	public void setStart(Long start) {
		this.start = start;
	}

	public Long getEnd() {
		return end;
	}

	public void setEnd(Long end) {
		this.end = end;
	}

	public Long getRaidLevel() {
		return raidLevel;
	}

	public void setRaidLevel(Long level) {
		this.raidLevel = level;
	}

	public Long getPokemonId() {
		return pokemonId;
	}

	public void setPokemonId(Long pokemonId) {
		this.pokemonId = pokemonId;
	}

	// public Long getCp() {
	// return cp;
	// }
	//
	// public void setCp(Long cp) {
	// this.cp = cp;
	// }

	public String getMove1() {
		return move1;
	}

	public void setMove1(String move1) {
		this.move1 = move1;
	}

	public String getMove2() {
		return move2;
	}

	public void setMove2(String move2) {
		this.move2 = move2;
	}

	// public String getExRaidEligible() {
	// return ex_raid_eligible;
	// }
	//
	// public void setExRaidEligible(String ex_raid_eligible) {
	// this.ex_raid_eligible = ex_raid_eligible;
	// }

	public String getSponsorId() {
		return sponsor_id;
	}

	public void setSponsorId(String sponsor_id) {
		this.sponsor_id = sponsor_id;
	}


	

	@Override
	public String toString() {
		return "Raid [" + (spawn != null ? "spawn=" + spawn + ", " : "")
				+ (start != null ? "start=" + start + ", " : "") + (end != null ? "end=" + end + ", " : "")
				+ (raidLevel != null ? "raidLevel=" + raidLevel + ", " : "")
				+ (pokemonId != null ? "pokemonId=" + pokemonId + ", " : "")
				+ (move1 != null ? "move1=" + move1 + ", " : "") + (move2 != null ? "move2=" + move2 + ", " : "")
				+ (sponsor_id != null ? "sponsor_id=" + sponsor_id : "") + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((end == null) ? 0 : end.hashCode());
		result = prime * result + ((pokemonId == null) ? 0 : pokemonId.hashCode());
		result = prime * result + ((raidLevel == null) ? 0 : raidLevel.hashCode());
		result = prime * result + ((start == null) ? 0 : start.hashCode());
		return result;
	}

	@Override
	@SuppressWarnings("squid:S2162") // because equals SHOULD be broken on raids
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Raid))
			return false;
		Raid other = (Raid) obj;
		if (end == null) {
			if (other.end != null)
				return false;
		} else if (!end.equals(other.end))
			return false;
		if (pokemonId == null) {
			if (other.pokemonId != null)
				return false;
		} else if (!pokemonId.equals(other.pokemonId))
			return false;
		if (raidLevel == null) {
			if (other.raidLevel != null)
				return false;
		} else if (!raidLevel.equals(other.raidLevel))
			return false;
		if (start == null) {
			if (other.start != null)
				return false;
		} else if (!start.equals(other.start))
			return false;
		return true;
	}

	
}
