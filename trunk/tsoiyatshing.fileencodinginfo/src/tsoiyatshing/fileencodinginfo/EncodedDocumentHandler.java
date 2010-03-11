package tsoiyatshing.fileencodinginfo;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.editors.text.IEncodingSupport;

import com.ibm.icu.text.CharsetMatch;

/**
 * This handler handles editors which support IEncodingSupport for ActiveDocumentAgent.
 * @author Tsoi Yat Shing
 *
 */
class EncodedDocumentHandler implements IActiveDocumentAgentHandler {

	// Invoke the callback on behalf of the agent.
	private IActiveDocumentAgentCallback callback;
	
	// The editor associated with this handler.
	private IEditorPart editor;
	
	// Obtained from the editor.
	private IEncodingSupport encoding_support;
	
	// The encoding setting of the text file.
	private String encoding;

	public EncodedDocumentHandler(IEditorPart part, IActiveDocumentAgentCallback callback) {
		if (callback == null) throw new IllegalArgumentException("callback must not be null.");
		if (part == null) throw new IllegalArgumentException("part must not be null.");
		
		this.callback = callback;
		editor = part;
		encoding_support = (IEncodingSupport) editor.getAdapter(IEncodingSupport.class);
		
		if (encoding_support == null) throw new IllegalArgumentException("part must provide IEncodingSupport.");
		
		update_encoding_info();
	}

	@Override
	public CharsetMatch[] getDetectedEncodings() {
		return null;
	}

	@Override
	public IEditorPart getEditor() {
		return editor;
	}

	@Override
	public String getEncoding() {
		return encoding;
	}

	@Override
	public int getEncodingConfidence() {
		return 0;
	}

	@Override
	public String getName() {
		return editor.getEditorInput().getName();
	}

	@Override
	public void propertyChanged(Object source, int propId) {
		if (!editor.isDirty()) {
			// The document may be just saved.
			if (update_encoding_info()) {
				// Invoke the callback if the encoding information is changed.
				callback.encodingInfoChanged();
			}
		}
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		// The editor's encoding setting may not be updated yet, so do nothing here.
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		// It seems that propertyChanged() can detect encoding setting changes well already.
	}

	@Override
	public void setEncoding(String encoding) {
		// This method should be unused.
		
		encoding_support.setEncoding(encoding);
		
		if (update_encoding_info()) {
			// Invoke the callback if the encoding information is changed.
			callback.encodingInfoChanged();
		}
	}

	/**
	 * Update the encoding information in member variables.
	 * @return true if encoding information is updated.
	 */
	private boolean update_encoding_info() {
		String encoding = null;
		
		encoding = encoding_support.getEncoding();
		if (encoding == null) {
			encoding = encoding_support.getDefaultEncoding();
		}
		
		boolean is_not_updated =
			(encoding == null ? this.encoding == null : encoding.equals(this.encoding));
		
		this.encoding = encoding;
		
		return !is_not_updated;
	}

}
