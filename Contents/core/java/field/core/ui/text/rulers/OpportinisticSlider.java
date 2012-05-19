package field.core.ui.text.rulers;

import java.text.NumberFormat;

import org.python.core.Py;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PyTuple;

import field.core.execution.PythonInterface;
import field.math.BaseMath;
import field.namespace.generic.Bind.iFunction;
import field.util.Dict.Prop;

/**
 * gui elements by tokenization
 * 
 * @author marc
 * 
 */
public class OpportinisticSlider {

	public static Prop<OpportinisticSlider> oSlider = new Prop<OpportinisticSlider>(
			"oSlider");

	private double dw;
	private double dn;
	private int loc;
	private String sn;

	private double increment;

	public OpportinisticSlider() {
	}

	// todo \u2014\u2014handle negative numbers!

	public boolean execute(String previous, String now) {
		try {
			PyList was = (PyList) PythonInterface.getPythonInterface()
					.executeStringReturnPyObject(
							"__tok = __tokenizeHelper('''" + previous
									+ "''')", "__tok");
			PyList is = (PyList) PythonInterface
					.getPythonInterface()
					.executeStringReturnPyObject(
							"__tok = __tokenizeHelper('''" + now + "''')",
							"__tok");

			if (was.__len__() != is.__len__())
				return false;

			for (int i = 0; i < was.__len__(); i++) {
				PyTuple w = (PyTuple) was.get(i);
				PyTuple n = (PyTuple) is.get(i);

				if (Py.py2int((PyObject) w.__getitem__(0)) == Py
						.py2int((PyObject) n.__getitem__(0))
						&& Py.py2int((PyObject) n.__getitem__(0)) == 2) {
					String sw = Py.tojava((PyObject) w.__getitem__(1),
							String.class);
					String sn = Py.tojava((PyObject) n.__getitem__(1),
							String.class);
					if (!sw.equals(sn)) {
						int wrstart = Py.py2int((PyObject) ((PyTuple) w
								.__getitem__(2)).__getitem__(0));
						int wrend = Py.py2int((PyObject) ((PyTuple) w
								.__getitem__(3)).__getitem__(0));
						int nrstart = Py.py2int((PyObject) ((PyTuple) n
								.__getitem__(2)).__getitem__(0));
						int nrend = Py.py2int((PyObject) ((PyTuple) n
								.__getitem__(3)).__getitem__(0));
						int wcstart = Py.py2int((PyObject) ((PyTuple) w
								.__getitem__(2)).__getitem__(1));
						int wcend = Py.py2int((PyObject) ((PyTuple) w
								.__getitem__(3)).__getitem__(1));
						int ncstart = Py.py2int((PyObject) ((PyTuple) n
								.__getitem__(2)).__getitem__(1));
						int ncend = Py.py2int((PyObject) ((PyTuple) n
								.__getitem__(3)).__getitem__(1));

						createSliderAt(sw, sn, nrstart, ncstart, nrend, ncend,
								now);
					}
				}

			}

			System.out.println(" os = <" + was + "> <" + is + ">");
			return true;
		} catch (Throwable t) {
			t.printStackTrace();
			return false;
		}
	}

	private void createSliderAt(String sw, String sn, int nrstart, int ncstart,
			int nrend, int ncend, String now) {

		System.out.println(" would create slider between <" + sw + "> <" + sn
				+ "> <" + nrstart + ":" + ncstart + "> <" + nrend + "> <"
				+ ncend + ">");

		dw = Double.parseDouble(sw);
		dn = Double.parseDouble(sn);

		loc = convertToLocation(nrstart, ncstart, nrend, ncend, now);

		this.sn = sn;

		increment = Math.pow(10, Math.round(Math.log10(Math.abs(dn - dw))));

	}

	private int convertToLocation(int nrstart, int ncstart, int nrend,
			int ncend, String now) {
		String[] mm = now.split("\n");
		int row = 0;
		int offset = 0;
		for (int i = 0; i < mm.length; i++) {
			if (mm[i].length() > 0) {
				row++;
				if (row == nrstart) {
					break;
				}
			}
			offset += mm[i].length() + 1;
		}

		return offset + ncstart;
	}

	public iFunction<String, String> getUp() {
		return new iFunction<String, String>() {
			public String f(String in) {
				return up(in);
			}
		};
	}

	public String up(String x) {
		if (x.length() < loc)
			return x;
		if (!x.substring(loc).startsWith(sn))
			return x;

		double nn = dn + increment;

		String next = roundLike(nn, sn);
		String n = x.substring(0, loc) + next + x.substring(loc + sn.length());

		dn = nn;

		sn = "" + next;

		return n;
	}

	public iFunction<String, String> getDown() {
		return new iFunction<String, String>() {
			public String f(String in) {
				return down(in);
			}
		};
	}

	public String down(String x) {
		if (x.length() < loc)
			return x;
		if (!x.substring(loc).startsWith(sn))
			return x;

		double nn = dn - increment;

		String next = roundLike(nn, sn);
		String n = x.substring(0, loc) + next + x.substring(loc + sn.length());

		dn = nn;

		sn = "" + roundLike(nn, sn);

		return n;
	}

	private String roundLike(double nn, String like) {

		int mm = 0;
		if (like.indexOf('.') == -1) {
			mm = 1;
			mm = like.length() - like.indexOf('.');
		}

		NumberFormat _format = NumberFormat.getInstance();
		_format.setMaximumFractionDigits(mm);
		_format.setMinimumFractionDigits(0);

		return _format.format(nn);
	}

}
