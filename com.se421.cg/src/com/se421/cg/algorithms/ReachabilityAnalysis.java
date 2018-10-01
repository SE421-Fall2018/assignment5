package com.se421.cg.algorithms;

import com.ensoftcorp.atlas.core.db.graph.Edge;
import com.ensoftcorp.atlas.core.db.graph.Graph;
import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.db.set.AtlasHashSet;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.atlas.core.xcsg.XCSG;

public class ReachabilityAnalysis {

	public static final String RA_CALL_EDGE = "RA_CALL_EDGE";
	
	/**
	 * An undocumented, but very useful Atlas attribute.
	 * 
	 * This attribute key corresponds to the raw Java/Jimple function signature as a string,
	 * which includes the return type, function name, and parameters.
	 */
	public static final String SIGNATURE = "##signature"; //$NON-NLS-1$
	
	/**
	 * The main entry point to run the call graph construction
	 */
	public static void run() {
		Q methods = Common.universe().nodes(XCSG.Method);
		// TODO: implement: Reachability Analysis (RA)
		// RA adds a call edge from a method to another method in any type whose method signature matches the callsite signature
		// You may wish to references the Class Hierarchy Analysis (which is an enhancement to RA)
		
		// for each method
			// for each callsite in the method
				// for each reachable method add an RA edge from the method to the reachable method
	}
	
	/**
	 * Finds or creates a new edge. A new edge will not be added if an edge already exists
	 * 
	 * @param from
	 * @param to
	 * @return
	 */
	private static Edge findOrCreateRACallEdge(Node from, Node to) {
		Q chaCallEdges = Common.universe().edges(RA_CALL_EDGE);
		Edge edge = chaCallEdges.betweenStep(Common.toQ(from), Common.toQ(to)).eval().edges().one();
		if(edge == null) {
			edge = Graph.U.createEdge(from, to);
			edge.tag(RA_CALL_EDGE);
		}
		return edge;
	}
	
	/**
	 * Returns a set of reachable methods (methods with the matching signature of the callsite)
	 * Note: This method specifically includes abstract methods
	 * 
	 * @param callsite
	 * @return
	 */
	public static AtlasSet<Node> getReachableMethods(Node callsite){
		Q allTypes = Common.universe().nodes(XCSG.Type);
		return getReachableMethods(callsite, allTypes);
	}
	
	/**
	 * Returns a set of reachable methods (methods with the matching signature of the callsite)
	 * Note: This method specifically includes abstract methods and static methods
	 * Note: This method restricts the search to a set of types to search
	 * 
	 * @param callsite
	 * @return
	 */
	public static AtlasSet<Node> getReachableMethods(Node callsite, Q typesToSearch){
		Q invokedFunctionEdges = Common.universe().edges(XCSG.InvokedFunction);
		Node staticDispatchTargetMethod = invokedFunctionEdges.successors(Common.toQ(callsite)).eval().nodes().one();
		
		AtlasSet<Node> result = new AtlasHashSet<Node>();
		if(staticDispatchTargetMethod != null) {
			result.add(staticDispatchTargetMethod);
		}
		
		Q methodSignatureEdges = Common.universe().edges(XCSG.InvokedSignature);
		Node methodSignature = methodSignatureEdges.successors(Common.toQ(callsite)).eval().nodes().one();
		if(methodSignature == null) {
			return result;
		} else {
			String signature = (String) methodSignature.getAttr(SIGNATURE);
			if(signature == null){
				return result;
			} else {
				Q matchingMethods = Common.universe().selectNode(SIGNATURE, signature);
				Q candidateMethods = typesToSearch.children().nodes(XCSG.Method)
						.difference(Common.universe().nodes(XCSG.Constructor, XCSG.ClassMethod));		
				result.addAll(matchingMethods.intersection(candidateMethods).eval().nodes());
				return result;
			}
		}
	}
	
}
