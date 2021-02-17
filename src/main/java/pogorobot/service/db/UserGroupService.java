package pogorobot.service.db;

import java.util.Map;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import pogorobot.entities.UserGroup;
import pogorobot.service.db.repositories.UserGroupRepository;

@Service("userGroupService")
public class UserGroupService {

	@Autowired
	private UserGroupRepository userGroupRepository;
	
	@Transactional
	public boolean saveRaidSummaries(Map<Long, Integer> raidSummariesToSave) {
		Optional<Boolean> optionalSavedAllEntities = raidSummariesToSave.entrySet().stream().map(x -> {
			UserGroup chat = userGroupRepository.findByChatId(x.getKey());
			chat.setRaidSummaryLink(x.getValue());
			UserGroup userGroup = userGroupRepository.save(chat);
			return userGroup != null;
		}).reduce((x, y) -> x && y);
		if (optionalSavedAllEntities.isPresent()) {
			return optionalSavedAllEntities.get();
		} else {
			return false;
		}
	}
	
}
