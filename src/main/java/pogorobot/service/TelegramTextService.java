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

import java.time.LocalDate;
import java.util.List;
import java.util.SortedSet;

import org.apache.commons.codec.DecoderException;

import pogorobot.entities.EventWithSubscribers;
import pogorobot.entities.Gym;
import pogorobot.entities.PokemonWithSpawnpoint;

public interface TelegramTextService {

	String getPokemonName(String pokemon);

	String getMoveName(String move);

	String getFilteredPokemonForUser(Integer telegramId);

	String createFormattedLocationString(String string, String string2);

	String getPrintFormattedRadiusForUser(Integer intValue, String type);

	String formatTimeFromSeconds(long l);

	String createDec() throws DecoderException;

	String createEggMessageText(Gym fullGym, Long end, String level,
			Double latitude, Double longitude);

	String getStickerUrl(int intValue) throws DecoderException;

	// String createRaidMessageText(String pokemonName, Long end, String level,
	// String move1, String move2, Double latitude, Double longitude, String url,
	// String address, String gymName);

	String createPokemonMessageNonIVText(PokemonWithSpawnpoint pokemon);

	String createPokemonMessageWithIVText(PokemonWithSpawnpoint pokemon);

	Double calculateWP(Long pokemonId, double cpMultiplier, int attack, int defense, int stamina);

	Double calculateIVs(int attack, int defense, int stamina);

	String getGymsAroundMessageText(List<Gym> gymsAround);

	String getGymsOrEggChoiceText();

	String getChooseLevelOrPokemon(String eggOrRaid);

	String getChooseTimeForRaidForHour(String eggOrRaid);

	String getChooseTimeForRaidForMinute(String eggOrRaid, String hour);

	String getChooseExactTimeForRaid(String eggOrRaid, String timePart);

	String getConfirmNewRaid(String eggOrRaid, String time, String pokemon, LocalDate date);

	String getChooseDateForRaid(String eggOrRaid, String time);

	String getParticipantsText(SortedSet<EventWithSubscribers> eventWithSubscribers);

	String getRaidMessagePokemonText(Gym fullGym);

	String formatLocaleTimeFromMillisSeconds(long l);

	String formatLocaleDateFromMillis(long l);

}
