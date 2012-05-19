package field.core.plugins.drawing.opengl;

import static org.lwjgl.opengl.GL11.GL_ONE;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA_SATURATE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import field.bytecode.protect.annotations.GenerateMethods;
import field.bytecode.protect.annotations.Mirror;
import field.core.windowing.GLComponentWindow;
import field.core.windowing.overlay.OverlayAnimationManager;
import field.graphics.core.Base;
import field.graphics.core.Base.iAcceptsSceneListElement;
import field.graphics.core.Base.iSceneListElement;
import field.graphics.core.BasicGLSLangProgram;
import field.graphics.core.BasicGeometry.LineList_long;
import field.graphics.core.BasicGeometry.TriangleMesh_long;
import field.graphics.core.BasicUtilities;
import field.graphics.dynamic.DynamicLine;
import field.graphics.dynamic.DynamicLine_long;
import field.graphics.dynamic.iDynamicMesh;
import field.launch.iUpdateable;
import field.math.abstraction.iProvider;
import field.math.linalg.Vector4;
import field.namespace.generic.Generics.Pair;
import field.namespace.generic.ReflectionTools;
import field.util.BiMap;
import field.util.Dict;
import field.util.TaskQueue;

@GenerateMethods
public class BaseGLGraphicsContext extends iLinearGraphicsContext {

	static public class DrawingResult {
		DrawingResultCode code;

		List<iDynamicMesh> ret;

		iUpdateable compute;
		public iUpdateable finalize;

		public DrawingResult(DrawingResultCode code, iUpdateable up, iDynamicMesh... ret) {
			super();
			this.code = code;
			this.ret = Arrays.asList(ret);
			this.compute = up;
		}
	}

	public enum DrawingResultCode {
		cont, stop, replace, abort;
	}

	public interface iDrawingAcceptor<T> {
		public DrawingResult accept(List<iDynamicMesh> soFar, T line, Dict properties);
	}

	public class InternalLine {

		List<iDynamicMesh> outputTo = new ArrayList<iDynamicMesh>();

		iDynamicMesh outputTo_list = ReflectionTools.listProxy(outputTo, iDynamicMesh.class);

		List<iUpdateable> updateTo = new ArrayList<iUpdateable>();
		List<iUpdateable> finalizeTo = new ArrayList<iUpdateable>();

		iUpdateable updateTo_list = ReflectionTools.listProxy(updateTo, iUpdateable.class);
		iUpdateable finalizeTo_list = ReflectionTools.listProxy(finalizeTo, iUpdateable.class);

		Dict properties;

		boolean touched = true;
	}

	public boolean draft = true;

	public String layersOn = ".*";

	public String layersOff = "\\!.*";

	private BasicGLSLangProgram textureProgram;

	protected List<iDrawingAcceptor<CachedLine>> lineAcceptors = new ArrayList<iDrawingAcceptor<CachedLine>>();

	protected final iAcceptsSceneListElement inside;

	protected iAcceptsSceneListElement vertexProgram;

	protected Dict globalProperties = new Dict();

	iLinearGraphicsContext parallel = null;

	LinkedHashMap<CachedLine, InternalLine> cache = new LinkedHashMap<CachedLine, InternalLine>();

	LinkedHashSet<Pair<CachedLine, Dict>> needingCreating = new LinkedHashSet<Pair<CachedLine, Dict>>();

	public BiMap<Float, iDynamicMesh> allreadyConstructedLines = new BiMap<Float, iDynamicMesh>();

	BaseGLGraphicsContext_m baseGLGraphicsContext_m = new BaseGLGraphicsContext_m(this);

	HashMap<iDynamicMesh, Integer> probation = new HashMap<iDynamicMesh, Integer>();

	TaskQueue endQueue = new TaskQueue();

	boolean insideWindow = false;

	private TaskQueue preswapQueue;

	public BaseGLGraphicsContext(iAcceptsSceneListElement inside) {
		this.inside = inside;
		vertexProgram = inside;
	}

	public BaseGLGraphicsContext(iAcceptsSceneListElement inside, boolean saturate) {
		this.inside = inside;

		if (inside instanceof BasicGLSLangProgram) {
			vertexProgram = inside;
		} else {

			vertexProgram = new BasicGLSLangProgram("content/shaders/TestGLSLangVertex_withPointsize.glslang", "content/shaders/VertexColorFragment.glslang").setDoPointSize();
			// vertexProgram = new
			// BasicGLSLangProgram("content/shaders/TestGLSLangVertex.glslang",
			// "content/shaders/WhiteFragment.glslang");//.setDoPointSize();
			inside.addChild((BasicGLSLangProgram) vertexProgram);
			if (saturate) {
				vertexProgram.addChild(new BasicUtilities.Smooth());
				vertexProgram.addChild(new BasicUtilities.SetBlendMode(Base.StandardPass.preRender, GL_SRC_ALPHA_SATURATE, GL_ONE));

				textureProgram = new BasicGLSLangProgram("content/shaders/glComponentVertex.glslang", "content/shaders/glComponentGLSLangFragment.glslang");
				textureProgram.new SetIntegerUniform("tex", new iProvider.Constant<Integer>(1));
				textureProgram.new SetIntegerUniform("texture2", new iProvider.Constant<Integer>(0));
				textureProgram.new SetUniform("mul", new Vector4(1, 1, 1, 1));
				textureProgram.new SetUniform("add", new Vector4(0, 0, 0, 0));

			} else {
				vertexProgram.addChild(new BasicUtilities.Smooth());
				vertexProgram.addChild(new BasicUtilities.SetBlendMode(Base.StandardPass.preRender, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA));

				textureProgram = new BasicGLSLangProgram("content/shaders/glComponentVertex.glslang", "content/shaders/glComponentGLSLangFragment.glslang");
				// textureProgram = new
				// BasicGLSLangProgram("content/shaders/TestGLSLangVertex.glslang",
				// "content/shaders/WhiteFragment.glslang");
				textureProgram.new SetIntegerUniform("tex", new iProvider.Constant<Integer>(1));
				textureProgram.new SetIntegerUniform("texture2", new iProvider.Constant<Integer>(0));
				textureProgram.new SetUniform("mul", new Vector4(1, 1, 1, 1));
				textureProgram.new SetUniform("add", new Vector4(0, 0, 0, 0));

			}
		}
	}

	public void addAcceptor(iDrawingAcceptor a) {
		lineAcceptors.add(a);
	}

	public void addAcceptorHead(iDrawingAcceptor a) {
		lineAcceptors.add(0, a);
	}

	public Set<CachedLine> getAllLines() {
		return cache.keySet();
	}

	public TaskQueue getEndQueue() {
		return endQueue;
	}

	public TaskQueue getPreSwapQueue() {
		return preswapQueue;
	}

	@Override
	public Dict getGlobalProperties() {
		return globalProperties;
	}

	public iAcceptsSceneListElement getVertexProgram() {
		return (iAcceptsSceneListElement) vertexProgram;
	}
	
	public iAcceptsSceneListElement getTextureProgram() {
		return (iAcceptsSceneListElement) textureProgram;
	}

	public void setVertexProgram(BasicGLSLangProgram program) {
		inside.addChild(program);
		List<iSceneListElement> c = new ArrayList<iSceneListElement>(((BasicGLSLangProgram) program).getChildren());
		for (iSceneListElement cc : c) {
			// if (cc instanceof BasicMesh)
			{
				program.addChild(cc);
				vertexProgram.removeChild(cc);
			}
		}

		vertexProgram = program;
		inside.removeChild((iSceneListElement) vertexProgram);
	}

	public void install(GLComponentWindow window) {
		window.getPreQueue().addUpdateable(baseGLGraphicsContext_m.windowDisplayEnter);
		window.getPostQueue().addUpdateable(baseGLGraphicsContext_m.windowDisplayExit);
		preswapQueue = window.getRunQueue();
		insideWindow = true;
	}

	public boolean isLayer(CachedLine l) {
		String layer = l.getProperties().get(iLinearGraphicsContext.layer);
		if (layer == null)
			layer = "none";
		Pattern on = Pattern.compile(layersOn);
		Pattern off = Pattern.compile(layersOff);

		if (on.matcher(layer).matches() && !off.matcher(layer).matches())
			return true;
		return false;
	}

	@Override
	public void resubmitLine(CachedLine line, Dict properties) {

		if (lineInteraction != null)
			lineInteraction.uncache(line);
		properties.remove(iLinearGraphicsContext.forceNew);

		InternalLine internalLine = cache.get(line);
		if (internalLine == null) {
			submitLine(line, properties);
		} else {
			needingCreating.add(new Pair<CachedLine, Dict>(line, properties));
			for (iDynamicMesh m : internalLine.outputTo) {
				m.open();
				m.close();
			}
			cache.remove(line);
		}

		if (parallel != null)
			parallel.resubmitLine(line, properties);
	}

	public void setParallel(iLinearGraphicsContext parallel) {
		this.parallel = parallel;
	}

	LateExecutingDrawing later = new LateExecutingDrawing();

	private iLinearGraphicsContext wasContext;

	private LineInteraction lineInteraction;

	@Override
	public void submitLine(CachedLine line, Dict properties) {

		if (!draft && properties.isTrue(iLinearGraphicsContext.notForExport, false))
			return;
		if (properties.isTrue(iLinearGraphicsContext.ignoreInPreview, false))
			return;

		if (properties.isTrue(iLinearGraphicsContext.forceNew, false)) {
			resubmitLine(line, properties);
			return;
		}

		InternalLine internalLine = cache.get(line);
		if (internalLine == null) {
			needingCreating.add(new Pair<CachedLine, Dict>(line, properties));
		} else {
			internalLine.touched = true;
			reviseProperties(internalLine.properties, properties);
		}

		if (parallel != null)
			parallel.submitLine(line, properties);
	}

	public void toggleDraft() {
		draft = !draft;
	}

	public void uninstall(GLComponentWindow window) {
	}

	public void uninstall(OverlayAnimationManager window) {
	}

	@Mirror
	public void windowDisplayEnter() {

		if (insideWindow)
			wasContext = GLComponentWindow.currentContext;
		else
			wasContext = GLComponentWindow.fastContext;

		if (insideWindow)
			GLComponentWindow.currentContext = this;
		else
			GLComponentWindow.fastContext = this;
	}

	@Mirror
	public void windowDisplayExit() {

		try {

			later.begin();

			if (textureProgram != null)
				textureProgram.performPass(null);

			HashSet<iDynamicMesh> allLines = new HashSet<iDynamicMesh>();
			for (InternalLine il : cache.values())
				allLines.addAll(il.outputTo);

			for (iDynamicMesh m : allreadyConstructedLines.values()) {
				// m.open();
				// m.close();
				m.open();
			}

			for (InternalLine il : cache.values())
				if (il.touched) {
					allLines.removeAll(il.outputTo);
					probation.remove(il.outputTo);
				}

			Iterator<Map.Entry<CachedLine, InternalLine>> ii = cache.entrySet().iterator();
			while (ii.hasNext()) {
				Entry<CachedLine, InternalLine> e = ii.next();
				if (!e.getValue().touched) {
					ii.remove();
					e.getValue().finalizeTo_list.update();
				} else {
					later.scanLine(e.getKey(), e.getKey().properties);
				}
			}

			for (iDynamicMesh o : allLines)
				removeLine(o);

			for (Pair<CachedLine, Dict> p : needingCreating) {
				later.scanLine(p.left, p.right);
			}

			for (Pair<CachedLine, Dict> p : needingCreating) {
				later.prepLine(p.left, p.right);

				InternalLine line = create(p);
				if (line != null)
					cache.put(p.left, line);
			}
			for (Map.Entry<CachedLine, InternalLine> il : cache.entrySet()) {
				for (iDynamicMesh mm : il.getValue().outputTo)
					mm.open();
			}

			needingCreating.clear();

			for (InternalLine il : cache.values()) {
				if (il.touched) {
					try {
						il.updateTo_list.update();
					} catch (Exception e) {
						e.printStackTrace();
					}

				}
			}

			// finallize all dynamic lines
			// untouch everything

			for (InternalLine il : cache.values()) {
				for (iDynamicMesh mm : il.outputTo)
					mm.close();
				il.touched = false;
				for (iDynamicMesh m : il.outputTo)
					probation.remove(m);
			}

			for (iDynamicMesh m : allreadyConstructedLines.values()) {
				m.close();
				if (m instanceof DynamicLine)
					assert ((DynamicLine) m).isClosed();
				if (m instanceof DynamicLine_long)
					assert ((DynamicLine_long) m).isClosed();
			}

			parallel = null;

			if (lineInteraction != null)
				lineInteraction.setAllCachedLines(cache.keySet());

			endQueue.update();

			if (insideWindow) {
				GLComponentWindow.currentContext = wasContext;
			} else
				GLComponentWindow.fastContext = wasContext;

			List<iSceneListElement> c = ((BasicGLSLangProgram) vertexProgram).getChildren();

			final List<InternalLine> indexer = new ArrayList<InternalLine>(cache.values());

			Collections.sort(c, new Comparator<iSceneListElement>() {

				@Override
				public int compare(iSceneListElement arg0, iSceneListElement arg1) {

					int a = arg0 instanceof LineList_long ? 2 : (arg0 instanceof TriangleMesh_long ? 1 : 0);
					int b = arg1 instanceof LineList_long ? 2 : (arg1 instanceof TriangleMesh_long ? 1 : 0);

					for (int i = 0; i < indexer.size(); i++) {
						if (indexer.get(i).outputTo.contains(arg0)) {
							a = i;
						}
						if (indexer.get(i).outputTo.contains(arg1)) {
							b = i;
						}
					}

					return Float.compare(a, b);
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected InternalLine create(Pair<CachedLine, Dict> p) {
		List<iDynamicMesh> meshes = new ArrayList<iDynamicMesh>();
		List<iUpdateable> computes = new ArrayList<iUpdateable>();
		List<iUpdateable> finalize = new ArrayList<iUpdateable>();
		for (iDrawingAcceptor a : lineAcceptors) {
			try {
				DrawingResult ret = a.accept(meshes, p.left, p.right);
				if (ret != null) {
					if (ret.code == DrawingResultCode.abort)
						return null;
					if (ret.code == DrawingResultCode.cont) {
						meshes.addAll(ret.ret);
						computes.add(ret.compute);
						if (ret.finalize != null)
							finalize.add(ret.finalize);
						continue;
					}
					if (ret.code == DrawingResultCode.replace) {
						meshes.clear();
						meshes.addAll(ret.ret);
						computes.clear();
						computes.add(ret.compute);
						if (ret.finalize != null)
							finalize.add(ret.finalize);
						continue;
					}
					if (ret.code == DrawingResultCode.stop) {
						meshes.clear();
						meshes.addAll(ret.ret);
						computes.clear();
						computes.add(ret.compute);
						if (ret.finalize != null)
							finalize.add(ret.finalize);
						break;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (meshes.size() == 0)
			return null;

		InternalLine il = new InternalLine();
		il.outputTo.clear();
		il.outputTo.addAll(meshes);
		il.updateTo.clear();
		il.updateTo.addAll(computes);
		il.finalizeTo.clear();
		il.finalizeTo.addAll(finalize);

		return il;

	}

	protected void removeLine(iDynamicMesh o) {
		if (!probation.containsKey(o)) {
			probation.put(o, 0);

			// vertexProgram.removeChild(o.getUnderlyingGeometry());
			List<iSceneListElement> p = (List<iSceneListElement>) o.getUnderlyingGeometry().getParents();
			for (iSceneListElement pp : new ArrayList<iSceneListElement>(p))
				pp.removeChild(o.getUnderlyingGeometry());

		} else if (probation.get(o) > 5) {
			o.remove();
			allreadyConstructedLines.removeBackwards(o);
			probation.remove(o);
		} else {
			probation.put(o, probation.get(o) + 1);
		}
	}

	protected void reviseProperties(Dict currentProperties, Dict newProperties) {
	}

	public void setLineInteraction(LineInteraction lineInteraction) {
		this.lineInteraction = lineInteraction;
	}

}
