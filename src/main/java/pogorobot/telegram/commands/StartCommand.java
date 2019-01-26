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

package pogorobot.telegram.commands;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Chat;
import org.telegram.telegrambots.api.objects.User;
import org.telegram.telegrambots.bots.AbsSender;
import org.telegram.telegrambots.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import pogorobot.service.TelegramKeyboardService;
import pogorobot.service.UserService;
import pogorobot.telegram.util.Emoji;

/**
 * This commands starts the conversation with the bot
 *
 * @author Benjamin Marstaller
 */
@Component
public class StartCommand extends BotCommand {

	@Autowired
	private UserService userService;
	
	@Autowired
	private TelegramKeyboardService telegramKeyboardService;

	Logger logger = LoggerFactory.getLogger(this.getClass());

    public StartCommand() {
		super("start", "Mit diesem Kommando wird der Bot neu gestartet (vorhandene Einstellungen bleiben erhalten)");
    }

    @Override
    public void execute(AbsSender bot, User user, Chat chat, String[] strings) {
//        DatabaseManager databseManager = DatabaseManager.getInstance();
        StringBuilder messageBuilder = new StringBuilder();

        String firstName = user.getFirstName() ==  null ? "" : user.getFirstName() + " ";
		String lastName = user.getLastName() == null? "" : user.getLastName();
		String userName = firstName + lastName;
		boolean isRaidAdmin = false;
		if (userService != null) {
			pogorobot.entities.User user2 = userService.getOrCreateUser(user.getId().toString());
			if (user2.isTelegramActive()) {
				messageBuilder.append("Hallo ").append(userName).append("\n");
				messageBuilder
						.append("wir kennen uns! \nEs werden dir die ausgewählten Pokémon und Raids (nicht) "
								+ "angezeigt. Und du kannst Raids hinzufügen...");
				System.out.println(Arrays.toString(strings));
				user2.setShowPokemonMessages(true);
			} else {
				user2.setShowPokemonMessages(true);
				user2.setTelegramActive(true);
				user2.setPayed(false);
				messageBuilder.append("Willkommen ").append(userName).append("\n");
				messageBuilder.append(
						"dieser Bot versucht dir immer die richtigen Raids und Pokémon anzuzeigen oder"
								+ " einzugeben! Dafür musst du den Bot zuerst kurz einstellen! "
								+ "\nIn der Leiste ganz unten findest du die entsprechenden Einstellungen.");
				messageBuilder.append("\nAls erstes solltest du deinen Standort einstellen. "
						+ "Unten rechts mit 'Ort' kannst du deinen Standort übertragen.\n"
						+ "Du kannst auch frei einen Standort definieren oder deinen Live-Standort senden,"
						+ " z.B. bei einem iPhone mit dem Büroklammersymbol "
						+ "links von der Texteingabe -> Menüpunkt Standort -> Menüpunkt Meinen Live-Standort teilen -> "
						+ "Dauer auswählen (z.B. für 1 Stunde).\n"
						+ "Es ist auch sinnvoll die Raid-Einstellungen selbst zu machen, nur um selbst definierte Raids"
						+ " im persönlichen Bot angezeigt zu bekommen (z.B. Karpador-Raids, die werden in den Kanälen nicht angezeigt)\n"
						+ "Viel Spaß " + Emoji.SMILING_FACE_WITH_SMILING_EYES
						+ Emoji.SMILING_FACE_WITH_SMILING_EYES + Emoji.SMILING_FACE_WITH_SMILING_EYES);
			}
			isRaidAdmin = true;
			user2.setRaidadmin(isRaidAdmin);
			user2 = userService.updateOrInsertUser(user2);
		} else {
			messageBuilder.append("Willkommen ").append(userName).append("\n");
			messageBuilder.append(
					"dieser Bot ist in einer ersten Betriebs-Phase und evtl. gibt es Probleme beim Autowiring!\n"
							+ "Bitte informiere einen Admin.");
		}

        SendMessage answer = new SendMessage();
		answer.setReplyMarkup(telegramKeyboardService.getSettingsKeyboard(isRaidAdmin));
        String chatId = chat.getId().toString();
        chatId = chat.isGroupChat() ? user.getId().toString() : chatId;
		answer.setChatId(chatId);
        answer.setText(messageBuilder.toString());
        try {
            bot.execute(answer);
        } catch (TelegramApiException e) {
			logger.error(e.getMessage(), e);
        }
    }
}