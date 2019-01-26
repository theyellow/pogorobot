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
import java.net.MalformedURLException;
import java.net.URL;
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

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.ElementNotFoundException;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.InteractivePage;
import com.gargoylesoftware.htmlunit.ScriptException;
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebWindow;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.javascript.JavaScriptErrorListener;

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
		WebClient webClient = new WebClient(BrowserVersion.BEST_SUPPORTED);
		webClient.getOptions().setUseInsecureSSL(true); // ignore ssl certificate
		webClient.getOptions().setThrowExceptionOnScriptError(false);
		webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
		webClient.setCssErrorHandler(new SilentCssErrorHandler());
		webClient.setJavaScriptErrorListener(new JavaScriptErrorListener() {
			@Override
			public void scriptException(InteractivePage page, ScriptException scriptException) {
			}

			@Override
			public void timeoutError(InteractivePage page, long allowedTime, long executionTime) {
			}

			@Override
			public void malformedScriptURL(InteractivePage page, String url,
					MalformedURLException malformedURLException) {
			}

			@Override
			public void loadScriptError(InteractivePage page, URL scriptUrl, Exception exception) {
			}
		});
		String url = "https://pokemongo.gamepress.gg/raid-boss-list";
		HtmlPage myPage = null;
		try {
			myPage = webClient.getPage(url);
		} catch (FailingHttpStatusCodeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		webClient.waitForBackgroundJavaScriptStartingBefore(200);
		webClient.waitForBackgroundJavaScript(20000);

		final List<WebWindow> windows = webClient.getWebWindows();
		for (final WebWindow wd : windows) {
			wd.getJobManager().removeAllJobs();
		}

		webClient.close();

		System.gc();

		// do stuff on page ex: myPage.getElementById("main")
		// myPage.asXml() <- tags and elements
		StringBuilder sb = new StringBuilder("<all>\n");
		try {
			HtmlElement htmlElementById = myPage.getHtmlElementById("raid-boss-table");
			List<DomElement> list = new ArrayList<>();
			htmlElementById.getChildElements().forEach(x -> list.add(x));
			if (list.isEmpty()) {
				return;
			}
			list.stream().filter(x -> x.getAttribute("style").contains("display: none;") ? false : true)
					.forEach(trElement -> {
						sb.append("  <row>\n");
						// For each tableRow
						trElement.getChildElements().forEach(tdElement -> {
							if (tdElement.getChildren() != null) {
								// For each table column
								tdElement.getChildren().forEach(tdElementChild -> {
									// Is it the level?
									if (tdElementChild.getNodeName().equals("div") && tdElementChild.getAttributes()
											.getNamedItem("class").getNodeValue().equals("raid-tier-stars")) {
										sb.append(parseLevel(tdElementChild));
									} else
									// or is it the pokemon?
									if (tdElementChild.getNodeName().equals("a")
											&& tdElementChild.getAttributes().getNamedItem("hreflang") != null) {
										sb.append(parsePokemon(tdElementChild));
									}
								});
							}
						});
						sb.append("  </row>\n");
					});
			sb.append("</all>");
			writeXml(sb.toString());
		} catch (ElementNotFoundException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// webClient.close();
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
	private String parseLevel(DomNode tdElementChild) {
		String level = "   <level>" + tdElementChild.getTextContent().trim() + "</level>\n";
		return level;
	}

	private String parsePokemon(DomNode tdElementChild) {
		String nodeValue = tdElementChild.getAttributes().getNamedItem("href").getNodeValue();
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
