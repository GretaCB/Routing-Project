package org.neo4j.neo4j;

import org.neo4j.graphdb.RelationshipType;

public class OsmRoutingRelationships 
{

	public enum RelTypes implements RelationshipType
	{
	        OSM,
			OSM_WAY,
	        OSM_NODENEXT
	}
	
}//end OsmRoutingRelationships class
