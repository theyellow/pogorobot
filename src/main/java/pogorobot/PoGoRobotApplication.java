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

package pogorobot;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.transaction.PlatformTransactionManager;
import org.telegram.telegrambots.bots.DefaultBotOptions;
//import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiValidationException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import liquibase.integration.spring.SpringLiquibase;
import pogorobot.entities.Filter;
import pogorobot.entities.FilterType;
import pogorobot.entities.PossibleRaidPokemon;
import pogorobot.entities.SendMessages;
import pogorobot.entities.User;
import pogorobot.entities.UserGroup;
import pogorobot.service.ConfigReader;
import pogorobot.service.TelegramKeyboardService;
import pogorobot.service.TelegramSendMessagesService;
import pogorobot.service.db.FilterService;
import pogorobot.service.db.GymService;
import pogorobot.service.db.PokemonService;
import pogorobot.service.db.ProcessedElementsServiceRepository;
import pogorobot.service.db.UserService;
import pogorobot.service.db.repositories.PossibleRaidPokemonRepository;
import pogorobot.telegram.PogoBot;
import pogorobot.telegram.commands.HelloCommand;
import pogorobot.telegram.commands.HelpCommand;
import pogorobot.telegram.commands.StartCommand;
import pogorobot.telegram.commands.StopCommand;
import pogorobot.telegram.commands.StopallCommand;
import pogorobot.telegram.config.StandardConfiguration;
import pogorobot.util.RaidBossListFetcher;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "pogorobot")
public class PoGoRobotApplication implements ApplicationRunner {

	private Logger logger = LoggerFactory.getLogger(PoGoRobotApplication.class);

	// @Autowired
	// @Qualifier("standard")
	// private StandardConfiguration standardConfiguration;
	//

	private String[] args;

	private ThreadPoolTaskScheduler taskScheduler;

	private RaidBossListUpdater raidBossListUpdater;

	private GroupfilesTimestamps groufileTimestamp;

//	private final Timer deleteMessageTimer = new Timer(true);

	public static void main(String[] args) {
		SpringApplication.run(PoGoRobotApplication.class, args);
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		this.args = args.getSourceArgs();
	}

	 private Runnable getDeleteOldProcessedMonsTask(PokemonService pokemonService)
	 {
	 return () -> {
		 pokemonService.cleanPokemonWithSpawnpointOnDatabase();
	 };
	 }

	private Runnable getDeleteOldGymMonsTask(GymService gymService) {
		return () -> {
			gymService.deleteOldGymPokemonOnDatabase();
		};
	}

	private Runnable getReloadConfigurationTask(ConfigReader configReader) {
		return () -> {
			logger.debug("Check for new configuration timestamps");
			if (groupfilesNewTimestamp()) {
				logger.info("Reload group*.txt files and geofences");
				try {
					loadConfiguration(configReader);
				} catch (IOException e) {
					logger.error("Error while reloading configuration", e);
				}
			}
		};
	}

	private boolean groupfilesNewTimestamp() {
		GroupfilesTimestamps groupfilesTimestamps = new GroupfilesTimestamps();
		int compareValue = groupfilesTimestamps.compareTo(groufileTimestamp);
		return compareValue != 0;
	}

	private final class CleanupMessageTask implements Runnable {

		private ProcessedElementsServiceRepository processedElementsService;
		private TelegramSendMessagesService telegramSendMessagesService;

		public CleanupMessageTask(ProcessedElementsServiceRepository processedElementsService,
				TelegramSendMessagesService telegramSendMessagesService) {
			this.processedElementsService = processedElementsService;
			this.telegramSendMessagesService = telegramSendMessagesService;
		}

		@Override
		public void run() {
			long nowInSeconds = System.currentTimeMillis() / 1000;
			// ProcessedRaids owningRaid = null;
			// ProcessedPokemon owningMon = null;
			// String errorsWhileDeleting = "";
			// StopWatch stopWatch = StopWatch.createStarted();

			StopWatch stopWatch = StopWatch.createStarted();
			List<SendMessages> messagesWithTimeOver = processedElementsService
					.retrievePostedMonsterMessagesWithTimeOver(nowInSeconds);
			messagesWithTimeOver.addAll(processedElementsService.retrievePostedRaidMessagesWithTimeOver(nowInSeconds));
			// all.addAll(processedElementsService.retrievePostedMessagesWithoutExistingProcessedElement());
			// try {
			processedElementsService.cleanupSendMessage(messagesWithTimeOver, nowInSeconds);
			logger.debug("Cleaned messages...");
			// toDeleteOnTelegram = deleteMessagesOnTelegram(sendMessagesClone, nowInSeconds
			// - endTime);

			stopWatch.stop();
			long time = stopWatch.getTime(TimeUnit.SECONDS);
			if (time > 10) {
				logger.warn("slow database and message cleanup took {} seconds", time);
			} else if (time > 5) {
				logger.info("database and message cleanup took {} seconds", time);
			} else {
				logger.debug("fast database and message cleanup took {} seconds", time);
			}
			stopWatch.reset();
			stopWatch.start();
			messagesWithTimeOver.forEach(x -> {
				telegramSendMessagesService.deleteMessagesOnTelegram(x, 1);
			});
			stopWatch.stop();
			time = stopWatch.getTime(TimeUnit.SECONDS);
			if (time > 10) {
				logger.warn("slow telegram cleanup took {} seconds", time);
			} else if (time > 5) {
				logger.info("telegram message cleanup took {} seconds", time);
			} else {
				logger.debug("fast telegram cleanup took {} seconds", time);
			}
		}
	}
	

//	private final class CleanupPokemonTask implements Runnable {
//
//		private PokemonService pokemonService;
//
//		public CleanupPokemonTask(PokemonService telegramSendMessagesService) {
//			this.pokemonService = telegramSendMessagesService;
//		}
//
//		@Override
//		public void run() {
//			pokemonService.cleanPokemonWithSpawnpointOnDatabase();
//			logger.debug("Cleaned pokemon...");
//		}
//	}

	private class GroupfilesTimestamps implements Comparable<GroupfilesTimestamps> {
		private long timestampGroupRaidLevelFile;
		private long timestampGroupRaidMonstersFile;
		private long timestampGroupMonsterFile;
		private long timestampGroupIvFile;
		private long timestampGroupGeofencesFile;
		private long timestampGroupGeofencesFileIv;
		private long timestampGroupGeofencesFileMonster;
		private long timestampGroupGeofencesFileRaids;
		private long timestampGroupChatIdFile;
		private long timestampGroupXraidFile;
		private long timestampGeofencesFile;

		public GroupfilesTimestamps() {
			String relativePath = System.getProperty("ext.properties.dir").substring(5);
			try {
				timestampGroupRaidLevelFile = Files.getLastModifiedTime(Paths.get(relativePath, "groupraidlevel.txt"))
						.toMillis();
			} catch (IOException e) {
				timestampGroupRaidLevelFile = 0;
				logger.error("Couldn't retrieve timestamps for raid level at " + relativePath);
			}
			try {
				timestampGroupRaidMonstersFile = Files
						.getLastModifiedTime(Paths.get(relativePath, "groupraidmonsters.txt")).toMillis();
			} catch (IOException e) {
				timestampGroupRaidMonstersFile = 0;
				logger.error("Couldn't retrieve timestamps for raid monsters at " + relativePath);
			}
			try {
				timestampGroupMonsterFile = Files.getLastModifiedTime(Paths.get(relativePath, "groupmonsters.txt"))
						.toMillis();
			} catch (IOException e) {
				timestampGroupMonsterFile = 0;
				logger.error("Couldn't retrieve timestamps for monsters settings at " + relativePath);
			}
			try {
				timestampGroupXraidFile = Files.getLastModifiedTime(Paths.get(relativePath, "groupxraidgymall.txt"))
						.toMillis();
			} catch (IOException e) {
				timestampGroupXraidFile = 0;
				logger.error("Couldn't retrieve timestamps of exraidgyms settings at " + relativePath);
			}
			try {
				timestampGroupIvFile = Files.getLastModifiedTime(Paths.get(relativePath, "groupiv.txt")).toMillis();
			} catch (IOException e) {
				timestampGroupIvFile = 0;
				logger.error("Couldn't retrieve timestamps monster iv settings at " + relativePath);
			}
			try {
				timestampGroupGeofencesFile = Files.getLastModifiedTime(Paths.get(relativePath, "groupgeofences.txt"))
						.toMillis();
			} catch (IOException e) {
				timestampGroupGeofencesFile = 0;
				logger.trace("Couldn't retrieve timestamps for geofence legacy settings at " + relativePath);
			}
			try {
				timestampGroupChatIdFile = Files.getLastModifiedTime(Paths.get(relativePath, "groupchatid.txt"))
						.toMillis();
			} catch (IOException e) {
				timestampGroupChatIdFile = 0;
				logger.error("Couldn't retrieve timestamps for chat id settings at " + relativePath);
			}
			try {
				timestampGeofencesFile = Files.getLastModifiedTime(Paths.get(relativePath, "geofences.txt")).toMillis();
			} catch (IOException e) {
				timestampGeofencesFile = 0;
				logger.error("Couldn't retrieve timestamps for geofences at " + relativePath);
			}
			try {
				timestampGroupGeofencesFileIv = Files
						.getLastModifiedTime(Paths.get(relativePath, "groupgeofencesiv.txt")).toMillis();
			} catch (IOException e) {
				timestampGroupGeofencesFileIv = 0;
				logger.debug("Couldn't retrieve timestamps for individual iv geofences settings at " + relativePath);
			}
			try {
				timestampGroupGeofencesFileMonster = Files
						.getLastModifiedTime(Paths.get(relativePath, "groupgeofencesmonsters.txt")).toMillis();
			} catch (IOException e) {
				timestampGroupGeofencesFileMonster = 0;
				logger.debug(
						"Couldn't retrieve timestamps for individual monster geofences settings at " + relativePath);
			}
			try {
				timestampGroupGeofencesFileRaids = Files
						.getLastModifiedTime(Paths.get(relativePath, "groupgeofencesraids.txt")).toMillis();

			} catch (IOException e) {
				timestampGroupGeofencesFileRaids = 0;
				logger.debug("Couldn't retrieve timestamps for special raid geofences settings at " + relativePath);
			}

		}

		@Override
		public int compareTo(GroupfilesTimestamps o) {
			if (null == o) {
				return 1;
			}
			// @formatter:off
			if (timestampGeofencesFile == o.timestampGeofencesFile &&
				timestampGroupChatIdFile == o.timestampGroupChatIdFile &&
				timestampGroupGeofencesFile == o.timestampGroupGeofencesFile &&
				timestampGroupGeofencesFileIv == o.timestampGroupGeofencesFileIv &&
				timestampGroupGeofencesFileMonster == o.timestampGroupGeofencesFileMonster &&
				timestampGroupGeofencesFileRaids == o.timestampGroupGeofencesFileRaids &&
				timestampGroupXraidFile == o.timestampGroupXraidFile &&
				timestampGroupIvFile == o.timestampGroupIvFile &&
				timestampGroupMonsterFile == o.timestampGroupMonsterFile &&
				timestampGroupRaidMonstersFile == o.timestampGroupRaidMonstersFile &&
				timestampGroupRaidLevelFile == o.timestampGroupRaidLevelFile) {
			// @formatter:on
				return 0;
			}
			return -1;
		}

	}

	private Runnable getUpdateRaidBossListTask(PossibleRaidPokemonRepository raidPokemonRepository) {
		return () -> {
			if (raidBossListUpdater == null) {
				raidBossListUpdater = updateRaidBossList(raidPokemonRepository);
			}
			raidBossListUpdater.updateRaidBossList();
		};
	}

	@Bean
	public RaidBossListUpdater updateRaidBossList(PossibleRaidPokemonRepository raidPokemonRepository) {
		if (raidBossListUpdater == null) {
			raidBossListUpdater = new RaidBossListUpdater(raidPokemonRepository);
		}
		return raidBossListUpdater;
	}

	public class RaidBossListUpdater {

		private PossibleRaidPokemonRepository raidPokemonRepository;

		public RaidBossListUpdater(PossibleRaidPokemonRepository raidPokemonRepository) {
			this.raidPokemonRepository = raidPokemonRepository;
		}

		public void updateRaidBossList() {
			RaidBossListFetcher raidBossListFetcher = new RaidBossListFetcher();

			// Attention, selenium is not threadsave and will be used here
			List<String> possibleRaidBosses = raidBossListFetcher.getBosses();
			if (possibleRaidBosses != null && possibleRaidBosses.size() > 0) {
				raidPokemonRepository.findAll().forEach(x -> raidPokemonRepository.delete(x));
				for (String levelAndPokemon : possibleRaidBosses) {
					String level = levelAndPokemon.substring(0, 1);
					String pokemonWithType = levelAndPokemon.substring(2);
					int pokemonStringLength = pokemonWithType.length();
					int endIndex = pokemonStringLength < 3 ? pokemonStringLength : 3;
					String pokemon = pokemonWithType.substring(0, endIndex).replaceAll("-", "").replaceAll("m", "");
					String type = "";
					if (endIndex <= pokemonStringLength) {
						type = pokemonWithType.substring(endIndex, pokemonStringLength).replaceAll("-", "");
					}
					PossibleRaidPokemon possibleRaidPokemon = new PossibleRaidPokemon();
					possibleRaidPokemon.setLevel(Integer.valueOf(level));
					possibleRaidPokemon.setPokemonId(Integer.valueOf(pokemon));
					possibleRaidPokemon.setType(type);
					raidPokemonRepository.save(possibleRaidPokemon);
				}
			}
		}
	}

	@Bean
	public LocalContainerEntityManagerFactoryBean entityManagerFactory(ApplicationContext ctx,
			ApplicationArguments aArgs) {
		StandardConfiguration standardConfiguration = BeanFactoryAnnotationUtils
				.qualifiedBeanOfType(ctx.getAutowireCapableBeanFactory(), StandardConfiguration.class, "standard");
		// StandardConfiguration standardConfiguration =
		// ctx.getBean(StandardConfiguration.class);
		LocalContainerEntityManagerFactoryBean entityManagerFactory = new LocalContainerEntityManagerFactoryBean();

		HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
		vendorAdapter.setGenerateDdl(standardConfiguration.isGenerateDdl());
		vendorAdapter.setShowSql(false);

		entityManagerFactory.setJpaVendorAdapter(vendorAdapter);

		entityManagerFactory.setDataSource(dataSource(ctx, aArgs));
		entityManagerFactory.setPackagesToScan(new String[] { "pogorobot.entities" });

		Properties additionalProperties = new Properties();
		// additionalProperties.put("hibernate.hbm2ddl.auto", "create");
		// additionalProperties.put("hibernate.show_sql", "true");

		additionalProperties.put("hibernate.dialect", standardConfiguration.getHibernateDialect());
		additionalProperties.put("hibernate.temp.use_jdbc_metadata_defaults", "false");
		additionalProperties.put("hibernate.jdbc.lob.non_contextual_creation", "true");
		entityManagerFactory.setJpaProperties(additionalProperties);


		return entityManagerFactory;
	}

	@Bean
	public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
		return args -> {
			this.args = args;
			// args = aArgs.getSourceArgs();
			String[] beanNames = ctx.getBeanDefinitionNames();
			Arrays.sort(beanNames);
			logger.info("Let's inspect the beans:");
			for (String beanName : beanNames) {
				logger.debug(beanName);
				if ("pokemonSender".equalsIgnoreCase(beanName)) {
					logger.info("Found pokemonSender");
				}
			}

			UserService userService = ctx.getBean(UserService.class);
			Boolean sentToAdmin = false;
			Iterable<User> allUsers = userService.getAllUsers();
			for (User user : allUsers) {
				if (user.isAdmin()) {
					user.setRaidadmin(true);
					sendStartMessage(ctx, user);
					sentToAdmin = true;
				}
			}
			ConfigReader configReader = ctx.getBean(ConfigReader.class);

			loadConfiguration(configReader);

			for (User user : allUsers) {
				if (user.isSuperadmin()) {
					sendUsergroupsMessageToSuperadmin(ctx, user);
				}
			}

			// setup cron for database cleanup jobs:

//			PokemonService pokemonService = ctx.getBean(PokemonService.class);
			GymService gymService = ctx.getBean(GymService.class);
			PossibleRaidPokemonRepository raidBossRepository = ctx.getBean(PossibleRaidPokemonRepository.class);
			ProcessedElementsServiceRepository processedElementsService = ctx.getBean(ProcessedElementsServiceRepository.class);
			TelegramSendMessagesService telegramSendMessagesService = ctx.getBean(TelegramSendMessagesService.class);

			taskScheduler = new ThreadPoolTaskScheduler();
			taskScheduler.setPoolSize(8);
			taskScheduler.initialize();
			 PokemonService pokemonService = ctx.getBean(PokemonService.class) ;
			taskScheduler.schedule(getDeleteOldProcessedMonsTask(pokemonService), new
			 CronTrigger("0 */4 * * * *"));
			taskScheduler.schedule(getDeleteOldGymMonsTask(gymService), new CronTrigger("0 18 22 1 1 *"));
			taskScheduler.schedule(getUpdateRaidBossListTask(raidBossRepository),
					new CronTrigger("0 16 5,11,17,23 * * *"));
			// taskScheduler.schedule(getDeleteOldGymsTask(pokemonService),
			// new CronTrigger("10 * * * * *"));
			taskScheduler.schedule(getReloadConfigurationTask(ctx.getBean(ConfigReader.class)),
					new CronTrigger("20/50 * * * * *"));

			taskScheduler.schedule(new CleanupMessageTask(processedElementsService, telegramSendMessagesService),
					new PeriodicTrigger(60 * 1000L));
//			taskScheduler.schedule(new CleanupPokemonTask(pokemonService), new PeriodicTrigger(10 * 60 * 1000L));

			
			
			// deleteMessageTimer.schedule(getCleanupMessagesGymsTask(pokemonService), 100L,
			// 60 * 1000L);
		};
	}

	private void loadConfiguration(ConfigReader configReader) throws IOException {
		groufileTimestamp = new GroupfilesTimestamps();
		configReader.updateGeofences();
		configReader.updateGroupsWithIds();
		configReader.updateGroupsWithExRaidFlags();
		configReader.updateGroupFiltersWithGeofences();
		configReader.updateGroupFilterWithMons();
		configReader.updateGroupFilterWithRaidMons();
		configReader.updateGroupFilterWithIV();
		configReader.updateGroupFilterWithLevel();
	}

	// private TimerTask getCleanupMessagesGymsTask(TelegramSendMessagesService
	// pokemonService) {
	// return new CleanupMessageTask(pokemonService);
//	}

	@Bean
	public DataSource dataSource(ApplicationContext ctx, ApplicationArguments aArgs) {
		args = aArgs.getSourceArgs();
		StandardConfiguration standardConfiguration = BeanFactoryAnnotationUtils
				.qualifiedBeanOfType(ctx.getAutowireCapableBeanFactory(), StandardConfiguration.class, "standard");
		final ComboPooledDataSource dataSource = new ComboPooledDataSource();
		try {
			dataSource.setDriverClass(standardConfiguration.getControllerdb());
		} catch (PropertyVetoException e) {
			logger.error("db-driver in configuration file wrong?");
			throw new RuntimeException(e);
		}
		dataSource.setJdbcUrl(standardConfiguration.getJdbcUrl());
		dataSource.setUser(standardConfiguration.getUserdb());
		String password = standardConfiguration.getPassword() != null
				&& standardConfiguration.getPassword().trim().isEmpty() ? null : standardConfiguration.getPassword();
		dataSource.setPassword(password);
		dataSource.setMinPoolSize(3);
		dataSource.setMaxPoolSize(80);
		dataSource.setDebugUnreturnedConnectionStackTraces(true);

		dataSource.setIdleConnectionTestPeriod(2);
		dataSource.setTestConnectionOnCheckin(true);

		return dataSource;
	}

	@Bean
	public LiquibaseProperties liquibaseProperties(ApplicationContext ctx) {
		LiquibaseProperties result = new LiquibaseProperties();

		StandardConfiguration standardConfiguration = BeanFactoryAnnotationUtils
				.qualifiedBeanOfType(ctx.getAutowireCapableBeanFactory(), StandardConfiguration.class, "standard");

		if (!standardConfiguration.isGenerateDdl()) {
			// production mode: "generateDdl" is set to false
			logger.info("'production-mode' database generation");
			String dbType = standardConfiguration.getJdbcUrl().contains("mysql") ? "mysql" : "postgresql";
			result.setChangeLog("classpath:/liquibase/datadir/db." + dbType + ".changelog.xml");
		} else {
			logger.info("'develop-mode' database generation with hbm2ddl will be done later");
			result.setEnabled(false);
		}

		return result;
	}
	// @EnableConfigurationProperties(LiquibaseProperties.class))
	// LiquibaseProperties properties;

	@Bean
	public SpringLiquibase liquibase(ApplicationContext ctx) {
		SpringLiquibase liquibase = new SpringLiquibase();
		LiquibaseProperties properties = ctx.getBean(LiquibaseProperties.class);

		DataSource dataSource = ctx.getBean(DataSource.class);

		// properties.setChangeLog("classpath:/db/changelog/db.changelog-master.xml");
		boolean enabled = properties.isEnabled();
		if (enabled) {
			liquibase.setChangeLog(properties.getChangeLog());
			liquibase.setContexts(properties.getContexts());
			liquibase.setDataSource(dataSource);
			liquibase.setDefaultSchema(properties.getDefaultSchema());
			liquibase.setDropFirst(properties.isDropFirst());
		}
		liquibase.setShouldRun(enabled);
		return liquibase;
	}

	@Bean
	public PlatformTransactionManager transactionManager(ApplicationContext ctx, ApplicationArguments aArgs) {
		JpaTransactionManager transactionManager = new JpaTransactionManager();
		transactionManager.setEntityManagerFactory(entityManagerFactory(ctx, aArgs).getObject());
		return transactionManager;
	}

	@Bean
	public PersistenceExceptionTranslationPostProcessor exceptionTranslation() {
		return new PersistenceExceptionTranslationPostProcessor();
	}

	private void sendStartMessage(ApplicationContext ctx, User user) {
		String text = "PogoRobot ist neu gestartet...";
		sendMessageToUser(ctx, text, user);
	}

	private void sendUsergroupsMessageToSuperadmin(ApplicationContext ctx, User user) {
		String text = "Vorhandene Gruppen:\n";
		FilterService filterService = ctx.getBean(FilterService.class);
		List<Filter> groupFilters = filterService.getFiltersByType(FilterType.GROUP);
		if (groupFilters != null) {
			for (Filter filter : groupFilters) {
				UserGroup group = filter.getGroup();
				if (null != group) {
					text += group.getGroupName() + " : " + group.getChatId() + "\n";
				}
			}
		}
		sendMessageToUser(ctx, text, user);
	}

	private void sendMessageToUser(ApplicationContext ctx, String text, User admin) {
		TelegramSendMessagesService telegramSendMessagesService = ctx.getBean(TelegramSendMessagesService.class);
		TelegramKeyboardService telegramKeyboardService = ctx.getBean(TelegramKeyboardService.class);
		String chatId = admin.getChatId() == null ? admin.getTelegramId() : admin.getChatId();
		SendMessage sendMessage = new SendMessage(chatId, text);
		ReplyKeyboardMarkup keyboard = telegramKeyboardService.getSettingsKeyboard(admin.isRaidadmin());
		sendMessage.setReplyMarkup(keyboard);
		telegramSendMessagesService.sendMessageTimed(Long.valueOf(chatId), sendMessage);
	}

	public final String[] getArgs() {
		return args;
	}

	@Bean
	public PogoBot pogoBot(ApplicationContext ctx, ApplicationArguments aArgs) {
		StandardConfiguration standardConfiguration = BeanFactoryAnnotationUtils
				.qualifiedBeanOfType(ctx.getAutowireCapableBeanFactory(), StandardConfiguration.class, "standard");
		String botname = standardConfiguration.getBotname();
		String bottoken = standardConfiguration.getBottoken();
		TelegramBotsApi botAPI = ctx.getBean(TelegramBotsApi.class);
		DefaultBotOptions options = new DefaultBotOptions();
	
		PogoBot pogoBot = new PogoBot(options, botname, bottoken);
		pogoBot.setConfiguration(standardConfiguration);
		try {
			botAPI.registerBot(pogoBot);
		} catch (TelegramApiException e) {
			if (e instanceof TelegramApiRequestException) {
				TelegramApiRequestException x = (TelegramApiRequestException) e;
				logger.error(
						x.getErrorCode() + " - " + x.getApiResponse()
								+ (x.getParameters() != null ? " - Parameter: " + x.getParameters().toString() : ""),
						x.getCause());
			} else if (e instanceof TelegramApiValidationException) {
				TelegramApiValidationException x = (TelegramApiValidationException) e;
				logger.error(x.getMethod() + " - " + x.getObject() + " - Error: " + x.toString(), x.getCause());
			} else {
				logger.error(e.getMessage(), e.getCause());
			}
		}
		StartCommand startCommand = ctx.getBean(StartCommand.class);
		HelloCommand helloCommand = ctx.getBean(HelloCommand.class);
		HelpCommand helpCommand = ctx.getBean(HelpCommand.class);
		StopCommand stopCommand = ctx.getBean(StopCommand.class);
		StopallCommand stopallCommand = ctx.getBean(StopallCommand.class);
		pogoBot.register(helloCommand);
		pogoBot.register(stopCommand);
		pogoBot.register(stopallCommand);
		pogoBot.register(helpCommand);
		pogoBot.register(startCommand);
		return pogoBot;
	}

	@Bean
	public TelegramBotsApi telegramBotsApi() {
		// Boolean usewebhook = standardConfiguration.getUsewebhook();
		try {
			TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
			return telegramBotsApi;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		logger.error("TelegramBotAPI could not be started");
		return null;
	}

}