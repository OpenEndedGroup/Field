package field.core.ui;

import java.awt.geom.GeneralPath;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;

import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.Region;

import field.launch.Launcher;
import field.math.linalg.Vector2;
import field.namespace.generic.Bind.iFunction;

/**
 * largely copied from work by L. Paul Chew (although modified enough so that
 * all the bugs are surely mine, not his)
 * http://www.cs.cornell.edu/Info/People/chew/Delaunay.html
 * 
 * original copyright notice follows:
 */

/*
 * Copyright (c) 2007 by L. Paul Chew.
 * 
 * Permission is hereby granted, without written agreement and without license
 * or royalty fees, to use, copy, modify, and distribute this software and its
 * documentation for any purpose, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

public class SimpleVoronoi {

	int initialSize = 10000;
	Triangle initialTriangle = new Triangle(new Pnt(-initialSize, -initialSize), new Pnt(initialSize, -initialSize), new Pnt(0, initialSize));
	Triangulation dt = new Triangulation(initialTriangle);

	public Pnt add(Vector2 location) {
		Pnt site = new Pnt(location.x, location.y);
		dt.delaunayPlace(site);
		return site;
	}

	public Pnt[] getContourForSite(Pnt sitex) {
		HashSet<Pnt> done = new HashSet<Pnt>(initialTriangle);
		for (Triangle triangle : dt)
			for (Pnt site : triangle) {
				if (done.contains(site))
					continue;
				done.add(site);
				List<Triangle> list = dt.surroundingTriangles(site, triangle);
				Pnt[] vertices = new Pnt[list.size()];
				int i = 0;
				for (Triangle tri : list)
					vertices[i++] = tri.getCircumcenter();
				// draw(vertices, withFill ? getColor(site) :
				// null);
				if (site == sitex)
					return vertices;
			}
		return null;
	}

	public List<Pnt[]> allContoursSortedByArea(iFunction<Number, Pnt[]> filter) {
		ArrayList<Pnt[]> p = new ArrayList<Pnt[]>();
		HashSet<Pnt> done = new HashSet<Pnt>(initialTriangle);
		for (Triangle triangle : dt)
			outer: for (Pnt site : triangle) {
//				if (done.contains(site))
//					continue;
				done.add(site);
				List<Triangle> list = dt.surroundingTriangles(site, triangle);
				Pnt[] vertices = new Pnt[list.size()];
				int i = 0;
				for (Triangle tri : list)
				{
					if (tri==null) break outer;
					vertices[i++] = tri.getCircumcenter();
				}
				if (filter == null || filter.f(vertices).intValue() > 0)
					p.add(vertices);
			}
		Collections.sort(p, new Comparator<Pnt[]>() {

			@Override
			public int compare(Pnt[] arg0, Pnt[] arg1) {
				return Double.compare(Math.abs(area(arg0)), Math.abs(area(arg1)));
			}
		});
		return p;
	}

	protected double area(Pnt[] A) {
		double a = 0;
		for (int i = 0; i < A.length ; i++) {
			a += A[i].coordinates[0] * A[(i+1)%A.length].coordinates[1] - A[(i+1)%A.length].coordinates[0] * A[i].coordinates[1];
		}
		return a / 2;
	}

	protected Vector2 centroid(Pnt[] A) {
		double x = 0;
		double y = 0;
		double aa = 0;
		for (int i = 0; i < A.length; i++) {
			double a = A[i].coordinates[0] * A[(i+1)%A.length].coordinates[1] - A[(i+1)%A.length].coordinates[0] * A[i].coordinates[1];
			aa += a;
			x += (A[i].coordinates[0] + A[(i+1)%A.length].coordinates[0]) * a;
			y += (A[i].coordinates[1] + A[(i+1)%A.length].coordinates[1]) * a;
		}
		aa /= 2;
		return new Vector2(x, y).scale((float) (1 / (6 * aa)));
	}

	public GeneralPath makeArea(Pnt[] p) {
		GeneralPath path = new GeneralPath();
		for (int i = 0; i < p.length; i++) {
			if (i == 0)
				path.moveTo((float) p[0].coord(0), (float) p[0].coord(1));
			else
				path.lineTo((float) p[i].coord(0), (float) p[i].coord(1));
		}
		path.closePath();
		return path;
	}

	public Region makeRegion(Pnt[] p) {

		Region path = new Region();

		int[] pp = new int[2 * p.length];

		for (int i = 0; i < p.length; i++) {
			pp[2 * i + 0] = (int) p[i].coord(0);
			pp[2 * i + 1] = (int) p[i].coord(1);

		}
		path.add(pp);
		return path;
	}

	public Path makePath(Pnt[] p) {

		Path path = new Path(Launcher.display);
		for (int i = 0; i < p.length; i++) {
			if (i == 0)
				path.moveTo((float) p[0].coord(0), (float) p[0].coord(1));
			else
				path.lineTo((float) p[i].coord(0), (float) p[i].coord(1));
		}
		path.close();
		return path;
	}

	static public class ArraySet<E> extends AbstractSet<E> {

		private ArrayList<E> items; // Items of the set

		/**
		 * Create an empty set (default initial capacity is 3).
		 */
		public ArraySet() {
			this(3);
		}

		/**
		 * Create an empty set with the specified initial capacity.
		 * 
		 * @param initialCapacity
		 *                the initial capacity
		 */
		public ArraySet(int initialCapacity) {
			items = new ArrayList<E>(initialCapacity);
		}

		/**
		 * Create a set containing the items of the collection. Any
		 * duplicate items are discarded.
		 * 
		 * @param collection
		 *                the source for the items of the small set
		 */
		public ArraySet(Collection<? extends E> collection) {
			items = new ArrayList<E>(collection.size());
			for (E item : collection)
				if (!items.contains(item))
					items.add(item);
		}

		/**
		 * Get the item at the specified index.
		 * 
		 * @param index
		 *                where the item is located in the ListSet
		 * @return the item at the specified index
		 * @throws IndexOutOfBoundsException
		 *                 if the index is out of bounds
		 */
		public E get(int index) throws IndexOutOfBoundsException {
			return items.get(index);
		}

		/**
		 * True iff any member of the collection is also in the
		 * ArraySet.
		 * 
		 * @param collection
		 *                the Collection to check
		 * @return true iff any member of collection appears in this
		 *         ArraySet
		 */
		public boolean containsAny(Collection<?> collection) {
			for (Object item : collection)
				if (this.contains(item))
					return true;
			return false;
		}

		@Override
		public boolean add(E item) {
			if (items.contains(item))
				return false;
			return items.add(item);
		}

		@Override
		public Iterator<E> iterator() {
			return items.iterator();
		}

		@Override
		public int size() {
			return items.size();
		}

	}

	public class Graph<N> {

		private Map<N, Set<N>> theNeighbors = // Node -> adjacent nodes
		new HashMap<N, Set<N>>();
		private Set<N> theNodeSet = // Set view of all nodes
		Collections.unmodifiableSet(theNeighbors.keySet());

		/**
		 * Add a node. If node is already in graph then no change.
		 * 
		 * @param node
		 *                the node to add
		 */
		public void add(N node) {
			if (theNeighbors.containsKey(node))
				return;
			theNeighbors.put(node, new ArraySet<N>());
		}

		/**
		 * Add a link. If the link is already in graph then no change.
		 * 
		 * @param nodeA
		 *                one end of the link
		 * @param nodeB
		 *                the other end of the link
		 * @throws NullPointerException
		 *                 if either endpoint is not in graph
		 */
		public void add(N nodeA, N nodeB) throws NullPointerException {
			theNeighbors.get(nodeA).add(nodeB);
			theNeighbors.get(nodeB).add(nodeA);
		}

		/**
		 * Remove node and any links that use node. If node not in
		 * graph, nothing happens.
		 * 
		 * @param node
		 *                the node to remove.
		 */
		public void remove(N node) {
			if (!theNeighbors.containsKey(node))
				return;
			for (N neighbor : theNeighbors.get(node))
				theNeighbors.get(neighbor).remove(node); // Remove
			// "to"
			// links
			theNeighbors.get(node).clear(); // Remove "from" links
			theNeighbors.remove(node); // Remove the node
		}

		/**
		 * Remove the specified link. If link not in graph, nothing
		 * happens.
		 * 
		 * @param nodeA
		 *                one end of the link
		 * @param nodeB
		 *                the other end of the link
		 * @throws NullPointerException
		 *                 if either endpoint is not in graph
		 */
		public void remove(N nodeA, N nodeB) throws NullPointerException {
			theNeighbors.get(nodeA).remove(nodeB);
			theNeighbors.get(nodeB).remove(nodeA);
		}

		/**
		 * Report all the neighbors of node.
		 * 
		 * @param node
		 *                the node
		 * @return the neighbors of node
		 * @throws NullPointerException
		 *                 if node does not appear in graph
		 */
		public Set<N> neighbors(N node) throws NullPointerException {
			return Collections.unmodifiableSet(theNeighbors.get(node));
		}

		/**
		 * Returns an unmodifiable Set view of the nodes contained in
		 * this graph. The set is backed by the graph, so changes to the
		 * graph are reflected in the set.
		 * 
		 * @return a Set view of the graph's node set
		 */
		public Set<N> nodeSet() {
			return theNodeSet;
		}

	}

	static public class Pnt {

		private double[] coordinates; // The point's coordinates

		/**
		 * Constructor.
		 * 
		 * @param coords
		 *                the coordinates
		 */
		public Pnt(double... coords) {
			// Copying is done here to ensure that Pnt's coords
			// cannot be altered.
			// This is necessary because the double... notation
			// actually creates a
			// constructor with double[] as its argument.
			coordinates = new double[coords.length];
			System.arraycopy(coords, 0, coordinates, 0, coords.length);
		}

		@Override
		public String toString() {
			if (coordinates.length == 0)
				return "Pnt()";
			String result = "Pnt(" + coordinates[0];
			for (int i = 1; i < coordinates.length; i++)
				result = result + "," + coordinates[i];
			result = result + ")";
			return result;
		}

		@Override
		public boolean equals(Object other) {
			if (!(other instanceof Pnt))
				return false;
			Pnt p = (Pnt) other;
			if (this.coordinates.length != p.coordinates.length)
				return false;
			for (int i = 0; i < this.coordinates.length; i++)
				if (this.coordinates[i] != p.coordinates[i])
					return false;
			return true;
		}

		@Override
		public int hashCode() {
			int hash = 0;
			for (double c : this.coordinates) {
				long bits = Double.doubleToLongBits(c);
				hash = (31 * hash) ^ (int) (bits ^ (bits >> 32));
			}
			return hash;
		}

		/* Pnts as vectors */

		/**
		 * @return the specified coordinate of this Pnt
		 * @throws ArrayIndexOutOfBoundsException
		 *                 for bad coordinate
		 */
		public double coord(int i) {
			return this.coordinates[i];
		}

		/**
		 * @return this Pnt's dimension.
		 */
		public int dimension() {
			return coordinates.length;
		}

		/**
		 * Check that dimensions match.
		 * 
		 * @param p
		 *                the Pnt to check (against this Pnt)
		 * @return the dimension of the Pnts
		 * @throws IllegalArgumentException
		 *                 if dimension fail to match
		 */
		public int dimCheck(Pnt p) {
			int len = this.coordinates.length;
			if (len != p.coordinates.length)
				throw new IllegalArgumentException("Dimension mismatch");
			return len;
		}

		/**
		 * Create a new Pnt by adding additional coordinates to this
		 * Pnt.
		 * 
		 * @param coords
		 *                the new coordinates (added on the right end)
		 * @return a new Pnt with the additional coordinates
		 */
		public Pnt extend(double... coords) {
			double[] result = new double[coordinates.length + coords.length];
			System.arraycopy(coordinates, 0, result, 0, coordinates.length);
			System.arraycopy(coords, 0, result, coordinates.length, coords.length);
			return new Pnt(result);
		}

		/**
		 * Dot product.
		 * 
		 * @param p
		 *                the other Pnt
		 * @return dot product of this Pnt and p
		 */
		public double dot(Pnt p) {
			int len = dimCheck(p);
			double sum = 0;
			for (int i = 0; i < len; i++)
				sum += this.coordinates[i] * p.coordinates[i];
			return sum;
		}

		/**
		 * Magnitude (as a vector).
		 * 
		 * @return the Euclidean length of this vector
		 */
		public double magnitude() {
			return Math.sqrt(this.dot(this));
		}

		/**
		 * Subtract.
		 * 
		 * @param p
		 *                the other Pnt
		 * @return a new Pnt = this - p
		 */
		public Pnt subtract(Pnt p) {
			int len = dimCheck(p);
			double[] coords = new double[len];
			for (int i = 0; i < len; i++)
				coords[i] = this.coordinates[i] - p.coordinates[i];
			return new Pnt(coords);
		}

		/**
		 * Add.
		 * 
		 * @param p
		 *                the other Pnt
		 * @return a new Pnt = this + p
		 */
		public Pnt add(Pnt p) {
			int len = dimCheck(p);
			double[] coords = new double[len];
			for (int i = 0; i < len; i++)
				coords[i] = this.coordinates[i] + p.coordinates[i];
			return new Pnt(coords);
		}

		/**
		 * Angle (in radians) between two Pnts (treated as vectors).
		 * 
		 * @param p
		 *                the other Pnt
		 * @return the angle (in radians) between the two Pnts
		 */
		public double angle(Pnt p) {
			return Math.acos(this.dot(p) / (this.magnitude() * p.magnitude()));
		}

		/**
		 * Perpendicular bisector of two Pnts. Works in any dimension.
		 * The coefficients are returned as a Pnt of one higher
		 * dimension (e.g., (A,B,C,D) for an equation of the form Ax +
		 * By + Cz + D = 0).
		 * 
		 * @param point
		 *                the other point
		 * @return the coefficients of the perpendicular bisector
		 */
		public Pnt bisector(Pnt point) {
			dimCheck(point);
			Pnt diff = this.subtract(point);
			Pnt sum = this.add(point);
			double dot = diff.dot(sum);
			return diff.extend(-dot / 2);
		}

		/* Pnts as simplices */

		/**
		 * Relation between this Pnt and a simplex (represented as an
		 * array of Pnts). Result is an array of signs, one for each
		 * vertex of the simplex, indicating the relation between the
		 * vertex, the vertex's opposite facet, and this Pnt.
		 * 
		 * <pre>
		 *   -1 means Pnt is on same side of facet
		 *    0 means Pnt is on the facet
		 *   +1 means Pnt is on opposite side of facet
		 * </pre>
		 * 
		 * @param simplex
		 *                an array of Pnts representing a simplex
		 * @return an array of signs showing relation between this Pnt
		 *         and simplex
		 * @throws IllegalArgumentExcpetion
		 *                 if the simplex is degenerate
		 */
		public int[] relation(Pnt[] simplex) {
			/*
			 * In 2D, we compute the cross of this matrix: 1 1 1 1
			 * p0 a0 b0 c0 p1 a1 b1 c1 where (a, b, c) is the
			 * simplex and p is this Pnt. The result is a vector in
			 * which the first coordinate is the signed area (all
			 * signed areas are off by the same constant factor) of
			 * the simplex and the remaining coordinates are the
			 * *negated* signed areas for the simplices in which p
			 * is substituted for each of the vertices. Analogous
			 * results occur in higher dimensions.
			 */
			int dim = simplex.length - 1;
			if (this.dimension() != dim)
				throw new IllegalArgumentException("Dimension mismatch");

			/* Create and load the matrix */
			Pnt[] matrix = new Pnt[dim + 1];
			/* First row */
			double[] coords = new double[dim + 2];
			for (int j = 0; j < coords.length; j++)
				coords[j] = 1;
			matrix[0] = new Pnt(coords);
			/* Other rows */
			for (int i = 0; i < dim; i++) {
				coords[0] = this.coordinates[i];
				for (int j = 0; j < simplex.length; j++)
					coords[j + 1] = simplex[j].coordinates[i];
				matrix[i + 1] = new Pnt(coords);
			}

			/*
			 * Compute and analyze the vector of
			 * areas/volumes/contents
			 */
			Pnt vector = cross(matrix);
			double content = vector.coordinates[0];
			int[] result = new int[dim + 1];
			for (int i = 0; i < result.length; i++) {
				double value = vector.coordinates[i + 1];
				if (Math.abs(value) <= 1.0e-6 * Math.abs(content))
					result[i] = 0;
				else if (value < 0)
					result[i] = -1;
				else
					result[i] = 1;
			}
			if (content < 0) {
				for (int i = 0; i < result.length; i++)
					result[i] = -result[i];
			}
			if (content == 0) {
				for (int i = 0; i < result.length; i++)
					result[i] = Math.abs(result[i]);
			}
			return result;
		}

		/**
		 * Test if this Pnt is outside of simplex.
		 * 
		 * @param simplex
		 *                the simplex (an array of Pnts)
		 * @return simplex Pnt that "witnesses" outsideness (or null if
		 *         not outside)
		 */
		public Pnt isOutside(Pnt[] simplex) {
			int[] result = this.relation(simplex);
			for (int i = 0; i < result.length; i++) {
				if (result[i] > 0)
					return simplex[i];
			}
			return null;
		}

		/**
		 * Test if this Pnt is on a simplex.
		 * 
		 * @param simplex
		 *                the simplex (an array of Pnts)
		 * @return the simplex Pnt that "witnesses" on-ness (or null if
		 *         not on)
		 */
		public Pnt isOn(Pnt[] simplex) {
			int[] result = this.relation(simplex);
			Pnt witness = null;
			for (int i = 0; i < result.length; i++) {
				if (result[i] == 0)
					witness = simplex[i];
				else if (result[i] > 0)
					return null;
			}
			return witness;
		}

		/**
		 * Test if this Pnt is inside a simplex.
		 * 
		 * @param simplex
		 *                the simplex (an arary of Pnts)
		 * @return true iff this Pnt is inside simplex.
		 */
		public boolean isInside(Pnt[] simplex) {
			int[] result = this.relation(simplex);
			for (int r : result)
				if (r >= 0)
					return false;
			return true;
		}

		/**
		 * Test relation between this Pnt and circumcircle of a simplex.
		 * 
		 * @param simplex
		 *                the simplex (as an array of Pnts)
		 * @return -1, 0, or +1 for inside, on, or outside of
		 *         circumcircle
		 */
		public int vsCircumcircle(Pnt[] simplex) {
			Pnt[] matrix = new Pnt[simplex.length + 1];
			for (int i = 0; i < simplex.length; i++)
				matrix[i] = simplex[i].extend(1, simplex[i].dot(simplex[i]));
			matrix[simplex.length] = this.extend(1, this.dot(this));
			double d = determinant(matrix);
			int result = (d < 0) ? -1 : ((d > 0) ? +1 : 0);
			if (content(simplex) < 0)
				result = -result;
			return result;
		}

		/**
		 * Circumcenter of a simplex.
		 * 
		 * @param simplex
		 *                the simplex (as an array of Pnts)
		 * @return the circumcenter (a Pnt) of simplex
		 */

	}

	/* Pnts as matrices */

	/**
	 * Create a String for a matrix.
	 * 
	 * @param matrix
	 *                the matrix (an array of Pnts)
	 * @return a String represenation of the matrix
	 */
	public static String toString(Pnt[] matrix) {
		StringBuilder buf = new StringBuilder("{");
		for (Pnt row : matrix)
			buf.append(" " + row);
		buf.append(" }");
		return buf.toString();
	}

	/**
	 * Compute the determinant of a matrix (array of Pnts). This is not an
	 * efficient implementation, but should be adequate for low dimension.
	 * 
	 * @param matrix
	 *                the matrix as an array of Pnts
	 * @return the determinnant of the input matrix
	 * @throws IllegalArgumentException
	 *                 if dimensions are wrong
	 */
	public static double determinant(Pnt[] matrix) {
		if (matrix.length != matrix[0].dimension())
			throw new IllegalArgumentException("Matrix is not square");
		boolean[] columns = new boolean[matrix.length];
		for (int i = 0; i < matrix.length; i++)
			columns[i] = true;
		try {
			return determinant(matrix, 0, columns);
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new IllegalArgumentException("Matrix is wrong shape");
		}
	}

	/**
	 * Compute the determinant of a submatrix specified by starting row and
	 * by "active" columns.
	 * 
	 * @param matrix
	 *                the matrix as an array of Pnts
	 * @param row
	 *                the starting row
	 * @param columns
	 *                a boolean array indicating the "active" columns
	 * @return the determinant of the specified submatrix
	 * @throws ArrayIndexOutOfBoundsException
	 *                 if dimensions are wrong
	 */
	private static double determinant(Pnt[] matrix, int row, boolean[] columns) {
		if (row == matrix.length)
			return 1;
		double sum = 0;
		int sign = 1;
		for (int col = 0; col < columns.length; col++) {
			if (!columns[col])
				continue;
			columns[col] = false;
			sum += sign * matrix[row].coordinates[col] * determinant(matrix, row + 1, columns);
			columns[col] = true;
			sign = -sign;
		}
		return sum;
	}

	/**
	 * Compute generalized cross-product of the rows of a matrix. The result
	 * is a Pnt perpendicular (as a vector) to each row of the matrix. This
	 * is not an efficient implementation, but should be adequate for low
	 * dimension.
	 * 
	 * @param matrix
	 *                the matrix of Pnts (one less row than the Pnt
	 *                dimension)
	 * @return a Pnt perpendicular to each row Pnt
	 * @throws IllegalArgumentException
	 *                 if matrix is wrong shape
	 */
	public static Pnt cross(Pnt[] matrix) {
		int len = matrix.length + 1;
		if (len != matrix[0].dimension())
			throw new IllegalArgumentException("Dimension mismatch");
		boolean[] columns = new boolean[len];
		for (int i = 0; i < len; i++)
			columns[i] = true;
		double[] result = new double[len];
		int sign = 1;
		try {
			for (int i = 0; i < len; i++) {
				columns[i] = false;
				result[i] = sign * determinant(matrix, 0, columns);
				columns[i] = true;
				sign = -sign;
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new IllegalArgumentException("Matrix is wrong shape");
		}
		return new Pnt(result);
	}

	/**
	 * Determine the signed content (i.e., area or volume, etc.) of a
	 * simplex.
	 * 
	 * @param simplex
	 *                the simplex (as an array of Pnts)
	 * @return the signed content of the simplex
	 */
	public static double content(Pnt[] simplex) {
		Pnt[] matrix = new Pnt[simplex.length];
		for (int i = 0; i < matrix.length; i++)
			matrix[i] = simplex[i].extend(1);
		int fact = 1;
		for (int i = 1; i < matrix.length; i++)
			fact = fact * i;
		return determinant(matrix) / fact;
	}

	public static Pnt circumcenter(Pnt[] simplex) {
		int dim = simplex[0].dimension();
		if (simplex.length - 1 != dim)
			throw new IllegalArgumentException("Dimension mismatch");
		Pnt[] matrix = new Pnt[dim];
		for (int i = 0; i < dim; i++)
			matrix[i] = simplex[i].bisector(simplex[i + 1]);
		Pnt hCenter = cross(matrix); // Center in homogeneous
		// coordinates
		double last = hCenter.coordinates[dim];
		double[] result = new double[dim];
		for (int i = 0; i < dim; i++)
			result[i] = hCenter.coordinates[i] / last;
		return new Pnt(result);
	}

	static public class Triangle extends ArraySet<Pnt> {

		private int idNumber; // The id number
		private Pnt circumcenter = null; // The triangle's circumcenter

		private static int idGenerator = 0; // Used to create id numbers
		public static boolean moreInfo = false; // True iff more info in

		// toString

		/**
		 * @param vertices
		 *                the vertices of the Triangle.
		 * @throws IllegalArgumentException
		 *                 if there are not three distinct vertices
		 */
		public Triangle(Pnt... vertices) {
			this(Arrays.asList(vertices));
		}

		/**
		 * @param collection
		 *                a Collection holding the Simplex vertices
		 * @throws IllegalArgumentException
		 *                 if there are not three distinct vertices
		 */
		public Triangle(Collection<? extends Pnt> collection) {
			super(collection);
			idNumber = idGenerator++;
			if (this.size() != 3)
				throw new IllegalArgumentException("Triangle must have 3 vertices");
		}

		@Override
		public String toString() {
			if (!moreInfo)
				return "Triangle" + idNumber;
			return "Triangle" + idNumber + super.toString();
		}

		/**
		 * Get arbitrary vertex of this triangle, but not any of the bad
		 * vertices.
		 * 
		 * @param badVertices
		 *                one or more bad vertices
		 * @return a vertex of this triangle, but not one of the bad
		 *         vertices
		 * @throws NoSuchElementException
		 *                 if no vertex found
		 */
		public Pnt getVertexButNot(Pnt... badVertices) {
			Collection<Pnt> bad = Arrays.asList(badVertices);
			for (Pnt v : this)
				if (!bad.contains(v))
					return v;
			throw new NoSuchElementException("No vertex found");
		}

		/**
		 * True iff triangles are neighbors. Two triangles are neighbors
		 * if they share a facet.
		 * 
		 * @param triangle
		 *                the other Triangle
		 * @return true iff this Triangle is a neighbor of triangle
		 */
		public boolean isNeighbor(Triangle triangle) {
			int count = 0;
			for (Pnt vertex : this)
				if (!triangle.contains(vertex))
					count++;
			return count == 1;
		}

		/**
		 * Report the facet opposite vertex.
		 * 
		 * @param vertex
		 *                a vertex of this Triangle
		 * @return the facet opposite vertex
		 * @throws IllegalArgumentException
		 *                 if the vertex is not in triangle
		 */
		public ArraySet<Pnt> facetOpposite(Pnt vertex) {
			ArraySet<Pnt> facet = new ArraySet<Pnt>(this);
			if (!facet.remove(vertex))
				throw new IllegalArgumentException("Vertex not in triangle");
			return facet;
		}

		/**
		 * @return the triangle's circumcenter
		 */
		public Pnt getCircumcenter() {
			if (circumcenter == null)
				circumcenter = circumcenter(this.toArray(new Pnt[0]));
			return circumcenter;
		}

		/* The following two methods ensure that a Triangle is immutable */

		@Override
		public boolean add(Pnt vertex) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Iterator<Pnt> iterator() {
			return new Iterator<Pnt>() {
				private Iterator<Pnt> it = Triangle.super.iterator();

				public boolean hasNext() {
					return it.hasNext();
				}

				public Pnt next() {
					return it.next();
				}

				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		}

		/*
		 * The following two methods ensure that all triangles are
		 * different.
		 */

		@Override
		public int hashCode() {
			return (int) (idNumber ^ (idNumber >>> 32));
		}

		@Override
		public boolean equals(Object o) {
			return (this == o);
		}

	}

	public class Triangulation extends AbstractSet<Triangle> {

		private Triangle mostRecent = null; // Most recently "active"
		// triangle
		private Graph<Triangle> triGraph; // Holds triangles for
		private Set<Triangle> lastCavity;

		// navigation

		/**
		 * All sites must fall within the initial triangle.
		 * 
		 * @param triangle
		 *                the initial triangle
		 */
		public Triangulation(Triangle triangle) {
			triGraph = new Graph<Triangle>();
			triGraph.add(triangle);
			mostRecent = triangle;
		}

		/* The following two methods are required by AbstractSet */

		@Override
		public Iterator<Triangle> iterator() {
			return triGraph.nodeSet().iterator();
		}

		@Override
		public int size() {
			return triGraph.nodeSet().size();
		}

		@Override
		public String toString() {
			return "Triangulation with " + size() + " triangles";
		}

		/**
		 * True iff triangle is a member of this triangulation. This
		 * method isn't required by AbstractSet, but it improves
		 * efficiency.
		 * 
		 * @param triangle
		 *                the object to check for membership
		 */
		public boolean contains(Object triangle) {
			return triGraph.nodeSet().contains(triangle);
		}

		/**
		 * Report neighbor opposite the given vertex of triangle.
		 * 
		 * @param site
		 *                a vertex of triangle
		 * @param triangle
		 *                we want the neighbor of this triangle
		 * @return the neighbor opposite site in triangle; null if none
		 * @throws IllegalArgumentException
		 *                 if site is not in this triangle
		 */
		public Triangle neighborOpposite(Pnt site, Triangle triangle) {
			if (!triangle.contains(site))
				throw new IllegalArgumentException("Bad vertex; not in triangle");
			for (Triangle neighbor : triGraph.neighbors(triangle)) {
				if (!neighbor.contains(site))
					return neighbor;
			}
			return null;
		}

		/**
		 * Return the set of triangles adjacent to triangle.
		 * 
		 * @param triangle
		 *                the triangle to check
		 * @return the neighbors of triangle
		 */
		public Set<Triangle> neighbors(Triangle triangle) {
			return triGraph.neighbors(triangle);
		}

		/**
		 * Report triangles surrounding site in order (cw or ccw).
		 * 
		 * @param site
		 *                we want the surrounding triangles for this
		 *                site
		 * @param triangle
		 *                a "starting" triangle that has site as a
		 *                vertex
		 * @return all triangles surrounding site in order (cw or ccw)
		 * @throws IllegalArgumentException
		 *                 if site is not in triangle
		 */
		public List<Triangle> surroundingTriangles(Pnt site, Triangle triangle) {
			if (!triangle.contains(site))
				throw new IllegalArgumentException("Site not in triangle");
			List<Triangle> list = new ArrayList<Triangle>();
			Triangle start = triangle;
			Pnt guide = triangle.getVertexButNot(site); // Affects
			// cw or
			// ccw
			while (true) {
				list.add(triangle);
				Triangle previous = triangle;

				System.out.println(" site :" + site + " " + guide + " " + triangle);

				if (triangle==null) break;
				
				triangle = this.neighborOpposite(guide, triangle); // Next
				// triangle
				guide = previous.getVertexButNot(site, guide); // Update
				// guide
				if (triangle == start)
					break;
			}
			return list;
		}

		/**
		 * Locate the triangle with point inside it or on its boundary.
		 * 
		 * @param point
		 *                the point to locate
		 * @return the triangle that holds point; null if no such
		 *         triangle
		 */
		public Triangle locate(Pnt point) {
			Triangle triangle = mostRecent;
			if (!this.contains(triangle))
				triangle = null;

			// Try a directed walk (this works fine in 2D, but can
			// fail in 3D)
			Set<Triangle> visited = new HashSet<Triangle>();
			while (triangle != null) {
				if (visited.contains(triangle)) { // This should
					// never
					// happen
					System.out.println("Warning: Caught in a locate loop");
					break;
				}
				visited.add(triangle);
				// Corner opposite point
				Pnt corner = point.isOutside(triangle.toArray(new Pnt[0]));
				if (corner == null)
					return triangle;
				triangle = this.neighborOpposite(corner, triangle);
			}
			// No luck; try brute force
			System.out.println("Warning: Checking all triangles for " + point);
			for (Triangle tri : this) {
				if (point.isOutside(tri.toArray(new Pnt[0])) == null)
					return tri;
			}
			// No such triangle
			System.out.println("Warning: No triangle holds " + point);
			return null;
		}

		/**
		 * Place a new site into the DT. Nothing happens if the site
		 * matches an existing DT vertex.
		 * 
		 * @param site
		 *                the new Pnt
		 * @throws IllegalArgumentException
		 *                 if site does not lie in any triangle
		 */
		public void delaunayPlace(Pnt site) {
			// Uses straightforward scheme rather than best
			// asymptotic time

			// Locate containing triangle
			Triangle triangle = locate(site);
			// Give up if no containing triangle or if site is
			// already in DT
			if (triangle == null)
				throw new IllegalArgumentException("No containing triangle");
			if (triangle.contains(site))
				return;

			// Determine the cavity and update the triangulation
			Set<Triangle> cavity = getCavity(site, triangle);

			lastCavity = cavity;

			mostRecent = update(site, cavity);
		}

		/**
		 * Determine the cavity caused by site.
		 * 
		 * @param site
		 *                the site causing the cavity
		 * @param triangle
		 *                the triangle containing site
		 * @return set of all triangles that have site in their
		 *         circumcircle
		 */
		private Set<Triangle> getCavity(Pnt site, Triangle triangle) {
			Set<Triangle> encroached = new HashSet<Triangle>();
			Queue<Triangle> toBeChecked = new LinkedList<Triangle>();
			Set<Triangle> marked = new HashSet<Triangle>();
			toBeChecked.add(triangle);
			marked.add(triangle);
			while (!toBeChecked.isEmpty()) {
				triangle = toBeChecked.remove();
				if (site.vsCircumcircle(triangle.toArray(new Pnt[0])) == 1)
					continue; // Site outside triangle =>
				// triangle not in
				// cavity
				encroached.add(triangle);
				// Check the neighbors
				for (Triangle neighbor : triGraph.neighbors(triangle)) {
					if (marked.contains(neighbor))
						continue;
					marked.add(neighbor);
					toBeChecked.add(neighbor);
				}
			}
			return encroached;
		}

		/**
		 * Update the triangulation by removing the cavity triangles and
		 * then filling the cavity with new triangles.
		 * 
		 * @param site
		 *                the site that created the cavity
		 * @param cavity
		 *                the triangles with site in their circumcircle
		 * @return one of the new triangles
		 */
		private Triangle update(Pnt site, Set<Triangle> cavity) {
			Set<Set<Pnt>> boundary = new HashSet<Set<Pnt>>();
			Set<Triangle> theTriangles = new HashSet<Triangle>();

			// Find boundary facets and adjacent triangles
			for (Triangle triangle : cavity) {
				theTriangles.addAll(neighbors(triangle));
				for (Pnt vertex : triangle) {
					Set<Pnt> facet = triangle.facetOpposite(vertex);
					if (boundary.contains(facet))
						boundary.remove(facet);
					else
						boundary.add(facet);
				}
			}
			theTriangles.removeAll(cavity); // Adj triangles only

			// Remove the cavity triangles from the triangulation
			for (Triangle triangle : cavity)
				triGraph.remove(triangle);

			// Build each new triangle and add it to the
			// triangulation
			Set<Triangle> newTriangles = new HashSet<Triangle>();
			for (Set<Pnt> vertices : boundary) {
				vertices.add(site);
				Triangle tri = new Triangle(vertices);
				triGraph.add(tri);
				newTriangles.add(tri);
			}

			// Update the graph links for each new triangle
			theTriangles.addAll(newTriangles); // Adj triangle + new
			// triangles
			for (Triangle triangle : newTriangles)
				for (Triangle other : theTriangles)
					if (triangle.isNeighbor(other))
						triGraph.add(triangle, other);

			// Return one of the new triangles
			return newTriangles.iterator().next();
		}

	}
}
