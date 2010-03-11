package tsoiyatshing.fileencodinginfo;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.editors.text.IEncodingSupport;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

import com.ibm.icu.text.CharsetMatch;

/**
 * This handler handles non-workspace text file for ActiveDocumentAgent.
 * @author Tsoi Yat Shing
 *
 */
class NonWorkspaceTextFileHandler implements IActiveDocumentAgentHandler {

	// Invoke the callback on behalf of the agent.
	private IActiveDocumentAgentCallback callback;
	
	// The editor associated with this handler.
	private ITextEditor editor;
	
	// The text file associated with the editor.
	private IFileStore text_file_store = null;
	
	// The encoding setting of the text file.
	private String encoding;
	
	// The confidence of the encoding.
	private int encoding_confidence;
	
	// The detected encodings of the text file.
	private CharsetMatch[] detected_encodings;

	public NonWorkspaceTextFileHandler(IEditorPart part, IActiveDocumentAgentCallback callback) {
		if (callback == null) throw new IllegalArgumentException("callback must not be null.");
		if (part == null || !(part instanceof ITextEditor)) throw new IllegalArgumentException("part must be an ITextEditor.");
		
		this.callback = callback;
		editor = (ITextEditor) part;
		
		if (!(editor.getEditorInput() instanceof FileStoreEditorInput)) throw new IllegalArgumentException("part must provide FileStoreEditorInput.");
		
		try {
			text_file_store = EFS.getStore(((FileStoreEditorInput) editor.getEditorInput()).getURI());
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		update_encoding_info();
	}

	@Override
	public CharsetMatch[] getDetectedEncodings() {
		return detected_encodings;
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
		return encoding_confidence;
	}

	@Override
	public String getName() {
		return text_file_store == null ? null : text_file_store.getName();
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
		// There is no IResourceChangeEvent for non-workspace file.
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		// It seems that propertyChanged() can detect changes well already.
	}

	@Override
	public void setEncoding(String encoding) {
		IEncodingSupport encoding_support = (IEncodingSupport) editor.getAdapter(IEncodingSupport.class);
		if (encoding_support != null) {
			encoding_support.setEncoding(encoding);
			
			// There is no IResourceChangeEvent for non-workspace file, so invoke the callback directly.
			if (update_encoding_info()) {
				// Invoke the callback if the encoding information is changed.
				callback.encodingInfoChanged();
			}
		}
	}

	/**
	 * Update the encoding information in member variables.
	 * @return true if encoding information is updated.
	 */
	private boolean update_encoding_info() {
		String encoding = null;
		CharsetMatch[] detected_encodings = null;
		int encoding_confidence = 0;
		
		IEncodingSupport encoding_support = (IEncodingSupport) editor.getAdapter(IEncodingSupport.class);
		if (encoding_support != null) {
			encoding = encoding_support.getEncoding();
			if (encoding == null) {
				encoding = encoding_support.getDefaultEncoding();
			}
		}
		
		if (text_file_store != null) {
			try {
				detected_encodings = EncodingUtil.detectCharsets(text_file_store.openInputStream(EFS.NONE, null));
				encoding_confidence = EncodingUtil.getConfidence(detected_encodings, encoding);
				
				// Check whether the text file can really be decoded by the encoding, and adjust the confidence.
				boolean is_text_file_decodable = EncodingUtil.isDecodable(text_file_store.openInputStream(EFS.NONE, null), encoding);
				if (!is_text_file_decodable) {
					// CharsetDetector may not read all the input data, so the confidence may not be zero even if the text cannot be decoded.
					encoding_confidence = 0;
				}
				else if (encoding_confidence == 0) {
					// CharsetDetector does not support all encodings, so the confidence may be zero even if the text can be decoded.
					encoding_confidence = 1;
				}
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		// TODO It seems that is_not_updated is always false.
		boolean is_not_updated =
			(encoding == null ? this.encoding == null : encoding.equals(this.encoding))
			&& (encoding_confidence == this.encoding_confidence)
			&& (detected_encodings == null ? this.detected_encodings == null : detected_encodings.equals(this.detected_encodings));
		
		this.encoding = encoding;
		this.detected_encodings = detected_encodings;
		this.encoding_confidence = encoding_confidence;
		
		return !is_not_updated;
	}

}
