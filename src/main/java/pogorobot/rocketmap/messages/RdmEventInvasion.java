package pogorobot.rocketmap.messages;

import pogorobot.events.rocketmap.RdmInvasion;

public class RdmEventInvasion extends IncomingEvent<RdmInvasion> {

	private RdmInvasion message;

	@Override
	public RdmInvasion getMessage() {
		// TODO Auto-generated method stub
		return message;
	}

	@Override
	public void setMessage(RdmInvasion message) {
		this.message = message;

	}

}
