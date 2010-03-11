package tsoiyatshing.fileencodinginfo;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

/**
 * Provide encoding related utility functions.
 * @author Tsoi Yat Shing
 *
 */
public class EncodingUtil {
	/**
	 * Check whether two charset strings really mean the same thing.
	 * For UTF-8, acceptable variants are utf-8, utf8.
	 * For Shift_JIS, acceptable variants are shift-jis, Shift-JIS, shift_jis.
	 * @param a The first charset string.
	 * @param b The second charset string.
	 * @return true/false
	 */
	public static boolean areCharsetsEqual(String a, String b) {
		if (a == null || b == null) return false;
		
		try {
			return Charset.forName(a).name().equals(Charset.forName(b).name());
		} catch (IllegalArgumentException e) {
			return false;
		}
	}
	
	/**
	 * Detect the possible charsets of an input stream using ICU.
	 * @param in The input stream, should close the stream before return.
	 * @return the detected charsets or null.
	 */
	public static CharsetMatch[] detectCharsets(InputStream in) {
		if (in != null) {
			try {
				// CharsetDetector.setText() requires that markSupported() == true.
				InputStream bin = new BufferedInputStream(in);
				try {
					CharsetDetector detector = new CharsetDetector();
					detector.setText(bin);
					return detector.detectAll();
				}
				finally {
					bin.close();
				}
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
	public static int getConfidence(CharsetMatch[] charset_match_list, String charset) {
		if (charset_match_list == null || charset == null) return 0;
		
		for (CharsetMatch match: charset_match_list) {
			if (areCharsetsEqual(match.getName(), charset)) {
				return match.getConfidence();
			}
		}
		return 0;
	}
	
	/**
	 * Check whether an input stream can be decoded by an encoding.
	 * @param in The input stream, should close the stream before return.
	 * @return true/false.
	 */
	public static boolean isDecodable(InputStream in, String encoding) {
		if (in != null) {
			try {
				try {
					if (encoding != null) {
						ByteArrayOutputStream out = new ByteArrayOutputStream();
						byte[] buffer = new byte[4096];
						int len;
						while((len = in.read(buffer)) > 0) {
							out.write(buffer, 0, len);
						}
						
						// Try to decode the input stream using the encoding.
						try {
							Charset.forName(encoding).newDecoder().decode(ByteBuffer.wrap(out.toByteArray()));
							return true;
						} catch (IOException e) {
						}
					}
				}
				finally {
					in.close();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return false;
	}
}
