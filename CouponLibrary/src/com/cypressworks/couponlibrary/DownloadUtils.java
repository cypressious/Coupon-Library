package com.cypressworks.couponlibrary;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import org.apache.http.util.ByteArrayBuffer;

/**
 * 
 * @author Kirill Rakhman
 */
public class DownloadUtils {

	public static String downloadHTTPSString(final URL url) throws IOException {
		final HttpsURLConnection conn = (HttpsURLConnection) url
				.openConnection();
		conn.setHostnameVerifier(new HostnameVerifier() {

			@Override
			public boolean verify(final String hostname,
					final SSLSession session) {
				return true;
			}
		});
		conn.setReadTimeout(10000);

		return downloadString(conn);
	}

	public static String downloadHTTPString(final URL url) throws IOException {
		final HttpURLConnection conn = (HttpURLConnection) url.openConnection();

		conn.setReadTimeout(10000);

		return downloadString(conn);
	}

	private static String downloadString(final HttpURLConnection conn)
			throws IOException {
		String html;

		try {
			if (conn.getResponseCode() == 404) {
				return "";
			}

			final InputStream is = conn.getInputStream();
			final BufferedInputStream bis = new BufferedInputStream(is);
			final ByteArrayBuffer baf = new ByteArrayBuffer(50);

			int current = 0;
			while ((current = bis.read()) != -1) {
				baf.append((byte) current);
			}

			/* Convert the Bytes read to a String. */
			html = new String(baf.toByteArray());

		} finally {
			conn.disconnect();
		}

		return html;
	}
}
