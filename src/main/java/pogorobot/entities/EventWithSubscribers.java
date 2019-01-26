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

import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import org.springframework.data.jpa.domain.AbstractPersistable;

@Entity
public class EventWithSubscribers extends AbstractPersistable<Long> implements Comparable<EventWithSubscribers> {

	private static final long serialVersionUID = 325243376183407598L;

	private String time;

	@ManyToMany(cascade = CascadeType.ALL)
	private Set<Subscriber> subscribers;

	@ManyToOne()
	@JoinColumn(name = "raid_id")
	private RaidAtGymEvent raid;

	public EventWithSubscribers() {
		this(null);
	}

	public EventWithSubscribers(Long id) {
		this.setId(id);
	}

	public final String getTime() {
		return time;
	}

	public final void setTime(String time) {
		this.time = time;
	}

	public final Set<Subscriber> getSubscribers() {
		return subscribers;
	}

	public final void setSubscribers(Set<Subscriber> users) {
		this.subscribers = users;
	}

	public final void addSubcriber(Subscriber subscriber) {
		this.subscribers.add(subscriber);
	}

	public final void removeSubscriber(Subscriber user) {
		// for (Subscriber subscriber : subscribers) {
		// if (this.users.contains(subscriber.getSubscriber())
		// }
		if (this.subscribers.contains(user)) {
			this.subscribers.remove(user);
		}
	}

	@Transient
	public final void removeSubscriber(User user) {
		for (Subscriber subscriber : subscribers) {
			if (user.getId().equals(subscriber.getSubscriber().getId())) {
				this.subscribers.remove(subscriber);
			}
		}
	}

	public final RaidAtGymEvent getRaid() {
		return raid;
	}

	public final void setRaid(RaidAtGymEvent raid) {
		this.raid = raid;
	}

	// @Transient
	// public String getGymId() {
	// return getRaid().getGymId();
	// }

	@Override
	public int compareTo(EventWithSubscribers o) {
		int compareTo;
		if (time == null) {
			compareTo = -1;
		} else if (o == null || o.getTime() == null) {
			compareTo = 1;
		} else {
			compareTo = time.compareTo(o.getTime());
		}
		return compareTo;
	}
}
