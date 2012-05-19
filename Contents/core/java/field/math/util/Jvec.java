package field.math.util;

import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import field.math.BaseMath;
import field.util.MiscNative;


/**
	files for makeing altivec calls from java
 */
public class Jvec
{
	public class FFTInit
	{
		protected int ref;
		protected FloatBuffer tempI;
		protected FloatBuffer tempO;
		protected FloatBuffer temp3;
		protected int log2Size;
		protected int size;
		FFTInit(int i, int log2Size)
		{
			ref= i;
			size= 1 << log2Size;
			int len= 4 * (size);
			tempI= newFloatBuffer(len);
			tempO= newFloatBuffer(len);
			temp3= newFloatBuffer(len);
			this.log2Size= log2Size;
		}

		// returns index that you'll want to look at
		// actually, the docs are not clear enough to know what is going on. use performComplexFFT
		public int readRealTransform(int element)
		{
			if (element == 0)
				return 0;
			if (element == size - 1)
				return 1;
			return element + 1;
		}

		/**
		 * @return
		 */
		public int size()
		{
			return log2Size;
		}

	}

	static {
		MiscNative.load();
	}
	static public void main(String[] s)
	{
		Jvec j= new Jvec();

		int l2= 8;
		int len= 1 << l2;

		FloatBuffer f1= j.newFloatBuffer(len);
		FloatBuffer f2= j.newFloatBuffer(len);
		for (int i= 0; i < f1.capacity(); i++)
		{
			f1.put(i, 1000 * (float) Math.cos(2 * Math.PI * 3 * i / f1.capacity()));
			f2.put(i, 0);
		}

		Jvec.FFTInit in= j.initializeFFT(l2);

		for (int i= 0; i < 10; i++)
		{
			j.performComplexFFT(in, f1, f2, true);

			j.vmul(f1, 1 / (float) len, f1);
			j.vmul(f2, 1 / (float) len, f2);

			j.performComplexFFT(in, f1, f2, false);
		}

	}
	static public void main0(String[] s)
	{
		Jvec t= new Jvec();

		FloatBuffer f1= t.newFloatBuffer(1000000);
		FloatBuffer f2= t.newFloatBuffer(1000000);
		for (int i= 0; i < f1.capacity(); i++)
		{
			f1.put(i, i + 1);
			f2.put(i, 1 / (float) (i + 1));
		}
		FloatBuffer f3= t.newFloatBuffer(1000000);
		//		t.vadd(f1,f2,f3);


	}

	static public String printAddress(Object i)
	{
		try
		{
			Field f= Buffer.class.getDeclaredField("address");
			f.setAccessible(true);
			long address= f.getLong(i);
			return address + "";
		}
		catch (SecurityException e)
		{
			e.printStackTrace();
		}
		catch (IllegalArgumentException e)
		{
			e.printStackTrace();
		}
		catch (NoSuchFieldException e)
		{
			e.printStackTrace();
		}
		catch (IllegalAccessException e)
		{
			e.printStackTrace();
		}
		return null;
	}

	// one vector, one scalar -> one vector

	static public String toString(FloatBuffer f)
	{
		String s= "";
		for (int i= 0; i < f.capacity(); i++)
		{
			s += f.get(i) + "\n";
		}
		return s;
	}
	FloatBuffer vmin4Distance_tmp = newFloatBuffer(4);

	public void complexToMagnitude(FloatBuffer realIn, FloatBuffer imagIn, FloatBuffer magOut)
	{
//		assert(
//			(realIn.capacity() == imagIn.capacity())
//				&& (imagIn.capacity() == magOut.capacity())) : "dimension mismatch";
		performComplexToM(realIn, imagIn, magOut, magOut.capacity());
	}
	public void complexToMagnitudePhase(FloatBuffer realIn, FloatBuffer imagIn, FloatBuffer magOut, FloatBuffer phaseOut)
	{
		assert(
			(realIn.capacity() == imagIn.capacity())
				&& (imagIn.capacity() == magOut.capacity())
				&& (magOut.capacity() == phaseOut.capacity())) : "dimension mismatch";
		performComplexToMP(realIn, imagIn, magOut, phaseOut, phaseOut.capacity());
	}

	// two vectors -> one vector ---------------------------------- ---------------------------------- ----------------------------------

	public void convertARGBToFloat(ByteBuffer inBuffer,FloatBuffer outBuffer){
		nativeConvertARGBToFloat(inBuffer,outBuffer,outBuffer.capacity());
	}
	native public void fastNDCBoundingBox(FloatBuffer model, FloatBuffer projection, FloatBuffer data, int numVertex, FloatBuffer output);

	public int findMaxMag(FloatBuffer buffer){
		return nativeFindMaxMag(buffer,buffer.capacity());
	}
	public int 	findMinAbs(FloatBuffer buffer){
		return nativeFindMinAbs(buffer,buffer.capacity());
	}


	public native void freeAlignedStorage(Buffer b);
	// dsp code --------------------------------------------------------------------------------------------------------------------------
	public FFTInit initializeFFT(int log2Size)
	{
		FFTInit i = new FFTInit(nativeInitializeFFT(log2Size), log2Size);
		return i;
	}

	public int log2Of(int s)
	{
		return (int)Math.floor(Math.log(s)/Math.log(2));
	}
	native public void nativeConvertARGBToFloat(ByteBuffer byteBuffer,FloatBuffer floatBuffer, int length);

	native public int nativeFindMaxMag(FloatBuffer fButter, int length);
	native public int nativeFindMinAbs(FloatBuffer fBuffer, int length);

	// three vectors -> one vector

	native public int nativeInitializeFFT(int l);
	//ie native method z eQuals a X plus Y
	native public void nativeZeQaXpY(FloatBuffer x,float alpha,FloatBuffer y, FloatBuffer z, int length);

	public native ByteBuffer newAlignedStorage(int i);

	public FloatBuffer newFloatBuffer(int i)
	{
//		FloatBuffer f =  newAlignedStorage(i * 4).asFloatBuffer();
//		vzero(f);
		return ByteBuffer.allocateDirect(i*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
	}
	public void performComplex2DFFTOutOfPlace(FFTInit i, FloatBuffer in, FloatBuffer inI, FloatBuffer outR, FloatBuffer outI)
	{
		int l2 = (int)(Math.log(Math.sqrt(in.capacity()))/Math.log(2));
		int w = (int)Math.sqrt(in.capacity());
		performComplex2DFFTOutOfPlace(i.ref, in, inI, outR, outI, w, l2,l2);
	}


	public native void performComplex2DFFTOutOfPlace(int ref, FloatBuffer in, FloatBuffer zero, FloatBuffer outR, FloatBuffer outI, int w, int c, int c2);
	//j.vmul(f1, 1/(float)Math.sqrt(len), f1);
	//j.vmul(f2, 1/(float)Math.sqrt(len), f2);
	// might be what you are looking for if you want a round trip to not scale by len
	public void performComplexFFT(FFTInit init, FloatBuffer realInplace, FloatBuffer imagInplace, boolean forward)
	{
		performComplexFFT(init.ref, realInplace, imagInplace, init.tempI, init.tempO, init.log2Size, forward ? 1 : 0);
	}

	native public void performComplexFFT(int i, FloatBuffer inR, FloatBuffer outI, FloatBuffer temp1, FloatBuffer temp2, int l2, int com);
	//j.vmul(f1, 1/(float)Math.sqrt(len), f1);
	//j.vmul(f2, 1/(float)Math.sqrt(len), f2);
	// might be what you are looking for if you want a round trip to not scale by len
	public void performComplexFFTOutOfPlace(
		FFTInit init,
		FloatBuffer realIn,
		FloatBuffer imagIn,
		FloatBuffer realOut,
		FloatBuffer imagOut,
		boolean forward)
	{
		performComplexFFTOutOfPlace(init.ref, realIn, imagIn, realOut, imagOut, init.tempI, init.tempO, init.log2Size, forward ? 1 : 0);
	}

	native public void performComplexFFTOutOfPlace(
		int i,
		FloatBuffer inR,
		FloatBuffer inI,
		FloatBuffer outR,
		FloatBuffer outI,
		FloatBuffer temp1,
		FloatBuffer temp2,
		int l2,
		int com);
	public native void performComplexToM(FloatBuffer r, FloatBuffer i, FloatBuffer m, int ll);


	public native void performComplexToMP(FloatBuffer r, FloatBuffer i, FloatBuffer m, FloatBuffer p, int ll);
	/**
	 * currently only featureDimension = 1 is supported
	 * @param features1In
	 * @param weights1In
	 * @param features2In
	 * @param weights2in
	 * @param featureDimension
	 * @param weightsIn
	 * @return
	 */
	public float performEMD(FloatBuffer features1In, FloatBuffer weights1In, FloatBuffer features2In, FloatBuffer weights2in, int featureDimension)
	{
		return performEMD(
			features1In,
			features1In.capacity(),
			weights1In,
			weights1In.capacity(),
			features2In,
			features2In.capacity(),
			weights2in,
			weights2in.capacity(),
			featureDimension);
	}


	native public float performEMD(
		FloatBuffer features1In,
		int i,
		FloatBuffer weights1In,
		int j,
		FloatBuffer features2In,
		int k,
		FloatBuffer weights2in,
		int l,
		int featureDimension);

	public void performReal2DFFTOutOfPlace(FFTInit i, FloatBuffer in, FloatBuffer zero, FloatBuffer outR, FloatBuffer outI)
	{
		int l2 = (int)(Math.log(Math.sqrt(in.capacity()))/Math.log(2));
		int w = (int)Math.sqrt(in.capacity());
		performReal2DFFTOutOfPlace(i.ref, in, zero, outR, outI, w, l2,l2);
	}
	public native void performReal2DFFTOutOfPlace(int ref, FloatBuffer in, FloatBuffer zero, FloatBuffer outR, FloatBuffer outI, int w, int c, int c2);

	// you'll need to use FFTInit.readRealTransform to get at the results of this thing, because the results are packed in a particular way
	// actually, the docs are not clear enough to know what is going on. use performComplexFFT
	public void performRealFFT(FFTInit init, FloatBuffer realInplace, FloatBuffer imagOut)
	{
		performRealFFT(init.ref, realInplace, imagOut, init.tempI, init.tempO, init.log2Size);
	}

	native public void performRealFFT(int i, FloatBuffer inR, FloatBuffer outI, FloatBuffer temp1, FloatBuffer temp2, int l2);

	public String toString(FloatBuffer[] f)
	{
		String s= "";
		for (int i= 0; i < f[0].capacity(); i++)
		{
			for (int m= 0; m < f.length; m++)
			{
				s += BaseMath.toDP(f[m].get(i), 4) + " ";
			}
			s += "\n";
		}
		return s;
	}

	// this doesn't seem to scale things at all (despite docs)
	// so...

	public float v4VecHausdorff(FloatBuffer a, FloatBuffer b, float max)
	{
		return v4VecHausdorff(a, a.remaining(), b, b.remaining(), max);
	}

	public native float v4VecHausdorff(FloatBuffer a, int i, FloatBuffer b, int j, float max);

	// this doesn't seem to scale things at all (despite docs)
	// so...

	native public void v5x5Convolve(Buffer image, int width, int height, FloatBuffer kernel, FloatBuffer output);

	public void vadd(FloatBuffer one, float scalar, FloatBuffer result)
	{
		// coded out underneal
		if (one.capacity()%4!=0)throw new IllegalArgumentException(one.capacity()+"");
		vaddS(one, scalar, result, one.capacity());
	}

	public void vadd(FloatBuffer one, FloatBuffer two, FloatBuffer result)
	{
		vadd(one, two, result, one.capacity());
	}

	native public void vadd(FloatBuffer one, FloatBuffer two, FloatBuffer result, int length);

	native public void vaddS(FloatBuffer one, float scalar, FloatBuffer result, int i);

	// no docs for which way around this is....
	public void vam(FloatBuffer one, FloatBuffer two, FloatBuffer three, FloatBuffer out)
	{
		vam(one, two, three, out, out.capacity());
	}

	native public void vam(FloatBuffer one, FloatBuffer two, FloatBuffer three, FloatBuffer out, int i);

	public void vcascadelowpass(FloatBuffer input, int startAt, float alpha) {
		vcascadelowpass(input, alpha, startAt, input.capacity());
	}

	public void vdistancematrix(FloatBuffer from, FloatBuffer to)
	{
		assert to.capacity() == (from.capacity()/4)*(from.capacity()/4)*4;
		vdistancematrix(from, to, from.capacity());
	}
	native public void vdistancematrix(FloatBuffer fomr, FloatBuffer to, int length);


	public void vdistancematrixToScalar(FloatBuffer from, FloatBuffer to)
	{
		assert to.capacity() == (from.capacity()/4)*(from.capacity()/4);
		vdistancematrixToScalar(from, to, from.capacity());
	}

	native public void vdistancematrixToScalar(FloatBuffer fomr, FloatBuffer to, int length);
	public void vdiv(FloatBuffer one, FloatBuffer two, FloatBuffer result)
	{
		// coded out underneath
		if (one.capacity()%4!=0)throw new IllegalArgumentException(one.capacity()+"");
		if (two.capacity()%4!=0)throw new IllegalArgumentException(two.capacity()+"");
		vdiv(one, two, result, one.capacity());
	}

	// note, long's are not accelerated it appears, this code, despite the name, isn't accelerated.

	native public void vdiv(FloatBuffer one, FloatBuffer two, FloatBuffer result, int length);

	native public void vDivergence(ByteBuffer input, int width, int height, ByteBuffer output);

	// like vrnd0v2 but insteady of outputing r1,r2,r3,r4 .... it does r1,r2,r1,r2,r3,r4,r3,r4 ....

	// two vectors -> one scalar
	public float vdot(FloatBuffer one, FloatBuffer two)
	{
		return vdot(one, two, one.capacity());
	}

	native public float vdot(FloatBuffer one, FloatBuffer two, int i);

	native public void vForwardWaveletTransform(IntBuffer rgb8in, LongBuffer rgb16Out, LongBuffer temp, int width, int height);

	public void vinterleave(FloatBuffer one, FloatBuffer two, FloatBuffer out)
	{
		vinterleave(one, two, out, one.capacity());
	}

	native public float vinterleave(FloatBuffer o, FloatBuffer t, FloatBuffer oo, int i);

	native public void vInverseWaveletTransform(LongBuffer rgb16in, IntBuffer rgb8Out, LongBuffer temp, int width, int height);
	public float vlpf(FloatBuffer outFilter, FloatBuffer inFilter, float filterConstant) {
		assert outFilter.capacity()==inFilter.capacity();
		return vlpf(outFilter, inFilter, filterConstant, outFilter.capacity());
	}
	public float vlpf_sqr(FloatBuffer outFilter, FloatBuffer inFilter, float filterUp, float filterDown) {
		assert outFilter.remaining() == inFilter.remaining();
		return vlpfabs(outFilter, inFilter, filterUp, filterDown, outFilter.capacity());
	}

	public float vlpf_sqr_diff(FloatBuffer outFilter, FloatBuffer inFilterA, FloatBuffer inFilterB, float filterUp, float filterDown) {
		assert outFilter.remaining() == inFilterA.remaining();
		assert inFilterB.remaining() == inFilterA.remaining();

		return vlpfabsdiff(outFilter, inFilterA, inFilterB, filterUp, filterDown, outFilter.capacity());
	}

	/**
	 * temp12= temp2*f+temp1;
	 */
	public void vmadd(FloatBuffer temp2, float f, FloatBuffer temp1, FloatBuffer temp12)
	{
		vmadd(temp2, f, temp1, temp12, temp2.capacity());
	}
	native public int vmadd(FloatBuffer temp2, float f, FloatBuffer temp1, FloatBuffer temp12, int length);

	public int vmin4Distance(float x,float y,float z,float w, FloatBuffer to)
	{
		vmin4Distance_tmp.rewind();
		vmin4Distance_tmp.put(x).put(y).put(z).put(w);
		vmin4Distance_tmp.rewind();
		return vmin4Distance(to, (to.limit()-to.position())/4, vmin4Distance_tmp);
	}
	native public int vmin4Distance(FloatBuffer to, int toLength, FloatBuffer fourVec);

	public void vmul(FloatBuffer one, float scalar, FloatBuffer result)
	{
		vmulS(one, scalar, result, one.capacity());
	}
	public void vmul(FloatBuffer one, FloatBuffer two, FloatBuffer result)
	{
		vmul(one, two, result, one.capacity());
	}

	native public void vmul(FloatBuffer one, FloatBuffer two, FloatBuffer result, int length);
	native public void vmulS(FloatBuffer one, float scalar, FloatBuffer result, int i);
	public void vmulscalaraddvec(FloatBuffer one,float mulOne,FloatBuffer addScaledOne,FloatBuffer result){
		nativeZeQaXpY(one,mulOne,addScaledOne,result,result.capacity());
	}
	/**
		vectorized random number generator

		TODO, bah, needs all kind of long routines, lets just put it native side

	float ran0(long *seed)
	{
		long k;

		k= (*seed)/127773;
		*seed=16807*( (*seed)-k*12773)-2836*k;
		if ((*seed)<0) *seed += 2147483647;
		return (*seed)/2147483647.0;
	}
	*/

	// if seed is null, returns a seed array, use this in subsequent calls, but don't interpret it
	public LongBuffer vran0(LongBuffer seed, FloatBuffer buffer)
	{
		if ((seed!=null) && (seed.capacity() != buffer.capacity()))
		{
			freeAlignedStorage(seed);
			seed= null;
		}
		if (seed == null)
		{
			seed= this.newAlignedStorage(buffer.capacity() * 8).asLongBuffer();
			for (int i= 0; i < seed.capacity(); i++)
			{
				seed.put((long) (Math.random() * Long.MAX_VALUE));
			}
		}
		vrnd0(seed, buffer, buffer.capacity());
		return seed;
	}

	public void vrefreshdistancematrix(FloatBuffer input, FloatBuffer distances, int cursor) {
		vrefreshdistancematrix(input,distances,cursor, input.capacity());
	}

	native public void  vrnd0(LongBuffer seed, FloatBuffer buffer, int length);

	// altivec implementation of the mersenne twister algorithm. 5 times faster than carefully optimized scalar
	// rnd0 it seems.... (and some 60 times faster than calling Math.random() a lot)
	// seed is ignored
	native public void vrnd0v(IntBuffer seed, FloatBuffer output, int length);

	native public void vrnd0v2(Object object, FloatBuffer aux6, int i);
	public void vsub(FloatBuffer one, FloatBuffer two, FloatBuffer result)
	{
		vsub(one, two, result, one.capacity());
	}

	native public void vsub(FloatBuffer one, FloatBuffer two, FloatBuffer result, int length);
	/**
	 * could be faster
	 * @param temp1
	 */
	public void vzero(FloatBuffer temp1)
	{
		vzero(temp1, temp1.capacity());
	}

	native private void vcascadelowpass(FloatBuffer input, float alpha, int startAt, int i);

	native private float vlpf(FloatBuffer outFilter, FloatBuffer inFilter, float filterConstant, int i);

	native private float vlpfabs(FloatBuffer outFilter, FloatBuffer inFilter, float filterUp, float filterDown, int num);

	native private float vlpfabsdiff(FloatBuffer outFilter, FloatBuffer inFilterA, FloatBuffer inFilterB, float filterUp, float filterDown, int num);

	native private void vrefreshdistancematrix(FloatBuffer input, FloatBuffer distances, int cursor, int i) ;

	native void vzero(FloatBuffer t1, int l);

}
