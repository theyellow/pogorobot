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
import javax.persistence.Entity;
import javax.persistence.OneToOne;

import org.springframework.data.jpa.domain.AbstractPersistable;

@Entity
public class User extends AbstractPersistable<Long> {

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((telegramId == null) ? 0 : telegramId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (super.equals(obj)) {
			return true;
		}
		if (!(obj instanceof User)) {
			return false;
		}
		User other = (User) obj;
		if (telegramId == null) {
			if (other.telegramId != null) {
				return false;
			}
		} else if (!telegramId.equals(other.telegramId)) {
			return false;
		}
		return true;
	}

	private static final long serialVersionUID = 4921112520268736431L;

	@Column(unique = true)
	private String telegramId;

	private String chatId;

	private String telegramName;

	private boolean telegramActive;

	private String name;

	private String ingameName;

	private Long trainerLevel;

	@OneToOne
	private Filter userFilter;
	
	@ElementCollection
	private List<Filter> filters;

	private boolean payed;

	private boolean showPokemonMessages;

	private boolean showRaidMessages;

	private boolean admin;

	private boolean raidadmin;

	private boolean superadmin;

	@ElementCollection
	private List<UserGroup> groups;

	public User() {
		this(null);
	}

	public User(Long id) {
		this.setId(id);
	}

	public final String getTelegramId() {
		return telegramId;
	}

	public final void setTelegramId(String telegramId) {
		this.telegramId = telegramId;
	}

	public final String getName() {
		return name;
	}

	public final void setName(String name) {
		this.name = name;
	}

	public final String getIngameName() {
		return ingameName;
	}

	public final void setIngameName(String ingameName) {
		this.ingameName = ingameName;
	}

	public final Long getTrainerLevel() {
		return trainerLevel;
	}

	public final void setTrainerLevel(Long trainerLevel) {
		this.trainerLevel = trainerLevel;
	}

	public final List<Filter> getFilters() {
		return filters;
	}

	public final void setFilters(List<Filter> filters) {
		this.filters = filters;
	}

	public final boolean isPayed() {
		return payed;
	}

	public final void setPayed(boolean payed) {
		this.payed = payed;
	}

	public final boolean isAdmin() {
		return admin;
	}

	public final void setAdmin(boolean admin) {
		this.admin = admin;
	}

	public boolean isRaidadmin() {
		return raidadmin;
	}

	public void setRaidadmin(boolean raidadmin) {
		this.raidadmin = raidadmin;
	}

	public final boolean isSuperadmin() {
		return superadmin;
	}

	public final void setSuperadmin(boolean superadmin) {
		this.superadmin = superadmin;
	}

	public final List<UserGroup> getGroups() {
		return groups;
	}

	public final void setGroups(List<UserGroup> groups) {
		this.groups = groups;
	}

	@Override
	public String toString() {
		return "User [" + (telegramId != null ? "telegramId=" + telegramId + ", " : "")
				+ (name != null ? "name=" + name + ", " : "")
				+ (ingameName != null ? "ingameName=" + ingameName + ", " : "")
				+ (trainerLevel != null ? "trainerLevel=" + trainerLevel + ", " : "")
				+ (filters != null ? "filters=" + filters + ", " : "") + "payed=" + payed + ", admin=" + admin
				+ ", superadmin=" + superadmin + ", " + (groups != null ? "groups=" + groups : "") + "]";
	}

	public String getTelegramName() {
		return telegramName;
	}

	public void setTelegramName(String telegramName) {
		this.telegramName = telegramName;
	}

	public final Filter getUserFilter() {
		return userFilter;
	}

	public final void setUserFilter(Filter userFilter) {
		this.userFilter = userFilter;
		if (userFilter != null && userFilter.getOwner() == null) {
			userFilter.setOwner(this);
		}
	}

	public final boolean isTelegramActive() {
		return telegramActive;
	}

	public final void setTelegramActive(boolean telegramActive) {
		this.telegramActive = telegramActive;
	}

	public boolean isShowPokemonMessages() {
		return showPokemonMessages;
	}

	public void setShowPokemonMessages(boolean showPokemonMessages) {
		this.showPokemonMessages = showPokemonMessages;
	}

	public boolean isShowRaidMessages() {
		return showRaidMessages;
	}

	public void setShowRaidMessages(boolean showRaidMessages) {
		this.showRaidMessages = showRaidMessages;
	}

	public final String getChatId() {
		return chatId;
	}

	public final void setChatId(String chatId) {
		this.chatId = chatId;
	}

}
