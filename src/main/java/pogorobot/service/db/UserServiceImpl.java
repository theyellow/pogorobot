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

/**
 * 
 */
package pogorobot.service.db;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import pogorobot.entities.Filter;
import pogorobot.entities.FilterType;
import pogorobot.entities.User;
import pogorobot.service.db.repositories.UserRepository;

@Service("userService")
public class UserServiceImpl implements UserService {

	@SuppressWarnings("squid:S3749")
	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private UserRepository userDAO;
	
	@Override
	public Iterable<User> getAllUsers() {
		return userDAO.findAll();
	}

	@Override
	public User createAdmin() {
		User user = getOrCreateUser("259390556");
		user.setAdmin(true);
		user.setIngameName("theyellow23");
//		user.setName("Benjamin");
		user.setPayed(true);
		user.setSuperadmin(true);
		user.setTrainerLevel(38L);
		user.setTelegramId("259390556");
		user.setShowPokemonMessages(true);
//		user = entityManager.merge(user);
		return user;
	}

	@Override
	public User createUser(String name, String telegramId, String chatId) {
		User user = new User();
		user.setAdmin(false);
		user.setIngameName("inGameName");
		user.setName(name);
		user.setPayed(true);
		user.setSuperadmin(false);
		user.setTrainerLevel(1L);
		user.setTelegramId(telegramId);
		user.setChatId(chatId);
		return user;
	}

	@Override
	public User createUser(String name, String telegramId) {
		return createUser(name, telegramId, telegramId);
	}

	@Override
	public void deleteUser(User user) {
		userDAO.delete(user);
	}

	@Override
	@Transactional(TxType.REQUIRED)
	public User updateOrInsertUser(User user) {
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		CriteriaQuery<User> query = criteriaBuilder.createQuery(User.class);
		Root<User> from = query.from(User.class);
		query = query.where(criteriaBuilder.equal(from.get("telegramId"), user.getTelegramId()));
		List<User> resultList = entityManager.createQuery(query).getResultList();
		Filter userFilter = user.getUserFilter();
		if (resultList.size() == 0) {
			if (userFilter != null) {
				userFilter.setFilterType(FilterType.USER);
				// entityManager.persist(userFilter);
			} else {
				Filter filter = new Filter();
				filter.setFilterType(FilterType.USER);
				entityManager.persist(filter);
				user.setUserFilter(filter);
			}
			entityManager.persist(user);
		} else {
			User userFromDB = resultList.get(0);
			userFromDB.setAdmin(user.isAdmin());
			// userFromDB.setUserFilter(userFilter);
			userFromDB.setFilters(user.getFilters());
			userFromDB.setGroups(user.getGroups());
			userFromDB.setIngameName(user.getIngameName());
			userFromDB.setName(user.getName());
			userFromDB.setPayed(user.isPayed());
			userFromDB.setSuperadmin(user.isSuperadmin());
			userFromDB.setShowPokemonMessages(user.isShowPokemonMessages());
			userFromDB.setShowRaidMessages(user.isShowRaidMessages());
			userFromDB.setTelegramId(user.getTelegramId());
			if (user.getChatId() != null && !user.getChatId().isEmpty()) {
				userFromDB.setChatId(user.getChatId());
			}
			userFromDB.setTelegramName(user.getTelegramName());
			userFromDB.setTelegramActive(user.isTelegramActive());
			userFromDB.setTrainerLevel(user.getTrainerLevel());
			if (userFilter != null) {
				userFilter.setFilterType(FilterType.USER);
				userFilter.setOwner(userFromDB);
				userFromDB.setUserFilter(userFilter);
				entityManager.merge(userFilter);
			} else {
				Filter filter = new Filter();
				filter.setFilterType(FilterType.USER);
				filter.setOwner(userFromDB);
				entityManager.persist(filter);
				userFromDB.setUserFilter(filter);
				filter = entityManager.merge(filter);
			}
			entityManager.merge(userFromDB);
		}
		entityManager.flush();
		return user;
	}

	@Override
	public User getOrCreateUser(String telegramId) {
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		CriteriaQuery<User> query = criteriaBuilder.createQuery(User.class);
		Root<User> from = query.from(User.class);
		query = query.where(criteriaBuilder.equal(from.get("telegramId"), telegramId));
		List<User> resultList = entityManager.createQuery(query).getResultList();
		User user;
		if (resultList.size() == 0) {
			user = createUser(null, telegramId);
		} else {
			user = resultList.get(0);
		}
		return user;
	}

}
