package field.core.ui.protect.protocols.fieldstring;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringBufferInputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public class Handler extends URLStreamHandler {

	@Override
	protected URLConnection openConnection(final URL u) throws IOException {
		final String p = FieldStrings.known.get(u.getPath());
		return new URLConnection(u) {

			@Override
			public void connect() throws IOException {
				if (p == null)
					throw new IOException(" no string for '" + u.getPath() + "'");
			}

			@Override
			public InputStream getInputStream() throws IOException {
				if (p == null)
					throw new IOException(" no string for '" + u.getPath() + "', strings are <"+FieldStrings.known.keySet()+">");
				return new ByteArrayInputStream(p.getBytes());
			}
		};
	}

}
