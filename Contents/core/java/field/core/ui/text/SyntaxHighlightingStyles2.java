package field.core.ui.text;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;

import field.core.plugins.selection.ToolBarFolder;
import field.core.ui.NewInspector2;
import field.core.ui.NewInspector2.ColorControl;
import field.core.ui.NewInspector2.Inspected;
import field.core.ui.NewInspector2.iIO;
import field.core.ui.text.syntax.PythonScanner;
import field.launch.iUpdateable;
import field.math.linalg.Color4;
import field.math.linalg.Vector4;
import field.util.AutoPersist;

public class SyntaxHighlightingStyles2 {

	public static final Color4 tab = new AutoPersist().persist("Color4_tab", new Color4(0, 0, 0, 0.1f));
	public static final Color4 self = new AutoPersist().persist("Color4_self", new Color4(0.5f, 0.5f, 0.4f, 1f));
	public static final Color4 localTemp = new AutoPersist().persist("Color4_localTemp", new Color4(0.5f, 0.5f, 0.6f, 1f));
	public static final Color4 localPersistant = new AutoPersist().persist("Color4_localPersistant", new Color4(0.6f, 0.5f, 0.5f, 1f));
	public static final Color4 number = new AutoPersist().persist("Color4_number", new Color4(0.6f, 0.8f, 1, 1f));
	public static final Color4 operator = new AutoPersist().persist("Color4_operator", new Color4(1, 0.7f, 0.75f, 1f));
	public static final Color4 identifier = new AutoPersist().persist("Color4_identifier", new Color4(1, 1, 1, 1f));
	public static final Color4 string = new AutoPersist().persist("Color4_string", new Color4(0.7f, 0.7f, 1, 1f));
	public static final Color4 keyword = new AutoPersist().persist("Color4_keyword", new Color4(0.75f, 0.8f, 1, 1f));
	public static final Color4 decorator = new AutoPersist().persist("Color4_decorator", new Color4(1.0f, 1.0f, 1.0f, 1f));
	public static final Color4 comment = new AutoPersist().persist("Color4_comment", new Color4(0.0f, 0.0f, 0.0f, 0.5f));
	public static final Color4 background = new AutoPersist().persist("Color4_background", new Color4(85 / 255f, 85 / 255f, 85 / 255f, 1f));
	public static final float[] fontSizeMod = new AutoPersist().persist("FontSizeMod", new float[] { 0 });

	static public void initStyles(Color[] colors, StyledText target) {

		colors[PythonScanner.TokenTypes.comment.ordinal()] = comment.toSWTColor();
		colors[PythonScanner.TokenTypes.self.ordinal()] = self.toSWTColor();
		colors[PythonScanner.TokenTypes.localTemp.ordinal()] = localTemp.toSWTColor();
		colors[PythonScanner.TokenTypes.localPersistant.ordinal()] = localPersistant.toSWTColor();
		colors[PythonScanner.TokenTypes.number.ordinal()] = number.toSWTColor();
		colors[PythonScanner.TokenTypes.operator.ordinal()] = operator.toSWTColor();
		colors[PythonScanner.TokenTypes.identifier.ordinal()] = identifier.toSWTColor();
		colors[PythonScanner.TokenTypes.string.ordinal()] = string.toSWTColor();
		colors[PythonScanner.TokenTypes.keyword.ordinal()] = keyword.toSWTColor();
		colors[PythonScanner.TokenTypes.decorator.ordinal()] = decorator.toSWTColor();

		target.setBackground(background.toSWTColor());

		// Color background = ed.getBackground();

		;//System.out.println(" background color is <" + background + "> <"+colors[PythonScanner.TokenTypes.keyword.ordinal()]+">");

	}
	
	static ToolBarFolder open;
	
	static public void openCustomizer(final Color[] colors, final StyledText target, final iUpdateable post) {
		if (open!=null && !open.getShell().isDisposed()) return;
		
		ToolBarFolder f = new ToolBarFolder(new Rectangle(50, 50, 300, 600), true);
		open = f;
		ToolBarFolder was = ToolBarFolder.currentFolder;
		ToolBarFolder.currentFolder = f;
		final NewInspector2 inspector = new NewInspector2();
		inspector.getShell().setText("Customize Syntax Highlighting");

		ToolBarFolder.currentFolder = was;

		String[] names = { "keyword", "comment", "decorator", "self", "string", "localTemp", "localPersistant", "number", "operator", "identifier", "background" };

		for (final String name : names) {

			iIO<Vector4> iio = new iIO<Vector4>(name)
			{

				@Override
				public Vector4 getValue() {
					try {
						return (Vector4) SyntaxHighlightingStyles2.class.getField(name).get(null);
					} catch (IllegalArgumentException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (SecurityException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (NoSuchFieldException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					return null;
				}

				@Override
				public field.core.ui.NewInspector2.Status getStatus() {
					return field.core.ui.NewInspector2.Status.valid;
				}

				@Override
				public void setValue(Vector4 s) {
					
					;//System.out.println(" setting color <"+name+"> now to be <"+s+">");
					
					try {
						((Vector4) SyntaxHighlightingStyles2.class.getField(name).get(null)).set(s);
					} catch (IllegalArgumentException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (SecurityException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (NoSuchFieldException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					initStyles(colors, target);
					post.update();
					target.redraw();
				}
				
			};
			
			iio.editor= ColorControl.class;
			try {
				iio.editor.getDeclaredConstructors()[0].newInstance(inspector, iio);
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

//		inspector.(all);
//		inspector.setWaterText("Editor Options");

		f.select(0);
	}
}
