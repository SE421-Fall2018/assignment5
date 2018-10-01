package com.se421.cg.ui;

import com.ensoftcorp.atlas.core.markup.Markup;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.atlas.core.script.FrontierStyledResult;
import com.ensoftcorp.atlas.core.script.StyledResult;
import com.ensoftcorp.atlas.core.xcsg.XCSG;
import com.ensoftcorp.atlas.ui.scripts.selections.FilteringAtlasSmartViewScript;
import com.ensoftcorp.atlas.ui.scripts.selections.IExplorableScript;
import com.ensoftcorp.atlas.ui.scripts.selections.IResizableScript;
import com.ensoftcorp.atlas.ui.scripts.util.SimpleScriptUtil;
import com.ensoftcorp.atlas.ui.selection.event.FrontierEdgeExploreEvent;
import com.ensoftcorp.atlas.ui.selection.event.IAtlasSelectionEvent;
import com.se421.cg.algorithms.ClassHiearchyAnalysis;

public class CHACallGraphSmartView extends FilteringAtlasSmartViewScript implements IResizableScript, IExplorableScript {

	@Override
	protected String[] getSupportedNodeTags() {
		return new String[]{ XCSG.Function };
	}

	@Override
	protected String[] getSupportedEdgeTags() {
		return NOTHING;
	}
	
	@Override
	public String getTitle() {
		return "CHA Call Graph";
	}
	
	@Override
	public FrontierStyledResult explore(FrontierEdgeExploreEvent event, FrontierStyledResult oldResult) {
		return SimpleScriptUtil.explore(this, event, oldResult);
	}

	@Override
	public FrontierStyledResult evaluate(IAtlasSelectionEvent event, int reverse, int forward) {
		Q filteredSelection = filter(event.getSelection());

		// get the call graph
		Q cg = Common.universe().edges(ClassHiearchyAnalysis.CHA_CALL_EDGE);
		
		// compute what to show for current steps
		Q f = filteredSelection.forwardStepOn(cg, forward);
		Q r = filteredSelection.reverseStepOn(cg, reverse);
		Q result = f.union(r).union(filteredSelection);
		
		// compute what is on the frontier
		Q frontierForward = filteredSelection.forwardStepOn(cg, forward+1);
		Q frontierReverse = filteredSelection.reverseStepOn(cg, reverse+1);
		frontierForward = frontierForward.retainEdges().differenceEdges(result);
		frontierReverse = frontierReverse.retainEdges().differenceEdges(result);

		return new com.ensoftcorp.atlas.core.script.FrontierStyledResult(result, frontierReverse, frontierForward, new Markup());
	}

	@Override
	public int getDefaultStepBottom() {
		return 1;
	}

	@Override
	public int getDefaultStepTop() {
		return 1;
	}

	@Override
	protected StyledResult selectionChanged(IAtlasSelectionEvent input, Q filteredSelection) {
		return null;
	}

}
