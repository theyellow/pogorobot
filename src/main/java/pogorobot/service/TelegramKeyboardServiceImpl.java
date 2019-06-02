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
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ForceReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import pogorobot.entities.EventWithSubscribers;
import pogorobot.entities.Geofence;
import pogorobot.entities.Gym;
import pogorobot.entities.PossibleRaidPokemon;
import pogorobot.entities.User;
import pogorobot.service.db.FilterService;
import pogorobot.service.db.repositories.PossibleRaidPokemonRepository;
import pogorobot.telegram.util.Emoji;
import pogorobot.telegram.util.Type;

@Service("telegramKeyboardService")
public class TelegramKeyboardServiceImpl implements TelegramKeyboardService {

	@Autowired
	private FilterService filterService;

	// @Autowired
	// private UserService userService;

	@Autowired
	private TelegramTextService telegramTextService;

	@Autowired
	private PossibleRaidPokemonRepository possibleRaidPokemonRepository;

	@Override
	public ReplyKeyboardMarkup getSettingsKeyboard(boolean raidAdmin) {
		ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
		List<KeyboardRow> keyboardRows = new ArrayList<>();
		KeyboardRow menuRow = new KeyboardRow();
		KeyboardButton button = new KeyboardButton("Monster");
		KeyboardButton button1 = new KeyboardButton("Raids");
		KeyboardButton button2 = new KeyboardButton("IV");
		// KeyboardButton pokemonButton = new
		// KeyboardButton(Emoji.EARTH_GLOBE_EUROPE_AFRICA + "");
		KeyboardButton locationButton = new KeyboardButton();
		locationButton.setText("Ort " + Emoji.EARTH_GLOBE_EUROPE_AFRICA);
		locationButton.setRequestLocation(true);
		// pokemonButton.setText("Standort");
		menuRow.add(button);
		menuRow.add(button1);
		menuRow.add(button2);
		// menuRow.add(pokemonButton);
		menuRow.add(locationButton);
		keyboardRows.add(menuRow);

		if (raidAdmin) {
			KeyboardRow menuRow2 = new KeyboardRow();
			KeyboardButton addRaidButton = new KeyboardButton("Raid hinzufügen");
			// addRaidButton.setRequestLocation(true);
			menuRow2.add(addRaidButton);
			keyboardRows.add(menuRow2);
		}

		keyboard.setKeyboard(keyboardRows);
		keyboard.setResizeKeyboard(true);
		keyboard.setSelective(true);
		return keyboard;
	}

	public ForceReplyKeyboard getForceReplyKeyboard() {
		ForceReplyKeyboard keyboard = new ForceReplyKeyboard();
		keyboard.setSelective(true);
		return keyboard;
	}

	private InlineKeyboardMarkup createPokemonKeyboard(List<Integer> pokemons, String command, int offset,
			String subCommand, String backCommand, String backSubcommand, String dayOfYear) {
		List<List<InlineKeyboardButton>> keyboardArray = new ArrayList<>();
		List<List<Integer>> sortedList = new ArrayList<>();
		List<Integer> partOfList = new ArrayList<>();

		int numberOfPokemonOnKeyboard = 0;
		if (pokemons != null) {
			for (int j = 0; j < 6; j++) {
				for (int i = 0; i < 3; i++) {
					if (!pokemons.isEmpty()) {
						partOfList.add(pokemons.remove(0));
						numberOfPokemonOnKeyboard++;
					}
				}
				if (!partOfList.isEmpty()) {
					sortedList.add(partOfList);
					partOfList = new ArrayList<>();
				}
			}
		}
		InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
		for (List<Integer> list : sortedList) {
			List<InlineKeyboardButton> keyboardRow = new ArrayList<>();
			for (Integer pokemon : list) {
				String pokemonName = telegramTextService.getPokemonName(pokemon.toString());
				InlineKeyboardButton button = new InlineKeyboardButton(pokemonName);
				String data = command + SPACE;
				data += subCommand;

				data += SPACE;
				data += pokemon.toString();

				if (dayOfYear != null && !dayOfYear.equals("-1")) {
					data += SPACE;
					data += dayOfYear;
				}
				// data += pokemon.toString() + SPACE + offset;
				button.setCallbackData(data);
				keyboardRow.add(button);
			}
			keyboardArray.add(keyboardRow);
		}
		List<InlineKeyboardButton> menuRow = new ArrayList<>();

		if (offset != 0) {
			InlineKeyboardButton backwardButton = new InlineKeyboardButton(Emoji.BLACK_LEFTWARDS_ARROW + " Zurück");
			int newOffset = offset - 18;
			// int newOffset = offset - numberOfPokemonOnKeyboard;
			newOffset = newOffset < 0 ? 0 : newOffset;
			// String callbackData = command + " keyboard " + newOffset;
			String callbackData = backCommand + SPACE + backSubcommand + SPACE + newOffset;
			if (dayOfYear != null && !dayOfYear.equals("-1")) {
				callbackData += SPACE;
				callbackData += dayOfYear;
			}
			backwardButton.setCallbackData(callbackData);
			menuRow.add(backwardButton);
		}

		// TODO: compare with number of mons left or offset, now there can be a
		// forward/backward-button if there are exactly 18 mon
		if (numberOfPokemonOnKeyboard >= 18) {
			InlineKeyboardButton forwardButton = new InlineKeyboardButton("Weiter " + Emoji.BLACK_RIGHTWARDS_ARROW);
			// String callbackData = command + " keyboard " + (offset +
			// numberOfPokemonOnKeyboard);
			String callbackData = backCommand + SPACE + backSubcommand + SPACE + (offset + numberOfPokemonOnKeyboard);
			if (dayOfYear != null && !dayOfYear.equals("-1")) {
				callbackData += SPACE;
				callbackData += dayOfYear;
			}
			forwardButton.setCallbackData(callbackData);
			menuRow.add(forwardButton);
		}

		if (!menuRow.isEmpty()) {
			keyboardArray.add(menuRow);
		}

		keyboard.setKeyboard(keyboardArray);
		return keyboard;
	}

	@Override
	public InlineKeyboardMarkup getPokemonSettingKeyboard(boolean raidPokemon) {
		InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
		List<List<InlineKeyboardButton>> keyboardArray = new ArrayList<>();

		// 1st row
		List<InlineKeyboardButton> addRemoveRow = new ArrayList<>();
		InlineKeyboardButton addButton = new InlineKeyboardButton();
		String text = "Zur Liste hinzufügen";
		addButton.setText(text);
		String callbackDataAdd = raidPokemon ? RAIDPOKEMONADD : ADD;
		addButton.setCallbackData(callbackDataAdd);
		addRemoveRow.add(addButton);
		InlineKeyboardButton removeButton = new InlineKeyboardButton();
		String textRemove = "Von der Liste entfernen";
		removeButton.setText(textRemove);
		String callbackDataRemove = raidPokemon ? RAIDPOKEMONREMOVE : REMOVE;
		removeButton.setCallbackData(callbackDataRemove);
		addRemoveRow.add(removeButton);
		keyboardArray.add(addRemoveRow);

		// 2nd row
		if (!raidPokemon) {
			List<InlineKeyboardButton> areaRow = new ArrayList<>();
			InlineKeyboardButton addMonButton = new InlineKeyboardButton();
			addMonButton.setText(Emoji.HEAVY_PLUS_SIGN + "Mon-Gebiet");
			addMonButton.setCallbackData(ADDAREA);
			areaRow.add(addMonButton);
			InlineKeyboardButton removeMonButton = new InlineKeyboardButton();
			removeMonButton.setText(Emoji.HEAVY_MINUS_SIGN + "Mon-Gebiet");
			removeMonButton.setCallbackData(REMOVEAREA);
			areaRow.add(removeMonButton);
			keyboardArray.add(areaRow);

			List<InlineKeyboardButton> keyboardRow = new ArrayList<>();
			InlineKeyboardButton levelButton = new InlineKeyboardButton();
			levelButton.setText("Umkreis");
			levelButton.setCallbackData(DISTANCESELECT + SPACE + "mon");
			keyboardRow.add(levelButton);
			keyboardArray.add(keyboardRow);
		}

		keyboard.setKeyboard(keyboardArray);
		return keyboard;
	}

	// @Override
	// public InlineKeyboardMarkup getPokemonRemoveKeyboard(String telegramId, int
	// offset, String command,
	// String subcommand, List<Integer> pokemon) {
	// // List<Integer> pokemon;
	// // if (RAIDPOKEMON.equals(subcommand)) {
	// // pokemon = filterService.getFilterRaidsForTelegramId(telegramId);
	// // } else {
	// // pokemon =
	// // userService.getOrCreateUser(telegramId).getUserFilter().getPokemons();
	// // }
	// return getPokemonKeyboard(telegramId, offset, command, pokemon, subcommand);
	// }

	@Override
	public InlineKeyboardMarkup getPokemonKeyboard(int offset, String command, List<Integer> pokemon, String subCommand,
			String backCommand, String backSubcommand, String dayOfYear) {
		for (int i = 0; i < offset; i++) {
			if (!pokemon.isEmpty()) {
				pokemon.remove(0);
			}
		}
		InlineKeyboardMarkup keyboard = createPokemonKeyboard(pokemon, command, offset, subCommand, backCommand,
				backSubcommand, dayOfYear);
		return keyboard;
	}

	@Override
	public InlineKeyboardMarkup getRaidLevelSettingKeyboard(String command, String dayOfYear) {
		InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
		List<List<InlineKeyboardButton>> keyboardArray = new ArrayList<>();

		List<InlineKeyboardButton> keyboardRow = new ArrayList<>();
		for (int i = 1; i <= 5; i++) {
			InlineKeyboardButton levelButton = new InlineKeyboardButton();
			levelButton.setText(Integer.toString(i));
			levelButton.setCallbackData(command + SPACE + i + SPACE + dayOfYear);
			keyboardRow.add(levelButton);
		}

		// level '-' means 'no raids by level, so it is in filter-settings level 6 ;)
		InlineKeyboardButton levelButton = new InlineKeyboardButton();
		levelButton.setText("-");
		levelButton.setCallbackData(command + SPACE + "6" + SPACE + dayOfYear);
		keyboardRow.add(levelButton);

		keyboardArray.add(keyboardRow);

		keyboard.setKeyboard(keyboardArray);
		return keyboard;
	}

	@Override
	public InlineKeyboardMarkup getDistanceselectSettingKeyboard(String type) {
		InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
		List<List<InlineKeyboardButton>> keyboardArray = new ArrayList<>();

		// 1st row
		List<InlineKeyboardButton> keyboardRow = new ArrayList<>();
		InlineKeyboardButton distance1Button = new InlineKeyboardButton();
		distance1Button.setText("200 m");
		distance1Button.setCallbackData(DISTANCESELECT + SPACE + type + SPACE + "0.2");
		keyboardRow.add(distance1Button);

		InlineKeyboardButton distance2Button = new InlineKeyboardButton();
		distance2Button.setText("500 m");
		distance2Button.setCallbackData(DISTANCESELECT + SPACE + type + SPACE + "0.5");
		keyboardRow.add(distance2Button);

		InlineKeyboardButton distance3Button = new InlineKeyboardButton();
		distance3Button.setText("1 km");
		distance3Button.setCallbackData(DISTANCESELECT + SPACE + type + SPACE + "1");
		keyboardRow.add(distance3Button);

		InlineKeyboardButton distance4Button = new InlineKeyboardButton();
		distance4Button.setText("2 km");
		distance4Button.setCallbackData(DISTANCESELECT + SPACE + type + SPACE + "2");
		keyboardRow.add(distance4Button);

		keyboardArray.add(keyboardRow);

		// 2nd row
		List<InlineKeyboardButton> keyboardRow2 = new ArrayList<>();
		InlineKeyboardButton distance5Button = new InlineKeyboardButton();
		distance5Button.setText("5 km");
		distance5Button.setCallbackData(DISTANCESELECT + SPACE + type + SPACE + "5");
		keyboardRow2.add(distance5Button);

		InlineKeyboardButton distance6Button = new InlineKeyboardButton();
		distance6Button.setText("10 km");
		distance6Button.setCallbackData(DISTANCESELECT + SPACE + type + SPACE + "10");
		keyboardRow2.add(distance6Button);

		InlineKeyboardButton distance7Button = new InlineKeyboardButton();
		distance7Button.setText("alles");
		distance7Button.setCallbackData(DISTANCESELECT + SPACE + type + SPACE + "10000");
		keyboardRow2.add(distance7Button);

		keyboardArray.add(keyboardRow2);

		keyboard.setKeyboard(keyboardArray);
		return keyboard;
	}

	@Override
	public InlineKeyboardMarkup getMinIvSettingKeyboard() {
		InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
		List<List<InlineKeyboardButton>> keyboardArray = new ArrayList<>();

		// 1st row
		List<InlineKeyboardButton> keyboardRow = new ArrayList<>();
		InlineKeyboardButton min89Button = new InlineKeyboardButton();
		min89Button.setText("\"89 %\"");
		min89Button.setCallbackData(MINIVSELECT + SPACE + "88.0");
		keyboardRow.add(min89Button);

		InlineKeyboardButton min91Button = new InlineKeyboardButton();
		min91Button.setText("\"91 %\"");
		min91Button.setCallbackData(MINIVSELECT + SPACE + "90.0");
		keyboardRow.add(min91Button);

		InlineKeyboardButton min93Button = new InlineKeyboardButton();
		min93Button.setText("\"93 %\"");
		min93Button.setCallbackData(MINIVSELECT + SPACE + "93.0");
		keyboardRow.add(min93Button);

		keyboardArray.add(keyboardRow);

		// 2nd row
		List<InlineKeyboardButton> keyboardRow2 = new ArrayList<>();
		InlineKeyboardButton min96Button = new InlineKeyboardButton();
		min96Button.setText("\"96 %\"");
		min96Button.setCallbackData(MINIVSELECT + SPACE + "95.0");
		keyboardRow2.add(min96Button);

		InlineKeyboardButton min98Button = new InlineKeyboardButton();
		min98Button.setText("\"98 %\"");
		min98Button.setCallbackData(MINIVSELECT + SPACE + "97.0");
		keyboardRow2.add(min98Button);

		InlineKeyboardButton min100Button = new InlineKeyboardButton();
		min100Button.setText("\"100 %\"");
		min100Button.setCallbackData(MINIVSELECT + SPACE + "99.0");
		keyboardRow2.add(min100Button);

		keyboardArray.add(keyboardRow2);

		keyboard.setKeyboard(keyboardArray);
		return keyboard;
	}

	@Override
	public ReplyKeyboardMarkup getLocationSettingKeyboard(User user) {
		ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
		List<KeyboardRow> keyboardArray = new ArrayList<>();
		// 1st row
		KeyboardRow keyboardRow = new KeyboardRow();
		KeyboardButton locationButton = new KeyboardButton();
		locationButton.setText(Emoji.EARTH_GLOBE_EUROPE_AFRICA + " Live " + Emoji.EARTH_GLOBE_EUROPE_AFRICA);
		locationButton.setRequestLocation(true);
		keyboardRow.add(locationButton);
		keyboardArray.add(keyboardRow);

		// 2nd row
		KeyboardRow backRow = new KeyboardRow();
		KeyboardButton backButton = new KeyboardButton();
		backButton.setText("Zurück zum Start");
		// locationButton.setRequestLocation(true);
		backRow.add(backButton);
		keyboardArray.add(backRow);

		keyboard.setKeyboard(keyboardArray);
		keyboard.setResizeKeyboard(true);
		return keyboard;
	}

	private InlineKeyboardMarkup createKeyboardWithAreas(List<String> geofences, boolean remove, Type pokemonFilter) {
		InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
		List<List<InlineKeyboardButton>> keyboardArray = new ArrayList<>();
		// 1st row
		for (String geofenceName : geofences) {
			List<InlineKeyboardButton> keyboardRow = new ArrayList<>();
			InlineKeyboardButton locationButton = new InlineKeyboardButton();
			String locationText = remove ? Emoji.HEAVY_MINUS_SIGN + SPACE : Emoji.HEAVY_PLUS_SIGN + SPACE;
			locationText += geofenceName;
			locationButton.setText(locationText);
			String callbackData = "";
			if (remove) {
				switch (pokemonFilter) {
				case POKEMON:
					callbackData = REMOVEAREA;
					break;
				case RAID:
					callbackData = REMOVERAIDAREA;
					break;
				case IV:
					callbackData = REMOVEIVAREA;
					break;
				default:
					break;
				}
			} else {
				switch (pokemonFilter) {
				case POKEMON:
					callbackData = ADDAREA;
					break;
				case RAID:
					callbackData = ADDRAIDAREA;
					break;
				case IV:
					callbackData = ADDIVAREA;
					break;
				default:
					break;
				}
			}
			callbackData += SPACE + geofenceName;
			locationButton.setCallbackData(callbackData);
			keyboardRow.add(locationButton);
			keyboardArray.add(keyboardRow);
		}

		keyboard.setKeyboard(keyboardArray);
		return keyboard;
	}

	@Override
	public InlineKeyboardMarkup getRaidSettingKeyboard(boolean enabled) {
		InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
		List<List<InlineKeyboardButton>> keyboardArray = new ArrayList<>();

		// 1st row
		List<InlineKeyboardButton> keyboardRow = new ArrayList<>();
		InlineKeyboardButton levelButton = new InlineKeyboardButton();
		levelButton.setText("Level");
		levelButton.setCallbackData(RAIDLEVEL);
		keyboardRow.add(levelButton);
		InlineKeyboardButton pokemonButton = new InlineKeyboardButton();
		pokemonButton.setText("Pokémon");
		pokemonButton.setCallbackData(RAIDPOKEMON);
		keyboardRow.add(pokemonButton);
		keyboardArray.add(keyboardRow);

		// 2nd row
		// if (raidPokemon) {
		List<InlineKeyboardButton> raidAreaRow = new ArrayList<>();
		InlineKeyboardButton addRaidButton = new InlineKeyboardButton();
		addRaidButton.setText(Emoji.HEAVY_PLUS_SIGN + " Raid-Gebiet");
		addRaidButton.setCallbackData(ADDRAIDAREA);
		raidAreaRow.add(addRaidButton);
		InlineKeyboardButton removeRaidButton = new InlineKeyboardButton();
		removeRaidButton.setText(Emoji.HEAVY_MINUS_SIGN + "Raid-Gebiet");
		removeRaidButton.setCallbackData(REMOVERAIDAREA);
		raidAreaRow.add(removeRaidButton);
		keyboardArray.add(raidAreaRow);

		List<InlineKeyboardButton> keyboardRowDistance = new ArrayList<>();
		InlineKeyboardButton distanceButton = new InlineKeyboardButton();
		distanceButton.setText("Umkreis");
		distanceButton.setCallbackData(DISTANCESELECT + SPACE + "raid");
		keyboardRowDistance.add(distanceButton);
		keyboardArray.add(keyboardRowDistance);
		// } else {
		// List<InlineKeyboardButton> areaRow = new ArrayList<>();
		// InlineKeyboardButton addMonButton = new InlineKeyboardButton();
		// addMonButton.setText(Emoji.HEAVY_PLUS_SIGN + "Mon-Gebiet");
		// addMonButton.setCallbackData(ADDAREA);
		// areaRow.add(addMonButton);
		// InlineKeyboardButton removeMonButton = new InlineKeyboardButton();
		// removeMonButton.setText(Emoji.HEAVY_MINUS_SIGN + "Mon-Gebiet");
		// removeButton.setCallbackData(REMOVEAREA);
		// areaRow.add(removeButton);
		// keyboardArray.add(areaRow);
		// }
		// 2nd row
		List<InlineKeyboardButton> enableRow = new ArrayList<>();
		InlineKeyboardButton enableButton = new InlineKeyboardButton();
		String enable = enabled ? "Deaktiveren" : "Aktivieren";
		enableButton.setText(enable);
		String callbackData = enabled ? DISABLERAIDS : ENABLERAIDS;
		enableButton.setCallbackData(callbackData);
		enableRow.add(enableButton);
		keyboardArray.add(enableRow);

		keyboard.setKeyboard(keyboardArray);
		return keyboard;
	}

	@Override
	public InlineKeyboardMarkup getIVSettingKeyboard(Double value) {
		InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
		List<List<InlineKeyboardButton>> keyboardArray = new ArrayList<>();

		// 2nd row
		List<InlineKeyboardButton> raidAreaRow = new ArrayList<>();
		InlineKeyboardButton addRaidButton = new InlineKeyboardButton();
		addRaidButton.setText(Emoji.HEAVY_PLUS_SIGN + " IV-Gebiet");
		addRaidButton.setCallbackData(ADDIVAREA);
		raidAreaRow.add(addRaidButton);
		InlineKeyboardButton removeRaidButton = new InlineKeyboardButton();
		removeRaidButton.setText(Emoji.HEAVY_MINUS_SIGN + "IV-Gebiet");
		removeRaidButton.setCallbackData(REMOVEIVAREA);
		raidAreaRow.add(removeRaidButton);
		keyboardArray.add(raidAreaRow);

		// 3rd row
		List<InlineKeyboardButton> keyboardRow = new ArrayList<>();
		InlineKeyboardButton distanceButton = new InlineKeyboardButton();
		distanceButton.setText("Umkreis");
		distanceButton.setCallbackData(DISTANCESELECT + SPACE + "iv");
		keyboardRow.add(distanceButton);
		keyboardArray.add(keyboardRow);

		// 1st row
		List<InlineKeyboardButton> ivRow = new ArrayList<>();
		InlineKeyboardButton ivButton = new InlineKeyboardButton();
		String ivSelect = value < 100.0 ? "IV Minimum .... ändern" : "IV-Minimum einstellen";
		ivButton.setText(ivSelect);
		ivButton.setCallbackData(MINIVSELECT);
		ivRow.add(ivButton);
		keyboardArray.add(ivRow);
		if (value < 100.0) {
			// 3rd row
			List<InlineKeyboardButton> keyboardRow3 = new ArrayList<>();
			InlineKeyboardButton minIvOffButton = new InlineKeyboardButton();
			minIvOffButton.setText("IV-basierte Meldungen aus");
			minIvOffButton.setCallbackData(MINIVSELECT + SPACE + "101.0");
			keyboardRow3.add(minIvOffButton);
			keyboardArray.add(keyboardRow3);
		}

		keyboard.setKeyboard(keyboardArray);
		return keyboard;
	}

	@Override
	public InlineKeyboardMarkup getLocationRemovKeyboard(User user, Type pokemonFilter) {
		List<Geofence> geofencesForUser = filterService.getGeofencesForUser(user, pokemonFilter);
		return getLocationAreaKeyboard(geofencesForUser, true, pokemonFilter);
	}

	@Override
	public InlineKeyboardMarkup getLocationAddKeyboard(User user, Type pokemonFilter) {
		List<Geofence> geofences = getGeofencesNotYetAdded(user, pokemonFilter);
		return getLocationAreaKeyboard(geofences, false, pokemonFilter);
	}

	private InlineKeyboardMarkup getLocationAreaKeyboard(List<Geofence> geofences, boolean remove, Type pokemonFilter) {
		List<String> geofencesName = new ArrayList<>();
		for (Geofence geofence : geofences) {
			geofencesName.add(geofence.getGeofenceName());
		}
		return createKeyboardWithAreas(geofencesName, remove, pokemonFilter);
	}

	private List<Geofence> getGeofencesNotYetAdded(User user, Type pokemonFilter) {
		List<Geofence> geofences = new ArrayList<>();
		geofences = filterService.getAllGeofences().stream().collect(Collectors.toList());
		List<Geofence> userGeofences = filterService.getGeofencesForUser(user, pokemonFilter);
		for (Geofence geofence : userGeofences) {
			geofences.remove(geofence);
		}
		return geofences;
	}

	@Override
	public InlineKeyboardMarkup getChooseGymsAroundKeyboard(List<Gym> gymsAround, String raidOrEgg, String dayOfYear) {
		String backCallbackCommand = BEGINNEWRAIDOREGG;
		InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
		List<String> gymIds = new ArrayList<>();
		List<List<InlineKeyboardButton>> keyboardArray = new ArrayList<>();
		gymsAround.stream().map(gym -> gym.getId() + " " + gym.getName()).forEach(idAndName -> {
			String id = idAndName.substring(0, idAndName.indexOf(" ")).trim();
			String name = idAndName.substring(idAndName.indexOf(" ") + 1).trim();
			gymIds.add(id);
			List<InlineKeyboardButton> gymRow = new ArrayList<>();
			InlineKeyboardButton button = new InlineKeyboardButton(gymIds.size() + ". " + name);
			button.setCallbackData(NEWRAIDATGYM + SPACE + id + SPACE + raidOrEgg + SPACE + "0"
					+ (dayOfYear == null ? "" : SPACE + dayOfYear));
			gymRow.add(button);
			keyboardArray.add(gymRow);
		});

		// echoMessage.setText(telegramTextService.getGymsOrEggChoiceText());
		// echoMessage.setReplyMarkup(telegramKeyboardService.getChooseRaidOrEggKeyboard());

		List<InlineKeyboardButton> backRow = new ArrayList<>();
		InlineKeyboardButton back = new InlineKeyboardButton("Zurück");
		back.setCallbackData(backCallbackCommand);
		backRow.add(back);
		keyboardArray.add(backRow);
		keyboard.setKeyboard(keyboardArray);
		return keyboard;
	}

	@Override
	public InlineKeyboardMarkup getChooseRaidOrEggKeyboard() {
		InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
		List<List<InlineKeyboardButton>> keyboardArray = new ArrayList<>();
		List<InlineKeyboardButton> keyboardRow = new ArrayList<>();
		InlineKeyboardButton eggButton = new InlineKeyboardButton("Ei");
		LocalDate date = LocalDate.now();
		eggButton.setCallbackData(NEWRAIDOREGG + SPACE + EGG + SPACE + date.getDayOfYear());
		keyboardRow.add(eggButton);
		InlineKeyboardButton raidButton = new InlineKeyboardButton("Raid");
		raidButton.setCallbackData(NEWRAIDOREGG + SPACE + RAID + SPACE + date.getDayOfYear());
		keyboardRow.add(raidButton);
		InlineKeyboardButton exRaidButton = new InlineKeyboardButton("Ex-Raid");
		exRaidButton.setCallbackData(NEWRAIDOREGG + SPACE + RAID);
		keyboardRow.add(exRaidButton);
		keyboardArray.add(keyboardRow);

		// String backCallbackCommand = "";
		// keyboardRow = new ArrayList<>();
		// InlineKeyboardButton back = new InlineKeyboardButton("Zurück");
		// back.setCallbackData(backCallbackCommand);
		// keyboardRow.add(back);
		// keyboardArray.add(keyboardRow);

		keyboard.setKeyboard(keyboardArray);
		return keyboard;
	}

	@Override
	public InlineKeyboardMarkup getLevelOrPokemonChoiceKeyboard(String eggOrRaid, String gymId, String offset,
			String dayOfYear) {
		InlineKeyboardMarkup keyboard = null;
		if (EGG.equals(eggOrRaid)) {
			keyboard = getRaidLevelSettingKeyboard(
					TIMEFORNEWRAIDATGYM + SPACE + gymId + SPACE + EGG, dayOfYear);
		} else if (RAID.equals(eggOrRaid)) {
			String subcommand = gymId + SPACE + RAID;
			keyboard = getPokemonKeyboard(Integer.valueOf(offset), TIMEFORNEWRAIDATGYM,
					getPossibleRaidPokemon(dayOfYear), subcommand, NEWRAIDATGYM, subcommand, dayOfYear);
			// keyboard = createPokemonKeyboard(getPossibleRaidPokemon(),
			// TIMEFORNEWRAIDATGYM, Integer.valueOf(offset),
			// gymId + SPACE + RAID, NEWRAIDATGYM);
		}
		return keyboard;
	}

	/**
	 * Lookup possible raid bosses in database, also depending on "ex-raid or normal
	 * raid" <br/>
	 * <b>ATTENTION:<br/>
	 * Hacky...</b> atm there is Mew2 hardconfigured for ex-raids, this needs to be
	 * in database (because i don't know where to get the information automatically)
	 * <br/>
	 * <b>Another hack:</b> for normal filter choice there is special meaning of
	 * "-1"
	 * 
	 * @param dayOfYear
	 *            <b>also HACKY:</b> <code>null</code> means ExRaid, "-1" means get
	 *            raid-bosses for filter choice, other values mean normal raids on
	 *            day x
	 * 
	 * @return List of possible raid bosses
	 */
	private List<Integer> getPossibleRaidPokemon(String dayOfYear) {
		List<Integer> possibleRaidPokemonResult = new ArrayList<>();
		Map<Integer, Integer> pokemonWithLevel = new HashMap<>();
		Iterable<PossibleRaidPokemon> all = possibleRaidPokemonRepository.findAll();
		for (PossibleRaidPokemon possibleRaidPokemon : all) {
			if (!pokemonWithLevel.containsKey(possibleRaidPokemon.getPokemonId())) {
				// Hack: to know if it should be for an ExRaid we have a special meaning of null
				// here
				if (dayOfYear == null) {
					possibleRaidPokemonResult = new ArrayList<>();
					// Next hack: manually configured Deoxys as ExRaidCounter
					// TODO: make configurable
					if (possibleRaidPokemon.getPokemonId().equals(386)) {
						pokemonWithLevel.put(possibleRaidPokemon.getPokemonId(), possibleRaidPokemon.getLevel());
					}
				} else if (dayOfYear.equals("-1")) {
					// Next hack: manually set all raid bosses for normal "filter choice"
					// TODO: make configurable
					pokemonWithLevel.put(possibleRaidPokemon.getPokemonId(), possibleRaidPokemon.getLevel());
				} else {
					// Next hack: manually configured Deoxys as ExRaidCounter
					// TODO: make configurable
					if (!possibleRaidPokemon.getPokemonId().equals(386)) {
						pokemonWithLevel.put(possibleRaidPokemon.getPokemonId(), possibleRaidPokemon.getLevel());
					}
				}
			}
		}
		possibleRaidPokemonResult = pokemonWithLevel.entrySet().stream().map(entry -> entry.getKey())
				.sorted((y, x) -> pokemonWithLevel.get(x).compareTo(pokemonWithLevel.get(y)))
				.collect(Collectors.toList());

		return possibleRaidPokemonResult;
	}

	@Override
	public InlineKeyboardMarkup getChooseHoursForRaidKeyboard(String eggOrRaid, String internalGymId,
			String pokemonOrLevel, String dayOfYear) {

		String dayOfYearEnding = dayOfYear == null ? "" : SPACE + dayOfYear;
		String backCallbackCommand = NEWRAIDOREGG + SPACE + eggOrRaid + dayOfYearEnding;
		// String backCallbackCommand = NEWRAIDATGYM + SPACE + eggOrRaid + SPACE +
		// internalGymId;

		InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
		List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
		List<InlineKeyboardButton> buttonRow = new ArrayList<>();
		for (int i = BEGIN_OF_RAIDS_IN_HOURS; i <= END_OF_RAIDS_IN_HOURS; i++) {
			int numberOfButton = i - BEGIN_OF_RAIDS_IN_HOURS;
			InlineKeyboardButton hour = new InlineKeyboardButton();
			String twoDigitHour = i < 10 ? " " + i : String.valueOf(i);
			hour.setText(twoDigitHour + ":__");
			String callbackData = "";
			callbackData += TIMEFORNEWRAIDATGYMMINUTES + SPACE + internalGymId + SPACE + eggOrRaid + SPACE
					+ pokemonOrLevel + SPACE + i + dayOfYearEnding;
			hour.setCallbackData(callbackData);
			buttonRow.add(hour);
			if (numberOfButton % 4 == 3 || i == END_OF_RAIDS_IN_HOURS) {
				keyboard.add(buttonRow);
				buttonRow = new ArrayList<>();
			}
		}
		// buttonRow = new ArrayList<>();
		InlineKeyboardButton back = new InlineKeyboardButton("Zurück");
		back.setCallbackData(backCallbackCommand);
		buttonRow.add(back);
		keyboard.add(buttonRow);
		keyboardMarkup.setKeyboard(keyboard);
		return keyboardMarkup;
	}

	@Override
	public InlineKeyboardMarkup getChooseMinutesForRaidKeyboard(String eggOrRaid, String internalGymId,
			String pokemonOrLevel, String hour, String dayOfYear) {
		InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
		List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
		List<InlineKeyboardButton> buttonRow = new ArrayList<>();
		String gymIdEnding = SPACE + internalGymId;
		String eggOrRaidEnding = SPACE + eggOrRaid;
		String pokemonOrLevelEnding = SPACE + pokemonOrLevel;
		String hourEnding = SPACE + hour;
		String dayOfYearEnding = dayOfYear == null ? "" : SPACE + dayOfYear;
		for (int i = 0; i < 6; i++) {
			InlineKeyboardButton minutes = new InlineKeyboardButton();
			String minuteTenthsEnding = ":" + i;
			minutes.setText(hour + minuteTenthsEnding + "_");
			String callbackData = "";
			callbackData += TIMEFORNEWRAIDATGYMMINUTE + gymIdEnding + pokemonOrLevelEnding + eggOrRaidEnding
					+ hourEnding + minuteTenthsEnding + dayOfYearEnding;
			minutes.setCallbackData(callbackData);
			buttonRow.add(minutes);
			if (i % 3 == 2) {
				keyboard.add(buttonRow);
				buttonRow = new ArrayList<>();
			}
		}
		buttonRow = new ArrayList<>();
		InlineKeyboardButton back = new InlineKeyboardButton("Zurück");
		back.setCallbackData(
				TIMEFORNEWRAIDATGYM + gymIdEnding + eggOrRaidEnding + pokemonOrLevelEnding + dayOfYearEnding);
		buttonRow.add(back);
		keyboard.add(buttonRow);
		keyboardMarkup.setKeyboard(keyboard);
		return keyboardMarkup;
	}

	@Override
	public InlineKeyboardMarkup getChooseDateForRaidKeyboard(String eggOrRaid, String internalGymId,
			String pokemonOrLevel, String time) {
		InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
		List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
		List<InlineKeyboardButton> buttonRow = new ArrayList<>();
		InlineKeyboardButton today = new InlineKeyboardButton("Heute");
		LocalDate date = LocalDate.now();
		DateTimeFormatter formatter = new DateTimeFormatterBuilder().appendValue(ChronoField.DAY_OF_MONTH)
				.appendLiteral(".").appendValue(ChronoField.MONTH_OF_YEAR, 2).appendLiteral(".")
				.appendValue(ChronoField.YEAR).toFormatter();
		today.setCallbackData(CONFIRMNEWRAIDATGYM + SPACE + internalGymId + SPACE + pokemonOrLevel + SPACE + eggOrRaid
				+ SPACE + time + SPACE + date.getDayOfYear());
		buttonRow.add(today);
		// keyboard.add(buttonRow);
		// buttonRow = new ArrayList<>();
		for (long i = 1; i < 13; i++) {
			InlineKeyboardButton dateKeyboardButton = new InlineKeyboardButton();
			String fullDate = date.plusDays(i).format(formatter);
			dateKeyboardButton.setText(fullDate);
			String callbackData = "";
			callbackData += CONFIRMNEWRAIDATGYM + SPACE + internalGymId + SPACE + pokemonOrLevel + SPACE + eggOrRaid
					+ SPACE + time + SPACE + date.plusDays(i).getDayOfYear();
			dateKeyboardButton.setCallbackData(callbackData);
			buttonRow.add(dateKeyboardButton);
			if (i % 2 == 1) {
				keyboard.add(buttonRow);
				buttonRow = new ArrayList<>();
			}
		}
		buttonRow = new ArrayList<>();
		InlineKeyboardButton back = new InlineKeyboardButton("Zurück");
		back.setCallbackData(TIMEFORNEWRAIDATGYMMINUTE + SPACE + internalGymId + SPACE + pokemonOrLevel + SPACE
				+ eggOrRaid + SPACE + time.substring(0, time.length() - 1));
		buttonRow.add(back);
		keyboard.add(buttonRow);
		keyboardMarkup.setKeyboard(keyboard);
		return keyboardMarkup;
	}

	@Override
	public InlineKeyboardMarkup getConfirmNewRaidKeyboard(String eggOrRaid, String internalGymId, String pokemonOrLevel,
			String time, LocalDate date) {
		InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
		List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
		List<InlineKeyboardButton> buttonRow = new ArrayList<>();
		InlineKeyboardButton yes = new InlineKeyboardButton("Ja");
		yes.setCallbackData(NEWRAIDATGYMCONFIRMED + SPACE + internalGymId + SPACE + pokemonOrLevel + SPACE + eggOrRaid
				+ SPACE + time + SPACE + date.getDayOfYear());
		buttonRow.add(yes);
		InlineKeyboardButton yesShare = new InlineKeyboardButton("Ja, teilen");
		yesShare.setCallbackData(NEWRAIDATGYMCONFIRMED + SPACE + internalGymId + SPACE + pokemonOrLevel + SPACE
				+ eggOrRaid + SPACE + time + SPACE + date.getDayOfYear() + SPACE + "share");
		buttonRow.add(yesShare);
		InlineKeyboardButton no = new InlineKeyboardButton("Nein, zurück!");
		// TODO: hacky configuration for ex-raids go on:
		String backCallbackData = null;
		if (pokemonOrLevel.equals("151")) {
			backCallbackData = DATEFORNEWRAIDATGYM + SPACE + internalGymId + SPACE + pokemonOrLevel + SPACE + eggOrRaid
				+ SPACE + time;
		} else {
			backCallbackData = TIMEFORNEWRAIDATGYMMINUTE + SPACE + internalGymId + SPACE + pokemonOrLevel + SPACE
					+ eggOrRaid + SPACE + time.substring(0, time.length() - 1) + SPACE + date.getDayOfYear();
		}

		no.setCallbackData(backCallbackData);
		// TIMEFORNEWRAIDATGYMMINUTE + SPACE + internalGymId + SPACE + pokemonOrLevel +
		// SPACE
		// + eggOrRaid + SPACE + time.substring(0, 4)
		buttonRow.add(no);
		keyboard.add(buttonRow);
		keyboardMarkup.setKeyboard(keyboard);
		return keyboardMarkup;
	}

	@Override
	public InlineKeyboardMarkup getChooseExactTimeForRaidKeyboard(String eggOrRaid, String internalGymId,
			String pokemonOrLevel, String timePart, String dayOfYear) {
		InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
		List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
		List<InlineKeyboardButton> buttonRow = new ArrayList<>();
		String dayOfYearEnding = dayOfYear == null ? "" : SPACE + dayOfYear;
		String gymIdEnding = SPACE + internalGymId;
		String pokemonOrLevelEnding = SPACE + pokemonOrLevel;
		String eggOrRaidEnding = SPACE + eggOrRaid;
		for (int i = 0; i < 10; i++) {
			InlineKeyboardButton minutes = new InlineKeyboardButton();
			minutes.setText(timePart + i);
			String callbackData = "";
			String exactTimeEnding = SPACE + timePart + i;
			String standardCallbackArguments = gymIdEnding + pokemonOrLevelEnding + eggOrRaidEnding + exactTimeEnding;
			if (dayOfYear != null) {
				callbackData += CONFIRMNEWRAIDATGYM + standardCallbackArguments + dayOfYearEnding;
			} else {
				callbackData += DATEFORNEWRAIDATGYM + standardCallbackArguments;
			}
			minutes.setCallbackData(callbackData);
			buttonRow.add(minutes);
			if (i % 3 == 2) {
				keyboard.add(buttonRow);
				buttonRow = new ArrayList<>();
			}
		}
		keyboard.add(buttonRow);
		buttonRow = new ArrayList<>();
		InlineKeyboardButton back = new InlineKeyboardButton("Zurück");
		String backTimePartEnding = SPACE + timePart.substring(0, timePart.length() - 2);
		String backCallbackData = TIMEFORNEWRAIDATGYMMINUTES + gymIdEnding + eggOrRaidEnding + pokemonOrLevelEnding
				+ backTimePartEnding + dayOfYearEnding;
		back.setCallbackData(backCallbackData);
		buttonRow.add(back);
		keyboard.add(buttonRow);

		keyboardMarkup.setKeyboard(keyboard);
		return keyboardMarkup;
	}

	@Override
	public InlineKeyboardMarkup getRaidSignupKeyboard(SortedSet<EventWithSubscribers> eventWithSubscribers,
			String gymId) {
		InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
		List<List<InlineKeyboardButton>> keyboardArray = new ArrayList<>();
		List<InlineKeyboardButton> keyboardRow = new ArrayList<>();

		for (EventWithSubscribers event : eventWithSubscribers) {
			if (keyboardRow.size() == 4) {
				keyboardArray.add(keyboardRow);
				keyboardRow = new ArrayList<>();
			}
			String time = event.getTime();
			InlineKeyboardButton raidButton = new InlineKeyboardButton(time);
			raidButton.setCallbackData(SIGNUPRAID + SPACE + gymId + SPACE + time);
			keyboardRow.add(raidButton);
		}

		keyboardArray.add(keyboardRow);

		String cancelCallbackCommand = SIGNUPRAID + SPACE + CANCEL + SPACE + gymId;
		keyboardRow = new ArrayList<>();

		InlineKeyboardButton cancelButton = new InlineKeyboardButton("Absagen");
		cancelButton.setCallbackData(cancelCallbackCommand);
		keyboardRow.add(cancelButton);

		InlineKeyboardButton removeOneButton = new InlineKeyboardButton("- 1");
		String removeOneCallbackCommand = SIGNUPRAID + SPACE + REMOVEONE + SPACE + gymId;
		removeOneButton.setCallbackData(removeOneCallbackCommand);
		keyboardRow.add(removeOneButton);

		InlineKeyboardButton addOneButton = new InlineKeyboardButton("+ 1");
		String addOneCallbackCommand = SIGNUPRAID + SPACE + ADDONE + SPACE + gymId;
		addOneButton.setCallbackData(addOneCallbackCommand);
		keyboardRow.add(addOneButton);

		keyboardArray.add(keyboardRow);

		keyboard.setKeyboard(keyboardArray);
		return keyboard;
	}
}