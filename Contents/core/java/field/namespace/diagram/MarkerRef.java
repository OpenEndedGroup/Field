package field.namespace.diagram;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import field.namespace.diagram.DiagramZero.iMarker;
import field.namespace.diagram.DiagramZero.iMarkerNotify;
import field.namespace.diagram.DiagramZero.iMarkerRef;
import field.namespace.diagram.DiagramZero.iMarkerRefNotify;
import field.namespace.generic.ReflectionTools;



/**
 * @author marc
 * Created on Sep 20, 2004 \u2014 bus to boston \u2014 extensively reworked Jan 21, 2006
 */
public class MarkerRef<T> implements DiagramZero.iMarkerRef<T> {

	WeakReference<iMarker<T>>  ref;
	List<iMarkerRefNotify<T>> notify = new ArrayList<iMarkerRefNotify<T>>();
	private iMarkerNotify<T> no;
	
	static final Object none = new Object();

	public MarkerRef(iMarker<T> marker)
	{
		ref = new WeakReference<iMarker<T>>(marker);
		marker.addNotify(no = new iMarkerNotify<T>() {
			boolean first = false;
			boolean willClear = false;

			public void beginMarkerNotify() {
				first = true;
			}

			public void endMarkerNotify() {
				if (!first)
				{
					ReflectionTools.apply(notify, DiagramZero.iMarkerRefNotify.endMarkerRefNotify);
				}
				first = true;
				if (willClear)
				{
					iMarker<T> m = getMarker();
					if (m!=null)
					{
						m.removeNotify(no);
						no=null;
					}
					ref = new WeakReference<iMarker<T>>(null);
				}
				willClear = false;
			}

			public void markerChanged(iMarker<T> thisMarker) {
			}

			public void markerRemoved(iMarker<T> thisMarker) {
				if (first)
				{
					first = false;
					ReflectionTools.apply(notify, DiagramZero.iMarkerRefNotify.beginMarkerRefNotify);
				}
				ReflectionTools.apply(notify, DiagramZero.iMarkerRefNotify.markerRefNowInvalid, MarkerRef.this);
				willClear = true;
			}
		});
	}
	
	public iMarker<T> getMarker() {
		iMarker<T> m = ref.get();
		if (m==none) return null;
		return m;
	}

	public void addNotify(iMarkerRefNotify<T> notify) {
		if (!this.notify.contains(notify))
		this.notify .add(notify);
	}

	public void removeNotify(iMarkerRefNotify notify) {
		this.notify.remove(notify);
	}
	
	@Override
	public String toString() {
		return "ref:"+ref.get();
	}
	
	@Override
	public int hashCode() {
		iMarker<T> r = ref.get();
		return r == null ? 0 : r.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof iMarkerRef)) return false;
		iMarker other = ((iMarkerRef)obj).getMarker();
		
		if (other == null) return this.getMarker()==null;
		return other.equals( getMarker());
	}

	public boolean hasNotify(iMarkerRefNotify l) {
		return this.notify.contains(l);
	}
}
