package org.neo4j.neo4j;
//import org.neo4j.graphdb.GraphDatabaseService;
//import org.neo4j.graphdb.Node;
//import org.neo4j.graphdb.Path;
//import org.neo4j.graphdb.traversal.Evaluation;
//import org.neo4j.graphdb.traversal.Evaluator;
//import org.neo4j.graphdb.traversal.Evaluators;
//import org.neo4j.graphdb.traversal.TraversalDescription;
//import org.neo4j.kernel.Traversal;
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
		//String output = "";

		OSMRoutingImporter importer = new OSMRoutingImporter(graphDbPath);

		importer.importXML(osmXmlFilePath);

	
      	/*        
      	try
      	{
      		//Obtain startnode from imported graph
    		//Node startNode = importer.importNode;
      		Node startNode = importer.graphDb.getReferenceNode();
        
      		System.out.println(output);
      	}
      	
      	catch(NullPointerException npe)
      	{
      		System.out.println("Error");
      	}
		*/
		
	}//end main

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