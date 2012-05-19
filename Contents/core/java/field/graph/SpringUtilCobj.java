package field.graph;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;

import field.context.Context.Cobj;
import field.math.abstraction.iMetric;
import field.math.linalg.Vector2;
import field.util.Dict.Prop;

/**
 * It's a kamanda-kawai graph layout algorithm
 * 
 * @author marc
 * 
 */
public class SpringUtilCobj {

	protected double diameter = 10;
	private double length_factor = 0.9;
	private double disconnected_multiplier = 0.5;

	int currentIteration = 0;

	double EPSILON = 0.0001d;

	private iMetric<Cobj, Cobj> distance;
	private final ArrayList<Cobj> vertex;
	// private double[][] dm;
	private double L;
	double K = 1;

	boolean exchangeVertices = false;

	Prop<Vector2> position = new Prop<Vector2>("position");
	Prop<Map<Cobj, Float>> distanceCache = new Prop<Map<Cobj, Float>>("distanceCache");
	Prop<Integer> locked = new Prop<Integer>("locked");

	public SpringUtilCobj(iMetric<Cobj, Cobj> distance, ArrayList<Cobj> vertex) {
		this.distance = distance;
		this.vertex = vertex;
	}

	public void initialize() {
		currentIteration = 0;

		int n = vertex.size();

		L = (1 / diameter) * length_factor;

	}

	private Vector2 initialPosition(Cobj v) {
		return new Vector2().noise(1);
	}

	public void step() {

		for (Cobj v : vertex) {

			Vector2 p = v.getProperty(position);
			if (p == null) {
				Vector2 xyd = initialPosition(v);
				v.setProperty(position, xyd);
				v.setProperty(locked, 0);
			}
		}

		try {
			currentIteration++;
			double energy = calcEnergy();
			// status = "Kamada-Kawai V=" +
			// getGraph().getVertexCount() + "(" +
			// getGraph().getVertexCount() + ")" + " IT: " +
			// currentIteration + " E=" + energy;

			int n = vertex.size();
			if (n == 0)
				return;

			double maxDeltaM = 0;
			int pm = -1; // the node having max deltaM
			for (int i = 0; i < n; i++) {
				if (isLocked(i))
					continue;
				double deltam = calcDeltaM(i);

				if (maxDeltaM < deltam) {
					maxDeltaM = deltam;
					pm = i;
				}
			}
			if (pm == -1)
				return;

			for (int i = 0; i < 100; i++) {
				double[] dxy = calcDeltaXY(pm);

				Cobj m = vertex.get(pm);
				Vector2 xydata = m.getProperty(position);

				xydata.set((float) (xydata.x + dxy[0]), (float) (xydata.y + dxy[1]));

				double deltam = calcDeltaM(pm);
				if (deltam < EPSILON)
					break;
			}

			adjustForGravity();

			if (exchangeVertices && maxDeltaM < EPSILON) {
				energy = calcEnergy();
				for (int i = 0; i < n - 1; i++) {
					if (isLocked(i))
						continue;
					for (int j = i + 1; j < n; j++) {
						if (isLocked(j))
							continue;
						double xenergy = calcEnergyIfExchanged(i, j);
						if (energy > xenergy) {

							Cobj mi = vertex.get(i);
							Vector2 xydatai = mi.getProperty(position);
							Cobj mj = vertex.get(j);
							Vector2 xydataj = mj.getProperty(position);

							double sx = xydatai.x;
							double sy = xydatai.y;
							xydatai.set(xydataj);
							xydataj.set((float) sx, (float) sy);
							return;
						}
					}
				}
			}
		} finally {
			// fireStateChanged();
		}
	}

	private boolean isLocked(int Cobj) {
		Integer m = vertex.get(Cobj).getProperty(locked);

		return m != null && m.intValue() == 1;
	}

	/**
	 * Shift all vertices so that the center of gravity is located at the
	 * center of the screen.
	 */
	public void adjustForGravity() {
		double gx = 0;
		double gy = 0;
		for (int i = 0; i < vertex.size(); i++) {
			Vector2 xydatai = vertex.get(i).getProperty(position);
			gx += xydatai.x;
			gy += xydatai.y;
		}
		gx /= vertex.size();
		gy /= vertex.size();
		double diffx = 1 / 2f - gx;
		double diffy = 1 / 2f - gy;
		for (int i = 0; i < vertex.size(); i++) {
			Vector2 xydatai = vertex.get(i).getProperty(position);
			xydatai.set((float) (xydatai.x + diffx), (float) (xydatai.y + diffy));
		}
	}

	private double[] calcDeltaXY(int m) {
		double dE_dxm = 0;
		double dE_dym = 0;
		double d2E_d2xm = 0;
		double d2E_dxmdym = 0;
		double d2E_dymdxm = 0;
		double d2E_d2ym = 0;

		for (int i = 0; i < vertex.size(); i++) {
			if (i != m) {
				Vector2 xydatam = vertex.get(m).getProperty(position);
				Vector2 xydatai = vertex.get(i).getProperty(position);

				double dist = getDistance(m, i); // dm[m][i];
				double l_mi = L * dist;
				double k_mi = K / (dist * dist);
				double dx = xydatam.x - xydatai.x;
				double dy = xydatam.y - xydatai.y;
				double d = Math.sqrt(dx * dx + dy * dy);
				double ddd = d * d * d;

				dE_dxm += k_mi * (1 - l_mi / d) * dx;
				dE_dym += k_mi * (1 - l_mi / d) * dy;
				d2E_d2xm += k_mi * (1 - l_mi * dy * dy / ddd);
				d2E_dxmdym += k_mi * l_mi * dx * dy / ddd;
				d2E_d2ym += k_mi * (1 - l_mi * dx * dx / ddd);
			}
		}
		// d2E_dymdxm equals to d2E_dxmdym.
		d2E_dymdxm = d2E_dxmdym;

		double denomi = d2E_d2xm * d2E_d2ym - d2E_dxmdym * d2E_dymdxm;
		double deltaX = (d2E_dxmdym * dE_dym - d2E_d2ym * dE_dxm) / denomi;
		double deltaY = (d2E_dymdxm * dE_dxm - d2E_d2xm * dE_dym) / denomi;
		return new double[] { deltaX, deltaY };
	}

	/**
	 * Calculates the gradient of energy function at the vertex m.
	 */
	private double calcDeltaM(int m) {
		double dEdxm = 0;
		double dEdym = 0;
		for (int i = 0; i < vertex.size(); i++) {
			if (i != m) {
				double dist = getDistance(m, i);// dm[m][i];
				double l_mi = L * dist;
				double k_mi = K / (dist * dist);

				Vector2 xydatam = vertex.get(m).getProperty(position);
				Vector2 xydatai = vertex.get(i).getProperty(position);

				double dx = xydatam.x - xydatai.x;
				double dy = xydatam.y - xydatai.y;
				double d = Math.sqrt(dx * dx + dy * dy);

				double common = k_mi * (1 - l_mi / d);
				dEdxm += common * dx;
				dEdym += common * dy;
			}
		}
		return Math.sqrt(dEdxm * dEdxm + dEdym * dEdym);
	}

	/**
	 * Calculates the energy function E.
	 */
	private double calcEnergy() {
		double energy = 0;
		for (int i = 0; i < vertex.size() - 1; i++) {
			for (int j = i + 1; j < vertex.size(); j++) {
				double dist = getDistance(i, j);// dm[i][j];
				double l_ij = L * dist;
				double k_ij = K / (dist * dist);

				Vector2 xydatai = vertex.get(i).getProperty(position);
				Vector2 xydataj = vertex.get(j).getProperty(position);

				double dx = xydatai.x - xydataj.x;
				double dy = xydatai.y - xydataj.y;
				double d = Math.sqrt(dx * dx + dy * dy);

				energy += k_ij / 2 * (dx * dx + dy * dy + l_ij * l_ij - 2 * l_ij * d);
			}
		}
		return energy;
	}

	/**
	 * Calculates the energy function E as if positions of the specified
	 * vertices are exchanged.
	 */
	private double calcEnergyIfExchanged(int p, int q) {
		if (p >= q)
			throw new RuntimeException("p should be < q");
		double energy = 0; // < 0
		for (int i = 0; i < vertex.size() - 1; i++) {
			for (int j = i + 1; j < vertex.size(); j++) {
				int ii = i;
				int jj = j;
				if (i == p)
					ii = q;
				if (j == q)
					jj = p;

				double dist = getDistance(i, j);// dm[i][j];
				double l_ij = L * dist;
				double k_ij = K / (dist * dist);

				Vector2 xydataii = vertex.get(ii).getProperty(position);
				Vector2 xydatajj = vertex.get(jj).getProperty(position);

				double dx = xydataii.x - xydatajj.x;
				double dy = xydataii.y - xydatajj.y;
				double d = Math.sqrt(dx * dx + dy * dy);

				energy += k_ij / 2 * (dx * dx + dy * dy + l_ij * l_ij - 2 * l_ij * d);
			}
		}
		return energy;
	}

	private double getDistance(int i, int j) {
		Cobj d = vertex.get(i);
		Map<Cobj, Float> c = d.getProperty(distanceCache);
		if (c == null)
			d.setProperty(distanceCache, c = new HashMap<Cobj, Float>());

		Float dd = c.get(vertex.get(j));

		if (dd == null) {
			float d_ij = distance.distance(vertex.get(i), vertex.get(j));
			float d_ji = distance.distance(vertex.get(j), vertex.get(i));
			double dist = diameter * disconnected_multiplier;
			if (d_ij > 0)
				dist = Math.min(d_ij, dist);
			if (d_ji > 0)
				dist = Math.min(d_ji, dist);
			c.put(vertex.get(j), dd = (float) dist);
		}

		return dd;
	}
}
