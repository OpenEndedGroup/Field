package field.core.plugins.drawing.opengl;

//import java.awt.BasicStroke;
//import java.awt.Font;
import java.awt.BasicStroke;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import field.core.dispatch.iVisualElement;
import field.core.plugins.drawing.opengl.CachedLine.Event;
import field.core.ui.MarkingMenuBuilder;
import field.core.ui.text.protect.ClassDocumentationProtect.Divider;
import field.graphics.ci.CoreImageCanvasUtils;
import field.math.linalg.CoordinateFrame;
import field.math.linalg.Vector2;
import field.math.linalg.Vector3;
import field.math.linalg.Vector4;
import field.namespace.generic.Bind.iFunction;
import field.util.Dict;
import field.util.Dict.Prop;

public abstract class iLinearGraphicsContext {

	/** should the line be stroked ? true / false */
	static public final Prop<Boolean> stroked = new Prop<Boolean>("stroked");
	static public final Prop<Boolean> filled = new Prop<Boolean>("filled");
	static public final Prop<Boolean> pointed = new Prop<Boolean>("pointed");

	static public final Divider divider_0 = new Divider();

	static public final Prop<Float> thickness = new Prop<Float>("thickness");

	static public final Divider divider_1 = new Divider();

	static public final Prop<Vector4> color = new Prop<Vector4>("color");
	static public final Prop<Vector4> strokeColor = new Prop<Vector4>("strokeColor");
	static public final Prop<Vector4> pointColor = new Prop<Vector4>("pointColor");
	static public final Prop<Vector4> fillColor = new Prop<Vector4>("fillColor");

	static public final Divider divider_2 = new Divider();

	static public final Prop<Number> totalOpacity = new Prop<Number>("totalOpacity");

	static public final Divider divider_3 = new Divider();

	static public final Prop<BasicStroke> strokeType = new Prop<BasicStroke>("strokeType");
	static public final Prop<Number> windingRule= new Prop<Number>("Number");

	static public final Divider divider_4 = new Divider();

	static public final Prop<Float> pointSize = new Prop<Float>("pointSize");
	static public final Prop<Float> pointSize_v = new Prop<Float>("pointSize_v");
	static public final Prop<Vector4> pointColor_v = new Prop<Vector4>("pointColor_v");

	static public final Divider divider_5 = new Divider();

	// this is nominally a float, but can also be a Vector3 for control
	// points (c1.z, c2.z, b.z)
	static public final Prop<Object> z_v = new Prop<Object>("z_v");
	static public final Prop<Float> containsDepth = new Prop<Float>("containsDepth");
	static public final Prop<CoordinateFrame> fastTransform= new Prop<CoordinateFrame>("_transform");
	static public final Prop<iFunction<Vector3, Vector3>> camera = new Prop<iFunction<Vector3, Vector3>>("camera");

	static public final Prop<Vector4> needVertexShading = new Prop<Vector4>("needVertexShading");
	static public final Prop<Vector4> strokeColor_v = new Prop<Vector4>("strokeColor_v");
	static public final Prop<Vector4> fillColor_v = new Prop<Vector4>("fillColor_v");

	static public final Prop<Boolean> starConvex = new Prop<Boolean>("starConvex");

	static public final Divider divider_7 = new Divider();

	static public final Prop<Vector4> thicknessProperties = new Prop<Vector4>("thicknessProperties");

	public static final Prop<Float> strokeThicknessMul = new Prop<Float>("strokeThicknessMul");
	public static final Prop<Float> geometricScale = new Prop<Float>("geometricScale");
	public static final Prop<Float> flatnessScale = new Prop<Float>("flatnessScale");

	static public final Divider divider_9 = new Divider();

	public static final Prop<Float> derived = new Prop<Float>("derived");
	public static final Prop<Float> hiddenControls= new Prop<Float>("hiddenControls");
	public static final Prop<Float> ignoreInPreview = new Prop<Float>("ignoreInPreview");
	public static final Prop<Boolean> soloCache = new Prop<Boolean>("soloCache");
	public static final Prop<Boolean> lateRendering= new Prop<Boolean>("lateRendering");
	public static final Prop<String> outputOpacityType = new Prop<String>("outputOpacityType");

	static public final Divider divider_10 = new Divider();

	public static final Prop<Boolean> notForExport = new Prop<Boolean>("notForExport");
	public static final Prop<String> layer = new Prop<String>("layer");
	public static final Prop<Vector4> saturationColor = new Prop<Vector4>("saturationColor");
	public static final Prop<Boolean> noTransform = new Prop<Boolean>("noTransform");

	public static final Prop<Boolean> shouldHighlight = new Prop<Boolean>("shouldHighlight");
	public static final Prop<Boolean> shouldCache = new Prop<Boolean>("shouldCache");

	static public final Divider divider_11 = new Divider();
	/* warning: the pdf and svg contexts don't support text right now */
	public static final Prop<Boolean> containsText = new Prop<Boolean>("containsText");
	public static final Prop<Boolean> containsMultilineText = new Prop<Boolean>("containsMultilineText");
	public static final Prop<String> text_v = new Prop<String>("text_v");
	public static final Prop<Number> textScale_v = new Prop<Number>("textScale_v");
	public static final Prop<Number> textRotation_v= new Prop<Number>("textRotation_v");
	public static final Prop<Vector2> textOffset_v= new Prop<Vector2>("textOffset_v");
	public static final Prop<String> infoText_v = new Prop<String>("infoText_v");
	public static final Prop<List<CachedLine>> infoAnnotation_v = new Prop<List<CachedLine>>("infoAnnotation_v");
	public static final Prop<MarkingMenuBuilder> infoRightClick_v = new Prop<MarkingMenuBuilder>("infoRightClick_v"); 
	public static final Prop<MarkingMenuBuilder> infoDoubleClick_v = new Prop<MarkingMenuBuilder>("infoDoubleClick_v"); 
	
	public static final Prop<Number> noTweak_v= new Prop<Number>("noTweak_v");
	public static final Prop<java.awt.Font> font_v = new Prop<java.awt.Font>("font_v");
	public static final Prop<Number> multilineWidth_v = new Prop<Number>("multilineWidth_v");
	public static final Prop<Boolean> textIsBlured_v= new Prop<Boolean>("textIsBlured_v");
	// -1 is rightAligned, 0 is centered, and 1 is left aligned
	public static final Prop<Float> alignment_v = new Prop<Float>("alignment_v");

	public static final Prop<Boolean> noHit = new Prop<Boolean>("noHit");

	public static final Prop<Integer> defaultMoveLock = new Prop<Integer>("defaultMoveLock");

	static public final Divider divider_12 = new Divider();
	public static final Prop<Boolean> isText_info = new Prop<Boolean>("isText_info");
	public static final Prop<Boolean> isArrow_info = new Prop<Boolean>("isArrow_info");

	static public final Divider divider_13 = new Divider();
	public static final Prop<iVisualElement> source = new Prop<iVisualElement>("source");

	static public final Divider divider_14 = new Divider();
	public static final Prop<Boolean> containsCode = new Prop<Boolean>("containsCode");
	public static final Prop<Collection<CachedLine>> codeDependsTo = new Prop<Collection<CachedLine>>("codeDependsTo");
	public static final Prop<Object> code_v = new Prop<Object>("code_v");
	public static final Prop<Object> name_v = new Prop<Object>("name_v");
	public static final Prop<Object> name = new Prop<Object>("name");
	public static final Prop<Object> internalName = new Prop<Object>("id");

	static public final Divider divider_15 = new Divider();
	public static final Prop<Vector3> paperColor = new Prop<Vector3>("paperColor");

	static public final Divider divider_16 = new Divider();
	public static final Prop<Boolean> containsImages = new Prop<Boolean>("containsImages");
	public static final Prop<CoreImageCanvasUtils.Image> image_v = new Prop<CoreImageCanvasUtils.Image>("image_v");
	public static final Prop<Number> imageDrawScale_v = new Prop<Number>("imageDrawScale_v");

	static public final Divider divider_17 = new Divider();
	public static final Prop<iLinearGraphicsContext> context = new Prop<iLinearGraphicsContext>("context");
	
	public static final Prop<Vector2> offsetFromSource = new Prop<Vector2>("offsetFromSource");
	public static final Prop<Vector2> offsetFromSource_v = new Prop<Vector2>("offsetFromSource_v");
	public static Prop<Boolean> usesAdjacency = new Prop<Boolean>("usesAdjacency");
	public static Prop<? extends Number> constantDistanceResampling = new Prop<Number>("constantDistanceResampling");
	public static Prop<Map<String, Number>> shaderAttributes = new Prop<Map<String, Number>>("shaderAttributes");
	public static Prop<Boolean> slow = new Prop<Boolean>("slow");
	
	public static Prop<Boolean> onSourceSelectedOnly = new Prop<Boolean>("onSourceSelectedOnly");
	public static Prop<CachedLine> offsetedLine = new Prop<CachedLine>("__offsetedLine");

	public static Prop<Integer> forceNew = new Prop<Integer>("forceNew");
	protected static Prop<CoordinateFrame> transform = new Prop<CoordinateFrame>("transform");

	abstract public Dict getGlobalProperties();

	abstract public void resubmitLine(CachedLine line, Dict properties);

	abstract public void submitLine(CachedLine line, Dict properties);

	static public String getClassDocumentation(String right, Object on) {
		return "class documentation";
	}
	
	public interface iTransformingContext<T>
	{
		public void convertDrawingSpaceToIntermediate(Vector2 drawing, T intermediate);
		public boolean convertIntermediateSpaceToDrawingSpace(T intermediate, Vector2 drawing);
		public boolean shouldClip(T intermediate);
		public T getIntermediateSpaceForEvent(CachedLine line, CachedLine.Event event, int index);
		public void setIntermediateSpaceForEvent(CachedLine onLine, Event vertex, int index, T currentIntermediate, Vector2 currentDrawing, Vector2 targetDrawing);
		public Object getTransformState();
		public void pushTransformState(Object state);
		public void popTransformState();
	}

}
