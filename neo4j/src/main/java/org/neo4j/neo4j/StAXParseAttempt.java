package org.neo4j.neo4j;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
//import java.io.FileReader;
//import java.io.Reader;
import java.io.InputStream;
//import java.util.ArrayList;
import java.util.Iterator;
//import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
//import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.Attribute;
//import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.neo4j.OSMway;

public class StAXParseAttempt {

	/**
	 * @param args
	 */
	public enum RelTypes implements RelationshipType
	{
	        OSM_WAY,
	        OSM_NODE,
	        OSM_NODENEXT
	}
	
	private static final String osmImport_DB = "target/osmImport-db";
	private GraphDatabaseService graphDb;
	private static String osmXmlFilePath = "C:\\Users\\Carol\\Desktop\\GSoC\\osm\\liechtenstein.osm";
	
	/*
	public static void main(String[] args) throws FileNotFoundException, XMLStreamException 
	{
		
	    // START SNIPPET: startDb
	    graphDb = new GraphDatabaseFactory().newEmbeddedDatabase( DB_PATH );
	    registerShutdownHook( graphDb );
	    // END SNIPPET: startDb
		
		try
		{
			XMLInputFactory factory = XMLInputFactory.newInstance();
			InputStream in = new FileInputStream(osmXmlFilePath);
			XMLEventReader eventReader = factory.createXMLEventReader(in);
		
			while (eventReader.hasNext()) 
			{
				XMLEvent event = eventReader.nextEvent();
				if (event.isStartElement()) 
				{
					StartElement startElement = event.asStartElement();
					System.out.println("at StartElement");
					
					if (startElement.getName().getLocalPart().equals("way"))
					{
						System.out.println("found new node");
						@SuppressWarnings("unchecked")
						Iterator<Attribute> attributes = startElement.getAttributes();
						while (attributes.hasNext()) 
						{
							Attribute attribute = attributes.next();
							
							System.out.println("created Iterator...");
							
							if (attribute.getName().equals("nd")) 
							{
								System.out.println("TAG you're it!");
								
								int count = eventReader.getAttributeCount();
								for(int i = 0; i < count; i++)
								{
									//System.out.println("Attribute Value: " + eventReader.getAttributeValue(i));
									//System.out.println("Attribute Name: " + eventReader.getAttributeName(i));
					                //System.out.println("Attribute Namespace: " + eventReader.getAttributeNamespace(i));
					                //System.out.println("Attribute Type: " + eventReader.getAttributeType(i));
					                //System.out.println("Attribute Prefix: " + eventReader.getAttributePrefix(i));
								}
								
								//System.out.println("Tag: " + attribute.getValue());
							}//end if(attribute.getName()...)
						
						}//end while(attributes.hasNext())
						
					}//end if(startElement.getName()...)
					
				}//end if(event.isStartElement())
			}//end while(eventReader.hasNext)
				
				
		}//end try
		
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		} 
		
		catch (XMLStreamException e) 
		{
			e.printStackTrace();
		}
		
		
		
		
		
		
		//get Reader connected to XML input from somewhere..
		//Reader reader = getXmlReader();
		//????? What is it reading? Where is this method?
		//Tutorial uses the following code as example...
		//but not sure of the purpose of the URL or InputStream...?
		/*
		 * 
		URL u = new URL("http://www.cafeconleche.org/");
		InputStream in = u.openStream();
		XMLStreamReader parser = factory.createXMLStreamReader(in);
		 * 
		 */
		/*
		try 
		{

			XMLStreamReader streamReader = factory.createXMLStreamReader(
				    new FileReader(osmXmlFilePath));

			while(streamReader.hasNext())
			{
				streamReader.next();
				
				if(streamReader.getEventType() == XMLStreamReader.START_ELEMENT)
				{
					if(streamReader.getLocalName() == "tag")
					{
						int count = streamReader.getAttributeCount();
						for(int i = 0; i < count; i++)
						{
							System.out.println("Attribute Value: " + streamReader.getAttributeValue(i));
							System.out.println("Attribute Name: " + streamReader.getAttributeName(i));
			                System.out.println("Attribute Namespace: " + streamReader.getAttributeNamespace(i));
			                System.out.println("Attribute Type: " + streamReader.getAttributeType(i));
			                System.out.println("Attribute Prefix: " + streamReader.getAttributePrefix(i));
						}
					}//end if(getLocalName == "tag")
				}//end if(getEventType)
			}//end while
		    
		} 
		
		catch (XMLStreamException e) 
		{
		    e.printStackTrace();
		}
		
		
		
		
	}//end main
 */
}//end StAXParseAttempt class
