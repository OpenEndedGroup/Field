package field.graph;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;

import field.math.abstraction.iMetric;
import field.math.linalg.Vector2;

/**
 * It's a kamanda-kawai graph layout algorithm
 * 
 * @author marc
 * 
 */
public class SpringUtil<t_node> {

	protected double diameter = 10;
	private double length_factor = 0.9;
	private double disconnected_multiplier = 0.5;

	int currentIteration = 0;

	double EPSILON = 0.0001d;

	private iMetric<t_node, t_node> distance;
	private final ArrayList<t_node> vertex;
	private double[][] dm;
	private Vector2[] xydata;
	private double L;
	double K = 1;
	
	boolean exchangeVertices = false;
	
	public boolean[] locked; 
	

	public SpringUtil(iMetric<t_node, t_node> distance, ArrayList<t_node> vertex)
	{
		this.distance = distance;
		this.vertex = vertex;
	}
	
	public void initialize() {
		currentIteration = 0;


		int n = vertex.size();
		dm = new double[n][n];
		xydata = new Vector2[n];
		locked = new boolean[n];

		// assign IDs to all visible vertices
		while (true) {
			try {
				int index = 0;
				for (t_node v : vertex) {
					Vector2 xyd = initialPosition(v);
					xydata[index] = xyd;
					locked[index] = false;
					index++;
				}
				break;
			} catch (ConcurrentModificationException cme) {
			}
		}

		//diameter = DistanceStatistics.<V, E> diameter(graph, distance, true);

		L = (1/ diameter) * length_factor; // length_factor
							// used to be
							// hardcoded to
							// 0.9
		// L = 0.75 * Math.sqrt(height * width / n);

		for (int i = 0; i < n - 1; i++) {
			for (int j = i + 1; j < n; j++) {
				float d_ij = distance.distance(vertex.get(i), vertex.get(j));
				float d_ji = distance.distance(vertex.get(j), vertex.get(i));
				double dist = diameter * disconnected_multiplier;
				if (d_ij >0)
					dist = Math.min(d_ij, dist);
				if (d_ji >0)
					dist = Math.min(d_ji, dist);
				dm[i][j] = dm[j][i] = dist;
			}
		}

	}

	private Vector2 initialPosition(t_node v) {
		return new Vector2().noise(1);
	}

	public void step() {
		try {
			currentIteration++;
			double energy = calcEnergy();
//			status = "Kamada-Kawai V=" + getGraph().getVertexCount() + "(" + getGraph().getVertexCount() + ")" + " IT: " + currentIteration + " E=" + energy;

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
				xydata[pm].set((float)(xydata[pm].x + dxy[0]), (float)(xydata[pm].y + dxy[1]));

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
							double sx = xydata[i].x;
							double sy = xydata[i].y;
							xydata[i].set(xydata[j]);
							xydata[j].set((float)sx, (float) sy);
							return;
						}
					}
				}
			}
		} finally {
			// fireStateChanged();
		}
	}

	private boolean isLocked(int t_node) {
		return locked[t_node];
	}

	/**
	 * Shift all vertices so that the center of gravity is located at the
	 * center of the screen.
	 */
	public void adjustForGravity() {
		double gx = 0;
		double gy = 0;
		for (int i = 0; i < xydata.length; i++) {
			gx += xydata[i].x;
			gy += xydata[i].y;
		}
		gx /= xydata.length;
		gy /= xydata.length;
		double diffx = 1/ 2f - gx;
		double diffy = 1/ 2f - gy;
		for (int i = 0; i < xydata.length; i++) {
			xydata[i].set((float)(xydata[i].x + diffx), (float)(xydata[i].y + diffy));
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

				double dist = dm[m][i];
				double l_mi = L * dist;
				double k_mi = K / (dist * dist);
				double dx = xydata[m].x - xydata[i].x;
				double dy = xydata[m].y - xydata[i].y;
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
				double dist = dm[m][i];
				double l_mi = L * dist;
				double k_mi = K / (dist * dist);

				double dx = xydata[m].x - xydata[i].x;
				double dy = xydata[m].y - xydata[i].y;
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
				double dist = dm[i][j];
				double l_ij = L * dist;
				double k_ij = K / (dist * dist);
				double dx = xydata[i].x - xydata[j].x;
				double dy = xydata[i].y - xydata[j].y;
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

				double dist = dm[i][j];
				double l_ij = L * dist;
				double k_ij = K / (dist * dist);
				double dx = xydata[ii].x - xydata[jj].x;
				double dy = xydata[ii].y - xydata[jj].y;
				double d = Math.sqrt(dx * dx + dy * dy);

				energy += k_ij / 2 * (dx * dx + dy * dy + l_ij * l_ij - 2 * l_ij * d);
			}
		}
		return energy;
	}
}
