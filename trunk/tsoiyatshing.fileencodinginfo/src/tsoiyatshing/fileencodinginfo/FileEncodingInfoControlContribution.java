package tsoiyatshing.fileencodinginfo;

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
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;

import com.ibm.icu.text.CharsetMatch;

/**
 * Show the file encoding information for the active document.
 * Include the current file encoding and the file encoding as detected by ICU.
 * @author Tsoi Yat Shing
 *
 */
public class FileEncodingInfoControlContribution extends
		WorkbenchWindowControlContribution implements IActiveDocumentAgentCallback {

	// The agent is responsible for monitoring the encoding information of the active document.
	private ActiveDocumentAgent agent = new ActiveDocumentAgent(this);
	
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
		// Start the agent, if needed.
		agent.start();
		
		// Get the encoding information of the active document.
		final String current_file_encoding = agent.getEncoding();
		final CharsetMatch[] charset_match_list = agent.getDetectedEncodings();
		String detected_file_encoding = charset_match_list == null ? null : charset_match_list[0].getName();
		int current_file_encoding_confidence = agent.getEncodingConfidence();
		int detected_file_encoding_confidence = charset_match_list == null ? 0 : charset_match_list[0].getConfidence();
		
		// Use GridLayout to center the label vertically.
		Composite comp = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		comp.setLayout(layout);
		
		// Set the label.
		Label file_encoding_label = new Label(comp, SWT.CENTER);
		if (current_file_encoding != null) {
			int file_encoding_label_background_color = SWT.COLOR_WIDGET_BACKGROUND;
			if (charset_match_list == null) {
				// No detected encoding.
				file_encoding_label.setText(String.format("%s(undetected)", current_file_encoding));
			}
			else if (EncodingUtil.areCharsetsEqual(current_file_encoding, detected_file_encoding)) {
				file_encoding_label.setText(String.format("%s(%d%%)", current_file_encoding, current_file_encoding_confidence));
				// Show the label in red color if the confidence of the current file encoding is zero.
				if (current_file_encoding_confidence == 0) {
					file_encoding_label_background_color = SWT.COLOR_RED;
				}
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
			file_encoding_label.setToolTipText(String.format("Right-click to change the encoding of '%s'", agent.getName()));
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
						if (EncodingUtil.areCharsetsEqual(match.getName(), current_file_encoding)) {
							item.setSelection(true);
						}
						item.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent e) {
								if (item.getSelection()) {
									// Set the charset.
									FileEncodingInfoControlContribution.this.agent.setEncoding(match.getName());
								}
							}
						});
					}
				}
			});
		}
		
		return comp;
	}

	@Override
	public void dispose() {
		// Stop the agent.
		agent.stop();
		
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
	public void encodingInfoChanged() {
		// Cannot make resize work, need to call createControl() again.
		// Do update in the UI thread.
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				getParent().update(true);
			}
		});
	}
}
