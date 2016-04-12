package tsoiyatshing.fileencodinginfo;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IStorageEditorInput;

import com.ibm.icu.text.CharsetMatch;

/**
 * This handler handles IStorageEditorInput for ActiveDocumentAgent.
 * Assume that the ITextEditor supports IEncodingSupport too.
 * @author Tsoi Yat Shing
 *
 */
class StorageEditorInputHandler extends EncodedDocumentHandler {

	// The storage object associated with the editor.
	private IStorage storage = null;
	
	// The confidence of the encoding.
	private int encoding_confidence;
	
	// The detected encodings of the text file.
	private CharsetMatch[] detected_encodings;

	public StorageEditorInputHandler(IEditorPart part, IActiveDocumentAgentCallback callback) throws CoreException {
		super(part, callback);
		
		if (!(part.getEditorInput() instanceof IStorageEditorInput)) throw new IllegalArgumentException("part must provide IStorageEditorInput.");
		
		storage = ((IStorageEditorInput) part.getEditorInput()).getStorage();
		
		updateEncodingInfoPrivately();
	}

	@Override
	public CharsetMatch[] getDetectedEncodings() {
		return detected_encodings;
	}

	@Override
	public int getEncodingConfidence() {
		return encoding_confidence;
	}

	/**
	 * Update the encoding information in member variables.
	 * This method may be overrided, but should be called by the sub-class.
	 * @return true if the encoding information is updated.
	 */
	protected boolean updateEncodingInfo() {
		return super.updateEncodingInfo() | updateEncodingInfoPrivately();
	}

	/**
	 * Update the encoding information in private member variables.
	 * @return true if the encoding information is updated.
	 */
	private boolean updateEncodingInfoPrivately() {
		// Get the updated encoding setting.
		String encoding = getEncoding();
		
		// Do detection.
		CharsetMatch[] detected_encodings = null;
		int encoding_confidence = 0;
		
		try {
			detected_encodings = EncodingUtil.detectCharsets(storage.getContents());
			encoding_confidence = EncodingUtil.getConfidence(detected_encodings, encoding);
			
			// Check whether the text can really be decoded by the encoding, and adjust the confidence.
			boolean is_text_decodable = EncodingUtil.isDecodable(storage.getContents(), encoding);
			if (!is_text_decodable) {
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
		
		this.detected_encodings = detected_encodings;
		this.encoding_confidence = encoding_confidence;
		
		// Just assume that the encoding information is updated.
		return true;
	}

}
