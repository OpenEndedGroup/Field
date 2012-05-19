//
//  BasicContextIDManager.java
//  Created by bruce blumberg on Mon Jan 20 2003.
//  Copyright (c) 2003 __MyCompanyName__. All rights reserved.
//
package field.graphics.core;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;


import field.core.Platform;
import field.core.Platform.OS;
import field.graphics.ci.CoreImage;

/**
 * This class is used internally to handle OpenGL context dependent ids such as
 * DisplayList ids, texture ids, vertex object ids, etc. Since we want objects
 * to be able to be used in multiple contexts (and ids can't be shared across
 * contexts) we need a level of indirection. This is provided by this class.
 * Each context has its own HashMap that takes an object and returns the
 * resource id associated with it. Prior to setup(), any CocoPuffController, or
 * its subclass needs to do a setCurrentContextFor() which will find the HashMap
 * for that instance and use that as the current map for context specific
 * resources. Similarly, prior to rendering, CocoPuffController, or its subclass
 * needs to call setCurrentContextFor() to make its map be the current map.
 * Classes which use OpenGL ids should use this class to store and get the ids.
 * To store an id, an instance should use putId(instance, id). Subsequently, the
 * id can be retrieved via: getId(instance). getId() will return
 * BasicContextIDManager.ID_NOT_FOUND if it doesn't have an id for that
 * instance. If this occurs, the instance will need to generate one, typically
 * via its setup() method and do a putId() when complete.
 */
public class BasicContextManager {
	private static HashMap<Object, Integer> currentContextIdMap = new HashMap<Object, Integer>();

	private static HashMap<Object, HashMap<Object, Integer>> contextOwnerMap = new HashMap<Object, HashMap<Object, Integer>>();

	private static HashMap<Object, Long> coreImageContextMap = new HashMap<Object, Long>();

	private static HashMap<Object, HashMap<Object, Boolean>> contextValidMap = new HashMap<Object, HashMap<Object, Boolean>>();

	private static HashMap<Object, Boolean> currentValidMap = new HashMap<Object, Boolean>();

	public static int ID_NOT_FOUND = -1;

	private static Object currentContext;

	private static ThreadLocal<Object> gl = new ThreadLocal<Object>();
	private static ThreadLocal<Object> glu = new ThreadLocal<Object>();

	public static long coreImageContext;

	public static Object gluLock = new Object();

	/**
	 * Call this to set the currentContextIdMap to be the one associated
	 * with x. Note, the maps are stored internally to
	 * BasicContextIDManager, so a client will never deal with the maps
	 * directly. I f one does not exist already, it is created and installed
	 * as the currentContextIdMap.
	 */
	public static void setCurrentContextFor(Object x, Object gl, Object glu) {
		HashMap<Object, Integer> idMap = contextOwnerMap.get(x);
		if (idMap == null) {
			idMap = new HashMap<Object, Integer>();
			contextOwnerMap.put(x, idMap);
		}
		currentContextIdMap = idMap;

		HashMap<Object, Boolean> validMap = contextValidMap.get(x);
		if (validMap == null) {
			validMap = new HashMap<Object, Boolean>();
			contextValidMap.put(x, validMap);
		}
		currentValidMap = validMap;

		currentContext = x;
		BasicContextManager.setGl(gl);
		BasicContextManager.setGlu(glu);

		Long ll = coreImageContextMap.get(x);
		if (ll == null && Platform.getOS()==OS.mac) {
//			coreImageContextMap.put(x, ll = new CoreImage().context_createOpenGLCIContextNow());
		}
		
		if (Platform.getOS()==OS.mac)
			coreImageContext = ll == null ? 0 : ll;
	}

	/**
	 * Use this to store an id into the currentContextIdMap.
	 */
	public static void putId(Object owner, int id) {
		currentContextIdMap.put(owner, new Integer(id));
	}

	/**
	 * Use this to retrieve an id from the currentContextIdMap. Note, be
	 * sure to check for ID_NOT_FOUND as a return. If this occurs, you will
	 * need to allocate one (typically done in setup() ), and then call
	 * putId() when you are done.
	 */
	public static int getId(Object owner) {
		Integer id = currentContextIdMap.get(owner);
		if (id == null)
			return ID_NOT_FOUND;
		else
			return id.intValue();
	}

	public static void markAsNoIDInAllContexts(Object owner) {
		Iterator<?> it = contextOwnerMap.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<?, ?> e = (Entry<?, ?>) it.next();
			Map<?, ?> m = (Map<?, ?>) e.getValue();
			m.remove(owner);
		}
		currentValidMap.remove(owner);
	}

	/**
	 * says that this object has become 'invalid' in all contexts, for
	 * example, a vertex buffer has moved
	 * 
	 * @param owner
	 */

	public static void markAsInvalidInAllContexts(Object owner) {
		Iterator<?> it = contextValidMap.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<?, ?> e = (Entry<?, ?>) it.next();
			Map<?, ?> m = (Map<?, ?>) e.getValue();
			m.remove(owner);
		}
		currentValidMap.remove(owner);
	}

	public static void markAsValidInThisContext(Object owner) {
		currentValidMap.put(owner, new Boolean(true));
	}

	public static boolean isValid(Object owner) {
		Boolean b = currentValidMap.get(owner);
		return b == null ? false : b.booleanValue();
	}

	public static void delete(Object owner) {
		currentContextIdMap.remove(owner);
		contextOwnerMap.remove(owner);
		currentValidMap.remove(owner);
		contextValidMap.remove(owner);
	}

	/**
	 * @return
	 */
	public static Object getCurrentContext() {
		return currentContext;
	}

	public static void setGl(Object gl) {
		BasicContextManager.gl.set(gl);
	}

	public static Object getGl() {
		return (Object) gl.get();
	}

	public static void setGlu(Object glu) {
		BasicContextManager.glu.set(glu);
	}

	public static Object getGlu() {
		return glu.get();
	}

}
