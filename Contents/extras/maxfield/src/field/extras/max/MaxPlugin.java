package field.extras.max;

import java.awt.Font;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Map;

import org.python.apache.xerces.impl.dv.util.Base64;

import field.core.StandardFluidSheet;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.dispatch.iVisualElementOverrides.DefaultOverride;
import field.core.execution.PythonInterface;
import field.core.execution.iExecutesPromise;
import field.core.network.OSCInput;
import field.core.network.OSCOutput;
import field.core.network.UDPNIOSender;
import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.iLinearGraphicsContext;
import field.core.plugins.python.PythonPluginEditor;
import field.core.ui.text.PythonTextEditor.EditorExecutionInterface;
import field.core.windowing.GLComponentWindow;
import field.launch.iUpdateable;
import field.math.graph.GraphNodeSearching.VisitCode;
import field.math.graph.iMutable;
import field.math.linalg.Vector3;
import field.math.linalg.Vector4;
import field.util.NPermuteMIterator;

public class MaxPlugin extends field.core.plugins.BaseSimplePlugin {

	static public final VisualElementProperty<MaxPlugin> maxPlugin = new VisualElementProperty<MaxPlugin>("maxPlugin");

	MaxExecution shim;

	public class LocalOverride extends DefaultOverride {
		@Override
		public <T> VisitCode getProperty(iVisualElement source, VisualElementProperty<T> prop, Ref<T> ref) {

			if (prop.equals(iExecutesPromise.promiseExecution)) {
				if (needsProcessing(source)) {
					ref.set((T) shim.getExecutesPromise(source, (iExecutesPromise) ref.get()));
				}
			} else if (prop.equals(PythonPluginEditor.editorExecutionInterface)) {
				if (needsProcessing(source)) {
					ref.set((T) shim.getEditorExecutionInterface(source, (EditorExecutionInterface) ref.get()));
				}
			}
			return super.getProperty(source, prop, ref);
		}

		public <T> VisitCode setProperty(iVisualElement source, iVisualElement.VisualElementProperty<T> prop, field.core.dispatch.iVisualElementOverrides.Ref<T> to) {

			if (prop.containsSuffix("toMax") && needsProcessing(source)) {
				sendData(source.getProperty(iVisualElement.name), prop.getName().replace("_toMax_", ""), to.get());
			}

			return super.setProperty(source, prop, to);
		};

		@Override
		public VisitCode menuItemsFor(final iVisualElement source, Map<String, iUpdateable> items) {
			if (source != null) {
				if (needsProcessing(source)) {
					items.put("MaxMSP", null);
					items.put("\u2464 <b>Remove bridge</b> to MaxMSP", new iUpdateable() {
						public void update() {
							needsMax.set(source, source, false);
							iVisualElement.dirty.set(source, source, true);
						}
					});
				} else {
					items.put("MaxMSP", null);
					items.put("\u2464 Bridge element to <b>MaxMSP</b>", new iUpdateable() {
						public void update() {
							needsMax.set(source, source, true);
							iVisualElement.dirty.set(source, source, true);
						}
					});
				}
			}
			return super.menuItemsFor(source, items);
		}

		@Override
		public VisitCode paintNow(iVisualElement source, Rect bounds, boolean visible) {
			Boolean n = source.getProperty(needsMax);
			if (n != null && n) {
				if (GLComponentWindow.currentContext != null && GLComponentWindow.draft) {
					CachedLine l = new CachedLine();
					l.getInput().moveTo((float) (bounds.x + bounds.w - 12), (float) (bounds.y + bounds.h - 12));
					l.getInput().setPointAttribute(iLinearGraphicsContext.text_v, "m ");
					l.getInput().setPointAttribute(iLinearGraphicsContext.textIsBlured_v, true);
					l.getInput().setPointAttribute(iLinearGraphicsContext.font_v, new Font("Gill Sans", Font.ITALIC, 30));
					l.getInput().setPointAttribute(iLinearGraphicsContext.fillColor_v, new Vector4(0, 0, 0, 0.4f));
					l.getProperties().put(iLinearGraphicsContext.containsText, true);
					GLComponentWindow.currentContext.submitLine(l, l.getProperties());
				}

				for (iMutable<iVisualElement> p : source.getParents()) {
					Boolean n2 = ((iVisualElement) p).getProperty(needsMax);
					if (n2 == null) {
						((iVisualElement) p).setProperty(needsMax, true);
						GLComponentWindow.getCurrentWindow(null).requestRepaint();
					}
				}

			}
			return super.paintNow(source, bounds, visible);
		}

	}

	static public final VisualElementProperty<Boolean> needsMax = new VisualElementProperty<Boolean>("needsMax");

	protected OSCInput in;
	protected OSCOutput out;

	@Override
	protected String getPluginNameImpl() {
		return "max";
	}

	@Override
	public void registeredWith(final iVisualElement root) {
		super.registeredWith(root);
		this.root = root;
		shim = new MaxExecution(this);

		in = new OSCInput(8790);
		out = new OSCOutput(20000, new UDPNIOSender(8789, "127.0.0.1"));

		in.registerHandler("/print", new OSCInput.DispatchableHandler() {

			public void handle(String s, Object[] args) {
				PythonInterface.getPythonInterface().print("" + args[0]);
				System.out.println(args[0]);
			}
		});
		in.registerHandler("/printError", new OSCInput.DispatchableHandler() {

			public void handle(String s, Object[] args) {
				PythonInterface.getPythonInterface().printError("" + args[0]);
				System.out.println(args[0]);
			}
		});

		in.registerHandler("/data", new OSCInput.DispatchableHandler() {

			public void handle(String s, Object[] args) {

				System.out.println(" got data message <" + s + "> <" + Arrays.asList(args) + ">");

				String destination = ((String) args[0]).substring(1);

				iVisualElement target = StandardFluidSheet.findVisualElementWithName(root, destination);
				if (target != null) {

					byte[] decoded = new Base64().decode((String) args[2]);
					try {
						Object d = new ObjectInputStream(new ByteArrayInputStream(decoded)).readObject();

						System.out.println(" setting <" + args[1] + "> to <" + d + ">");

						target.setProperty(new VisualElementProperty(((String) args[1]) + "_fromMax_"), d);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

			}
		});

		root.setProperty(maxPlugin, this);
	}

	public void sendMessage(String path, Object... args) {
		if (!path.startsWith("/"))
			path = "/" + path;
		out.simpleSend("/message" + path, args);
	}

	public void sendData(String path, String name, Object value) {
		if (!path.startsWith("/"))
			path = "/" + path;

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(value);
			oos.close();
			String encodedValue = new Base64().encode(baos.toByteArray());

			out.simpleSend("/data" + path, name, encodedValue);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected DefaultOverride newVisualElementOverrides() {
		return new LocalOverride();
	}

	@Override
	public void update() {
		super.update();
		in.update();
		shim.update();
	}

	public boolean needsProcessing(iVisualElement source) {
		Object o = needsMax.get(source);
		if (o == null)
			return false;
		if (o instanceof Boolean)
			return ((Boolean) o).booleanValue();
		if (o instanceof Number)
			return ((Number) o).intValue() > 0;
		return false;
	}

	public Vector3[] computeLine(iVisualElement from, iVisualElement to) {
		Rect f1 = from.getFrame(null);
		Rect f2 = to.getFrame(null);

		Vector3[] l1 = new Vector3[4];
		Vector3[] l2 = new Vector3[4];
		l1[0] = new Vector3(f1.x, f1.y + f1.h / 2, 0);
		l1[1] = new Vector3(f1.x + f1.w, f1.y + f1.h / 2, 0);
		l1[2] = new Vector3(f1.x + f1.w / 2, f1.y, 0);
		l1[3] = new Vector3(f1.x + f1.w / 2, f1.y + f1.h, 0);

		l2[0] = new Vector3(f2.x, f2.y + f2.h / 2, 0);
		l2[1] = new Vector3(f2.x + f2.w, f2.y + f2.h / 2, 0);
		l2[2] = new Vector3(f2.x + f2.w / 2, f2.y, 0);
		l2[3] = new Vector3(f2.x + f2.w / 2, f2.y + f2.h, 0);

		NPermuteMIterator iter = new NPermuteMIterator(new Integer[] { 0, 1, 2, 3 }, 2);
		Integer[] ii = new Integer[2];
		float close = Float.POSITIVE_INFINITY;
		Integer[] closei = new Integer[2];
		while (iter.hasNext()) {
			iter.next(ii);
			float d = l1[ii[0]].distanceFrom(l2[ii[1]]);
			if (d < close) {
				closei = ii;
				ii = new Integer[2];
				close = d;
			}
		}

		return new Vector3[] { l1[closei[0]], l2[closei[1]] };
	}

	public iVisualElement getRoot() {
		return root;
	}
}
