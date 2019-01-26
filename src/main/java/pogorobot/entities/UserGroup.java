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

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToOne;

import org.springframework.data.jpa.domain.AbstractPersistable;

@Entity
public class UserGroup extends AbstractPersistable<Long> {

	private static final long serialVersionUID = -3163891384170628265L;

	@Column(unique = true)
	private String groupName;
	private Long chatId;
	private String picture;
	
	@OneToOne(cascade = CascadeType.ALL)
	private Filter groupFilter;

	
	// public UserGroup() {
	//// this(null);
	// }

	// public UserGroup(Long id) {
	// this.setId(id);
	// }

	public final String getGroupName() {
		return groupName;
	}

	public final void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public final String getPicture() {
		return picture;
	}

	public final void setPicture(String picture) {
		this.picture = picture;
	}

	public final Filter getGroupFilter() {
		return groupFilter;
	}

	public final void setGroupFilter(Filter groupFilter) {
		this.groupFilter = groupFilter;
	}

	public final Long getChatId() {
		return chatId;
	}

	public final void setChatId(Long chatId) {
		this.chatId = chatId;
	}

	@Override
	public String toString() {
		return "UserGroup [" + (groupName != null ? "groupName=" + groupName + ", " : "")
				+ (chatId != null ? "chatId=" + chatId + ", " : "")
				+ (picture != null ? "picture=" + picture + ", " : "")
				+ (groupFilter != null ? "groupFilter=" + groupFilter : "") + "]";
	}

}
