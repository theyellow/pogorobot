package pogorobot.service.db;

import java.util.List;
import java.util.Set;

import pogorobot.entities.PokemonWithSpawnpoint;
import pogorobot.entities.ProcessedPokemon;
import pogorobot.entities.SendMessages;
import pogorobot.telegram.util.SendMessageAnswer;

public interface ProcessedElementsServiceRepository {

	public List<SendMessages> retrievePostedMessagesForGymId(String gymId);

	public void cleanupSendMessage(List<SendMessages> messagesWithTimeOver, long nowInSeconds);

	public ProcessedPokemon updateProcessedMonster(ProcessedPokemon processedPokemon, SendMessages sentMessage);

	public List<Set<SendMessages>> retrievePossibleMessageIdToUpdate(PokemonWithSpawnpoint pokemon, boolean deepScan,
			List<Long> updatedChats, List<SendMessages> sendMessageAnswers);

	public int deleteNonPostedProcessedRaidsOnDatabase();

	public void deleteOldRaidsOnDatabase(long nowInSeconds);

	public List<SendMessages> retrievePostedRaidMessagesWithTimeOver(Long nowInSeconds);

	public List<SendMessages> retrievePostedMonsterMessagesWithTimeOver(Long nowInSeconds);

}
