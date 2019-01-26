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
 * This command simply replies with a hello to the users command and
 * sends them the 'kind' words back, which they send via command parameters
 *
 * @author Timo Schulz (Mit0x2)
 */
@Component
public class HelloCommand extends BotCommand {

	Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private UserService userService;
    
	@Autowired
	private TelegramKeyboardService telegramKeyboardService;

    public HelloCommand() {
		super("hello", "Sag Hallo zu dem Bot");
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] arguments) {

		pogorobot.entities.User user2 = userService.getOrCreateUser(user.getId().toString());
		if (!user2.isTelegramActive()) {
            return;
        }

        String userName = chat.getUserName();
        if (userName == null || userName.isEmpty()) {
            userName = user.getFirstName() + " " + user.getLastName();
        }

		StringBuilder messageTextBuilder = new StringBuilder("Hallo ").append(userName);
        if (arguments != null && arguments.length > 0) {
            messageTextBuilder.append("\n");
			messageTextBuilder.append("Danke für deine netten Worte \n");
            messageTextBuilder.append(String.join(" ", arguments));
			messageTextBuilder.append("\nEs hat mich sehr gefreut. " + Emoji.SMILING_FACE_WITH_SMILING_EYES);
        }

        SendMessage answer = new SendMessage();
        answer.setChatId(chat.getId().toString());
        answer.setText(messageTextBuilder.toString());
		boolean isRaidAdmin = user2.isRaidadmin();
		answer.setReplyMarkup(telegramKeyboardService.getSettingsKeyboard(isRaidAdmin));

        try {
            absSender.execute(answer);
        } catch (TelegramApiException e) {
			logger.error(e.getMessage(), e);
        }
    }
}