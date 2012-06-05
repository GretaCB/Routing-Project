package org.neo4j.neo4j;

import java.io.IOException;
import java.nio.charset.Charset;
import javax.xml.stream.XMLStreamException;
import org.neo4j.gis.spatial.osm.OSMImporter;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

public class OSMImport 
{
	private static GraphDatabaseService graphDb;
	private static final String DB_PATH = "target/OSMliechtenstein";
	private static String layerName = "liechtenstein";
	private static String osmXmlFilePath = "C:\\Users\\Carol\\Desktop\\GSoC\\osm\\liechtenstein.osm";
	private static int commitInterval = 10000;
	//it means: every 10000 node inserted, commit...save the transaction
	//if you use a very low value, like 1, you'll have a very slow import
	//if you use an high value, you'll get an OutOfMemory error
	
	
	public static void main(String[] args) throws IOException, XMLStreamException
	{
		graphDb = new GraphDatabaseFactory().newEmbeddedDatabase( DB_PATH );
		OSMImporter importer = new OSMImporter( layerName );
		importer.setCharset( Charset.forName( "UTF-8" ) );
	    importer.importFile( graphDb, osmXmlFilePath );
	    importer.reIndex( graphDb, commitInterval );
	    graphDb.shutdown();
	    
	}//end main
	

}//end OSMImport
