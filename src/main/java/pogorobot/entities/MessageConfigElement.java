package pogorobot.entities;

public enum MessageConfigElement {

	// @formatter:off
	CONFIG_ELEMENT_MONSTER("monster", null), 
	CONFIG_ELEMENT_MONSTER_NOIV("monsterNoIv", null), 
	CONFIG_ELEMENT_RAID("raid", null), 
	CONFIG_ELEMENT_EGG("egg", null), 
	CONFIG_ELEMENT_QUEST_SIMPLE("quest","simple"), 
	CONFIG_ELEMENT_QUEST_SHORT("quest", "short"), 
	CONFIG_ELEMENT_QUEST_GOOGLE_MONSTER("quest", "pokestop_google_monster"), 
	CONFIG_ELEMENT_QUEST_APPLE_MONSTER("quest", "pokestop_apple_monster"), 
	CONFIG_ELEMENT_QUEST_CANDY("quest", "pokestop_candy");
	// @formatter:on

	String key;
	String subkey;

	MessageConfigElement(String key, String subkey) {
		this.key = key;
		this.subkey = subkey;
	}

	public String getPath() {
		String possibleSubelement = subkey != null && !subkey.trim().isEmpty() ? "." + subkey : "";
		return key + possibleSubelement;
	}
}
