package org.neo4j.neo4j;

import org.neo4j.graphalgo.CommonEvaluators;
import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphalgo.EstimateEvaluator;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Expander;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.Traversal;
import org.neo4j.neo4j.OsmRoutingRelationships.RelTypes;
import org.neo4j.neo4j.OSMRoutingImporter;

public class OSMRouting{
	
	protected GraphDatabaseService graphDb;
	protected String startNodeID;
	protected String endNodeID;
	
	
	public OSMRouting(GraphDatabaseService graphDb, String startNodeID, String endNodeID) {
		
		this.graphDb = graphDb;
		this.startNodeID = startNodeID;
		this.endNodeID = endNodeID;
	}
	
	 public void createRoute() {
	
	    	Transaction tx = graphDb.beginTx();
	    	
	    	try{
	    	System.out.println("In createRoute method...");
	    	EstimateEvaluator<Double> estimateEval = CommonEvaluators.geoEstimateEvaluator(
	    	            "lat", "lon" );
	    	
	    	Expander relExpander = Traversal.expanderForTypes(
	                RelTypes.ONEWAY_NEXT, Direction.OUTGOING, RelTypes.BIDIRECTIONAL_NEXT, Direction.BOTH );
	    	relExpander.add( RelTypes.ONEWAY_NEXT, Direction.OUTGOING );
	    	relExpander.add( RelTypes.BIDIRECTIONAL_NEXT, Direction.BOTH );
	    	
	    	CostEvaluator<Double> costEval = CommonEvaluators.doubleCostEvaluator("distance_in_meters");
	    	
	    	PathFinder<WeightedPath> finder = GraphAlgoFactory.aStar(
	               relExpander , costEval, estimateEval );
	    	
	    	
	    	//Test class nodes to route (passed from constructor)
			Node startNode = OSMRoutingImporter.getOsmNode(startNodeID);
			Node endNode = OSMRoutingImporter.getOsmNode(endNodeID);
	    	
			System.out.println(startNode.getProperty("id")); 
			System.out.println(endNode.getProperty("id"));

		  	Path route = finder.findSinglePath(startNode, endNode); 
		  	
		  	for ( Node node : route.nodes() ) {
		  		System.out.println( node.getProperty( "id" ) );
	        }
		  	
	    	}//end try
	    	
	    	finally {
	    		tx.finish();
	        }
		  	  
		}//end createRoute()
		
}
