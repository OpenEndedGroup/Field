package field.core.plugins.drawing.text;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

import field.core.dispatch.iVisualElement.Rect;
import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.LineUtils;
import field.math.linalg.Vector2;


public class AdvancedTextToCachedLineCache {

	public class CacheRecord {

		String font;
		String text;
		int size;

		Rect upperRect;
		Rect fullRect;
		CachedLine c;

		Vector2 at;

		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((font == null) ? 0 : font.hashCode());
			result = prime * result + size;
			result = prime * result + ((text == null) ? 0 : text.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			final CacheRecord other = (CacheRecord) obj;
			if (font == null) {
				if (other.font != null) return false;
			} else if (!font.equals(other.font)) return false;
			if (size != other.size) return false;
			if (text == null) {
				if (other.text != null) return false;
			} else if (!text.equals(other.text)) return false;
			return true;
		}
	}

	static public final int maxCacheSize = 500;
	LinkedHashMap<CacheRecord, CacheRecord> cache = new LinkedHashMap<CacheRecord, CacheRecord>(){
		@Override
		protected boolean removeEldestEntry(Entry<CacheRecord, CacheRecord> eldest) {
			if (this.size() > maxCacheSize) { return true; }
			return false;
		}
	};

	CacheRecord t = new CacheRecord();

	public CacheRecord find(String font, String text, int size) {
		t.font = font;
		t.text = text;
		t.size = size;
		return cache.get(t);
	}

	public void record(CachedLine c, String font, String text, int size, float ox, float oy, Rect upper, Rect full) {
		CacheRecord r = new CacheRecord();
		r.font = font;
		r.text = text;
		r.size = size;
		r.at = new Vector2(ox, oy);
		//r.upperRect = new Rect(0, 0, 0, 0).setValue(upper);
		//r.fullRect = new Rect(0, 0, 0, 0).setValue(full);
		r.c = c;
		cache.put(r, r);
	}

	public CachedLine convert(AdvancedTextToCachedLine c, float ox, float oy, CacheRecord r)
	{
		CachedLine ret = new LineUtils().transformLine(r.c, new Vector2(ox-r.at.x, oy-r.at.y), null, null, null);
		
//		c.upperRect = new Rect(0,0,0,0).setValue(r.upperRect);
//		c.fullRect = new Rect(0,0,0,0).setValue(r.fullRect);
//		
//		c.upperRect.x += ox-r.at.x;
//		c.upperRect.y += oy-r.at.y;
//		c.fullRect.x += ox-r.at.x;
//		c.fullRect.y += oy-r.at.y;
		return ret;
	}
}
