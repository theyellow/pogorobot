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

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.springframework.data.jpa.domain.AbstractPersistable;

@Entity
@Table(name = "\"ProcessedPokemon\"")
public class ProcessedPokemon extends AbstractPersistable<Long> {

	private static final long serialVersionUID = 887237113173573779L;

	@Column(length = 50)
	String encounterId;

	Long endTime;

	@OneToMany(cascade = CascadeType.ALL)
	Set<SendMessages> chatsPokemonIsPosted;

	public ProcessedPokemon() {
		this(null);
	}

	public ProcessedPokemon(Long id) {
		super();
		setId(id);
	}

	public ProcessedPokemon(String encounterId, Long endTime) {
		super();
		this.encounterId = encounterId;
		this.endTime = endTime;
	}


	public Long getEndTime() {
		return endTime;
	}

	public void setEndTime(Long endTime) {
		this.endTime = endTime;
	}

	public Set<SendMessages> getChatsPokemonIsPosted() {
		if (chatsPokemonIsPosted == null) {
			chatsPokemonIsPosted = new HashSet<>();
		}
		return chatsPokemonIsPosted;
	}

	public void setChatsPokemonIsPosted(Set<SendMessages> chatsPokemonIsPosted) {
		this.chatsPokemonIsPosted = chatsPokemonIsPosted;
	}

	public void setEncounterId(String pokemonId) {
		this.encounterId = pokemonId;
	}

	public void addToChatsPokemonIsPosted(SendMessages group) {
		if (group != null) {
			getChatsPokemonIsPosted().add(group);
			group.setOwningPokemon(this);
		}
	}

	public boolean removeFromChatsPokemonIsPosted(SendMessages group) {
		group.setOwningPokemon(null);
		return getChatsPokemonIsPosted().remove(group);
	}

	@Transient
	public boolean isSomewherePosted() {
		return chatsPokemonIsPosted != null && !chatsPokemonIsPosted.isEmpty();
	}


	@Override
	public String toString() {
		return "ProcessedPokemon [" + (encounterId != null ? "encounterId=" + encounterId + ", " : "")
				+ (endTime != null ? "endTime=" + endTime + ", " : "")
				+ (chatsPokemonIsPosted != null ? "chatsPokemonIsPosted=" + chatsPokemonIsPosted : "") + "]";
	}

	public String getEncounterId() {
		return encounterId;
	}

}
