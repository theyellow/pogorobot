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

package pogorobot.service.db.repositories;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import pogorobot.entities.RaidAtGymEvent;

public interface RaidAtGymEventRepository extends CrudRepository<RaidAtGymEvent, Serializable> {

	public RaidAtGymEvent findByGymId(String gymId);

	@Query(value = "SELECT distinct(r) "
			+ "	FROM RaidAtGymEvent r, SendMessages s JOIN FETCH r.eventsWithSubscribers t JOIN FETCH t.subscribers"
			+ "	WHERE s.groupChatId = :chatId AND s.owningRaid IS NOT NULL AND s.owningRaid.gymId LIKE r.gymId")
	public List<RaidAtGymEvent> findRaidAtGymEventsByChat(@Param("chatId") Long chatId);
	
}