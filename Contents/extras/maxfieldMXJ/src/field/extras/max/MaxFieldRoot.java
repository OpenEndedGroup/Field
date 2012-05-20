package field.extras.max;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.cycling74.max.Atom;
import com.cycling74.max.Executable;
import com.cycling74.max.MaxBox;
import com.cycling74.max.MaxClock;
import com.cycling74.max.MaxObject;
import com.cycling74.max.MaxPatcher;

import field.extras.max.OSCInput.DispatchableHandler;

public class MaxFieldRoot extends MaxObject {

	public OSCInput in;
	public OSCOutput out;
	public OSCOutput toField;
	private MaxClock clock;

	public ByteArrayOutputStream printout = new ByteArrayOutputStream() {
		@Override
		public void flush() throws IOException {
			try {
				String s = new String(toByteArray());
				
				if (returnAddress != null)
					out.simpleSend("/print/" + returnAddress, s);
				else
					out.simpleSend("/print", s);
			} finally {
				this.reset();
			}
		}
	};
	public ByteArrayOutputStream printerr = new ByteArrayOutputStream() {
		@Override
		public void flush() throws IOException {
			try {
				String s = new String(toByteArray());

				if (returnAddress != null)
					out.simpleSend("/printError/" + returnAddress, s);
				else
					out.simpleSend("/printError", s);
				if (s.length() > 0)
					MaxObject.error(s);
			} finally {
				this.reset();
			}
		}
	};

	String returnAddress = null;

	public MaxFieldRoot(Atom[] args) {
		MaxField.thisRoot = this;
		in = new OSCInput(8789);
		out = new OSCOutput(20000, new UDPNIOSender(8790, "127.0.0.1"));
		toField = new OSCOutput(20000, new UDPNIOSender(8791, "127.0.0.1"));
		in.setDefaultHandler(new DispatchableHandler() {
			public void handle(String s, Object[] args) {
				MaxFieldRoot.this.handle(s, args);
			}
		});
		clock = new MaxClock(new Executable() {
			boolean first = true;
			
			public void execute() {
				if (first) {
					first = false;
					updateMapping();
				}
				clock.delay(50f);
				in.update();
			}
		});
		clock.delay(50);
		
	}
	
	@Override
	protected void loadbang() {
		super.loadbang();
	}

	protected void handle(String s, Object[] args) {
		MaxField.thisRoot = this;
		if (s.startsWith("/message/")) {
			String address = s.substring("/message".length());
			MaxBox m = mapping.get(address);
			if (m == null) {
				MaxObject.post("couldn't find reciever for internal osc message '" + address + "'");
				return;
			}
			String message = (String) args[0];
			m.send(message, new Atom[0]);
		}

		if (s.startsWith("/data/")) {
			String address = s.substring("/data".length());
			MaxBox m = mapping.get(address);
			if (m == null) {
				MaxObject.post("couldn't find reciever for internal osc data message '" + address + "'");
				return;
			}
			String message = (String) args[1];

			;//;//System.out.println(" sending data message called <" + args[0] + "> of length <" + message.length() + "> to <" + address + ">");
			m.send("__data__", new Atom[] { Atom.newAtom((String) args[0]), Atom.newAtom(message) });
		}
		if (s.startsWith("/return/")) {
			returnAddress = s.substring("/return/".length());
		}
	}

	@Override
	protected void bang() {
		if (getInlet() == 0) {
			updateMapping();
		}
	}

	Map<String, MaxBox> mapping = new LinkedHashMap<String, MaxBox>();

	protected void updateMapping() {
		MaxObject.post("updating mapping");
		MaxPatcher inside = this.getParentPatcher();
		map("", inside);
		MaxObject.post("final mapping is <" + mapping + ">");
	}

	protected void map(String string, MaxPatcher inside) {
		MaxBox[] boxes = inside.getAllBoxes();
		for (MaxBox b : boxes) {
			if (b.isPatcher()) {
				MaxPatcher subpatch = b.getPatcher();
				String name = b.getName();
				map(string + "/" + name, subpatch);
			}
			MaxObject.post(b + " " + b.getName() + " " + b.getMaxClass() + " " + b.getClass());
			// if (b.getMaxClass().equals("mxj"))
			{
				mapping.put(string + "/" + b.getName(), b);
			}
		}
	}

	@Override
	protected void notifyDeleted() {
		clock.unset();
		in.close();
		out.close();
	}

	public void notifyDeleted(MaxField maxField) {
		mapping.values().remove(maxField);
	}

	public String getPath(MaxBox maxBox) {
		Set<Entry<String, MaxBox>> es = mapping.entrySet();
		for (Entry<String, MaxBox> e : es) {
			if (e.getValue().equals(maxBox))
				return e.getKey();
		}
		return null;

	}

}
