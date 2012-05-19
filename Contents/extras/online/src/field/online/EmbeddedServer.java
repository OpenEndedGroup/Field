package field.online;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.NotYetConnectedException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketServer;
import org.java_websocket.handshake.ClientHandshake;
import org.python.google.common.io.CharStreams;

import field.core.StandardFluidSheet;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.plugins.help.NanoHTTPD;
import field.core.plugins.help.NanoHTTPD.Response;
import field.core.plugins.log.Logging.iLoggingEvent;
import field.core.util.PythonCallableMap;
import field.graphics.core.Base.iGeometry;
import field.graphics.core.Base.iLongGeometry;
import field.graphics.core.BasicGeometry.VertexBuffer;
import field.launch.iUpdateable;
import field.namespace.generic.Bind.iFunction;
import field.util.HashMapOfLists;

public class EmbeddedServer implements iUpdateable {

	String page = "";

	String init = "";
	String boot = "";

	String start = "";

	public class Coalesce {
		int group;
		String contents; 
		public ByteBuffer buffer;
	}

	List<Coalesce> contents = new ArrayList<Coalesce>();
	HashMap<Integer, Integer> cursors = new HashMap<Integer, Integer>();

	Object contentsLock = new Object();

	NanoHTTPD server;

	int uniq = 0;

	List<String> error = new ArrayList<String>();
	List<String> print = new ArrayList<String>();

	static public interface Handler {
		public Response serve(Response r, String uri, String metod, java.util.Properties header, java.util.Properties parms);

		public boolean isTransient();
	}

	HashMapOfLists<String, Handler> handlers = new HashMapOfLists<String, EmbeddedServer.Handler>();
	HashMap<String, PythonCallableMap> dataHandler = new HashMap<String, PythonCallableMap>();

	List<String> fileSearchPaths = new ArrayList<String>();

	public WebSocketServer websocketServer;

	public EmbeddedServer(int port) {
		try {
			server = new NanoHTTPD(port) {
				public Response serve(String uri, String method, java.util.Properties header, java.util.Properties parms) {

					Object id = parms.get("id");

					if (id == null) {
						id = "" + (uniq++);
					}

					if (uri.equals("/field/init"))
						return new Response(HTTP_OK, null, page + "\n" + init.replace("///ID///", "" + id));
					if (uri.equals("/field/boot"))
						return new Response(HTTP_OK, null, boot.replace("///ID///", "" + id));
					if (uri.equals("/field/start"))
						return new Response(HTTP_OK, null, start.replace("///ID///", "" + id));
					if (uri.equals("/field/update")) {
						return new Response(HTTP_OK, null, ""+getUpdateForID(Integer.parseInt((String) id)));
					}

					if (uri.startsWith("/field/element/")) {

						String e = uri.substring("/field/element/".length());

						String[] m = e.split("/");
						if (m.length == 1) {
							return new Response(HTTP_NOTFOUND, null, "couldn't find " + e + " (malformed)");
						} else {
							String found = findElementAndProperty(m[0], m[1]);
							if (found == null)
								return new Response(HTTP_NOTFOUND, null, "couldn't find " + e);
							return new Response(HTTP_OK, null, found);
						}
					}

					if (uri.startsWith("/field/filesystem/")) {

						String e = uri.substring("/field/filesystem/".length());

						for (String s : fileSearchPaths) {
							File ff = new File(s + "/" + e);
							if (ff.exists()) {
								try {
									return new Response(HTTP_OK, null, new BufferedInputStream(new FileInputStream(ff)));
								} catch (FileNotFoundException e1) {
									e1.printStackTrace();
								}
							}
						}
						return new Response(HTTP_NOTFOUND, null, "couldn't find " + e);
					}

					if (uri.equals("/field/error")) {
						synchronized (contentsLock) {
							error.add((String) parms.get("text"));
						}
						return new Response(HTTP_OK, null, "");
					}
					if (uri.equals("/field/print")) {
						synchronized (contentsLock) {
							print.add((String) parms.get("text"));
						}
						return new Response(HTTP_OK, null, "");
					}

					if (uri.startsWith("/field/run/")) {
						String name = uri.replace("/field/run/", "");
						InputStream ans = findAndRun(name, parms);
						if (ans == null)
							return new Response(HTTP_NOTFOUND, null, "no element called <" + name + ">");
						return new Response(HTTP_OK, null, ans);
					}

					System.out.println(" -- looking up handlers for <" + uri + ">");

					synchronized (handlers) {

						Collection<Handler> m = handlers.get(uri);

						System.out.println(" handlers for <" + uri + "> are <" + m + "> <" + handlers + ">");
						if (m != null) {
							Iterator<Handler> mi = m.iterator();
							Response r = null;
							while (mi.hasNext()) {
								Handler h = mi.next();
								r = h.serve(r, uri, method, header, parms);
								if (h.isTransient())
									mi.remove();
							}
							return r;
						}
					}

					return new Response(HTTP_NOTFOUND, null, "");
				};
			};

			websocketServer = new WebSocketServer(new InetSocketAddress(8081)) {

				@Override
				public void onOpen(WebSocket arg0, ClientHandshake arg1) {
				}

				@Override
				public void onMessage(final WebSocket arg0, String uri) {

					System.out.println(" WSS :" + uri);

					Properties parms = new Properties();
					int qmi = uri.indexOf('?');
					if (qmi >= 0) {
						try {
							NanoHTTPD.decodeParms(uri.substring(qmi + 1), parms);
							uri = NanoHTTPD.decodePercent(uri.substring(0, qmi));
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					Object id = parms.get("id");

					System.out.println(" ID = " + id);

					if (id == null || ((String) id).trim().length() == 0) {
						id = "" + (uniq++);
					}

					if (uri.equals("/field/init")) {
						respond(arg0, page + "\n" + init.replace("///ID///", "" + id));
						// return new Response(HTTP_OK,
						// null, page + "\n" +
						// init.replace("///ID///", "" +
						// id));
					}
					if (uri.equals("/field/boot")) {
						respond(arg0, boot.replace("///ID///", "" + id));
					}
					if (uri.equals("/field/start"))
						respond(arg0, start.replace("///ID///", "" + id));
					if (uri.equals("/field/update")) {

						getUpdateForIDContinuation(Integer.parseInt((String) id), new iFunction<Object, Object>() {
							@Override
							public Object f(Object in) {
								if (in instanceof String)
									respond(arg0, (String) in);
								else if (in instanceof ByteBuffer)
									sendBuffer(arg0, (ByteBuffer) in);

								return null;
							}
						});

					}

					if (uri.startsWith("/field/element/")) {

						String e = uri.substring("/field/element/".length());

						String[] m = e.split("/");
						if (m.length == 1) {

							// return new
							// Response(HTTP_NOTFOUND,
							// null,
							// "couldn't find " + e
							// + " (malformed)");

						} else {
							String found = findElementAndProperty(m[0], m[1]);
							// if (found == null)
							// return new
							// Response(HTTP_NOTFOUND,
							// null,
							// "couldn't find " +
							// e);

							respond(arg0, found);
						}
					}

					if (uri.startsWith("/field/filesystem/")) {

						String e = uri.substring("/field/filesystem/".length());

						for (String s : fileSearchPaths) {
							File ff = new File(s + "/" + e);
							if (ff.exists()) {
								try {
									respond(arg0, new BufferedInputStream(new FileInputStream(ff)));
								} catch (FileNotFoundException e1) {
									e1.printStackTrace();
								}
							}
						}
						// return new
						// Response(HTTP_NOTFOUND, null,
						// "couldn't find " + e);
					}

					if (uri.equals("/field/error")) {
						synchronized (contentsLock) {
							error.add((String) parms.get("text"));
						}
						// return new Response(HTTP_OK,
						// null, "");
					}
					if (uri.equals("/field/print")) {
						synchronized (contentsLock) {
							print.add((String) parms.get("text"));
						}
						// return new Response(HTTP_OK,
						// null, "");
					}

					if (uri.startsWith("/field/run/")) {
						String name = uri.replace("/field/run/", "");
						InputStream ans = findAndRun(name, parms);
						// if (ans == null)
						// return new
						// Response(HTTP_NOTFOUND, null,
						// "no element called <" + name
						// + ">");
						respond(arg0, ans);
					}

					System.out.println(" -- looking up handlers for <" + uri + ">");

					synchronized (handlers) {

						Collection<Handler> m = handlers.get(uri);

						System.out.println(" handlers for <" + uri + "> are <" + m + "> <" + handlers + ">");
						if (m != null) {
							Iterator<Handler> mi = m.iterator();
							Response r = null;
							while (mi.hasNext()) {
								Handler h = mi.next();
								r = h.serve(r, uri, "websocket", new Properties(), parms);
								if (h.isTransient())
									mi.remove();
							}
							respond(arg0, "" + r.data);
						}
					}

					// return new Response(HTTP_NOTFOUND,
					// null, "");
				}

				@Override
				public void onError(WebSocket arg0, Exception arg1) {
				}

				@Override
				public void onClose(WebSocket arg0, int arg1, String arg2, boolean arg3) {
				}
			};

			websocketServer.start();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	protected void respond(WebSocket arg0, InputStream ans) {
		try {
			String s = CharStreams.toString(new InputStreamReader(ans));
			respond(arg0, s);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void respond(WebSocket arg0, String string) {
		try {
			arg0.send(string);
		} catch (NotYetConnectedException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void sendBuffer(WebSocket arg0, ByteBuffer b) {
		try {
			arg0.send(b);
		} catch (NotYetConnectedException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void sendBuffer(WebSocket arg0, iGeometry b, int a) {
		try {
			VertexBuffer vb = (VertexBuffer) b.auxBuffers().get(a);
			arg0.send(vb.bBuffer);
		} catch (NotYetConnectedException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	protected InputStream findAndRun(String replace, Properties parms) {
		System.out.println(" find and run <" + replace + "> <" + parms + ">");
		return null;
	}

	public iVisualElement root;

	protected String findElementAndProperty(String nam, String prop) {

		System.out.println(" root is <" + root + ">");

		if (root == null)
			return null;
		iVisualElement e = StandardFluidSheet.findVisualElementWithName(root, nam);
		if (e == null)
			return null;
		Object mm = new VisualElementProperty<Object>(prop).get(e);
		return mm + "";
	}

	public void addHandler(String uri, Handler h) {
		synchronized (handlers) {
			handlers.addToList(uri, h);
		}
	}

	public void addContent(String s) {
		synchronized (contentsLock) {

			Coalesce c = new Coalesce();
			c.contents = s;
			c.group = 0;
			contents.add(c);
			contentsLock.notifyAll();
		}
	}

	public void addContent(ByteBuffer s) {
		synchronized (contentsLock) {

			Coalesce c = new Coalesce();
			c.contents = null;
			c.buffer = s;
			c.group = 0;
			contents.add(c);
			contentsLock.notifyAll();
		}
	}

	public List<String> getError() {
		synchronized (contentsLock) {
			ArrayList<String> errors = new ArrayList<String>(error);
			error.clear();
			return errors;
		}
	}

	public List<String> getPrint() {
		synchronized (contentsLock) {
			ArrayList<String> errors = new ArrayList<String>(print);
			print.clear();
			return errors;
		}
	}

	protected Object getUpdateForID(Integer id) {

		System.out.println(" get update for ID :" + id);

		synchronized (contentsLock) {
			Integer lastUpdate = cursors.get(id);
			if (lastUpdate == null)
				lastUpdate = 0;

			System.out.println(" wait and collect for <" + id + " " + lastUpdate + " " + contents + ">");

			Object r = waitAndCollect(id, lastUpdate, contents);

			System.out.println(" wait and collect finished for <" + id + ">");

			System.out.println(" returning <" + r + "> for <" + id + ">");

			return r;
		}
	}

	protected void getUpdateForIDContinuation(final Integer id, final iFunction<Object, Object> continuation) {

		new Thread() {
			@Override
			public void run() {
				Object r = getUpdateForID(id);
				if (r instanceof String) {
					String rr = (String) r;
					if (rr.trim().length() > 0)
						continuation.f(rr.trim());
				}
				else if (r instanceof ByteBuffer)
				{
					continuation.f((ByteBuffer)r);
				}
			}
		}.start();

	}

	private Object waitAndCollect(Integer id, Integer lastUpdate, List<Coalesce> stack) {
		synchronized (contentsLock) {
			while (stack.size() == lastUpdate)
				try {
					contentsLock.wait(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

			Integer lastUpdate2 = cursors.get(id);
			if (lastUpdate2 == null)
				cursors.put(id, lastUpdate2 = 0);

			if (lastUpdate2 > lastUpdate)
				return "";

			if (lastUpdate2 >= stack.size())
				return "";

			Coalesce c = stack.get(lastUpdate2);

			cursors.put(id, lastUpdate2 + 1);
			System.out.println(" id :" + id + " now at " + lastUpdate);

			if (c.contents != null)
				return c.contents;
			else
				return c.buffer;

			// if (lastUpdate > stack.size())
			// cursors.put(id, 0);

		}
	}

	@Override
	public void update() {

	}
}
