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

	@Value("${standard.debug:false}")
	private Boolean debug;

	@Value("${standard.usewebhook:false}")
	private Boolean useWebHook;

	@Value("${standard.port:-1}")
	private int port;

	@Value("${standard.raidtime}")
	private int raidtime;

	@Value("${standard.externalwebhookurl:someurl}")
	private String externalwebhookurl;

	@Value("${standard.internalwebhookurl:anotherurl}")
	private String internalwebhookurl;

	@Value("${standard.pathToCertificatePublicKey:path}")
	private String pathToCertificatePublicKey;

	@Value("${standard.pathToCertificateStore:path}")
	private String pathToCertificateStore;

	@Value("${standard.certificateStorePassword:path}")
	private String certificateStorePassword;

	@Value("${standard.pathToLogs:./logs}")
	private String pathToLogs;

	@Value("${standard.jdbcurl}")
	private String jdbcUrl;

	@Value("${standard.controllerdbclass}")
	private String controllerDB;

	@Value("${standard.user}")
	private String user;

	@Value("${standard.password}")
	private String password;

	@Value("${standard.gmapskey:notnecessary}")
	private String gmapskey;

	@Value("${standard.botname}")
	private String botname;

	@Value("${standard.bottoken}")
	private String bottoken;

	@Value("${standard.hibernate-dialect}")
	private String hibernateDialect;

	@Value("${standard.generateDdl}")
	private boolean generateDdl;

	@Value("${standard.alternativeStickers}")
	private boolean alternativeStickers;

	@Value("${standard.showStickers}")
	private boolean showStickers;

	@Value("${standard.showLocation}")
	private boolean showLocation;

	@Value("${standard.enableWebPagePreview}")
	private boolean enableWebPagePreview;

	@Value("${standard.showRaidStickers}")
	private boolean showRaidStickers;

	@Value("${standard.showRaidLocation}")
	private boolean showRaidLocation;

	@Value("${standard.enableRaidWebPagePreview}")
	private boolean enableRaidWebPagePreview;

	@Bean
	public StandardConfiguration getStandardConfiguration() {
		StandardConfiguration vars = new StandardConfiguration(debug, useWebHook, port, externalwebhookurl, internalwebhookurl,
				pathToCertificatePublicKey, pathToCertificateStore, certificateStorePassword, pathToLogs, jdbcUrl,
				controllerDB, user, password, generateDdl, gmapskey, botname, bottoken, raidtime, hibernateDialect,
				alternativeStickers, showStickers, showLocation, enableWebPagePreview, showRaidStickers,
				showRaidLocation, enableRaidWebPagePreview);
		return vars;
	}
}
