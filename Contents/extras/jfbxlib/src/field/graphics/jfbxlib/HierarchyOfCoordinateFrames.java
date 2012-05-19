package field.graphics.jfbxlib;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import field.math.abstraction.iAcceptor;
import field.math.abstraction.iInplaceProvider;
import field.math.graph.iTopology;
import field.math.graph.iTopology.iHasTopology;
import field.math.linalg.CoordinateFrame;

public class HierarchyOfCoordinateFrames {

	/**
	 * this is supposed to be the fast and dumb version
	 */
	public class Element implements iInplaceProvider<CoordinateFrame>, iHasTopology, iAcceptor<CoordinateFrame> {
		public CoordinateFrame localFrame = new CoordinateFrame();

		public CoordinateFrame worldFrame = new CoordinateFrame();

		public Element parent;

		public List<Element> children = new ArrayList<Element>();

		public String name;

		CoordinateFrame tmp = new CoordinateFrame();

		int modCountParent = -1;

		int modCountHere = -1;

		int globalModCountHere = -1;

		public CoordinateFrame get(CoordinateFrame o) {
			if (o == null)
				o = new CoordinateFrame();
			getWorld(o);
			return o;
		}

		public iTopology getTopology() {
			return HierarchyOfCoordinateFrames.this.getTopology();
		}

		public void getWorld(CoordinateFrame out) {
			// first, lets just check to see if anything has changed
			// if (globalModCountHere == globalModCount) {
			// out.setValue(cachedWorld);
			// }

			// next, lets see if just us has changed
			Element p = this;
			Element root = this;
			boolean chainDirty = false;
			while (p != null) {
				if (p.parent != null) {
					if (p.modCountParent != p.parent.modCountHere) {
						chainDirty = true;
					}
				}
				p = p.parent;
			}

			if (chainDirty) {
				if (parent != null) {
					parent.getWorld(tmp);
				} else {
					tmp = new CoordinateFrame();
				}
			} else {
				if (parent != null) {
					tmp.setValue(parent.worldFrame);
				} else {
					tmp = new CoordinateFrame();

				}
			}

			tmp.forceTRS();
			localFrame.forceTRS();
			worldFrame.forceTRS();
			
			worldFrame.multiply(tmp, localFrame);

			modCountParent = parent != null ? parent.modCountHere : 0;
			this.globalModCountHere = globalModCount;

			out.setValue(worldFrame);
		}

		public void setLocal(CoordinateFrame frame) {
			this.localFrame.setValue(frame);
			modCountHere++;
			globalModCount++;
		}
		
		public void dirty()
		{
			modCountHere++;
			globalModCount++;
		}

		public CoordinateFrame getLocal() {
			return this.localFrame;
		}

		@Override
		public String toString() {
			return "element:" + name;
		}

		public iAcceptor<CoordinateFrame> set(CoordinateFrame to) {
			setLocal(to);
			return this;
		}
	}

	

	int globalModCount = -1;

	HashMap<String, Element> namedElements = new HashMap<String, Element>();

	public Element getChildOf(Element childOf, String name) {
		Element e = namedElements.get(name);
		if (e == null) {
			namedElements.put(name, e = new Element());
			e.parent = childOf;
			e.name = name;
			e.parent.children.add(e);
		}
		return e;
	}

	public Element getNamed(String name) {
		return namedElements.get(name);
	}

	public Element getRoot(String name) {
		Element e = namedElements.get(name);
		if (e == null) {
			namedElements.put(name, e = new Element());
			e.name = name;
		}
		return e;
	}

	public Set<Element> getRoots() {
		LinkedHashSet<Element> ret = new LinkedHashSet<Element>();
		for (Element e : namedElements.values()) {
			if (e.parent == null)
				ret.add(e);
		}
		return ret;
	}

	public iTopology<Element> getTopology() {
		return new iTopology<Element>() {

			public List<Element> getChildrenOf(Element of) {
				return of.children;
			}

			public List<Element> getParentsOf(Element of) {
				if (of.parent == null)
					return null;
				return Collections.singletonList(of.parent);
			}
		};
	}

}
