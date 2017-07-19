package com.lorepo.icf.xml;

import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.XMLParser;

public class XMLClientElement implements XMLElement {
	private Document doc = null;
	private Element element = null;
	
	public XMLClientElement() {
		doc = XMLParser.createDocument();
	}
	
	@Override
	public void initializeElement(String name) {
		element = doc.createElement(name);
	}
	
	@Override
	public void initializeCDATASection(String value) {
		element = (Element) doc.createCDATASection(value);
	}

	@Override
	public void appendChild(XMLElement element) {
		XMLClientElement xmlClient = (XMLClientElement) element;
		this.element.appendChild(xmlClient.getElement());
	}

	private Node getElement() {
		return this.element;
	}
	
	

	@Override
	public void setBooleanAttribute(String name, boolean value) {
		this.element.setAttribute(name, Boolean.toString(value));
	}

	@Override
	public void setIntegerAttribute(String name, int value) {
		this.element.setAttribute(name, Integer.toString(value));
	}
	
	public String toString() {
		return this.element.toString();
	}

	@Override
	public void setAttribute(String name, String value) {
		this.element.setAttribute(name, value);
	}
}
