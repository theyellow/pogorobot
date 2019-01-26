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

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.SortedSet;
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

import pogorobot.entities.EventWithSubscribers;
import pogorobot.entities.Filter;
import pogorobot.entities.Gym;
import pogorobot.entities.PokemonWithSpawnpoint;
import pogorobot.entities.Raid;
import pogorobot.entities.Subscriber;
import pogorobot.entities.User;
import pogorobot.telegram.util.Emoji;

@Service("telegramTextService")
public class TelegramTextServiceImpl implements TelegramTextService {

	Logger logger = LoggerFactory.getLogger(this.getClass().getInterfaces()[0]);



	private static final String MESSAGE_SPACE = " ";

	private static final String MESSAGE_NEWLINE = "\n";

	private static final String MESSAGE_END_A_HREF = "</a>";

	private static final String MESSAGE_END_OF_A_TAG_WITH_COMMENT = "\">";

	private static final String MESSAGE_BEGIN_A_HREF = "<a href=\"";

	private static final String MESSAGE_SLASH = "/";

	private static final String MESSAGE_BOLD_ON = "<b>";

	private static final String MESSAGE_BOLD_OFF = "</b>";

	private static final String LOGTAG = "TELEGRAM TEXT SERVICE";

	private String serialString = "68747470733a2f2f6d6f6e73746572696d616765732e746b2f76312e342f74656c656772616d";

	private JSONObject jsonMoves;

	private JSONObject jsonPokemons;

	@Autowired
	private UserService userService;

	private JSONObject jsonBaseStats;

	private JSONObject jsonForms;

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
		} catch (JSONException ex){
			logger.error("Form \"" + form + "\" not found for pokemon: " + jsonPokemons.getJSONObject(pokemonId)
					+ " -> send message to developer to update internal configuration.");
		}
		if (pokemonForms != null && pokemonForms.getString(form) != null) {
			result = "(" + pokemonForms.getString(form) + ")";

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
		return new String(Hex.decodeHex(serialString.toCharArray()));
	}

	private String getThreeDigitFormattedPokemonId(int pokemonInt) {
		return pokemonInt >= 100 ? pokemonInt + "" : (pokemonInt >= 10 ? "0" + pokemonInt : "00" + pokemonInt);
	}

	@Override
	public String getStickerMonUrl(int pokemonInt) throws DecoderException {
		String decUrl = createDec();
		String pokemonId = getThreeDigitFormattedPokemonId(pokemonInt);
		String url = decUrl + "/mon" + "sters/" + pokemonId + "_00" + "0.we" + "bp";
		return url;
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
			PokemonWithSpawnpoint pokemon) {
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
		stringBuilder.append(pokemonName);
		if (gender != null) {
			Emoji genderEmoji = getGenderEmoji(pokemonId, gender.intValue());
			if (!Emoji.NONE.equals(genderEmoji)) {
				stringBuilder.append(MESSAGE_SPACE);
				stringBuilder.append(genderEmoji);
			}
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
			String ivString = Double.toString(ivs);
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
		stringBuilder.append(getGoogleLink(latitude, longitude));
		return stringBuilder.toString();
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
		return new SimpleDateFormat("dd.MM.", Locale.GERMAN).format(new Date(1000 * l));
	}

	@Override
	public String formatTimeFromSeconds(long l) {
		return new SimpleDateFormat("HH:mm", Locale.GERMAN).format(new Date(1000 * l));
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
		return MESSAGE_BEGIN_A_HREF + url + MESSAGE_END_OF_A_TAG_WITH_COMMENT + name + MESSAGE_END_A_HREF;
	}

	private String getGoogleLink(Double latitude, Double longitude) {
		String gUrl = "https://www.google.com/maps/?q=" + latitude + "," + longitude;
		return getLink(gUrl, gUrl);
	}

	@Override
	public String getGymsAroundMessageText(List<Gym> gymsAround) {
		List<String> gymNames = new ArrayList<>();
		StringBuilder stringBuilder = new StringBuilder();
		if (gymsAround.size() == 0) {
			stringBuilder.append(
					"Du hast vermutlich keinen Standort übertragen, daher kann dir keine Arena zur Auswahl angezeigt werden.\nÜbertrage einfach (d)einen Standort und führe die Aktion erneut durch. Falls dann immer noch nichts angezeigt wird wende dich an deinen Admin.");
		} else {
			stringBuilder.append(
					"<b>Hier kannst du Raids erstellen. Wähle unten eine Arena aus. Zuerst aber eine Liste der zur Auswahl stehenden Arenen im Umkreis von 1,5 km (jeweils mit Link):</b>");

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
			text = "Wann beginnt der Raid? Nun werden die Minuten ausgewählt. (" + hour + ":" + "<b>??</b>)";
		} else if (eggOrRaid.equals(TelegramKeyboardService.RAID)) {
			text = "Wann beginnt/begann der Raid? Nun werden die Minuten ausgewählt. (" + hour + ":" + "<b>??</b>)";
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
				return createEggMessageText(fullGym, end, level, latitude, longitude);
			}
			pokemonName = getPokemonName(pokemonId);
		}
		pokemonText = createRaidMessageText(pokemonName, end, level, move1, move2, latitude, longitude, url, address,
				gymName);
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