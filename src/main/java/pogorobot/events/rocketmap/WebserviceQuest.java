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

package pogorobot.events.rocketmap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;

import pogorobot.entities.Quest;
import pogorobot.events.EventMessage;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSubTypes({ @JsonSubTypes.Type(value = QuestCondition.class, name = "condition"),
		@JsonSubTypes.Type(value = QuestReward.class, name = "reward") })
public class WebserviceQuest implements EventMessage<Quest> {

	private String pokestop_id;
	private String pokestop_name;
	private String template;
	private String pokestop_url;
	private QuestCondition conditions; // ": [ {"type":7,"info":{"raid_levels":[1,2,3,4,5]} },{"type":6}],
	private Double latitude;
	private Double longitude;
	private String type;
	private String target;
	private QuestReward rewards; // ":
					// [{"type":7,"info":{"shiny":false,"costume_id":0,"pokemon_id":25,"form_id":0,"gender_id":0}
					// }],
	private Long updated;


	@Override
	public Quest transformToEntity() {
		Quest quest = new Quest();
		quest.setPokestopId(pokestop_id);
		quest.setPokestopName(pokestop_name);
		quest.setTemplate(template);
		quest.setPokestopUrl(pokestop_url);
		quest.setConditions(conditions != null ? conditions.toString() : ""); // ": [
																				// {"type":7,"info":{"raid_levels":[1,2,3,4,5]}
																				// },{"type":6}],
		quest.setLatitude(latitude);
		quest.setLongitude(longitude);
		quest.setType(type);
		quest.setTarget(target);
		quest.setRewards(rewards != null ? rewards.toString() : ""); // ":
						// [{"type":7,"info":{"shiny":false,"costume_id":0,"pokemon_id":25,"form_id":0,"gender_id":0}
						// }],
		quest.setUpdated(updated);
		return quest;
	}


}
