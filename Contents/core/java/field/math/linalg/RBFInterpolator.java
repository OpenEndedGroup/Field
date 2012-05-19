package field.math.linalg;

/**
 * A structure for solving the multivariate interpolation problem using RBFs.
 * This assumes a linear combination of RBFs augmented by a simple first order
 * linear fit.
 *
 * @author Michael Patrick Johnson <aries@media.mit.edu>
 */

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import field.math.linalg.MatrixN.DimensionMismatchException;
import field.math.linalg.SVD.SVDException;


public class RBFInterpolator implements Serializable {

	/** this is an inner class for storing examples */
	public static class Example implements Serializable {

		static final long serialVersionUID = -7873495106647562179L;

		private final VectorN x;

		private final VectorN y;

		private final VectorN d;

		/**
		 * this constructor copies storage of x
		 */
		public Example(VectorN x, VectorN y) {
			this.x = x.copy();
			this.y = y.copy();
			this.d = new VectorN(x.dim());
		}

		public double distanceFromExample(VectorN new_x) {
			VectorN.sub(new_x, x, d);
			return d.mag();
		}

		public VectorN getInput() {
			return x;
		}

		public VectorN getOutput() {
			return y;
		}
	}

	static final long serialVersionUID = -4958373802920387817L;

	private List<Example> examples = new ArrayList<Example>();

	private final List<VectorN> rbf_weights;

	private final List<VectorN> linear_weights;

	private final int input_dimension;

	private final int output_dimension;

	private final RadialBasisFunction rbf;

	/**
	 * Constructor. All data is copied to internal representations.
	 *
	 * @param input_dimension
	 *                        the number of dimensions in the domain.
	 * @param output_dimension
	 *                        the number of dimensions in the range.
	 * @param inputs
	 *                        the domain points to interpolate between, given as a List of Vecs.
	 * @param outputs
	 *                        the function values corresponding to the points in <code>inputs</code>. Given as a List of Vecs. Must contain the same number of Vecs as <code>inputs</code>.
	 */
	public RBFInterpolator(int input_dimension, int output_dimension, List<VectorN> inputs, List<VectorN> outputs, RadialBasisFunction rbf) {
		int n = inputs.size();
		examples = new ArrayList<Example>(n);
		int i;
		for (i = 0; i < n; i++) {
			examples.add(new Example(inputs.get(i), outputs.get(i)));
		}

		this.input_dimension = input_dimension;
		this.output_dimension = output_dimension;
		rbf_weights = new ArrayList<VectorN>(outputDimension());
		for (i = 0; i < outputDimension(); i++)
			rbf_weights.add(new VectorN(numExamples()));
		linear_weights = new ArrayList<VectorN>(outputDimension());
		for (i = 0; i < outputDimension(); i++)
			linear_weights.add(new VectorN(inputDimension() + 1));
		this.rbf = rbf;
		findWeights();
	}

	/** return the ith example */
	public Example getExample(int i) {
		return examples.get(i);
	}

	/**
	 * Returns the Jacobian matrix of the function, evaluated at point <code>in</code>.
	 *
	 * @param in
	 *                        The domain point to evaluate the gradient at.
	 * @return A matrix of the form:
	 *         <P>
	 *         <code>
	 ( dy1/dx1,  dy1/dx2,  ...,  dyN/dx1 )
	 ( ...                               )
	 ( dyN/dx1,  dyN/dx2,  ...,  dyN/dxM )</code>
	 */
	public MatrixN gradientAt(VectorN in) {

		MatrixN m = new MatrixN(input_dimension, output_dimension);
		VectorN total = new VectorN(input_dimension);
		VectorN temp = new VectorN(input_dimension);
		for (int c = 0; c < output_dimension; c++) {

			// we
			// need
			// to
			// calculate
			// a
			// gradient
			// function...
			VectorN w = getLinearWeights(c);
			for (int d = 0; d < input_dimension; d++) {
				total.set(d, w.get(d + 1));
			}

			// add in
			// examples...
			double mag;
			w = getRadialWeights(c);
			for (int n = 0; n < examples.size(); n++) {
				Example exn = examples.get(n);
				VectorN pos = exn.getInput();
				VectorN.sub(pos, in, temp);
				mag = temp.mag();
				temp.normalize();
				temp.scale(w.get(n) * rbf.derivativeAt(mag)); // this
				// also
				// needs
				// to
				// be
				// scaled
				// by
				// weight.
				VectorN.add(temp, total, total);
			}

			for (int d = 0; d < input_dimension; d++) {
				m.set(d, c, total.get(d));
			}
		}
		return m;
	}

	/**
	 * Returns the number of dimensions in the domain.
	 */
	final public int inputDimension() {
		return input_dimension;
	}

	/**
	 * Evaluates the function at <code>x</code>, by interpolating between stored examples. Stores the result in <code>y</code>.
	 *
	 * @param x
	 *                        (Input) the domain point to evaluate the function at.
	 * @param y
	 *                        (Output) the approximate function value at <code>x</code>.
	 */
	public void interpolate(VectorN x, VectorN y) {
		for (int j = 0; j < outputDimension(); j++)
			
			y.set(j, interpolateLinear(x, j) + interpolateRadial(x, j));
	}

	/**
	 * Evaluates a particular dimension of the function at point <code>x</code>.
	 *
	 * @param x
	 *                        The point to evaluate the function at.
	 * @param component
	 *                        The dimension of the function value to return.
	 * @return The <code>component</code> -th dimension of <code>f(x)</code>
	 */
	public double interpolateComponent(VectorN x, int component) {
		return interpolateLinear(x, component) + interpolateRadial(x, component);
	}

	public double interpolateLinear(VectorN x, int j) {
		VectorN w = getLinearWeights(j);
		double sum = 0.0;
		for (int i = 0; i < inputDimension() + 1; i++)
			sum += w.get(i) * linearBasis(x, i);
		return sum;
	}

	/* private */

	public double interpolateRadial(VectorN x, int j) {
		VectorN w = getRadialWeights(j);
		double sum = 0.0;
		for (int i = 0; i < numExamples(); i++)
			sum += w.get(i) * rbf.evaluate(getExample(i).distanceFromExample(x));
		return sum;
	}

	/**
	 * Returns the number of interpolation samples stored.
	 */
	final public int numExamples() {
		return examples.size();
	}

	/**
	 * Returns the number of dimensions in the range.
	 */
	final public int outputDimension() {
		return output_dimension;
	}

	public void resolveRadial() {

		MatrixN a = makeLinearDesignMatrixN();

		List<VectorN> residuals = new ArrayList<VectorN>(outputDimension());
		for (int j = 0; j < outputDimension(); j++) {
			VectorN b = new VectorN(numExamples());
			for (int i = 0; i < numExamples(); i++)
				b.set(i, getOutputs(i).get(j));
			VectorN d = linear_weights.get(j);

			VectorN residual = new VectorN(b);
			MatrixN.mult(a, d, residual);
			VectorN.sub(b, residual, residual);
			residuals.add(residual);
		}

		solveRadialPortion(rbf_weights, residuals);
	}

	private void findWeights() {
		List<VectorN> residuals = solveLinearPortion(linear_weights);
		solveRadialPortion(rbf_weights, residuals);
	}

	private VectorN getInputs(int i) {
		return getExample(i).getInput();
	}

	private VectorN getLinearWeights(int output) {
		return linear_weights.get(output);
	}

	private VectorN getOutputs(int i) {
		return getExample(i).getOutput();
	}

	private VectorN getRadialWeights(int output) {
		return rbf_weights.get(output);
	}

	/**
	 * this is the linear part. The ith basis is the i+1th component of the input x, with i = 0 being the constant factor 1.0
	 */
	private double linearBasis(VectorN x, int i) {
		if (i == 0)
			return 1.0;
		else
			return x.get(i - 1);
	}

	private MatrixN makeLinearDesignMatrixN() {
		int n = numExamples();
		int m = inputDimension() + 1;
		MatrixN a = new MatrixN(n, m);
		int i, j;
		for (i = 0; i < n; i++)
			for (j = 0; j < m; j++)
				a.set(i, j, linearBasis(getInputs(i), j));
		return a;
	}

	private MatrixN makeRadialDesignMatrixN() {
		int i, j;
		int n = numExamples();
		MatrixN a = new MatrixN(n, n);
		for (i = 0; i < n; i++)
			for (j = 0; j < n; j++)
				a.set(i, j, rbf.evaluate(getExample(i).distanceFromExample(getInputs(j))));

		return a;
	}

	/**
	 * fill in the d Lists with the linear weights solved for with a least squares fit on the linear basis. Returns the residual Ld - b of the fit for each output.
	 */
	private List<VectorN> solveLinearPortion(List<VectorN> weights) {

		MatrixN a = makeLinearDesignMatrixN();
		SVD svd = null;
		try {
			svd = new SVD(a);
		} catch (SVDException e) {
			// Debug.doAssert(false, "solveLinearPortion: SVDException: " + e);
			e.printStackTrace();
			assert false;
		} catch (DimensionMismatchException e) {
			// Debug.doAssert(false, "solveLinearPortion: bas dimensions. " + e);
			e.printStackTrace();
			assert false;
		}

		List<VectorN> residuals = new ArrayList<VectorN>(outputDimension());
		for (int j = 0; j < outputDimension(); j++) {
			VectorN b = new VectorN(numExamples());
			for (int i = 0; i < numExamples(); i++)
				b.set(i, getOutputs(i).get(j));
			VectorN d = weights.get(j);
			svd.solve(b, d);

			VectorN residual = new VectorN(b);
			MatrixN.mult(a, d, residual);
			VectorN.sub(b, residual, residual);
			residuals.add(residual);
		}
		return residuals;
	}

	/*
	 * Ayyayay. Return a jacobian matrix? This will be of dimensions output_dim X input_dim interpretable as a gradient for each dimension (one in each column)
	 *
	 * Say you were interpolating in n dimensions on an x,y input space. It would be ( x1 x2 ... xn ) ( y1 y2 ... yn )
	 *
	 *
	 */

	/** solve the radial weights for the residual r. */
	private void solveRadialPortion(List<VectorN> weights, List<VectorN> residuals) {
		SVD svd = null;
		try {
			MatrixN a = makeRadialDesignMatrixN();
			svd = new SVD(a);
		} catch (Exception e) {
			e.printStackTrace();
			assert false;
		}

		for (int j = 0; j < outputDimension(); j++) {
			VectorN r = residuals.get(j);
			VectorN c = weights.get(j);
			svd.solve(r, c);
		}
	}

}