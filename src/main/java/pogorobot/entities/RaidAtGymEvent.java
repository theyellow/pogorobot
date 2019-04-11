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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Transient;

@Entity
@Table(name = "\"RaidAtGymEvent\"")
public class RaidAtGymEvent {

	// private String gymId;

	@Override
	public String toString() {
		return "RaidAtGymEvent ["
				+ (eventsWithSubscribers != null ? "eventsWithSubscribers=" + eventsWithSubscribers + ", " : "")
				+ (id != null ? "id=" + id + ", " : "") + (gymId != null ? "gymId=" + gymId + ", " : "")
				+ (start != null ? "start=" + start + ", " : "") + (end != null ? "end=" + end + ", " : "")
				+ (pokemonId != null ? "pokemonId=" + pokemonId : "") + "]";
	}

	@OneToMany
	@OrderBy("time ASC")
	private SortedSet<EventWithSubscribers> eventsWithSubscribers;

	@Id
	@Column(length = 64)
	private String id;

	@Column(length = 64)
	private String gymId;

	private Long start;

	@Column(name = "\"end\"")
	private Long end;

	private Long level;

	private Long pokemonId;

	private Double latitude;

	private Double longitude;

	public Long getLevel() {
		return level;
	}

	public void setLevel(Long level) {
		this.level = level;
	}

	public Double getLatitude() {
		return latitude;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	public Double getLongitude() {
		return longitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	public RaidAtGymEvent(RaidWithGym raid) {
		gymId = raid.getGymId();
		start = raid.getStart();
		end = raid.getEnd();
		pokemonId = raid.getPokemonId();
		level = raid.getRaidLevel();
		// latitude = raid.getLatitude();
		// longitude = raid.getLongitude();
		setId(gymId);
	}

	public RaidAtGymEvent(EggWithGym egg) {
		gymId = egg.getGymId();
		start = egg.getStart();
		end = egg.getEnd();
		pokemonId = egg.getPokemonId();
		level = egg.getRaidLevel();
		// latitude = raid.getLatitude();
		// longitude = raid.getLongitude();
		setId(gymId);
	}

	public RaidAtGymEvent() {
		this((String) null);
	}

	public RaidAtGymEvent(String id) {
		setId(id);
	}

	public SortedSet<EventWithSubscribers> getEventsWithSubscribers() {
		if (null == eventsWithSubscribers) {
			eventsWithSubscribers = createEmptyEventSet();
		}
		return eventsWithSubscribers;
	}

	@Transient
	public boolean hasEventWithSubscribers() {
		return eventsWithSubscribers != null && !eventsWithSubscribers.isEmpty();
	}

	public void setEventsWithSubscribers(SortedSet<EventWithSubscribers> eventsWithSubscribers) {
		if (null == this.eventsWithSubscribers) {
			this.eventsWithSubscribers = eventsWithSubscribers == null ? createEmptyEventSet() : eventsWithSubscribers;
		} else {
			for (EventWithSubscribers oldEventWithSubscribers : this.eventsWithSubscribers) {

				for (EventWithSubscribers newEventWithSubscribers : eventsWithSubscribers) {
					if (newEventWithSubscribers.getTime().equals(oldEventWithSubscribers.getTime())) {
						newEventWithSubscribers.getSubscribers().stream().forEach(newSubscriber -> {
							oldEventWithSubscribers.addSubcriber(newSubscriber);
						});
						Set<Subscriber> toRemove = new HashSet<>();
						oldEventWithSubscribers.getSubscribers().stream().forEach(oldSubscriber -> {
							if (!newEventWithSubscribers.getSubscribers().contains(oldSubscriber)) {
								toRemove.add(oldSubscriber);
							}
						});
						oldEventWithSubscribers.getSubscribers().removeAll(toRemove);
					}
				}
			}
		}
		this.eventsWithSubscribers.stream().forEach(x -> x.setRaid(this));
	}

	@Transient
	public void addEventsWithSubscribers(EventWithSubscribers eventWithSubscribers) {
		if (null == eventsWithSubscribers) {
			this.eventsWithSubscribers = createEmptyEventSet();
		}
		eventWithSubscribers.setRaid(this);
		this.eventsWithSubscribers.add(eventWithSubscribers);
	}

	@Transient
	public void addUserAtTime(User user, String time) {
		if (null == eventsWithSubscribers) {
			this.eventsWithSubscribers = createEmptyEventSet();
		}
		this.eventsWithSubscribers.stream().forEach(x -> {
			if (x.getTime().equals(time)) {
				Subscriber subscriber = new Subscriber();
				subscriber.setSubscriber(user);
				subscriber.setAdditionalParticipants(0);
				x.getSubscribers().add(subscriber);
			}
		});
	}

	@Transient
	private SortedSet<EventWithSubscribers> createEmptyEventSet() {
		SortedSet<EventWithSubscribers> events = new TreeSet<>();
		SortedSet<String> possibleTimes = getPossibleTimeStrings();
		for (String time : possibleTimes) {
			EventWithSubscribers event = new EventWithSubscribers();
			event.setTime(time);
			event.setSubscribers(new HashSet<>());
			event.setRaid(this);
			events.add(event);
		}
		return events;
	}

	@Transient
	private final SortedSet<String> getPossibleTimeStrings() {
		Long start = getStart();
		long nowInSeconds = System.currentTimeMillis() / 1000;
		if (start == null) {
			setStart(nowInSeconds);
		}

		// Long endInSeconds = getEnd();

		SortedSet<String> result = new TreeSet<>();
		ZoneId systemDefault = ZoneId.systemDefault();
		LocalDateTime endTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(getEnd()), systemDefault);
		DateTimeFormatter formatter = new DateTimeFormatterBuilder().appendValue(ChronoField.HOUR_OF_DAY)
				.appendLiteral(":").appendValue(ChronoField.MINUTE_OF_HOUR, 2).toFormatter();

		int minute = endTime.getMinute();
		int timeToEnd = (minute % 5 == 0 ? 5 : minute % 5) + 5;
		LocalDateTime firstTime = endTime.minusMinutes(timeToEnd + 35L);
		result.add(firstTime.format(formatter));
		LocalDateTime nextTime = firstTime.plusMinutes(15);
		result.add(nextTime.format(formatter));
		nextTime = nextTime.plusMinutes(10);
		result.add(nextTime.format(formatter));
		nextTime = nextTime.plusMinutes(10);
		result.add(nextTime.format(formatter));

		return result;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getGymId() {
		return gymId;
	}

	public void setGymId(String gymId) {
		this.gymId = gymId;
	}

	public Long getStart() {
		return start;
	}

	public void setStart(Long start) {
		this.start = start;
	}

	public Long getEnd() {
		return end;
	}

	public void setEnd(Long end) {
		this.end = end;
	}

	public Long getPokemonId() {
		return pokemonId;
	}

	public void setPokemonId(Long pokemonId) {
		this.pokemonId = pokemonId;
	}

}
