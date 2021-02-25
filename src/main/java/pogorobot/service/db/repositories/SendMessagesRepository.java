/**
 Copyright 2021 Benjamin Marstaller
 
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

import pogorobot.entities.Raid;
import pogorobot.entities.RaidAtGymEvent;
import pogorobot.entities.SendMessages;

public interface SendMessagesRepository extends CrudRepository<SendMessages, Serializable> {

	public List<SendMessages> findByGroupChatId(Long groupchatId);

	@Query(value = "SELECT distinct(s) "
			+ "	FROM SendMessages s "
			+ "	WHERE s.groupChatId = :chatId AND s.owningRaid IS NOT NULL AND s.owningRaid.gymId LIKE :gymId")
	public List<SendMessages> findSendMessagesByChatAndGym(@Param("chatId") Long chatId, @Param("gymId") String gymId);


	@Query(value = "SELECT distinct(s) "
			+ "	FROM SendMessages s "
			+ "	WHERE s.groupChatId = :chatId AND s.owningRaid IS NOT NULL")
	public List<SendMessages> findAllSendMessagesRaidsByGroupChatId(@Param("chatId") Long chatId);

//	@Query(value = "SELECT distinct(s) "
//			+ "	FROM RaidAtGymEvent r, SendMessages s JOIN FETCH r.eventsWithSubscribers t JOIN FETCH t.subscribers "
//			+ "	WHERE s.groupChatId = :chatId AND s.owningRaid IS NOT NULL and s.owningRaid.gymId LIKE r.gymId")
//	public List<SendMessages> findInactiveSendMessagesRaidsByChat(@Param("chatId") Long chatId);

}