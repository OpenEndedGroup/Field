package field.graph;

import java.util.ArrayList;
import java.util.Collection;

import field.context.Context.Cobj;
import field.context.Context.DebugToString;
import field.graph.MotionGraphs.Graph;
import field.graph.MotionGraphs.iAxis;
import field.graph.MotionGraphs.iLayout;
import field.launch.iLaunchable;
import field.math.linalg.Vector2;
import field.util.Dict.Prop;

public class T_Example implements iLaunchable {

	static public final Prop<Vector2> at = new Prop<Vector2>("at");
	
	static public class Mark extends Cobj implements iLayout {
		
		
		@Override
		public void layout() {
			
			
			iAxis x = getProperty(MotionGraphs.x_axis);
			iAxis y = getProperty(MotionGraphs.y_axis);

			Cobj datum = getProperty(MotionGraphs.datum);

			setProperty(at, new Vector2(x.map(datum), y.map(datum)));
			;//System.out.println(" laying out mark <"+this+">");
			
		}
		
		@Override
		public String toString() {
			return "m@"+getProperty(at);
		}
		
		@Override
		public void begin() {
		}
		
		@Override
		public void end() {
		}
		
	}

	static public class MarkMaker extends Cobj implements iLayout {

		@Override
		public void layout() {

			Collection<Cobj> data = getProperty(MotionGraphs.dataSet);
			for (Cobj c : data) {
				Mark m = new Mark();
				c.setProperty(MotionGraphs.mark, m);

				m.layout();
			}
		}

		@Override
		public void begin() {
		}
		
		@Override
		public void end() {
		}
	}

	static public final Prop<Number> x = new Prop<Number>("x");
	static public final Prop<Number> y = new Prop<Number>("y");
	
	static public class Datum extends Cobj
	{
		public Datum(float x, float y)
		{
			setProperty(T_Example.x, x);
			setProperty(T_Example.y, y);
			this.setProperty(MotionGraphs.datum, this);
		}
		
		@Override
		public String toString() {
			return "datum["+getProperty(x)+", "+getProperty(y)+"]";
		}
	}
	
	static public class Axis extends Cobj implements iLayout, iAxis {

		private final Prop<Number> dimension;
		private float min;
		private float max;
		private float length;
		private float offset;

		public Axis(Prop<Number> dimension, float length, float offset) {
			this.dimension = dimension;
			this.length = length;
		}

		@Override
		public void layout() {
			Collection<Cobj> all = getProperty(MotionGraphs.dataSet);

			min = Float.POSITIVE_INFINITY;
			max = Float.NEGATIVE_INFINITY;

			for (Cobj c : all) {
				Number d = c.getProperty(dimension);
				if (d != null) {
					min = Math.min(min, d.floatValue());
					max = Math.max(max, d.floatValue());
				}
			}

			if (min == Float.POSITIVE_INFINITY) {
				min = Float.NaN;
				max = Float.NaN;
			}
		}

		@Override
		public float map(Cobj x) {
			Number d = x.getProperty(dimension);
			if (d == null)
				return Float.NaN;
			else
				return mapNumber(d.floatValue());
		}

		public float mapNumber(float f) {
			return offset+length*(f - min) / (max - min);
		}

		
		
		@Override
		public String toString() {
			return "axis["+min+" -> "+max+"]";
		}
		
		@Override
		public void begin() {
			
		}
		
		@Override
		public void end() {
			
		}

	}

	private Graph graph;

	@Override
	public void launch() {

		graph = new Graph();

		graph.setProperty(MotionGraphs.x_axis, new Axis(x, 100,0));
		graph.setProperty(MotionGraphs.y_axis, new Axis(y, 100,0));
		
		ArrayList<Cobj> data = new ArrayList<Cobj>();
		
		data.add(new Datum(0,0));
		data.add(new Datum(1,0));
		data.add(new Datum(4,2));
		data.add(new Datum(8,5));

		graph.setProperty(MotionGraphs.dataSet, data);
		graph.setProperty(MotionGraphs.markMaker, new MarkMaker());

		graph.proxy(iLayout.class).layout();

		graph.depthFirst(new DebugToString());
		
	}

}
