package field.core.plugins.drawing.opengl;

import java.awt.geom.CubicCurve2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import field.math.linalg.Vector2;

public class PathFlattener {

	private final CachedLine c;

	public class Mapping {
		float dotStart; // node.t
		float dotEnd; // node.t
		Vector2 start;
		Vector2 end;
		float cumulativeDistanceAtEnd;
		
		@Override
		public String toString() {
			return dotStart+"->"+dotEnd+" > "+cumulativeDistanceAtEnd;
		}
	}

	List<Mapping> mappings = new ArrayList<Mapping>();
	private final float tol;

	public PathFlattener(CachedLine c, float tol) {
		this.c = c;
		this.tol = tol;
		CachedLineCursor cursor = new CachedLineCursor(c);
		int index = 0;

		while (cursor.hasNextSegment()) {
			if (cursor.nextIsCubic()) {
				Vector2 a = new Vector2();
				Vector2 c1 = new Vector2();
				Vector2 c2 = new Vector2();
				Vector2 b = new Vector2();
				cursor.nextCubicFrame(a, c1, c2, b);
				emitCubicFrame(index - 1, index, a, c1, c2, b);
			} else if (!cursor.nextIsSkip()) {
				Vector2 a = new Vector2();
				Vector2 b = new Vector2();
				cursor.nextLinearFrame(a, b);
				emitLinearFrame(index - 1, index, a, b);
			}
			index++;
			cursor.next();
		}
	}
	
	
	

	Comparator searchDistance = new Comparator() {
		public int compare(Object o1, Object o2) {
			float f1 = o1 instanceof Number ? ((Number) o1).floatValue() : ((Mapping) o1).cumulativeDistanceAtEnd;
			float f2 = o2 instanceof Number ? ((Number) o2).floatValue() : ((Mapping) o2).cumulativeDistanceAtEnd;
			return Float.compare(f1, f2);
		}
	};

	Comparator searchDot = new Comparator() {
		public int compare(Object o1, Object o2) {
			float f1 = o1 instanceof Number ? ((Number) o1).floatValue() : ((Mapping) o1).dotEnd;
			float f2 = o2 instanceof Number ? ((Number) o2).floatValue() : ((Mapping) o2).dotEnd;
			return Float.compare(f1, f2);
		}
	};

	public float length() {
		if (mappings.size()==0) return 0;
		return mappings.get(mappings.size() - 1).cumulativeDistanceAtEnd;
	}

	public float lengthToDot(float length) {
		int found = Collections.binarySearch((List) mappings, new Float(length), searchDistance);
		if (found >= 0)
			return mappings.get(found).dotEnd;

		int leftOf = -found - 1;
		int rightOf = leftOf - 1;
		if (leftOf > mappings.size() - 1)
			return mappings.get(mappings.size() - 1).dotEnd;

		float l1 = rightOf >= 0 ? mappings.get(rightOf).cumulativeDistanceAtEnd : 0;
		float l2 = mappings.get(leftOf).cumulativeDistanceAtEnd;

		if (l2 == l1)
			return mappings.get(leftOf).dotEnd;
		float x = (length - l1) / (l2 - l1);
		float de = mappings.get(leftOf).dotStart * (1 - x) + x * mappings.get(leftOf).dotEnd;

		return de;
	}

	public float dotToLength(float dot) {
		
//		System.out.println(" dot to length <"+dot+">");
//		System.out.println(" mappings :"+mappings);
		
		int found = Collections.binarySearch((List) mappings, new Float(dot), searchDot);
//		System.out.println(" found <"+found+">");
		if (found >= 0)
			return mappings.get(found).cumulativeDistanceAtEnd;

		int leftOf = -found - 1;
		int rightOf = leftOf - 1;
		
//		System.out.println(" left right <"+leftOf+"> <"+rightOf+">");
		
		if (leftOf > mappings.size() - 1)
			return mappings.get(mappings.size() - 1).cumulativeDistanceAtEnd;


		float l1 = mappings.get(leftOf).dotStart;
		float l2 = mappings.get(leftOf).dotEnd;
		
//		System.out.println(" left <"+l1+"> <"+l2+">");

		if (l2 == l1)
			return mappings.get(rightOf).cumulativeDistanceAtEnd;
		float x = (dot - l1) / (l2 - l1);
		float de = (rightOf>=0 ? mappings.get(rightOf).cumulativeDistanceAtEnd : 0) * (1 - x) + x * mappings.get(leftOf).cumulativeDistanceAtEnd;

		return de;
	}

	private void emitLinearFrame(float dotStart, float dotEnd, Vector2 a, Vector2 b) {
		Mapping m = new Mapping();
		m.start = a;
		m.end = b;
		m.dotStart = dotStart;
		m.dotEnd = dotEnd;
		if (mappings.size() == 0)
			m.cumulativeDistanceAtEnd = b.distanceFrom(a);
		else
			m.cumulativeDistanceAtEnd = b.distanceFrom(a) + mappings.get(mappings.size() - 1).cumulativeDistanceAtEnd;
		mappings.add(m);
	}

	Vector2 tmp = new Vector2();

	private void emitCubicFrame(float dotStart, float dotEnd, Vector2 a, Vector2 c1, Vector2 c2, Vector2 b) {

		float f = flatnessFor(a, c1, c2, b);
		if (f > tol) {
			Vector2 c12 = new Vector2();
			Vector2 c21 = new Vector2();
			Vector2 m = new Vector2();

			LineUtils.splitCubicFrame(a, c1 = new Vector2(c1), c2 = new Vector2(c2), b, 0.5f, c12, m, c21, tmp);

			float mp = dotStart + (dotEnd - dotStart) * 0.5f;

			emitCubicFrame(dotStart, mp, a, c1, c12, m);
			emitCubicFrame(mp, dotEnd, m, c21, c2, b);
		} else {
			emitLinearFrame(dotStart, dotEnd, a, b);
		}
	}

	private float flatnessFor(Vector2 a, Vector2 c1, Vector2 c2, Vector2 b) {
		return (float) CubicCurve2D.getFlatness(a.x, a.y, c1.x, c1.y, c2.x, c2.y, b.x, b.y);
	}

}
