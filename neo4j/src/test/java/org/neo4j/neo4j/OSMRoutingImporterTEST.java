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
		
		// importXML shouldn't be a static method
		importer.importXML(osmXmlFilePath);
		
		// right now importXML doesn't import anything :(
	}//end main

}//end TEST
