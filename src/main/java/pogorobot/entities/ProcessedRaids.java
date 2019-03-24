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

import org.springframework.data.jpa.domain.AbstractPersistable;

@Entity
@Table(name = "\"ProcessedRaids\"")
public class ProcessedRaids extends AbstractPersistable<Long> {

	public ProcessedRaids() {
		this(null, null);
	}

	public ProcessedRaids(Long id) {
		super();
		setId(id);
	}

	public ProcessedRaids(String gymId, Long endTime) {
		super();
		this.gymId = gymId;
		this.endTime = endTime;
	}

	private static final long serialVersionUID = 887237883173573779L;

	@Column(length = 50)
	String gymId;

	Long endTime;

	@OneToMany(cascade = CascadeType.ALL)
	Set<SendMessages> groupsRaidIsPosted;

	public String getGymId() {
		return gymId;
	}

	public void setGymId(String gymId) {
		this.gymId = gymId;
	}

	public Long getEndTime() {
		return endTime;
	}

	public void setEndTime(Long endTime) {
		this.endTime = endTime;
	}

	public Set<SendMessages> getGroupsRaidIsPosted() {
		if (groupsRaidIsPosted == null) {
			groupsRaidIsPosted = new HashSet<>();
		}
		return groupsRaidIsPosted;
	}

	public void addToGroupsRaidIsPosted(SendMessages group) {
		if (group != null) {
			getGroupsRaidIsPosted().add(group);
			group.setOwningRaid(this);
		}
	}

	public boolean removeFromGroupsRaidIsPosted(SendMessages group) {
		group.setOwningRaid(null);
		return getGroupsRaidIsPosted().remove(group);
	}

	@Override
	public String toString() {
		return "ProcessedRaids [" + (gymId != null ? "gymId=" + gymId + ", " : "")
				+ (endTime != null ? "endTime=" + endTime : "") + "]";
	}

}
