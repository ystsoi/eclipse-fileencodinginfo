package tsoiyatshing.fileencodinginfo;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.INullSelectionListener;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.IEncodingSupport;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

import com.ibm.icu.text.CharsetMatch;


/**
 * This agent tries to provide the encoding of the document of the active editor. It also provides method to set the encoding of the document.
 * @author Tsoi Yat Shing
 *
 */
public class ActiveDocumentAgent implements INullSelectionListener, IResourceChangeListener, IPropertyListener {
	// Callback for this agent.
	private IActiveDocumentAgentCallback callback;
	
	// The current handler for the agent.
	private IActiveDocumentAgentHandler current_handler;
	
	// Indicate whether the agent has started monitoring the encoding of the active document.
	private boolean is_started = false;
	
	public ActiveDocumentAgent(IActiveDocumentAgentCallback callback) {
		if (callback == null) throw new IllegalArgumentException("Please provide a callback.");
		
		this.callback = callback;
		
		// Initialize the current handler to a dummy handler, so that we do not need to check whether it is null.
		setCurrentHandler(getHandler(null));
	}
	
	/**
	 * Get the active editor.
	 * @return the active editor, or null if there is no active editor.
	 */
	private IEditorPart getActiveEditor() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window != null) {
			IWorkbenchPage page = window.getActivePage();
			if (page != null) {
				return page.getActiveEditor();
			}
		}
		return null;
	}
	
	/**
	 * Get the detected encodings (and their confidences) of the active document using ICU, if supported by the editor and the editor input.
	 * @return the detected encodings or null.
	 */
	public CharsetMatch[] getDetectedEncodings() {
		return current_handler.getDetectedEncodings();
	}
	
	/**
	 * Get the encoding setting of the active document, if supported by the editor.
	 * @return the encoding setting or null.
	 */
	public String getEncoding() {
		return current_handler.getEncoding();
	}
	
	/**
	 * Get the confidence of the encoding of the active document, if supported by the editor and the editor input.
	 * @return the confidence of the encoding returned by getEncoding(), if getDetectedEncodings() provides a confidence value for that encoding.
	 */
	public int getEncodingConfidence() {
		return current_handler.getEncodingConfidence();
	}
	
	/**
	 * Get a handler for an editor.
	 * @return a specific handler, or DummyHandler if there is no specific handler for an editor.
	 */
	private IActiveDocumentAgentHandler getHandler(IEditorPart part) {
		if (part != null) {
			if (part.getAdapter(IEncodingSupport.class) != null) {
				if (part instanceof ITextEditor) {
					ITextEditor editor = (ITextEditor) part;
					IEditorInput editor_input = editor.getEditorInput();
					if (editor_input instanceof IFileEditorInput) {
						return new WorkspaceTextFileHandler(part, callback);
					}
					else if (editor_input instanceof FileStoreEditorInput) {
						return new NonWorkspaceTextFileHandler(part, callback);
					}
				}
				return new EncodedDocumentHandler(part, callback);
			}
		}
		return new DummyHandler(part, callback);
	}
	
	/**
	 * Get the name of the active document, if supported by the editor and the editor input.
	 * @return the name or null.
	 */
	public String getName() {
		return current_handler.getName();
	}
	
	/**
	 * Check whether the active document is dirty or not.
	 * @return true/false
	 */
	public boolean isDocumentDirty() {
		return current_handler == null ? false : (current_handler.getEditor() == null ? false : current_handler.getEditor().isDirty());
	}
	
	@Override
	public void propertyChanged(Object source, int propId) {
		if (propId == IEditorPart.PROP_INPUT) {
			// The current handler may not be able to handle the new editor input, so get a new handler for the active editor, and invoke the callback.
			setCurrentHandler(getHandler(getActiveEditor()));
			callback.encodingInfoChanged();
		}
		else {
			// Pass the event to the handler.
			current_handler.propertyChanged(source, propId);
		}
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		current_handler.resourceChanged(event);
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		IEditorPart active_editor = getActiveEditor();
		if (active_editor == current_handler.getEditor()) {
			// Pass the event to the handler.
			current_handler.selectionChanged(part, selection);
		}
		else {
			// Get a new handler for the active editor, and invoke the callback.
			// Assume that the handler has been initialized from the editor, so no need to pass the event to the handler.
			setCurrentHandler(getHandler(active_editor));
			callback.encodingInfoChanged();
		}
	}

	/**
	 * Change the current handler.
	 * This method helps to add/remove IPropertyListener as needed.
	 * @param handler
	 */
	private void setCurrentHandler(IActiveDocumentAgentHandler handler) {
		if (handler == null) throw new IllegalArgumentException("handler must not be null.");
		
		// Remove IPropertyListener from the old editor.
		if (current_handler != null) {
			IEditorPart editor = current_handler.getEditor();
			if (editor != null) {
				editor.removePropertyListener(this);
			}
		}
		
		current_handler = handler;
		
		// Add IPropertyListener to the new editor.
		IEditorPart editor = current_handler.getEditor();
		if (editor != null) {
			editor.addPropertyListener(this);
		}
	}
	
	/**
	 * Set the encoding of the active document, if supported by the editor.
	 */
	public void setEncoding(String encoding) {
		current_handler.setEncoding(encoding);
	}
	
	/**
	 * Start to monitor the encoding of the active document, if not started yet.
	 * There should be an active workbench window.
	 */
	public void start() {
		if (!is_started) {
			IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			if (window != null) {
				is_started = true;
				
				// Update the current handler.
				// Not invoke the callback during start.
				setCurrentHandler(getHandler(getActiveEditor()));
				
				// Add listeners.
				ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
				window.getSelectionService().addPostSelectionListener(this);
			}
		}
	}

	/**
	 * Stop to monitor the encoding of the active document, if started.
	 * There should be an active workbench window.
	 */
	public void stop() {
		if (is_started) {
			IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			if (window != null) {
				is_started = false;
				
				// Remove listeners.
				ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
				window.getSelectionService().removePostSelectionListener(this);
				
				// Reset the current handler to a dummy handler, which will remove IPropertyListener if added.
				setCurrentHandler(getHandler(null));
			}
		}
	}
}
