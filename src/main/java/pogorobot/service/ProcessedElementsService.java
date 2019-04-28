package pogorobot.service;

import java.util.List;

import pogorobot.entities.SendMessages;

public interface ProcessedElementsService {

	public List<SendMessages> retrievePostedMessagesForGymId(String gymId);

}
