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

import org.springframework.data.jpa.domain.AbstractPersistable;

@Entity
public class PossibleRaidPokemon extends AbstractPersistable<Long> {

	private static final long serialVersionUID = 5211782625695222990L;

	private Integer pokemonId;
	private Integer level;
	private String type;

	public PossibleRaidPokemon() {
		this(null);
	}

	public PossibleRaidPokemon(Long id) {
		this.setId(id);
	}

	public final Integer getPokemonId() {
		return pokemonId;
	}

	public final void setPokemonId(Integer pokemonId) {
		this.pokemonId = pokemonId;
	}

	public final Integer getLevel() {
		return level;
	}

	public final void setLevel(Integer level) {
		this.level = level;
	}

	public final String getType() {
		return type;
	}

	public final void setType(String type) {
		this.type = type;
	}
}
