package field.math.linalg;

/**
 * A very simple interface that defines a radial basis function.
 * 
 * @author Michael Patrick Johnson <aries@media.mit.edu>
 *         <p>
 *         Added derivative call.
 * @author naimad
 */
public interface RadialBasisFunction {
	/**
	 * Given an r in [0, +Inf], returns the value of this RBF.
	 */
	public double evaluate(double r);

	/**
	 * Sets the characteristic width.
	 */
	public void setCharacteristicWidth(double alpha);

	/**
	 * Returns the characrteristic width.
	 */
	public double getCharacteristicWidth();

	/**
	 * Returns the magnitude of the gradient at the specified
	 * radius.
	 */
	public double derivativeAt(double r);
}
