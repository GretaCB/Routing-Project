package org.neo4j.neo4j;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.neo4j.OSMRoutingImporter;

public class OSMRoutingProjectTEST {
	
	public static GraphDatabaseService graphDb;
	public static String osmXmlFilePath;
	public static String graphDbPath;
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
	
		//Obtain info from user
	 	osmXmlFilePath = JOptionPane.showInputDialog("Enter the filepath where your OpenStreetMap XML file is located.\n" +
	 		  		"(Please use two slashes between each folder. " +
	 		  		"For example: C:\\\\Users\\\\Me\\\\Documents\\\\delaware.osm):");
	 	graphDbPath = JOptionPane.showInputDialog("Enter the filepath where you want to store your graph database folder.\n" +
 		  		"(Please use two slashes between each folder. " +
 		  		"For example: C:\\\\Users\\\\Me\\\\Documents\\\\osmImport-db):");
	 	
		graphDb = new GraphDatabaseFactory().newEmbeddedDatabase( graphDbPath );
		
		JFrame frame = new JFrame("Nodes to Route:");
		String startNodeID = JOptionPane.showInputDialog(frame, "Enter nodeID for the Start Node: ");
		String endNodeID = JOptionPane.showInputDialog(frame, "Enter nodeID for the End Node: ");
		

		//Import and Route input
		registerShutdownHook();
		OSMRoutingImporter importer = new OSMRoutingImporter(graphDb);

		importer.importXML(osmXmlFilePath);
		
		OSMRouting router = new OSMRouting (graphDb, startNodeID, endNodeID);
		
		System.out.println("Creating route...");
		router.createRoute();
		
		System.out.println( "Shutting down database ..." );
        shutdown();

	}//end main

	  private static void shutdown()
	  {
	      graphDb.shutdown();
	  }
	
	  
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
	
	
}//end OSMRoutingProjectTEST class
