package org.neo4j.neo4j;

import java.io.FileNotFoundException;
import java.io.FileReader;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;


public class OSMImporterNew 
{
	//Neo4j variables
	private static final String osmImport_DB = "target/osmImport-db";
	private static GraphDatabaseService graphDb;
	private static String osmXmlFilePath = "C:\\Users\\Carol\\Desktop\\GSoC\\osm\\liechtenstein.osm";
    private static Index<Node> nodeIdIndex;
    private static final String NODE_ID = "node_id";
    private static Node priorNode; //used to keep track of former connecting node in the sequence of nodes within a Way
	

	
	/**
	 * @param args
	 */
	public enum RelTypes implements RelationshipType
	{
	        OSM,
			OSM_WAY,
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

        Transaction tx = graphDb.beginTx();
      		
      		try 
      		{
      			
      			XMLStreamReader streamReader = factory.createXMLStreamReader(
      				    new FileReader(osmXmlFilePath));
      			
      		    //Create Routing node and connect to Reference node
                Node routingNode = graphDb.createNode();
                graphDb.getReferenceNode().createRelationshipTo(
                    routingNode, RelTypes.OSM );
                
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
      						
      						//parse though the way tag's attributes and values
      						int count = streamReader.getAttributeCount();
      						for(int i = 0; i < count; i++)
      						{    
      							//set new Way node's properties
      			                wayNode.setProperty(streamReader.getAttributeName(i).toString(), streamReader.getAttributeValue(i).toString());
      			                
      						}
      					
      						priorNode = wayNode;
      					}//end if(getLocalName == "way")

      					
      					
      					
      			        //create corresponding "nd" nodes and their properties
      			        if(streamReader.getLocalName() == "nd")
      			        {
      			            
      			        	Node nd;
			                //Check whether or not the specific node was already created by checking for its id within index
			               	if(!idPresent(streamReader.getAttributeValue(0).toString()))
			              	    nd = createAndIndexNode(streamReader.getAttributeValue(0).toString());
			               	
			               	//How do I assign the address of the indexed node to this local node ("nd")?
			               	else
			               	{
			               		IndexHits<Node> indexedNode = nodeIdIndex.get( NODE_ID, streamReader.getAttributeValue(0) );
			               		nd = indexedNode.getSingle();
			               	}
			               	
			               	priorNode.createRelationshipTo( nd, RelTypes.OSM_NODENEXT);
			               	priorNode = nd;
      			               		
      			        }// end if(streamReader.getLocalName() == "nd"
      			                
      			        
      			        
      			        //Do I need to create a node for "tag" elements?
      			        //I think I do because they contain relevant routing information
      			        if(streamReader.getLocalName()== "tag")
      			        {
      			            //create tag nodes and set properties...key and value
      			            //Create a separate class to check for relevant tag values for routing
      			        }//end if(streamReader.getLocalNale() == "tag"
      				}//end if(streamReader.getEventType)
      			}//end while
      		    
      			//Parse through graphDb again to add Node Info to indexed nodes
      			getNodeInfo();
      			
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
    protected static Node createAndIndexNode( final String id )
    {
        Node node = graphDb.createNode();
        node.setProperty( NODE_ID, id );
        nodeIdIndex.add( node, NODE_ID, id );
        return node;
    }//end createAndIndexNode()

    
    protected static boolean idPresent(String id)
    {
    	IndexHits<Node> checkForID = nodeIdIndex.get( NODE_ID, id );
    	Node test = checkForID.getSingle();
    	if(test == null)
    		return false;
    	
    	else 
    		return true;
    	
    }//end idPresent()
    
    //Parse through xml file again to gather info from indexed "Node" elements
    private static void getNodeInfo() throws FileNotFoundException
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
  				        	//Check whether or not the specific node was indexed within the graphDb (is in a Way)
  							//Copy over its info to the indexed node
  			              	if(idPresent(streamReader.getAttributeValue(0).toString()))
  			              	{
  			              		IndexHits<Node> indexedNode = nodeIdIndex.get( NODE_ID, streamReader.getAttributeValue(0) );
  			              		
  			              		int count = streamReader.getAttributeCount();
  			              		for(int i = 1; i < count; i++)
  			              		{    
  			              			//****************
  			              			//****FIX THIS****
  			              			//****************
  			              			//Not sure how to access the indexed node to updates its properties....casting doesn't work
  			              			//Is an index even made up of nodes? What is an index made up of?
  			              			//...Maybe I dont need to check the index...Maybe I need to traverse the graphDb to update the "nd" node.
  			              			
  			              			//((Node) indexedNode).setProperty(streamReader.getAttributeName(i).toString(), streamReader.getAttributeValue(i).toString());
      			                
  			              		}
  			              		
  			              	}//end if(idPresent...)
  			             	    
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
    	
    	
    }//end getNodeInfo
}//end OSMImporterNew
