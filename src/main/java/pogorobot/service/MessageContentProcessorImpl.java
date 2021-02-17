/**
 Copyright 2019 Benjamin Marstaller
 
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at
 
    http://www.apache.org/licenses/LICENSE-2.0
 
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/

package pogorobot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.UnexpectedRollbackException;

import pogorobot.entities.EggWithGym;
import pogorobot.entities.Gym;
import pogorobot.entities.PokemonWithSpawnpoint;
import pogorobot.entities.RaidAtGymEvent;
import pogorobot.entities.RaidWithGym;
import pogorobot.events.EventMessage;
import pogorobot.events.webservice.WebserviceEgg;
import pogorobot.events.webservice.WebserviceGym;
import pogorobot.events.webservice.WebserviceInvasionEventMessage;
import pogorobot.events.webservice.WebserviceQuest;
import pogorobot.events.webservice.WebserviceRaid;
import pogorobot.service.db.GymService;
import pogorobot.service.db.PokemonService;
import pogorobot.service.pure.LoggingHelperService;

@Service("messageContentProcessor")
public class MessageContentProcessorImpl implements MessageContentProcessor {

	@Autowired
	private GymService gymService;

	@Autowired
	private PokemonService pokemonService;

	@Autowired
	private TelegramService telegramService;

	@Autowired
	private LoggingHelperService loggingHelperService;

	private static Logger logger = LoggerFactory.getLogger(MessageContentProcessor.class);

	@Override
	public <T> EventMessage<T> processContent(EventMessage<T> message) {
		T entity = message.transformToEntity();
		// Hack: if gym not exists with lat/long we have a problem -> pgss
		if (message instanceof WebserviceRaid) {
			WebserviceRaid rocketmapRaid = ((WebserviceRaid) message);
			if (rocketmapRaid.getName() != null && !rocketmapRaid.getName().isEmpty()) {
				WebserviceGym rocketmapGym = new WebserviceGym();
				rocketmapGym.setGym_id(rocketmapRaid.getGym_id());
				rocketmapGym.setName(rocketmapRaid.getName());
				rocketmapGym.setMove_1(rocketmapRaid.getMove_1());
				rocketmapGym.setMove_2(rocketmapRaid.getMove_2());
				rocketmapGym.setUrl(rocketmapRaid.getUrl());
				rocketmapGym.setLatitude(rocketmapRaid.getLatitude());
				rocketmapGym.setLongitude(rocketmapRaid.getLongitude());
				rocketmapGym.setEx_raid_eligible(rocketmapRaid.getEx_raid_eligible());
				rocketmapGym.setSponsor_id(rocketmapRaid.getSponsor_id());
				logger.debug(message.toString());
				processContent(rocketmapGym);
			}
		} else if (message instanceof WebserviceEgg) {
			WebserviceEgg rocketmapEgg = ((WebserviceEgg) message);
			if (rocketmapEgg.getName() != null && !rocketmapEgg.getName().isEmpty()) {
				WebserviceGym rocketmapGym = new WebserviceGym();
				rocketmapGym.setGym_id(rocketmapEgg.getGym_id());
				rocketmapGym.setName(rocketmapEgg.getName());
				rocketmapGym.setUrl(rocketmapEgg.getUrl());
				rocketmapGym.setLatitude(rocketmapEgg.getLatitude());
				rocketmapGym.setLongitude(rocketmapEgg.getLongitude());
				rocketmapGym.setEx_raid_eligible(rocketmapEgg.getExraid_eglible());
				rocketmapGym.setSponsor_id(rocketmapEgg.getSponsor_id());
				logger.debug(message.toString());
				processContent(rocketmapGym);
			}
		}

		if (entity instanceof Gym) {
			gymService.updateOrInsertGym((Gym) entity);
			logger.debug(message.toString());
		} else if (entity instanceof RaidAtGymEvent) {
			RaidAtGymEvent incomingRaid = (RaidAtGymEvent) entity;
			if (incomingRaid.getGymId() != null) {
				// gymService.updateOrInsertGymWithRaid(incomingRaid);
				boolean changed = gymService.updateOrInsertGymWithRaid(incomingRaid);
				if (changed) {
					telegramService.triggerRaidMessages(incomingRaid);
				}
				logger.debug(message.toString());
			}
		} else if (entity instanceof RaidWithGym) {
			RaidWithGym raidWithGym = (RaidWithGym) entity;
			if (raidWithGym.getGymId() != null) {
				RaidAtGymEvent incomingRaid = new RaidAtGymEvent(raidWithGym);
				boolean changed = gymService.updateOrInsertGymWithRaid(incomingRaid);
				if (changed) {
					telegramService.triggerRaidMessages(incomingRaid);
				}
				logger.debug(message.toString());
			}
		} else if (entity instanceof EggWithGym) {
			// logger.warn("Egg!!! RocketmapEgg!!! EggWithGym!!!\n"
			// + "That can't be possible, it's a new kind of message. Message:");
			// logger.warn(message.toString());
			EggWithGym eggWithGym = (EggWithGym) entity;
			if (eggWithGym.getGymId() != null) {
				RaidAtGymEvent incomingRaid = new RaidAtGymEvent(eggWithGym);
				boolean changed = gymService.updateOrInsertGymWithRaid(incomingRaid);
				if (changed) {
					telegramService.triggerRaidMessages(incomingRaid);
				}
				logger.debug(message.toString());
			}
		} else if (entity instanceof PokemonWithSpawnpoint) {
			try {
				boolean changed = pokemonService.updateOrInsertPokemon((PokemonWithSpawnpoint) entity);
				if (changed) {
					telegramService.triggerPokemonMessages((PokemonWithSpawnpoint) entity);
				}
			} catch (TransactionException ex) {
				logger.error("Transactional {}: {}", ex.getClass().getName(), ex.getMessage());
				loggingHelperService.logStacktraceForMethod(ex.getStackTrace(), "pogorobot");
			} catch (Exception ex) {
				logger.error("{}: {}", ex.getClass().getName(), ex.getMessage());
				loggingHelperService.logStacktraceForMethod(ex.getStackTrace(), "pogorobot");
			}
		} else if (message instanceof WebserviceQuest) {
			logger.trace("Quest found " + ((WebserviceQuest) message).toString());
			// logger.info(message + "");
		} else if (message instanceof WebserviceInvasionEventMessage) {
			logger.trace("Invasion found " + ((WebserviceInvasionEventMessage) message).toString());
			// logger.info(message.toString());
		}
		return message;
	}


}
