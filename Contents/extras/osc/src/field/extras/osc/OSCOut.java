package field.extras.osc;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import field.core.dispatch.iVisualElement;
import field.core.network.OSCOutput;
import field.core.network.UDPNIOSender;
import field.core.util.FieldPyObjectAdaptor.iCallable;
import field.namespace.generic.Generics.Pair;

public class OSCOut implements iCallable {

	static public HashMap<Pair<Integer, String>, OSCOut> knownInterfaces = new LinkedHashMap<Pair<Integer, String>, OSCOut>();

	private final iVisualElement root;

	private OSCOutput out;

	Set<OSCOut> children = new LinkedHashSet<OSCOut>();
	
	public OSCOut(iVisualElement root, int port, String address) {
		this.root = root;
		
		out = new OSCOutput(20000, new UDPNIOSender(port, address));
	}
	
	public void send(String path, Object... message)
	{
		out.simpleSend(path, message);
	}

	public Object call(Object[] args) {

		if (args.length == 0)
			return this;
		if (args.length == 1) {
			OSCOut x = knownInterfaces.get(new Pair<Integer, String>(((Number) args[0]).intValue(), "255.255.255.255"));
			if (x == null)
				knownInterfaces.put(new Pair<Integer, String>(((Number) args[0]).intValue(), "255.255.255.255"), x = new OSCOut(root, ((Number) args[0]).intValue(), "255.255.255.255"));
			children.add(x);
			return x;
		} else if (args.length == 2) {
			OSCOut x = knownInterfaces.get(new Pair<Integer, String>(((Number) args[0]).intValue(), (String) args[1]));
			if (x == null)
				knownInterfaces.put(new Pair<Integer, String>(((Number) args[0]).intValue(), (String) args[1]), x = new OSCOut(root, ((Number) args[0]).intValue(), (String) args[1]));
			children.add(x);
			return x;
		}

		throw new IllegalArgumentException(" expected 0, 1, or 2 args to OSCOut()");
	}

	public void close() {
		out.close();
		for(OSCOut c : children)
		{
			c.close();
		}
	}

}
