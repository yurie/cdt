/*******************************************************************************
 * Copyright (c) 2011, 2012 Institute for Software, HSR Hochschule fuer Technik  
 * Rapperswil, University of applied sciences and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html  
 * 
 * Contributors: 
 * 		Martin Schwab & Thomas Kallenberg - initial API and implementation
 * 		Sergey Prigogin (Google)
 ******************************************************************************/
package org.eclipse.cdt.internal.ui.refactoring.togglefunction;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.index.IIndexManager;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;

import org.eclipse.cdt.internal.ui.refactoring.CRefactoring;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;

/**
 * Determines whether a valid function was selected by the user to be able to
 * run the appropriate strategy for moving the function body to another
 * position.
 */
public class ToggleRefactoring extends CRefactoring {
	private ITextSelection selection;
	private IToggleRefactoringStrategy strategy;
	private ToggleRefactoringContext context;
	
	public ToggleRefactoring(ICElement element, ITextSelection selection, ICProject project) {
		super(element, selection, project);
		if (selection == null || tu.getResource() == null || project == null)
			initStatus.addFatalError(Messages.ToggleRefactoring_InvalidSelection);
		this.selection = selection;
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		try {
			pm.subTask(Messages.ToggleRefactoring_WaitingForIndexer);
			prepareIndexer(pm);
			pm.subTask(Messages.ToggleRefactoring_AnalyseSelection);
			context = new ToggleRefactoringContext(refactoringContext, getIndex(), tu, selection);
			strategy = new ToggleStrategyFactory(context).getAppropriateStategy();
		} catch (NotSupportedException e) {
			initStatus.addFatalError(e.getMessage());
		}
		return initStatus;
	}

	private void prepareIndexer(IProgressMonitor pm) throws CoreException  {
		IIndexManager im = CCorePlugin.getIndexManager();
		while (!im.isProjectIndexed(project)) {
			im.joinIndexer(500, pm);
			if (pm.isCanceled())
				throw new NotSupportedException(Messages.ToggleRefactoring_NoIndex);
		}
		if (!im.isProjectIndexed(project))
			throw new NotSupportedException(Messages.ToggleRefactoring_NoIndex);
	}

	@Override
	protected void collectModifications(IProgressMonitor pm, ModificationCollector modifications)
			throws CoreException {
		pm.subTask(Messages.ToggleRefactoring_CalculateModifications);
		strategy.run(modifications);
	}

	@Override
	protected RefactoringDescriptor getRefactoringDescriptor() {
		return null;  // Refactoring history is not supported.
	}

	public ToggleRefactoringContext getContext() {
		return context;
	}
}
