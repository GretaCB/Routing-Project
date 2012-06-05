package org.neo4j.neo4j;

import java.io.FileNotFoundException;
import java.io.FileReader;
//import java.io.Reader;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public class StAXParseAttempt {

	/**
	 * @param args
	 */
	private static String osmXmlFilePath = "C:\\Users\\Carol\\Desktop\\GSoC\\osm\\liechtenstein.osm";
	
	public static void main(String[] args) throws FileNotFoundException 
	{
		XMLInputFactory factory = XMLInputFactory.newInstance();

		//get Reader connected to XML input from somewhere..
		//Reader reader = getXmlReader();
		//????? What is it reading? Where is this method?
		//Tutorial uses the following code as example...
		//but not sure of the purpose of the URL or InputStream...?
		/*
		 * 
		URL u = new URL("http://www.cafeconleche.org/");
		InputStream in = u.openStream();
		XMLInputFactory factory = XMLInputFactory.newInstance();
		XMLStreamReader parser = factory.createXMLStreamReader(in);
		 * 
		 */
		
		try 
		{

			XMLStreamReader streamReader = factory.createXMLStreamReader(
				    new FileReader(osmXmlFilePath));

			while(streamReader.hasNext())
			{
				streamReader.next();
				if(streamReader.getEventType() == XMLStreamReader.START_ELEMENT)
				{
					System.out.println(streamReader.getLocalName());
				}//end if
			}//end while
		    
		} 
		
		catch (XMLStreamException e) 
		{
		    e.printStackTrace();
		}
		
		
		
		
	}//end main

}//end StAZParseAttempt class
