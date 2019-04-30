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
import java.util.concurrent.TimeUnit;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class RaidBossListFetcher {

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	public static void main(String[] args) {
		RaidBossListFetcher rblf = new RaidBossListFetcher();
		// rblf.createXmlFile();
		List<String> parseXmlFile = rblf.getBosses();
		for (String string : parseXmlFile) {
			System.out.println(string);
		}
	}

	private boolean waitForFetch = false;

	private void createXmlFile() {
		waitForFetch = true;
		java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(java.util.logging.Level.OFF);
		StringBuilder sb = new StringBuilder("<all>\n");
		String url = "https://pokemongo.gamepress.gg/raid-boss-list";
		Runnable r = () -> {
			WebClient webClient = new WebClient(BrowserVersion.BEST_SUPPORTED);
			webClient.getOptions().setThrowExceptionOnScriptError(false);
			try {
				final HtmlPage startPage = webClient.getPage(url);
				DomElement raidbosstable = startPage.getElementById("raid-boss-table");
				try {
					TimeUnit.SECONDS.sleep(12);
				} catch (InterruptedException e) {
					logger.warn("Got interrupted");
					Thread.currentThread().interrupt();
				}
				raidbosstable.getElementsByTagName("tr").stream().filter(row -> row.isDisplayed()).forEach(row -> {
					System.out.println(row);
					sb.append("  <row>\n");
					row.getElementsByTagName("td").stream().forEach(td -> {
						System.out.println(td);
						if (td.hasChildNodes()) {
							td.getChildElements().forEach(subelement -> {
								// Is it the pokemon?
								if (subelement.getTagName().equals("a") && subelement.hasAttribute("hreflang")) {
									sb.append(parsePokemon(subelement));
								} else
								// Or is it the level?
								if (subelement.getTagName().equals("div")
										&& subelement.getAttribute("class").equals("raid-tier-stars")) {
									sb.append(parseLevel(subelement));
								}
							});
						}
						// if (td.getTagName().equals("div") &&
						// td.getAttribute("class").equals("raid-tier-stars")) {
						// sb.append(parseLevel(td));
						// } else
						// if (td.getTagName().equals("a") && td.getAttribute("hreflang") != null) {
						// sb.append(parsePokemon(td));
						// }

					});
					sb.append("  </row>\n");
				});
			} catch (FailingHttpStatusCodeException | IOException e1) {
				logger.warn("Error while generating raidbosslist.xml", e1);
			}
			webClient.close();
			webClient = null;
			// WebDriver webDriver = openUrl(url);
			//
			// WebElement raidBossTable = webDriver.findElement(By.id("raid-boss-table"));
			// raidBossTable.findElements(By.tagName("tr")).stream().filter(tr ->
			// tr.isDisplayed()).forEach(tr -> {
			// System.out.println(tr);
			// tr.findElements(By.tagName("td")).stream().forEach(td -> {
			// System.out.println(td);
			// // Is it the level?
			// if (td.getTagName().equals("div") &&
			// td.getAttribute("class").equals("raid-tier-stars")) {
			// sb.append(parseLevel(td));
			// } else
			// // or is it the pokemon?
			// if (td.getTagName().equals("a") && td.getAttribute("hreflang") != null) {
			// sb.append(parsePokemon(td));
			// }
			//
			// });
			// sb.append(" </row>\n");
			// });
			// webDriver.close();
			sb.append("</all>");
			try {
				writeXml(sb.toString());
			} catch (IOException e) {
				logger.warn("Error while parsing raidbosslist.", e);
			}
			waitForFetch = false;
		};
		Thread xmCreator = new Thread(r);

		long startTimeMillis = System.currentTimeMillis();
		try {
			xmCreator.start();
			// wait 120 s for timeout
			xmCreator.join(1000 * 120L);
		} catch (InterruptedException e) {
			logger.warn("XmlCreator-thread got interrupted");
			Thread.currentThread().interrupt();
		}
		long currentTimeMillis = System.currentTimeMillis();
		long durationInMillis = currentTimeMillis - startTimeMillis;
		logger.warn("XmlCreator-thread finished after " + durationInMillis / 1000 + "." + (durationInMillis % 1000) / 10
				+ " s");
	}

	// private WebDriver openUrl(String url) {
	// HtmlUnitDriver driver = new HtmlUnitDriver(BrowserVersion.BEST_SUPPORTED,
	// true);
	// driver.navigate().to(url);
	// return driver;
	// }

	public List<String> parseXmlFile() {
		List<String> result = new ArrayList<>();

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		// disable external entities
		factory.setExpandEntityReferences(false);
		try {
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		} catch (ParserConfigurationException e1) {
			logger.warn("WARN: Setting FEATURE_SECURE_PROCESSING to true failed while parsing XML");
		}
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
				logger.error("Error while parsing", e);
			}
		} catch (ParserConfigurationException | SAXException | IOException e) {
			logger.warn("Another error while parsing", e);
		}
		return result;
	}

	private String parseLevel(DomElement subelement) {
		String level = "   <level>" + subelement.getTextContent().trim() + "</level>\n";
		return level;
	}

	private String parsePokemon(DomElement subelement) {
		String nodeValue = subelement.getAttribute("href");
		nodeValue = "   <pokemon>" + nodeValue.substring(9, nodeValue.length()) + "</pokemon>\n";
		return nodeValue;
	}

	private void writeXml(String inputLine) throws IOException {
		// save to this filename
		String fileName = "./raidbosslist.xml";
		File file = new File(fileName);

		if (!file.exists()) {
			boolean createNewFile = file.createNewFile();
			logger.info(createNewFile ? "New file created" : "Error, perhaps old file wasn't correctly deleted");
		} else {
			boolean delete = file.delete();
			if (!delete) {
				logger.warn("Error, can't delete file " + file.getAbsolutePath());
			}
			boolean createNewFile = file.createNewFile();
			logger.info(createNewFile ? "New file created"
					: "Error, new file could not be generated. Perhaps old file wasn't correctly deleted");
		}

		// use FileWriter to write file
		try (FileWriter fw = new FileWriter(file.getAbsoluteFile())) {
			try (BufferedWriter bw = new BufferedWriter(fw)) {
				bw.write(inputLine);
			}
		}
	}

	public List<String> getBosses() {
		createXmlFile();
		while (true) {
			if (waitForFetch) {
				try {
					TimeUnit.SECONDS.sleep(3);
				} catch (InterruptedException e) {
					logger.warn("Got interrupted");
					Thread.currentThread().interrupt();
				}
			} else {
				break;
			}
		}
		return parseXmlFile();
	}

}
