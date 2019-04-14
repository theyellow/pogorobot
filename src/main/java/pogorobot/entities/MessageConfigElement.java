package pogorobot.entities;

public enum MessageConfigElement {


	// @formatter:off
	CONFIG_ELEMENT_MONSTER(Type.MONSTER, null), 
	CONFIG_ELEMENT_MONSTER_NOIV(Type.MONSTER_NO_IV, null), 
	CONFIG_ELEMENT_RAID(Type.RAID, null), 
	CONFIG_ELEMENT_EGG(Type.EGG, null), 
	CONFIG_ELEMENT_QUEST_SIMPLE(Type.QUEST,"simple"), 
	CONFIG_ELEMENT_QUEST_SHORT(Type.QUEST, "short"), 
	CONFIG_ELEMENT_QUEST_GOOGLE_MONSTER(Type.QUEST, "pokestop_google_monster"), 
	CONFIG_ELEMENT_QUEST_APPLE_MONSTER(Type.QUEST, "pokestop_apple_monster"), 
	CONFIG_ELEMENT_QUEST_CANDY(Type.QUEST, "pokestop_candy");
	// @formatter:on

	private String key;
	private String subkey;

	MessageConfigElement(Type type, String subkey) {
		this.key = type.getType();
		this.subkey = subkey;
	}

	public String getPath() {
		String possibleSubelement = subkey != null && !subkey.trim().isEmpty() ? "." + subkey : "";
		return key + possibleSubelement;
	}

	private enum Type {

		// @formatter:off
		MONSTER("monster"),
		MONSTER_NO_IV("monsterNoIv"),
		RAID("raid"),
		EGG("egg"),
		QUEST("quest");
		// @formatter:on

		private String type;

		Type(String type) {
			this.type = type;
		}

		public String getType() {
			return type;
		}
	}
}
