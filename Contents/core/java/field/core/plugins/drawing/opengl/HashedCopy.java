package field.core.plugins.drawing.opengl;

import java.util.HashMap;

public class HashedCopy {

	private long hash = -1;

	long accumulatedHash = 0;

	public HashedCopy() {
	}

	public class CopyRecord {
		int index;
		long accumulatedHash;
		float next = 0;
		
		public CopyRecord(int index, long accumulatedHash) {
			this.index = index;
			this.accumulatedHash = accumulatedHash;
		}
		@Override
		public String toString() {
			return index+"@"+accumulatedHash;
		}
	}

	HashMap<Long, CopyRecord> cached = new HashMap<Long, CopyRecord>();

	public boolean copyInto(HashedCopy target, int targetPosition) {
		CopyRecord c = cached.get(target.getHash());

		if (c != null && c.index == targetPosition && c.accumulatedHash == target.accumulatedHash) {
			target.accumulatedHash = target.accumulatedHash * 31L + getHash();
			return false;
		} else {
//			System.out.println(" miss <"+target.getHash()+" | "+targetPosition+"@"+target.accumulatedHash+"> <"+cached+">");
			cached.put(target.getHash(), new CopyRecord(targetPosition, target.accumulatedHash));
			target.accumulatedHash = target.accumulatedHash * 31L + getHash();
			return true;
		}
	}
	

	public void reset() {
		accumulatedHash = 0;
	}

	long getHash() {
		if (hash==-1)
			hash = System.identityHashCode(this) + System.nanoTime();
		return hash;
	}

}
