package field.graphics.windowing;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Iterator;

import sunw.io.Serializable;
import field.graphics.core.BasicCamera;
import field.graphics.core.BasicGeometry;
import field.launch.iUpdateable;
import field.math.abstraction.iInplaceProvider;
import field.math.graph.iGraphNode;
import field.math.linalg.AxisAngle;
import field.math.linalg.CoordinateFrame;
import field.math.linalg.Quaternion;
import field.math.linalg.Vector3;
import field.math.linalg.iCoordinateFrame;

/**
 * @author marc <I>Created on Mar 7, 2003</I>
 */
public class CoordinateFrameCamera implements iUpdateable {

	static public class SavedCamera implements Serializable {
		public Vector3 position = new Vector3();
		public Vector3 lookAt = new Vector3();
		public Vector3 up = new Vector3();

		transient iInplaceProvider<iCoordinateFrame.iMutable> positionFrame;
		transient iInplaceProvider<iCoordinateFrame.iMutable> lookAtFrame;
		//static iNamedGroup startSearchFrom;

		public SavedCamera(CoordinateFrameCamera camera) {
			position.setValue(camera.position);
			lookAt.setValue(camera.lookAt);
			up.setValue(camera.up);
			positionFrame = camera.positionFrame;
			lookAtFrame = camera.lookAtFrame;
		}

		public void load(CoordinateFrameCamera camera) {
			camera.position.setValue(position);
			camera.lookAt.setValue(lookAt);
			camera.up.setValue(up);
			camera.positionFrame = positionFrame;
			camera.lookAtFrame = lookAtFrame;
		}

		private void readObject(ObjectInputStream i) throws IOException, ClassNotFoundException {
			i.defaultReadObject();
//			try {
//				if (startSearchFrom == null) {
//					i.readObject();
//					i.readObject();
//				} else {
//					try {
//						positionFrame = (iInplaceProvider<iCoordinateFrame.iMutable>) NamedGroupPathName.fromExternalizedString((String) i.readObject())
//								.find(startSearchFrom, true);
//						lookAtFrame = (iInplaceProvider<iCoordinateFrame.iMutable>) NamedGroupPathName.fromExternalizedString((String) i.readObject())
//								.find(startSearchFrom, true);
//					} catch (Exception e) {
//					}
//				}
//			} catch (Exception e) {
//			}
		}

		private void writeObject(ObjectOutputStream o) throws IOException {
			o.defaultWriteObject();
//			if (startSearchFrom == null) {
//				o.writeObject(null);
//				o.writeObject(null);
//			} else {
//				try {
//
//					if (positionFrame instanceof iNamedObject)
//						o.writeObject(NamedGroupPathName.fromNamedObject((iNamedObject) positionFrame, startSearchFrom).toExternalizedString());
//					if (lookAtFrame instanceof iNamedObject)
//						o.writeObject(NamedGroupPathName.fromNamedObject((iNamedObject) lookAtFrame, startSearchFrom).toExternalizedString());
//				} catch (Exception e) {
//				}
//			}
		}
	}

	public boolean openLoop = true;
	public boolean orbitRige = false;
	public boolean orbitLeft = false;

	public float driftAmount= 1;
	BasicCamera camera;
	Vector3 position = new Vector3();

	Vector3 lookAt = new Vector3();
	Vector3 up = new Vector3();

	Vector3 resetUpTo = new Vector3();
	Vector3 resetLookAtTo = new Vector3();

	Vector3 resetPositionTo = new Vector3();
	iInplaceProvider<iCoordinateFrame.iMutable> positionFrame;
	iInplaceProvider<iCoordinateFrame.iMutable> lookAtFrame;
	float alpha = 0;
	float speed = 1;

	boolean doResync = false;

	boolean cameraChanged = false;

	HashMap savedCameras = new HashMap();

	CoordinateFrame frame = new CoordinateFrame();

	public CoordinateFrameCamera(BasicCamera camera) {
		this.camera = camera;
		camera.getPosition(position);
		camera.getLookAt(lookAt);
		camera.getUp(up);
		resetUpTo.setValue(up);
		resetPositionTo.setValue(position);
		resetLookAtTo.setValue(lookAt);
	}

	public void changeDoSync(boolean too) {
		doResync = too;
	}

	public void changeLookAtCoordinateFrame(iInplaceProvider<iCoordinateFrame.iMutable> to) {
		Vector3 lookAtNow = new Vector3();
		lookInWorld(lookAtNow);
		lookAtFrame = to;
		setLookInWorld(lookAtNow);
	}
	public void changePositionCoordinateFrame(iInplaceProvider<iCoordinateFrame.iMutable> to) {
		Vector3 positionNow = new Vector3();
		positionInWorld(positionNow);
		positionFrame = to;
		setPositionInWorld(positionNow);
	}
	public Iterator deserializeSaved(String string) {
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(string)));
			savedCameras = (HashMap) ois.readObject();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return savedCameras.keySet().iterator();
	}

	public void driftLeft() {
		orbitLeft = true;
		orbitRige = false;
	}

	public void driftRight() {
		orbitLeft = false;
		orbitRige = true;
	}

	public void freeCamera() {
		changeLookAtCoordinateFrame(null);
		changePositionCoordinateFrame(null);
	}

	public SavedCamera getCamera(String k) {
		return (SavedCamera) savedCameras.get(k);
	}

	/**
	 * returns a quaternion corresponding to rotation from arbitrary forward and up vectors
	 *
	 * @param up
	 * @param forward
	 * @return Quaternion
	 */
	public Quaternion getQuat(Vector3 up, Vector3 forward) {
		Quaternion forwardToLook = new Quaternion(forward, lookAt.sub(position));

		Vector3 camup = new Vector3(up);
		forwardToLook.rotateVector(camup);

		Quaternion camUpToUp = new Quaternion(camup, up);

		return forwardToLook.mul(forwardToLook, camUpToUp);
	}
	public iInplaceProvider<iCoordinateFrame.iMutable>[] loadCamera(String called) {
		noOrbit();
		resync();
		SavedCamera camera = (SavedCamera) savedCameras.get(called);
		if (camera != null)
			camera.load(this);
		if (camera == null) {
			return null;
		}
		return new iInplaceProvider[]{camera.lookAtFrame, camera.positionFrame};
	}
	public iInplaceProvider<iCoordinateFrame.iMutable>[] loadCameraNoFrameChange(String called) {
		noOrbit();
		resync();
		SavedCamera camera = (SavedCamera) savedCameras.get(called);
		if (camera != null) {
			iInplaceProvider<iCoordinateFrame.iMutable> oldp = this.positionFrame;
			iInplaceProvider<iCoordinateFrame.iMutable> oldl = this.lookAtFrame;
			camera.load(this);
			this.positionFrame = oldp;
			this.lookAtFrame = oldl;
		}
		if (camera == null) {
			return null;
		}
		return new iInplaceProvider[]{camera.lookAtFrame, camera.positionFrame};
	}
	public void moveAndRotate(double Langle, double Uangle, float Funits, float Lunits, float Uunits, boolean worldUp, Vector3 trans, double maxX,
			double maxY, double maxZ, double minDist) {
		resync();
		// move position towards lookAt <b>in world coordinates</b>
		Vector3 look = new Vector3();
		lookInWorld(look);
		Vector3 at = new Vector3();
		positionInWorld(at);

		Vector3 ray = new Vector3();
		ray.sub(look, at, ray);
		if (ray.mag() == 0)
			return;
		ray.normalize();

		Vector3 realUp = new Vector3(0, 0, 1);

		if (worldUp == false) {
			Vector3 tmp = new Vector3();
			tmp.cross(ray, this.up);
			realUp.cross(tmp, ray);
		}

		//rotateLeft
		Quaternion q = new Quaternion().set(new AxisAngle(realUp, (float) Langle));
		q.rotateVector(ray);
		look.blendRepresentation_add(ray, at, look);

		Vector3 right = new Vector3();
		right.cross(ray, realUp);
		right.normalize();

		//rotateUp
		Quaternion q2 = new Quaternion().set(new AxisAngle(right, (float) Uangle));
		q2.rotateVector(ray);
		q2.rotateVector(realUp);

		if ((ray.z < 0.99 && ray.z > -0.99) || !worldUp)
			look.blendRepresentation_add(ray, at, look);

		Vector3 temp = new Vector3();

		//moveForward
		Vector3.add(ray, Funits, at, temp);
		if (Math.abs(temp.x) <= maxX && Math.abs(temp.y) <= maxY && Math.abs(temp.z) <= maxZ
				&& (trans == null || (trans != null && Math.abs(temp.distanceFrom(trans)) > minDist))) {
			Vector3.add(ray, Funits, at, at);
			Vector3.add(ray, Funits, look, look);

		}

		//moveRight
		Vector3.add(right, Lunits, at, temp);

		if (Math.abs(temp.x) <= maxX && Math.abs(temp.y) <= maxY && Math.abs(temp.z) <= maxZ
				&& (trans == null || (trans != null && Math.abs(temp.distanceFrom(trans)) > minDist))) {

			Vector3.add(right, Lunits, at, at);
			Vector3.add(right, Lunits, look, look);

		}

		//moveUp
		Vector3.add(realUp, Uunits, at, temp);

		if (Math.abs(temp.x) <= maxX && Math.abs(temp.y) <= maxY && Math.abs(temp.z) <= maxZ
				&& (trans == null || (trans != null && Math.abs(temp.distanceFrom(trans)) > minDist))) {

			Vector3.add(realUp, Uunits, at, at);
			Vector3.add(realUp, Uunits, look, look);

		}

		if (trans != null) {
			look = trans;
		}


		setPositionInWorld(at);
		setLookInWorld(look);
		setUpInWorld(realUp);
	}

	public void moveAndRotateAround(double leftAngle, float upUnits, float forwardUnits, Vector3 origin) {

		resync();

		leftAngle *= speed;
		//		work out what the real up vector is by lookAtVec x (lookAtVec x suposed up)
		// in world coordinates;

		Vector3 realUp = new Vector3();
		Vector3 ray = new Vector3();
		Vector3 tmp = new Vector3();
		Vector3 up = new Vector3();

		upInWorld(up);
		Vector3 look = new Vector3();
		lookInWorld(look);
		Vector3 at = new Vector3();
		positionInWorld(at);
		ray.sub(at, look, ray);

		tmp.cross(ray, up);
		realUp.cross(tmp, ray);

		Quaternion q = new Quaternion().set(new AxisAngle(realUp, (float) leftAngle));
		q.rotateVector(ray);

		at.blendRepresentation_add(ray, look, at);
		setPositionInWorld(at);
		setUpInWorld(up);

	}

	public void moveForward(float units) {
		resync();
		units *= speed;
		// move position towards lookAt <b>in world coordinates</b>
		Vector3 look = new Vector3();
		lookInWorld(look);
		Vector3 at = new Vector3();
		positionInWorld(at);
		Vector3 ray = new Vector3();
		ray.sub(look, at);
		if (ray.mag() == 0)
			return;
		ray.normalize();

		
		Vector3.add(ray, units, at, at);
		Vector3.add(ray, units, look, look);

		setPositionInWorld(at);
		setLookInWorld(look);
	}

	public void moveForwardAlong(float units) {
		resync();
		units *= speed;
		// move position towards lookAt <b>in world coordinates</b>
		Vector3 look = new Vector3();
		lookInWorld(look);
		Vector3 at = new Vector3();
		positionInWorld(at);
		Vector3 ray = new Vector3();
		ray.sub(look, at);
		if (ray.mag() == 0)
			return;
		units*=ray.mag()/10;
		ray.normalize();

		Vector3.add(ray, units, at, at);
		Vector3.add(ray, units, look, look);

		// protect against singularity
		Vector3 currentLookAt = new Vector3();
		lookInWorld(currentLookAt);
		if (at.distanceFrom(currentLookAt) > 1e-3)
			setPositionInWorld(at);

	}

	public void noOrbit() {
		orbitLeft = false;
		orbitRige = false;
	}

	public void panDown(double amount)
	{
		panUp(-amount);
	}

	public void panLeft(double angle)
	{
		resync();
		angle *= speed;

		Vector3 t1 = new Vector3();
		Vector3 up= new Vector3();
		lookInWorld(t1);
		upInWorld(up);

		Vector3 ray = new Vector3();
		Vector3 look = new Vector3();
		lookInWorld(look);
		Vector3 at = new Vector3();
		positionInWorld(at);
		ray.sub(look, at);

		Vector3 right = new Vector3();
		right.cross(ray, up);
		right.normalize();

		right.scale((float) angle);
		t1.add(right);
		setLookInWorld(t1);

		positionInWorld(t1);
		t1.add(right);
		setPositionInWorld(t1);
	}

	public void panRight(double amount)
	{
		panLeft(-amount);

	}

	public void panUp(double angle)
	{
		resync();
		angle *= speed;

		Vector3 t1 = new Vector3();
		Vector3 up= new Vector3();
		lookInWorld(t1);
		upInWorld(up);

		up.scale((float) angle);
		t1.add(up);
		setLookInWorld(t1);

		positionInWorld(t1);
		t1.add(up);
		setPositionInWorld(t1);

	}
	public void positionInWorld(Vector3 into) {
		if (positionFrame != null) {
			positionFrame.get(frame);
			frame.transformPosition(into.set(position));
		} else
			into.setValue(position);
	}

	public void reset() {
		this.changeLookAtCoordinateFrame(null);
		this.changePositionCoordinateFrame(null);
		this.position.setValue(new Vector3(0, -10, 0));
		this.lookAt.setValue(new Vector3(0, 0, 0));
		this.up.setValue(new Vector3(0, 0, 1));
		this.alpha = 0;
		this.speed = 1;
	}

	public void rotateLeft(double angle) {
		resync();
		angle *= speed;
		// work out what the real up vector is by lookAtVec x (lookAtVec x suposed up)
		// in world coordinates;

		Vector3 startUp = new Vector3();

		upInWorld(startUp);

		Vector3 realUp = new Vector3();
		Vector3 ray = new Vector3();
		Vector3 tmp = new Vector3();

		Vector3 look = new Vector3();
		lookInWorld(look);
		Vector3 at = new Vector3();
		positionInWorld(at);
		ray.sub(look, at);

		tmp.cross(ray, up);
		realUp.cross(tmp, ray);

		Quaternion q = new Quaternion().set(new AxisAngle(realUp,(float) angle));
		q.rotateVector(ray);

		ray.add(ray, at);
		setLookInWorld(ray);

		setUpInWorld(startUp);
	}

	public void rotateLeftAround(double angle) {
		resync();

		//	System.err.println(" rotateLeftAround <"+angle+"> <"+speed+">");

		angle *= speed;
		//		work out what the real up vector is by lookAtVec x (lookAtVec x suposed up)
		// in world coordinates;

		Vector3 realUp = new Vector3();
		Vector3 ray = new Vector3();
		Vector3 tmp = new Vector3();
		Vector3 up = new Vector3();

		upInWorld(up);
		Vector3 look = new Vector3();
		lookInWorld(look);
		Vector3 at = new Vector3();
		positionInWorld(at);
		ray.sub(at, look, ray);

		tmp.cross(ray, up);
		realUp.cross(tmp, ray);

		Quaternion q = new Quaternion().set(new AxisAngle(realUp, (float) angle));
		q.rotateVector(ray);

		ray.blendRepresentation_add(ray, look, at);
		setPositionInWorld(at);
		setUpInWorld(up);
	}

	public void rotateUp(double angle) {
		resync();
		angle *= speed;
		// make left vector
		Vector3 ray = new Vector3();
		Vector3 look = new Vector3();
		lookInWorld(look);
		Vector3 at = new Vector3();
		positionInWorld(at);
		ray.sub(look, at);

		Vector3 right = new Vector3();
		Vector3 up = new Vector3();
		upInWorld(up);
		right.cross(ray, up);

		Quaternion q = new Quaternion().set(new AxisAngle(right, (float) angle));
		q.rotateVector(ray);

		q.rotateVector(up);

		ray.add(ray, at);
		setLookInWorld(ray);
		setUpInWorld(up);
	}

	public void rollLeft(double angle) {
		resync();
		angle *= speed;
		// make left vector
		Vector3 ray = new Vector3();
		Vector3 look = new Vector3();
		lookInWorld(look);
		Vector3 at = new Vector3();
		positionInWorld(at);
		ray.sub(look, at);

		Vector3 up = new Vector3();
		upInWorld(up);

		Quaternion q = new Quaternion().set(new AxisAngle(ray, (float) angle));
		q.rotateVector(up);

		setUpInWorld(up);
	}

	public void rotateUpAround(double angle) {
		resync();
		angle *= speed;
		//		work out what the real up vector is by lookAtVec x (lookAtVec x suposed up)
		// in world coordinates;

		// make left vector
		Vector3 ray = new Vector3();
		Vector3 look = new Vector3();
		lookInWorld(look);
		Vector3 at = new Vector3();
		positionInWorld(at);
		ray.sub(at, look, ray);

		Vector3 right = new Vector3();
		Vector3 up = new Vector3();
		upInWorld(up);
		right.cross(ray, up);

		Quaternion q = new Quaternion().set(new AxisAngle(right, (float) angle));
		q.rotateVector(ray);
		q.rotateVector(up);

		ray.blendRepresentation_add(ray, look, ray);
		setPositionInWorld(ray);
		setUpInWorld(up);
	}

	public void saveCamera(String called) {
		savedCameras.put(called, new SavedCamera(this));
	}

	/**
	 * @param string
	 */
	public void serializeSaved(String string) {
		try {
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File(string)));
			oos.writeObject(savedCameras);
			oos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void setAlpha(float d) {
		this.alpha = d;
	}
	public void setLookInWorld(Vector3 to) {
		if (lookAtFrame != null) {
			lookAtFrame.get(frame);
			frame.invert();
			frame.transformPosition(lookAt.set(to));
		} else
			lookAt.setValue(to);
	}

	public void setPositionInWorld(Vector3 to) {
		if (positionFrame != null) {
			positionFrame.get(frame);
			frame.invert();
			frame.transformPosition(position.set(to));
		} else
			position.setValue(to);
	}
	public void setSpeed(float f) {
		this.speed = f;
	}
	public void setUpInWorld(Vector3 to) {
		if (positionFrame != null) {
			positionFrame.get(frame);
			frame.invert();
			frame.transformDirection(up.set(to));
		} else
			up.setValue(to);
	}

	public boolean tryChangeLookAtCoordinateFrame(Object o) {
		resync();
		try {
			if (o instanceof iInplaceProvider) {
				changeLookAtCoordinateFrame((iInplaceProvider<iCoordinateFrame.iMutable>) o);
			} else if (o instanceof BasicGeometry.BasicMesh) {
				changeLookAtCoordinateFrame(((BasicGeometry.BasicMesh) o).getCoordindateFrameProvider());
			} else if (o == null)
				changeLookAtCoordinateFrame(null);
			else if (o instanceof iGraphNode<?>)
				tryChangeLookAtCoordinateFrame(((iGraphNode<?>) o).getParents().get(0));
			else
				return false;
		} catch (ClassCastException ex) {
			return false;
		}
		return true;
	}

	public boolean tryChangePositionCoordinateFrame(Object o) {
		resync();
		try {
			if (o instanceof iInplaceProvider)
				changePositionCoordinateFrame((iInplaceProvider<iCoordinateFrame.iMutable>) o);
			else if (o instanceof BasicGeometry.BasicMesh) {

				changePositionCoordinateFrame(((BasicGeometry.BasicMesh) o).getCoordindateFrameProvider());
			} else if (o == null)
				changePositionCoordinateFrame(null);
			else if (o instanceof iGraphNode<?>)
				tryChangeLookAtCoordinateFrame(((iGraphNode<?>) o).getParents().get(0));
			else
				return false;
		} catch (ClassCastException ex) {
			return false;
		}
		return true;
	}

	public void update() {

		//new Exception().printStackTrace();

		if (orbitRige) {
			rotateLeftAround(0.03 * 0.56*driftAmount);
			orbitRige = false;
			update();
			orbitRige = true;
		}
		if (orbitLeft) {
			rotateLeftAround(-0.03 * 0.56*driftAmount);
			orbitLeft = false;
			update();
			orbitLeft = true;
		}
		if ((alpha == 0) && (cameraChanged == false) && (openLoop))
		{
			
			return;
		}

		cameraChanged = false;

		Vector3 newPosition = new Vector3();
		Vector3 oldPosition = new Vector3();
		Vector3 newUp = new Vector3();
		Vector3 oldUp = new Vector3();
		Vector3 newLook = new Vector3();
		Vector3 oldLook = new Vector3();

		positionInWorld(newPosition);
		upInWorld(newUp);
		lookInWorld(newLook);


		// filter here
		camera.getPosition(oldPosition);
		// filter here
		newPosition.lerp(oldPosition, newPosition, 1 - alpha);
		camera.setPosition(newPosition);

		camera.getUp(oldUp);

		newUp.lerp(oldUp, newUp, 1 - alpha);
		camera.setUp(newUp);

		// filter here
		camera.getLookAt(oldLook);
		// filter here
		oldLook.lerp(oldLook, newLook, 1 - alpha);
		camera.setLookAt(oldLook);


	}

	protected void lookInWorld(Vector3 into) {
		if (lookAtFrame != null) {
			lookAtFrame.get(frame);
			frame.transformPosition(into.set(lookAt));
		} else
			into.setValue(lookAt);
	}

	protected void resync() {
		cameraChanged = true;
		if (doResync) {
			Vector3 oldPosition = new Vector3();
			Vector3 oldUp = new Vector3();
			Vector3 oldLook = new Vector3();

			camera.getPosition(oldPosition);
			camera.getUp(oldUp);
			camera.getLookAt(oldLook);

			
			Vector3 left = new Vector3().cross(camera.getViewRay(null), up);
			Vector3 nup= new Vector3().cross(left, camera.getViewRay(null));
			nup.normalize();
			
			setPositionInWorld(oldPosition);
			setLookInWorld(oldLook);
			//
			setUpInWorld(oldUp);

		}
	}

	protected void upInWorld(Vector3 into) {
		if (positionFrame != null) {
			positionFrame.get(frame);
			frame.transformDirection(into.set(up));
		} else
			into.setValue(up);
	}

}