/*******************************************************************************
 *  Copyright (c) 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.core.resources.tests;

import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.cdt.core.CProjectNature;
import org.eclipse.cdt.core.resources.ExclusionInstance;
import org.eclipse.cdt.core.resources.ExclusionType;
import org.eclipse.cdt.core.resources.RefreshExclusion;
import org.eclipse.cdt.core.resources.RefreshScopeManager;
import org.eclipse.cdt.core.testplugin.CTestPlugin;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * @author crecoskie
 *
 */
public class RefreshScopeTests extends TestCase {
	
	private IProject fProject;
	private IResource fFolder1;
	private IResource fFolder2;

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		
		// create project
		CTestPlugin.getWorkspace().run(new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				IWorkspaceRoot root = CTestPlugin.getWorkspace().getRoot();
				IProject project = root.getProject("testRefreshScope");
				if (!project.exists()) {
					project.create(null);
				} else {
					project.refreshLocal(IResource.DEPTH_INFINITE, null);
				}
				if (!project.isOpen()) {
					project.open(null);
				}
				if (!project.hasNature(CProjectNature.C_NATURE_ID)) {
					addNatureToProject(project, CProjectNature.C_NATURE_ID, null);
				}
				fProject = project;
			}
		}, null);
		
		
		IWorkspaceRoot root = CTestPlugin.getWorkspace().getRoot();
		IProject project = root.getProject("testRefreshScope");
		
		// create a couple folders
		final IFolder folder1 = project.getFolder("folder1");
		fFolder1 = folder1;
		final IFolder folder2 = project.getFolder("folder2");
		fFolder2 = folder2;
		
		CTestPlugin.getWorkspace().run(new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				
				folder1.create(true, true, monitor);
				folder2.create(true, true, monitor);
			}
		}, null);
		
	}

	private static void addNatureToProject(IProject proj, String natureId, IProgressMonitor monitor) throws CoreException {
		IProjectDescription description = proj.getDescription();
		String[] prevNatures = description.getNatureIds();
		String[] newNatures = new String[prevNatures.length + 1];
		System.arraycopy(prevNatures, 0, newNatures, 0, prevNatures.length);
		newNatures[prevNatures.length] = natureId;
		description.setNatureIds(newNatures);
		proj.setDescription(description, monitor);
	}
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	@Override
	protected void tearDown() throws Exception {
		fProject.delete(true, true, null);
	}
	
	public void testAddDeleteResource() throws CoreException {
		
		
		RefreshScopeManager manager = RefreshScopeManager.getInstance();
		manager.addResourceToRefresh(fProject, fFolder1);
		
		IResource[] resources = manager.getResourcesToRefresh(fProject).toArray(new IResource[0]);
		assertEquals(resources.length, 1);
		assertEquals(fFolder1, resources[0]);
		
		manager.addResourceToRefresh(fProject, fFolder2);
		resources = manager.getResourcesToRefresh(fProject).toArray(new IResource[0]);
		assertEquals(resources.length, 2);
		assertEquals(fFolder1, resources[0]);
		assertEquals(fFolder2, resources[1]);
		
		// first try deleting a resource that was never added... the project
		manager.deleteResourceToRefresh(fProject, fProject);
		IResource[] resourcesAfterDelete = manager.getResourcesToRefresh(fProject).toArray(new IResource[0]);
		assertEquals(resourcesAfterDelete.length, 2);
		assertEquals(fFolder1, resources[0]);
		assertEquals(fFolder2, resources[1]);
		
		
		// now delete the resources from the manager one by one
		manager.deleteResourceToRefresh(fProject, resources[1]);
		resourcesAfterDelete = manager.getResourcesToRefresh(fProject).toArray(new IResource[0]);
		assertEquals(resourcesAfterDelete.length, 1);
		assertEquals(resourcesAfterDelete[0], resources[0]);
		
		manager.deleteResourceToRefresh(fProject, resources[0]);
		resourcesAfterDelete = manager.getResourcesToRefresh(fProject).toArray(new IResource[0]);
		assertEquals(resourcesAfterDelete.length, 0);
		
	}
	
	public void testSetResourcesToRefresh() {
		RefreshScopeManager manager = RefreshScopeManager.getInstance();
		List<IResource> resources = new LinkedList<IResource>();
		resources.add(fFolder1);
		resources.add(fFolder2);
		manager.setResourcesToRefresh(fProject, resources);
		
		IResource[] resourcesAfterSet = manager.getResourcesToRefresh(fProject).toArray(new IResource[0]);
		assertEquals(resourcesAfterSet.length, 2);
		assertEquals(fFolder1, resourcesAfterSet[0]);
		assertEquals(fFolder2, resourcesAfterSet[1]);
		
		manager.clearResourcesToRefresh(fProject);
		
	}
	
	public void testAddRemoveExclusion() {
		RefreshScopeManager manager = RefreshScopeManager.getInstance();
		manager.addResourceToRefresh(fProject, fProject);
		RefreshExclusion exclusion1 = new TestExclusion();
		manager.addExclusion(fProject, exclusion1);
		RefreshExclusion exclusion2 = new TestExclusion();
		manager.addExclusion(fProject, exclusion2);
		
		// make sure the exclusions are there
		List<RefreshExclusion> exclusionsList = manager.getExclusions(fProject);
		RefreshExclusion[] exclusionsArray = exclusionsList.toArray(new RefreshExclusion[0]);
		assertEquals(exclusionsArray.length, 2);
		assertEquals(exclusionsArray[0], exclusion1);
		assertEquals(exclusionsArray[1], exclusion2);
		
		// remove the exclusions one by one
		manager.removeExclusion(fProject, exclusion2);
		exclusionsList = manager.getExclusions(fProject);
		exclusionsArray = exclusionsList.toArray(new RefreshExclusion[0]);
		assertEquals(exclusionsArray.length, 1);
		assertEquals(exclusionsArray[0], exclusion1);
		
		manager.removeExclusion(fProject, exclusion1);
		exclusionsList = manager.getExclusions(fProject);
		exclusionsArray = exclusionsList.toArray(new RefreshExclusion[0]);
		assertEquals(exclusionsArray.length, 0);
		
	}
	
	public void testPersistAndLoad() {
		RefreshScopeManager manager = RefreshScopeManager.getInstance();
		manager.addResourceToRefresh(fProject, fProject);
		
		RefreshExclusion exclusion1 = new TestExclusion();
		manager.addExclusion(fProject, exclusion1);
		RefreshExclusion exclusion2 = new TestExclusion();
		manager.addExclusion(fProject, exclusion2);
		
		// add a nested exclusion to the first exclusion
		RefreshExclusion exclusion3 = new TestExclusion();
		exclusion1.addNestedExclusion(exclusion3);
		
		// add an instance to the second exclusion
		ExclusionInstance instance = new ExclusionInstance();
		instance.setDisplayString("foo");
		instance.setResource(fFolder2);
		instance.setExclusionType(ExclusionType.RESOURCE);
		instance.setParentExclusion(exclusion2);
		
		try {
			manager.persistSettings();
		} catch (CoreException e) {
			fail();
		}
		
		// now clear all the settings out of the manager
		manager.clearAllData();
		
		// now load the settings
		try {
			manager.loadSettings();
		} catch (CoreException e) {
			fail();
		}
		
		// make sure we got the same stuff we saved
		
		// the project should be set to refresh its root
		List<IResource> resources = manager.getResourcesToRefresh(fProject);
		assertEquals(resources.size(), 1);
		assertEquals(resources.toArray(new IResource[0])[0], fProject);
		
		// there should be 2 top-level exclusions
		List<RefreshExclusion> exclusions = manager.getExclusions(fProject);
		assertEquals(exclusions.size(), 2);
		RefreshExclusion[] exclusionsArray = exclusions.toArray(new RefreshExclusion[0]);
		
		// both exclusions should have parent resource set to the project
		assertEquals(exclusionsArray[0].getParentResource(), fProject);
		assertEquals(exclusionsArray[1].getParentResource(), fProject);
		
		// the first exclusion should have one nested exclusion
		List<RefreshExclusion> nestedExclusions1 = exclusionsArray[0].getNestedExclusions();
		assertEquals(nestedExclusions1.size(), 1);
		RefreshExclusion[] nestedExclusionsArray =  nestedExclusions1.toArray(new RefreshExclusion[0]);
		// the nested exclusion should have its parent exclusion set properly
		assertEquals(nestedExclusionsArray[0].getParentExclusion(), exclusionsArray[0]);
		
		// the second exclusion should have no nested exclusions
		List<RefreshExclusion> nestedExclusions2 = exclusionsArray[1].getNestedExclusions();
		assertEquals(nestedExclusions2.size(), 0);
		
		// the second exclusion should have an instance
		List<ExclusionInstance> instances = exclusionsArray[1].getExclusionInstances();
		assertEquals(instances.size(), 1);
		ExclusionInstance[] instancesArray = instances.toArray(new ExclusionInstance[0]);
		ExclusionInstance loadedInstance = instancesArray[0];
		
		// check the contents of the instance
		assertEquals(exclusionsArray[1], loadedInstance.getParentExclusion());
		assertEquals("foo", loadedInstance.getDisplayString());
		assertEquals(fFolder2, loadedInstance.getResource());
		assertEquals(ExclusionType.RESOURCE, loadedInstance.getExclusionType());
		
		// cleanup
		manager.clearAllData();
	}
	

}