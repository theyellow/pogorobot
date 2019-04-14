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

package pogorobot.service;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import pogorobot.entities.EventWithSubscribers;
import pogorobot.entities.RaidAtGymEvent;
import pogorobot.entities.Subscriber;
import pogorobot.entities.User;
import pogorobot.repositories.EventWithSubscribersRepository;
import pogorobot.repositories.RaidAtGymEventRepository;
import pogorobot.repositories.SubscriberRepository;

@Service("eventWithSubscribersService")
public class EventWithSubscribersServiceImpl implements EventWithSubscribersService {

	@SuppressWarnings("squid:S3749")
	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private EventWithSubscribersRepository eventWithSubscribersRepository;

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

}
