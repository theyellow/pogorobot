package pogorobot.telegram.summary;

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import pogorobot.entities.EventWithSubscribers;
import pogorobot.entities.RaidAtGymEvent;
import pogorobot.entities.SendMessages;
import pogorobot.entities.Subscriber;
import pogorobot.entities.UserGroup;
import pogorobot.service.TelegramTextService;
import pogorobot.service.db.UserGroupService;
import pogorobot.service.db.repositories.GymRepository;
import pogorobot.service.db.repositories.SendMessagesRepository;
import pogorobot.service.db.repositories.UserGroupRepository;
import pogorobot.telegram.PogoBot;

@Service
public class RaidSummaryMessageSender {

	private static final int PEOPLE_EMOJI = 0x1F465;

	private static final String SPACE = " ";

	@Autowired
	private UserGroupRepository userGroupRepository;

	@Autowired
	private TelegramTextService telegramTextService;

	@Autowired
	private GymRepository gymRepository;

	@Autowired
	private SendMessagesRepository sendMessagesRepository;

	@Autowired
	private PogoBot pogoBot;

	@Autowired
	private UserGroupService userGroupService;

	private static MutableInteger chatsUpdated;

	private static Logger logger = LoggerFactory.getLogger(RaidSummaryMessageSender.class);

	private static Map<Long, String> raidSummaries;

	public Map<Long, Integer> sendRaidSummaries() {
		if (chatsUpdated.get() == 0) {
			pogoBot.updateUserGroups();
			chatsUpdated.set(1);
			;
		}
		Map<Long, Integer> result = new HashMap<>();
		Map<Long, String> createRaidSummariesFromDatabase = createRaidSummariesFromDatabase();
		for (Entry<Long, String> newRaidSummary : createRaidSummariesFromDatabase.entrySet()) {
			String newRaidText = newRaidSummary.getValue();
			Long chatId = newRaidSummary.getKey();
			String oldRaidText = raidSummaries.get(chatId);
			if (!newRaidText.equals(oldRaidText)) {
				Integer raidSummaryMessageId = sendRaidSummary(chatId, newRaidText);
				raidSummaries.put(chatId, newRaidText);
				result.put(chatId, raidSummaryMessageId);
			}
		}
		return result;
	}

	public RaidSummaryMessageSender() {
		raidSummaries = new HashMap<Long, String>();
		chatsUpdated = new MutableInteger(0);
	}

	private Integer sendRaidSummary(Long chatId, String text) {
		UserGroup userGroup = userGroupRepository.findByChatId(chatId);
		Integer raidSummaryMessageId = userGroup.getRaidSummaryLink();
		if (null == raidSummaryMessageId) {
			SendMessage message = SendMessage.builder().chatId(String.valueOf(chatId)).disableNotification(true)
					.parseMode("MarkdownV2").disableWebPagePreview(true).text(text).build();
			try {
				Message messageResult = pogoBot.execute(message);
				raidSummaryMessageId = messageResult.getMessageId();
			} catch (TelegramApiException e) {
				logger.error("New raid summary for {} gave error {}", chatId, e.getMessage());
			}
			if (raidSummaryMessageId != null) {
				logger.debug("Updated raid-summary with id {} on chat {}", raidSummaryMessageId, chatId);
			}
		} else {
			DeleteMessage deleteMessage = DeleteMessage.builder().chatId(String.valueOf(chatId))
					.messageId(raidSummaryMessageId).build();
			SendMessage message = SendMessage.builder().chatId(String.valueOf(chatId)).disableNotification(true)
					.parseMode("MarkdownV2").disableWebPagePreview(true).text(text).build();
			Boolean executed = null;
			try {
				executed = pogoBot.execute(deleteMessage);
			} catch (TelegramApiException e) {
				logger.error("Delete raid summary for {} gave error {}", chatId, e.getMessage());
				logger.warn("Old message (if existing) was not deleted.");
			}
			try {
				Message messageResult = pogoBot.execute(message);
				raidSummaryMessageId = messageResult.getMessageId();
			} catch (TelegramApiException e) {
				logger.error("Sending new (updated) raid summary for {} gave error {}", chatId, e.getMessage());
				logger.warn("Old message (if existing) was {}deleted.", executed != null && executed ? "" : "not ");
			}
			logger.debug("Updated raid-summary with id {} on chat {}", raidSummaryMessageId, chatId);
		}
		return raidSummaryMessageId;
	}

	static Function<? super EventWithSubscribers, Integer> eventSizeMapper = x -> x.getSubscribers().size();

	static Function<? super EventWithSubscribers, String> eventTimeMapper = x -> {
		if (x.getSubscribers().size() > 0) {
			return x.getTime() + SPACE + x.getSubscribers().size();
		} else {
			return "";
		}
	};

	private Map<Long, String> createRaidSummariesFromDatabase() {
		Map<Long, String> result = new HashMap<>();
		Map<Long, Map<RaidAtGymEvent, SendMessages>> fullRaids = getRaidsFromDatabaseByUserGroup();
		int size = 0;
		if (fullRaids != null) {
			for (Entry<Long, Map<RaidAtGymEvent, SendMessages>> chatWithRaids : fullRaids.entrySet()) {
				Long chatId = chatWithRaids.getKey();
				if (chatId != null) {
					Map<RaidAtGymEvent, SendMessages> raidsOfChat = chatWithRaids.getValue();
					if (raidsOfChat != null) {
						Set<Entry<RaidAtGymEvent, SendMessages>> raidsOfChatSet = raidsOfChat.entrySet();
						for (Entry<RaidAtGymEvent, SendMessages> entry : raidsOfChatSet) {
							RaidAtGymEvent gymEvent = entry.getKey();
							SortedSet<EventWithSubscribers> eventsWithSubscribers = gymEvent.getEventsWithSubscribers();
							for (EventWithSubscribers event : eventsWithSubscribers) {
								Set<Subscriber> subscribers = event.getSubscribers();
								if (subscribers != null) {
									size += subscribers.size();
								}

							}
						}
						result.put(chatId, toText(getTelegramLink(chatId), raidsOfChat));
					}
					logger.debug("chat {} has {} participating raiders", chatId, size);
					size = 0;
				}
			}
		}
		return result;
	}

	private String getTelegramLink(Long chatId) {
		UserGroup userGroup = userGroupRepository.findByChatId(chatId);
		return userGroup.getLinkOfGroup();
	}

	private Map<Long, Map<RaidAtGymEvent, SendMessages>> getRaidsFromDatabaseByUserGroup() {
		Map<Long, Map<RaidAtGymEvent, SendMessages>> result = new HashMap<>();
		Map<RaidAtGymEvent, SendMessages> chatResult = new HashMap<>();
		Iterable<UserGroup> allGroups = userGroupRepository.findAll();
		if (allGroups != null) {
			allGroups.forEach(userGroup -> {
				Long chatId = userGroup.getChatId();
				List<RaidAtGymEvent> raids = sendMessagesRepository.findRaidAtGymEventsByChat(chatId);
				for (RaidAtGymEvent raid : raids) {
					List<Subscriber> size = raid.getEventsWithSubscribers().stream()
							.flatMap(x -> x.getSubscribers().stream()).collect(Collectors.toList());
					logger.debug("Got {} subscribers for chat {} on raid {}", size.size(), chatId, raid.getId());
					List<SendMessages> processedRaid = sendMessagesRepository.findSendMessagesByChatAndGym(chatId,
							raid.getGymId());
					if (processedRaid != null && !processedRaid.isEmpty()) {
						chatResult.put(raid, processedRaid.get(0));
					}
				}
				result.put(chatId, chatResult);
			});
		}
		return result;
	}

	private static Comparator<? super RaidAtGymEvent> raidStartComparator = new Comparator<RaidAtGymEvent>() {

		@Override
		public int compare(RaidAtGymEvent o1, RaidAtGymEvent o2) {
			return o1.getStart().compareTo(o2.getStart());
		}

	};

	private String toText(String telegramLink, Map<RaidAtGymEvent, SendMessages> events) {
		StringBuilder sb = new StringBuilder();
		sb.append("*Raids*");
		sb.append("\n");
		sb.append("*-------------*");
		sb.append("\n");
		events.entrySet().stream().map(x -> x.getKey()).sorted(raidStartComparator).forEach(raidEvent -> {
			String beginning = telegramTextService.formatTimeFromSeconds(raidEvent.getStart());
			String ending = telegramTextService.formatTimeFromSeconds(raidEvent.getEnd());
			String gymName = gymRepository.findNameByGymId(raidEvent.getGymId());
			Long level = raidEvent.getLevel();
			Long pokemonId = raidEvent.getPokemonId();
			String pokemonName = level > 0 && pokemonId > 0
					? telegramTextService.getPokemonName(String.valueOf(pokemonId))
					: "Ei";
			pokemonName = +level == 6 || level == 7 ? "(Mega) " : "";

			SortedSet<EventWithSubscribers> eventsWithSubscribers = raidEvent.getEventsWithSubscribers();
			SortedSet<String> participatingGroups = new TreeSet<>();
			for (EventWithSubscribers eventWithSubscribers : eventsWithSubscribers) {
				Set<Subscriber> subscribers = eventWithSubscribers.getSubscribers();
				if (subscribers != null) {
					int size = subscribers.size();
					if (size > 0) {
						StringBuilder builder = new StringBuilder();
						builder.append(eventWithSubscribers.getTime());
						builder.append(SPACE);
						builder.appendCodePoint(PEOPLE_EMOJI);
						builder.append(SPACE);
						builder.append(size);
						participatingGroups.add(builder.toString());
					}
				}
			}

			sb.append(pokemonName);
			sb.append(SPACE);
			sb.append(beginning);
			sb.append("\\-");
			sb.append(ending);
			sb.append(SPACE);
			sb.append("\\-");
			sb.append(SPACE);
			if (!telegramLink.isEmpty()) {
				sb.append("[" + gymName + "](" + telegramLink + "/" + events.get(raidEvent).getMessageId() + ")");
			} else {
				sb.append(gymName);
			}
			sb.append("\n");
			for (String group : participatingGroups) {
				sb.append("└ " + group + "\n");
			}
		});
		if (events.isEmpty()) {
			sb.append("Niemand will raiden.\n");
		}
		return sb.toString();
	}

	public boolean saveRaidSummaries(Map<Long, Integer> raidSummaries) {
		return userGroupService.saveRaidSummaries(raidSummaries);
	}

	private class MutableInteger {

		private int value;

		public MutableInteger(int vauel) {
			this.value = vauel;
		}

		public int get() {
			return value;
		}

		public void set(int value) {
			this.value = value;
		}

		public String toString() {
			return Integer.toString(value);
		}
	}
}
