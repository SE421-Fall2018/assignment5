package com.se421.cg.algorithms;

import com.ensoftcorp.atlas.core.db.graph.Edge;
import com.ensoftcorp.atlas.core.db.graph.Graph;
import com.ensoftcorp.atlas.core.db.graph.GraphElement.EdgeDirection;
import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.db.set.AtlasHashSet;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.atlas.core.xcsg.XCSG;

public class ClassHierarchyAnalysis {

	public static final String CHA_CALL_EDGE = "CHA_CALL_EDGE";
	
	/**
	 * The main entry point to run the call graph construction
	 */
	public static void run() {
		// some useful graphs / sets
		Q typeHierarchy = Common.universe().edges(XCSG.Supertype);
		Q typeOfEdges = Common.universe().edges(XCSG.TypeOf);
		Q invokedFunctionEdges = Common.universe().edges(XCSG.InvokedFunction);
		Q identityPassedToEdges = Common.universe().edges(XCSG.IdentityPassedTo);
		Q dataFlowEdges = Common.universe().edges(XCSG.DataFlow_Edge);
		Q methodSignatureEdges = Common.universe().edges(XCSG.InvokedSignature);
		Q methods = Common.universe().nodes(XCSG.Method);
		
		// for each method
		for(Node method : methods.eval().nodes()){
			// for each callsite
			AtlasSet<Node> callsites = Common.toQ(method).contained().nodes(XCSG.CallSite).eval().nodes();
			for(Node callsite : callsites){
				if(callsite.taggedWith(XCSG.StaticDispatchCallSite)){
					// static dispatches (calls to constructors or methods marked as static) can be resolved immediately
					Node staticDispatchTargetMethod = invokedFunctionEdges.successors(Common.toQ(callsite)).eval().nodes().one();
					
					// create a CHA_CALL_EDGE from method to static dispatch target method
					findOrCreateCHACallEdge(method, staticDispatchTargetMethod);
				} else if(callsite.taggedWith(XCSG.DynamicDispatchCallSite)){
					// dynamic dispatches require additional analysis to be resolved

					// first get the declared type of the receiver object
					Q thisNode = identityPassedToEdges.predecessors(Common.toQ(callsite));
					Q receiverObject = dataFlowEdges.predecessors(thisNode);
					Q declaredType = typeOfEdges.successors(receiverObject);
					
					// since the dispatch was called on the "declared" type there must be at least one signature
					// (abstract or concrete) in the descendant path (the direct path from Object to the declared path)
					// the nearest method definition is the method definition closest to the declared type (including
					// the declared type itself) while traversing from declared type to Object on the descendant path
					// but an easier way to get this is to use Atlas' InvokedSignature edge to get the nearest method definition
					Edge methodSignatureEdge = methodSignatureEdges.forwardStep(Common.toQ(callsite)).eval().edges().one();
					Node methodSignature = methodSignatureEdge.getNode(EdgeDirection.TO);
					Q dynamicDispatchTargetMethods = Common.toQ(methodSignature);
					
					// subtypes of the declared type can override the nearest target method definition, 
					// so make sure to include all the subtype method definitions
					Q declaredSubtypeHierarchy = typeHierarchy.reverse(declaredType);

					// next perform a reachability analysis (RA) withing the set of subtypes
					Q reachableMethods = Common.toQ(ReachabilityAnalysis.getReachableMethods(callsite, declaredSubtypeHierarchy));
					dynamicDispatchTargetMethods = dynamicDispatchTargetMethods.union(reachableMethods);
					
					// if a method is abstract, then its children must override it, so we can just remove all abstract
					// methods from the graph (this might come into play if nearest matching method definition was abstract)
					// note: its possible for a method to be re-abstracted by a subtype after its been made concrete
					dynamicDispatchTargetMethods = dynamicDispatchTargetMethods.difference(Common.universe().nodes(XCSG.abstractMethod));
					
					// lastly, if the method signature is concrete and the type of the method signature is abstract 
					// and all subtypes override the method signature then the method signature can never be called
					// directly, so remove it from the result
					boolean abstractMethodSignature = methodSignature.taggedWith(XCSG.abstractMethod);
					if(!abstractMethodSignature){
						Q methodSignatureType = Common.toQ(methodSignature).parent();
						boolean abstractMethodSignatureType = methodSignatureType.eval().nodes().one().taggedWith(XCSG.Java.AbstractClass);
						if(abstractMethodSignatureType){
							Q resolvedDispatchConcreteSubTypes = dynamicDispatchTargetMethods.difference(Common.toQ(methodSignature)).parent()
									.difference(Common.universe().nodes(XCSG.Java.AbstractClass));
							if(!resolvedDispatchConcreteSubTypes.eval().nodes().isEmpty()){
								// there are concrete subtypes
								if(declaredSubtypeHierarchy.difference(methodSignatureType, resolvedDispatchConcreteSubTypes).eval().nodes().isEmpty()){
									// all subtypes override method signature, method signature implementation can never be called
									dynamicDispatchTargetMethods = dynamicDispatchTargetMethods.difference(Common.toQ(methodSignature));
								}
							}
						}
					}
					
					// create a CHA_CALL_EDGE from method to each concrete dynamic dispatch target methods
					for(Node resolvedDispatch : dynamicDispatchTargetMethods.eval().nodes()){
						findOrCreateCHACallEdge(method, resolvedDispatch);
					}
				}
			}
		}
	}
	
	/**
	 * Returns a set of reachable methods (methods with the matching signature of the callsite)
	 * Note: This method specifically includes abstract methods
	 * Note: This method restricts the search to a set of types to search
	 * 
	 * @param callsite
	 * @return
	 */
	public static AtlasSet<Node> getReachableDispatchMethods(Node callsite, Q typesToSearch){
		Q methodSignatureEdges = Common.universe().edges(XCSG.InvokedSignature);
		Node methodSignature = methodSignatureEdges.successors(Common.toQ(callsite)).eval().nodes().one();
		if(methodSignature == null) {
			return new AtlasHashSet<Node>();
		} else {
			String signature = (String) methodSignature.getAttr(ReachabilityAnalysis.SIGNATURE);
			if(signature == null){
				return new AtlasHashSet<Node>();
			} else {
				Q matchingMethods = Common.universe().selectNode(ReachabilityAnalysis.SIGNATURE, signature);
				Q candidateMethods = typesToSearch.children().nodes(XCSG.Method)
						.difference(Common.universe().nodes(XCSG.Constructor, XCSG.ClassMethod));
				
				return new AtlasHashSet<Node>(matchingMethods.intersection(candidateMethods).eval().nodes());
			}
		}
	}
		
	private static Edge findOrCreateCHACallEdge(Node from, Node to) {
		Q chaCallEdges = Common.universe().edges(CHA_CALL_EDGE);
		Edge edge = chaCallEdges.betweenStep(Common.toQ(from), Common.toQ(to)).eval().edges().one();
		if(edge == null) {
			edge = Graph.U.createEdge(from, to);
			edge.tag(CHA_CALL_EDGE);
		}
		return edge;
	}
}
