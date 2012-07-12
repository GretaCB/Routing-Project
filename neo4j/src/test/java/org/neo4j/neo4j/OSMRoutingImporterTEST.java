package org.neo4j.neo4j;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
//import org.neo4j.graphdb.traversal.Evaluation;
//import org.neo4j.graphdb.traversal.Evaluator;
//import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;
import org.neo4j.neo4j.OSMRoutingImporter;


public class OSMRoutingImporterTEST 
{
	//public static Evaluator e = new Evaluator();
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception 
	{
		String graphDbPath = "target/osmImport-db";
		String osmXmlFilePath = "C:\\Users\\Carol\\Desktop\\GSoC\\osm\\liechtenstein.osm";
		String output = "";
		
		OSMRoutingImporter importer = new OSMRoutingImporter(graphDbPath);
		
		importer.importXML(osmXmlFilePath);
		//GraphDatabaseService graphDb = importer.graphDb;
		//Obtain startnode from imported graph
		//Node startNode = graphDb.getNodeById("importNode");
		System.out.println( importer.printRoute() );
		/*
        //Traverse imported graph and add calculated distance between nodes
      	final TraversalDescription NODE_TRAVERSAL = Traversal.description()
      		        .depthFirst();
      		        
      	try
      	{
      		
      		for ( Path path : NODE_TRAVERSAL.traverse( startNode ) )
      		{
      			//evaluate(path);
      			if(path.length() > 0)
      				output += path + "\n";
      		}
       
        
      		System.out.println(output);
      	}
      	
      	catch(NullPointerException npe)
      	{
      		System.out.println("Error");
      	}
		*/
	
	}//end main

	private Node getNode()
	{
	     return importer.graphDb.getReferenceNode()
	             .getSingleRelationship( RelTypes.NEO_NODE, Direction.OUTGOING )
	             .getEndNode();
	}
	
	
	public String printRoute()
	{
	        Node currentNode = getNode();
	        // START SNIPPET: friends-usage
	        int numberOfFriends = 0;
	        String output = currentNode.getProperty( "nodeID" ) + " node:\n";
	        Traverser routeTraverser = getFriends( neoNode );
	        for ( Path friendPath : friendsTraverser )
	        {
	            output += "At depth " + friendPath.length() + " => "
	                      + friendPath.endNode()
	                              .getProperty( "name" ) + "\n";
	            numberOfFriends++;
	        }
	        output += "Number of friends found: " + numberOfFriends + "\n";
	        // END SNIPPET: friends-usage
	        return output;
	    }

	    // START SNIPPET: get-friends
	    private static Traverser getFriends(
	            final Node person )
	    {
	        TraversalDescription td = Traversal.description()
	                .breadthFirst()
	                .relationships( RelTypes.KNOWS, Direction.OUTGOING )
	                .evaluator( Evaluators.excludeStartPosition() );
	        return td.traverse( person );
	    }
	
	/*
	public static Evaluation evaluate(Path path)
	{
		if(path.lastRelationship().getProperty(wayID).equals(wayID))
			return Evaluation.INCLUDE_AND_CONTINUE;
		
		else
			return Evaluation.EXCLUDE_AND_CONTINUE;
	}
	*/
}//end TEST
