package field.graphics.jfbxlib;

import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

import field.math.graph.GraphNodeSearching;
import field.math.graph.GraphNodeSearching.GraphNodeVisitor_depthFirst;
import field.math.graph.GraphNodeSearching.VisitCode;
import field.math.graph.SimpleNode;
import field.math.linalg.CoordinateFrame;
import field.math.linalg.Matrix3;
import field.math.linalg.Matrix4;
import field.math.linalg.Quaternion;
import field.math.linalg.Vector3;

public class BuildTransformTreeVisitor extends AbstractVisitor {

	static public class Transform implements Serializable {

		public long uid;

		public String name;

		public Quaternion worldRotation;

		public Vector3 worldTranslation;

		public Vector3 worldScale;

		public Quaternion localRotation;

		public Vector3 localTranslation;

		public Vector3 localScale;

		// public HashMap<String, Object> properties = new
		// HashMap<String, Object>();

		public Transform(String name, Quaternion rotation, Vector3 translation, Vector3 scale) {
			this.name = name;
			this.worldRotation = rotation;
			this.worldTranslation = translation;
			this.worldScale = scale;
		}

		public CoordinateFrame createLocalCoordinateFrame() {
			return new CoordinateFrame(localRotation, localTranslation, localScale);
		}

		@Override
		public String toString() {
			if (localRotation == null)
				return "r:" + worldRotation + " t:" + worldTranslation + " s:" + worldScale;
			return "\u2014\u2014 local \u2014\u2014 r:" + localRotation + " t:" + localTranslation + " s:" + localScale;
		}

	}

	Map<Long, SimpleNode<Transform>> tree = new HashMap<Long, SimpleNode<Transform>>();

	Stack<SimpleNode<Transform>> currentTransform = new Stack<SimpleNode<Transform>>();

	List<SimpleNode<Transform>> roots = new ArrayList<SimpleNode<Transform>>();

	public BuildTransformTreeVisitor() {
	}

	public BuildTransformTreeVisitor(JFBXVisitor delegate) {
		super.delegate = delegate;
	}

	public void computeLocals() {
		GraphNodeVisitor_depthFirst<SimpleNode<Transform>> print = new GraphNodeSearching.GraphNodeVisitor_depthFirst<SimpleNode<Transform>>(false) {
			@Override
			protected VisitCode visit(SimpleNode<Transform> n) {

				//if (true) return VisitCode.cont;
				
				if (stack.size() == 1) {
					
					
				//	System.out.println("stack1 :"+n.payload().name+" "+n.payload().localRotation+" "+n.payload().localTranslation+" "+n.payload().localScale);
					
					n.payload().localRotation = new Quaternion(n.payload().worldRotation);
					n.payload().localTranslation = new Vector3(n.payload().worldTranslation);
					n.payload().localScale = new Vector3(n.payload().worldScale);

				//	System.out.println("stack1 out:"+n.payload().name+" "+n.payload().localRotation+" "+n.payload().localTranslation+" "+n.payload().localScale);

					//System.out.println(" stack size is 1, just copying <"+n.payload()+">");

				} else {
					Transform above = stack.get(stack.size() - 2).payload();
					Transform here = n.payload();

					//if (n.payload().name.equals("RightHip"))
					//System.out.println("stack2 :"+n.payload().name+" "+n.payload().localRotation+" "+n.payload().localTranslation+" "+n.payload().localScale);

					
					Matrix4 above_m = new Matrix4(above.worldRotation, above.worldTranslation, above.worldScale.x);
					Matrix4 here_m = new Matrix4(here.worldRotation, here.worldTranslation, here.worldScale.x);

					above_m.invert();
					Matrix4 left = new Matrix4().mul(above_m, here_m);

					here.localRotation = new Quaternion();
					here.localTranslation = new Vector3();
					here.localScale = new Vector3();

					left.get(here.localRotation);
					if (Float.isNaN(here.localRotation.x)) {
						System.out.println(" -- nan while computing transform for <" + here.name + ">");
						above_m.invert();
						System.out.println("above \n" + above_m);
						System.out.println("here \n" + here_m);
						System.out.println("left \n" + left);
						here.localRotation.set(0, 0, 0, 1);
					}

//					if (Math.abs(here.localRotation.mag() - 1.0) > 1e-2) {
//						System.out.println(" warning: non unit rotation ? <" + here.localRotation + "> at <" + n.payload().name + ">");
//						System.out.println(" n.payload()" + " " + n.payload());
//						System.out.println("     above: "+above_m);
//						System.out.println("     here: "+here_m);
//						System.out.println("     left: "+left);
//						left.get(here.localRotation);
//					}

					left.get(here.localTranslation);
					Matrix3 m3 = new Matrix3();
					left.getRotationScale(m3);
					m3.getScale(here.localScale);

					if (n.payload().name.equals("Hips"))
					{
						System.out.println("stack2 out:"+n.payload().name+" "+n.payload().localRotation+" "+n.payload().localTranslation+" "+n.payload().localScale);
						System.out.println("stack2 out:"+n.payload().name+" "+n.payload().worldRotation);
						
					}

				}
				return VisitCode.cont;
			}
		};
		for (SimpleNode<Transform> t : roots)
			print.apply(t);
	}

	public List<SimpleNode<Transform>> getRoots() {
		return roots;
	}

	public SimpleNode<Transform> getTransformForUID(int uid) {
		return tree.get(uid);
	}

	public Map<Long, String> getTransformMap() {
		HashMap<Long, String> ret = new HashMap<Long, String>();
		Iterator<Entry<Long, SimpleNode<Transform>>> i = tree.entrySet().iterator();
		while (i.hasNext()) {
			Entry<Long, SimpleNode<Transform>> e = i.next();
			ret.put(e.getKey(), e.getValue().payload().name);
		}
		return ret;
	}

	public Map<Long, SimpleNode<Transform>> getTransforms() {
		return tree;
	}

	public void printAllNodes(final PrintStream output) {
		GraphNodeVisitor_depthFirst<SimpleNode<Transform>> print = new GraphNodeSearching.GraphNodeVisitor_depthFirst<SimpleNode<Transform>>(false) {
			@Override
			protected VisitCode visit(SimpleNode<Transform> n) {
				output.println(spaces(this.stack.size()) + n);
				return VisitCode.cont;
			}
		};
		for (SimpleNode<Transform> t : roots)
			print.apply(t);
	}

	// @Override
	// public boolean visitTransformUserProperty(String name, int type,
	// float min, float max, boolean animateable, double value) {
	// super.visitTransformUserProperty(name, type, min, max, animateable,
	// value);
	// currentTransform.peek().payload().properties.put(name, type==4 ?
	// Double.doubleToLongBits(value) : value);
	// return true;
	// }

	@Override
	public void visitTransformBegin(String name, int type, long uid) {
		super.visitTransformBegin(name, type, uid);

		// .out.println("!! " + name);

		Transform newTransform = new Transform(name, new Quaternion(), new Vector3(), new Vector3(1, 1, 1));
		newTransform.uid = uid;
		SimpleNode<Transform> sn = new SimpleNode<Transform>().setPayload(newTransform);
		if (currentTransform.size() == 0)
			roots.add(sn);
		else
			currentTransform.peek().addChild(sn);

		currentTransform.push(sn);

		tree.put(uid, sn);
	}

	@Override
	public void visitTransformEnd() {
		super.visitTransformEnd();

		currentTransform.pop();
	}

	@Override
	public void visitTransformInfo(float oq0, float oq1, float oq2, float oq3, float ot0, float ot1, float ot2, float os0, float os1, float os2, float q0, float q1, float q2, float q3, float t0, float t1, float t2, float s0, float s1, float s2) {

		super.visitTransformInfo(oq0, oq1, oq2, oq3, ot0, ot1, ot2, os0, os1, os2, q0, q1, q2, q3, t0, t1, t2, s0, s1, s2);
		if (currentTransform.peek().payload().name.endsWith("Hips")) {
			System.out.println(" straight up from fbx");
			System.out.println(currentTransform.peek().payload().name);
			System.out.printf("world%f %f %f %f    %f %f %f   %f %f %f\nlocal%f %f %f %f    %f %f %f   %f %f %f\n", oq0, oq1, oq2, oq3, ot0, ot1, ot2, os0, os1, os2, q0, q1, q2, q3, t0, t1, t2, s0, s1, s2);
		}
		Quaternion q = new Quaternion(oq0, oq1, oq2, oq3);
		Vector3 t = new Vector3(ot0, ot1, ot2);
		Vector3 s = new Vector3(os0, os1, os2);

		currentTransform.peek().payload().worldRotation.set(q);
		currentTransform.peek().payload().worldTranslation.set(t);
		currentTransform.peek().payload().worldScale.set(s);
		

		Quaternion lq = new Quaternion(q0, q1, q2, q3);
		Vector3 lt = new Vector3(t0, t1, t2);
		Vector3 ls = new Vector3(s0, s1, s2);

		currentTransform.peek().payload().localRotation = new Quaternion();
		currentTransform.peek().payload().localTranslation= new Vector3();
		currentTransform.peek().payload().localScale = new Vector3();
		currentTransform.peek().payload().localRotation.set(lq);
		currentTransform.peek().payload().localTranslation.set(lt);
		currentTransform.peek().payload().localScale.set(ls);
		
		
		if (currentTransform.peek().payload().name.endsWith("Hips")) {
			System.out.println(" straight up from fbx at end");
			System.out.println(currentTransform.peek().payload());
		}
	}

}
