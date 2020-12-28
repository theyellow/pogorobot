package pogorobot.rocketmap.messages;

import pogorobot.events.rocketmap.RdmAccount;

public class RdmEventAccount extends IncomingEvent<RdmAccount> {

	private RdmAccount message;

	@Override
	public RdmAccount getMessage() {
		// TODO implement account
		return message;
	}

	@Override
	public void setMessage(RdmAccount message) {
		this.message = message;
	}

}
