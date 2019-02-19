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

package pogorobot.entities;

import javax.persistence.Entity;
import javax.persistence.Table;

import org.springframework.data.jpa.domain.AbstractPersistable;

@Entity
@Table(name = "\"Quest\"")
public class Quest extends AbstractPersistable<Long> {

	String pokestopId;
	String pokestopName;
	String template;
	String pokestopUrl;
	String conditions; // ": [ {"type":7,"info":{"raid_levels":[1,2,3,4,5]} },{"type":6}],
	Double latitude;
	Double longitude;
	String type;
	String target;
	String rewards; // ":
					// [{"type":7,"info":{"shiny":false,"costume_id":0,"pokemon_id":25,"form_id":0,"gender_id":0}
					// }],
	Long updated;

	public final String getTemplate() {
		return template;
	}

	public final void setTemplate(String template) {
		this.template = template;
	}

	public final String getPokestopId() {
		return pokestopId;
	}

	public final void setPokestopId(String pokestopId) {
		this.pokestopId = pokestopId;
	}

	public final String getPokestopName() {
		return pokestopName;
	}

	public final void setPokestopName(String pokestopName) {
		this.pokestopName = pokestopName;
	}

	public final String getPokestopUrl() {
		return pokestopUrl;
	}

	public final void setPokestopUrl(String pokestopUrl) {
		this.pokestopUrl = pokestopUrl;
	}

	public final String getConditions() {
		return conditions;
	}

	public final void setConditions(String conditions) {
		this.conditions = conditions;
	}

	public final Double getLatitude() {
		return latitude;
	}

	public final void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	public final Double getLongitude() {
		return longitude;
	}

	public final void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	public final String getType() {
		return type;
	}

	public final void setType(String type) {
		this.type = type;
	}

	public final String getTarget() {
		return target;
	}

	public final void setTarget(String target) {
		this.target = target;
	}

	public final String getRewards() {
		return rewards;
	}

	public final void setRewards(String rewards) {
		this.rewards = rewards;
	}

	public final Long getUpdated() {
		return updated;
	}

	public final void setUpdated(Long updated) {
		this.updated = updated;
	}
	
}