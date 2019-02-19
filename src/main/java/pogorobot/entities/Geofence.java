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

import java.util.List;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.OrderColumn;
import javax.persistence.Table;

import org.springframework.data.jpa.domain.AbstractPersistable;

@Entity
@Table(name = "\"Geofence\"")
public class Geofence extends AbstractPersistable<Long> {

	private static final long serialVersionUID = -3743355376417698226L;

	@OrderColumn
	@ElementCollection
	private List<Double> polygon;

	// @Column(unique = true, nullable = false)
	private String geofenceName;

	public Geofence() {
		this.setId(null);
	}

	public Geofence(Long id) {
		this.setId(id);
		// this.setGeofenceName(geofenceName);
	}

	public String getGeofenceName() {
		return geofenceName;
	}

	public void setGeofenceName(String geofenceName) {
		this.geofenceName = geofenceName;
	}

	public List<Double> getPolygon() {
		return polygon;
	}

	public void setPolygon(List<Double> polygon) {
		this.polygon = polygon;
	}


}