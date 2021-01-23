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

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import pogorobot.entities.EventWithSubscribers;
import pogorobot.entities.Filter;
import pogorobot.entities.Gym;
import pogorobot.entities.MessageConfigElement;
import pogorobot.entities.PokemonWithSpawnpoint;
import pogorobot.entities.Raid;
import pogorobot.entities.Subscriber;
import pogorobot.entities.User;
import pogorobot.service.db.GymService;
import pogorobot.service.db.UserService;
import pogorobot.telegram.PogoBot;
import pogorobot.telegram.util.Emoji;

@Service("telegramTextService")
public class TelegramTextServiceImpl<R> implements TelegramTextService {

	private static Logger logger = LoggerFactory.getLogger(TelegramTextService.class);

	private static final String MESSAGE_SPACE = " ";

	private static final String MESSAGE_NEWLINE = "\n";

	// private static final String MESSAGE_END_A_HREF = "</a>";
	// private static final String MESSAGE_END_OF_A_TAG_WITH_COMMENT = "\">";
	// private static final String MESSAGE_BEGIN_A_HREF = "<a href=\"";

	private static final String MESSAGE_SLASH = "/";

	private static final String MESSAGE_BOLD_ON = "*"; // "<b>";

	private static final String MESSAGE_BOLD_OFF = "*";// "</b>";

	private static final Marker LOGTAG = null; // "TELEGRAM TEXT SERVICE";

	private static String serialString = "68747470733a2f2f6d6f6e73746572696d616765732e746b2f76312e342f74656c656772616d";
	private static String serialStringDh = "2f2f2068747470733a2f2f6769746875622e636f6d2f506f676f64656e68656c6465722f737072697465732f626c6f622f6d61737465722f";

	private static JSONObject jsonMoves;

	private static JSONObject jsonPokemons;

	@Autowired
	private UserService userService;

	@Autowired
	private ConfigReader configReader;

	private static JSONObject jsonBaseStats;

	private static JSONObject jsonForms;

	public String getPokemonNameFromProtobuf(String pokemon) {
		if ("-1".equals(pokemon)) {
			return "Ei";
		}
		// int value = Integer.valueOf(pokemon);
		// PokemonId pokemonId = PokemonId.forNumber(value);
		// String normalizedPokemonName = pokemonId.name();
		// PokemonDisplay.newBuilder().getForm().name()getClass();
		// PokemonForm pf = PokemonForm.forNumber(pokemonId.getNumber());

		return "";
	}

	@Override
	public String getPokemonName(String pokemon) {
		if ("-1".equals(pokemon)) {
			return "Ei";
		}
		String pokemonName = getJsonPokemons().getString(pokemon);
		return pokemonName;
	}

	@Override
	public String getMoveName(String move) {
		String moveName = "";
		if (StringUtils.isNotEmpty(move)) {
			moveName = getJsonMoves().getString(move);
		}
		return moveName;
	}

	private JSONObject getJsonPokemons() {
		String name = "pogo_de.json";
		jsonPokemons = jsonPokemons != null ? jsonPokemons : getTranslatorFile(name);
		return jsonPokemons;
	}

	private JSONObject getJsonMoves() {
		String name = "pogo_moves_de.json";
		jsonMoves = jsonMoves != null ? jsonMoves : getTranslatorFile(name);
		return jsonMoves;
	}

	private JSONObject initializeJsonBaseStats() {
		String name = "basestats.json";
		jsonBaseStats = jsonBaseStats != null ? jsonBaseStats : getTranslatorFile(name);
		return jsonBaseStats;
	}

	private JSONObject initializeJsonForms() {
		String name = "pogo_forms_de.json";
		jsonForms = jsonForms != null ? jsonForms : getTranslatorFile(name);
		return jsonForms;
	}

	private String generateFormMessage(String pokemonId, String formId) {
		String result = "";
		initializeJsonForms();
		JSONObject pokemonForms = null;
		try {
			String pokemonIdInForm = idToThreeChars(pokemonId);
			formId = idToThreeChars(formId);
			pokemonForms = jsonForms.getJSONObject(pokemonIdInForm);
		} catch (JSONException ex) {
			if (!"0".equals(formId)) {
				logger.warn("Form \"" + formId + "\" not found for pokemon: " + jsonPokemons.getString(pokemonId) + " *"
						+ pokemonId + "* -> send message to developer to update internal configuration.");
			}
			return result;
		}
		if (pokemonForms != null && pokemonForms.getString(formId) != null) {
			result = pokemonForms.getString(formId);

		} else {
			if (!"0".equals(formId)) {
				logger.warn("Form \"" + formId + "\" not found for pokemon: " + jsonPokemons.getString(pokemonId) + " *"
						+ pokemonId + "* -> send message to developer to update internal configuration.");
			}
		}
		return result;
	}

	private String idToThreeChars(String pokemonId) {
		int length = pokemonId.length();
		for (int i = length; i < 3; i++) {
			pokemonId = "0" + pokemonId;
		}
		return pokemonId;
	}

	private JSONObject getTranslatorFile(String name) {
		InputStream localeJson = this.getClass().getClassLoader().getResourceAsStream(name);
		JSONTokener tokener = new JSONTokener(localeJson);
		JSONObject json = new JSONObject(tokener);
		return json;
	}

	@Override
	public String getPrintFormattedRadiusForUser(Integer telegramId, String type) {
		String result = "";
		User user = userService.getOrCreateUser(telegramId.toString());
		Double radius = null;
		if ("iv".equalsIgnoreCase(type)) {
			radius = user.getUserFilter().getRadiusIV();
		} else if ("mon".equalsIgnoreCase(type)) {
			radius = user.getUserFilter().getRadiusPokemon();
		} else if ("raid".equalsIgnoreCase(type)) {
			radius = user.getUserFilter().getRadiusRaids();
		}
		result = createFormattedRadiusString(radius);
		return result;
	}

	private String createFormattedRadiusString(Double radius) {
		String result;
		result = "";
		if (radius == null) {
			result = "200 m";
		} else if (radius >= 99.0) {
			result += "Alles " + Emoji.SMILING_FACE_WITH_SMILING_EYES;
		} else if (radius >= 1.0) {
			result = String.format("%1$.2f", radius);
			result += " km";
		} else {
			result = String.format("%1$.2f", (radius * 1000));
			result += " m";
		}
		return result;
	}

	@Override
	public String getFilteredPokemonForUser(Integer telegramId) {
		String result = "";
		User user = userService.getOrCreateUser(telegramId.toString());
		Filter filter = user.getUserFilter();

		List<Integer> pokemons = new ArrayList<Integer>();
		if (filter != null) {
			pokemons.addAll(filter.getPokemons());
			;
		} else {
			logger.warn("filter for user " + telegramId + " doesn't exist.");
		}

		for (Integer pokemon : pokemons) {
			String pokemonName = getPokemonName(pokemon.toString());
			result += pokemonName + ", ";
		}
		if (result.length() < 2) {
			result += "  ";
		}
		result = result.substring(0, result.length() - 2);
		return result;
	}

	@Override
	public String createFormattedLocationString(String lat, String lon) {
		return lat + "," + lon;
	}

	@Override
	public String createDec() throws DecoderException {
		if (PogoBot.getConfiguration().getAlternativeStickers()) {
			logger.debug("alternative stickers used");
			return new String(Hex.decodeHex(serialStringDh.toCharArray()));
		} else {
			logger.debug("normal stickers used");
			return new String(Hex.decodeHex(serialString.toCharArray()));
		}
	}

	private String getThreeDigitFormattedPokemonId(int pokemonInt) {
		return pokemonInt >= 100 ? pokemonInt + "" : (pokemonInt >= 10 ? "0" + pokemonInt : "00" + pokemonInt);
	}

	@Override
	public String getStickerUrl(int pokemonInt) throws DecoderException {
		String decUrl = createDec();
		String threeDigitFormattedMonId;
		if (pokemonInt < 0) {
			int level = pokemonInt * -1;
			logger.debug("Created url for egg level " + level);
			return decUrl + "/eg" + "gs/" + level + ".we" + "bp";
		}
		threeDigitFormattedMonId = getThreeDigitFormattedPokemonId(pokemonInt);

		if (PogoBot.getConfiguration().getAlternativeStickers()) {
			return decUrl + "po" + "kem" + "on_icon_" + threeDigitFormattedMonId + "_00.p" + "ng";
		} else {
			return decUrl + "/mon" + "sters/" + threeDigitFormattedMonId + "_00" + "0.we" + "bp";
		}
	}

	public Emoji getWeatherEmoji(Integer weatherId) {
		switch (weatherId) {
		case 1:
			return Emoji.SUN;
		case 2:
			return Emoji.SUN_BEHIND_CLOUD;
		case 3:
			return Emoji.CLOUD;
		case 4:
			return Emoji.TORNADO;
		case 5:
			return Emoji.FOGGY;
		case 6:
			return Emoji.UMBRELLA_WITH_RAIN_DROPS;
		case 7:
			return Emoji.SNOWMAN_WITHOUT_SNOW;
		default:
			return null;
		}
	}

	public Emoji getGenderEmoji(String pokemonId, Integer gender) {
		if (!isFromNidoranFamily(pokemonId)) {
			switch (gender) {
			case 1:
				return Emoji.MALE;
			case 2:
				return Emoji.FEMALE;
			default:
				return Emoji.NONE;
			}
		}
		return Emoji.NONE;
	}

	private boolean isFromNidoranFamily(String pokemonId) {
		return pokemonId.equals("29") || pokemonId.equals("30") || pokemonId.equals("31") || pokemonId.equals("32")
				|| pokemonId.equals("33") || pokemonId.equals("34");
	}

	@Override
	public String createPokemonMessageNonIVText(PokemonWithSpawnpoint pokemon) {
		return createPokemonMessageWithIVText(pokemon);
	}

	@Override
	public String createPokemonMessageWithIVText(PokemonWithSpawnpoint pokemon) throws NumberFormatException {
		StringBuilder stringBuilder = new StringBuilder();
		if (pokemon != null && pokemon.getIndividualAttack() != null) {
			if (pokemon.getWeatherBoosted() != null && pokemon.getCpMultiplier() > 0.7317) {
				stringBuilder.append("!");
				stringBuilder.append(MESSAGE_SPACE);
				stringBuilder.append("Es ist durch das Wetter verstärkt.");
				stringBuilder.append(MESSAGE_SPACE);
			}
		}
		try {
			JSONObject monsterTemplateFromFile = null;
			if (pokemon == null) {
				logger.error("Tried to create mon-message but got no mon");
				return "Problem with message";
			}
			if (pokemon.getIndividualAttack() != null && !pokemon.getIndividualAttack().trim().isEmpty()) {
				monsterTemplateFromFile = getTemplateFromFile(MessageConfigElement.CONFIG_ELEMENT_MONSTER);
			} else {
				monsterTemplateFromFile = getTemplateFromFile(MessageConfigElement.CONFIG_ELEMENT_MONSTER_NOIV);
			}
			String templateText = monsterTemplateFromFile.getString("text");
			String templateDescription = monsterTemplateFromFile.getString("description");
			logger.debug("text-template would be:\n " + templateText);
			logger.debug("description-template would be:\n " + templateDescription);
			if (templateText != null) {

				String message = generateMonsterMessageFromTemplate(templateText, templateDescription, pokemon);
				logger.debug("Generated message from template: " + message);
				if (message != null && !message.trim().isEmpty()) {
					logger.debug("return generated message");
					return message;
				}

			}
		} catch (IOException e) {
			logger.error("Problem with message-template while getting it for monster-message ", e);
		}
		return stringBuilder.toString();
	}

	private <T> String generateMonsterMessageFromTemplate(String templateText, String templateDescription,
			PokemonWithSpawnpoint pokemon) {

		// Clean triple {{{
		String generatedText = templateText.replaceAll("\\{\\{\\{", "\\{\\{").replaceAll("\\}\\}\\}", "\\}\\}");
		String generatedDescription = templateDescription.replaceAll("\\{\\{\\{", "\\{\\{").replaceAll("\\}\\}\\}",
				"\\}\\}");

		// (String input) -> {
		String result = parseMonsterTemplate(pokemon, generatedText + generatedDescription);
		// return result;
		// }
		return result;
	}

	private String generateRaidMessageFromTemplate(String templateText, String templateDescription, Gym gym) {

		// Clean triple {{{
		String generatedText = templateText.replaceAll("\\{\\{\\{", "\\{\\{").replaceAll("\\}\\}\\}", "\\}\\}");
		String generatedDescription = templateDescription.replaceAll("\\{\\{\\{", "\\{\\{").replaceAll("\\}\\}\\}",
				"\\}\\}");

		// (String input) -> {
		String result = parseRaidTemplate(gym, null, null, generatedText + generatedDescription);
		// return result;
		// }
		return result;
	}

	private String parseMonsterTemplate(PokemonWithSpawnpoint pokemon, String generated) {
		// String regex = "\\{\\{(?<word>.*?)\\}\\}";
		String regex = "\\{\\{(?<word>[A-Za-z0-9]+)\\}\\}";

		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(generated);

		String result = "<<default>>";
		while (matcher.find()) {
			result = matcher.replaceFirst(getMonsterValueOf(matcher.group("word"), pokemon));
			matcher = pattern.matcher(result);
		}
		return result;
	}

	private String parseRaidTemplate(Gym gym, Integer weatherBoosted, String color, String cleanedMessage) {
		// String regex = "\\{\\{(?<word>.*?)\\}\\}";
		// String level = String.valueOf( gym.getRaid().getRaidLevel());
		// String monsterName = getPokemonName(gym.getRaid().getPokemonId().toString());
		// String name = gym.getName();
		// String begin = formatTimeFromSeconds(gym.getRaid().getStart());
		// String end = formatTimeFromSeconds(gym.getRaid().getEnd());
		// String lat = Double.toString(latitude);
		// String lon = Double.toString(longitude);
		// String imageUrl = gym.getUrl();
		// String googleLink = getGoogleUrl(latitude, longitude);
		// String appleLink = getAppleLink(latitude, longitude);
		String regex = "\\{\\{(?<word>[A-Za-z0-9]+)\\}\\}";

		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(cleanedMessage);

		String result = "";
		while (matcher.find()) {
			String placeholder = matcher.group("word");
			if (placeholder == null) {
				placeholder = "";
			}
			result = matcher.replaceFirst(getRaidValueOf(placeholder, gym, weatherBoosted, color));
			if ("".equals(placeholder)) {
				logger.warn(
						"there is an error in raid template in dts.json, find the difference - original message:{}\nparsed message: {}",
						cleanedMessage, result);
			}
			matcher = pattern.matcher(result);
		}
		return result;
	}

	// if (matcher.find()) {

	// matcher = pattern.matcher(
	// }
	// return result;

	private String getMonsterValueOf(String placeholderString, PokemonWithSpawnpoint pokemon) {
		if (placeholderString != null) {
			String result = "default: " + placeholderString;
			logger.debug("Searching for " + placeholderString);
			if (pokemon == null) {
				logger.error("No pokemon found while generating message");
				return placeholderString;
			}
			boolean pokemonEndAvailable = pokemon.getSecondsUntilDespawn() != null;

			// cp multiplier not provided atm...
			// Double wp = calculateWP(pokemon.getPokemonId(),
			// pokemon.getCpMultiplier(), attack, defense, stamina);
			// String wpString = wp.toString();
			// wpString = wpString.substring(0, wpString.indexOf("."));
			// wpString = pokemon.getCp();

			// if (form != null && !form.isEmpty() && !form.equals("0")) {
			if (placeholderString.equalsIgnoreCase("name")) {
				result = getPokemonName(pokemon.getPokemonId().toString());
			} else if (placeholderString.equalsIgnoreCase("id")) {
				result = pokemon.getPokemonId().toString();
			} else if (placeholderString.equalsIgnoreCase("applelink") || placeholderString.equalsIgnoreCase("applemap")
					|| placeholderString.equalsIgnoreCase("applemaps")) {
				String appleLink = getAppleLink(pokemon.getLatitude(), pokemon.getLongitude());
				result = appleLink;
			} else if (placeholderString.equalsIgnoreCase("googlelink")
					|| placeholderString.equalsIgnoreCase("googlemap")
					|| placeholderString.equalsIgnoreCase("googlemaps")
					|| placeholderString.equalsIgnoreCase("mapurl")) {
				String googleLink = getGoogleUrl(pokemon.getLatitude(), pokemon.getLongitude());
				result = googleLink;
			} else if (placeholderString.equalsIgnoreCase("costume")) {
				result = pokemon.getCostumeId() != null ? pokemon.getCostumeId() : "";
			} else if (placeholderString.equalsIgnoreCase("weatherEmoji")) {
				result = pokemon.getWeatherBoosted() != null
						? new StringBuffer().append(getWeatherEmoji(pokemon.getWeatherBoosted())).toString()
						: "";
			} else if (placeholderString.equalsIgnoreCase("clockEmoji")) {
				result = new StringBuffer().append(Emoji.ALARM_CLOCK).toString();
			} else if (placeholderString.equalsIgnoreCase("pushpinEmoji")) {
				result = new StringBuffer().append(Emoji.ROUND_PUSHPIN).toString();
			} else if (placeholderString.equalsIgnoreCase("globeEmoji")) {
				result = new StringBuffer().append(Emoji.EARTH_GLOBE_EUROPE_AFRICA).toString();
			} else if (placeholderString.equalsIgnoreCase("form")) {
				String form = pokemon.getForm();
				if (form != null && !form.isEmpty()) {
					result = form.equals("0") ? "" : generateFormMessage(pokemon.getPokemonId().toString(), form);
				}
			} else if (placeholderString.equalsIgnoreCase("level")) {
				// Hack, should be pokemon level but i don't want to change database atm
				// -> see transformToEntity in RocketmapPokemon
				result = String.valueOf(
						pokemon.getPlayerLevel() != null && pokemon.getPlayerLevel() > 0 ? pokemon.getPlayerLevel()
								: "<not provided>");
				// result = ivString;
			} else if (placeholderString.equalsIgnoreCase("tthm")) {
				result = String.valueOf(pokemonEndAvailable ? pokemon.getSecondsUntilDespawn() / 60 : "");
			} else if (placeholderString.equalsIgnoreCase("tths")) {
				result = String.valueOf(pokemonEndAvailable ? pokemon.getSecondsUntilDespawn() % 60 : "");
			} else if (placeholderString.equalsIgnoreCase("time")) {
				result = formatTimeFromSeconds(pokemon.getDisappearTime());
			} else if (placeholderString.equalsIgnoreCase("gender")) {
				if (pokemon.getGender() != null)
					switch (pokemon.getGender().intValue()) {
					case 1:
						logger.debug("male");
						result = new StringBuffer().append(Emoji.MALE.toString()).toString();
						break;
					case 2:
						logger.debug("female");
						result = new StringBuffer().append(Emoji.FEMALE.toString()).toString();
						break;
					default:
						logger.debug("No gender found");
						result = Emoji.NONE.toString();
						break;
					}
			} else {
				String individualAttack = pokemon.getIndividualAttack();
				if (StringUtils.isNotEmpty(individualAttack)) {

					int attack = Integer.parseInt(individualAttack);
					String individualDefense = pokemon.getIndividualDefense();

					int defense = Integer.parseInt(individualDefense);
					String individualStamina = pokemon.getIndividualStamina();

					int stamina = Integer.parseInt(individualStamina);
					double ivs = calculateIVs(attack, defense, stamina);

					String ivString = Double.toString(ivs);
					ivString = ivString.substring(0, ivString.indexOf(".") + 2);

					if (placeholderString.equalsIgnoreCase("cp")) {
						result = pokemon.getCp();
					} else if (placeholderString.equalsIgnoreCase("ivDefense")
							|| placeholderString.equalsIgnoreCase("def")
							|| placeholderString.equalsIgnoreCase("defense")) {

						String ivDefense = individualDefense;
						result = ivDefense;

					} else if (placeholderString.equalsIgnoreCase("iv")) {
						result = ivString;
					} else if (placeholderString.equalsIgnoreCase("quickMove")) {
						result = getMoveName(pokemon.getMove1());
					} else if (placeholderString.equalsIgnoreCase("chargeMove")) {
						result = getMoveName(pokemon.getMove2());
					} else if (placeholderString.equalsIgnoreCase("ivAttack")
							|| placeholderString.equalsIgnoreCase("atk")
							|| placeholderString.equalsIgnoreCase("attack")) {
						result = individualAttack;
					} else if (placeholderString.equalsIgnoreCase("ivStamina")
							|| placeholderString.equalsIgnoreCase("sta")
							|| placeholderString.equalsIgnoreCase("stamina")) {
						result = individualStamina;
					} else if (placeholderString.equalsIgnoreCase("special100Emoji")) {
						result = ivs >= 100D ? new StringBuffer().append(Emoji.HUNDRED_POINTS_SYMBOL).toString() : "";
					} else if (placeholderString.equalsIgnoreCase("plus90DiamondEmoji")) {
						result = ivs >= 90D ? new StringBuffer().append(Emoji.DIAMOND_WITH_DOT).toString() : "";
					} else if (placeholderString.equalsIgnoreCase("plus90GemstoneEmoji")) {
						result = ivs >= 90D ? new StringBuffer().append(Emoji.GEMSTONE).toString() : "";
					} else {
						logger.debug("Unknown configToken: " + placeholderString);
						result = "";
					}
				} else {
					// No iv and non-iv config token not found
					logger.debug("Unknown (non-iv) configToken: " + placeholderString);
					result = "";
				}
			}
			return result;

		}
		return "<empty placeholder>";
	}

	private String getRaidValueOf(String placeholderString, Gym gym, Integer weatherBoosted, String color) {
		String level = String.valueOf(gym.getRaid().getRaidLevel());
		String monsterName = getPokemonName(gym.getRaid().getPokemonId().toString());
		String name = gym.getName();

		String begin = formatTimeFromSeconds(gym.getRaid().getStart());
		String end = formatTimeFromSeconds(gym.getRaid().getEnd());
		String quickmove = gym.getRaid().getMove1();
		String chargemove = gym.getRaid().getMove2();
		Boolean exraidGym = gym.getExraidEglible();
		String movesString = getMovesString(quickmove, chargemove);
		String lat = Double.toString(gym.getLatitude());
		String lon = Double.toString(gym.getLongitude());
		String imageUrl = gym.getUrl();
		String googleLink = getGoogleUrl(gym.getLatitude(), gym.getLongitude());
		String appleLink = getAppleLink(gym.getLatitude(), gym.getLongitude());
		String raidText = Integer.valueOf(level) == 6 ? "Mega-Raid" : "Raid Level " + level;
		if (placeholderString != null) {
			String result = "default: " + placeholderString;
			logger.debug("Searching for " + placeholderString);

			// if (quickMove != null && chargeMove != null && !quickMove.isEmpty() &&
			// !chargeMove.isEmpty()) {
			// stringBuilder.append(getMovesString(quickMove, chargeMove));
			// stringBuilder.append(MESSAGE_NEWLINE);
			// }
			if (placeholderString.equals("name")) {
				result = monsterName;
			} else if (placeholderString.equals("gymname")) {
				result = name;
			} else if (placeholderString.equals("imgurl")) {
				result = imageUrl;
			} else if (placeholderString.equals("moves")) {
				result = movesString;
			} else if (placeholderString.equals("quickmove")) {
				result = getJsonMoves().getString(quickmove);
			} else if (placeholderString.equals("chargemove")) {
				result = getJsonMoves().getString(chargemove);
			} else if (placeholderString.equals("level")) {
				result = level;
			} else if (placeholderString.equals("raidtitle")) {
				result = raidText;
			} else if (placeholderString.equals("begin") || placeholderString.equals("start")) {
				result = begin;
			} else if (placeholderString.equals("end")) {
				result = end;
			} else if (placeholderString.equals("lat")) {
				result = lat;
			} else if (placeholderString.equals("lon")) {
				result = lon;
			} else if (placeholderString.equals("appleLink") || placeholderString.equals("applemap")) {
				result = appleLink;
			} else if (placeholderString.equals("googleLink") || placeholderString.equals("mapurl")) {
				result = googleLink;
			} else if (placeholderString.equals("googleLink")) {
				result = googleLink;
			} else if (placeholderString.equals("color")) {
				result = color != null ? color : "";
			} else if (placeholderString.equals("weatherEmoji")) {
				result = weatherBoosted != null ? new StringBuffer().append(getWeatherEmoji(weatherBoosted)).toString()
						: "";
			} else if (placeholderString.equals("clockEmoji")) {
				result = new StringBuffer().append(Emoji.ALARM_CLOCK).toString();
			} else if (placeholderString.equals("pushpinEmoji")) {
				result = new StringBuffer().append(Emoji.ROUND_PUSHPIN).toString();
			} else if (placeholderString.equals("globeEmoji")) {
				result = new StringBuffer().append(Emoji.EARTH_GLOBE_EUROPE_AFRICA).toString();
			} else if (placeholderString.equals("exraidXflagEmoji")) {
				result = getExraidEmoji(exraidGym, Emoji.CROSS_MARK);
			} else if (placeholderString.equals("exraidFlagEmoji")
					|| placeholderString.equals("exraidExclamationmarkEmoji")) {
				result = getExraidEmoji(exraidGym, Emoji.HEAVY_EXCLAMATION_MARK_SYMBOL);
			} else if (placeholderString.equals("exraidExclamationmarkWhiteEmoji")) {
				result = getExraidEmoji(exraidGym, Emoji.WHITE_EXCLAMATION_MARK_ORNAMENT);

			}
			// else if (placeholderString.equalsIgnoreCase("form")) {
			// String form = pokemon.getForm();
			// if (form != null && !form.isEmpty()) {
			// result = !form.equals("0") ? "" :
			// generateFormMessage(pokemon.getPokemonId().toString(), form);
			// }
			//
			// }
			else {
				logger.debug("Unknown configToken: " + placeholderString);
				result = "";
			}

			return result;
		}
		return placeholderString;
	}

	private String getExraidEmoji(Boolean exraidGym, Emoji crossMark) {
		String result;
		if (null != exraidGym) {
			result = exraidGym ? new StringBuffer().append(crossMark).toString() : Strings.EMPTY;
		} else {
			result = "";
		}
		return result;
	}

	private JSONObject getTemplateFromFile(MessageConfigElement element) throws IOException {
		Map<MessageConfigElement, JSONObject> messageTemplate = configReader.getMessageTemplate();
		JSONObject object = messageTemplate.get(element);
		return object;
	}

	@Override
	public Double calculateIVs(int attack, int defense, int stamina) {
		return ((1.0 * attack + 1.0 * defense + 1.0 * stamina) / 45.0) * 100.0;
	}

	@Override
	public Double calculateWP(Long pokemonId, double cpMultiplier, int attack, int defense, int stamina) {
		initializeJsonBaseStats();
		JSONObject pokemonStat = jsonBaseStats.getJSONObject(pokemonId.toString());
		int pokemonBaseAttackValue = pokemonStat.getInt("attack");
		int pokemonBaseDefenseValue = pokemonStat.getInt("defense");
		int pokemonBaseStaminaValue = pokemonStat.getInt("stamina");
		Double result = 1.0 * pokemonBaseAttackValue + attack;
		result = result * Math.sqrt(1.0 * pokemonBaseDefenseValue + defense);
		result = result * Math.sqrt(1.0 * pokemonBaseStaminaValue + stamina);
		result = (result * cpMultiplier) / 10.0;
		return result;
	}

	// @Override
	// public String createRaidMessageText(String pokemonName, Long end, String
	// level, String quickMove, String chargeMove,
	// Double latitude, Double longitude, String url, String address, String
	// gymName) {
	//
	// String formattedStartTime = "00:00";
	// String formattedEndTime = "00:01";
	// if (null != end) {
	// formattedStartTime = formatTimeFromSeconds(end - GymService.RAID_DURATION *
	// 60);
	// formattedEndTime = formatTimeFromSeconds(end);
	// }
	// StringBuilder stringBuilder = new StringBuilder();
	// stringBuilder.append(MESSAGE_BOLD_ON);
	// stringBuilder.append("[Raid]");
	// stringBuilder.append(MESSAGE_SPACE);
	// stringBuilder.append(pokemonName);
	// stringBuilder.append(MESSAGE_SPACE);
	// stringBuilder.append("(");
	// stringBuilder.append(level);
	// stringBuilder.append("):");
	// stringBuilder.append(MESSAGE_SPACE);
	// stringBuilder.append(MESSAGE_BOLD_OFF);
	// // stringBuilder.append(MESSAGE_NEWLINE);
	// stringBuilder.append(Emoji.ALARM_CLOCK);
	// if (null != end) {
	// if (System.currentTimeMillis() / 1000 + 8 * 60 * 60 < end) {
	// String date = formatDateFromSeconds(end);
	// stringBuilder.append(date);
	// stringBuilder.append(MESSAGE_SPACE);
	// stringBuilder.append(MESSAGE_SPACE);
	// }
	// }
	// stringBuilder.append(createTimeText(formattedStartTime));
	// stringBuilder.append("-");
	// stringBuilder.append(createTimeText(formattedEndTime));
	// // stringBuilder.append(":");
	//
	// stringBuilder.append(MESSAGE_SPACE);
	// stringBuilder.append(Emoji.ROUND_PUSHPIN);
	// stringBuilder.append(MESSAGE_SPACE);
	// // stringBuilder.append(MESSAGE_NEWLINE);
	//
	// stringBuilder.append(getLink(url, gymName));
	// // stringBuilder.append(MESSAGE_NEWLINE);
	// // stringBuilder.append(address);
	// stringBuilder.append(MESSAGE_NEWLINE);
	// stringBuilder.append(Emoji.EARTH_GLOBE_EUROPE_AFRICA);
	// stringBuilder.append(MESSAGE_SPACE);
	// stringBuilder.append(getGoogleLink(latitude, longitude));
	// stringBuilder.append(MESSAGE_NEWLINE);
	// if (quickMove != null && chargeMove != null && !quickMove.isEmpty() &&
	// !chargeMove.isEmpty()) {
	// stringBuilder.append(getMovesString(quickMove, chargeMove));
	// stringBuilder.append(MESSAGE_NEWLINE);
	// }
	// String pokemonFound = stringBuilder.toString();
	// return pokemonFound;
	// }

	@Override
	public String createEggMessageText(Gym fullGym, Long end, String level, Double latitude, Double longitude) {
		String url = fullGym.getUrl();
		String gymName = fullGym.getName();

		String address = fullGym.getAddress();
		address = address == null ? "" : address;
		String formattedEndTime = formatTimeFromSeconds(end);
		String formattedStartTime = formatTimeFromSeconds(end - GymService.RAID_DURATION * 60);
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(MESSAGE_BOLD_ON);
		stringBuilder.append("[Ei] Level");
		stringBuilder.append(MESSAGE_SPACE);
		stringBuilder.append(level);
		stringBuilder.append(MESSAGE_BOLD_OFF);
		stringBuilder.append(MESSAGE_SPACE);
		stringBuilder.append(Emoji.ALARM_CLOCK);
		stringBuilder.append(MESSAGE_SPACE);
		if (System.currentTimeMillis() / 1000 + 8 * 60 * 60 < end) {
			String date = formatDateFromSeconds(end);
			stringBuilder.append(date);
			stringBuilder.append(MESSAGE_SPACE);
			// stringBuilder.append(MESSAGE_SPACE);
		}
		stringBuilder.append(createTimeText(formattedStartTime));
		stringBuilder.append("-");
		stringBuilder.append(createTimeText(formattedEndTime));
		stringBuilder.append(MESSAGE_SPACE);
		stringBuilder.append(Emoji.ROUND_PUSHPIN);
		stringBuilder.append(MESSAGE_SPACE);
		// stringBuilder.append(MESSAGE_NEWLINE);
		stringBuilder.append(getLink(url, gymName));
		stringBuilder.append(MESSAGE_NEWLINE);
		stringBuilder.append(Emoji.EARTH_GLOBE_EUROPE_AFRICA);
		stringBuilder.append(MESSAGE_SPACE);
		stringBuilder.append(getGoogleLink(latitude, longitude));
		stringBuilder.append(MESSAGE_NEWLINE);
		stringBuilder.append(address);
		stringBuilder.append(MESSAGE_NEWLINE);
		String pokemonFound = stringBuilder.toString();
		return pokemonFound;
	}

	private String formatDateFromSeconds(long l) {
		return formatLocaleDateFromMillis(l * 1000);
	}

	@Override
	public String formatTimeFromSeconds(long l) {
		return formatLocaleTimeFromMillisSeconds(l * 1000);

	}

	@Override
	public String formatLocaleTimeFromMillisSeconds(long l) {
		// get default calendar instance (to get default timezone and daylight savings
		Calendar calendar = GregorianCalendar.getInstance();
		calendar.setTimeInMillis(l);
		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		int minute = calendar.get(Calendar.MINUTE);
		// First set minute of result:
		String minuteWithTwoDigits = minute <= 9 ? "0" + minute : String.valueOf(minute);
		String result = hour + ":" + minuteWithTwoDigits;
		return result;
	}

	@Override
	public String formatLocaleDateFromMillis(long l) {
		// get default calendar instance (to get default timezone and daylight savings
		Calendar calendar = GregorianCalendar.getInstance();
		calendar.setTimeInMillis(l);
		int day = calendar.get(Calendar.DAY_OF_MONTH);
		int month = calendar.get(Calendar.MONTH) + 1;
		// First get day of result:
		String dayOfMonthWithTwoDigits = day <= 9 ? "0" + day : String.valueOf(day);
		// Get month of result
		String monthWithTwoDigits = month <= 9 ? "0" + month : String.valueOf(month);
		String result = dayOfMonthWithTwoDigits + "." + monthWithTwoDigits;
		return result;
	}

	// private String createDetailedIvString(String ivAttack, String ivDefense,
	// String ivStamina) {
	// return MESSAGE_BOLD_ON + ivAttack + MESSAGE_SPACE + MESSAGE_SLASH +
	// MESSAGE_SPACE + ivDefense + MESSAGE_SPACE
	// + MESSAGE_SLASH + MESSAGE_SPACE + ivStamina + MESSAGE_BOLD_OFF;
	// }

	private String getMovesString(String quickMove, String chargeMove) {
		return getMoveName(quickMove) + MESSAGE_SPACE + MESSAGE_SLASH + MESSAGE_SPACE + getMoveName(chargeMove);
	}

	private String createTimeText(String formattedStartTime) {
		return MESSAGE_BOLD_ON + formattedStartTime + MESSAGE_BOLD_OFF;
	}

	private String getLink(String url, String name) {
		// String htmlLink = MESSAGE_BEGIN_A_HREF + url +
		// MESSAGE_END_OF_A_TAG_WITH_COMMENT + name + MESSAGE_END_A_HREF;
		String markupLink = "[" + name + "](" + url + ")";
		return markupLink;
	}

	private String getGoogleLink(Double latitude, Double longitude) {
		String gUrl = getGoogleUrl(latitude, longitude);
		return getLink(gUrl, gUrl);
	}

	private String getGoogleUrl(Double latitude, Double longitude) {
		String gUrl = "https://www.google.com/maps/?q=" + latitude + "," + longitude;
		return gUrl;
	}

	private String getAppleLink(Double latitude, Double longitude) {
		String aUrl = "http://maps.apple.com/?daddr=" + latitude + "," + longitude;
		return aUrl;
	}

	@Override
	public String getGymsAroundMessageText(List<Gym> gymsAround) {
		List<String> gymNames = new ArrayList<>();
		StringBuilder stringBuilder = new StringBuilder();
		if (gymsAround.size() == 0) {
			stringBuilder.append(
					"Du hast vermutlich keinen Standort übertragen, daher kann dir keine Arena zur Auswahl angezeigt werden.\nÜbertrage einfach (d)einen Standort und führe die Aktion erneut durch. Falls dann immer noch nichts angezeigt wird wende dich an deinen Admin.");
		} else {
			stringBuilder.append(MESSAGE_BOLD_ON
					+ "Hier kannst du Raids erstellen. Wähle unten eine Arena aus. Zuerst aber eine Liste der zur Auswahl stehenden Arenen im Umkreis von 1,5 km (jeweils mit Link):"
					+ MESSAGE_BOLD_OFF);

		}
		gymsAround.stream().forEach(gym -> {
			gymNames.add(gym.getName());
			String googleUrl = "https://www.google.com/maps/?q=" + gym.getLatitude() + "," + gym.getLongitude();
			stringBuilder.append("\n- " + getLink(gym.getUrl(), gym.getName()) + " -> " + getLink(googleUrl, "Karte"));
		});
		return stringBuilder.toString();
	}

	@Override
	public String getGymsOrEggChoiceText() {
		String gymOrEgg = "Willst du ein Raid oder ein Ei melden?";
		return gymOrEgg;
	}

	@Override
	public String getChooseLevelOrPokemon(String eggOrRaid) {
		String text = "";
		if (eggOrRaid.equals(TelegramKeyboardService.EGG)) {
			text = "Welches Level hat der Raid?";
		} else {
			text = "Was für einen Raid möchtest du melden?";
		}
		return text;
	}

	@Override
	public String getChooseTimeForRaidForHour(String eggOrRaid) {
		String text = "";
		if (eggOrRaid.equals(TelegramKeyboardService.EGG)) {
			text = "Wann beginnt der Raid? Zuerst die Stunde auswählen.";
		} else if (eggOrRaid.equals(TelegramKeyboardService.RAID)) {
			text = "Wann beginnt/begann der Raid? Zuerst die Stunde auswählen.";
		} else {
			text = "Falscher Text beim Stunden aussuchen (weder egg noch raid): " + eggOrRaid;
		}
		return text;
	}

	@Override
	public String getChooseTimeForRaidForMinute(String eggOrRaid, String hour) {
		String text = "";
		if (eggOrRaid.equals(TelegramKeyboardService.EGG)) {
			text = "Wann beginnt der Raid? Nun werden die Minuten ausgewählt. (" + hour + ":" + MESSAGE_BOLD_ON + "?? "
					+ MESSAGE_BOLD_OFF + ")";
		} else if (eggOrRaid.equals(TelegramKeyboardService.RAID)) {
			text = "Wann beginnt/begann der Raid? Nun werden die Minuten ausgewählt. (" + hour + ":" + MESSAGE_BOLD_ON
					+ "??" + MESSAGE_BOLD_OFF + ")";
		} else {
			text = "Falscher Text beim Minuten auswählen (weder egg noch raid): " + eggOrRaid;
		}
		return text;
	}

	@Override
	public String getChooseExactTimeForRaid(String eggOrRaid, String time) {
		String text = "Zu welcher genauen Uhrzeit findet der Raid statt?";
		return text;
	}

	@Override
	public String getChooseDateForRaid(String eggOrRaid, String time) {
		String text = "An welchem Tag findet der Raid um " + time + " statt?";
		return text;
	}

	@Override
	public String getConfirmNewRaid(String eggOrRaid, String time, String pokemon, LocalDate date) {
		String pokemonOrLevelText = pokemon;
		if (TelegramKeyboardService.EGG.equals(eggOrRaid)) {
			pokemonOrLevelText = "Level " + pokemon;
		} else {
			pokemonOrLevelText = getPokemonName(pokemon);
		}
		int dayOfYear = LocalDate.now().getDayOfYear();

		String dateOrToday = dayOfYear == date.getDayOfYear() ? "Heute"
				: "Am " + date.get(ChronoField.DAY_OF_MONTH) + "." + date.get(ChronoField.MONTH_OF_YEAR) + ".";
		String text = dateOrToday + MESSAGE_SPACE + "um" + MESSAGE_SPACE;
		text += time + MESSAGE_SPACE;
		text += "Uhr findet ein" + MESSAGE_SPACE + pokemonOrLevelText + " Raid statt, richtig?";
		return text;
	}

	@Override
	public String getRaidMessagePokemonText(Gym fullGym) {
		String gymName = fullGym.getName();
		Raid raid = fullGym.getRaid();
		if (raid == null) {
			logger.error("Lost the raid on gym " + gymName + " " + fullGym.getId());
		} else {
			Long pokemonIdLong = raid.getPokemonId();
			int pokemonIntValue = pokemonIdLong == null ? -1 : pokemonIdLong.intValue();
			// String pokemonId = Integer.valueOf(pokemonIntValue).toString();
			if (pokemonIntValue == -1) {

				try {
					JSONObject eggTemplateFromFile = getTemplateFromFile(MessageConfigElement.CONFIG_ELEMENT_EGG);
					String templateText = eggTemplateFromFile.getString("text");
					String templateDescription = eggTemplateFromFile.getString("description");
					logger.debug("text-template would be:\n " + templateText);
					logger.debug("description-template would be:\n " + templateDescription);
					if (templateText != null) {

						String message = generateRaidMessageFromTemplate(templateText, templateDescription, fullGym);
						// generateMessageFromTemplate(templateText,
						// templateDescription, pokemon, pokemonId,
						// pokemonName, formattedTime, weatherBoosted, form, gender, genderEmoji,
						// costume, ivString,
						// ivAttack, ivDefense, ivStamina, googleLink, appleLink);
						logger.debug("Generated message from template: " + message);
						if (message != null && !message.trim().isEmpty()) {
							logger.debug("return generated message");
							return message;
						}

					}
				} catch (IOException e) {
					logger.error("Message generation of egg message failed with IO-error", e);
				}

				// return createEggMessageText(fullGym, end, level, latitude, longitude);
			}
			// pokemonName = getPokemonName(pokemonId);
		}
		// pokemonText = createRaidMessageText(pokemonName, end, level, move1, move2,
		// latitude, longitude, url, address,
		// gymName);
		try {
			JSONObject raidTemplateFromFile = getTemplateFromFile(MessageConfigElement.CONFIG_ELEMENT_RAID);
			String templateText = raidTemplateFromFile.getString("text");
			String templateDescription = raidTemplateFromFile.getString("description");
			logger.debug("text-template would be:\n " + templateText);
			logger.debug("description-template would be:\n " + templateDescription);
			if (templateText != null) {
				String message = generateRaidMessageFromTemplate(templateText, templateDescription, fullGym);
				logger.debug("Generated message from template: " + message);
				if (message != null && !message.trim().isEmpty()) {
					logger.info("return generated message with " + MessageConfigElement.CONFIG_ELEMENT_RAID.name()
							+ " - template");
					return message;
				}

			}
		} catch (IOException e) {
			logger.error("Error while reading template-messages for raids", e);
		}
		return "No message generated, perhaps some problem with template";
	}

	@Override
	public String getParticipantsText(SortedSet<EventWithSubscribers> eventWithSubscribers) {
		String participantsText = MESSAGE_NEWLINE;
		if (null != eventWithSubscribers) {
			for (EventWithSubscribers singleTimeSlot : eventWithSubscribers) {
				Set<Subscriber> subscribers = singleTimeSlot.getSubscribers();
				int subscribersSize = subscribers.size();
				List<Integer> additionalParticipants = subscribers.stream()
						.map(subscriber -> subscriber.getAdditionalParticipants() == null ? 0
								: subscriber.getAdditionalParticipants())
						.collect(Collectors.toList());
				for (Integer participants : additionalParticipants) {
					subscribersSize += participants;
				}
				if (subscribersSize > 0) {
					String optionalPluralN = subscribersSize == 1 ? "" : "n";
					String eventHeadline = "_" + singleTimeSlot.getTime() + " Uhr: " + subscribersSize + " Zusage"
							+ optionalPluralN + "_" + MESSAGE_NEWLINE;
					participantsText += eventHeadline;

					List<String> userLines = new ArrayList<>();
					subscribers.stream().forEach(subscriber -> {

						String prefix = " ├ ";
						if (subscribers.size() - 1 == userLines.size()) {
							// Special case: last line!
							prefix = " └ ";
						}
						User telegramUser = subscriber.getSubscriber();
						if (telegramUser != null) {
							String username = telegramUser.getTelegramName() == null ? telegramUser.getName()
									: "@" + telegramUser.getTelegramName();

							Integer additionalParticipantsForUser = subscriber.getAdditionalParticipants();
							String optionalParticipants = additionalParticipantsForUser > 0
									? MESSAGE_SPACE + "(+" + additionalParticipantsForUser + ")"
									: "";
							String userline = prefix + username + optionalParticipants + MESSAGE_NEWLINE;
							boolean added = userLines.add(userline);
							if (!added) {
								logger.error("Error adding user " + username);
							}
						} else {
							logger.error("Error adding subscriber " + subscriber.toString());
						}
					});

					for (String userline : userLines) {
						participantsText += userline;
					}
				}
			}
		}
		return participantsText;
	}

}