package org.neo4j.neo4j;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
//import java.util.Iterator;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.neo4j.OsmRoutingRelationships.RelTypes;

public class OSMRoutingImporter 
{
	//Neo4j variables
	private static String osmImport_DB = "target/osmImport-db";
	private static GraphDatabaseService graphDb;
	private static String osmXmlFilePath = "C:\\Users\\Carol\\Desktop\\GSoC\\osm\\liechtenstein.osm";
    private static Index<Node> nodeIdIndex;
    private static final String NODE_ID = "node_id";
    private static Node priorNode; //used to keep track of former connecting node in the sequence of nodes within a Way
	private static boolean wayNested; //used to detect whether or not the streamer is nested within a Way element
	private static boolean nodeNested = false;
	//private static int nodeCount = 0; //used to pace committing to graph
	private static ArrayList<String> nodeList = new ArrayList<String>();
	private static Map<String, String> wayMap = new HashMap<String, String>();
	private static Map<String, String> nodeMap = new HashMap<String, String>();
	private static boolean commitToGraph = false;
	private static boolean idPresent = false;
	
	
	/**
	 * @param args
	 */
	
	public OSMRoutingImporter (String filePath)
	{
		osmImport_DB = filePath;
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
      					
      					//*******************************************************
      					//Insert Way element's properties into wayMap
      					//*******************************************************
      					if(streamReader.getLocalName() == "way")
      					{
      						//parse though the way tag's attributes and values
      						int count = streamReader.getAttributeCount();
      						for(int i = 0; i < count; i++)
      						{    
      							//insert Way element's properties into wayMap
      			                wayMap.put(streamReader.getAttributeName(i).toString(), streamReader.getAttributeValue(i).toString());
      			                System.out.println("In way node block");
      						}
      						wayNested = true;
      					}
      					
      					
      					//*******************************************************
      					//Insert nodeID into ArrayList if nested within a Way tag
      					//*******************************************************
      			        if(streamReader.getLocalName() == "nd" && wayNested == true)
      			        {
      			        	nodeList.add(streamReader.getAttributeValue(0)); //Insert nodeID into ArrayList
      			        	System.out.println("In nd block");
      			        }//end if(getLocalName() == "nd"
      			        
      				     
      			        //*******************************************************
      					//Insert tag elements into wayMap if relevant to routing
      					//*******************************************************
      			        if(streamReader.getLocalName()== "tag" && wayNested == true)
      			        {
      			        	//parse though tag attributes and values and check if way element is relevant to routing
      						int count = streamReader.getAttributeCount();
      						for(int i = 0; i < count; i++)
      						{    
      							//insert tag element's properties into wayMap
      			                wayMap.put(streamReader.getAttributeValue(0).toString(), streamReader.getAttributeValue(1).toString());
      			                
      			                //check to see if relevant to routing
      			                if(streamReader.getAttributeValue(i).toString().equals("highway"))
      			                	commitToGraph = true;
      			                
      			                System.out.println("In tag block");
      						}//end for(int i...)
      			        }//if(streamReader.getLocalName()== "tag"...
      				}//end if(streamReader.getEventType)
      				
      				
      		
      			    //*****************************************************************
      				//Check to see if reached closing way tag
      				//If so, continue with creating Nodes to graph, if way is relevant to routing
  					//*****************************************************************
      				if(streamReader.getEventType() == XMLStreamReader.END_ELEMENT && streamReader.getLocalName() == "way" && commitToGraph == true)
      				{
      					System.out.println("In way END_ELEMENT block");
  						Node wayNode = graphDb.createNode();
  						//nodeCount++;
  						//connect new way node with its property/attributes to the import node
  						importNode.createRelationshipTo(wayNode, RelTypes.OSM_WAY);
      					priorNode = wayNode;
  						
  						//parse through Map and set wayNodes's properties based on the contents of the Map
  						for (Map.Entry<String, String> entry : wayMap.entrySet())
  						{
  							wayNode.setProperty(entry.getKey(), entry.getValue()); 
  							System.out.println(entry.getKey() + " " + entry.getValue());
  						}
  						int count = nodeList.size();
  						for(int i = 0; i < count; i++)
  						{
  							Node nd;
  							
  							if(!idPresent(nodeList.get(i).toString()))
  							{
  								nd = createAndIndexNode(nodeList.get(i));
  			              	  	//nodeCount++;
  							}
			              	    
			               	
			               	//Assign the address of the indexed node to this local node ("nd")
			               	else
			               	{
			               		IndexHits<Node> indexedNode = nodeIdIndex.get( NODE_ID, nodeList.get(i));
			               		nd = indexedNode.getSingle();
			               	}
			               	
			               	priorNode.createRelationshipTo( nd, RelTypes.OSM_NODENEXT);
			               	priorNode = nd;
  						}
  						
  						//RESET ALL VARIABLES AND DATA STRUCTURES
  			        	wayMap.clear(); //Reset Map for next Way element and its properties
  			        	nodeList.clear(); //Reset ArrayList for nodes in the next Way element
  			        	wayNested = false; //Reset Nested boolean
  			        	commitToGraph = false;
      				}//end if(streamReader.getEventType() == END.ELEMENT...
      					
      			}//end while streamReader.hasNext()
      			
      			//Parse through graphDb again to add Node Info to indexed nodes
      			getNodeInfo();
      			
      		  //if(nodeCount >= 5 && wayNested == false)
      			tx.success();
      		  
      			  
      		
      		}//end try
      		
      		
      		
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

    
    protected static void importXML(String filePath)
    {
    	osmXmlFilePath = filePath;
    }
    
    
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
  				        	nodeNested = true;
  				        	
  							//Check whether or not the specific node was indexed within the graphDb (is in a Way)
  							//Copy over its info to the indexed node
  			              	if(idPresent(streamReader.getAttributeValue(0).toString()))
  			              	{
  			              		idPresent = true;
  			              		priorNode = nodeIdIndex.get( NODE_ID, streamReader.getAttributeValue(0) ).getSingle();
  			              		
  			              		int count = streamReader.getAttributeCount();
  			              		for(int i = 1; i < count; i++)
  			              		{    
  			              			//insert tag element's properties into nodeMap
  			              			nodeMap.put(streamReader.getAttributeName(i).toString(), streamReader.getAttributeValue(i).toString());
  			              		}
  			              		
  			              	}//end if(idPresent...)
  			             	    
  		   			}//end if
  			        
  			        
  			        if(streamReader.getLocalName()== "tag" && nodeNested == true && idPresent == true)
  			        {
  							//insert tag element's properties into nodeMap
  			                nodeMap.put(streamReader.getAttributeValue(0).toString(), streamReader.getAttributeValue(1).toString());
  						
  			        }//end if(streamReader.getLocalName() == "tag"
  			        
  			        if(streamReader.getEventType() == XMLStreamReader.END_ELEMENT && streamReader.getLocalName() == "node" && idPresent == true)
  			        {
  			        	for (Map.Entry<String, String> entry : nodeMap.entrySet())
  						{
  							priorNode.setProperty(entry.getKey(), entry.getValue()); 
  							System.out.println(entry.getKey() + " " + entry.getValue());
  						}
			            
			            //Reset variables and data structures
  			        	nodeMap.clear(); //Reset Map for next Way element and its properties
  			        	nodeNested = false; //Reset Nested boolean
  			        	idPresent = false;
			           
  			        }//end if streamReader.getEventType() == END.ELEMENT...
  				}//end if(streamReader.getEventType)
  			}//end while
  		}//end try
  		
  		catch (XMLStreamException e) 
  		{
  		    e.printStackTrace();
  		}
    	
    	
    }//end getNodeInfo
}//end OSMImporterNew
