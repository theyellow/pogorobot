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

package pogorobot.telegram.util;

import java.io.Serializable;

import org.telegram.telegrambots.meta.api.objects.Message;

public class SendMessageAnswer {

	Message stickerAnswer;

	Message mainMessageAnswer;

	Message locationAnswer;

	Serializable eventAnswer;

	public final Serializable getEventAnswer() {
		return eventAnswer;
	}

	public final void setEventAnswer(Serializable eventAnswer) {
		this.eventAnswer = eventAnswer;
	}

	public final Message getStickerAnswer() {
		return stickerAnswer;
	}

	public final void setStickerAnswer(Message stickerAnswer) {
		this.stickerAnswer = stickerAnswer;
	}

	public final Message getMainMessageAnswer() {
		return mainMessageAnswer;
	}

	public final void setMainMessageAnswer(Message mainMessageAnswer) {
		this.mainMessageAnswer = mainMessageAnswer;
	}

	public final Message getLocationAnswer() {
		return locationAnswer;
	}

	public final void setLocationAnswer(Message locationAnswer) {
		this.locationAnswer = locationAnswer;
	}

}
