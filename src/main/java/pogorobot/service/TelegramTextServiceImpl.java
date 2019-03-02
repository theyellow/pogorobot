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
import java.util.Date;
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
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import POGOProtos.Enums.PokemonIdOuterClass.PokemonId;
import pogorobot.entities.EventWithSubscribers;
import pogorobot.entities.Filter;
import pogorobot.entities.Gym;
import pogorobot.entities.MessageConfigElement;
import pogorobot.entities.PokemonWithSpawnpoint;
import pogorobot.entities.Raid;
import pogorobot.entities.Subscriber;
import pogorobot.entities.User;
import pogorobot.telegram.PogoBot;
import pogorobot.telegram.util.Emoji;

@Service("telegramTextService")
public class TelegramTextServiceImpl<R> implements TelegramTextService {

	Logger logger = LoggerFactory.getLogger(this.getClass().getInterfaces()[0]);

	private static final String MESSAGE_SPACE = " ";

	private static final String MESSAGE_NEWLINE = "\n";

	private static final String MESSAGE_END_A_HREF = "</a>";

	private static final String MESSAGE_END_OF_A_TAG_WITH_COMMENT = "\">";

	private static final String MESSAGE_BEGIN_A_HREF = "<a href=\"";

	private static final String MESSAGE_SLASH = "/";

	private static final String MESSAGE_BOLD_ON = "*"; // "<b>";

	private static final String MESSAGE_BOLD_OFF = "*";// "</b>";

	private static final String LOGTAG = "TELEGRAM TEXT SERVICE";

	private String serialString = "68747470733a2f2f6d6f6e73746572696d616765732e746b2f76312e342f74656c656772616d";
	private String serialStringDh = "2f2f2068747470733a2f2f6769746875622e636f6d2f506f676f64656e68656c6465722f737072697465732f626c6f622f6d61737465722f";

	private JSONObject jsonMoves;

	private JSONObject jsonPokemons;

	@Autowired
	private UserService userService;

	@Autowired
	private ConfigReader configReader;

	private JSONObject jsonBaseStats;

	private JSONObject jsonForms;

	public String getPokemonNameFromProtobuf(String pokemon) {
		if ("-1".equals(pokemon)) {
			return "Ei";
		}
		int value = Integer.valueOf(pokemon);
		PokemonId pokemonId = PokemonId.forNumber(value);
		String normalizedPokemonName = pokemonId.name();
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
		String moveName = getJsonMoves().getString(move);
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

	private String generateFormMessage(String pokemonId, String form) {
		String result = "";
		initializeJsonForms();
		JSONObject pokemonForms = null;
		try {
			pokemonForms = jsonForms.getJSONObject(pokemonId);
		} catch (JSONException ex) {
			logger.error("Form \"" + form + "\" not found for pokemon: " + jsonPokemons.getString(pokemonId)
					+ " -> send message to developer to update internal configuration.");
		}
		if (pokemonForms != null && pokemonForms.getString(form) != null) {
			result = pokemonForms.getString(form);

		}
		return result;
	}

	private JSONObject getTranslatorFile(String name) {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		InputStream localeJson = classLoader.getResourceAsStream(name);
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
		List<Integer> pokemons = filter.getPokemons();
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
			logger.info("alternative stickers used");
			return new String(Hex.decodeHex(serialStringDh.toCharArray()));
		} else {
			logger.info("normal stickers used");
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
			return decUrl + "/eg" + "gs/" + (pokemonInt * -1) + ".we" + "bp";
		} else
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
	public String createPokemonMessageNonIVText(String formattedTime, String pokemonName, String pokemonId, String form,
			String costume, Long gender, Integer weatherBoosted, Double latitude, Double longitude) {
		return createPokemonMessageWithIVText(formattedTime, pokemonName, pokemonId, form, costume, gender,
				weatherBoosted, latitude, longitude, null);
	}

	@Override
	public String createPokemonMessageWithIVText(String formattedTime, String pokemonName, String pokemonId,
			String form, String costume, Long gender, Integer weatherBoosted, Double latitude, Double longitude,
			PokemonWithSpawnpoint pokemon) throws NumberFormatException {
		String ivAttack = "0";
		String ivDefense = "0";
		String ivStamina = "0";
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(MESSAGE_BOLD_ON);
		stringBuilder.append("[Pokémon]");
		stringBuilder.append(MESSAGE_BOLD_OFF);
		stringBuilder.append(MESSAGE_SPACE);
		stringBuilder.append("Ein ");
		stringBuilder.append(MESSAGE_BOLD_ON);
		String ivString = "";
		Emoji genderEmoji = null;
		stringBuilder.append(pokemonName);
		stringBuilder.append(MESSAGE_SPACE);
		if (gender != null) {
			genderEmoji = getGenderEmoji(pokemonId, gender.intValue());
		}
		if (null != genderEmoji || !Emoji.NONE.equals(genderEmoji)) {
			stringBuilder.append(genderEmoji);
		}
		if (form != null && !form.isEmpty() && !form.equals("0")) {
			stringBuilder.append(MESSAGE_SPACE);
			stringBuilder.append(generateFormMessage(pokemonId, form));
		}
		if (pokemon != null) {
			ivAttack = pokemon.getIndividualAttack();
			ivDefense = pokemon.getIndividualDefense();
			ivStamina = pokemon.getIndividualStamina();
			int attack = Integer.parseInt(ivAttack);
			int defense = Integer.parseInt(ivDefense);
			int stamina = Integer.parseInt(ivStamina);
			double ivs = calculateIVs(attack, defense, stamina);
			// Double wp = calculateWP(pokemon.getPokemonId(),
			// pokemon.getCpMultiplier(), attack, defense, stamina);
			// String wpString = wp.toString();
			// wpString = wpString.substring(0, wpString.indexOf("."));
			// wpString = pokemon.getCp();
			ivString = Double.toString(ivs);
			ivString = ivString.substring(0, ivString.indexOf(".") + 2);
			stringBuilder.append(" mit ");
			stringBuilder.append(ivString);
			stringBuilder.append(" % und ");
			stringBuilder.append(pokemon.getCp());
			stringBuilder.append(" WP");
			if (pokemon.getWeatherBoosted() != null && pokemon.getCpMultiplier() > 0.7317) {
				stringBuilder.append("!");
				stringBuilder.append(MESSAGE_SPACE);
				stringBuilder.append("Es ist durch das Wetter verstärkt.");
				stringBuilder.append(MESSAGE_SPACE);
			}
		}
		if (weatherBoosted != null) {
			stringBuilder.append(MESSAGE_SPACE);
			stringBuilder.append(getWeatherEmoji(weatherBoosted));
		}
		stringBuilder.append("!");
		stringBuilder.append(MESSAGE_BOLD_OFF);
		stringBuilder.append(MESSAGE_SPACE);
		stringBuilder.append(MESSAGE_BOLD_ON);
		stringBuilder.append("Bis ");
		stringBuilder.append(formattedTime);
		stringBuilder.append(MESSAGE_BOLD_OFF);
		stringBuilder.append(" kannst du es fangen!");
		if (pokemon != null) {
			stringBuilder.append(MESSAGE_SPACE);
			stringBuilder.append("Die genauen Werte sind: ");
			stringBuilder.append(createDetailedIvString(ivAttack, ivDefense, ivStamina));
		}
		stringBuilder.append(MESSAGE_NEWLINE);
		String googleLink = getGoogleUrl(latitude, longitude);
		String appleLink = getAppleLink(latitude, longitude);
		stringBuilder.append("[Google Maps](" + googleLink + ") [AppleLink](" + appleLink + ")");
		// stringBuilder.append(googleLink);
		try {
			JSONObject monsterTemplateFromFile = null;
			if (ivString != null && !ivString.trim().isEmpty()) {
				monsterTemplateFromFile = getTemplateFromFile(MessageConfigElement.CONFIG_ELEMENT_MONSTER);
			} else {
				monsterTemplateFromFile = getTemplateFromFile(MessageConfigElement.CONFIG_ELEMENT_MONSTER_NOIV);
			}
			String templateText = monsterTemplateFromFile.getString("text");
			String templateDescription = monsterTemplateFromFile.getString("description");
			logger.debug("text-template would be:\n " + templateText);
			logger.debug("description-template would be:\n " + templateDescription);
			if (templateText != null) {

				String message = generateMonsterMessageFromTemplate(templateText, templateDescription, pokemon,
						pokemonId, pokemonName, formattedTime, weatherBoosted, form, gender, genderEmoji, costume,
						ivString, ivAttack, ivDefense, ivStamina, googleLink, appleLink);
				logger.info("Generated message from template: " + message);
				if (message != null && !message.trim().isEmpty()) {
					logger.debug("return generated message");
					return message;
				}

			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return stringBuilder.toString();
	}

	private <T> String generateMonsterMessageFromTemplate(String templateText, String templateDescription,
			PokemonWithSpawnpoint pokemon, String pokemonId, String pokemonName, String formattedTime,
			Integer weatherBoosted, String form, Long gender, Emoji genderEmoji, String costume, String ivString,
			String ivAttack, String ivDefense, String ivStamina, String googleLink, String appleLink) {

		// Clean triple {{{
		String generatedText = templateText.replaceAll("\\{\\{\\{", "\\{\\{").replaceAll("\\}\\}\\}", "\\}\\}");
		String generatedDescription = templateDescription.replaceAll("\\{\\{\\{", "\\{\\{").replaceAll("\\}\\}\\}",
				"\\}\\}");

		// (String input) -> {
		String result = parseMonsterTemplate(pokemon, pokemonId, pokemonName, formattedTime, weatherBoosted, form,
				gender, genderEmoji, costume, ivString, ivAttack, ivDefense, ivStamina, googleLink, appleLink,
				generatedText + generatedDescription);
		// return result;
		// }
		return result;
	}

	private String generateRaidMessageFromTemplate(String templateText, String templateDescription, Gym gym, Long end,
			String level, Double latitude, Double longitude) {

		// Clean triple {{{
		String generatedText = templateText.replaceAll("\\{\\{\\{", "\\{\\{").replaceAll("\\}\\}\\}", "\\}\\}");
		String generatedDescription = templateDescription.replaceAll("\\{\\{\\{", "\\{\\{").replaceAll("\\}\\}\\}",
				"\\}\\}");

		// (String input) -> {
		String result = parseRaidTemplate(String.valueOf(gym.getRaid().getRaidLevel()),
				getPokemonName(gym.getRaid().getPokemonId().toString()), gym.getName(),
				formatTimeFromSeconds(gym.getRaid().getStart()), formatTimeFromSeconds(gym.getRaid().getEnd()),
				Double.toString(latitude), Double.toString(longitude), null, null, gym.getUrl(),
				getGoogleUrl(latitude, longitude), getAppleLink(latitude, longitude),
				generatedText + generatedDescription);
		// return result;
		// }
		return result;
	}

	private String parseMonsterTemplate(PokemonWithSpawnpoint pokemon, String pokemonId, String pokemonName,
			String formattedTime, Integer weatherBoosted, String form, Long gender, Emoji genderEmoji, String costume,
			String ivString, String ivAttack, String ivDefense, String ivStamina, String googleLink, String appleLink,
			String generated) {
		// String regex = "\\{\\{(?<word>.*?)\\}\\}";
		String regex = "\\{\\{(?<word>[A-Za-z]+)\\}\\}";

		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(generated);

		String result = "<<default>>";
		while (matcher.find()) {
			result = matcher.replaceFirst(getMonsterValueOf(matcher.group("word"), pokemon, pokemonId, pokemonName,
					formattedTime, weatherBoosted, form, gender, genderEmoji, costume, ivString, ivAttack, ivDefense,
					ivStamina, googleLink, appleLink));
			matcher = pattern.matcher(result);
		}
		return result;
	}

	private String parseRaidTemplate(String level, String monsterName, String name, String begin, String end,
			String lat, String lon, Integer weatherBoosted, String color, String imageUrl, String googleLink,
			String appleLink, String generated) {
		// String regex = "\\{\\{(?<word>.*?)\\}\\}";
		String regex = "\\{\\{(?<word>[A-Za-z]+)\\}\\}";

		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(generated);

		String result = "<<default>>";
		while (matcher.find()) {
			result = matcher.replaceFirst(getRaidValueOf(matcher.group("word"), level, monsterName, name, begin, end,
					lat, lon, weatherBoosted, color, imageUrl, googleLink, appleLink));
			matcher = pattern.matcher(result);
		}
		return result;
	}

	// if (matcher.find()) {

	// matcher = pattern.matcher(
	// }
	// return result;

	private String getMonsterValueOf(String placeholderString, PokemonWithSpawnpoint pokemon, String pokemonId,
			String pokemonName, String formattedTime, Integer weatherBoosted, String form, Long gender,
			Emoji genderEmoji, String costume, String ivString, String ivAttack, String ivDefense, String ivStamina,
			String googleLink, String appleLink) {
		if (placeholderString != null) {
			String result = "default: " + placeholderString;
			logger.debug("Searching for " + placeholderString);

			if (placeholderString.equals("name")) {
				result = pokemonName;
			} else if (placeholderString.equals("id")) {
				result = pokemonId;
			} else if (placeholderString.equals("iv")) {
				result = ivString;
			} else {
				boolean pokemonEndAvailable = pokemon != null && pokemon.getSecondsUntilDespawn() != null;
				if (placeholderString.equals("tthm")) {
					result = String.valueOf(pokemonEndAvailable ? pokemon.getSecondsUntilDespawn() / 60 : "");
				} else if (placeholderString.equals("tths")) {
					result = String.valueOf(pokemonEndAvailable ? pokemon.getSecondsUntilDespawn() % 60 : "");
				} else if (placeholderString.equals("level")) {
					// Hack, should be pokemon level but i don't want to change database atm
					// -> see transformToEntity in RocketmapPokemon
					result = String.valueOf(pokemon.getPlayerLevel() != null ? pokemon.getPlayerLevel() : "");
				} else if (placeholderString.equals("ivAttack") || placeholderString.equals("atk")) {
					result = ivAttack;

				} else if (placeholderString.equals("ivStamina") || placeholderString.equals("sta")) {
					result = ivStamina;
				} else if (placeholderString.equals("appleLink") || placeholderString.equals("applemap")) {
					result = appleLink;
				} else if (placeholderString.equals("googleLink") || placeholderString.equals("mapurl")) {
					result = googleLink;
				} else if (placeholderString.equals("googleLink")) {
					result = googleLink;
				} else if (placeholderString.equals("costume")) {
					result = costume != null ? costume : "";
				} else if (placeholderString.equals("weatherEmoji")) {
					result = weatherBoosted != null
							? new StringBuffer().append(getWeatherEmoji(weatherBoosted)).toString()
							: "";
				} else if (placeholderString.equals("form")) {
					result = form != null && !form.equals("0") ? generateFormMessage(pokemonId, form) : "";
				} else if (placeholderString.equals("ivDefense") || placeholderString.equals("def")) {
					result = ivDefense;

				} else if (placeholderString.equals("gender")) {
					if (gender != null)
						switch (gender.intValue()) {
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
				} else if (placeholderString.equals("time")) {
					result = formattedTime;
				} else if (placeholderString.equals("id")) {
					result = pokemonId;
				} else if (placeholderString.equals("cp")) {
					result = pokemon.getCp();
				} else {
					logger.debug("Unknown configToken: " + placeholderString);
					result = "";
				}
			}

			return result;
		}
		return placeholderString;
	}

	private String getRaidValueOf(String string, String level, String monsterName, String gymName, String begin,
			String end, String lat, String lon, Integer weatherBoosted, String color, String imageUrl,
			String googleLink, String appleLink) {
		if (string != null) {
			String result = "default: " + string;
			logger.debug("Searching for " + string);

			if (string.equals("name")) {
				result = monsterName;
			} else if (string.equals("gymname")) {
				result = gymName;
			} else if (string.equals("imgurl")) {
				result = imageUrl;
			} else if (string.equals("level")) {
				result = level;
			} else if (string.equals("begin") || string.equals("start")) {
				result = begin;
			} else if (string.equals("end")) {
				result = end;
			} else if (string.equals("lat")) {
				result = lat;
			} else if (string.equals("lon")) {
				result = lon;
			} else if (string.equals("appleLink") || string.equals("applemap")) {
				result = appleLink;
			} else if (string.equals("googleLink") || string.equals("mapurl")) {
				result = googleLink;
			} else if (string.equals("googleLink")) {
				result = googleLink;
			} else if (string.equals("color")) {
				result = color != null ? color : "";
			} else if (string.equals("weatherEmoji")) {
				result = weatherBoosted != null ? new StringBuffer().append(getWeatherEmoji(weatherBoosted)).toString()
						: "";
			} else {
				logger.debug("Unknown configToken: " + string);
				result = "";
			}

			return result;
		}
		return string;
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

	@Override
	public String createRaidMessageText(String pokemonName, Long end, String level, String quickMove, String chargeMove,
			Double latitude, Double longitude, String url, String address, String gymName) {

		String formattedStartTime = "00:00";
		String formattedEndTime = "00:01";
		if (null != end) {
			formattedStartTime = formatTimeFromSeconds(end - GymService.RAID_DURATION * 60);
			formattedEndTime = formatTimeFromSeconds(end);
		}
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(MESSAGE_BOLD_ON);
		stringBuilder.append("[Raid]");
		stringBuilder.append(MESSAGE_SPACE);
		stringBuilder.append(pokemonName);
		stringBuilder.append(MESSAGE_SPACE);
		stringBuilder.append("(");
		stringBuilder.append(level);
		stringBuilder.append("):");
		stringBuilder.append(MESSAGE_SPACE);
		stringBuilder.append(MESSAGE_BOLD_OFF);
		// stringBuilder.append(MESSAGE_NEWLINE);
		stringBuilder.append(Emoji.ALARM_CLOCK);
		if (null != end) {
			if (new Date().getTime() / 1000 + 8 * 60 * 60 < end) {
				String date = formatDateFromSeconds(end);
				stringBuilder.append(date);
				stringBuilder.append(MESSAGE_SPACE);
				stringBuilder.append(MESSAGE_SPACE);
			}
		}
		stringBuilder.append(createTimeText(formattedStartTime));
		stringBuilder.append("-");
		stringBuilder.append(createTimeText(formattedEndTime));
		// stringBuilder.append(":");

		stringBuilder.append(MESSAGE_SPACE);
		stringBuilder.append(Emoji.ROUND_PUSHPIN);
		stringBuilder.append(MESSAGE_SPACE);
		// stringBuilder.append(MESSAGE_NEWLINE);

		stringBuilder.append(getLink(url, gymName));
		// stringBuilder.append(MESSAGE_NEWLINE);
		// stringBuilder.append(address);
		stringBuilder.append(MESSAGE_NEWLINE);
		stringBuilder.append(Emoji.EARTH_GLOBE_EUROPE_AFRICA);
		stringBuilder.append(MESSAGE_SPACE);
		stringBuilder.append(getGoogleLink(latitude, longitude));
		stringBuilder.append(MESSAGE_NEWLINE);
		if (quickMove != null && chargeMove != null && !quickMove.isEmpty() && !chargeMove.isEmpty()) {
			stringBuilder.append(getMovesString(quickMove, chargeMove));
			stringBuilder.append(MESSAGE_NEWLINE);
		}
		String pokemonFound = stringBuilder.toString();
		return pokemonFound;
	}

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
		if (new Date().getTime() / 1000 + 8 * 60 * 60 < end) {
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

	private String createDetailedIvString(String ivAttack, String ivDefense, String ivStamina) {
		return MESSAGE_BOLD_ON + ivAttack + MESSAGE_SPACE + MESSAGE_SLASH + MESSAGE_SPACE + ivDefense + MESSAGE_SPACE
				+ MESSAGE_SLASH + MESSAGE_SPACE + ivStamina + MESSAGE_BOLD_OFF;
	}

	private String getMovesString(String quickMove, String chargeMove) {
		return getMoveName(quickMove) + MESSAGE_SPACE + MESSAGE_SLASH + MESSAGE_SPACE + getMoveName(chargeMove);
	}

	private String createTimeText(String formattedStartTime) {
		return MESSAGE_BOLD_ON + formattedStartTime + MESSAGE_BOLD_OFF;
	}

	private String getLink(String url, String name) {
		String htmlLink = MESSAGE_BEGIN_A_HREF + url + MESSAGE_END_OF_A_TAG_WITH_COMMENT + name + MESSAGE_END_A_HREF;
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
		String pokemonText;
		String url = fullGym.getUrl();
		String address = fullGym.getAddress();
		String gymName = fullGym.getName();
		Double longitude = fullGym.getLongitude();
		Double latitude = fullGym.getLatitude();
		Raid raid = fullGym.getRaid();
		String level = "Unbekannt";
		String move1 = "";
		String move2 = "";
		Long end = null;
		String pokemonName = "";
		if (raid == null) {
			logger.error("Lost the raid on gym " + gymName + " " + fullGym.getId());
		} else {
			Long raidLevel = raid.getRaidLevel();
			level = raidLevel != null ? raidLevel.toString() : "Unbekannt";
			move1 = raid.getMove1();
			move2 = raid.getMove2();
			end = raid.getEnd();
			Long pokemonIdLong = raid.getPokemonId();
			int pokemonIntValue = pokemonIdLong == null ? -1 : pokemonIdLong.intValue();
			String pokemonId = Integer.valueOf(pokemonIntValue).toString();
			if (pokemonIntValue == -1) {

				try {
					JSONObject eggTemplateFromFile = getTemplateFromFile(MessageConfigElement.CONFIG_ELEMENT_EGG);
					String templateText = eggTemplateFromFile.getString("text");
					String templateDescription = eggTemplateFromFile.getString("description");
					logger.debug("text-template would be:\n " + templateText);
					logger.debug("description-template would be:\n " + templateDescription);
					if (templateText != null) {

						String message = generateRaidMessageFromTemplate(templateText, templateDescription, fullGym,
								end, level, latitude, longitude);
						// generateMessageFromTemplate(templateText,
						// templateDescription, pokemon, pokemonId,
						// pokemonName, formattedTime, weatherBoosted, form, gender, genderEmoji,
						// costume, ivString,
						// ivAttack, ivDefense, ivStamina, googleLink, appleLink);
						logger.info("Generated message from template: " + message);
						if (message != null && !message.trim().isEmpty()) {
							logger.debug("return generated message");
							return message;
						}

					}
				} catch (IOException e) {
					logger.error("Message generation of egg message failed with IO-error", e);
				}

				return createEggMessageText(fullGym, end, level, latitude, longitude);
			}
			pokemonName = getPokemonName(pokemonId);
		}
		pokemonText = createRaidMessageText(pokemonName, end, level, move1, move2, latitude, longitude, url, address,
				gymName);
		try {
			JSONObject raidTemplateFromFile = getTemplateFromFile(MessageConfigElement.CONFIG_ELEMENT_RAID);
			String templateText = raidTemplateFromFile.getString("text");
			String templateDescription = raidTemplateFromFile.getString("description");
			logger.debug("text-template would be:\n " + templateText);
			logger.debug("description-template would be:\n " + templateDescription);
			if (templateText != null) {

				// TODO: IMPLEMENT!

				// String message = null;
				String message = generateRaidMessageFromTemplate(templateText, templateDescription, fullGym, end, level,
						latitude, longitude);

				// generateMessageFromTemplate(templateText,
				// templateDescription, pokemon, pokemonId,
				// pokemonName, formattedTime, weatherBoosted, form, gender, genderEmoji,
				// costume, ivString,
				// ivAttack, ivDefense, ivStamina, googleLink, appleLink);
				logger.debug("Generated message from template: " + message);
				if (message != null && !message.trim().isEmpty()) {
					logger.info("return generated message with " + MessageConfigElement.CONFIG_ELEMENT_RAID.name() + " - template");
					return message;
				}

			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return pokemonText;
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
					String eventHeadline = singleTimeSlot.getTime() + " Uhr: " + subscribersSize + " Zusage"
							+ optionalPluralN + MESSAGE_NEWLINE;
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
								System.out.println("Error adding user " + username);
								// TODO: Log error!
							}
						} else {
							System.out.println("Error adding subscriber " + subscriber.toString());
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