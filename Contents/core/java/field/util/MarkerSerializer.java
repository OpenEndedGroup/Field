package field.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;


import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;

import field.namespace.diagram.Channel;
import field.namespace.diagram.DiagramZero.iMarker;

public class MarkerSerializer implements Converter {

	private final Mapper mapper;

	public MarkerSerializer(Mapper mapper)
	{
		this.mapper = mapper;
	}
	
	public boolean canConvert(Class type) {
		return iMarker.class.isAssignableFrom(type);
	}

	public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
		iMarker marker = (iMarker) source;

		writer.startNode("marker");
		writer.startNode("class");
		context.convertAnother(marker.getClass());
		writer.endNode();
		writer.startNode("time");
		context.convertAnother(marker.getTime());
		writer.endNode();
		writer.startNode("duration");
		context.convertAnother(marker.getDuration());
		writer.endNode();
		writer.startNode(mapper.serializedClass(marker.getPayload().getClass()));
		context.convertAnother(marker.getPayload());
		writer.endNode();
		writer.startNode(mapper.serializedClass(marker.getRootChannel().getClass()));
		context.convertAnother(marker.getRootChannel());
		writer.endNode();
		writer.endNode();
	}

	public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
		try {
			reader.moveDown();
			reader.moveDown();
			Class markerClass = (Class) context.convertAnother(null, Class.class);
			reader.moveUp();
			reader.moveDown();
			double time = (Double) context.convertAnother(null, Double.TYPE);
			reader.moveUp();
			reader.moveDown();
			double duration = (Double) context.convertAnother(null, Double.TYPE);
			reader.moveUp();
			reader.moveDown();
			Object payload = (Object) context.convertAnother(null, mapper.realClass(reader.getNodeName()) );
			reader.moveUp();
			reader.moveDown();
			Object channel;
			channel = (Object) context.convertAnother(null, mapper.realClass(reader.getNodeName()));
			reader.moveUp();
			reader.moveUp();

			Constructor constructor;
			constructor = markerClass.getConstructor(new Class[] { Double.TYPE, Double.TYPE, Object.class, Channel.class});
			Object marker = constructor.newInstance(new Object[] { time, duration, payload, channel});
			return marker;
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} 
		return null;
	}

}
