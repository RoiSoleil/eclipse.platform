/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal;

import java.lang.reflect.InvocationTargetException;
import java.util.ResourceBundle;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileState;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.WorkspaceModifyOperation;


public class AddFromHistoryAction extends BaseCompareAction {

	private static final String BUNDLE_NAME= "org.eclipse.compare.internal.AddFromHistoryAction"; //$NON-NLS-1$

	public AddFromHistoryAction() {
		// empty default implementation
	}

	@Override
	protected boolean isEnabled(ISelection selection) {
		return Utilities.getResources(selection).length == 1;
	}

	@Override
	protected void run(ISelection selection) {

		ResourceBundle bundle= ResourceBundle.getBundle(BUNDLE_NAME);
		String title= Utilities.getString(bundle, "title"); //$NON-NLS-1$

		Shell parentShell= CompareUIPlugin.getShell();
		AddFromHistoryDialog dialog= null;

		Object[] s= Utilities.getResources(selection);

		for (Object o : s) {
			if (o instanceof IContainer container) {
				ProgressMonitorDialog pmdialog= new ProgressMonitorDialog(parentShell);
				IProgressMonitor pm= pmdialog.getProgressMonitor();
				IFile[] states= null;
				try {
					states= container.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, pm);
				} catch (CoreException ex) {
					pm.done();
				}

				// There could be a recently deleted file at the same path as
				// the container. If such a file is the only one to restore, we
				// should not suggest to restore it, so set states to null.
				if (states.length == 1 && states[0].getFullPath().equals(container.getFullPath())) {
					states = null;
				}

				if (states == null || states.length <= 0) {
					String msg= Utilities.getString(bundle, "noLocalHistoryError"); //$NON-NLS-1$
					MessageDialog.openInformation(parentShell, title, msg);
					return;
				}

				if (dialog == null) {
					dialog= new AddFromHistoryDialog(parentShell, bundle);
					dialog.setHelpContextId(ICompareContextIds.ADD_FROM_HISTORY_DIALOG);
				}

				if (dialog.select(container, states)) {
					AddFromHistoryDialog.HistoryInput[] selected = dialog
							.getSelected();
					if (selected != null && selected.length > 0) {
						try {
							updateWorkspace(bundle, parentShell, selected);
						} catch (InterruptedException x) {
							// Do nothing. Operation has been canceled by user.
						} catch (InvocationTargetException x) {
							String reason = x.getTargetException().getMessage();
							MessageDialog.openError(parentShell, title,
									Utilities.getFormattedString(bundle,
											"replaceError", reason)); //$NON-NLS-1$
						}
					}
				}
			}
		}
	}

	void createContainers(IResource resource) throws CoreException {
		IContainer container= resource.getParent();
		if (container instanceof IFolder parent) {
			if (parent != null && !parent.exists()) {
				createContainers(parent);
				parent.create(false, true, null);
			}
		}
	}

	private void updateWorkspace(final ResourceBundle bundle, Shell shell,
					final AddFromHistoryDialog.HistoryInput[] selected)
									throws InvocationTargetException, InterruptedException {

		WorkspaceModifyOperation operation= new WorkspaceModifyOperation() {
			@Override
			public void execute(IProgressMonitor pm) throws InvocationTargetException {
				try {
					String taskName= Utilities.getString(bundle, "taskName"); //$NON-NLS-1$
					pm.beginTask(taskName, selected.length);

					for (AddFromHistoryDialog.HistoryInput s : selected) {
						IFile file = s.fFile;
						IFileState fileState = s.fFileState;
						createContainers(file);
						SubMonitor subMonitor= SubMonitor.convert(pm, 1);
						try {
							file.create(fileState.getContents(), false, subMonitor);
						} finally {
							subMonitor.done();
						}
					}
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} finally {
					pm.done();
				}
			}
		};

		ProgressMonitorDialog pmdialog= new ProgressMonitorDialog(shell);
		pmdialog.run(false, true, operation);
	}
}
