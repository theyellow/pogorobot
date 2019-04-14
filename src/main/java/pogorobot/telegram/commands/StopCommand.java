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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import pogorobot.service.UserService;

/**
 * This commands stops the conversation with the bot. Bot won't respond to user
 * until he sends a start command
 *
 * @author Timo Schulz (Mit0x2)
 */
@Component
public class StopCommand extends BotCommand {

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private UserService userService;

	/**
	 * Construct
	 */
	public StopCommand() {
		super("stop",
				"Hiermit können Pokémon-Meldungen deaktiviert werden und der Bot kann gestoppt werden.\nBenutze <code>/stop all</code> um den Bot komplett zu stoppen, Benutze <code>/stop pokemon</code> oder <code>/stop</code> um Pokémon zu deaktivieren.");
	}

	@Override
	public void execute(AbsSender absSender, User user, Chat chat, String[] arguments) {
		pogorobot.entities.User realUser = userService.getOrCreateUser(user.getId().toString());
		String userName = user.getFirstName() + " " + user.getLastName();
		SendMessage answer = new SendMessage();
		String text = "Du hast */stop* eingegeben.";
		if (realUser.isTelegramActive() && arguments.length > 0 && arguments[0].equals("all")) {
			realUser.setTelegramActive(false);
			realUser.setShowPokemonMessages(false);
			realUser.setShowRaidMessages(false);
			realUser = userService.updateOrInsertUser(realUser);
			ReplyKeyboard replyMarkup = new ReplyKeyboardRemove();
			answer.setReplyMarkup(replyMarkup);
			text = "Tschüssi " + userName + "\n" + "Ich hoffe du komst bald wieder!";
		} else if (realUser.isTelegramActive() && arguments.length > 0 && arguments[0].equals("pokemon")) {
			realUser.setShowPokemonMessages(false);
			realUser = userService.updateOrInsertUser(realUser);
			text = "Es werden im Moment keine Pokémon mehr angezeigt.";
		} else {
			realUser.setShowPokemonMessages(false);
			realUser = userService.updateOrInsertUser(realUser);
			text = "Es werden im Moment keine Pokémon mehr angezeigt. Zum Beendes aller Nachrichten gib folgendes ein:\n/stopall";
		}
		answer.setChatId(chat.getId().toString());
		answer.enableMarkdown(true);
		answer.setText(text);

		try {
			absSender.execute(answer);
		} catch (TelegramApiException e) {
			logger.error(e.getMessage(), e);
		}
	}
}
