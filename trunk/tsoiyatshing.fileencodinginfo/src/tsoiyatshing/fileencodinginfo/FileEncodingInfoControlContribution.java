package tsoiyatshing.fileencodinginfo;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.INullSelectionListener;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;
import org.eclipse.ui.texteditor.ITextEditor;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

/**
 * Show the file encoding information for the currently editing text file in the workspace.
 * Include the current file encoding and the file encoding as detected by ICU.
 * @author Tsoi Yat Shing
 *
 */
public class FileEncodingInfoControlContribution extends
		WorkbenchWindowControlContribution implements INullSelectionListener,
		IResourceChangeListener {

	// File encoding information is shown for this file.
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
		
		// Get the encoding information of the active text file.
		current_text_file = getActiveTextFile();
		final String current_file_encoding = getCurrentTextFileCharset();
		final CharsetMatch[] charset_match_list = detectCurrentTextFileCharsets();
		String detected_file_encoding = charset_match_list == null ? null : charset_match_list[0].getName();
		int current_file_encoding_confidence = getConfidence(charset_match_list, current_file_encoding);
		int detected_file_encoding_confidence = charset_match_list == null ? 0 : charset_match_list[0].getConfidence();
		
		// Use GridLayout to center the label vertically.
		Composite comp = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		comp.setLayout(layout);
		
		// Set the label.
		Label file_encoding_label = new Label(comp, SWT.CENTER);
		// The file may have been deleted if charset_match_list is null.
		if (current_file_encoding != null && charset_match_list != null) {
			int file_encoding_label_background_color = SWT.COLOR_WIDGET_BACKGROUND;
			if (areCharsetsEqual(current_file_encoding, detected_file_encoding)) {
				file_encoding_label.setText(String.format("%s(%d%%)", current_file_encoding, current_file_encoding_confidence));
			}
			else {
				file_encoding_label.setText(String.format("%s(%d%%) => %s(%d%%)?", current_file_encoding, current_file_encoding_confidence, detected_file_encoding, detected_file_encoding_confidence));
				// Show the label in red color if the confidence of the current file encoding is zero or if the confidence of the detected file encoding is high.
				if (current_file_encoding_confidence == 0 || detected_file_encoding_confidence >= 50) {
					file_encoding_label_background_color = SWT.COLOR_RED;
				}
			}
			file_encoding_label.setBackground(file_encoding_label.getDisplay().getSystemColor(file_encoding_label_background_color));
		}
		else {
			file_encoding_label.setText("");
		}
		
		// Set the popup menu for changing file encoding.
		if (charset_match_list != null) {
			final Menu file_encoding_popup_menu = new Menu(file_encoding_label);
			file_encoding_label.setMenu(file_encoding_popup_menu);
			file_encoding_label.setToolTipText(String.format("Right-click to change the encoding of '%s'", current_text_file.getName()));
			// Add the menu items dynamically.
			file_encoding_popup_menu.addMenuListener(new MenuAdapter() {
				@Override
				public void menuShown(MenuEvent e) {
					// Remove existing menu items.
					for (MenuItem item: file_encoding_popup_menu.getItems()) item.dispose();
					// Add menu items, the charset with the highest confidence is in the bottom.
					for (int i = charset_match_list.length - 1; i >= 0; i--) {
						final CharsetMatch match = charset_match_list[i];
						final MenuItem item = new MenuItem(file_encoding_popup_menu, SWT.RADIO);
						item.setText(match.getName() + "\t(Confidence:" + match.getConfidence() + "%)");
						if (areCharsetsEqual(match.getName(), current_file_encoding)) {
							item.setSelection(true);
						}
						item.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent e) {
								if (item.getSelection()) {
									try {
										// Set the charset.
										FileEncodingInfoControlContribution.this.current_text_file.setCharset(match.getName(), null);
									} catch (CoreException e1) {
										// TODO Auto-generated catch block
										e1.printStackTrace();
									}
								}
							}
						});
					}
				}
			});
		}
		
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
		// Update the encoding information if the active text file changed.
		if (getActiveTextFile() != current_text_file) {
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
		// Do update in the UI thread.
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				getParent().update(true);
			}
		});
	}
	
	/**
	 * Get the file associated with the active text editor.
	 * @return the active text file, or null if there is no active text editor or if the editor input is not IFileEditorInput or if the file does not exist.
	 */
	private IFile getActiveTextFile() {
		IWorkbenchPage page = getWorkbenchWindow().getActivePage();
		IEditorPart part = page == null ? null : page.getActiveEditor();
		if (part != null && part instanceof ITextEditor) {
			ITextEditor editor = (ITextEditor) part;
			if (editor.getEditorInput() instanceof IFileEditorInput) {
				IFile file = ((IFileEditorInput) editor.getEditorInput()).getFile();
				return file.exists() ? file : null;
			}
		}
		return null;
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
	 * Detect the possible charsets of the current text file using ICU.
	 * @return the detected charsets or null.
	 */
	private CharsetMatch[] detectCurrentTextFileCharsets() {
		if (current_text_file != null) {
			try {
				// CharsetDetector.setText() requires that markSupported() == true.
				InputStream in = new BufferedInputStream(current_text_file.getContents(true));
				try {
					CharsetDetector detector = new CharsetDetector();
					detector.setText(in);
					return detector.detectAll();
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
	
	/**
	 * Get the confidence of a charset, given a set of CharsetMatch.
	 * @return the confidence of the charset, or 0 if not founded.
	 */
	private int getConfidence(CharsetMatch[] charset_match_list, String charset) {
		if (charset_match_list == null || charset == null) return 0;
		
		for (CharsetMatch match: charset_match_list) {
			if (areCharsetsEqual(match.getName(), charset)) {
				return match.getConfidence();
			}
		}
		return 0;
	}
	
	/**
	 * Check whether two charset strings really mean the same thing.
	 * For UTF-8, acceptable variants are utf-8, utf8.
	 * For Shift_JIS, acceptable variants are shift-jis, Shift-JIS, shift_jis.
	 * @param a The first charset string.
	 * @param b The second charset string.
	 * @return true/false
	 */
	private boolean areCharsetsEqual(String a, String b) {
		if (a == null || b == null) return false;
		
		try {
			return Charset.forName(a).name().equals(Charset.forName(b).name());
		} catch (IllegalArgumentException e) {
			return false;
		}
	}
}
