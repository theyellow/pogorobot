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

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.api.methods.BotApiMethod;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.api.objects.CallbackQuery;
import org.telegram.telegrambots.api.objects.Chat;
import org.telegram.telegrambots.api.objects.Location;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;

import pogorobot.PoGoRobotApplication.RaidBossListUpdater;
import pogorobot.entities.EventWithSubscribers;
import pogorobot.entities.Filter;
import pogorobot.entities.Gym;
import pogorobot.entities.Subscriber;
import pogorobot.entities.User;
import pogorobot.events.telegrambot.IncomingManualRaid;
import pogorobot.repositories.PossibleRaidPokemonRepository;
import pogorobot.repositories.RaidAtGymEventRepository;
import pogorobot.telegram.util.Emoji;
import pogorobot.telegram.util.Type;

@Service("telegramMessageCreatorService")
public class TelegramMessageCreatorServiceImpl implements TelegramMessageCreatorService {

	// TODO: this has to be in properties-file!!!
	private static final int NUMBER_OF_POKEMONS = 493;

	Logger logger = LoggerFactory.getLogger(this.getClass().getInterfaces()[0]);

	@Autowired
	private UserService userService;

	@Autowired
	private FilterService filterService;

	@Autowired
	private GymService gymService;

	@Autowired
	private TelegramKeyboardService telegramKeyboardService;

	@Autowired
	private TelegramTextService telegramTextService;

	@Autowired
	private PossibleRaidPokemonRepository possibleRaidPokemonRepository;

	@Autowired
	private RaidAtGymEventRepository raidAtGymEventRepository;

	@Autowired
	private EventWithSubscribersService eventWithSubscribersService;

	@Autowired
	private RaidBossListUpdater raidBossListUpdater;

	@Override
	public EditMessageText getPokemonAddDialog(CallbackQuery callbackquery, String[] data, boolean raids) {
		String commandElement = data.length > 0 ? data[0] : "";
		String subcommandElement = data.length > 1 ? data[1] : "";
		int offsetOrPokemon = data.length > 2 ? Integer.parseInt(data[2]) : 0;
		// int pokemonToAdd = data.length > 3 ? Integer.parseInt(data[3]) : 0;

		String telegramId = callbackquery.getFrom().getId().toString();
		System.out.println(Arrays.toString(data));
		String text = "";
		if (subcommandElement.equals(TelegramKeyboardService.POKEMON) && offsetOrPokemon != 0) {
			filterService.addPokemonToUserfilter(telegramId, offsetOrPokemon);
			text = "Gerade wurde " + telegramTextService.getPokemonName(String.valueOf(offsetOrPokemon))
					+ " hinzugefügt. ";
			offsetOrPokemon = 0;
		} else if (subcommandElement.equals(TelegramKeyboardService.RAIDPOKEMON) && offsetOrPokemon != 0) {
			filterService.addRaidPokemonToUserfilter(telegramId, offsetOrPokemon);
			text = "Gerade wurde " + telegramTextService.getPokemonName(String.valueOf(offsetOrPokemon))
					+ " hinzugefügt. ";
			offsetOrPokemon = 0;
		}
		EditMessageText editMessage = new EditMessageText();
		editMessage.setChatId(callbackquery.getMessage().getChatId());
		editMessage.setMessageId(callbackquery.getMessage().getMessageId());
		editMessage.enableHtml(true);

		text += raids ? "<b>Raids  zum Filter hinzufügen:</b>" : "<b>Pokémon zum Filter hinzufügen:</b>";
		editMessage.setText(text);
		List<Integer> pokemon = raids ? getUserRaidPokemon(telegramId) : getUserFilteredPokemon(telegramId);
		// offset = data.length > 3 ? Integer.parseInt(data[3]) : offset;
		String command = raids ? TelegramKeyboardService.RAIDPOKEMONADD : TelegramKeyboardService.ADD;
		String subCommand = raids ? TelegramKeyboardService.RAIDPOKEMON : TelegramKeyboardService.POKEMON;

		// This dialog always return to itself, so command and backwardCommand are equal
		InlineKeyboardMarkup keyboard = telegramKeyboardService.getPokemonKeyboard(offsetOrPokemon, command, pokemon,
				subCommand,
				// + TelegramKeyboardService.SPACE + offset
				command, "keyboard", "-1");
		editMessage.setReplyMarkup(keyboard);
		return editMessage;
	}

	@Override
	public EditMessageText getLocationSettingsAreaDialog(CallbackQuery callbackquery, String[] data, Type type) {
		String commandElement = data.length > 0 ? data[0] : "";
		String area = data.length > 1 ? data[1] : null;

		String telegramId = callbackquery.getFrom().getId().toString();

		String text = "";
		InlineKeyboardMarkup keyboard = null;
		if (commandElement.equals(TelegramKeyboardService.ADDAREA)
				|| commandElement.equals(TelegramKeyboardService.ADDRAIDAREA)
				|| commandElement.equals(TelegramKeyboardService.ADDIVAREA)) {
			text = "Hier werden Gebiete hinzugefügt. Bitte Gebiete zum Hinzufügen aussuchen.";
			if (area != null) {
				for (int i = 2; i < data.length; i++) {
					area += " " + data[i];
				}
				handleAddLocation(area, telegramId, type);
			}
			keyboard = telegramKeyboardService.getLocationAddKeyboard(userService.getOrCreateUser(telegramId), type);
			text += null == area ? "" : " Es wurde " + area + " hinzugefügt.       ";
		} else if (commandElement.equals(TelegramKeyboardService.REMOVEAREA)
				|| commandElement.equals(TelegramKeyboardService.REMOVERAIDAREA)
				|| commandElement.equals(TelegramKeyboardService.REMOVEIVAREA)) {
			text = "Hier werden Gebiete entfernt. Bitte Gebiete zum Entfernen aussuchen... ";
			if (area != null) {
				for (int i = 2; i < data.length; i++) {
					area += " " + data[i];
				}
				handleRemoveLocation(area, telegramId, type);
			}
			keyboard = telegramKeyboardService.getLocationRemovKeyboard(userService.getOrCreateUser(telegramId), type);
			text += null == area ? "" : " Es wurde " + area + " entfernt.       ";
		}

		EditMessageText editMessage = new EditMessageText();
		editMessage.setChatId(callbackquery.getMessage().getChatId());
		editMessage.setMessageId(callbackquery.getMessage().getMessageId());
		editMessage.enableHtml(true);

		editMessage.setText(text);
		editMessage.setReplyMarkup(keyboard);
		return editMessage;
	}

	@Override
	public EditMessageText getChooseGymOfRaidChoiceDialog(CallbackQuery callbackQuery, String[] data) {
		Integer messageId = callbackQuery.getMessage().getMessageId();
		String raidOrEgg = data.length > 1 ? data[1] : null;
		String dayOfYear = data.length > 2 ? data[2] : null;
		String telegramId = callbackQuery.getFrom().getId().toString();
		// if (raidOrEgg.equals(TelegramKeyboardService.EGG)) {
		//
		// }
		EditMessageText editMessage = new EditMessageText();
		editMessage.setChatId(callbackQuery.getMessage().getChatId());
		editMessage.enableHtml(true);
		editMessage.setMessageId(messageId);
		editMessage.disableWebPagePreview();

		List<Gym> gymsAround = getGymsAround(telegramId.toString(), 30.0);
		editMessage.setText(telegramTextService.getGymsAroundMessageText(gymsAround));
		editMessage
				.setReplyMarkup(telegramKeyboardService.getChooseGymsAroundKeyboard(gymsAround, raidOrEgg, dayOfYear));

		return editMessage;
	}

	@Override
	public EditMessageText getChooseRaidOrEggDialog(CallbackQuery callbackQuery, String[] data) {
		Integer messageId = callbackQuery.getMessage().getMessageId();
		EditMessageText editMessage = new EditMessageText();
		editMessage.setChatId(callbackQuery.getMessage().getChatId());
		editMessage.enableHtml(true);
		editMessage.setMessageId(messageId);
		editMessage.disableWebPagePreview();
		editMessage.setText(telegramTextService.getGymsOrEggChoiceText());
		editMessage.setReplyMarkup(telegramKeyboardService.getChooseRaidOrEggKeyboard());
		return editMessage;
	}

	@Override
	public EditMessageText getChooseRaidLevelOrPokemonDialog(CallbackQuery callbackQuery, String[] data) {
		Integer messageId = callbackQuery.getMessage().getMessageId();
		String gymId = data.length > 1 ? data[1] : null;
		String eggOrRaid = data.length > 2 ? data[2] : null;
		String offset = data.length > 3 ? data[3] : "0";
		String dayOfYear = data.length > 4 ? data[4] : null;
		String telegramId = callbackQuery.getFrom().getId().toString();
		// if (raidOrEgg.equals(TelegramKeyboardService.EGG)) {
		//
		// }
		EditMessageText editMessage = new EditMessageText();
		editMessage.setChatId(callbackQuery.getMessage().getChatId());
		editMessage.enableHtml(true);
		editMessage.setMessageId(messageId);
		editMessage.disableWebPagePreview();

		editMessage.setText(telegramTextService.getChooseLevelOrPokemon(eggOrRaid));
		editMessage.setReplyMarkup(
				telegramKeyboardService.getLevelOrPokemonChoiceKeyboard(eggOrRaid, gymId, offset, dayOfYear));
		return editMessage;
	}

	@Override
	public EditMessageText getPokemonRemoveDialog(CallbackQuery callbackquery, String[] data, boolean raids) {
		Integer telegramId = callbackquery.getFrom().getId();
		Integer messageId = callbackquery.getMessage().getMessageId();
		String subcommand = data.length > 1 ? data[1] : "";
		int offsetOrPokemon = data.length > 2 ? Integer.parseInt(data[2]) : 0;
		// int pokemonToRemove = data.length > 3 ? Integer.parseInt(data[3]) : 0;

		System.out.println(Arrays.toString(data));

		EditMessageText editMessage = new EditMessageText();
		editMessage.setChatId(callbackquery.getMessage().getChatId());
		editMessage.enableHtml(true);
		editMessage.setMessageId(messageId);
		String filteredPokemon = "";
		String text = "";
		if (raids) {
			filteredPokemon = getFilteredRaidPokemonForUser(telegramId);
			text = "<b>Bisher hast du folgende Raids ausgewählt:</b>\n" + filteredPokemon
					+ "\n<b>Raids aus dem Filter entfernen:</b>";
		} else {
			filteredPokemon = telegramTextService.getFilteredPokemonForUser(telegramId);
			text = "<b>Bisher hast du folgende Pokemon zum anzeigen ausgewählt:</b>\n" + filteredPokemon
					+ "\n<b>Pokémon aus dem Filter entfernen:</b>";
		}
		String pokemonName = offsetOrPokemon == 0 ? " Gar keins..." + Emoji.SMILING_FACE_WITH_SMILING_EYES
				: telegramTextService.getPokemonName(String.valueOf(offsetOrPokemon));
		if (subcommand.equals(TelegramKeyboardService.POKEMON) && offsetOrPokemon != 0) {
			text = "Es wird " + pokemonName
					+ " aus deinem Filter entfernt.\n<b>Pokémon aus dem Filter entfernen:             </b>";
			filterService.removePokemonFromUserfilter(telegramId.toString(), offsetOrPokemon);
			// offset = data.length > 3 ? Integer.parseInt(data[3]) : offset;
			offsetOrPokemon = 0;
		} else if (subcommand.equals(TelegramKeyboardService.RAIDPOKEMON) && offsetOrPokemon != 0) {
			text = "Es wird " + pokemonName
					+ " aus deinem Filter entfernt.\n<b>Raids aus dem Filter entfernen:               </b>";
			filterService.removeRaidPokemonFromUserfilter(telegramId.toString(), offsetOrPokemon);
			// offset = data.length > 3 ? Integer.parseInt(data[3]) : offset;
			offsetOrPokemon = 0;
		}
		editMessage.setText(text);

		List<Integer> pokemon;
		if (raids) {
			pokemon = filterService.getFilterRaidsForTelegramId(telegramId.toString());
		} else {
			pokemon = userService.getOrCreateUser(telegramId.toString()).getUserFilter().getPokemons();
		}
		String command = raids ? TelegramKeyboardService.RAIDPOKEMONREMOVE : TelegramKeyboardService.REMOVE;
		String subCommand = raids ? TelegramKeyboardService.RAIDPOKEMON : TelegramKeyboardService.POKEMON;
		// InlineKeyboardMarkup keyboard =
		// telegramKeyboardService.getPokemonRemoveKeyboard(telegramId.toString(),
		// offsetOrPokemon, command, subCommand, pokemon);
		InlineKeyboardMarkup keyboard = telegramKeyboardService.getPokemonKeyboard(offsetOrPokemon, command, pokemon,
				subCommand, command, "keyboard", "-1");
		editMessage.setReplyMarkup(keyboard);
		return editMessage;
	}

	// @Override
	// public SendMessage getLiveLocationDialog(String telegramId, String[] data) {
	// // Long telegramId = callbackquery.getMessage().getChatId();
	// if (data != null && data.length > 1) {
	// String location = data[1];
	// User user = userService.getOrCreateUser(telegramId);
	// Double lon = user.getUserFilter().getLongitude();
	// Double lat = user.getUserFilter().getLatitude();
	// if (lat == null || lon == null) {
	// return null;
	// }
	// String locationFromFilter =
	// telegramTextService.createFormattedLocationString(lat.toString(),
	// lon.toString());
	// if (location.equals(locationFromFilter)) {
	// return null;
	// }
	// }
	// SendMessage message = new SendMessage();
	// message.setChatId(telegramId);
	// message.enableHtml(true);
	// message.disableNotification();
	// String text = "<b>Nun kannst du mit dem Button unten einen Ort schicken.";
	//
	// message.setText(text + "</b> ");
	// message.setReplyMarkup(
	// telegramKeyboardService.getLocationSettingKeyboard(userService.getOrCreateUser(telegramId.toString())));
	// return message;
	// }

	@Override
	public EditMessageText getEnabledRaidsMainDialog(CallbackQuery callbackquery) {
		Message message = callbackquery.getMessage();
		Integer telegramId = callbackquery.getFrom().getId();
		User user = userService.getOrCreateUser(telegramId.toString());
		user.setShowRaidMessages(true);
		user = userService.updateOrInsertUser(user);
		String filteredPokemon = getFilteredRaidPokemonForUser(telegramId);
		EditMessageText editMessage = new EditMessageText();
		editMessage.setChatId(message.getChatId());
		editMessage.enableHtml(true);
		editMessage.setMessageId(message.getMessageId());
		editMessage.setText("<b>Bisher hast du folgende Raids ausgewählt:</b>\n" + filteredPokemon
				+ "\nEs werden Raids ab <b>Level " + user.getUserFilter().getRaidLevel()
				+ ".</b> angezeigt.\nRaid zum Filter hinzufügen: ");
		editMessage
				.setReplyMarkup(telegramKeyboardService.getRaidSettingKeyboard(isShowRaidsActiceForUser(telegramId)));
		return editMessage;
	}

	@Override
	public EditMessageText getRaidPokemonDialog(CallbackQuery callbackquery) {
		EditMessageText editMessage = new EditMessageText();
		Long telegramId = callbackquery.getMessage().getChatId();
		editMessage.setChatId(telegramId);
		editMessage.setMessageId(callbackquery.getMessage().getMessageId());
		editMessage.enableHtml(true);
		String text = "<b>Bisher ausgewählte Raid-Pokémon im Filter:</b>                     \n";
		text += getFilteredRaidPokemonForUser(telegramId.intValue());
		editMessage.setText(text);
		editMessage.setReplyMarkup(telegramKeyboardService.getPokemonSettingKeyboard(true));
		return editMessage;
	}

	@Override
	public SendMessage answerUserMessage(Message message, org.telegram.telegrambots.api.objects.User from) {
		User user = null;
		if (userService != null) {
			user = userService.getOrCreateUser(from.getId().toString());
		}

		SendMessage echoMessage = new SendMessage();
		Long chatId = message.getChatId();
		echoMessage.setChatId(chatId);
		echoMessage.enableHtml(true);
		echoMessage.disableNotification();
		String inputText = "";
		Location location = message.getLocation();
		if (location != null) {

			// TODO: what is with "add raid"?

			user = setLocationForUser(user, location);

			String messageText = "Dein Standort wurde aktualisiert.";
			echoMessage.setText(messageText);
			echoMessage.setReplyMarkup(telegramKeyboardService.getSettingsKeyboard(user.isRaidadmin()));
			inputText = "Standort";
		}
		inputText = message.getText() == null ? inputText : message.getText();

		if (inputText.startsWith("raidbossupdate")
				&& (user.isRaidadmin() || user.isAdmin() || user.isSuperadmin())) {
			raidBossListUpdater.updateRaidBossList();
			echoMessage.setText("Raidboss-Liste wurde aktualisiert. Es geht nun normal weiter " + Emoji.SUN_WITH_FACE);
		} else if (inputText.startsWith("Zurück zum Start")) {
			echoMessage.setText("Es geht nun normal weiter " + Emoji.SUN_WITH_FACE);
			echoMessage.setReplyMarkup(telegramKeyboardService.getSettingsKeyboard(user.isAdmin()));
		} else if (inputText.startsWith(Emoji.HEAVY_PLUS_SIGN + " ")) {
			// TODO: Dead code?
			handleAddLocation(inputText.substring(2), message.getFrom().getId().toString(), Type.POKEMON);
			echoMessage.setText("Es wurde ein Gebiet hinzugefügt: " + inputText.substring(2));
			echoMessage.setReplyMarkup(telegramKeyboardService.getSettingsKeyboard(user.isAdmin()));
		} else if (inputText.startsWith(Emoji.HEAVY_MINUS_SIGN + " ")) {
			// TODO: Dead code?
			handleRemoveLocation(inputText.substring(2), message.getFrom().getId().toString(), Type.POKEMON);
			echoMessage.setText("Es wurde ein Gebiet entfernt: " + inputText.substring(2));
			echoMessage.setReplyMarkup(telegramKeyboardService.getSettingsKeyboard(user.isAdmin()));
		} else if (inputText.equalsIgnoreCase("Pokémon") || inputText.equalsIgnoreCase("Pokemon")
				|| inputText.equalsIgnoreCase("Monster")) {
			Integer telegramId = message.getFrom().getId();
			String filteredPokemon = telegramTextService.getFilteredPokemonForUser(telegramId);
			echoMessage.setText("<b>Bisher hast du folgende ausgewählt:</b>\n" + filteredPokemon
					+ "\n<b>Pokémon zum Filter hinzufügen oder entfernen?</b>");
			echoMessage.setReplyMarkup(telegramKeyboardService.getPokemonSettingKeyboard(false));
		} else if (inputText.equalsIgnoreCase("Raids")) {
			Integer telegramId = message.getFrom().getId();
			String filteredPokemon = getFilteredRaidPokemonForUser(telegramId);
			boolean showRaidsActiceForUser = isShowRaidsActiceForUser(telegramId);
			if (showRaidsActiceForUser) {
				echoMessage.setText("<b>Bisher hast du folgende Raids ausgewählt:</b>\n" + filteredPokemon
						+ "\nEs werden Raids ab <b>Level " + user.getUserFilter().getRaidLevel()
						+ ".</b> angezeigt.\n<b>Raids im Filter konfigurieren:</b> ");
			} else {
				echoMessage.setText(
						"<b>Du hast Raids im Moment deaktiviert. Zum Aktivieren den Button unten verwenden.</b>");
			}
			echoMessage.setReplyMarkup(telegramKeyboardService.getRaidSettingKeyboard(showRaidsActiceForUser));
		} else if (inputText.equalsIgnoreCase("Raid hinzufügen")) {
			// Integer telegramId = message.getFrom().getId();
			// List<Gym> gymsAround = getGymsAround(telegramId.toString(), 1.5);
			// echoMessage.setText(telegramTextService.getGymsAroundMessageText(gymsAround));
			echoMessage.setText(telegramTextService.getGymsOrEggChoiceText());
			echoMessage.setReplyMarkup(telegramKeyboardService.getChooseRaidOrEggKeyboard());
			echoMessage.disableWebPagePreview();
		} else if (inputText.equalsIgnoreCase("IV")) {
			Double minIV = user.getUserFilter().getMinIV();
			if (minIV == null) {
				minIV = 101.0;
			}
			String minIvText = 100.0 < minIV ? "Deaktiviert\n"
					: "Ab " + String.valueOf(minIV + 1.0) + "% werden dir alle Pokémon "
							+ "im ausgewählten Gebiet angezeigt.\n";

			echoMessage.setText("<b>Einstellungen zur IV-Suche:</b> " + minIvText
					+ "Falls noch nicht geschehen suche dir am besten gleich jetzt "
					+ "ein Suchgebiet für IV aus oder aktualisiere deinen Umkreis.");
			echoMessage.setReplyMarkup(telegramKeyboardService.getIVSettingKeyboard(minIV));
		} else if (inputText.equalsIgnoreCase("Standort") || inputText.isEmpty()) {
			// Nothing to to, if location was shared it is already set on
			// database
		} else {
			echoMessage.setText("Huhu " + from.getUserName() + ", deine Nachricht:\n" + inputText
					+ "\nIch weiß nichts damit anzufangen. Wolltest du vielleicht /start oder /stop eingeben?");
		}
		return echoMessage;
	}

	@Override
	public EditMessageText getRaidDisabledDialog(CallbackQuery callbackquery) {
		Message message = callbackquery.getMessage();
		Integer telegramId = callbackquery.getFrom().getId();
		User user = userService.getOrCreateUser(telegramId.toString());
		user.setShowRaidMessages(false);
		user = userService.updateOrInsertUser(user);
		EditMessageText editMessage = new EditMessageText();
		editMessage.setChatId(message.getChatId());
		editMessage.enableHtml(true);
		editMessage.setMessageId(message.getMessageId());
		editMessage.setText("<b>Du hast Raids im Moment deaktiviert. Zum aktivieren den Button unten verwenden.</b>");
		editMessage
				.setReplyMarkup(telegramKeyboardService.getRaidSettingKeyboard(isShowRaidsActiceForUser(telegramId)));
		return editMessage;
	}

	@Override
	public EditMessageText getDistanceSelectDialog(CallbackQuery callbackquery, String[] data) {
		Long telegramId = callbackquery.getMessage().getChatId();
		String type = "";
		String typeText = "";
		if (data.length > 1) {
			type = data[1];
			if ("iv".equalsIgnoreCase(type)) {
				typeText = "der IV-basierten Anzeige";
			} else if ("mon".equalsIgnoreCase(type)) {
				typeText = "von Pokémon";
			} else if ("raid".equalsIgnoreCase(type)) {
				typeText = "von Raids";
			}
		}
		if (data.length > 2) {
			String maxDistance = data[2];
			User user = userService.getOrCreateUser(telegramId.toString());
			if ("iv".equalsIgnoreCase(type)) {
				Double radius = user.getUserFilter().getRadiusIV();
				if (radius == null) {
					radius = 0.5;
				}
				if (!radius.equals(Double.valueOf(maxDistance))) {
					filterService.setUserIvRadius(telegramId, maxDistance);
				} else {
					return null;
				}
			} else if ("mon".equalsIgnoreCase(type)) {
				Double radius = user.getUserFilter().getRadiusPokemon();
				if (radius == null) {
					radius = 0.5;
				}
				if (!radius.equals(Double.valueOf(maxDistance))) {
					filterService.setUserPokemonRadius(telegramId, maxDistance);
				} else {
					return null;
				}
			} else if ("raid".equalsIgnoreCase(type)) {
				Double radius = user.getUserFilter().getRadiusRaids();
				if (radius == null) {
					radius = 0.5;
				}
				if (!radius.equals(Double.valueOf(maxDistance))) {
					filterService.setUserRaidRadius(telegramId, maxDistance);
				} else {
					return null;
				}
			}
		}
		EditMessageText message = new EditMessageText();
		message.setChatId(telegramId);
		message.setMessageId(callbackquery.getMessage().getMessageId());
		message.enableHtml(true);
		String text = "Maximale Entfernung " + typeText + " im Filter einstellen: ";

		text += telegramTextService.getPrintFormattedRadiusForUser(telegramId.intValue(), type);
		message.setText(text + "");
		message.setReplyMarkup(telegramKeyboardService.getDistanceselectSettingKeyboard(type));
		return message;
	}

	@Override
	public EditMessageText getRaidLevelDialog(CallbackQuery callbackquery, String[] data) {
		Long telegramId = callbackquery.getMessage().getChatId();
		if (data.length > 1) {
			String level = data[1];
			User user = userService.getOrCreateUser(telegramId.toString());
			String raidLevel = user.getUserFilter().getRaidLevel() == null ? "keins"
					: user.getUserFilter().getRaidLevel().toString();
			if (level.equals(raidLevel)) {
				return null;
			}
			filterService.setUserRaidLevel(telegramId, level);
		}
		EditMessageText message = new EditMessageText();
		message.setChatId(telegramId);
		message.setMessageId(callbackquery.getMessage().getMessageId());
		message.enableHtml(true);
		String text = "<b>Raid-Level im Filter einstellen: ";
		text += getFilteredRaidLevelForUser(telegramId.intValue());
		message.setText(text + "</b>                       ");
		message.setReplyMarkup(
				telegramKeyboardService.getRaidLevelSettingKeyboard(TelegramKeyboardService.RAIDLEVEL, "-1"));
		return message;
	}

	@Override
	public BotApiMethod<? extends Serializable> getIvSettingDialog(CallbackQuery callbackquery, String[] data) {
		Long telegramId = callbackquery.getMessage().getChatId();
		EditMessageText message = new EditMessageText();
		message.setChatId(callbackquery.getMessage().getChatId());
		message.setMessageId(callbackquery.getMessage().getMessageId());
		message.enableHtml(true);
		String text = "Minimale IV, ab denen alle gefundenen Pokémon gezeigt werden: ";

		if (data.length > 1) {
			String minIVIn = data[1];
			User user = userService.getOrCreateUser(telegramId.toString());
			Double minIV = user.getUserFilter().getMinIV();
			String minIVsaved = minIV == null ? "keins" : minIV.toString();
			String minIVsavedPlusOne = minIV == null ? "keins" : Double.toString(minIV + 1);
			if (minIVsaved.equals(minIVIn) || minIVsavedPlusOne.equals(minIVIn)) {
				return null;
			}
			filterService.setUserMinIv(telegramId, minIVIn);
			if (100 < Double.valueOf(minIVIn)) {
				text += getFilteredMinIvForUser(telegramId.intValue());
				message.setText(text + "                       ");
				message.setReplyMarkup(telegramKeyboardService.getIVSettingKeyboard(Double.valueOf(minIVIn)));
				return message;
			}
		}
		text += getFilteredMinIvForUser(telegramId.intValue());
		message.setText(text + "                       ");
		InlineKeyboardMarkup minIvSettingKeyboard = telegramKeyboardService.getMinIvSettingKeyboard();
		message.setReplyMarkup(minIvSettingKeyboard);
		return message;
	}

	private User setLocationForUser(User user, Location location) {
		Double latitude = location.getLatitude().doubleValue();
		Double longitude = location.getLongitude().doubleValue();
		user.getUserFilter().setLatitude(latitude);
		user.getUserFilter().setLongitude(longitude);
		user = userService.updateOrInsertUser(user);
		return user;
	}

	private boolean isShowRaidsActiceForUser(Integer telegramId) {
		User user = userService.getOrCreateUser(telegramId.toString());
		return user.isShowRaidMessages();
	}

	private void handleAddLocation(String inputText, String telegramId, Type type) {
		// boolean pokemonFilter = !inputText.startsWith("Raid");
		filterService.addGeofenceToUserfilter(telegramId, inputText, type);
	}

	private void handleRemoveLocation(String inputText, String telegramId, Type type) {
		// boolean pokemonFilter = !inputText.startsWith("Raid");
		filterService.removeGeofenceFromUserfilter(telegramId, inputText, type);
	}

	// @Override
	private List<Integer> getUserRaidPokemon(String telegramId) {
		List<Integer> filteredPokemon = filterService.getFilterRaidsForTelegramId(telegramId).stream().sorted()
				.collect(Collectors.toList());
		List<Integer> pokemon = filterService.getAllRaidPokemon();
		pokemon.removeAll(filteredPokemon);
		return pokemon;
	}

	private String getFilteredRaidPokemonForUser(Integer telegramId) {
		String result = "";
		List<Integer> raidPokemon = filterService.getFilterRaidsForTelegramId(telegramId.toString());
		for (Integer pokemon : raidPokemon) {
			String pokemonName = telegramTextService.getPokemonName(pokemon.toString());
			result += pokemonName + ", ";
		}
		result = result.isEmpty() ? "" : result.substring(0, result.length() - 2);
		return result;
	}

	private List<Integer> getUserFilteredPokemon(String telegramId) {
		User user = userService.getOrCreateUser(telegramId);
		Filter filter = user.getUserFilter();
		List<Integer> filteredPokemons = filter.getPokemons();
		List<Integer> pokemons = new ArrayList<>();
		for (int i = 1; i <= NUMBER_OF_POKEMONS; i++) {
			pokemons.add(i);
		}
		pokemons.removeAll(filteredPokemons);
		return pokemons;
	}

	private List<Gym> getGymsAround(String telegramId, Double radius) {
		User user = userService.getOrCreateUser(telegramId);
		return getGymsAround(user.getUserFilter().getLatitude(), user.getUserFilter().getLongitude(), radius);
	}

	private List<Gym> getGymsAround(Double latitude, Double longitude, Double radius) {
		List<Gym> result = new ArrayList<>();
		Map<Gym, Double> gymWithDistance = new HashMap<>();
		Iterable<Gym> allGyms = gymService.getAllGym();
		allGyms.forEach(x -> {
			if (x.getLatitude() != null && x.getLongitude() != null) {
				Double distance = filterService.calculateDistanceInKilometer(x.getLatitude(), x.getLongitude(),
						latitude, longitude);
				if (distance != null && distance < radius) {
					gymWithDistance.put(x, distance);
					result.add(x);
				}
			}
		});
		return result.stream().sorted((x, y) -> {
			return gymWithDistance.get(x).compareTo(gymWithDistance.get(y));
		}).limit(10).collect(Collectors.toList());
	}

	private String getFilteredRaidLevelForUser(Integer telegramId) {
		String result = "";
		User user = userService.getOrCreateUser(telegramId.toString());
		Filter filter = user.getUserFilter();
		result = filter.getRaidLevel() == null ? "5" : Integer.toString(filter.getRaidLevel());
		return result;
	}

	private String getFilteredMinIvForUser(Integer telegramId) {
		String result = "";
		User user = userService.getOrCreateUser(telegramId.toString());
		Filter filter = user.getUserFilter();
		result = "100";
		Double minIV = filter.getMinIV();
		if (minIV != null) {
			if (minIV.equals(93.0)) {
				result = Double.toString(minIV);
			} else if (minIV.equals(101.0)) {
				result = "kein Minimum definiert";
			} else {
				minIV++;
				result = Double.toString(minIV);
			}
		}
		return result;
	}

	@Override
	public EditMessageText getHoursForNewRaidDialog(CallbackQuery callbackQuery, String[] data) {
		Integer messageId = callbackQuery.getMessage().getMessageId();
		String internalGymId = data.length > 1 ? data[1] : null;
		String eggOrRaid = data.length > 2 ? data[2] : null;
		String pokemonOrLevel = data.length > 3 ? data[3] : null;
		String dayOfYear = data.length > 4 ? data[4] : null;
		String telegramId = callbackQuery.getFrom().getId().toString();
		// if (raidOrEgg.equals(TelegramKeyboardService.EGG)) {
		//
		// }
		EditMessageText editMessage = new EditMessageText();
		editMessage.setChatId(callbackQuery.getMessage().getChatId());
		editMessage.enableHtml(true);
		editMessage.setMessageId(messageId);
		editMessage.disableWebPagePreview();

		editMessage.setText(telegramTextService.getChooseTimeForRaidForHour(eggOrRaid));
		// editMessage.setReplyMarkup(null);
		editMessage.setReplyMarkup(
				telegramKeyboardService.getChooseHoursForRaidKeyboard(eggOrRaid, internalGymId, pokemonOrLevel,
						dayOfYear));
		return editMessage;
	}

	@Override
	public EditMessageText getMinutesForNewRaidDialog(CallbackQuery callbackQuery, String[] data) {
		Integer messageId = callbackQuery.getMessage().getMessageId();
		String internalGymId = data.length > 1 ? data[1] : null;
		String eggOrRaid = data.length > 2 ? data[2] : null;
		String pokemonOrLevel = data.length > 3 ? data[3] : null;
		String hour = data.length > 4 ? data[4] : null;
		String dayOfYear = data.length > 5 ? data[5] : null;
		String telegramId = callbackQuery.getFrom().getId().toString();
		// if (raidOrEgg.equals(TelegramKeyboardService.EGG)) {
		//
		// }
		EditMessageText editMessage = new EditMessageText();
		editMessage.setChatId(callbackQuery.getMessage().getChatId());
		editMessage.enableHtml(true);
		editMessage.setMessageId(messageId);
		editMessage.disableWebPagePreview();

		editMessage.setText(telegramTextService.getChooseTimeForRaidForMinute(eggOrRaid, hour));
		// editMessage.setReplyMarkup(null);
		editMessage.setReplyMarkup(telegramKeyboardService.getChooseMinutesForRaidKeyboard(eggOrRaid, internalGymId,
				pokemonOrLevel, hour, dayOfYear));
		return editMessage;
	}

	@Override
	public EditMessageText getExactTimeForNewRaidDialog(CallbackQuery callbackQuery, String[] data) {
		Integer messageId = callbackQuery.getMessage().getMessageId();
		String internalGymId = data.length > 1 ? data[1] : null;
		String pokemonOrLevel = data.length > 2 ? data[2] : null;
		String eggOrRaid = data.length > 3 ? data[3] : null;
		String timePart = data.length > 4 ? data[4] : null;
		String dayOfYear = data.length > 5 ? data[5] : null;
		String telegramId = callbackQuery.getFrom().getId().toString();
		// if (raidOrEgg.equals(TelegramKeyboardService.EGG)) {
		//
		// }
		EditMessageText editMessage = new EditMessageText();
		editMessage.setChatId(callbackQuery.getMessage().getChatId());
		editMessage.enableHtml(true);
		editMessage.setMessageId(messageId);
		editMessage.disableWebPagePreview();

		editMessage.setText(telegramTextService.getChooseExactTimeForRaid(eggOrRaid, timePart));
		// editMessage.setReplyMarkup(null);
		editMessage.setReplyMarkup(telegramKeyboardService.getChooseExactTimeForRaidKeyboard(eggOrRaid, internalGymId,
				pokemonOrLevel, timePart, dayOfYear));
		return editMessage;
	}

	@Override
	public EditMessageText getDateForNewRaidDialog(CallbackQuery callbackQuery, String[] data) {
		Integer messageId = callbackQuery.getMessage().getMessageId();
		String internalGymId = data.length > 1 ? data[1] : null;
		String pokemonOrLevel = data.length > 2 ? data[2] : null;
		String eggOrRaid = data.length > 3 ? data[3] : null;
		String timePart = data.length > 4 ? data[4] : null;
		String telegramId = callbackQuery.getFrom().getId().toString();
		// if (raidOrEgg.equals(TelegramKeyboardService.EGG)) {
		//
		// }
		EditMessageText editMessage = new EditMessageText();
		editMessage.setChatId(callbackQuery.getMessage().getChatId());
		editMessage.enableHtml(true);
		editMessage.setMessageId(messageId);
		editMessage.disableWebPagePreview();

		editMessage.setText(telegramTextService.getChooseDateForRaid(eggOrRaid, timePart));
		// editMessage.setReplyMarkup(null);
		editMessage.setReplyMarkup(telegramKeyboardService.getChooseDateForRaidKeyboard(eggOrRaid, internalGymId,
				pokemonOrLevel, timePart));
		return editMessage;
	}

	@Override
	public EditMessageText getConfirmNewRaidDialog(CallbackQuery callbackQuery, String[] data) {
		Integer messageId = callbackQuery.getMessage().getMessageId();
		String internalGymId = data.length > 1 ? data[1] : null;
		String pokemonOrLevel = data.length > 2 ? data[2] : null;
		String eggOrRaid = data.length > 3 ? data[3] : null;
		String time = data.length > 4 ? data[4] : null;
		String dayOfYear = data.length > 5 ? data[5] : null;
		String telegramId = callbackQuery.getFrom().getId().toString();
		// if (raidOrEgg.equals(TelegramKeyboardService.EGG)) {
		// CONFIRMNEWRAIDATGYM + SPACE + internalGymId + SPACE + pokemonOrLevel + SPACE
		// + eggOrRaid
		// + SPACE + time + SPACE + date.getDayOfYear()
		// }
		EditMessageText editMessage = new EditMessageText();
		editMessage.setChatId(callbackQuery.getMessage().getChatId());
		editMessage.enableHtml(true);
		editMessage.setMessageId(messageId);
		editMessage.disableWebPagePreview();
		LocalDate date = LocalDate.ofYearDay(LocalDate.now().get(ChronoField.YEAR), Integer.valueOf(dayOfYear));
		editMessage.setText(telegramTextService.getConfirmNewRaid(eggOrRaid, time, pokemonOrLevel, date));
		// editMessage.setReplyMarkup(null);
		editMessage.setReplyMarkup(telegramKeyboardService.getConfirmNewRaidKeyboard(eggOrRaid, internalGymId,
				pokemonOrLevel, time, date));
		return editMessage;
	}

	@Override
	public IncomingManualRaid getManualRaid(CallbackQuery callbackQuery, String[] data) {
		String internalGymId = data.length > 1 ? data[1] : null;
		String gymId = gymService.getGymByInternalId(Long.parseLong(internalGymId)).getGymId();
		String pokemonOrLevelRaw = data.length > 2 ? data[2] : null;
		Long pokemonOrLevel = Long.valueOf(pokemonOrLevelRaw);
		String eggOrRaid = data.length > 3 ? data[3] : null;
		String time = data.length > 4 ? data[4] : null;
		String dayOfYear = data.length > 5 ? data[5] : null;

		IncomingManualRaid manualRaid = new IncomingManualRaid();

		manualRaid.setGymId(gymId);

		DateTimeFormatter parser = new DateTimeFormatterBuilder()
				.appendValue(ChronoField.HOUR_OF_DAY, 1, 2, SignStyle.NEVER).appendLiteral(":")
				.appendValue(ChronoField.MINUTE_OF_HOUR, 1, 2, SignStyle.NEVER).appendLiteral(" ")
				.appendValue(ChronoField.DAY_OF_YEAR, 1, 3, SignStyle.NEVER).appendLiteral(" ")
				.appendValue(ChronoField.YEAR, 1, 4, SignStyle.NEVER).toFormatter();
		LocalDateTime begin = LocalDateTime
				.parse(time + " " + dayOfYear + " " + LocalDateTime.now().get(ChronoField.YEAR), parser);
		long secondsFromEpoch = begin.atZone(ZoneId.of("Europe/Berlin")).toEpochSecond();
		manualRaid.setStart(secondsFromEpoch);
		manualRaid.setEnd(secondsFromEpoch + GymService.RAID_DURATION * 60);

		// telegramTextService.formatTimeFromSeconds(millisFromEpoch);
		if (eggOrRaid.equals(TelegramKeyboardService.EGG)) {
			manualRaid.setLevel(pokemonOrLevel);
		} else if (eggOrRaid.equals(TelegramKeyboardService.RAID)) {
			manualRaid.setPokemonId(pokemonOrLevel);
			manualRaid.setLevel(
					possibleRaidPokemonRepository.findByPokemonId(pokemonOrLevel.intValue()).getLevel().longValue());
		} else {
			logger.error("Manual raid setup had errors, command : " + callbackQuery.getData() + " - chat : "
					+ callbackQuery.getMessage().getChatId());
		}
		return manualRaid;
	}

	@Override
	public EditMessageText getChooseShareRaidDialog(CallbackQuery callbackQuery, String[] data,
			IncomingManualRaid manualRaid) {
		Integer messageId = callbackQuery.getMessage().getMessageId();
		String internalGymId = data.length > 1 ? data[1] : null;
		String gymId = gymService.getGymByInternalId(Long.parseLong(internalGymId)).getGymId();
		String pokemonOrLevelRaw = data.length > 2 ? data[2] : null;
		Long pokemonOrLevel = Long.valueOf(pokemonOrLevelRaw);
		String eggOrRaid = data.length > 3 ? data[3] : null;
		String time = data.length > 4 ? data[4] : null;
		String dayOfYear = data.length > 5 ? data[5] : null;

		EditMessageText editMessage = new EditMessageText();
		editMessage.setChatId(callbackQuery.getMessage().getChatId());
		editMessage.enableHtml(true);
		editMessage.setMessageId(messageId);
		editMessage.disableWebPagePreview();
		LocalDate date = LocalDate.ofYearDay(LocalDate.now().get(ChronoField.YEAR), Integer.valueOf(dayOfYear));
		// editMessage.setText(telegramTextService.getShareRaid(eggOrRaid, time,
		// pokemonOrLevel, date));
		// editMessage.setReplyMarkup(null);
		// editMessage.setReplyMarkup(
		// telegramKeyboardService.getShareRaidKeyboard(eggOrRaid, internalGymId,
		// pokemonOrLevel, time, date));
		return editMessage;
	}

	@Override
	public EditMessageText getSignupRaidDialog(CallbackQuery callbackQuery, String[] data) {
		Message message = callbackQuery.getMessage();
		Integer messageId = message.getMessageId();
		Chat chat = message.getChat();
		Integer fromId;
		if (chat.isUserChat()) {
			fromId = chat.getId().intValue();
		} else {
			org.telegram.telegrambots.api.objects.User from = callbackQuery.getFrom();
			fromId = from.getId();
		}
		User user = userService.getOrCreateUser(fromId.toString());
		String commandOrGymId = data.length > 1 ? data[1] : null;
		String gymId = "";
		String time = "";
		if (null == commandOrGymId) {
			logger.error("Error while trying to signup raid");
		} else if (TelegramKeyboardService.CANCEL.equals(commandOrGymId)) {
			gymId = data.length > 2 ? data[2] : null;
		} else if (TelegramKeyboardService.ADDONE.equals(commandOrGymId)) {
			gymId = data.length > 2 ? data[2] : null;
		} else if (TelegramKeyboardService.REMOVEONE.equals(commandOrGymId)) {
			gymId = data.length > 2 ? data[2] : null;
		} else {
			gymId = commandOrGymId;
			commandOrGymId = TelegramKeyboardService.SIGNUPRAID;
			time = data.length > 2 ? data[2] : null;

		}

		// Attention: Action starts here
		modifyEvent(commandOrGymId, user, gymId, time);

		// event.getGymId()
		String pokemonText = telegramTextService.getRaidMessagePokemonText(gymService.getGym(gymId));
		// eventsWithSubscribers.stream().forEach(x -> x.getTime().equals(time));;

		SortedSet<EventWithSubscribers> eventsWithSubscribers = eventWithSubscribersService
				.getSubscribersForRaid(gymId);
		String participantsText = telegramTextService.getParticipantsText(eventsWithSubscribers);
		EditMessageText editMessage = new EditMessageText();
		editMessage.setText(pokemonText + participantsText);
		editMessage.setChatId(callbackQuery.getMessage().getChatId());
		editMessage.enableHtml(true);
		editMessage.setMessageId(messageId);
		editMessage.disableWebPagePreview();
		editMessage.setReplyMarkup(telegramKeyboardService.getRaidSignupKeyboard(eventsWithSubscribers, gymId));
		// LocalDate date = LocalDate.ofYearDay(LocalDate.now().get(ChronoField.YEAR),
		// Integer.valueOf(dayOfYear));
		// editMessage.setText(telegramTextService.getShareRaid(eggOrRaid, time,
		// pokemonOrLevel, date));
		// editMessage.setReplyMarkup(null);
		// editMessage.setReplyMarkup(
		// telegramKeyboardService.getShareRaidKeyboard(eggOrRaid, internalGymId,
		// pokemonOrLevel, time, date));
		return editMessage;
	}

	private void modifyEvent(String commandOrGymId, User user, String gymId, String time) {

		// Gym fullGym = gymService.getGym(gymId);
		SortedSet<EventWithSubscribers> eventsWithSubscribers = eventWithSubscribersService
				.getSubscribersForRaid(gymId);
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
				if (users.contains(user)) {
					// eventWithSubscribersRepository.save(x);
				}

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
			// TODO: implement addOne - action
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
			// TODO: implement signup in first place - action
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
		eventWithSubscribersService.saveSubscribersForRaid(eventsWithSubscribers);
		// String time = ;

	}

}
