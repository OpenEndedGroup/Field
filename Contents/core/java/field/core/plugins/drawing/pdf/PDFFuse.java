package field.core.plugins.drawing.pdf;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;


import com.lowagie.text.DocumentException;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGState;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfTransparencyGroup;

import field.core.dispatch.iVisualElement.Rect;
import field.math.linalg.Vector2;

public class PDFFuse {

	private PdfReader reader;

	private PdfStamper stamp;

	private PdfContentByte over;

	public PDFFuse(String fileIn, String fileOut) {
		try {
			reader = new PdfReader(fileIn);
			stamp = new PdfStamper(reader, new FileOutputStream(fileOut));
			over = stamp.getOverContent(1);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (DocumentException e) {
			e.printStackTrace();
		}
	}

	public void addImage(String filename, Rect intoRect) throws DocumentException, MalformedURLException, IOException {
		Image pdfimage = Image.getInstance(filename);
		over.addImage(pdfimage, (int) intoRect.w, 0, 0, (int) intoRect.h, (int) intoRect.x, (int) intoRect.y);
	}

	public void addImageMultiply(String filename, Rect intoRect, float opacity) throws DocumentException, MalformedURLException, IOException {
		Image pdfimage = Image.getInstance(filename);
		over.saveState();
		PdfGState gs1 = new PdfGState();
		gs1.setFillOpacity(opacity);
		gs1.setStrokeOpacity(opacity);
		gs1.setBlendMode(new PdfName("Multiply"));
		over.setGState(gs1);
		over.addImage(pdfimage, (int) intoRect.w, 0, 0, (int) intoRect.h, (int) intoRect.x, (int) intoRect.y);
		over.restoreState();
	}

	public void addImageScreen(String filename, Rect intoRect, float opacity) throws DocumentException, MalformedURLException, IOException {
		Image pdfimage = Image.getInstance(filename);
		over.saveState();
		PdfGState gs1 = new PdfGState();
		gs1.setFillOpacity(opacity);
		gs1.setStrokeOpacity(opacity);
		gs1.setBlendMode(new PdfName("Screen"));
		over.setGState(gs1);
		over.addImage(pdfimage, (int) intoRect.w, 0, 0, (int) intoRect.h, (int) intoRect.x, (int) intoRect.y);
		over.restoreState();
	}
	
	public void addImageScreen(String filename, Rect intoRect, float opacity, float rotation) throws DocumentException, MalformedURLException, IOException {
		Image pdfimage = Image.getInstance(filename);
		over.saveState();
		PdfGState gs1 = new PdfGState();
		gs1.setFillOpacity(opacity);
		gs1.setStrokeOpacity(opacity);
		gs1.setBlendMode(new PdfName("Screen"));
		over.setGState(gs1);
				
		over.addImage(pdfimage,  (float) (intoRect.w*Math.cos(rotation)), (float)(Math.sin(rotation)*intoRect.w), (float)(-Math.sin(rotation)*intoRect.h), (float)(Math.cos(rotation)*intoRect.h), (int) intoRect.x, (int) intoRect.y);
		over.restoreState();
	}

	
	public void addPdf(String filename) throws IOException {
		PdfImportedPage imp = stamp.getImportedPage(new PdfReader(filename), 1);
		over.addTemplate(imp, 0, 0);
	}
	public void addPdf(String filename, Vector2 offset) throws IOException {
		PdfImportedPage imp = stamp.getImportedPage(new PdfReader(filename), 1);
		over.addTemplate(imp, offset.x, offset.y);
	}
	
	public void addPdf(String filename, Vector2 offset, float scale) throws IOException {
		PdfImportedPage imp = stamp.getImportedPage(new PdfReader(filename), 1);
		over.addTemplate(imp, scale, 0, 0, scale, offset.x, offset.y);
	}

	public void addPdf(String filename, float[] opacity, String[] mode, Vector2[] ox) throws IOException {
		// try {
		PdfImportedPage imp = stamp.getImportedPage(new PdfReader(filename), 1);

		PdfTemplate template = over.createTemplate(imp.getWidth(), imp.getHeight());
		template.addTemplate(imp, 0, 0);
		
		PdfTransparencyGroup group = new PdfTransparencyGroup();
		group.setIsolated(true);
		group.setKnockout(true);
		template.setGroup(group);

		for (int i = 0; i < opacity.length; i++) {
			PdfGState gs1 = new PdfGState();
			gs1.setFillOpacity(opacity[i]);
			gs1.setStrokeOpacity(opacity[i]);
			gs1.setBlendMode(new PdfName(mode[i]));
			
			over.saveState();
			over.setGState(gs1);
			over.addTemplate(template, ox == null ? 0 : ox[i].x, ox == null ? 0 : ox[i].y);
			over.restoreState();
		}

		// } catch (BadElementException e) {
		// e.printStackTrace();
		// }

	}

	public void close() throws DocumentException, IOException {
		stamp.close();
	}

}
