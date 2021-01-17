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
package pogorobot.service.db;

import pogorobot.entities.Gym;
import pogorobot.entities.RaidAtGymEvent;
import pogorobot.telegram.PogoBot;

public interface GymService {

	public static final int RAID_DURATION = PogoBot.getConfiguration().getRaidtime();

	public Iterable<Gym> getAllGym();

	public Gym getGym(String gymId);
	
	public Gym getGymByInternalId(Long id);

	public void deleteGym(Gym gym);

	public boolean updateOrInsertGym(Gym gym);
	
	public boolean updateOrInsertGymWithRaid(RaidAtGymEvent raid);

	public void deleteOldGymPokemonOnDatabase();

	public RaidAtGymEvent updateOrInsertRaidWithGymEvent(RaidAtGymEvent gym);
	
}