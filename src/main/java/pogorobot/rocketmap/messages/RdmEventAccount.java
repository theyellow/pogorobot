package pogorobot.rocketmap.messages;

import pogorobot.events.webservice.WebserviceAccount;

public class RdmEventAccount extends IncomingEvent<WebserviceAccount> {

	private WebserviceAccount message;

	@Override
	public WebserviceAccount getMessage() {
		// TODO implement account
		return message;
	}

	@Override
	public void setMessage(WebserviceAccount message) {
		this.message = message;
	}

}
