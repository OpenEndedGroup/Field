package field.util;

import java.util.List;


import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import field.namespace.diagram.Channel;
import field.namespace.diagram.DiagramZero.iMarker;

public class ChannelSerializer implements Converter {

	public boolean canConvert(Class type) {
		return Channel.class.isAssignableFrom(type);
	}

	public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
		writer.startNode("class");
		context.convertAnother(source.getClass());
		writer.endNode();
		writer.startNode("markers");

		Channel c = (Channel) source;
		List<iMarker> i = c.getIterator().remaining();
		context.convertAnother(i);
		writer.endNode();
	}

	public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
		reader.moveDown();
		Class c = (Class) context.convertAnother(null, Class.class);
		reader.moveUp();

		try {
			Channel channel = (Channel) c.newInstance();

			reader.moveDown();

			List<iMarker> i = (List<iMarker>) context.convertAnother(channel, List.class);
			
			reader.moveUp();

			return channel;
			
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		return null;
	}
}
