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
import javax.persistence.Table;

import org.springframework.data.jpa.domain.AbstractPersistable;

@Entity
@Table(name = "\"UserGroup\"")
public class UserGroup extends AbstractPersistable<Long> {

	private static final long serialVersionUID = -3163891384170628265L;

	@Column(unique = true)
	private String groupName;
	private Long chatId;
	private String picture;
	private Boolean postRaidSummary;
	
	@OneToOne(cascade = CascadeType.ALL)
	private Filter groupFilter;

//	private boolean publicGroup;

	private String linkOfGroup;

	private Integer raidSummaryLink;

	public String getLinkOfGroup() {
		return linkOfGroup;
	}

	public void setLinkOfGroup(String linkOfGroup) {
		this.linkOfGroup = linkOfGroup;
	}

//	public boolean isPublicGroup() {
//		return publicGroup;
//	}
//
//	public void setPublicGroup(boolean publicGroup) {
//		this.publicGroup = publicGroup;
//	}

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public String getPicture() {
		return picture;
	}

	public void setPicture(String picture) {
		this.picture = picture;
	}

	public Filter getGroupFilter() {
		return groupFilter;
	}

	public void setGroupFilter(Filter groupFilter) {
		this.groupFilter = groupFilter;
	}

	public Long getChatId() {
		return chatId;
	}

	public void setChatId(Long chatId) {
		this.chatId = chatId;
	}

	public Integer getRaidSummaryLink() {
		return raidSummaryLink;
	}

	public void setRaidSummaryLink(Integer raidSummaryLink) {
		this.raidSummaryLink = raidSummaryLink;
	}

	/**
	 * @return the postRaidSummary
	 */
	public Boolean getPostRaidSummary() {
		return postRaidSummary;
	}

	/**
	 * @param postRaidSummary the postRaidSummary to set
	 */
	public void setPostRaidSummary(Boolean postRaidSummary) {
		this.postRaidSummary = postRaidSummary;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("UserGroup [");
		if (groupName != null) {
			builder.append("groupName=");
			builder.append(groupName);
			builder.append(", ");
		}
		if (chatId != null) {
			builder.append("chatId=");
			builder.append(chatId);
			builder.append(", ");
		}
		if (picture != null) {
			builder.append("picture=");
			builder.append(picture);
			builder.append(", ");
		}
		if (groupFilter != null) {
			builder.append("groupFilter=");
			builder.append(groupFilter);
			builder.append(", ");
		}
//		builder.append("publicGroup=");
//		builder.append(publicGroup);
//		builder.append(", ");
		if (linkOfGroup != null) {
			builder.append("linkOfGroup=");
			builder.append(linkOfGroup);
			builder.append(", ");
		}
		if (raidSummaryLink != null) {
			builder.append("raidSummaryLink=");
			builder.append(raidSummaryLink);
		}
		if (postRaidSummary != null) {
			builder.append("postRaidSummary=");
			builder.append(postRaidSummary);
		}
		builder.append("]");
		return builder.toString();
	}

}
