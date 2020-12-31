package pogorobot.rocketmap.messages;

import pogorobot.events.webservice.WebserviceWeatherEventMessage;

public class WebserviceEventWeather extends IncomingEvent<WebserviceWeatherEventMessage> {

	private WebserviceWeatherEventMessage message;

	@Override
	public WebserviceWeatherEventMessage getMessage() {
		// TODO Auto-generated method stub
		return message;
	}

	@Override
	public void setMessage(WebserviceWeatherEventMessage message) {
		this.message = message;

	}

	
}
