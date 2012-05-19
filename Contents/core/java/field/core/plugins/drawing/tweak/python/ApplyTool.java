package field.core.plugins.drawing.tweak.python;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;

import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.CachedLineCursor;
import field.core.plugins.drawing.opengl.CachedLine.Event;
import field.core.plugins.drawing.tweak.Visitors;
import field.core.plugins.drawing.tweak.NodeModifiers.iNodeModifier;
import field.core.plugins.drawing.tweak.TweakSplineUI.SelectedVertex;
import field.core.plugins.drawing.tweak.Visitors.BaseFilter;
import field.core.plugins.drawing.tweak.Visitors.Needs;
import field.core.plugins.drawing.tweak.Visitors.WritingReturn;
import field.core.plugins.drawing.tweak.Visitors.WritingVisitor;
import field.math.linalg.Vector2;
import field.util.ANSIColorUtils;

public class ApplyTool implements iCoordTransformation {

	static HashMap<String, Class> foundClasses = new HashMap<String, Class>();

	public ApplyTool(String toolname) {
		this(toolname, true, true, true);
	}

	Object[] args;

	Class found;

	Constructor foundConstructor;

	private boolean maskp;

	private boolean maskn;

	private boolean maska;

	public ApplyTool(String toolname, boolean maskp, boolean maskn, boolean maska) {
		this.args = new Object[0];

		this.maskp = maskp;
		this.maskn = maskn;
		this.maska = maska;

		Class found = foundClasses.get(toolname);
		if (found != null)
			this.found = found;
		else {
			// look in visitors
			try {
				this.found = this.getClass().getClassLoader().loadClass("field.core.plugins.drawing.tweak.Visitors$" + toolname);
			} catch (ClassNotFoundException e) {
			}
			if (this.found == null) try {
				this.found = this.getClass().getClassLoader().loadClass("field.core.plugins.drawing.tweak.NodeModifiers$" + toolname);
			} catch (ClassNotFoundException e) {
			}

			if (this.found == null)
				System.err.println(ANSIColorUtils.red(" can't find class <" + toolname + ">"));
			else {
				found = this.found;
				foundClasses.put(toolname, found);
			}
		}

		if (this.found != null) {
			Constructor[] c = found.getConstructors();
			for (int i = 0; i < c.length; i++)
				if (c[i].getParameterTypes().length == args.length) {
					foundConstructor = c[i];
				}
			if (foundConstructor == null) System.err.println(ANSIColorUtils.red(" can't find constructor from <" + Arrays.asList(c) + "> / <" + args.length + ">"));
		}
	}

	public ApplyTool(String toolname, boolean maskp, boolean maskn, boolean maska, Object... args) {
		this.args = args;

		this.maskp = maskp;
		this.maskn = maskn;
		this.maska = maska;

		Class found = foundClasses.get(toolname);
		if (found != null)
			this.found = found;
		else {
			// look in visitors
			try {
				this.found = this.getClass().getClassLoader().loadClass("field.core.plugins.drawing.tweak.Visitors$" + toolname);
			} catch (ClassNotFoundException e) {
			}
			if (this.found == null) try {
				this.found = this.getClass().getClassLoader().loadClass("field.core.plugins.drawing.tweak.NodeModifiers$" + toolname);
			} catch (ClassNotFoundException e) {
			}

			if (this.found == null)
				System.err.println(ANSIColorUtils.red(" can't find class <" + toolname + ">"));
			else {
				found = this.found;
				foundClasses.put(toolname, found);
			}
		}

		if (this.found != null) {
			Constructor[] c = found.getConstructors();
			for (int i = 0; i < c.length; i++)
				if (c[i].getParameterTypes().length == args.length) {
					foundConstructor = c[i];
				}
			if (foundConstructor == null) System.err.println(ANSIColorUtils.red(" can't find constructor from <" + Arrays.asList(c) + "> / <" + args.length + ">"));
		}
	}

	public void transformNode(float selectedAmount, final SelectedVertex vertex) {
		if (foundConstructor != null) {
			try {
				Object o = foundConstructor.newInstance(args);

				if (o instanceof iNodeModifier) {
					((iNodeModifier) o).apply(vertex.onLine, vertex.vertex, vertex.vertexIndex);
				} else

				if (o instanceof WritingReturn) {

					final WritingReturn wr = (WritingReturn) o;

					BaseFilter f = new Visitors.BaseFilter();
					CachedLine out = f.visitRewriting(vertex.onLine, new WritingVisitor(){

						public WritingReturn rewrite(Event before, Event now, Event after, BaseFilter inside) {
							if (now == vertex.vertex) {

								System.err.println(" actually applying the tool");
								return new WritingReturn(){

									public Vector2 setRightTangent(Vector2 out, CachedLineCursor now) {
										System.err.println(" srt :" + out + " " + now);
										Vector2 o = maska ? wr.setRightTangent(out, now) : out;
										System.err.println("    -->" + o);
										return o;
									}

									public Vector2 setPosition(Vector2 out, CachedLineCursor now) {
										System.err.println(" sp :" + out + " " + now);
										Vector2 o = maskn ? wr.setPosition(out, now) : out;
										System.err.println("    -->" + o);
										return o;
									}

									public Vector2 setLeftTangent(Vector2 out, CachedLineCursor now) {
										System.err.println(" slt :" + out + " " + now);
										Vector2 o = maskp ? wr.setLeftTangent(out, now) : out;
										System.err.println("    -->" + o);
										return o;
									}

									public Needs needsRightTangent() {
										Needs r = maska ? wr.needsRightTangent() : Needs.dontcare;
										System.err.println(" nrt :" + r);
										return r;
									}

									public Needs needsLeftTangent() {
										Needs r = maskp ? wr.needsLeftTangent() : Needs.dontcare;
										System.err.println(" nlt :" + r);
										return r;
									}

								};
							}

							System.err.println(" not applying the tool :" + now + " " + vertex + " " + vertex.vertex);
							return null;
						}
					});

					vertex.onLine.events = out.events;
				}

			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
	}
}
