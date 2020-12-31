package pogorobot.rocketmap.messages;

import pogorobot.events.webservice.WebserviceInvasionEventMessage;

public class WebserviceEventInvasion extends IncomingEvent<WebserviceInvasionEventMessage> {

	private WebserviceInvasionEventMessage message;

	@Override
	public WebserviceInvasionEventMessage getMessage() {
		// TODO Auto-generated method stub
		return message;
	}

	@Override
	public void setMessage(WebserviceInvasionEventMessage message) {
		this.message = message;

	}

}
