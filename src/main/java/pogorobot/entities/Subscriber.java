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
import javax.persistence.Entity;
import javax.persistence.OneToOne;

import org.springframework.data.jpa.domain.AbstractPersistable;

@Entity
public class Subscriber extends AbstractPersistable<Long> {

	private static final long serialVersionUID = 7708283021538734650L;

	@OneToOne(cascade = CascadeType.PERSIST)
	// @JoinColumn(name = "user_id")
	private User subscriber;

	private Integer additionalParticipants;

	public User getSubscriber() {
		return subscriber;
	}

	public void setSubscriber(User subscriber) {
		this.subscriber = subscriber;
	}

	public Integer getAdditionalParticipants() {
		return additionalParticipants;
	}

	public void setAdditionalParticipants(Integer additionalParticipants) {
		this.additionalParticipants = additionalParticipants;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((subscriber == null) ? 0 : subscriber.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		// if (!super.equals(obj)) {
		// return false;
		// }
		if (!(obj instanceof Subscriber)) {
			return false;
		}
		Subscriber other = (Subscriber) obj;
		if (subscriber == null) {
			if (other.subscriber != null) {
				return false;
			}
		} else if (!subscriber.equals(other.subscriber)) {
			return false;
		}
		return true;
	}

}
