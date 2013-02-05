package field.graphics.dynamic;

import java.util.Collections;
import java.util.List;

import field.core.plugins.drawing.opengl.SimpleTess;
import field.math.linalg.Vector3;

public class DynamicMesh_tessSupport {

	private SimpleTess t;

	public DynamicMesh_tessSupport(final DynamicMesh_long into) {
		t = new SimpleTess() {

			@Override
			protected int nextVertex(Vector3 position) {
				return into.nextVertex(position);
			}

			@Override
			protected void nextFace(int a, int b, int c) {
				
//				System.out.println(" _tessSupport <"+a+" "+b+" "+c+">");
				
				into.nextFace(a, b, c);
			}

			@Override
			protected void decorateVertex(int vertex, List<Object> properties) {
			}
		};
	}

	public void tessSingleContour(List<Vector3> m) {
		t.begin();
		t.beginContour();
		for (int i = 1; i < m.size(); i++) {
			t.line(m.get(i - 1), m.get(i), Collections.EMPTY_LIST, Collections.EMPTY_LIST);
		}
		t.endContour();
		t.end();
	}

}
