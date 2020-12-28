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

package pogorobot.service.db;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import pogorobot.entities.EventWithSubscribers;
import pogorobot.entities.RaidAtGymEvent;
import pogorobot.entities.Subscriber;
import pogorobot.entities.User;
import pogorobot.service.TelegramKeyboardService;
import pogorobot.service.db.repositories.EventWithSubscribersRepository;
import pogorobot.service.db.repositories.RaidAtGymEventRepository;
import pogorobot.service.db.repositories.SubscriberRepository;

@Service("eventWithSubscribersService")
public class EventWithSubscribersServiceImpl implements EventWithSubscribersService {

	@SuppressWarnings("squid:S3749")
	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private EventWithSubscribersRepository eventWithSubscribersRepository;

	private static Logger logger = LoggerFactory.getLogger(EventWithSubscribersService.class);

	@Autowired
	private SubscriberRepository subscriberRepository;

	@Autowired
	private RaidAtGymEventRepository raidAtGymEventRepository;

	@Override
	@Transactional(TxType.REQUIRES_NEW)
	public SortedSet<EventWithSubscribers> getSubscribersForRaid(String gymId) {

		RaidAtGymEvent gymEvent = raidAtGymEventRepository.findById(gymId).orElse(null);
		if (gymEvent == null) {
			System.out.println("Error, no gym found for gymId:" + gymId);
			// EventWithSubscribers.
			return new RaidAtGymEvent().getEventsWithSubscribers();
		}
		SortedSet<EventWithSubscribers> eventsWithSubscribers = gymEvent.getEventsWithSubscribers();
		Hibernate.initialize(eventsWithSubscribers);
		for (EventWithSubscribers event : eventsWithSubscribers) {
			Set<Subscriber> subscribers = event.getSubscribers();
			Hibernate.initialize(subscribers);
			subscribers.stream().forEach(x -> {
				Hibernate.initialize(x.getSubscriber());
			});
			if (subscribers.size() > 0) {
				System.out.println("Something is happening, an event somewhere with people?");
			}

		}
		// SortedSet<EventWithSubscribers> eventsForRaid =
		// eventWithSubscribersRepository.findByRaid(gymEvent);
		return eventsWithSubscribers;
	}

	@Override
	@Transactional(TxType.REQUIRES_NEW)
	public void saveSubscribersForRaid(SortedSet<EventWithSubscribers> eventsForRaid) {
		RaidAtGymEvent raid = null;
		for (EventWithSubscribers eventWithSubscribers : eventsForRaid) {
			raid = eventWithSubscribers.getRaid();
			Set<Subscriber> subscribers = eventWithSubscribers.getSubscribers();
			Set<Subscriber> mergedSubscribers = new HashSet<>();
			for (Subscriber subscriber : subscribers) {
				User user = subscriber.getSubscriber();
				if (user != null) {
					user = entityManager.merge(user);
					subscriber.setSubscriber(user);
					mergedSubscribers.add(entityManager.merge(subscriber));
				} else {
					System.out.println("Don't know why, but the user is null");
				}
			}
			eventWithSubscribers.getSubscribers().clear();
			eventWithSubscribers.getSubscribers().addAll(mergedSubscribers);
			EventWithSubscribers merge = entityManager.merge(eventWithSubscribers);
		}
		if (raid != null) {
			// raid.setEventsWithSubscribers(eventsForRaid);
			entityManager.merge(raid);
		}
		// entityManager.merge(eventsForRaid);
		entityManager.flush();
	}

	@Override
	@Transactional(TxType.REQUIRED)
	public void deleteEvent(String gymId) {
		RaidAtGymEvent gymEvent = raidAtGymEventRepository.findById(gymId).orElse(null);
		if (gymEvent == null) {
			System.out.println("No Raid found for gymId " + gymId);
			return;
		}
		SortedSet<EventWithSubscribers> eventsWithSubscribers = gymEvent.getEventsWithSubscribers();
		eventsWithSubscribers.stream().forEach(eventWithSubscriber -> {
			// eventWithSubscriber.getSubscribers().stream().forEach(realSubscriber -> {
			// subscriberRepository.delete(realSubscriber);
			// });
			if (eventWithSubscriber != null) {
				eventWithSubscribersRepository.delete(eventWithSubscriber);
			}
		});
		// if (eventsWithSubscribers != null) {
		// eventWithSubscribersRepository.delete(eventsWithSubscribers);
		// }
		raidAtGymEventRepository.delete(gymEvent);
		entityManager.flush();
	}

	@Override
	@Transactional(TxType.REQUIRES_NEW)
	public void modifyEvent(String commandOrGymId, User user, String gymId, String time) {

		// Gym fullGym = gymService.getGym(gymId);
		SortedSet<EventWithSubscribers> eventsWithSubscribers = getSubscribersForRaid(gymId);
		// event.setEventsWithSubscribers(eventsWithSubscribers);
		// SortedSet<EventWithSubscribers> eventsWithSubscribers =
		// event.getEventsWithSubscribers();

		if (null == commandOrGymId) {
			logger.error("Error while trying to signup raid");
		} else if (TelegramKeyboardService.CANCEL.equals(commandOrGymId)) {
			for (EventWithSubscribers eventWithSubscribers : eventsWithSubscribers) {
				Set<Subscriber> users = eventWithSubscribers.getSubscribers();
				Set<Subscriber> toRemove = new HashSet<>();
				for (Subscriber subscriber : users) {
					if (subscriber.getSubscriber().equals(user)) {
						toRemove.add(subscriber);
					}
				}
				for (Subscriber subscriber : toRemove) {
					eventWithSubscribers.removeSubscriber(subscriber);
				}
				// TODO: Where is save now?
				// if (users.contains(user)) {
				// eventWithSubscribersRepository.save(x);
				// }

			}
		} else if (TelegramKeyboardService.ADDONE.equals(commandOrGymId)) {
			// TODO: implement addOne - action
			List<Boolean> breakOuterLoop = new ArrayList<>();
			for (EventWithSubscribers eventWithSubscribers : eventsWithSubscribers) {
				Set<Subscriber> users = eventWithSubscribers.getSubscribers();
				users.stream().forEach(subscriber -> {
					if (user.equals(subscriber.getSubscriber())) {
						subscriber.setAdditionalParticipants(subscriber.getAdditionalParticipants() + 1);
						breakOuterLoop.add(true);
					}
				});
				if (!breakOuterLoop.isEmpty()) {
					break;
				}
			}
		} else if (TelegramKeyboardService.REMOVEONE.equals(commandOrGymId)) {
			List<Boolean> breakOuterLoop = new ArrayList<>();
			for (EventWithSubscribers eventWithSubscribers : eventsWithSubscribers) {
				Set<Subscriber> users = eventWithSubscribers.getSubscribers();
				users.stream().forEach(subscriber -> {
					if (user.equals(subscriber.getSubscriber())) {
						Integer additionalParticipants = subscriber.getAdditionalParticipants();
						if (additionalParticipants > 0) {
							subscriber.setAdditionalParticipants(additionalParticipants - 1);
						}
						breakOuterLoop.add(true);
					}
				});
				if (!breakOuterLoop.isEmpty()) {
					break;
				}
			}
		} else {
			for (EventWithSubscribers eventWithSubscribers : eventsWithSubscribers) {
				Set<Subscriber> users = eventWithSubscribers.getSubscribers();
				if (eventWithSubscribers.getTime().equals(time)) {
					Set<User> usersParticipating = users.stream().map(subscriber -> subscriber.getSubscriber())
							.collect(Collectors.toSet());
					if (!usersParticipating.contains(user)) {
						Subscriber subscriber = new Subscriber();
						subscriber.setSubscriber(user);
						subscriber.setAdditionalParticipants(0);
						// Hibernate.initialize(subscriber);
						eventWithSubscribers.addSubcriber(subscriber);
					}
				} else {
					Set<User> usersParticipating = users.stream().map(subscriber -> subscriber.getSubscriber())
							.collect(Collectors.toSet());
					if (usersParticipating.contains(user)) {

						eventWithSubscribers.removeSubscriber(user);
					}
					// eventWithSubscribersRepository.save(x);
				}

			}
			// eventsWithSubscribers.stream().forEach(x -> {
			// });

		}
		saveSubscribersForRaid(eventsWithSubscribers);
		// String time = ;

	}
}
