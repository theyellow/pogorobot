package pogorobot.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import pogorobot.entities.ProcessedRaids;
import pogorobot.entities.SendMessages;

@Service("processedElementsService")
public class ProcessedElementsServiceImpl implements ProcessedElementsService {

	private static Logger logger = LoggerFactory.getLogger(ProcessedElementsService.class);

	@SuppressWarnings("squid:S3749")
	@PersistenceContext
	private EntityManager entityManager;

	@Override
	@Transactional(TxType.REQUIRED)
	public List<SendMessages> retrievePostedMessagesForGymId(String gymId) {
		List<SendMessages> result = new ArrayList<>();
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		CriteriaQuery<ProcessedRaids> queryProcessedRaids = criteriaBuilder.createQuery(ProcessedRaids.class);

		Root<ProcessedRaids> processedRaid = queryProcessedRaids.from(ProcessedRaids.class);
		queryProcessedRaids = queryProcessedRaids.where(criteriaBuilder.equal(processedRaid.get("gymId"), gymId));
		
		List<ProcessedRaids> processeRaidsForGym = entityManager.createQuery(queryProcessedRaids).getResultList();
		
		for (ProcessedRaids processedRaids : processeRaidsForGym) {
			Set<SendMessages> groupsRaidIsPosted = processedRaids.getGroupsRaidIsPosted();
			logger.debug("found " + groupsRaidIsPosted.size());
			for (SendMessages message : groupsRaidIsPosted) {
				if (message.getMessageId() != null && message.getGroupChatId() != null) {
					result.add(message);
				}
			}
		}
		
		return result;
	}

}
