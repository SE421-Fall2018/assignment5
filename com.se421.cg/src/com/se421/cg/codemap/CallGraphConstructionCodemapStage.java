package com.se421.cg.codemap;

import java.text.DecimalFormat;

import org.eclipse.core.runtime.IProgressMonitor;

import com.ensoftcorp.atlas.core.indexing.providers.ToolboxIndexingStage;
import com.se421.cg.algorithms.ClassHierarchyAnalysis;
import com.se421.cg.algorithms.ReachabilityAnalysis;
import com.se421.cg.log.Log;

public class CallGraphConstructionCodemapStage implements ToolboxIndexingStage {

	@Override
	public String displayName() {
		return "CHA Call Graph Construction";
	}

	@Override
	public void performIndexing(IProgressMonitor monitor) {
		try {
			Log.info("Starting RA call graph construction");
			long start = System.nanoTime();
			ReachabilityAnalysis.run();
			long stop = System.nanoTime();
			DecimalFormat decimalFormat = new DecimalFormat("#.##");
			double time = (stop - start)/1000.0/1000.0; // ms
			if(time < 100) {
				Log.info("Finished RA call graph construction in " + decimalFormat.format(time) + "ms");
			} else {
				time = (stop - start)/1000.0/1000.0/1000.0; // s
				if(time < 60) {
					Log.info("Finished RA call graph construction in " + decimalFormat.format(time) + "s");
				} else {
					time = (stop - start)/1000.0/1000.0/1000.0/60.0; // m
					if(time < 60) {
						Log.info("Finished RA call graph construction in " + decimalFormat.format(time) + "m");
					} else {
						time = (stop - start)/1000.0/1000.0/1000.0/60.0/60.0; // h
						Log.info("Finished RA call graph construction in " + decimalFormat.format(time) + "h");
					}
				}
			}
		} catch (Exception e) {
			Log.error("Error running RA call graph construction codemap stage", e);
		}
		
		try {
			Log.info("Starting CHA call graph construction");
			long start = System.nanoTime();
			ClassHierarchyAnalysis.run();
			long stop = System.nanoTime();
			DecimalFormat decimalFormat = new DecimalFormat("#.##");
			double time = (stop - start)/1000.0/1000.0; // ms
			if(time < 100) {
				Log.info("Finished CHA call graph construction in " + decimalFormat.format(time) + "ms");
			} else {
				time = (stop - start)/1000.0/1000.0/1000.0; // s
				if(time < 60) {
					Log.info("Finished CHA call graph construction in " + decimalFormat.format(time) + "s");
				} else {
					time = (stop - start)/1000.0/1000.0/1000.0/60.0; // m
					if(time < 60) {
						Log.info("Finished CHA call graph construction in " + decimalFormat.format(time) + "m");
					} else {
						time = (stop - start)/1000.0/1000.0/1000.0/60.0/60.0; // h
						Log.info("Finished CHA call graph construction in " + decimalFormat.format(time) + "h");
					}
				}
			}
		} catch (Exception e) {
			Log.error("Error running CHA call graph construction codemap stage", e);
		}
	}
	
}
