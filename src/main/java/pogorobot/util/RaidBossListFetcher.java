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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.gargoylesoftware.htmlunit.BrowserVersion;

public class RaidBossListFetcher {

	public static void main(String[] args) {
		RaidBossListFetcher rblf = new RaidBossListFetcher();
		rblf.createXmlFile();
		List<String> parseXmlFile = rblf.parseXmlFile();
		for (String string : parseXmlFile) {
			System.out.println(string);
		}
	}

	public void createXmlFile() {
		String url = "https://pokemongo.gamepress.gg/raid-boss-list";

		WebDriver webDriver = openUrl(url);

		StringBuilder sb = new StringBuilder("<all>\n");

		WebElement raidBossTable = webDriver.findElement(By.id("raid-boss-table"));
		raidBossTable.findElements(By.tagName("tr")).stream().filter(tr -> tr.isDisplayed()).forEach(tr -> {
			System.out.println(tr);
			tr.findElements(By.tagName("td")).stream().forEach(td -> {
				System.out.println(td);
				// Is it the level?
				if (td.getTagName().equals("div") && td.getAttribute("class").equals("raid-tier-stars")) {
					sb.append(parseLevel(td));
				} else
				// or is it the pokemon?
				if (td.getTagName().equals("a") && td.getAttribute("hreflang") != null) {
					sb.append(parsePokemon(td));
				}

			});
			sb.append("  </row>\n");
		});
		sb.append("</all>");
		webDriver.close();
		try {
			writeXml(sb.toString());
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Error while parsing raidbosslist.");
		}
	}

	private WebDriver openUrl(String url) {
		HtmlUnitDriver driver = new HtmlUnitDriver(BrowserVersion.BEST_SUPPORTED, true);
		driver.navigate().to(url);
		return driver;
	}

	public List<String> parseXmlFile() {
		List<String> result = new ArrayList<>();

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder builder;
		Document doc = null;
		try {
			builder = factory.newDocumentBuilder();
			doc = builder.parse("./raidbosslist.xml");

			// Create XPathFactory object
			XPathFactory xpathFactory = XPathFactory.newInstance();

			// Create XPath object
			XPath xpath = xpathFactory.newXPath();

			try {
				// create XPathExpression object
				XPathExpression expr = xpath.compile("/all/row");
				// evaluate expression result on XML document
				NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
				for (int i = 0; i < nodes.getLength(); i++) {
					Node item = nodes.item(i);
					NodeList childNodes = item.getChildNodes();
					String pokemonAndLevel = "";
					for (int j = 0; j < childNodes.getLength(); j++) {
						Node firstChild = childNodes.item(j).getFirstChild();
						if (firstChild != null) {
							pokemonAndLevel += firstChild.getNodeValue() + " ";
						}
					}
					result.add(pokemonAndLevel.trim());
				}
			} catch (XPathExpressionException e) {
				e.printStackTrace();
			}
		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	private String parseLevel(WebElement tdElementChild) {
		String level = "   <level>" + tdElementChild.getText().trim() + "</level>\n";
		return level;
	}

	private String parsePokemon(WebElement tdElementChild) {
		String nodeValue = tdElementChild.getAttribute("href");
		nodeValue = "   <pokemon>" + nodeValue.substring(9, nodeValue.length()) + "</pokemon>\n";
		return nodeValue;
	}

	private void writeXml(String inputLine) throws IOException {
		// save to this filename
		String fileName = "./raidbosslist.xml";
		File file = new File(fileName);

		if (!file.exists()) {
			file.createNewFile();
		} else {
			file.delete();
			file.createNewFile();
		}

		// use FileWriter to write file
		FileWriter fw = new FileWriter(file.getAbsoluteFile());
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write(inputLine);
		bw.close();
	}

}
