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

public class Configuration {

	private Boolean debug;
	private Boolean useWebHook;
	private int port;

	private int raidtime;

	private String externalWebhookUrl; // https://(xyz.)externaldomain.tld
	private String internalWebhookUrl; // https://(xyz.)localip/domain(.tld)
	private String pathToCertificatePublicKey; // only
												// for
												// self-signed
												// webhooks
	private String pathToCertificateStore; // self-signed
											// and
											// non-self-signed.
	private String certificateStorePassword; // password for
												// your
												// certificate-store

	//
	// private String DirectionsApiKey = "<your-api-key>";
	//

	private String hibernateDialect;

	private String pathToLogs;

	private String gmapsKey;

	private String linkDB;
	private String controllerDB;
	private String userDB;
	private String password;
	private String botname;
	private String bottoken;
	private boolean generateDdl;
	private boolean alternativeStickers;
	private boolean showStickers;
	private boolean showLocation;
	private boolean enableWebPagePreview;
	private boolean showRaidStickers;
	private boolean showRaidLocation;
	private boolean enableRaidWebPagePreview;

	public Configuration(Boolean debug, Boolean useWebHook, int port, String externalWebhookUrl,
			String internalWebhookUrl, String pathToCertificatePublicKey, String pathToCertificateStore,
			String certificateStorePassword, String pathToLogs, String linkDB, String controllerDB, String user,
			String password, boolean generateDdl, String gmapsKey, String botname, String bottoken, int raidtime,
			String hibernateDialect, boolean alternativeStickers, boolean showStickers, boolean showLocation,
			boolean enableWebPagePreview, boolean showRaidStickers, boolean showRaidLocation,
			boolean enableRaidWebPagePreview) {
		this.debug = debug;
		this.useWebHook = useWebHook;
		this.port = port;
		this.externalWebhookUrl = externalWebhookUrl;
		this.internalWebhookUrl = internalWebhookUrl;
		this.pathToCertificatePublicKey = pathToCertificatePublicKey;
		this.pathToCertificateStore = pathToCertificateStore;
		this.certificateStorePassword = certificateStorePassword;
		this.pathToLogs = pathToLogs;
		this.linkDB = linkDB;
		this.controllerDB = controllerDB;
		this.userDB = user;
		this.password = password;
		this.generateDdl = generateDdl;
		this.gmapsKey = gmapsKey;
		this.botname = botname;
		this.bottoken = bottoken;
		this.raidtime = raidtime;
		this.hibernateDialect = hibernateDialect;
		this.alternativeStickers = alternativeStickers;
		this.showLocation = showLocation;
		this.showStickers = showStickers;
		this.enableWebPagePreview = enableWebPagePreview;
		this.showRaidLocation = showLocation;
		this.showRaidStickers = showRaidStickers;
		this.enableRaidWebPagePreview = enableRaidWebPagePreview;
	}

	public Boolean getDebug() {
		return debug;
	}

	public Boolean getUsewebhook() {
		return useWebHook;
	}

	public int getPort() {
		return port;
	}

	public final int getRaidtime() {
		return raidtime;
	}

	public String getExternalWebhookUrl() {
		return externalWebhookUrl;
	}

	public String getInternalwebhookurl() {
		return internalWebhookUrl;
	}

	public String getPathtocertificatepublickey() {
		return pathToCertificatePublicKey;
	}

	public String getPathtocertificatestore() {
		return pathToCertificateStore;
	}

	public String getCertificatestorepassword() {
		return certificateStorePassword;
	}

	public String getPathtologs() {
		return pathToLogs;
	}

	public String getJdbcUrl() {
		return linkDB;
	}

	public String getControllerdb() {
		return controllerDB;
	}

	public String getUserdb() {
		return userDB;
	}

	public String getPassword() {
		return password;
	}

	public boolean isGenerateDdl() {
		return generateDdl;
	}

	public final String getGmapsKey() {
		return gmapsKey;
	}

	public final String getBotname() {
		return botname;
	}

	public final String getBottoken() {
		return bottoken;
	}

	public String getHibernateDialect() {
		return hibernateDialect;
	}

	public void setHibernateDialect(String hibernateDialect) {
		this.hibernateDialect = hibernateDialect;
	}

	public Boolean getAlternativeStickers() {
		return alternativeStickers;
	}

	public boolean getShowStickers() {
		return showStickers;
	}

	public boolean getShowLocation() {
		return showLocation;
	}

	public boolean getShowRaidStickers() {
		return showRaidStickers;
	}

	public boolean getShowRaidLocation() {
		return showRaidLocation;
	}

	public boolean getEnableRaidWebPagePreview() {
		return enableRaidWebPagePreview;
	}

	public boolean getEnableWebPagePreview() {
		return enableWebPagePreview;
	}
}
