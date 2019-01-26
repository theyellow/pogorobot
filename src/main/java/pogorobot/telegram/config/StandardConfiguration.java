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

package pogorobot.telegram.config;

import org.springframework.beans.factory.annotation.Qualifier;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * @brief Custom build vars FILL EVERYTHING CORRECTLY
 * @date 20 of June of 2015
 */
@Qualifier("standard")
public class StandardConfiguration extends Configuration {

	public StandardConfiguration(Boolean debug, Boolean useWebHook, int port, String externalWebhookUrl, String internalWebhookUrl,
			String pathToCertificatePublicKey, String pathToCertificateStore, String certificateStorePassword,
			String pathToLogs, String linkDB, String controllerDB, String user, String password,
			boolean generateDdl, String gmapsKey, String botname, String bottoken, int raidtime) {
		super(debug, useWebHook, port, externalWebhookUrl, internalWebhookUrl, pathToCertificatePublicKey,
				pathToCertificateStore, certificateStorePassword, pathToLogs, linkDB, controllerDB, user, password,
				generateDdl, gmapsKey, botname, bottoken, raidtime);
	}

}
