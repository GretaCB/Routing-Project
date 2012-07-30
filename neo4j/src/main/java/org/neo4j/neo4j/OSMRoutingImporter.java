package org.neo4j.neo4j;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

//import javax.swing.JFrame;
//import javax.swing.JOptionPane;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.neo4j.gis.spatial.pipes.processing.OrthodromicDistance;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Expander;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.kernel.Traversal;
import org.neo4j.neo4j.OsmRoutingRelationships.RelTypes;
import org.neo4j.graphalgo.CommonEvaluators;
import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphalgo.EstimateEvaluator;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphalgo.WeightedPath;

import com.vividsolutions.jts.geom.Coordinate;


public class OSMRoutingImporter 
{
	//Neo4j variables
	private String osmImport_DB;
	protected GraphDatabaseService graphDb;
	private String osmXmlFilePath;
    private Index<Node> nodeIdIndex;
    private Index<Node> wayIdIndex;
    private Index<Node> wayNameIndex;
    private final String NODE_ID = "node_id";
    protected Node importNode;
	private int nodeCount = 0; //used to pace committing to graph
	private String wayID;
	
	
	//Constructor
	public OSMRoutingImporter (String filePath)
	{
		osmImport_DB = filePath;
	}
	
	
	public void importXML(String filePath) throws FileNotFoundException, XMLStreamException
    {
    	osmXmlFilePath = filePath;
    
    	// START SNIPPET: startDb
        graphDb = new GraphDatabaseFactory().newEmbeddedDatabase( osmImport_DB );
        nodeIdIndex = graphDb.index().forNodes( "nodeIds" ); 
        wayIdIndex = graphDb.index().forNodes( "wayIds" );
        wayNameIndex = graphDb.index().forNodes( "wayNames" );
        registerShutdownHook();
        XMLInputFactory factory = XMLInputFactory.newInstance();
        // END SNIPPET: startDb

        Transaction tx = graphDb.beginTx();
      		
      		try 
      		{
      			File osmXmlFile = new File(osmXmlFilePath);
      			XMLStreamReader streamReader = factory.createXMLStreamReader(
      				    new FileReader(osmXmlFilePath));
      			
      		    //Create Routing node and connect to Reference node
                Node routingNode = graphDb.createNode();
                graphDb.getReferenceNode().createRelationshipTo(
                    routingNode, RelTypes.OSM );
                
                //Create import node and connect to Routing node
                importNode = graphDb.createNode();
            	routingNode.createRelationshipTo(importNode, RelTypes.OSM);
                importNode.setProperty("name", System.currentTimeMillis() + " " + osmXmlFile.getName());
                
            	ArrayList<String> nodeList = new ArrayList<String>();
            	Map<String, String> wayMap = new HashMap<String, String>();
            	boolean commitToGraph = false;
            	boolean wayNested = false; //used to detect whether or not the streamer is nested within a Way element
      			
            	while(streamReader.hasNext())
      			{
      				streamReader.next();
      				
      				if(streamReader.getEventType() == XMLStreamReader.START_ELEMENT)
      				{
      					if(streamReader.getLocalName().equals("way"))
      					{
      						wayMap.clear();
      						nodeList.clear();
      						commitToGraph = false;
      						
      						//Insert Way element's properties into wayMap
      						
      						//Obtain wayID for connected Node relationships
      						wayID = streamReader.getAttributeValue(0);
      						
      						//parse though the way tag's attributes and values
      						int count = streamReader.getAttributeCount();
      						for(int i = 0; i < count; i++)
      						{    
      							//insert Way element's properties into wayMap
      			                wayMap.put(streamReader.getAttributeName(i).toString(), streamReader.getAttributeValue(i));      			               
      						}
      						
      						wayNested = true;
      					} 
      					else if(streamReader.getLocalName().equals("nd") && wayNested) 
      					{
      						//Insert nodeID into ArrayList if nested within a Way tag      			        	
      						nodeList.add(streamReader.getAttributeValue(0));      			      
      			        } 
      					else if(streamReader.getLocalName().equals("tag") && wayNested)
      			        {
      			        	//Insert tag elements into wayMap if relevant to routing
      			        	
      			        	//parse though tag attributes and values and check if way element is relevant to routing
      						int count = streamReader.getAttributeCount();
      						for(int i = 0; i < count; i++)
      						{    
      							//insert tag element's properties into wayMap
      			                wayMap.put(streamReader.getAttributeValue(0).toString(), streamReader.getAttributeValue(1).toString());
      			                
      			                //check to see if relevant to routing
      			                if(streamReader.getAttributeValue(i).toString().equals("highway")) {
      			                	commitToGraph = true;
      			                }      			                     			                
      						}
      			        }
      				} else if(streamReader.getEventType() == XMLStreamReader.END_ELEMENT && streamReader.getLocalName().equals("way")) {
      					if (commitToGraph) {
	  						Node wayNode = graphDb.createNode();
	  						
	  						wayIdIndex.add(wayNode, "way_id", wayID);
	  						
	  						nodeCount++;
	  						//connect new way node with its property/attributes to the import node
	  						importNode.createRelationshipTo(wayNode, RelTypes.OSM_WAY);
	      					Node priorNode = wayNode;
	  						
	  						//parse through Map and set wayNodes's properties based on the contents of the Map
	  						for (Map.Entry<String, String> entry : wayMap.entrySet())
	  						{
	  							wayNode.setProperty(entry.getKey(), entry.getValue()); 
	  							
	  							//Add wayNode's highway name to the wayName index
	  							if(entry.getKey().equals("name"))
	  								wayNameIndex.add(wayNode, "name", entry.getValue());
	  						}
	  						
	  						int count = nodeList.size();
	  						for(int i = 0; i < count; i++)
	  						{
	  							Node nd = getOsmNode(nodeList.get(i));	  							
	  							if(nd == null)
	  								nd = createAndIndexNode(nodeList.get(i));
				               	
	  							//Create relationship between nodes and set wayID
				               	Relationship rel = priorNode.createRelationshipTo(nd, RelTypes.OSM_NODENEXT);
				               	rel.setProperty("wayID", wayID);
				               	priorNode = nd;
	  						}//end for(int i = 0...)
      					}
      					
  						wayNested = false; //Reset Nested boolean
  			        }
      				
      				if(nodeCount == 50000)
      				{
            			tx.success();
            			tx.finish();
            			tx = graphDb.beginTx();
            			
            			nodeCount = 0;
      				}
      			}//end while streamReader.hasNext()
      			
      			//Parse through graphDb again to add Node Info to indexed nodes
      			System.out.println("Parsing through 2nd time for Node data...");
      			getNodeInfo();
      			//testWayIndex();
      			
      			//Traverse graph to add distance between nodes
          		System.out.println("Traversing graph for distance between nodes...");
          		traverseToCalculateDistances(tx);

      			//Commit the remaining nodes, if nodeCount < 50000
      			tx.success();
      			
      			//createRoute();
      			
      		}      		
      		finally
            {
                 tx.finish();
            }
            
      		createRoute();
      		
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


    protected Node getOsmNode(String id)
    {
    	IndexHits<Node> checkForID = nodeIdIndex.get( NODE_ID, id );
    	return checkForID.getSingle();
    }
    
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
				if (!nodeRel.hasProperty("distance_in_meters") && 
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
    
    
    public void createRoute()
	{
    	//Not sure when I need to start a new transaction...and I'm assuming this is the reason I'm getting a NotFoundException
    	Transaction tx = graphDb.beginTx();
    	
    	try{
    	System.out.println("In creatRoute method...");
    	EstimateEvaluator<Double> estimateEval = CommonEvaluators.geoEstimateEvaluator(
    	            "lat", "lon" );
    	System.out.println("After EstimateEvaluator");
    	
    	Expander relExpander = Traversal.expanderForTypes(
                RelTypes.OSM_NODENEXT, Direction.BOTH );
    	relExpander.add( RelTypes.OSM_NODENEXT, Direction.BOTH );
    	System.out.println("After relationship expander");
    	
    	CostEvaluator<Double> costEval = CommonEvaluators.doubleCostEvaluator("distance_in_meters");
    	System.out.println("After Cost Evaluator");
    	
    	PathFinder<WeightedPath> finder = GraphAlgoFactory.aStar(
               relExpander , costEval, estimateEval );
    	System.out.println("After PathFinder");
    	
    	//String startNodeID = "278451834";
		//String endNodeID = "268222979";
		/*
		JFrame frame = new JFrame("Nodes to Route:");
		startNodeID = JOptionPane.showInputDialog(frame, "Enter nodeID for the Start Node: ");
		endNodeID = JOptionPane.showInputDialog(frame, "Enter nodeID for the End Node: ");
	  	*/
		
		Node startNode = getOsmNode("278451834");
		Node endNode = getOsmNode("268222979");
		//long startNodeid = Long.parseLong(startNodeID);
	  	//long endNodeid = Long.parseLong(endNodeID);
		System.out.println(startNode.getProperty("id")); 
		System.out.println(endNode.getProperty("id"));
	  	//Node start = graphDb.getNodeById(startNode);
	  	//Node end = graphDb.getNodeById(endNode);
	  	Path route = finder.findSinglePath(startNode, endNode); 
	  	
	  	for ( Node node : route.nodes() )
        {
	  		System.out.println( node.getProperty( "id" ) );
        }
	  	
    	}//end try
    	
    	finally
        {
    		tx.finish();
        }
	  	  
	}//end createRoute()
	
    
    
    //Parse through xml file again to gather info from indexed "Node" elements
    private void getNodeInfo() throws FileNotFoundException, XMLStreamException
    {
    	XMLInputFactory factory = XMLInputFactory.newInstance();
    	Node osmNode = null;
    	
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
  					if(streamReader.getLocalName().equals("node"))
  		   			{
  				        osmNode = getOsmNode(streamReader.getAttributeValue(0));  				        	
  						if(osmNode != null)
  			            {
  			              	//Insert Node properties into nodeMap
  			              	int count = streamReader.getAttributeCount();
  			              	for(int i = 0; i < count; i++)
  			              	{    
  			              		//Insert tag element's properties into nodeMap
  			              		String k = streamReader.getAttributeName(i).toString();
  			              		if ("lat".equals(k) || "lon".equals(k)) {
  			              			Double value = Double.parseDouble(streamReader.getAttributeValue(i));
  			              			osmNode.setProperty(k, value);
  			              		} else {
  			              			osmNode.setProperty(k, streamReader.getAttributeValue(i));
  			              		}
  			              	}
  			            }      	
  		   			} 
  					else if(streamReader.getLocalName().equals("tag") && osmNode != null)
  			        {
  							//insert tag element's properties into nodeMap
  			                osmNode.setProperty(streamReader.getAttributeValue(0), streamReader.getAttributeValue(1));
  			        }  			        
  				} 
  				else if(streamReader.getEventType() == XMLStreamReader.END_ELEMENT && streamReader.getLocalName().equals("node"))
			    {
  					osmNode = null;
			    }//end if streamReader.getEventType() == END.ELEMENT...
  			}//end while
  		
    	
    }//end getNodeInfo
}//end OSMImporterNew