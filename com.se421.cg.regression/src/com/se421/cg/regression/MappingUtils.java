package com.se421.cg.regression;

import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IProject;

import com.ensoftcorp.atlas.core.index.ProjectPropertiesUtil;
import com.ensoftcorp.atlas.core.indexing.IIndexListener;
import com.ensoftcorp.atlas.core.indexing.IMappingSettings;
import com.ensoftcorp.atlas.core.indexing.IndexingUtil;
import com.ensoftcorp.atlas.core.licensing.AtlasLicenseException;

/**
 * A wrapper around Atlas indexing utils with added listeners for error handling.
 * 
 * @author Ben Holland
 */
public class MappingUtils {

	private MappingUtils() {}
	
	private static class IndexerError extends Exception {
		private static final long serialVersionUID = 1L;

		public IndexerError(Throwable t) {
			super(t);
		}
	}
	
	private static class IndexerErrorListener implements IIndexListener {

		private Throwable t = null;

		public boolean hasCaughtThrowable() {
			return t != null;
		}

		public Throwable getCaughtThrowable() {
			return t;
		}

		@Override
		public void indexOperationCancelled(IndexOperation io) {}

		@Override
		public void indexOperationError(IndexOperation io, Throwable t) {
			this.t = t;
		}

		@Override
		public void indexOperationStarted(IndexOperation io) {}

		@Override
		public void indexOperationComplete(IndexOperation io) {}

		@Override
		public void indexOperationScheduled(IndexOperation io) {}
	};

	/**
	 * Index the workspace (blocking mode and throws index errors)
	 * @throws AtlasLicenseException 
	 * 
	 * @throws Throwable
	 */
	public static void mapWorkspace() throws IndexerError, AtlasLicenseException {
		IndexerErrorListener errorListener = new IndexerErrorListener();
		IndexingUtil.addListener(errorListener);
		IndexingUtil.indexWorkspace(true);
		IndexingUtil.removeListener(errorListener);
		if (errorListener.hasCaughtThrowable()) {
			try {
				throw errorListener.getCaughtThrowable();
			} catch (Throwable t) {
				throw new IndexerError(t);
			}
		}
	}

	/**
	 * Configures a project for indexing
	 * @param project
	 * @throws AtlasLicenseException
	 * @throws IndexerError 
	 */
	public static void mapProject(IProject project) throws AtlasLicenseException, IndexerError {
		// disable indexing for all projects
		List<IProject> allEnabledProjects = ProjectPropertiesUtil.getAllEnabledProjects();
		ProjectPropertiesUtil.setIndexingEnabledAndDisabled(Collections.<IProject>emptySet(), allEnabledProjects);
		
		// enable indexing for this project
		List<IProject> ourProjects = Collections.singletonList(project);
		ProjectPropertiesUtil.setIndexingEnabledAndDisabled(ourProjects, Collections.<IProject>emptySet());
	
		IndexerErrorListener errorListener = new IndexerErrorListener();
		IndexingUtil.addListener(errorListener);
		
		/*
		 * Index only the specified projects with the specified settings.
		 * 
		 * @param saveIndex schedule saving of index?
		 * @param indexingSettings if null, default settings
		 * @param projects List of projects to index
		 * @throws AtlasLicenseException 
		 * 
		 * @see IMappingSettings
		 */
		// TODO: Investigate: using null for indexingSettings causes null pointers in Atlas so using an empty set instead, its unclear if this should be the default
		IndexingUtil.indexWithSettings(/*saveIndex*/false, /*indexingSettings*/Collections.<IMappingSettings>emptySet(), ourProjects.toArray(new IProject[1]));
		
		IndexingUtil.removeListener(errorListener);
		if (errorListener.hasCaughtThrowable()) {
			try {
				throw errorListener.getCaughtThrowable();
			} catch (Throwable t) {
				throw new IndexerError(t);
			}
		}
	}
}
