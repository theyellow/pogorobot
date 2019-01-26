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
import javax.persistence.ManyToOne;

import org.springframework.data.jpa.domain.AbstractPersistable;

@Entity
public class GroupMessages extends AbstractPersistable<Long> {

	private static final long serialVersionUID = 2140983079194139692L;

	@ManyToOne
	// @JoinColumn(name = "owningRaid")
	private ProcessedRaids owningRaid;

	private Long groupChatId;

	private Integer stickerId;

	private Integer messageId;

	private Integer locationId;

	public final ProcessedRaids getOwningRaid() {
		return owningRaid;
	}

	public final void setOwningRaid(ProcessedRaids owningRaid) {
		this.owningRaid = owningRaid;
	}

	public final Long getGroupChatId() {
		return groupChatId;
	}

	public final void setGroupChatId(Long chatId) {
		this.groupChatId = chatId;
	}

	public final Integer getStickerId() {
		return stickerId;
	}

	public final void setStickerId(Integer stickerId) {
		this.stickerId = stickerId;
	}

	public final Integer getMessageId() {
		return messageId;
	}

	public final void setMessageId(Integer messageId) {
		this.messageId = messageId;
	}

	public final Integer getLocationId() {
		return locationId;
	}

	public final void setLocationId(Integer locationId) {
		this.locationId = locationId;
	}

}
