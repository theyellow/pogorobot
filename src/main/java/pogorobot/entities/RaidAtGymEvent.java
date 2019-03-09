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

	public Long getLevel() {
		return level;
	}

	public void setLevel(Long level) {
		this.level = level;
	}

	private Long pokemonId;

	// @Transient
	private Double latitude;

	// @Transient
	private Double longitude;

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

		// this.eventsWithSubscribers.removeAll(this.eventsWithSubscribers);
		// this.eventsWithSubscribers.addAll(eventsWithSubscribers);
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
		// int secondTimeSlotAfterMinutes = 10;
		// int thirdTimeSlotAfterMinutes = 25;
		// int fourthTimeSlotAfterMinutes = 40;
		// List<Integer> timeslotsAfterMinutes = new ArrayList<>();
		// Integer slot = 10;

		// TODO: Do we really need this:
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
		LocalDateTime firstTime = endTime.minusMinutes(timeToEnd + 35);
		result.add(firstTime.format(formatter));
		LocalDateTime nextTime = firstTime.plusMinutes(15);
		result.add(nextTime.format(formatter));
		nextTime = nextTime.plusMinutes(10);
		result.add(nextTime.format(formatter));
		nextTime = nextTime.plusMinutes(10);
		result.add(nextTime.format(formatter));

		// while (getEnd() > (getStart() + slot * 60) && timeslotsAfterMinutes.size() <=
		// 4) {
		// timeslotsAfterMinutes.add(slot);
		// slot += 15;
		// }
		// LocalDateTime beginTime =
		// LocalDateTime.ofInstant(Instant.ofEpochSecond(getStart()), systemDefault);
		// LocalDateTime firstSlot = beginTime.truncatedTo(ChronoUnit.MINUTES);
		// LocalDateTime nextFiveMinuteSlot = beginTime.truncatedTo(ChronoUnit.HOURS)
		// .plusMinutes(5 * (beginTime.getMinute() / 5) + 5);
		// refactoredAddTimeSlots(result, beginTime, endTime, formatter,
		// nextFiveMinuteSlot,
		// timeslotsAfterMinutes.toArray(new Integer[timeslotsAfterMinutes.size()]));
		// addTimeSlots(secondTimeSlotAfterMinutes, thirdTimeSlotAfterMinutes,
		// fourthTimeSlotAfterMinutes, result,
		// endTime, formatter, nextFiveMinuteSlot);
		return result;
	}

	// private void refactoredAddTimeSlots(SortedSet<String> result, LocalDateTime
	// beginTime, LocalDateTime endTime,
	// DateTimeFormatter formatter,
	// LocalDateTime nextFiveMinuteSlot, Integer... timeSlotsAfterMinutes) {
	// Arrays.asList(timeSlotsAfterMinutes).stream().forEach((afterMinutes) -> {
	// if (isNotAfterEnd(afterMinutes, endTime, nextFiveMinuteSlot)) {
	// addNormalTimeslot(afterMinutes, result, formatter, nextFiveMinuteSlot);
	// }
	// });
	// addEndTime(result, endTime, formatter);
		// setStart(end - GymService.RAID_DURATION * 60);
		// if (isNotAfterEnd(nextTimeSlotAfterMinutes, endTime, nextFiveMinuteSlot)) {
		// if (fourthTimeSlotAfterMinutes != null) {
		// addTimeSlots(thirdTimeSlotAfterMinutes, fourthTimeSlotAfterMinutes, null,
		// result, endTime, formatter,
		// nextFiveMinuteSlot);
		// } else if (thirdTimeSlotAfterMinutes != null) {
		// addTimeSlots(nextTimeSlotAfterMinutes, null, null, result, endTime,
		// formatter, nextFiveMinuteSlot);
		// }
		// // addLastTimeslots(thirdTimeSlotAfterMinutes, fourthTimeSlotAfterMinutes,
		// // result, endTime, formatter, nextFiveMinuteSlot);
		// } else {
		// addEndTime(result, endTime, formatter);
		// }
	// }

	// private void addTimeSlots(int nextTimeSlotAfterMinutes, Integer
	// thirdTimeSlotAfterMinutes,
	// Integer fourthTimeSlotAfterMinutes, SortedSet<String> result, LocalDateTime
	// endTime,
	// DateTimeFormatter formatter, LocalDateTime nextFiveMinuteSlot) {
	// if (isNotAfterEnd(nextTimeSlotAfterMinutes, endTime, nextFiveMinuteSlot)) {
	// addNormalTimeslot(nextTimeSlotAfterMinutes, result, formatter,
	// nextFiveMinuteSlot);
	// if (fourthTimeSlotAfterMinutes != null) {
	// addTimeSlots(thirdTimeSlotAfterMinutes, fourthTimeSlotAfterMinutes, null,
	// result, endTime, formatter,
	// nextFiveMinuteSlot);
	// } else if (thirdTimeSlotAfterMinutes != null) {
	// addTimeSlots(nextTimeSlotAfterMinutes, null, null, result, endTime,
	// formatter, nextFiveMinuteSlot);
	// }
	// // addLastTimeslots(thirdTimeSlotAfterMinutes, fourthTimeSlotAfterMinutes,
	// // result, endTime, formatter, nextFiveMinuteSlot);
	// } else {
	// addEndTime(result, endTime, formatter);
	// }
	// }

	// private void addLastTimeslots(int nextTimeSlotAfterMinutes, Integer
	// nextNextTimeSlotAfterMinutes,
	// SortedSet<String> result, LocalDateTime endTime,
	// DateTimeFormatter formatter, LocalDateTime nextFiveMinuteSlot) {
	// if (isNotAfterEnd(nextTimeSlotAfterMinutes, endTime, nextFiveMinuteSlot)) {
	// addNormalTimeslot(nextTimeSlotAfterMinutes, result, formatter,
	// nextFiveMinuteSlot);
	// if (nextNextTimeSlotAfterMinutes != null) {
	// addLastTimeslots(nextNextTimeSlotAfterMinutes, null, result, endTime,
	// formatter, nextFiveMinuteSlot);
	// }
	// // addLastTimeslot(nextNextTimeSlotAfterMinutes, result, endTime, formatter,
	// // nextFiveMinuteSlot);
	// } else {
	// addEndTime(result, endTime, formatter);
	// }
	// }

	// private void addLastTimeslot(int nextTimeSlotAfterMinutes,
	// SortedSet<String> result, LocalDateTime endTime, DateTimeFormatter formatter,
	// LocalDateTime nextFiveMinuteSlot) {
	// if (isNotAfterEnd(nextTimeSlotAfterMinutes, endTime, nextFiveMinuteSlot)) {
	// addNormalTimeslot(nextTimeSlotAfterMinutes, result, formatter,
	// nextFiveMinuteSlot);
	// } else {
	// addEndTime(result, endTime, formatter);
	// }
	// }

	// private boolean isNotAfterEnd(int fourthTimeSlotAfterMinutes, LocalDateTime
	// endTime,
	// LocalDateTime nextFiveMinuteSlot) {
	// return
	// endTime.isAfter(nextFiveMinuteSlot.plusMinutes(fourthTimeSlotAfterMinutes));
	// }
	//
	// private boolean addNormalTimeslot(int fourthTimeSlotAfterMinutes,
	// SortedSet<String> result,
	// DateTimeFormatter formatter, LocalDateTime nextFiveMinuteSlot) {
	// return
	// result.add(nextFiveMinuteSlot.plusMinutes(fourthTimeSlotAfterMinutes).format(formatter));
	// }
	//
	// private boolean addEndTime(SortedSet<String> result, LocalDateTime endTime,
	// DateTimeFormatter formatter) {
	// return result.add(endTime.minusMinutes(2).format(formatter));
	// }

	// public static void main(String[] args) {
	// RaidWithGym raid = new RaidWithGym("myGymId");
	// new RaidAtGymEvent(raid).getEventsWithSubscribers().stream()
	// .forEachOrdered(x -> System.out.println(x.getTime()));
	// }

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
