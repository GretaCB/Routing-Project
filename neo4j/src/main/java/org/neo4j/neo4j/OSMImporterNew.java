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
import org.neo4j.graphdb.index.Index;
//import org.neo4j.graphdb.index.Index;
//import org.neo4j.neo4j.OSMway;

public class OSMImporterNew 
{
	//Neo4j variables
	private static final String osmImport_DB = "target/osmImport-db";
	private static GraphDatabaseService graphDb;
	private static String osmXmlFilePath = "C:\\Users\\Carol\\Desktop\\GSoC\\osm\\liechtenstein.osm";
    private static Index<Node> nodeIdIndex;
    private static final String NODE_ID = "node_id";
    private static Node priorNode; //used to keep track of former connecting node in the sequence of nodes within a Way
	
	
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
        nodeIdIndex = graphDb.index().forNodes( "nodeIds" ); 
        registerShutdownHook();
        XMLInputFactory factory = XMLInputFactory.newInstance();
        // END SNIPPET: startDb

        // START SNIPPET: addUsers
        Transaction tx = graphDb.beginTx();
      		
      		try 
      		{
      			
      			XMLStreamReader streamReader = factory.createXMLStreamReader(
      				    new FileReader(osmXmlFilePath));
      			
      			// Create reference node
                Node referenceNode = graphDb.createNode();
                graphDb.getReferenceNode().createRelationshipTo(
                    referenceNode, RelTypes.OSM );
      			
                //Create Routing node and connect to Reference node
                Node routingNode = graphDb.createNode();
                referenceNode.createRelationshipTo( routingNode, RelTypes.OSM);
                
                //Create import node and connect to Routing node
                Node importNode = graphDb.createNode();
            	routingNode.createRelationshipTo(importNode, RelTypes.OSM);
                importNode.setProperty("name", "filename+filesize+currentdate");
                
      			while(streamReader.hasNext())
      			{
      				streamReader.next();
      				
      				if(streamReader.getEventType() == XMLStreamReader.START_ELEMENT)
      				{
      					if(streamReader.getLocalName() == "way")
      					{
      						
      						Node wayNode = graphDb.createNode();
      						//connect new way node with its property/attributes to the import node
      						importNode.createRelationshipTo(wayNode, RelTypes.OSM_WAY);
      						
      						//parse through all "way" tags and create a node for each with its properties
      						int count = streamReader.getAttributeCount();
      						for(int i = 0; i < count; i++)
      						{
      			                //set new Way node's properties
      			                wayNode.setProperty("name", streamReader.getAttributeName(i).toString());
      			                wayNode.setProperty("value", streamReader.getAttributeValue(i).toString());
      			                
      			                //go to next node
      			                streamReader.next();
      			                
      			                //create corresponding "nd" nodes and their properties
      			                if(streamReader.getLocalName() == "nd")
      			                {
      			                	//int n = 1;
      			                	
      			                	//Check whether or not the specific node was already created by checking for its id within index
      			                	if(!idPresent(streamReader.getAttributeValue(0).toString()))
      			                	{
      			                		Node node = graphDb.createNode();
      			                		wayNode.createRelationshipTo( node, RelTypes.OSM_NODE);
      			                		//set new Node's properties
      			                		node.setProperty("name", streamReader.getAttributeName(0).toString());
      			                		node.setProperty("value", streamReader.getAttributeValue(0).toString());
      			                		priorNode = node;
      			                	}
      			                	
      			                	//go to next node
      			                	streamReader.next();
      			                	
      			                	while(streamReader.getLocalName() == "nd")
      			                	{
      			                		Node nd = createAndIndexNode(streamReader.getAttributeValue(0).toString());
      			                		priorNode.createRelationshipTo( nd, RelTypes.OSM_NODENEXT);
          			                	
      			                		
      			                		//set new Node's properties
          			                	nd.setProperty("name", streamReader.getAttributeName(0).toString());
          			                	nd.setProperty("value", streamReader.getAttributeValue(0).toString());
      			                	
          			                	priorNode = nd;
      			                	}//end while "nd"
      			                	
      			                }// end if(streamReader.getLocalName()
      			                
      			                //Do I need to create a node for "tag" elements?
      			                //I think I do because they contain relevant routing information
      			                if(streamReader.getLocalName()== "tag")
      			                {
      			                	//create tag nodes and set properties...key and value
      			                	//Create a separate class to check for relevant tag values for routing
      			                }
      			                
      						}// end for(int i=0; i....)
      						
      					
      					
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
	
    //index "nd" elements and their node id
    private static Node createAndIndexNode( final String id )
    {
        Node node = graphDb.createNode();
        node.setProperty( NODE_ID, id );
        nodeIdIndex.add( node, NODE_ID, id );
        return node;
    }//end createAndIndexNode()

    
    private static boolean idPresent(String id)
    {
    	/*
    	 * Parse through index??
    	 * 
    	 * if id is already indexed
    		return true;
    	
    		else return false;
    	*/
    }//end idPresent()
    
}//end OSMImporterNew
