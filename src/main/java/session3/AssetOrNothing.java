package session3;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationModel;
import net.finmath.montecarlo.assetderivativevaluation.products.AbstractAssetMonteCarloProduct;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;

/**
 * This class extends AbstractAssetMonteCarloProduct, and represents an "asset
 * or nothing" option, i.e., an option on an underlying S with
 * payoff at maturity S(T) 1_{S(T)>K}. Note that we only have to implement
 * the constructor and the method getValue(final double evaluationTime, final
 * AssetModelMonteCarloSimulationModel model).
 */
public class AssetOrNothing extends AbstractAssetMonteCarloProduct {

	private final double maturity;
	private final double strike;
	private final int underlyingIndex; // it is useful in the case of a multi-dimensional process. Otherwise, it is 0
	private final String nameOfUnderliyng;

	/**
	 * Construct a product representing an "asset or nothing" option on an asset S
	 * (where S is the asset with index 0 from the model).
	 *
	 * @param maturity        The maturity T in the option payoff S(T) 1_{S(T)>K}
	 * @param strike          The strike K in the option payoff S(T) 1_{S(T)>K}.
	 * @param underlyingIndex The index of the underlying to be fetched from the
	 *                        model.
	 * @param underlyingName  Name of the underlying
	 */
	public AssetOrNothing(final double maturity, final double strike, final int underlyingIndex,
			final String nameOfUnderliyng) {
		this.maturity = maturity;
		this.strike = strike;
		this.underlyingIndex = underlyingIndex;
		this.nameOfUnderliyng = nameOfUnderliyng;
	}

	/**
	 * Construct a product representing an "asset or nothing" option on an asset S
	 * (where S is the asset with index 0 from the model).
	 *
	 * @param maturity       The maturity T in the option payoff S(T) 1_{S(T)>K}
	 * @param strike         The strike K in the option payoff S(T) 1_{S(T)>K}.
	 * @param underlyingName Name of the underlying
	 */
	public AssetOrNothing(final double maturity, final double strike, final String nameOfUnderliyng) {
		this(maturity, strike, 0, nameOfUnderliyng);
	}

	/**
	 * Construct a product representing an "asset or nothing" option on an asset S
	 * (where S the asset with index underlyingIndex from the model).
	 *
	 * @param maturity        The maturity T in the option payoff S(T) 1_{S(T)>K}
	 * @param strike          The strike K in the option payoff S(T) 1_{S(T)>K}.
	 * @param underlyingIndex The index of the underlying to be fetched from the
	 *                        model.
	 */
	public AssetOrNothing(final double maturity, final double strike, final int underlyingIndex) {
		this(maturity, strike, underlyingIndex, null);
	}

	/**
	 * Construct a product representing an "asset or nothing" option on an asset S
	 * (where S the asset with index 0 from the model).
	 *
	 * @param maturity The maturity T in the option payoff S(T) 1_{S(T)>K}
	 * @param strike   The strike K in the option payoff S(T) 1_{S(T)>K}.
	 */
	public AssetOrNothing(final double maturity, final double strike) {
		this(maturity, strike, 0, null);
	}

	/**
	 * This method returns the value random variable of the product within the
	 * specified model, evaluated at a given evalutationTime. In this case, the
	 * product is an "asset or nothing" option, whose payoff is S(T) 1_{S(T)>K}.
	 * Here S is the asset with index underlyingIndex from the model.
	 *
	 * @param evaluationTime The time on which this products value should be
	 *                       observed.
	 * @param model          The model used to price the product. It gives the
	 *                       underlying of the option.
	 * @return The random variable representing the value of the product discounted
	 *         to evaluation time
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation
	 *                                                    fails, specific cause may
	 *                                                    be available via the
	 *                                                    <code>cause()</code>
	 *                                                    method.
	 */
	@Override
	public RandomVariable getValue(final double evaluationTime, final AssetModelMonteCarloSimulationModel model)
			throws CalculationException {

		// Get S(T)
		final RandomVariable underlyingAtMaturity = model.getAssetValue(maturity, underlyingIndex);

		RandomVariable zeroAsRandomVariable = new Scalar(0.0);
		/*
		 * The payoff. Look at the application of the method choose. Note here that the
		 * second argument must be of type RandomVariable. S_T 1_{S_T-K>=0}
		 */
		RandomVariable values = (underlyingAtMaturity.sub(strike)).choose(underlyingAtMaturity, zeroAsRandomVariable);

		// or:
		// final DoubleUnaryOperator payoffFunction = (x) -> (x - strike >= 0 ? x : 0);
		// RandomVariable values = underlyingAtMaturity.apply(payoffFunction);

		// Discounting...
		final RandomVariable numeraireAtMaturity = model.getNumeraire(maturity);
		final RandomVariable monteCarloWeights = model.getMonteCarloWeights(maturity);
		values = values.div(numeraireAtMaturity).mult(monteCarloWeights);

		// ...to evaluation time.
		final RandomVariable numeraireAtEvalTime = model.getNumeraire(evaluationTime);
		final RandomVariable monteCarloWeightsAtEvalTime = model.getMonteCarloWeights(evaluationTime);
		values = values.mult(numeraireAtEvalTime).div(monteCarloWeightsAtEvalTime);

		return values;
	}

	
	public double getMaturity() {
		return maturity;
	}

	public double getStrike() {
		return strike;
	}

	public Integer getUnderlyingIndex() {
		return underlyingIndex;
	}

	public String getNameOfUnderliyng() {
		return nameOfUnderliyng;
	}
}