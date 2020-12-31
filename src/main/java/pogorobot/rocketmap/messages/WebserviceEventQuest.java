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

package pogorobot.rocketmap.messages;

import pogorobot.events.webservice.WebserviceQuest;

public class WebserviceEventQuest extends IncomingEvent<WebserviceQuest> {


	private WebserviceQuest message;
	
	@Override
	public final WebserviceQuest getMessage() {
		return message;
	}

	@Override
	public final void setMessage(WebserviceQuest message) {
		this.message = message;
	}
	
}
