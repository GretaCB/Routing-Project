package org.neo4j.neo4j;

import java.io.FileNotFoundException;
import java.io.FileReader;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.neo4j.neo4j.OSMImporterNew;

public class OSMNodeParse extends OSMImporterNew
{

	private static String osmXmlFilePath = "C:\\Users\\Carol\\Desktop\\GSoC\\osm\\liechtenstein.osm";
	
	/**
	 * @param args
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException 
	{
		
		 XMLInputFactory factory = XMLInputFactory.newInstance();
		 
		 try 
   		 {
   			
   			XMLStreamReader streamReader = factory.createXMLStreamReader(
   				    new FileReader(osmXmlFilePath));
   			while(streamReader.hasNext())
  			{
  				streamReader.next();
  				
  				if(streamReader.getEventType() == XMLStreamReader.START_ELEMENT)
  				{
  				
  					
  					if(streamReader.getLocalName() == "node")
  		   			{
  				        	//Check whether or not the specific node was already created by checking for its id within index
  			              	if(idPresent(streamReader.getAttributeValue(0).toString()))
  			             	    createAndIndexNode(streamReader.getAttributeValue(0).toString());
  			              	
  		   			}
  			        
  			        
  			        //Do I need to create a node for "tag" elements?
  			        //I think I do because they contain relevant routing information
  			        if(streamReader.getLocalName()== "tag")
  			        {
  			            //create tag nodes and set properties...key and value
  			            //Create a separate class to check for relevant tag values for routing
  			        }//end if(streamReader.getLocalNale() == "tag"
  			           
  			        
  			       

  				}//end if(streamReader.getEventType)
  			}//end while
  		}//end try
  		
  		catch (XMLStreamException e) 
  		{
  		    e.printStackTrace();
  		}
  		

	}//end main

}//end OSMNodeParse
