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

import org.telegram.telegrambots.api.methods.BotApiMethod;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.api.objects.CallbackQuery;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.User;

import pogorobot.events.telegrambot.IncomingManualRaid;
import pogorobot.telegram.util.Type;

public interface TelegramMessageCreatorService {

	// SendMessage getLiveLocationDialog(String telegramId, String[] data);

	SendMessage answerUserMessage(Message message, User from);

	EditMessageText getPokemonRemoveDialog(CallbackQuery callbackquery, String[] data, boolean raids);

	EditMessageText getLocationSettingsAreaDialog(CallbackQuery callbackquery, String[] data, Type pokemonFilter);

	EditMessageText getPokemonAddDialog(CallbackQuery callbackquery, String[] data, boolean raids);

	EditMessageText getEnabledRaidsMainDialog(CallbackQuery callbackquery);

	EditMessageText getRaidDisabledDialog(CallbackQuery callbackquery);

	EditMessageText getDistanceSelectDialog(CallbackQuery callbackquery, String[] data);

	EditMessageText getRaidLevelDialog(CallbackQuery callbackquery, String[] data);

	BotApiMethod<? extends Serializable> getIvSettingDialog(CallbackQuery callbackquery, String[] data);

	EditMessageText getRaidPokemonDialog(CallbackQuery callbackquery);

	EditMessageText getChooseGymOfRaidChoiceDialog(CallbackQuery callbackQuery, String[] data);

	EditMessageText getChooseRaidOrEggDialog(CallbackQuery callbackQuery, String[] data);

	EditMessageText getChooseRaidLevelOrPokemonDialog(CallbackQuery callbackquery, String[] data);

	EditMessageText getHoursForNewRaidDialog(CallbackQuery callbackquery, String[] data);

	EditMessageText getMinutesForNewRaidDialog(CallbackQuery callbackquery, String[] data);

	EditMessageText getExactTimeForNewRaidDialog(CallbackQuery callbackquery, String[] data);

	EditMessageText getDateForNewRaidDialog(CallbackQuery callbackquery, String[] data);

	EditMessageText getConfirmNewRaidDialog(CallbackQuery callbackquery, String[] data);

	IncomingManualRaid getManualRaid(CallbackQuery callbackQuery, String[] data);

	EditMessageText getChooseShareRaidDialog(CallbackQuery callbackQuery, String[] data, IncomingManualRaid manualRaid);

	EditMessageText getSignupRaidDialog(CallbackQuery callbackquery, String[] data);

}
