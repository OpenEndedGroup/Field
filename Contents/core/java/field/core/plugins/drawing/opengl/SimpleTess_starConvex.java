package field.core.plugins.drawing.opengl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import field.math.linalg.Vector2;
import field.math.linalg.Vector3;
import field.math.linalg.Vector4;

public abstract class SimpleTess_starConvex {

	
	public List<Vector3> positions;
	public List<Map<Integer, Object>> properties;
	
	public SimpleTess_starConvex()
	{
		
	}
	
	public void beginContour()
	{
		positions = new ArrayList<Vector3>();  
		properties = new ArrayList<Map<Integer, Object>>();
	}
	
	public void next(Vector3 p, Map<Integer, Object> properties)
	{
		positions.add(p);
		this.properties.add(properties);
	}
	
	public void endContour()
	{
		if (positions.size()<3) return;
		
		Vector3 a = new Vector3();
		for(int i=0;i<positions.size();i++) a.add(positions.get(i));
		
		a.scale(1f/positions.size());

		Map<Integer, Object> center = new LinkedHashMap<Integer, Object>();
		Map<Integer, Integer> count = new LinkedHashMap<Integer, Integer>();
		
		for(Map<Integer, Object> p : properties)
		{
			for(Map.Entry<Integer, Object> pp : p.entrySet())
			{
				Integer x = count.get(pp.getKey());
				if (x==null) x = 0;
				count.put(pp.getKey(), x+1);
			
				Object o = center.get(pp.getKey());
				o = add(o, pp.getValue());
				center.put(pp.getKey(), o);
			}
		}
		
		for(Map.Entry<Integer, Object> pp : center.entrySet())
		{
			Integer c = count.get(pp.getKey());
			Object v = pp.getValue();
			pp.setValue(div(v, c));
		}
		
		
		int f = nextVertex(a, center);
		for(int i=0;i<positions.size();i++)
		{
			nextVertex(positions.get(i), properties.get(i));
		}
		
		for(int i=0;i<positions.size();i++)
		{
			nextFace(f+i+1, 1+f+((i+1)%positions.size()), f);
		}
		
		
	}

	abstract protected int nextVertex(Vector3 a, Map<Integer, Object> center);
	abstract protected void nextFace(int a, int b, int c);
	
	private Object div(Object v, Integer c) {
		if (v==null) return null;
		if (v instanceof Number)
			return ((Number)v).floatValue()/c;
		if (v instanceof Vector2)
			return new Vector2( (Vector2)v).scale(1f/c);
		if (v instanceof Vector3)
			return new Vector3( (Vector3)v).scale(1f/c);
		if (v instanceof Vector4)
			return new Vector4( (Vector4)v).scale(1f/c);
		return null;
	}

	private Object add(Object o, Object value) {
		if (o == null) return value;
		if (value == null) return o;
		
		if (o instanceof Number)
			return ((Number)o).floatValue()+((Number)value).floatValue();
		if (o instanceof Vector2)
			return new Vector2( (Vector2)o).add((Vector2)value);
		if (o instanceof Vector3)
			return new Vector3( (Vector3)o).add((Vector3)value);
		if (o instanceof Vector4)
			return new Vector4( (Vector4)o).add((Vector4)value);
		
		return null;
	}
	
}
