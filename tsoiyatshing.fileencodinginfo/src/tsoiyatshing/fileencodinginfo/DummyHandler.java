package tsoiyatshing.fileencodinginfo;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;

import com.ibm.icu.text.CharsetMatch;

/**
 * This is a dummy handler for ActiveDocumentAgent.
 * @author Tsoi Yat Shing
 *
 */
class DummyHandler implements IActiveDocumentAgentHandler {

	// The editor associated with this handler.
	private IEditorPart editor;
	
	public DummyHandler(IEditorPart part, IActiveDocumentAgentCallback callback) {
		editor = part;
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
		return null;
	}

	@Override
	public int getEncodingConfidence() {
		return 0;
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public void propertyChanged(Object source, int propId) {
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
	}

	@Override
	public void setEncoding(String encoding) {
	}

}
