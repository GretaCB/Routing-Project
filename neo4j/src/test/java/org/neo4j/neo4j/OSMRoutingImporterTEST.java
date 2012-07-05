package org.neo4j.neo4j;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;
import org.neo4j.kernel.Uniqueness;
import org.neo4j.neo4j.OSMRoutingImporter;


public class OSMRoutingImporterTEST 
{

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
		
		Node importNode = importer.importNode;
		

        //Traverse imported graph and add calculated distance between nodes
      	final TraversalDescription NODE_TRAVERSAL = Traversal.description()
      		        .depthFirst()
      		        .uniqueness( Uniqueness.RELATIONSHIP_GLOBAL );
      		
      	for ( Path path : NODE_TRAVERSAL.traverse( importNode ) )
      	{
      		output += path + "\n";
      	}
       
        
      	System.out.println(output);
    
		
	}//end main

}//end TEST
