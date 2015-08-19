package com.yuriyapps.imapreader.controller;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class SAXPars extends DefaultHandler
{
	StringBuilder data = new StringBuilder();
	@Override
	public void startDocument() throws SAXException
	{
		System.out.println("Start parse XML...");
	}
	
	@Override
	public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException
	{
		
	}
	
	@Override
	public void characters(char[] ch, int start, int length) throws SAXException
	{
		data.append(new String(ch));
	}
	
	@Override 
	public void endElement(String namespaceURI, String localName, String qName) throws SAXException
	{
		
	}
	
	@Override 
	public void endDocument()
	{
		System.out.println("Stop parse XML...");
	}
	
	public String getData()
	{
		return data.toString();
	}
}