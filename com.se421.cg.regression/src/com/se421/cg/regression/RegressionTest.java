/**
 * 
 */
package com.se421.cg.regression;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.osgi.framework.Bundle;

import com.se421.cg.regression.log.Log;

/**
 * A helper class for importing, mapping, and removing known projects before and after tests
 * 
 * @author Ben Holland
 */
public class RegressionTest {
	
	public static void setUpBeforeClass(Bundle bundle, String relativeArchivedProjectPath, String projectName) throws Exception {
		// delete the project if it exists already
		IProject project = WorkspaceUtils.getProject(projectName);
		if(project != null && project.exists()) {
			try {
				// refresh the project files
				project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
				// delete project
				WorkspaceUtils.deleteProject(project);
			} catch (Throwable e) {
				Log.error("Could not delete project: " + projectName, e);
			}
		}
		
		// import a fresh copy of the project
		try {
			Job job = WorkspaceUtils.importProjectFromArchivedResource(bundle, relativeArchivedProjectPath, projectName);
			job.join(); // block and wait until import is complete
			project = WorkspaceUtils.getProject(projectName);
			if(project != null && project.exists()) {
				// refresh the project files
				project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
				// map the project
				MappingUtils.mapProject(project);
			} else {
				throw new RuntimeException("Error importing test project: " + relativeArchivedProjectPath);
			}			
		} catch (Exception e) {
			Log.error("Error searching for project:" + relativeArchivedProjectPath, e);
		}
	}

}
