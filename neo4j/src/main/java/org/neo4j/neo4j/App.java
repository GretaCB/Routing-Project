package org.neo4j.neo4j;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

/**
 * Hello world!
 *
 */
public class App 
{
	private static final String DB_PATH = "target/neo4j-hello-db";
	GraphDatabaseService graphDb;
	Node firstNode;
	Node secondNode;
	Relationship relationship;
	
	
	private static enum RelTypes implements RelationshipType
	{
	    KNOWS
	}//end RelTypes
	
	public static void main( String[] args )
    {
		
		App hello = new App();
        hello.createDB();
        hello.removeData();
        hello.shutDown();
		
    }//end main
	
	void createDB()
	{
		graphDb = new GraphDatabaseFactory().newEmbeddedDatabase( DB_PATH );
		registerShutdownHook( graphDb );
		
		Transaction tx = graphDb.beginTx();
		try
		{
		    // Mutating operations go here
			firstNode = graphDb.createNode();
			firstNode.setProperty( "message", "Hello, " );
			secondNode = graphDb.createNode();
			secondNode.setProperty( "message", "World!" );
			
			relationship = firstNode.createRelationshipTo( secondNode, RelTypes.KNOWS );
			relationship.setProperty( "message", "brave Neo4j " );
			
			System.out.print( firstNode.getProperty( "message" ) );
			System.out.print( relationship.getProperty( "message" ) );
			System.out.println( secondNode.getProperty( "message" ) );
			
			
		    tx.success();
		}
		finally
		{
		    tx.finish();
		}
		
	}//end createDB
	
	


	private void removeData() 
	{
		
		Transaction tx = graphDb.beginTx();
		
		try
		{
			// let's remove the data
			firstNode.getSingleRelationship( RelTypes.KNOWS, Direction.OUTGOING ).delete();
			firstNode.delete();
			secondNode.delete();
			
			tx.success();
		}//end try
		
		finally
		{
			tx.finish();
		}
	}//end removeData
	
	
	private static void registerShutdownHook( final GraphDatabaseService graphDb )
	{
	    // Registers a shutdown hook for the Neo4j instance so that it
	    // shuts down nicely when the VM exits (even if you "Ctrl-C" the
	    // running example before it's completed)
	    Runtime.getRuntime().addShutdownHook( new Thread()
	    {
	        @Override
	        public void run()
	        {
	            graphDb.shutdown();
	        }
	        
	    } );
	}//end registerShutdownHook
	
	


	
	
	private void shutDown() 
	{
		System.out.println( "Shutting down database ..." );
		graphDb.shutdown();
		
	}//end shutDown
	
}//end App class
