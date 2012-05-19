package field.namespace.change;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * @author marc Created on May 6, 2003
 */
public class SimpleChangable implements iChangable, Serializable {

	private class ModCount implements iChangable.iModCount, Serializable {

		transient int dirty = -1;

		transient Object data = null;

		transient iRecompute default_recompute;

		iChangable.iModCount[] localChain = new iChangable.iModCount[0];

		public iModCount clear(Object data) {
			this.data = data;
			dirty = count;
			for(int i=0;i<localChain.length;i++)
			{
				localChain[i].clear(null);
			}
			return this;
		}

		public Object data() {
			if (default_recompute != null) return data(default_recompute);

			return ((dirty != count) | checkChain() | checkLocalChain(localChain)) ? null : data;
		}

		public Object data(iRecompute recomp) {
			if ((dirty != count) | checkChain() | checkLocalChain(localChain)) {
				Object r = recomp.recompute();
				clear(r);
				return r;
			} else
				return data;
		}

		public boolean hasChanged() {
			boolean d = (dirty != count) | checkChain() | checkLocalChain(localChain);
			return d;
		}

		public iModCount localChainWith(iModCount[] localChain)
		{
			this.localChain = localChain;
			return this;
		}

		public iModCount setRecompute(iRecompute r) {
			this.default_recompute = r;
			return this;
		}

		@Override
		public String toString() {
			return "child of <" + SimpleChangable.this.toString() + ">";
		}

		private boolean checkLocalChain(iModCount[] localChain2) {

			for(int i=0;i<localChain2.length;i++)
			{
//				System.err.println("localChain <"+i+"> is <"+localChain2[i]+"> is <"+localChain2[i].hasChanged()+">");
//				if (localChain2[i].hasChanged()) new Exception().printStackTrace();
				if (localChain2[i].hasChanged()) return true;
			}
			return false;
		}
	}

	transient Map map = new WeakHashMap();

	int count = 0;

	iChangable.iModCount[] chainCount;

	public SimpleChangable chainWith(iChangable.iModCount[] chainCount) {
		this.chainCount = chainCount;
		return this;
	}

	public void dirty() {
		count++;
	}

	public iModCount getModCount(Object withRespectTo) {
		iModCount count = (iModCount) map.get(withRespectTo);
		if (count == null) {
			map.put(withRespectTo, count = new ModCount());
		}
		if (map.size() == 0) throw new IllegalArgumentException();

		return count;
	}

	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		stream.defaultReadObject();
		map = new WeakHashMap((Map) stream.readObject());
	}

	private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
		stream.defaultWriteObject();
		stream.writeObject(new HashMap(map));
	}

	protected boolean checkChain() {
		if (chainCount == null) return false;
		boolean ret = false;
		for (int i = 0; i < chainCount.length; i++) {
			if (chainCount[i].hasChanged()) {
				dirty();

				//?? why was this here? I'm sure it has something to do with diagram
				//chainCount[i].clear(null);

				// I'm going to try this instead
				chainCount[i].data();
				ret = true;
			}
		}
		return ret;
	}

}