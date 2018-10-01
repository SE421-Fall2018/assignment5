package com.se421.cg.regression;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.internal.wizards.datatransfer.ZipLeveledStructureProvider;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;
import org.osgi.framework.Bundle;

import com.se421.cg.regression.log.Log;

/**
 * Helper class for dealing with Eclipse workspaces
 * 
 * @author Ben Holland
 */
@SuppressWarnings("restriction")
public class WorkspaceUtils {

	private WorkspaceUtils() {}
	
	/**
	 * Returns a project in the workspace for the given project name
	 * @param projectName
	 * @return
	 */
	public static IProject getProject(String projectName){
		return ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
	}
	
	/**
	 * Converts a File to an Eclipse IFile 
	 * Source: http://stackoverflow.com/questions/960746/how-to-convert-from-file-to-ifile-in-java-for-files-outside-the-project
	 * 
	 * @param file
	 * @return
	 */
	public static IFile getFile(File file) {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IPath location = Path.fromOSString(file.getAbsolutePath());
		IFile iFile = workspace.getRoot().getFileForLocation(location);
		return iFile;
	}
	
	/**
	 * Converts an IFile to a Java File
	 * 
	 * @param file
	 * @return
	 * @throws CoreException 
	 */
	public static File getFile(IFile iFile) throws CoreException {
		URI uri; 

		// get the file uri, accound for symbolic links
		if(!iFile.isLinked()){
			uri = iFile.getLocationURI();
		} else {
			uri = iFile.getRawLocationURI();
		}

		// get the native file using Eclipse File System
		File file;
		if(uri != null){
			file = EFS.getStore(uri).toLocalFile(0, new NullProgressMonitor());
		} else {
			// Eclipse is weird...this last resort should work
			file = new File(iFile.getFullPath().toOSString());
		}
		
		return file;
	}
	
	public static IStatus deleteProject(IProject project) {
		if (project != null && project.exists())
			try {
				project.delete(true, true, new NullProgressMonitor());
			} catch (CoreException e) {
				Log.error("Could not delete project", e);
				return new Status(Status.ERROR, Activator.PLUGIN_ID, "Could not delete project", e);
			}
		return Status.OK_STATUS;
	}
	
	public static Job importProjectFromArchivedResource(final Bundle bundle, final String relativeArchivedProjectPath, final String projectName) {
		Job j = new Job("Import Resource Project") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					// XXX: does not support multiple projects, e.g. if OpenJDK is a separate project
					IProject project = ProjectUtils.importProject(monitor, bundle, relativeArchivedProjectPath, projectName);
					if(!project.exists()) {
						throw new IOException("Imported project could not be found in workspace.");
					}
				} catch (CoreException | IOException | InvocationTargetException | InterruptedException e) {
					Log.error("Error importing project", e);
					return Status.CANCEL_STATUS;
				}
				return Status.OK_STATUS;
			}
		};
		j.schedule();
		return j;
	}
	
	/**
	 * Helper utilities for importing projects from resource archives
	 * 
	 * @author Jon Mathews
	 * @author Ben Holland
	 */
	@SuppressWarnings("rawtypes")
	private static class ProjectUtils {
		
		private ProjectUtils() {}

		public static IProject importProject(IProgressMonitor monitor, Bundle bundle, String bundlePath, String newProjectName) throws InvocationTargetException, InterruptedException, CoreException, IOException {
			return new ProjectUtils().importProjectFromArchive(monitor, bundle, bundlePath, newProjectName);
		}
		
		/**
		 * @param monitor
		 * @param bundle The bundle which stores the archived project
		 * @param bundlePath bundle-relative path to archive
		 * @param newProjectName
		 * @return
		 * @throws IOException
		 * @throws InvocationTargetException
		 * @throws InterruptedException
		 * @throws CoreException
		 */
		public IProject importProjectFromArchive(IProgressMonitor monitor,
				Bundle bundle, String bundlePath, String newProjectName) throws IOException,
				InvocationTargetException, InterruptedException, CoreException {
			
			URL entry = bundle.getEntry(bundlePath);
			if (entry == null) {
				throw new IllegalStateException("Archive not found: " + bundlePath);
			}
			URL fileURL = FileLocator.toFileURL(entry);
			
			File pathZip;
			try {
				// this constructor ensures the URI is correctly escaped
				// http://stackoverflow.com/questions/14676966/escape-result-of-filelocator-resolveurl/14677157#14677157
					
				URI fileURI = new URI(fileURL.getProtocol(), fileURL.getPath(), null);
				
				pathZip = URIUtil.toFile(fileURI);
			} catch (URISyntaxException e) {
				throw new IllegalStateException("Archive cannot be loaded", e);
			}
			
			return importProjectFromArchive(monitor, pathZip, newProjectName);
		}
		
		private IProject importProjectFromArchive(IProgressMonitor monitor, File pathZip, String newProjectName) throws InvocationTargetException, InterruptedException, CoreException, IOException {
//			http://stackoverflow.com/questions/12484128/how-do-i-import-an-eclipse-project-from-a-zip-file-programmatically
			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			IProjectDescription newProjectDescription = workspace.newProjectDescription(newProjectName);
			IProject newProject = workspace.getRoot().getProject(newProjectName);
			
			if (newProject.exists()) {
				final String message = "The project '" + newProject.getName() + "' has already been imported; please delete it first if you want an updated version.  Proceeding with old version in the workspace.";
				Log.info(message);
				UIJob hey = new UIJob("Project already imported") {
					
					@Override
					public IStatus runInUIThread(IProgressMonitor monitor) {
						MessageDialog.openInformation(getDisplay().getActiveShell(), "", message);
						return Status.OK_STATUS;
					}
				};
				hey.schedule();
				
				return newProject;
			}
			
			newProject.create(newProjectDescription, null);
			newProject.open(null);
			

			IOverwriteQuery overwriteQuery = new IOverwriteQuery() {
			    public String queryOverwrite(String file) { return ALL; }
			};
			ZipLeveledStructureProvider provider = new ZipLeveledStructureProvider(new ZipFile(pathZip));
			
			List<Object> fileSystemObjects = new ArrayList<Object>();
			List children = provider.getChildren(provider.getRoot());
			Iterator itr = children.iterator();
			ZipEntry source = null;
			while (itr.hasNext()) {
			    ZipEntry nextElement = (ZipEntry) itr.next();
			    
			    // have to find the ZipEntry instance created by the Provider, because .equals() is not defined using the entry name
			    if ((newProjectName + "/").equals(nextElement.getName())) {
			    	source = nextElement;
			    }
			    
			    // NOTE: ImportOperation will fail on empty directories; ignore /bin as a stopgap
			    if (nextElement.toString().contains("/bin"))
			    	continue;
			    
				fileSystemObjects.add(nextElement);
			}
			if (source == null)
				throw new RuntimeException("Project not found in archive");
			
			ImportOperation importOperation = new ImportOperation(newProject.getFullPath(), source, provider, overwriteQuery, fileSystemObjects);
			importOperation.setCreateContainerStructure(false);
			importOperation.run(monitor);
			
			return newProject;
		}

	}


}