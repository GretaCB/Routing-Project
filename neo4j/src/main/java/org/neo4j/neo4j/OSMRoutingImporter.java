package org.neo4j.neo4j;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.neo4j.gis.spatial.pipes.processing.OrthodromicDistance;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.neo4j.OsmRoutingRelationships.RelTypes;

import com.vividsolutions.jts.geom.Coordinate;


public class OSMRoutingImporter 
{
	//Neo4j variables
	private String osmImport_DB;
	protected GraphDatabaseService graphDb;
	private String osmXmlFilePath;
    private Index<Node> nodeIdIndex;
    private Index<Node> wayNameIndex;
    private final String NODE_ID = "node_id";
    private Node priorNode; //used to keep track of former connecting node in the sequence of nodes within a Way
    protected Node importNode;
	private boolean wayNested; //used to detect whether or not the streamer is nested within a Way element
	private boolean nodeNested = false;
	private int nodeCount = 0; //used to pace committing to graph
	private ArrayList<String> nodeList = new ArrayList<String>();
	private Map<String, String> wayMap = new HashMap<String, String>();
	private Map<String, String> nodeMap = new HashMap<String, String>();
	private boolean commitToGraph = false;
	private boolean idPresentTest = false;
	private String wayID;
	
	//Constructor
	public OSMRoutingImporter (String filePath)
	{
		osmImport_DB = filePath;
	}
	
	
	public void importXML(String filePath) throws FileNotFoundException
    {
    	osmXmlFilePath = filePath;
    
    	// START SNIPPET: startDb
        graphDb = new GraphDatabaseFactory().newEmbeddedDatabase( osmImport_DB );
        nodeIdIndex = graphDb.index().forNodes( "nodeIds" ); 
        wayNameIndex = graphDb.index().forNodes( "wayNames" );
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
                importNode = graphDb.createNode();
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
      						//Obtain wayID for connected Node relationships
      						wayID = streamReader.getAttributeValue(0).toString();
      						
      						//parse though the way tag's attributes and values
      						int count = streamReader.getAttributeCount();
      						for(int i = 0; i < count; i++)
      						{    
      							//insert Way element's properties into wayMap
      			                wayMap.put(streamReader.getAttributeName(i).toString(), streamReader.getAttributeValue(i).toString());
      			               
      						}
      						wayNested = true;
      					}
      					
      					
      					//*******************************************************
      					//Insert nodeID into ArrayList if nested within a Way tag
      					//*******************************************************
      			        if(streamReader.getLocalName() == "nd" && wayNested == true)
      			        {
      			        	nodeList.add(streamReader.getAttributeValue(0)); //Insert nodeID into ArrayList
      			        
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
  						nodeCount++;
  						//connect new way node with its property/attributes to the import node
  						importNode.createRelationshipTo(wayNode, RelTypes.OSM_WAY);
      					priorNode = wayNode;
  						
  						//parse through Map and set wayNodes's properties based on the contents of the Map
  						for (Map.Entry<String, String> entry : wayMap.entrySet())
  						{
  							wayNode.setProperty(entry.getKey(), entry.getValue()); 
  							System.out.println(entry.getKey() + " " + entry.getValue());
  							
  							//Add wayNode's highway name to the wayName index
  							if(entry.getKey() == "name")
  								wayNameIndex.add(wayNode, "name", entry.getValue());
  							
  						}
  						
  						int count = nodeList.size();
  						for(int i = 0; i < count; i++)
  						{
  							Node nd;
  							
  							if(!idPresent(nodeList.get(i).toString()))
  								nd = createAndIndexNode(nodeList.get(i));
  			              	
			              	    
			               	
			               	//Assign the address of the indexed node to this local node ("nd")
			               	else
			               	{
			               		IndexHits<Node> indexedNode = nodeIdIndex.get( NODE_ID, nodeList.get(i));
			               		nd = indexedNode.getSingle();
			               	}
			               	
  							//Create relationship between nodes and set wayID
			               	Relationship rel = priorNode.createRelationshipTo( nd, RelTypes.OSM_NODENEXT);
			               	rel.setProperty("wayID", wayID);
			               	priorNode = nd;
  						}//end for(int i = 0...)
  						
  						//RESET ALL VARIABLES AND DATA STRUCTURES
  			        	wayMap.clear(); //Reset Map for next Way element and its properties
  			        	nodeList.clear(); //Reset ArrayList for nodes in the next Way element
  			        	wayNested = false; //Reset Nested boolean
  			        	commitToGraph = false;
      				}//end if(streamReader.getEventType() == END.ELEMENT...
      				
      				//Commit after every 5000 nodes
      				if(nodeCount >= 50000 && wayNested == false)
      				{
            			tx.success();
            			tx.finish();
            			tx = graphDb.beginTx();
            			
            			nodeCount = 1;
      				}
      				
      			}//end while streamReader.hasNext()
      			
      			//Parse through graphDb again to add Node Info to indexed nodes
      			System.out.println("Parsing through 2nd time for Node data...");
      			getNodeInfo();
      			//testWayIndex();
      			
      			//Traverse graph to add distance between nodes
          		System.out.println("Traversing graph for distance between nodes...");
          		traverseToCalculateDistances(tx);

      			//Commit the remaining nodes, if nodeCount < 5000
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
      		
    }//end importXML
	

	

	
	private void registerShutdownHook()
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
	
    private void shutdown()
    {
        graphDb.shutdown();
    }
	
    //index "nd" elements and their node id
    protected Node createAndIndexNode( final String id )
    {
        Node node = graphDb.createNode();
        nodeCount++;
        node.setProperty( NODE_ID, id );
        nodeIdIndex.add( node, NODE_ID, id );
        return node;
    }//end createAndIndexNode()


    protected boolean idPresent(String id)
    {
    	IndexHits<Node> checkForID = nodeIdIndex.get( NODE_ID, id );
    	Node test = checkForID.getSingle();
    	if(test == null)
    		return false;
    	
    	else 
    		return true;
    	
    }//end idPresent()
    
    
   
    
    /*
    private void testWayIndex()
    {
    	IndexHits<Node> hits = wayNameIndex.query("name", "*");
    	Node testNode;
    	int count = hits.size();
    	for(int i = -1; i < count; i++)
    	{
    		testNode = hits.next();
    		testNode.getPropertyKeys();
    		System.out.println();
    	}
    }
    */

    private Coordinate getCoordinate(Node node) {
		if (node.hasProperty("lon") && node.hasProperty("lat")) {
			Double lon = (Double) node.getProperty("lon");
			Double lat = (Double) node.getProperty("lat");
			return new Coordinate(lon, lat);
		} else {
			return null;
		}
    }
    
    private void setDistanceBetweenNodes(Node wayNode, Relationship rel, Node otherWayNode) {
		Coordinate first = getCoordinate(wayNode);
		Coordinate second = getCoordinate(otherWayNode);
		if (first != null && second != null) {
			rel.setProperty("distance_in_meters", OrthodromicDistance.calculateDistance(first, second) * 1000);
		}
    }
    
    private void traverseWayToCalculateDistance(Node wayNode, String wayId) {
    	System.out.println("Calculating distances in Way: " + wayId);
    	
    	boolean foundSomeWayNode = true;
    	while (foundSomeWayNode) {
			Iterator<Relationship> nodesRelationships = wayNode.getRelationships(Direction.OUTGOING, RelTypes.OSM_NODENEXT).iterator();
			foundSomeWayNode = false;			
			while (!foundSomeWayNode && nodesRelationships.hasNext()) {				
				Relationship nodeRel = nodesRelationships.next();
				if (!nodeRel.hasProperty("distance") && 
					nodeRel.hasProperty("wayID") && 
					wayId.equals(nodeRel.getProperty("wayID"))) 
				{
					Node otherWayNode = nodeRel.getOtherNode(wayNode);
					setDistanceBetweenNodes(wayNode, nodeRel, otherWayNode);
					wayNode = otherWayNode;
					foundSomeWayNode = true;
				}
			}
    	}
    }
    
    private void traverseToCalculateDistances(Transaction tx) {
    	Iterable<Relationship> waysRelationships = importNode.getRelationships(Direction.OUTGOING, RelTypes.OSM_WAY);
    	for (Relationship wayRel : waysRelationships) {
    		Node way = wayRel.getOtherNode(importNode);
    		String wayId = (String) way.getProperty("id");
    		traverseWayToCalculateDistance(way, wayId);    			
    		
    		tx.success();
    		tx.finish();
    		tx = graphDb.beginTx();
    	}
    }
    	
    //Parse through xml file again to gather info from indexed "Node" elements
    private void getNodeInfo() throws FileNotFoundException
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
  				
  					//*******************************************************
  					//Insert Node Element's properties into graphDB node
  					//*******************************************************
  					if(streamReader.getLocalName() == "node")
  		   			{
  				        	nodeNested = true;
  				        	
  							//Check whether or not the specific node was indexed within the graphDb (is in a Way)
  							//Copy over its info to the indexed node
  			              	if(idPresent(streamReader.getAttributeValue(0).toString()))
  			              	{
  			              		idPresentTest = true;
  			              		
  			              		IndexHits<Node> indexedNode = nodeIdIndex.get( NODE_ID, streamReader.getAttributeValue(0));
  			              		priorNode = indexedNode.getSingle();
  			              		
  			              		//Insert Node properties into nodeMap
  			              		int count = streamReader.getAttributeCount();
  			              		for(int i = 0; i < count; i++)
  			              		{    
  			              			//Insert tag element's properties into nodeMap
  			              			String k = streamReader.getAttributeName(i).toString();
  			              			if ("lat".equals(k) || "lon".equals(k)) {
  			              				Double value = Double.parseDouble(streamReader.getAttributeValue(i));
  			              				priorNode.setProperty(k, value);
  			              			} else {
  			              				priorNode.setProperty(k, streamReader.getAttributeValue(i));
  			              			}
  			              		}
  			              		//System.out.println("reading Node parameters");
  			              	}//end if(idPresent...)
  			             	
  			              	else
  			              		idPresentTest = false;
  			              	
  		   			}//end if
  			        
  				    
  					//*******************************************************
  					//Insert tag elements into nodeMap if Node ID is indexed
  					//*******************************************************
  			        if(streamReader.getLocalName().equals("tag") && nodeNested == true && idPresentTest == true)
  			        {
  							//insert tag element's properties into nodeMap
  			                nodeMap.put(streamReader.getAttributeValue(0).toString(), streamReader.getAttributeValue(1).toString());
  			                //System.out.println("reading Tag element");
  			        }//end if(streamReader.getLocalName() == "tag"
  			        
  				}//end if(streamReader.getEventType is START_ELEMENT)
  				
  				
			    //***************************************************************************
  				//Check to see if reached closing node tag and if node is indexed in GraphDB
  				//If so, continue with adding Tag properties from the nodeMap to the graphDB node
				//***************************************************************************
  				if(streamReader.getEventType() == XMLStreamReader.END_ELEMENT && streamReader.getLocalName() == "node" && idPresentTest == true)
			    {
			        	
			        	for (Map.Entry<String, String> entry : nodeMap.entrySet())
						{
							priorNode.setProperty(entry.getKey(), entry.getValue()); 
							// System.out.println(entry.getKey() + " " + entry.getValue());
						}
		            
			        	//Reset variables and data structures
			        	nodeMap.clear(); //Reset Map for next Way element and its properties
			        	nodeNested = false; //Reset Nested boolean
			        	idPresentTest = false;
			        
			    }//end if streamReader.getEventType() == END.ELEMENT...
  			}//end while
  		}//end try
  		
  		catch (XMLStreamException e) 
  		{
  		    e.printStackTrace();
  		}
    	
    	
    }//end getNodeInfo
}//end OSMImporterNew
