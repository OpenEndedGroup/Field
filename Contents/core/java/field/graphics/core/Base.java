package field.graphics.core;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.List;
import java.util.Map;

import field.math.abstraction.iInplaceProvider;
import field.math.graph.iMutable;
import field.math.linalg.CoordinateFrame;
import field.math.linalg.Quaternion;
import field.math.linalg.Vector3;
import field.math.linalg.iCoordinateFrame;
import field.namespace.change.iChangable;
import field.namespace.context.SimpleContextTopology;

/**
 * base interfaces for new graphics system, nothing too specific here, just the interfaces and new principles that do not exist somewhere in the older graphics system for implementations see subpackages
 * 
 * work in progress need phantomqueue for natively allocated buffers - otherwise we have a leak. nnn
 * 
 */
public class Base {

	//static public final iContextTree context = new LocalContextTree();
	static public final SimpleContextTopology context = new SimpleContextTopology();
	
	// this is the id of vertex_data, taken from the spec (ARB_vertex_program)
	static final public int vertex_id = 0;

	static final public int normal_id = 2;

	static final public int color0_id = 3;

	static final public int texture0_id = 8;
	static final public int noise = 11;

	static final public int max_id = 16;

	static final public int min_id = 1;

	static public final boolean trace = false;

	
	/**
	 * these are the standard passes inside the renderer: do stuff that can be done before transform transform stuff into world space. do stuff that depends on world space (e.g. skinning) put it on the screen
	 */
	
	static public class LocalPass implements iPass
	{
		protected  float value;

		public LocalPass(float value) {
			this.value = value;
		}
		
		public float getValue()
		{
			return value;
		}
		
		public boolean isLaterThan(iPass p)
		{
			return p.getValue() < value;
		}

		public boolean isEarlierThan(iPass p)
		{
			return p.getValue() > value;			
		}
	}

	public enum StandardPass implements iPass {
		preTransform(-1), transform(0), postTransform(1), preRender(2), render(3), postRender(4), preDisplay(5);

		private int value;

		StandardPass(int value) {
			this.value = value;
		}
		
		public float getValue()
		{
			return value;
		}
		
		public boolean isLaterThan(iPass p)
		{
			return p.getValue() < value;
		}

		public boolean isEarlierThan(iPass p)
		{
			return p.getValue() > value;			
		}
	};

	/**
	 * represents a rendering pass, Passes are interned by the iSceneList, so you can always use == to check to see what you should do
	 */
	public interface iPass {
		public boolean isLaterThan(iPass p);

		public boolean isEarlierThan(iPass p);
		
		public float getValue();
	}

	public interface iAcceptsSceneListElement
	{
		public void addChild(iSceneListElement e);
		public void removeChild(iSceneListElement e);
		public boolean isChild(iSceneListElement e);
		
	}

	public interface iProvidesSceneListElement
	{
		
		public List<iSceneListElement> elements();
	}

	/**
	 * this is an interface for something that can go into a multipass scene list
	 */
	public interface iSceneListElement extends iMutable<iSceneListElement> {
		public iPass requestPass(iPass pass);
		
		public iPass requestPassAfter(iPass pass);

		public iPass requestPassBefore(iPass pass);

		public iPass requestPassAfterAndBefore(iPass after, iPass before);


		/**
		 * main entry point, do your work for iPass 'p' here
		 */
		public void performPass(iPass p);
	}

	/**
	 * A iTransform computes and maintains a current LocalToWorld transform. This way we can do all kinds of things (like skinning, proceedural lines, collisions) that repeatedly need local to world information. It's not like this information wasn't being computed before. Its just that it was buried in the native side of the graphics system
	 * 
	 * this is an interface, because its important that these things are interfaces, _and_ its unclear right now whether we want to lock into an implementation - (for example, sometimes we might want scales to propogate through, othertimes we'd just as soon save the cycles and use quat-vec)
	 * 
	 * we also use modification counters so that people can cache things about this object inteligently
	 * 
	 * @see innards.graphics.basic.BasicFrameTransform
	 */

	public interface iTransform extends iMutable<iTransform>, iInplaceProvider<iCoordinateFrame.iMutable>, iChangable {
		/**
		 * this will not reflect non-committed changes to the transform controllers, or any other transform controller further up the chain. hence the name, 'committed' it may, however, change during the rendering pass process (this is from iCoordinateFrameProvider
		 */
		public CoordinateFrame getLocal(CoordinateFrame out);

		/**
		 * get should always return localToWorld
		 */
		public CoordinateFrame get(CoordinateFrame out);

		/**
		 * This method sets the current value of the rotation buffer to r with no blend
		 */
		public void setRotation(Quaternion r);

		public void setTranslation(Vector3 v);

		/**
		 * This method blends r using a weight of w with other rotations in the buffer
		 */
		public void blendRotation(Quaternion r, float w);

		public void blendTranslation(Vector3 v, float w);

		/**
		 * Returns the current contents of the rotation buffer after any blends.
		 */

		public Quaternion getCurrentRotation(Quaternion r);

		/**
		 * Returns the current contents of the translation buffer.
		 */
		public Vector3 getCurrentTranslation(Vector3 t);

		/**
		 * returns an object that you can ask about if this thing has changed or not
		 */
		public iModCount getModCount(Object withRespectTo);

	}

	/**
	 * an interface for geometry
	 */
	public interface iGeometry extends iSceneListElement {
		/** for manipulating geometry, here's the rule. don't hang onto these buffers, we'll update modification count so we know to resend these buffers */
		public FloatBuffer vertex();

		public ShortBuffer triangle();

		/**
		 * this will lazily create an aux buffer with id 'auxID', this will also include normal and texture coordinate info , refer to the standard shader library for things like normals and texture coordinates
		 * 
		 * if you pass in 0 you will still get the FloatBuffer if it currently exists, otherwise you will get null. i.e. no aux buffer will be created for you
		 */
		public FloatBuffer aux(int auxId, int elementSize);

		public boolean hasAux(int auxId);

		/** for initializing, and reinitializing geometry, typicaly, these just return 'this' */
		public iGeometry rebuildTriangle(int numTriangles);

		/** this call will typically throw out all the normal information and aux information */
		public iGeometry rebuildVertex(int numVertex);

		public iGeometry setVertexLimit(int numVertex);

		public iGeometry setTriangleLimit(int numVertex);

		public int numVertex();

		public int numTriangle();

		/** map of Integer (id) vs buffer */
		public Map auxBuffers();

		public iInplaceProvider<field.math.linalg.iCoordinateFrame.iMutable> getCoordinateProvider();
	}
	
	public interface iLongGeometry extends iGeometry
	{
		public IntBuffer longTriangle();
	}
	

	/**
	 * an advanced piece of geometry, that is the results of processing one mesh into another mesh
	 * 
	 * calls like myInputOutputGeometry.aux(3,3) and .vertex().put(...) get piped through to input. skinning is one example of a transformative geometry, proceedural splines will be another.
	 * 
	 */
	public interface iInputOuputGeometry extends iGeometry {
		public iGeometry getInput();

		public iGeometry getOutput();

		// just copies an aux channel (if it exists in the inpu);
		public void copyAux(int auxId);
	}

	public interface iSkin extends iSceneListElement {
		// this vertex, has this weight on this bone
		public void addBoneWeightInfo(int vertexIndex, float weight, int boneIndex);

		// this bone is this transform
		public void setBone(int boneIndex, iInplaceProvider<iCoordinateFrame.iMutable> frame);

		// we've finished setting the weights for this vertex
		public void normalizeBoneWeights(int vertexIndex, float scaleFactor);
	}

	public interface iBuildable
	{
		public boolean attach(Object b);
	}
	
}
