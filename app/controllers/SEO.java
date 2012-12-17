package controllers;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import logic.TalkerLogic;
import models.ConversationBean;
import models.DiseaseBean;
import models.TalkerBean;
import models.TopicBean;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import dao.DiseaseDAO;

import play.mvc.Controller;
import play.mvc.Router;
import play.mvc.Router.Route;

public class SEO extends Controller  {

	/*public static void sitemap() {
		String cancerType = session.get("cancerType");;
		if(StringUtils.isNotBlank(cancerType) && cancerType.equals("Breast Cancer")) {
			render("SEO/sitemap_bc.xml");			
		} else {
			render("SEO/sitemap.xml");	
		}
	}*/
	
	public static void sitemap(String name) {
		//String cancerType = session.get("cancerType");;
		//if(StringUtils.isNotBlank(cancerType) && cancerType.equals("Breast Cancer")) {
		//	render("SEO/sitemap_bc.xml");			
		//} else {
			if(StringUtils.isNotBlank(name)) {
				if("cancer".equals(name)) {
					Document xml = getCancerXml();
					renderXml(xml);
				} else if("talker".equals(name)) {
					Document xml = getTalkerXml();
					renderXml(xml);
				} else if("topic".equals(name)) {
					Document xml = getTopicXml();
					renderXml(xml);
				} else if("convo".equals(name)) {
					Document xml = getConvoXml();
					renderXml(xml);
				} else if("static".equals(name))  {
					render("SEO/sitemapstatic.xml");
				} else {
					render("SEO/sitemap.xml");
				}
			} else {
				render("SEO/sitemap.xml");
			}
		//}
	}

	private static Document getCancerXml() {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder;
		Document xml = null;
		try {
			docBuilder = docFactory.newDocumentBuilder();
			xml = docBuilder.newDocument();

			Element rootElement = xml.createElement("urlset");
			rootElement.setAttribute("xmlns", "http://www.sitemaps.org/schemas/sitemap/0.9");
			rootElement.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
			rootElement.setAttribute("xsi:schemaLocation", "http://www.sitemaps.org/schemas/sitemap/0.9\nhttp://www.sitemaps.org/schemas/sitemap/0.9/sitemap.xsd");
			Element urlElement,locElement,freqElement,priorityElement;

			List<DiseaseBean> diseaseList = DiseaseDAO.getCatchedDiseasesList(session);
			int it = 0;
			for (DiseaseBean diseaseBean : diseaseList) {

				urlElement = xml.createElement("url");

				locElement = xml.createElement("loc");
				locElement.appendChild(xml.createTextNode("http://talkabouthealth.com/explore/" + diseaseBean.getName().toLowerCase().replaceAll(" ", "_")));
				urlElement.appendChild(locElement);

				freqElement = xml.createElement("changefreq");
				freqElement.appendChild(xml.createTextNode("weekly"));
				urlElement.appendChild(freqElement);

				priorityElement = xml.createElement("priority");
				priorityElement.appendChild(xml.createTextNode("0.80"));
				urlElement.appendChild(priorityElement);

				rootElement.appendChild(urlElement);
				it++;
			}
			xml.appendChild(rootElement);
			System.out.println("Cancer URL count: " + it);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		return xml;
	}

	private static Document getTalkerXml() {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder;
		Document xml = null;
		Element urlElement,locElement,freqElement; //,priorityElement;
		try {
			docBuilder = docFactory.newDocumentBuilder();
			xml = docBuilder.newDocument();
			
			Element rootElement = xml.createElement("urlset");
			rootElement.setAttribute("xmlns", "http://www.sitemaps.org/schemas/sitemap/0.9");
			rootElement.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
			rootElement.setAttribute("xsi:schemaLocation", "http://www.sitemaps.org/schemas/sitemap/0.9\nhttp://www.sitemaps.org/schemas/sitemap/0.9/sitemap.xsd");
			
			List<TalkerBean> talkerList = TalkerLogic.loadAllTalkersFromCache();
			int it = 0;
			for (TalkerBean talkerBean : talkerList) {
				if(!talkerBean.isSuspended() && !talkerBean.isAdmin() && !talkerBean.isDeactivated()) {

					urlElement = xml.createElement("url");
	
					locElement = xml.createElement("loc");
					locElement.appendChild(xml.createTextNode("http://talkabouthealth.com/" + talkerBean.getUserName()));
					urlElement.appendChild(locElement);

					//Made update daily so that search engines can search sitemap on daily basis
					freqElement = xml.createElement("changefreq");
					freqElement.appendChild(xml.createTextNode("daily"));
					urlElement.appendChild(freqElement);

					//Removing priority to as it might be harm ranking
					//priorityElement = xml.createElement("priority");
					//priorityElement.appendChild(xml.createTextNode("0.80"));
					//urlElement.appendChild(priorityElement);

					rootElement.appendChild(urlElement);
					it++;
				}
			}
			System.out.println("Talker URL Count: " + it);
			xml.appendChild(rootElement);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		return xml;
	}
	
	private static Document getTopicXml() {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder;
		Document xml = null;
		Element urlElement,locElement,freqElement,priorityElement;
		try {
			docBuilder = docFactory.newDocumentBuilder();
			xml = docBuilder.newDocument();
			
			Element rootElement = xml.createElement("urlset");
			rootElement.setAttribute("xmlns", "http://www.sitemaps.org/schemas/sitemap/0.9");
			rootElement.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
			rootElement.setAttribute("xsi:schemaLocation", "http://www.sitemaps.org/schemas/sitemap/0.9\nhttp://www.sitemaps.org/schemas/sitemap/0.9/sitemap.xsd");
			
			Set<TopicBean> topicList = TalkerLogic.loadAllTopicsFromCache();
			int it = 0;
			for (TopicBean topicBean : topicList) {
				if(!topicBean.isDeleted()) {

					urlElement = xml.createElement("url");
	
					locElement = xml.createElement("loc");
					locElement.appendChild(xml.createTextNode("http://talkabouthealth.com/" + topicBean.getMainURL()));
					urlElement.appendChild(locElement);

					freqElement = xml.createElement("changefreq");
					freqElement.appendChild(xml.createTextNode("weekly"));
					urlElement.appendChild(freqElement);

					priorityElement = xml.createElement("priority");
					priorityElement.appendChild(xml.createTextNode("0.80"));
					urlElement.appendChild(priorityElement);

					rootElement.appendChild(urlElement);
					it++;
				}
			}
			System.out.println("Topic URL Count: " + it);
			xml.appendChild(rootElement);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		return xml;
	}
	
	private static Document getConvoXml() {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder;
		Document xml = null;
		Element urlElement,locElement,freqElement; //,priorityElement;
		try {
			docBuilder = docFactory.newDocumentBuilder();
			xml = docBuilder.newDocument();
			int it = 0;
			Element rootElement = xml.createElement("urlset");
			rootElement.setAttribute("xmlns", "http://www.sitemaps.org/schemas/sitemap/0.9");
			rootElement.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
			rootElement.setAttribute("xsi:schemaLocation", "http://www.sitemaps.org/schemas/sitemap/0.9\nhttp://www.sitemaps.org/schemas/sitemap/0.9/sitemap.xsd");
			
			List<ConversationBean> convoList = TalkerLogic.loadAllConversationsFromCache();
			
			for (ConversationBean convoBean : convoList) {
				if(!convoBean.isDeleted() && !convoBean.isRemovedByadmin()) {

					urlElement = xml.createElement("url");
	
					locElement = xml.createElement("loc");
					locElement.appendChild(xml.createTextNode("http://talkabouthealth.com/" + convoBean.getMainURL()));
					urlElement.appendChild(locElement);

					freqElement = xml.createElement("changefreq");
					freqElement.appendChild(xml.createTextNode("daily")); // daily/weekly
					urlElement.appendChild(freqElement);

					//priorityElement = xml.createElement("priority");
					//priorityElement.appendChild(xml.createTextNode("0.80"));
					//urlElement.appendChild(priorityElement);

					rootElement.appendChild(urlElement);
					it++;
				}
			}
			System.out.println("Convo URL Count : " + it);
			xml.appendChild(rootElement);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		return xml;
	}
}