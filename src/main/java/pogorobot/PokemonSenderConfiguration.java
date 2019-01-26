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

package pogorobot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

import pogorobot.telegram.config.StandardConfiguration;

@Configuration

@PropertySources({ @PropertySource("${ext.properties.dir:classpath:}/botconfig.properties") })
public class PokemonSenderConfiguration {

	@Value("${standard.debug}")
	private Boolean debug;

	@Value("${standard.usewebhook}")
	private Boolean useWebHook;

	@Value("${standard.port}")
	private int port;

	@Value("${standard.raidtime}")
	private int raidtime;

	@Value("${standard.externalwebhookurl}")
	private String externalwebhookurl;

	@Value("${standard.internalwebhookurl}")
	private String internalwebhookurl;

	@Value("${standard.pathToCertificatePublicKey}")
	private String pathToCertificatePublicKey;

	@Value("${standard.pathToCertificateStore}")
	private String pathToCertificateStore;

	@Value("${standard.certificateStorePassword}")
	private String certificateStorePassword;

	@Value("${standard.pathToLogs}")
	private String pathToLogs;

	@Value("${standard.jdbcurl}")
	private String jdbcUrl;

	@Value("${standard.controllerdbclass}")
	private String controllerDB;

	@Value("${standard.user}")
	private String user;

	@Value("${standard.password}")
	private String password;

	@Value("${standard.gmapskey}")
	private String gmapskey;

	@Value("${standard.botname}")
	private String botname;

	@Value("${standard.bottoken}")
	private String bottoken;

	@Value("${standard.generateDdl}")
	private boolean generateDdl;

	@Bean
	public StandardConfiguration getStandardConfiguration() {
		StandardConfiguration vars = new StandardConfiguration(debug, useWebHook, port, externalwebhookurl, internalwebhookurl,
				pathToCertificatePublicKey, pathToCertificateStore, certificateStorePassword, pathToLogs, jdbcUrl,
				controllerDB, user, password, generateDdl, gmapskey, botname, bottoken, raidtime);
		return vars;
	}
}
