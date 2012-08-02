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
	
	public OSMRouting(GraphDatabaseService graphDb)
	{
		
		this.graphDb = graphDb;
	}
	
	 public void createRoute()
		{
	    	//Not sure when I need to start a new transaction...and I'm assuming this is the reason I'm getting a NotFoundException
	    	Transaction tx = graphDb.beginTx();
	    	
	    	try{
	    	System.out.println("In creatRoute method...");
	    	EstimateEvaluator<Double> estimateEval = CommonEvaluators.geoEstimateEvaluator(
	    	            "lat", "lon" );
	    	System.out.println("After EstimateEvaluator");
	    	
	    	Expander relExpander = Traversal.expanderForTypes(
	                RelTypes.OSM_NODENEXT, Direction.BOTH );
	    	relExpander.add( RelTypes.OSM_NODENEXT, Direction.BOTH );
	    	System.out.println("After relationship expander");
	    	
	    	CostEvaluator<Double> costEval = CommonEvaluators.doubleCostEvaluator("distance_in_meters");
	    	System.out.println("After Cost Evaluator");
	    	
	    	PathFinder<WeightedPath> finder = GraphAlgoFactory.aStar(
	               relExpander , costEval, estimateEval );
	    	System.out.println("After PathFinder");
	    	
	    	//String startNodeID = "278451834";
			//String endNodeID = "268222979";
			/*
			JFrame frame = new JFrame("Nodes to Route:");
			startNodeID = JOptionPane.showInputDialog(frame, "Enter nodeID for the Start Node: ");
			endNodeID = JOptionPane.showInputDialog(frame, "Enter nodeID for the End Node: ");
		  	*/
			
			Node startNode = OSMRoutingImporter.getOsmNode("278451834");
			Node endNode = OSMRoutingImporter.getOsmNode("268222979");
			//long startNodeid = Long.parseLong(startNodeID);
		  	//long endNodeid = Long.parseLong(endNodeID);
			System.out.println(startNode.getProperty("id")); 
			System.out.println(endNode.getProperty("id"));
		  	//Node start = graphDb.getNodeById(startNode);
		  	//Node end = graphDb.getNodeById(endNode);
		  	Path route = finder.findSinglePath(startNode, endNode); 
		  	
		  	for ( Node node : route.nodes() )
	        {
		  		System.out.println( node.getProperty( "id" ) );
	        }
		  	
	    	}//end try
	    	
	    	finally
	        {
	    		tx.finish();
	        }
		  	  
		}//end createRoute()
		
}
