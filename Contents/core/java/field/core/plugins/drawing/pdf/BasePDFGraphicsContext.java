package field.core.plugins.drawing.pdf;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import com.lowagie.text.Document;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;

import field.bytecode.protect.annotations.GenerateMethods;
import field.bytecode.protect.annotations.Mirror;
import field.core.dispatch.iVisualElement.Rect;
import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.iLinearGraphicsContext;
import field.core.windowing.GLComponentWindow;
import field.launch.iUpdateable;
import field.math.linalg.Vector3;
import field.math.linalg.Vector4;
import field.util.Dict;
import field.util.LinkedHashMapOfLists;

/**
 * this is simpler than BaseGLGraphicsContext because it doesn't need to be so fast, which means that it doesn't have to deal with caching anything
 *
 * @author marc
 *
 */

@GenerateMethods
public class BasePDFGraphicsContext extends iLinearGraphicsContext {

	static public class DrawingResult {
		DrawingResultCode code;

		iUpdateable compute;

		public DrawingResult(DrawingResultCode code, iUpdateable up) {
			super();
			this.code = code;
			this.compute = up;
		}
	}

	public enum DrawingResultCode {
		cont, stop, replace, abort;
	}

	public interface iDrawingAcceptor {
		public DrawingResult accept(List<iUpdateable> soFar, CachedLine line, Dict properties);
	}

	public String layersOn = ".*";

	public String layersOff = "\\!.*";

	private iLinearGraphicsContext wasContext;

	private GLComponentWindow window;

	private PdfContentByte content;

	private PdfWriter writer;

	public float paperWidth = 3370;

	public float paperHeight = 3370;

	protected File drawTo;

	Dict globalProperties = new Dict();

	List<iDrawingAcceptor> acceptors = new ArrayList<iDrawingAcceptor>();

	LinkedHashMapOfLists<CachedLine, Dict> linesToDraw = new LinkedHashMapOfLists<CachedLine, Dict>();

	BasePDFGraphicsContext_m instance = new BasePDFGraphicsContext_m(this);

	public void addDrawingAcceptor(iDrawingAcceptor c) {
		acceptors.add(c);
	}

	public void finish() {
		System.err.println(" svg context exit");
		try {	
			Document document = new Document(new Rectangle(paperWidth, paperHeight), 0, 0, 0, 0);
			writer = PdfWriter.getInstance(document, new FileOutputStream(drawTo));
			writer.setPdfVersion(PdfWriter.VERSION_1_6);
			document.open();
			document.newPage();

			content = writer.getDirectContent();

			Vector3 pc = globalProperties.get(iLinearGraphicsContext.paperColor);
			if (pc != null) {
				CachedLine paper = makePaper(pc, new Rect(-1, -1, paperWidth, paperHeight));
				draw(paper, paper.getProperties());
			}

			Set<Entry<CachedLine, Collection<Dict>>> set = linesToDraw.entrySet();
			int n = 0;
			int nTot = set.size();
//			for (Entry<CachedLine, Collection<Dict>> d : set)
			Iterator<Entry<CachedLine, Collection<Dict>>> ii = set.iterator();
			while(ii.hasNext())
			{
				Entry<CachedLine, Collection<Dict>> d = ii.next();
				n++;
				for (Dict dd : d.getValue()) {
//					System.err.println(" drawing <"+n+" / "+nTot+"> <"+Runtime.getRuntime().freeMemory()+">");
					draw(d.getKey(), dd);

				}

				// help out gc ?
				ii.remove();
			}


			Vector4 sc = globalProperties.get(iLinearGraphicsContext.saturationColor);
			if (sc != null) {
				CachedLine paper = new CachedLine();
				paper.getInput().moveTo(-1, -1);
				paper.getInput().lineTo(paperWidth + 1, -1);
				paper.getInput().lineTo(paperWidth + 1, paperHeight + 1);
				paper.getInput().lineTo(-1, paperHeight + 1);
				paper.getInput().lineTo(-1, -1);
				paper.getProperties().put(iLinearGraphicsContext.filled, true);
				paper.getProperties().put(iLinearGraphicsContext.stroked, false);
				paper.getProperties().put(iLinearGraphicsContext.noTransform, true);
				paper.getProperties().put(iLinearGraphicsContext.outputOpacityType, "Saturation");
				paper.getProperties().put(iLinearGraphicsContext.fillColor, new Vector4(sc.x, sc.y, sc.z, sc.w));
				draw(paper, paper.getProperties());
			}

			document.close();

			GLComponentWindow.currentContext = wasContext;
			content = null;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public Dict getGlobalProperties() {
		return globalProperties;
	}

	public PdfContentByte getOutput() {

		return content;
	}

	public PdfWriter getWriter() {
		return writer;
	}

	public void install(GLComponentWindow window, File drawTo) {
		this.drawTo = drawTo;
		this.window = window;
		if (window != null) {
			window.getPreQueue().addUpdateable(instance.windowDisplayEnter);
			window.getPostQueue().addUpdateable(instance.windowDisplayExit);
		}
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

	public CachedLine makePaper(Vector3 pc, Rect paperRect) {
		CachedLine paper = new CachedLine();
		paper.getInput().moveTo((float)paperRect.x, (float)paperRect.y);
		paper.getInput().lineTo((float)(paperRect.w+paperRect.x), (float)paperRect.y);
		paper.getInput().lineTo((float)(paperRect.w+paperRect.x), (float)(paperRect.y+paperRect.h));
		paper.getInput().lineTo((float)paperRect.x, (float)(paperRect.y+paperRect.h));
		paper.getInput().lineTo((float)paperRect.x, (float)paperRect.y);
		paper.getProperties().put(iLinearGraphicsContext.filled, true);
		paper.getProperties().put(iLinearGraphicsContext.stroked, false);
		paper.getProperties().put(iLinearGraphicsContext.noTransform, true);
		paper.getProperties().put(iLinearGraphicsContext.outputOpacityType, "Multiply");
		paper.getProperties().put(iLinearGraphicsContext.fillColor, new Vector4(pc.x, pc.y, pc.z, 1));
		return paper;
	}

	@Override
	public void resubmitLine(CachedLine line, Dict properties) {
	}

	public void setDrawTo(File drawTo) {
		this.drawTo = drawTo;
	}

	public void setPaperSize(float width, float height) {
		paperWidth = width;
		paperHeight = height;
	}

	@Override
	public void submitLine(CachedLine line, Dict properties) {
		
		System.out.println(" submitting line for pdf output <"+line+"> <"+properties+">");
		
		if (properties.isTrue(iLinearGraphicsContext.notForExport, false))
			return;
		float tot = properties.getFloat(iLinearGraphicsContext.totalOpacity, 1);
		if (tot == 0)
			return;

		linesToDraw.addToList(line, properties);
	}

	public void uninstall(GLComponentWindow window) {
		if (window != null) {
			window.getPreQueue().removeUpdateable(instance.windowDisplayEnter);
			window.getPostQueue().removeUpdateable(instance.windowDisplayExit);
		}
	}

	@Mirror
	public void windowDisplayEnter() {
		System.err.println(" svg context enter");
		wasContext = GLComponentWindow.currentContext;
		GLComponentWindow.currentContext = this;
	}

	@Mirror
	public void windowDisplayExit() {
		finish();
		uninstall(window);

		GLComponentWindow.currentContext = wasContext;

		System.err.println(" svg context exit, finished");
	}

	protected void draw(CachedLine key, Dict dd) {

		List<iUpdateable> computes = new ArrayList<iUpdateable>();
		for (iDrawingAcceptor a : acceptors) {
			DrawingResult ret = a.accept(computes, key, dd);
			if (ret != null) {
				if (ret.code == DrawingResultCode.abort)
					return;
				if (ret.code == DrawingResultCode.cont) {
					computes.add(ret.compute);
					continue;
				}
				if (ret.code == DrawingResultCode.replace) {
					computes.clear();
					computes.add(ret.compute);
					continue;
				}
				if (ret.code == DrawingResultCode.stop) {
					computes.clear();
					computes.add(ret.compute);
					break;
				}
			}
		}

		System.out.println(" computes <"+computes+">");

		if (computes.size() == 0)
			return;

		for (iUpdateable u : computes)
			u.update();

	}

}
