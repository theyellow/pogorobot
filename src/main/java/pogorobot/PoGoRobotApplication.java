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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.transaction.PlatformTransactionManager;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiValidationException;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import pogorobot.entities.Filter;
import pogorobot.entities.FilterType;
import pogorobot.entities.PossibleRaidPokemon;
import pogorobot.entities.User;
import pogorobot.entities.UserGroup;
import pogorobot.repositories.PossibleRaidPokemonRepository;
import pogorobot.service.ConfigReader;
import pogorobot.service.FilterService;
import pogorobot.service.GymService;
import pogorobot.service.PokemonService;
import pogorobot.service.TelegramKeyboardService;
import pogorobot.service.TelegramSendMessagesService;
import pogorobot.service.UserService;
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

	Logger logger = LoggerFactory.getLogger(PoGoRobotApplication.class);

	// @Autowired
	// @Qualifier("standard")
	// private StandardConfiguration standardConfiguration;
	//

	private String[] args;

	private ThreadPoolTaskScheduler taskScheduler;

	private RaidBossListUpdater raidBossListUpdater;

	public static void main(String[] args) {
		SpringApplication.run(PoGoRobotApplication.class, args);
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		this.args = args.getSourceArgs();
	}

	private Runnable getDeleteOldProcessedMonsTask(PokemonService pokemonService) {
		return () -> {
			pokemonService.deleteProcessedPokemonOnDatabase();
		};
	}

	private Runnable getDeleteOldGymMonsTask(GymService gymService) {
		return () -> {
			gymService.deleteOldGymPokemonOnDatabase();
		};
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
					String pokemon = pokemonWithType.substring(0, endIndex).replaceAll("-", "");
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
			if (!sentToAdmin) {
				User admin = userService.createAdmin();
				FilterService filterService = ctx.getBean(FilterService.class);
				Filter userFilter = admin.getUserFilter();
				if (userFilter == null) {
					List<Integer> pokemon = new ArrayList<Integer>();
					pokemon.add(133);
					Double latitude = 48.743127;
					Double longitude = 9.207476;
					Double radius = 5.0;
					userFilter = filterService.createFilter(null, pokemon, null, latitude, longitude, radius, 3);
					userFilter = filterService.updateOrInsertFilter(userFilter);
					userFilter.setOwner(admin);
					admin.setUserFilter(userFilter);
					admin.setRaidadmin(true);
					admin = userService.updateOrInsertUser(admin);
				}
				sendStartMessage(ctx, admin);
			}

			ConfigReader configReader = ctx.getBean(ConfigReader.class);

			configReader.updateGeofences();
			configReader.updateGroupsWithIds();
			configReader.updateGroupFiltersWithGeofences();
			configReader.updateGroupFilterWithMons();
			configReader.updateGroupFilterWithRaidMons();
			configReader.updateGroupFilterWithIV();

			for (User user : allUsers) {
				if (user.isSuperadmin()) {
					sendUsergroupsMessageToSuperadmin(ctx, user);
				}
			}

			// setup cron for database cleanup jobs:

			PokemonService pokemonService = ctx.getBean(PokemonService.class);
			GymService gymService = ctx.getBean(GymService.class);
			PossibleRaidPokemonRepository raidBossRepository = ctx.getBean(PossibleRaidPokemonRepository.class);
			TelegramSendMessagesService telegramSendMessagesService = ctx.getBean(TelegramSendMessagesService.class);

			taskScheduler = new ThreadPoolTaskScheduler();
			taskScheduler.setPoolSize(5);
			taskScheduler.initialize();
			taskScheduler.schedule(getDeleteOldProcessedMonsTask(pokemonService), new CronTrigger("0 4 * * * *"));
			taskScheduler.schedule(getDeleteOldGymMonsTask(gymService), new CronTrigger("0 18 * * * *"));
			taskScheduler.schedule(getUpdateRaidBossListTask(raidBossRepository),
					new CronTrigger("0 16 5,11,17,23 * * *"));
			taskScheduler.schedule(getDeleteOldGymsTask(telegramSendMessagesService),
					new CronTrigger("10/40 * * * * *"));
		};
	}

	private Runnable getDeleteOldGymsTask(TelegramSendMessagesService telegramSendMessagesService) {
		return () -> {
			try {
				telegramSendMessagesService.removeGroupRaidMessage();
				logger.info("Cleaned messages...");
			} catch (TelegramApiException e) {
				logger.error("Error while deleting messages...", e);
			}
		};
	}

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
		dataSource.setMaxPoolSize(25);
		dataSource.setDebugUnreturnedConnectionStackTraces(true);

		return dataSource;
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
		telegramSendMessagesService.sendMessage(sendMessage);
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
		PogoBot pogoBot = new PogoBot(botname, bottoken);
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
		HelpCommand helpCommand = ctx.getBean(HelpCommand.class, pogoBot);
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
			ApiContextInitializer.init();
			TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
			return telegramBotsApi;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		logger.error("TelegramBotAPI could not be started");
		return null;
	}

}