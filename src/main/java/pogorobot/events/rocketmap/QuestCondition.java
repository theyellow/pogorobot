package pogorobot.events.rocketmap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type")
// @JsonSubTypes({ @JsonSubTypes.Type(value = QuestCondition.class, name =
// "info"),
// @JsonSubTypes.Type(value = QuestReward.class, name = "reward") })
public class QuestCondition {

	private String type;

	private String info;

	public final String getInfo() {
		return info;
	}

	public final void setInfo(String info) {
		this.info = info;
	}

	public final String getType() {
		return type;
	}

	public final void setType(String type) {
		this.type = type;
	}

	// ": [ {"type":7,"info":{"raid_levels":[1,2,3,4,5]} },{"type":6}],
}
