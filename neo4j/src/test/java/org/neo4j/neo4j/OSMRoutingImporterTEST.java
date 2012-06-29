package org.neo4j.neo4j;
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
		
		OSMRoutingImporter importer = new OSMRoutingImporter(graphDbPath);
	
		importer.importXML(osmXmlFilePath);
		

	}//end main

}//end TEST
