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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.springframework.stereotype.Component;

@Component
public class RaidImageScanner {

	public RaidImageScanner() {
	}

	public void scanImage(String url) {
		String filename = url.substring(url.lastIndexOf("/") + 1).toLowerCase();
		URL realUrl = null;

		try {
			realUrl = new URL(url);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			if (realUrl == null) {
				System.out.println("WARN: Couldn't open " + url);
			} else {
				try (ReadableByteChannel rbc = Channels.newChannel(realUrl.openStream())) {
					// FileOutputStream fos;
					String workingDir = System.getProperty("user.dir") + "/";
					try (FileOutputStream fos = new FileOutputStream(workingDir + filename)) {
						fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
					}
					File src = new File(workingDir + filename);
					// TODO: refactor for config
					String destPathname = workingDir + "/screenshots/";
					File destFile = new File(destPathname);
					if (!destFile.exists()) {
						destFile.mkdirs();
					}
					File dest = new File(destPathname + filename);
					Path srcPath = src.toPath();
					Path destPath = dest.toPath();
					Files.move(srcPath, destPath, StandardCopyOption.REPLACE_EXISTING);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// BytePointer outText;
		// System.setProperty("TESSDATA_PREFIX", "tessdata");
		// TessBaseAPI api = new TessBaseAPI();
		// // Initialize tesseract-ocr with German, without specifying tessdata path
		// if (api.Init("./tessdata", "ENG") != 0) {
		// System.err.println("Could not initialize tesseract.");
		// System.exit(1);
		// }
		//
		// // Open input image with leptonica library
		// PIX image = lept.pixRead("temp.jpg");
		// api.SetImage(image);
		// // Get OCR result
		// outText = api.GetUTF8Text();
		// String string = outText.getString();
		// if (!string.isEmpty())
		// System.out.println("OCR output:\n" + string);
		//
		// // Destroy used object and release memory
		// api.End();
		// outText.deallocate();
		// lept.pixDestroy(image);
		// api.close();
	}

	private void cropImages() {
		// crop_w = round(0,2086 * width)
		// ****crop_h = round(0,1920 * height)
		// ***crop_y1 = round(0,38496 * width)
		// crop_y2 = round(0,6078 * height)
		// zeilenhoehe_comp_x = round(0,14609 * height)
		// comp3_x = round(0,10709 * width)
		// comp2_x = comp3_x + zeilenhoehe_comp_x
		// comp1_x = comp2_x + zeilenhoehe_comp_x
		//
		// crop_x1 = round(width / 355)
		// crop1_x1 = comp1_x - crop_x1
		// crop2_x1 = comp2_x - crop_x1
		// crop3_x1 = comp3_x - crop_x1
		//
		//
		// crop_diff = 362 round(0,29107 * width)
		// crop2_x2 = crop2_x1 + crop_diff
		// crop3_x2 = crop3_x1 + crop_diff
		// crop3_x3 = crop3_x2 + crop_diff
		//
		// comp_y = round(0,4426 * height)
	}

}
