package field.math.util;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * @author marc
 */
public class Hungarian
{
	// what is this, fortran77 ?
	static public final int maxHungarianSize = 500;

	// convience

	private final float[] temp = new float[2];
	private final IntBuffer tempDistance = ByteBuffer.allocateDirect(maxHungarianSize*maxHungarianSize*4).asIntBuffer();
	private final IntBuffer tempAnswer = ByteBuffer.allocateDirect(maxHungarianSize*4).asIntBuffer();

	/**
	 * returns the assigment such that each of m is maped to a different one of n, maximizing the benifits[m*n] array (which is stored as m first e.g. x*m+y)
	 * answer can be null, and is returned
	 */
	public IntBuffer maximumAssigment(FloatBuffer benifits, int m, int n, IntBuffer answer)
	{
		if (true) throw new IllegalArgumentException(" this native code has changed");
		rangeOf(benifits, temp);
		for(int i=0;i<benifits.capacity();i++)
		{
			tempDistance.put(i,(int)(0.05f*Integer.MAX_VALUE*(benifits.get(i)-temp[1])/(temp[0]-temp[1])));
		}
		tempDistance.rewind();
		if (answer == null) answer = ByteBuffer.allocateDirect(m*4).asIntBuffer();
		perform(tempDistance, m, n, answer);
		return answer;
	}

	public IntBuffer minimumAssignment(FloatBuffer distances, int m, int n, IntBuffer answer)
	{
		if (true) throw new IllegalArgumentException(" this native code has changed");
		rangeOf(distances, temp);
		for(int i=0;i<distances.capacity();i++)
		{
//			tempDistance.put(i,(int)(0.05f*Integer.MAX_VALUE*(temp[0]-temp[1])/(distances.get(i)-temp[1]+1)));
			tempDistance.put(i,(int)((1.0f/distances.capacity())*Integer.MAX_VALUE*(temp[0]-temp[1])/(distances.get(i)-temp[1]+1)));
		}
		tempDistance.rewind();
		if (answer == null) answer = ByteBuffer.allocateDirect(m*4).asIntBuffer();


		perform(tempDistance, m, n, answer);
		return answer;
	}

	// the actual method
	native public void perform(Buffer matrix, int rows, int cols, Buffer output);
	
	protected void rangeOf(FloatBuffer a, float[] out)
	{
		out[0] = Float.NEGATIVE_INFINITY;
		out[1] = Float.POSITIVE_INFINITY;
		for(int i=0;i<a.capacity();i++)
		{
			if (a.get(i)<out[1]) out[1] = a.get(i);
			if (a.get(i)>out[0]) out[0] = a.get(i);
		}
	}


}
