package org.neo4j.neo4j;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.lang.StringUtils;
import org.neo4j.gis.spatial.pipes.processing.OrthodromicDistance;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.neo4j.OsmRoutingRelationships.RelTypes;

import com.vividsolutions.jts.geom.Coordinate;


public class OSMRoutingImporter {

	protected GraphDatabaseService graphDb;
	private String osmXmlFilePath;
    private static Index<Node> nodeIdIndex;
    private static Index<Node> wayIdIndex;
    private Index<Node> wayNameIndex;
    private final static String NODE_ID = "node_id";
    private final static String WAY_ID = "way_id";
    protected Node importNode;
	private int nodeCount = 0; //used to pace committing to graph
	private String wayID;
	private String oneWayValue;
	
	//Constructor
	public OSMRoutingImporter (GraphDatabaseService graphDb) {
		this.graphDb = graphDb;
	}
	
	
	public void importXML(String filePath) throws FileNotFoundException, XMLStreamException {
    	osmXmlFilePath = filePath;
    
    	// START SNIPPET: startDb
        nodeIdIndex = graphDb.index().forNodes( "nodeIds" ); 
        wayIdIndex = graphDb.index().forNodes( "wayIds" );
        wayNameIndex = graphDb.index().forNodes( "wayNames" );
       
        XMLInputFactory factory = XMLInputFactory.newInstance();
        // END SNIPPET: startDb

        Transaction tx = graphDb.beginTx();
      		
      		try {
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
      			
            	
            	while(streamReader.hasNext()) {
      				streamReader.next();
      				
      				if(streamReader.getEventType() == XMLStreamReader.START_ELEMENT) {
      					if(streamReader.getLocalName().equals("way")) {
      						wayMap.clear();
      						nodeList.clear();
      						commitToGraph = false;
      						
      						//Obtain wayID for connected Node relationships
      						wayID = streamReader.getAttributeValue(0);
      					
      						//parse though the way tag's attributes and values
      						int count = streamReader.getAttributeCount();
      						for(int i = 0; i < count; i++){    
      							//insert Way element's properties into wayMap
      			                wayMap.put(streamReader.getAttributeName(i).toString(), streamReader.getAttributeValue(i));  
      			             
      						}
      						
      						wayNested = true;
      					} 
      					
      					else if(streamReader.getLocalName().equals("nd") && wayNested) {
      						//Insert nodeID into ArrayList if nested within a Way tag      			        	
      						nodeList.add(streamReader.getAttributeValue(0));      			      
      			        } 
      					
      					else if(streamReader.getLocalName().equals("tag") && wayNested) {	
      			        	//parse though tag attributes and values and check if way element is relevant to routing
      						int count = streamReader.getAttributeCount();
      						for(int i = 0; i < count; i++){    
      							//insert tag element's properties into wayMap
      			                wayMap.put(streamReader.getAttributeValue(0).toString(), streamReader.getAttributeValue(1).toString());
      			                
      			                //check to see if relevant to routing
      			                if(streamReader.getAttributeValue(i).toString().equals("highway")) {
      			                	commitToGraph = true;
      			                }      			                     			                
      						}
      			        }	
      				} //end if(streamReader.getEventType() == XMLStreamReader.START_ELEMENT) {
      				
      				else if(streamReader.getEventType() == XMLStreamReader.END_ELEMENT && streamReader.getLocalName().equals("way")) {
      					if (commitToGraph) {
	  						Node wayNode = graphDb.createNode();
	  						
	  						wayIdIndex.add(wayNode, "way_id", wayID);
	  						
	  						nodeCount++;
	  						//connect new way node with its property/attributes to the import node
	  						importNode.createRelationshipTo(wayNode, RelTypes.OSM_WAY);
	      					Node priorNode = wayNode;
	  						
	  						//parse through Map and set wayNodes's properties based on the contents of the Map
	  						for (Map.Entry<String, String> entry : wayMap.entrySet()) {
	  							wayNode.setProperty(entry.getKey(), entry.getValue()); 
	  							
	  							//Add wayNode's highway name to the wayName index
	  							if(entry.getKey().equals("name"))
	  								wayNameIndex.add(wayNode, "name", entry.getValue());
	  							
	  							if(entry.getKey().equals("oneway"))
	  								oneWayValue = entry.getValue();
	  						}
	  						//In case oneWayValue has a different value than yes/no or no value.
	  						//In this case, the default should be a two way
	  						if(oneWayValue == null || oneWayValue.trim().equals("") || (!oneWayValue.equalsIgnoreCase("no") && !oneWayValue.equalsIgnoreCase("yes")))
	  							oneWayValue = "default";
	  						
	  						int count = nodeList.size();
	  						for(int i = 0; i < count; i++) {
	  							Node nd = getOsmNode(nodeList.get(i));	  							
	  							if(nd == null)
	  								nd = createAndIndexNode(nodeList.get(i));
	  							
	  							
	  							//The SHORTCUT issue is happening somewhere between these next two else-if statements
	  							//...but I'm not sure why. It is creating a shortcut to every node just before a cross road
		  						//Add SHORTCUT relationships for cross roads to optimize routing
		  						//Have to figure out how to sum up the distance between all the nodes and then add to this relationship...
		  						//That may entail a separate traversal
	  							
	  							//One-way cross roads
	  							else if(nd != null && oneWayValue.equalsIgnoreCase("yes") && priorNode != wayNode) {
	  								Iterator<Relationship> nodesRelationships = nd.getRelationships(Direction.OUTGOING, RelTypes.ONEWAY_NEXT, RelTypes.BIDIRECTIONAL_NEXT).iterator();
	  								//if not a FIRST_NODE relationship
	  								if(nodesRelationships.hasNext()) {
	  									Relationship rel = wayNode.createRelationshipTo(priorNode, RelTypes.ONEWAY_SHORTCUT);
	  									rel.setProperty("wayID", wayID);
	  									rel.setProperty("oneWay", oneWayValue);
	  								}
	  							}
	  							
	  							//Two-way cross roads
	  							else if(nd != null && !oneWayValue.equalsIgnoreCase("yes") && priorNode != wayNode) {
	  								Iterator<Relationship> nodesRelationships = nd.getRelationships(Direction.OUTGOING, RelTypes.ONEWAY_NEXT, RelTypes.BIDIRECTIONAL_NEXT).iterator();
	  								//if not a FIRST_NODE relationship
	  								if(nodesRelationships.hasNext()) {
	  									Relationship rel = wayNode.createRelationshipTo(priorNode, RelTypes.BIDIRECTIONAL_SHORTCUT);
	  									rel.setProperty("wayID", wayID);
	  									rel.setProperty("oneWay", oneWayValue);
	  								}
	  							}
	  							
	  							if(priorNode == wayNode) {
	  						    	Relationship rel = priorNode.createRelationshipTo(nd, RelTypes.OSM_FIRSTNODE);
					               	rel.setProperty("wayID", wayID);
					               	rel.setProperty("oneWay", oneWayValue);
					               	priorNode = nd;
	  							}
	  							
	  							else if(oneWayValue.equalsIgnoreCase("yes")) {
	  								//Create relationship between nodes and set wayID
	  								Relationship rel = priorNode.createRelationshipTo(nd, RelTypes.ONEWAY_NEXT);
	  								rel.setProperty("wayID", wayID);
	  								rel.setProperty("oneWay", oneWayValue);
	  								priorNode = nd;
	  							}
	  							
	  							//else...oneWayValue.equals("no") || oneWayValue.equals("default")
	  							else {
	  								//Create relationship between nodes and set wayID
	  								Relationship rel = priorNode.createRelationshipTo(nd, RelTypes.BIDIRECTIONAL_NEXT);
	  								rel.setProperty("wayID", wayID);
	  								rel.setProperty("oneWay", oneWayValue);
	  								priorNode = nd;
		  						}
	  						
	  						}//end for(int i = 0...)
	  						
	  						
	  						//Add SHORTCUT relationships for cross roads to optimize routing
	  						//Have to figure out how to sum up the distance between all the nodes and then add to this relationship...
	  						//That may entail a separate traversal
	  						if(oneWayValue.equalsIgnoreCase("yes")) {
	  							Relationship rel = wayNode.createRelationshipTo(priorNode, RelTypes.ONEWAY_SHORTCUT);
	  							rel.setProperty("wayID", wayID);
					            rel.setProperty("oneWay", oneWayValue);
	  						}
	  						
	  						else {
	  							Relationship rel = wayNode.createRelationshipTo(priorNode, RelTypes.BIDIRECTIONAL_SHORTCUT);
	  							rel.setProperty("wayID", wayID);
					            rel.setProperty("oneWay", oneWayValue);
	  						}
	  						
      					} //end if (commitToGraph)
      					
  						wayNested = false; //Reset Nested boolean
  						oneWayValue = ""; //Reset oneWayValue
  			        } //end if(streamReader.getEventType() == XMLStreamReader.END_ELEMENT && streamReader.getLocalName().equals("way"))
      				
      				if(nodeCount >= 50000) {
            			tx.success();
            			tx.finish();
            			tx = graphDb.beginTx();
            			System.out.println("COMMITTED!");
            			nodeCount = 0;
      				}
      			}//end while streamReader.hasNext()
      			
            	streamReader.close();
            	
            	//Commit the remaining nodes, if nodeCount < 50000
            	tx.success();
    			tx.finish();
    			tx = graphDb.beginTx();
            	
      			//Parse through graphDb again to add Node Info to indexed nodes
      			System.out.println("Parsing through 2nd time for Node data...");
      			getNodeInfo(tx);
      			
      			//Commit remaining new nodes
      			tx.success();
    			tx.finish();
    			tx = graphDb.beginTx();
      			
      			//Traverse graph to add distance between nodes
          		System.out.println("Traversing graph for distance between nodes...");
          		traverseToCalculateDistances(tx);
      			
      		}
      		
      		finally{
      			 tx.finish();
      		}
            		
      		
    }//end importXML
	

    //index "nd" elements and their node id
    protected Node createAndIndexNode( final String id ) {
        Node node = graphDb.createNode();
        nodeCount++;
        node.setProperty( NODE_ID, id );
        nodeIdIndex.add( node, NODE_ID, id );
        return node;
    }//end createAndIndexNode()

    
    public static Node getOsmWay(String id) {
    	IndexHits<Node> checkForID = wayIdIndex.get( WAY_ID, id );
    	return checkForID.getSingle();
    }
    
    public static Node getOsmNode(String id) {
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
    
    private void setDistanceBetweenNodes(Node firstNode, Relationship rel, Node otherWayNode, Double speedLimit) {
		Coordinate first = getCoordinate(firstNode);
		Coordinate second = getCoordinate(otherWayNode);
		
		//Calculate distance for km
		if (first != null && second != null) {
			Double distance = OrthodromicDistance.calculateDistance(first, second) * 1000;
			Double metersPerSec = speedLimit / 3.6; //1 meter per second = 3.6 km per hour...this will convert the speed into meters
			
			rel.setProperty("distance_in_meters", distance);
			//cost is the number of seconds to travel the distance in an hour traveling the max speed
			rel.setProperty("secondsToTravel", (distance / metersPerSec)); 
		}
		
    }
    
    private void traverseWayToCalculateDistance(Node firstNode, String wayId, Double speedLimit) {
    	System.out.println("Calculating distances in Way: " + wayId);
    	
    	boolean foundSomeWayNode = true;
    	while (foundSomeWayNode) {
			Iterator<Relationship> nodesRelationships = firstNode.getRelationships(Direction.OUTGOING, RelTypes.ONEWAY_NEXT, RelTypes.BIDIRECTIONAL_NEXT).iterator();
			foundSomeWayNode = false;			
			while (!foundSomeWayNode && nodesRelationships.hasNext()) {				
				Relationship nodeRel = nodesRelationships.next();
				if (!nodeRel.hasProperty("distance_in_meters") && 
					nodeRel.hasProperty("wayID") && 
					wayId.equals(nodeRel.getProperty("wayID"))) 
				{
					Node otherWayNode = nodeRel.getOtherNode(firstNode);
					setDistanceBetweenNodes(firstNode, nodeRel, otherWayNode, speedLimit);
					firstNode = otherWayNode;
					foundSomeWayNode = true;
				}
			}
    	}
    }
    
    private void traverseToCalculateDistances(Transaction tx) {	
    	Iterable<Relationship> waysRelationships = importNode.getRelationships(Direction.OUTGOING, RelTypes.OSM_WAY);
    	for (Relationship wayRel : waysRelationships) {
    		Node way = wayRel.getOtherNode(importNode);
    		Double speedLimit;
    		if(way.hasProperty("maxspeed")){		
    			String speed = (String) way.getProperty("maxspeed");
    			
    			//Check for mph
    			if(speed.contains("m")){
    				speed = StringUtils.left(speed, 2);
    				speedLimit = Double.parseDouble(speed);
    				
    				//Convert from miles per hr to kilometers per hr
    				//One mile = 1.609 km
    				speedLimit = speedLimit * 1.609;
    			}
    			
    			else
    				speedLimit = Double.parseDouble(speed);
    		}
    		
    		//set to default
    		else
    			speedLimit = 50.00; //km
    		
    		
    		Relationship rel = way.getSingleRelationship(RelTypes.OSM_FIRSTNODE, Direction.OUTGOING);
    		Node firstNode = rel.getEndNode();
    		String wayId = (String) way.getProperty("id");
    		traverseWayToCalculateDistance(firstNode, wayId, speedLimit);    			
    		
    		tx.success();
    		tx.finish();
    		tx = graphDb.beginTx();
    	}
    }
    

    //Parse through xml file again to gather info from indexed "Node" elements
    private void getNodeInfo(Transaction tx) throws FileNotFoundException, XMLStreamException {
    	XMLInputFactory factory = XMLInputFactory.newInstance();
    	Node osmNode = null;
    	nodeCount = 0;
    	
   			XMLStreamReader streamReader = factory.createXMLStreamReader(
   				    new FileReader(osmXmlFilePath));
   			while(streamReader.hasNext()) {
  				streamReader.next();
  				
  				if(streamReader.getEventType() == XMLStreamReader.START_ELEMENT) {
  					//*******************************************************
  					//Insert Node Element's properties into graphDB node
  					//*******************************************************
  					if(streamReader.getLocalName().equals("node")) {
  				        
  						osmNode = getOsmNode(streamReader.getAttributeValue(0));  				        	
  						if(osmNode != null) {
  							nodeCount++;
  							
  							//Insert Node properties into nodeMap
  			              	int count = streamReader.getAttributeCount();
  			              	for(int i = 0; i < count; i++) {    
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
  						
  						if(nodeCount >= 50000) {
  					           tx.success();
  					           tx.finish();
  					           tx = graphDb.beginTx();
  					           System.out.println("COMMITTED!");
  					           nodeCount = 0;
  					     }
  						
  		   			} 
  					else if(streamReader.getLocalName().equals("tag") && osmNode != null) {
  							//insert tag element's properties into nodeMap
  			                osmNode.setProperty(streamReader.getAttributeValue(0), streamReader.getAttributeValue(1));
  			        }  			        
  				} 
  				else if(streamReader.getEventType() == XMLStreamReader.END_ELEMENT && streamReader.getLocalName().equals("node")) {
  					osmNode = null;
			    }//end if streamReader.getEventType() == END.ELEMENT...
  			}//end while
   			
   			streamReader.close();
   			
    }//end getNodeInfo
}//end OSMImporterNew