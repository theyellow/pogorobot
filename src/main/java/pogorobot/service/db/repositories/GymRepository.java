/**
 Copyright 2021 Benjamin Marstaller
 
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

package pogorobot.service.db.repositories;

import java.io.Serializable;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import pogorobot.entities.Gym;

public interface GymRepository extends CrudRepository<Gym, Serializable> {

	@Query(value="select g.name from Gym g where g.gymId = :gymId")
	public String findNameByGymId(@Param("gymId") String gymId);

	@Query(value="select g from Gym g where g.gymId = :gymId")
	public Gym findByGymId(@Param("gymId") String gymId);
}