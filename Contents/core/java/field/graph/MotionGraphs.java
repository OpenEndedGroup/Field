package field.graph;

import java.util.Collection;

import field.context.Context.Cobj;
import field.core.plugins.drawing.opengl.CachedLine;
import field.util.Dict.Prop;

public class MotionGraphs {

	static public final Prop<iAxis> x_axis = new Prop<iAxis>("x_axis");
	static public final Prop<iAxis> y_axis = new Prop<iAxis>("y_axis");

	static public final Prop<iLayout> mark = new Prop<iLayout>("mark");

	static public final Prop<Collection<Cobj>> dataSet = new Prop<Collection<Cobj>>("dataSet");

	static public final Prop<Cobj> datum = new Prop<Cobj>("datum");

	static public final Prop<iLayout> markMaker = new Prop<iLayout>("markMaker");

	static public final Prop<Collection<CachedLine>> geometry = new Prop<Collection<CachedLine>>("geometry");
	
	static public interface iLayout
	{
		public void begin();
		public void layout();
		public void end();
	}

	static public interface iDraw
	{
		public void draw();
	}

	static public interface iAxis
	{
		public float map(Cobj x);
		public float mapNumber(float n);
	}

	static public class Graph extends Cobj
	{
		
	}
		
	
}
