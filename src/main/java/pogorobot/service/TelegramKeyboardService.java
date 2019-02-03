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

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

import pogorobot.entities.EventWithSubscribers;
import pogorobot.entities.Gym;
import pogorobot.entities.User;
import pogorobot.telegram.util.Type;

public interface TelegramKeyboardService {

	public static int END_OF_RAIDS_IN_HOURS = 21;

	public static int BEGIN_OF_RAIDS_IN_HOURS = 7;

	public static final String MINIVSELECT = "minivselect";

	public static final String SPACE = " ";

	public static final String DISTANCESELECT = "distanceselect";

	public static final String REMOVERAIDAREA = "removeraidarea";

	public static final String ADDRAIDAREA = "addraidarea";

	public static final String REMOVEAREA = "removearea";

	public static final String ADDAREA = "addarea";

	public static final String REMOVEIVAREA = "removeivarea";

	public static final String ADDIVAREA = "addivarea";

	// public static final String LOCATION = "location";

	public static final String ENABLERAIDS = "enableraids";

	public static final String DISABLERAIDS = "disableraids";

	public static final String RAIDLEVEL = "raidlevel";

	public static final String ADD = "add";

	public static final String REMOVE = "remove";

	public static final String EGG = "egg";

	public static final String RAID = "raid";

	public static final String CANCEL = "cancel";

	public static final String RAIDPOKEMONREMOVE = "raidpokemonremove";

	public static final String RAIDPOKEMONADD = "raidpokemonadd";

	public static final String POKEMON = "pokemon";

	public static final String RAIDPOKEMON = "raidpokemon";

	public static final String NEWRAIDOREGG = "newraidoregg";

	public static final String SIGNUPRAID = "signupraid";

	public static final String ADDONE = "addone";

	public static final String REMOVEONE = "removeone";

	public static final String BEGINNEWRAIDOREGG = "beginnewraidoregg";

	public static final String NEWRAIDATGYM = "newraidatgym";

	public static final String TIMEFORNEWRAIDATGYM = "timefornewraidatgym";

	public static final String TIMEFORNEWRAIDATGYMMINUTES = "timefornewraidatminutes";

	public static final String TIMEFORNEWRAIDATGYMMINUTE = "timefornewraidatminute";

	public static final String DATEFORNEWRAIDATGYM = "datefornewraidatminute";

	public static final String CONFIRMNEWRAIDATGYM = "confirmnewraidatgym";

	public static final String NEWRAIDATGYMCONFIRMED = "newraidatgymconfirmed";

	ReplyKeyboardMarkup getSettingsKeyboard(boolean raidAdmin);

	InlineKeyboardMarkup getPokemonKeyboard(int offsetOrPokemon, String command,
			List<Integer> pokemon, String subCommand, String backCommand, String backSubcommand, String dayOfYear);

	InlineKeyboardMarkup getLocationRemovKeyboard(User orCreateUser, Type pokemonFilter);

	InlineKeyboardMarkup getLocationAddKeyboard(User orCreateUser, Type pokemonFilter);

	ReplyKeyboard getLocationSettingKeyboard(User orCreateUser);

	InlineKeyboardMarkup getDistanceselectSettingKeyboard(String type);

	InlineKeyboardMarkup getRaidLevelSettingKeyboard(String command, String dayOfYear);

	// InlineKeyboardMarkup getRaidPokemonSettingKeyboard();
	//
	// InlineKeyboardMarkup getPokemonRemoveKeyboard(String string, int
	// offsetOrPokemon, String command,
	// String subcommand, List<Integer> pokemon);

	InlineKeyboardMarkup getRaidSettingKeyboard(boolean showRaidsActiceForUser);

	InlineKeyboardMarkup getIVSettingKeyboard(Double value);

	InlineKeyboardMarkup getPokemonSettingKeyboard(boolean raidPokemon);

	InlineKeyboardMarkup getMinIvSettingKeyboard();

	InlineKeyboardMarkup getChooseGymsAroundKeyboard(List<Gym> gymsAround, String raidOrEgg, String dayOfYear);

	InlineKeyboardMarkup getChooseRaidOrEggKeyboard();

	InlineKeyboardMarkup getRaidSignupKeyboard(SortedSet<EventWithSubscribers> eventWithSubscribers, String gymId);

	InlineKeyboardMarkup getLevelOrPokemonChoiceKeyboard(String eggOrRaid, String gymId, String offset,
			String dayOfYear);

	InlineKeyboardMarkup getChooseHoursForRaidKeyboard(String eggOrRaid, String internalGymId, String pokemonOrLevel,
			String dayOfYear);

	InlineKeyboardMarkup getChooseMinutesForRaidKeyboard(String eggOrRaid, String internalGymId, String pokemonOrLevel,
			String hour, String dayOfYear);

	InlineKeyboardMarkup getChooseExactTimeForRaidKeyboard(String eggOrRaid, String internalGymId,
			String pokemonOrLevel, String timePart, String dayOfYear);

	InlineKeyboardMarkup getChooseDateForRaidKeyboard(String eggOrRaid, String internalGymId,
			String pokemonOrLevel, String timePart);

	InlineKeyboardMarkup getConfirmNewRaidKeyboard(String eggOrRaid, String internalGymId, String pokemonOrLevel,
			String time, LocalDate date);

}
