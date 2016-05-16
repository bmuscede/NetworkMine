package ca.uwaterloo.cs.cs846Boa.bmuscede.network;

import java.util.HashMap;
import java.util.Map;
import edu.uci.ics.jung.algorithms.scoring.VertexScorer;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;

//TODO: Do something about the edge weights.
public class ReachabilityCentrality<V, E> implements VertexScorer<V, Double> {
	private Graph<V, E> graph;
	private DijkstraShortestPath<V,E> dijkstra;
	
	public ReachabilityCentrality(Graph<V, E> graph){
		this.graph = graph;
		dijkstra = new DijkstraShortestPath<V, E>(graph);
	}
	
	@Override
	public Double getVertexScore(V vertex) {
		//Generate the hops.
		Map<Integer, Integer> hops = generateHops(vertex);
		
		//Compute the final score.
		double finalSum = 0;
		for (Map.Entry<Integer, Integer> entry : hops.entrySet()){
			finalSum += entry.getValue() * (1d / entry.getKey());
		}
		
		return finalSum;
	}

	private Map<Integer, Integer> generateHops(V vertex) {
		Map<Integer, Integer> results = new HashMap<Integer, Integer>();
		
		//Start by iterating through the vertices.
		int hops;
		for (V otherVertex : graph.getVertices()){
			if (vertex == otherVertex) continue;
			
			//Gets the shortest path and counts the edges.
			hops = dijkstra.getPath(vertex, otherVertex).size();
			
			//Inserts it into the graph.
			if (results.containsKey(hops)){
				results.put(hops, results.get(hops) + 1);
			} else {
				results.put(hops, 1);
			}
		}
		
		return results;
	}

}
