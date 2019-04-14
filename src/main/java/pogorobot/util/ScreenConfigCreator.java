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

package pogorobot.util;

import java.util.Date;

public class ScreenConfigCreator {

	public static void main(String[] args) {
		ScreenConfigCreator screenConfigCreator = new ScreenConfigCreator();
		// Julian:
		System.out.println(new Date(1537805700L * 1000L).toString());
		System.out.println(new Date(1537811100L * 1000L).toString());
		// System.out.println(screenConfigCreator.calculateConfig(622, 1280));
		// System.out.println("");
		// System.out.println(screenConfigCreator.calculateConfig(720, 1280));
	}

	private String calculateConfig(Integer width, Integer height) {
		String result = "";
		Long crop_w = Math.round(0.2086 * width);
		Long crop_h_attention = Math.round(0.1920 * height);
		Long crop_y1_attention = Math.round(0.38496 * width);
		Long crop_y2 = Math.round(0.6078 * height);
		Long zeilenhoehe_comp_x = Math.round(0.14609 * height);
		Long comp3_x = Math.round(0.10709 * width);
		Long comp2_x = comp3_x + zeilenhoehe_comp_x;
		Long comp1_x = comp2_x + zeilenhoehe_comp_x;

		long crop_x1 = Math.round(width / 355d);
		Long crop1_x1 = comp1_x - crop_x1;
		Long crop2_x1 = comp2_x - crop_x1;
		Long crop3_x1 = comp3_x - crop_x1;

		Long crop_diff = Math.round(0.29107 * width);
		Long crop2_x2 = crop2_x1 + crop_diff;
		Long crop3_x2 = crop3_x1 + crop_diff;
		Long crop3_x3 = crop3_x2 + crop_diff;

		Long comp_y = Math.round(0.4426 * height);

		result = "{'width': " + width + ", 'height': " + height + ", 'crop_w': " + crop_w + ", 'crop_h': "
				+ crop_h_attention + ", 'crop_y1': " + crop_y1_attention + ", 'crop_y2': " + crop_y2 + ",\n"
				+ "'comp3_x': " + comp3_x + ", 'comp3_y': " + comp_y + ", 'crop3_x1': " + crop3_x1 + ", 'crop3_x2': "
				+ crop3_x2 + ", 'crop3_x3': " + crop3_x3 + ",\n" + " 'comp2_x': " + comp2_x + ", 'comp2_y': " + comp_y
				+ ", 'crop2_x1': " + crop2_x1 + ", 'crop2_x2': " + crop2_x2 + ",\n" + " 'comp1_x': " + comp1_x
				+ ", 'comp1_y': " + comp_y + ", 'crop1_x1': " + crop1_x1 + "},  # Julian";

		return result;
	}
}
