package field.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.List;

import sun.awt.image.ByteBandedRaster;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class FloatBufferSerializer implements Converter {

	public boolean canConvert(Class type) {
		return FloatBuffer.class.isAssignableFrom(type);
	}

	public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
		writer.startNode("floatBuffer");

		FloatBuffer c = (FloatBuffer) source;
		c.rewind();
		float[] a = new float[c.capacity()];
		c.get(a);
		context.convertAnother(a);
		writer.endNode();
	}

	public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
		reader.moveDown();
		float[] channel = (float[]) context.convertAnother(null, float[].class);

		FloatBuffer fb = ByteBuffer.allocateDirect(4 * channel.length).order(ByteOrder.nativeOrder()).asFloatBuffer();
		fb.put(channel);
		fb.rewind();

		reader.moveUp();

		return fb;
	}
}
