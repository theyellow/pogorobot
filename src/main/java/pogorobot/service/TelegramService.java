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

/**
 * 
 */
package pogorobot.service;

import pogorobot.entities.PokemonWithSpawnpoint;
import pogorobot.entities.RaidAtGymEvent;

public interface TelegramService {

	// public Message sendMessageToRecipient(User userId, String message);
	//
	// public boolean sendMessageToAll(String message);
	//
	// public boolean sendMessageToGroup(UserGroup groupId, String message);
	//
	// public boolean sendMessageToFilteredPeople(Filter filter, String
	// message);

	public void triggerPokemonMessages(PokemonWithSpawnpoint pokemon);

	// public void triggerEggMessages(RaidWithGym gym);

	public void triggerRaidMessages(RaidAtGymEvent gym);

	// public JSONObject getJsonPokemons();
	//
	// public String getPokemonName(String pokemon);

}
