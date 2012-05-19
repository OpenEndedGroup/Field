package field.core.ui;

import java.awt.Color;
import java.util.Set;
import java.util.Map.Entry;

import javax.swing.UIDefaults;

import com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel;

public class NimbusLookAndFeelField extends NimbusLookAndFeel{

	UIDefaults h = null;
	
	@Override
	public UIDefaults getDefaults() {
		if (h!=null) return h;
		h = super.getDefaults();
		
		h.put("nimbusBase", new Color(0.4f, 0.4f, 0.4f, 1f));
		h.put("nimbusLightBackground", new Color(0.8f, 0.8f, 0.8f, 1f));
		h.put("nimbusFocus", new Color(0.5f, 0.5f, 0.5f, 0.1f));
		h.put("nimbusBorder", new Color(0,0,0,0f));
		h.put("background", new Color(0.5f,0.5f,0.5f,0f));
		h.put("Tree.opaque", false);
		
		Set<Entry<Object, Object>> e = h.entrySet();
		for(Entry<Object, Object> oo :e)
		{
			if (oo.getKey().toString().endsWith("opaque"))
				oo.setValue(false);
		}
		
		h.put("EditorPane.background", new Color(0.3f, 0.3f, 0.3f, 1f));
		
		
		
		return h;
	}
		
}
