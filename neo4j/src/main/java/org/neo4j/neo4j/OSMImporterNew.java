package org.neo4j.neo4j;

import java.io.FileNotFoundException;
import java.io.FileReader;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

//import org.neo4j.examples.EmbeddedNeo4jWithIndexing.RelTypes;
import org.neo4j.graphdb.GraphDatabaseService;
//import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
//import org.neo4j.graphdb.index.Index;
//import org.neo4j.neo4j.OSMway;

public class OSMImporterNew 
{
	//Neo4j variables
	private static final String osmImport_DB = "target/osmImport-db";
	private static GraphDatabaseService graphDb;
	private static String osmXmlFilePath = "C:\\Users\\Carol\\Desktop\\GSoC\\osm\\liechtenstein.osm";
    //private static Index<Node> nodeIndex;//will I need to create my own index? Or is this done automatically?
    
	
	
	//Elements and attributes to be read from the XML file
	static final String ND = "nd";
	static final String TAG = "tag";
	static final String K = "k";
	static final String V = "v";

	
	/**
	 * @param args
	 */
	public enum RelTypes implements RelationshipType
	{
	        OSM,
			OSM_WAY,
	        OSM_NODE,
	        OSM_NODENEXT
	}
	
	public static void main( final String[] args ) throws FileNotFoundException
    {
        // START SNIPPET: startDb
        graphDb = new GraphDatabaseFactory().newEmbeddedDatabase( osmImport_DB );
        //nodeIndex = graphDb.index().forNodes( "nodes" ); //what is stored in this variable?
        registerShutdownHook();
        XMLInputFactory factory = XMLInputFactory.newInstance();
        // END SNIPPET: startDb

        // START SNIPPET: addUsers
        Transaction tx = graphDb.beginTx();
      		
      		try 
      		{
      			
      			XMLStreamReader streamReader = factory.createXMLStreamReader(
      				    new FileReader(osmXmlFilePath));
      			
      			// Create sub reference node
                Node referenceNode = graphDb.createNode();
                graphDb.getReferenceNode().createRelationshipTo(
                    referenceNode, RelTypes.OSM );
      			
                Node importNode = graphDb.createNode();
            	referenceNode.createRelationshipTo(referenceNode, RelTypes.OSM);
                importNode.setProperty("name", "filename+currentdate");
                
      			while(streamReader.hasNext())
      			{
      				streamReader.next();
      				
      				if(streamReader.getEventType() == XMLStreamReader.START_ELEMENT)
      				{
      					if(streamReader.getLocalName() == "way")
      					{
      						
      						Node wayNode = graphDb.createNode();
      						//connect new way node with its property/attributes to the import node
      						wayNode.createRelationshipTo(importNode, RelTypes.OSM_WAY);
      						
      						//parse through all "way" tags and create a node for each with its properties
      						int count = streamReader.getAttributeCount();
      						for(int i = 0; i < count; i++)
      						{
     			                
      			                //set new Way node's properties
      			                wayNode.setProperty("name", streamReader.getAttributeName(i).toString());
      			                wayNode.setProperty("value", streamReader.getAttributeValue(i).toString());
      					
      						}
      						
      					
      					
      					}//end if(getLocalName == "way")
      				}//end if(getEventType)
      			}//end while
      		    
      		  tx.success();
      		} 
      		
      		catch (XMLStreamException e) 
      		{
      		    e.printStackTrace();
      		}
      		
      		 finally
             {
                 tx.finish();
             }
             System.out.println( "Shutting down database ..." );
             shutdown();
      		
      		
    }//end main
	
	
	/*
	//what data or properties should be within 
	private static Node createAndIndexWayNode( final String ??? )
	{
	    Node node = graphDb.createNode();
	    node.setProperty( USERNAME_KEY, username );
	    nodeIndex.add( node, USERNAME_KEY, username );
	    return node;
	}
	*/
	
	
	private static void registerShutdownHook()
	{
	        // Registers a shutdown hook for the Neo4j and index service instances
	        // so that it shuts down nicely when the VM exits (even if you
	        // "Ctrl-C" the running example before it's completed)
	      Runtime.getRuntime().addShutdownHook( new Thread()
	      {
	          @Override
	          public void run()
	          {
	              shutdown();
	          }
	       } );
	}//end registerShutdownHook()
	
    private static void shutdown()
    {
        graphDb.shutdown();
    }
	

}//end OSMImporterNew
