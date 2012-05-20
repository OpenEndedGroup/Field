package field.core.plugins.drawing.opengl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OptimizeCachedLineSet {

	public OptimizeCachedLineSet() {
	}

	public void doOptimization(List<CachedLine> c, String[] excludeKeys) {
		Map<Long, CachedLine> cache = new LinkedHashMap<Long, CachedLine>();
		List<CachedLine> extra = new ArrayList<CachedLine>();

		Set<String> ex = new LinkedHashSet<String>();
		if (excludeKeys != null)
			for (String s : excludeKeys)
				ex.add(s);

		for (CachedLine cc : c) {
			long hash = getHashFor(cc, ex);
			CachedLine target = cache.get(hash);
			if (target != null && hash != -1) {
				concat(cc, target);
			} else {
				CachedLine displaced = cache.put(hash, cc);
				if (displaced != null)
					extra.add(displaced);
			}
		}

		int old = c.size();
		c.clear();
		int newSize = cache.size();
		c.addAll(cache.values());
		c.addAll(extra);
		;//System.out.println(" optimized <" + old + " -> " + newSize + ">");
	}

	private long getHashFor(CachedLine cc, Set<String> ex) {
		if (cc.properties == null)
			return 0;
		// if (cc.properties.isTrue(iLinearGraphicsContext.filled,
		// false))
		// return -1;

		return (cc.properties.longHash(ex));
	}

	private void concat(CachedLine cc, CachedLine target) {
		for (CachedLine.Event e : cc.events) {
			e.setContainer(target);
		}
		target.events.addAll(cc.events);
	}

}
