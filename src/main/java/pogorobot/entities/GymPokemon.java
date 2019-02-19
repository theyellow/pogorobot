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

import javax.persistence.Entity;
import javax.persistence.Table;

import org.springframework.data.jpa.domain.AbstractPersistable;

@Entity
@Table(name = "\"GymPokemon\"")
public class GymPokemon extends AbstractPersistable<Long> {

	private static final long serialVersionUID = 9144074287421723607L;

	private String trainerName;
	private Long trainerLevel;
	private Long pokemonId;

	public GymPokemon() {
		this(null);
	}

	public GymPokemon(Long id) {
		this.setId(id);
	}

	public String getTrainerName() {
		return trainerName;
	}

	public void setTrainerName(String trainerName) {
		this.trainerName = trainerName;
	}

	public Long getTrainerLevel() {
		return trainerLevel;
	}

	public void setTrainerLevel(Long trainerLevel) {
		this.trainerLevel = trainerLevel;
	}

	public Long getPokemonId() {
		return pokemonId;
	}

	public void setPokemonId(Long pokemonId) {
		this.pokemonId = pokemonId;
	}

	@Override
	public String toString() {
		return "GymPokemon [" + (trainerName != null ? "trainerName=" + trainerName + ", " : "")
				+ (trainerLevel != null ? "trainerLevel=" + trainerLevel + ", " : "")
				+ (pokemonId != null ? "pokemonId=" + pokemonId : "") + "]";
	}

}
