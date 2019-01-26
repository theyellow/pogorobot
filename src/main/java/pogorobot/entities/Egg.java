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

import javax.persistence.Embeddable;

//@Entity
@Embeddable
public class Egg {

//	private Double latitude;
//	private Double longitude;
//	private Long spawn;
	// private Long start;
//	private Long end;
	private Long level;
//	private Long pokemonId;
//	private Long cp;
//	private String move1;
//	private String move2;

	public Egg() {
	}
//
//	public final Double getLatitude() {
//		return latitude;
//	}
//
//	public final void setLatitude(Double latitude) {
//		this.latitude = latitude;
//	}
//
//	public final Double getLongitude() {
//		return longitude;
//	}
//
//	public final void setLongitude(Double longitude) {
//		this.longitude = longitude;
//	}

//	public final Long getSpawn() {
//		return spawn;
//	}
//
//	public final void setSpawn(Long spawn) {
//		this.spawn = spawn;
//	}

	// public final Long getStart() {
	// return start;
	// }

	// public final void setStart(Long start) {
	// this.start = start;
	// }

//	public final Long getEnd() {
//		return end;
//	}
//
//	public final void setEnd(Long end) {
//		this.end = end;
//	}

	public final Long getLevel() {
		return level;
	}

	public final void setLevel(Long level) {
		this.level = level;
	}

//	public final Long getPokemonId() {
//		return pokemonId;
//	}
//
//	public final void setPokemonId(Long pokemonId) {
//		this.pokemonId = pokemonId;
//	}

//	public final Long getCp() {
//		return cp;
//	}
//
//	public final void setCp(Long cp) {
//		this.cp = cp;
//	}

//	public final String getMove1() {
//		return move1;
//	}
//
//	public final void setMove1(String move1) {
//		this.move1 = move1;
//	}
//
//	public final String getMove2() {
//		return move2;
//	}
//
//	public final void setMove2(String move2) {
//		this.move2 = move2;
//	}

	@Override
	public String toString() {
		return "Egg [" + (level != null ? "level=" + level + ", " : "") + "]";
	}

}
