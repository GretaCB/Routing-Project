package org.neo4j.neo4j;

import java.io.FileNotFoundException;
import java.io.FileReader;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.neo4j.graphdb.GraphDatabaseService;
//import org.neo4j.graphdb.Node;
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
	        OSM_WAY,
	        OSM_NODE,
	        OSM_NODENEXT,
	        REFERENCE_NODE
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

      			while(streamReader.hasNext())
      			{
      				streamReader.next();
      				
      				if(streamReader.getEventType() == XMLStreamReader.START_ELEMENT)
      				{
      					if(streamReader.getLocalName() == "way")
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
      		
      		
        /*
        try
        {
            // Create users sub reference node
            Node referenceNode = graphDb.createNode();
            graphDb.getReferenceNode().createRelationshipTo(
                referenceNode, RelTypes.REFERENCE_NODE );
            
            Node wayNode = createAndIndexWayNode( ??? );//what do I want to pass to this method?
            referenceNode.createRelationshipTo( wayNode,
                RelTypes.OSM_WAY );
            
        }//end try
        
        catch
        {
        	
        }
        */
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
	
    /*//The following is for using the StAX API
    try 
	{
		// First create a new XMLInputFactory
		XMLInputFactory inputFactory = XMLInputFactory.newInstance();
		// Setup a new eventReader
		InputStream in = new FileInputStream(configFile);
		XMLEventReader eventReader = inputFactory.createXMLEventReader(in);
	
	}//end try
	*/
}//end OSMImporterNew
