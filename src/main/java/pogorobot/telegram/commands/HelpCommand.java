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
import org.telegram.telegrambots.extensions.bots.commandbot.commands.IBotCommand;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import pogorobot.service.TelegramKeyboardService;
import pogorobot.service.UserService;
import pogorobot.telegram.PogoBot;

/**
 * This command helps the user to find the command they need
 *
 * @author Timo Schulz (Mit0x2)
 */
@Component
public class HelpCommand extends BotCommand {

	Logger logger = LoggerFactory.getLogger(this.getClass());

	// private ICommandRegistry pogoBot;

	@Autowired
	private UserService userService;

	@Autowired
	private TelegramKeyboardService telegramKeyboardService;

	// @Autowired
	private PogoBot pogoBot;

	public HelpCommand(PogoBot pogoBot) {
		super("help", "Zeig alle Befehle des Bots an");
		this.pogoBot = pogoBot;
	}
	// public HelpCommand(ICommandRegistry pogoBot, UserService userService,
	// TelegramKeyboardService keyboardService) {
	// super("help", "Zeig alle Befehle des Bots an");
	// this.pogoBot = pogoBot;
	// this.userService = userService;
	// this.telegramKeyboardService = keyboardService;
	// }

	@Override
	public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
		pogorobot.entities.User user2 = userService.getOrCreateUser(user.getId().toString());
		if (!user2.isTelegramActive()) {
			return;
		}

		StringBuilder helpMessageBuilder = new StringBuilder("<b>Help</b>\n");
		helpMessageBuilder.append("Das sind die Befehle für diesen Bot:\n\n");

		for (IBotCommand botCommand : pogoBot.getRegisteredCommands()) {
			helpMessageBuilder.append(botCommand.toString()).append("\n\n");
		}

		SendMessage helpMessage = new SendMessage();
		helpMessage.setChatId(chat.getId().toString());
		helpMessage.enableHtml(true);
		helpMessage.setText(helpMessageBuilder.toString());
		boolean isRaidAdmin = user2.isRaidadmin();
		helpMessage.setReplyMarkup(telegramKeyboardService.getSettingsKeyboard(isRaidAdmin));

		try {
			absSender.execute(helpMessage);
		} catch (TelegramApiException e) {
			logger.error(e.getMessage(), e);
		}
	}
}
