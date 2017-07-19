package com.lorepo.icf.xml;

public interface XMLElement {
	void initializeElement(String name);
	void initializeCDATASection(String value);
	void appendChild(XMLElement element);
	void setBooleanAttribute(String name, boolean value);
	void setIntegerAttribute(String name, int value);
	void setAttribute(String name, String value);
	String toString();
}
