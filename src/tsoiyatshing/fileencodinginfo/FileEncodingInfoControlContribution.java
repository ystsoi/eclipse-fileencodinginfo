package tsoiyatshing.fileencodinginfo;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.INullSelectionListener;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;
import org.eclipse.ui.texteditor.ITextEditor;

import com.ibm.icu.text.CharsetDetector;

/**
 * Show the file encoding information for the currently editing text file in the workspace.
 * Include the current file encoding and the file encoding as detected by ICU.
 * @author Tsoi Yat Shing
 *
 */
public class FileEncodingInfoControlContribution extends
		WorkbenchWindowControlContribution implements INullSelectionListener,
		IResourceChangeListener {

	// The file associated with the current text editor.
	private IFile current_text_file = null;
	
	// Listeners.
	private ISelectionListener selection_listener;
	private IResourceChangeListener resource_change_listener;
	
	public FileEncodingInfoControlContribution() {
	}

	public FileEncodingInfoControlContribution(String id) {
		super(id);
	}

	/**
	 * This method will be called each time to update the label, as resize cannot be made to work.
	 */
	@Override
	protected Control createControl(Composite parent) {
		// Add listeners, if needed.
		addListeners();
		
		// Get the encoding information.
		String file_encoding = getCurrentTextFileCharset();
		String detected_file_encoding = detectCurrentTextFileCharset();
		
		// Give some room around the label.
		Composite comp = new Composite(parent, SWT.NONE);
		FillLayout layout = new FillLayout();
		layout.marginHeight = 2;
		layout.marginWidth = 2;
		comp.setLayout(layout);
		
		// Set the label.
		Label file_encoding_label = new Label(comp, SWT.CENTER);
		if (file_encoding != null || detected_file_encoding != null) {
			file_encoding_label.setText(file_encoding + "/" + detected_file_encoding);
		}
		else {
			file_encoding_label.setText("");
		}
		file_encoding_label.setToolTipText(file_encoding_label.getText());
		file_encoding_label.setBackground(file_encoding_label.getDisplay().getSystemColor(file_encoding == null || file_encoding.equals(detected_file_encoding) ? SWT.COLOR_WIDGET_BACKGROUND : SWT.COLOR_RED));
		
		return comp;
	}

	/**
	 * Add the listeners if not added yet.
	 */
	private void addListeners() {
		if (resource_change_listener == null) {
			resource_change_listener = this;
			selection_listener = this;
			ResourcesPlugin.getWorkspace().addResourceChangeListener(resource_change_listener, IResourceChangeEvent.POST_CHANGE);
			getWorkbenchWindow().getSelectionService().addPostSelectionListener(selection_listener);
		}
	}
	
	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		// Check whether the user switches to edit another text file.
		IFile new_current_text_file = null;
		if (part != null && part instanceof ITextEditor) {
			ITextEditor editor = (ITextEditor) part;
			if (editor.getEditorInput() instanceof IFileEditorInput) {
				new_current_text_file = ((IFileEditorInput) editor.getEditorInput()).getFile();
			}
		}
		
		// Update the encoding information if the current text file changed.
		if (new_current_text_file != current_text_file) {
			current_text_file = new_current_text_file;
			updateEncodingInfo();
		}
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		// Check whether the current text file is changed.
		if (current_text_file != null) {
			if (event.getDelta().findMember(current_text_file.getFullPath()) != null) {
				// Update the encoding information if changed.
				updateEncodingInfo();
			}
		}
	}

	@Override
	public void dispose() {
		// Remove listeners.
		if (resource_change_listener != null) {
			ResourcesPlugin.getWorkspace().removeResourceChangeListener(resource_change_listener);
			getWorkbenchWindow().getSelectionService().removePostSelectionListener(selection_listener);
		}
		super.dispose();
	}

	@Override
	public boolean isDynamic() {
		// Call createControl() on update.
		return true;
	}

	/**
	 * Update the encoding information in the label.
	 * Like after the user switches to another editor.
	 */
	private void updateEncodingInfo() {
		// Cannot make resize work, need to call createControl() again.
		getParent().update(true);
	}
	
	/**
	 * Get the charset of the current text file.
	 * @return current_text_file.getCharset() or null.
	 */
	private String getCurrentTextFileCharset() {
		if (current_text_file != null) {
			try {
				return current_text_file.getCharset();
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}
	
	/**
	 * Detect the charset of the current text file using ICU.
	 * @return the detected charset or "".
	 */
	private String detectCurrentTextFileCharset() {
		if (current_text_file != null) {
			try {
				// CharsetDetector.setText() requires that markSupported() == true.
				InputStream in = new BufferedInputStream(current_text_file.getContents(true));
				try {
					CharsetDetector detector = new CharsetDetector();
					detector.setText(in);
					return detector.detect().getName();
				}
				finally {
					in.close();
				}
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}
}
