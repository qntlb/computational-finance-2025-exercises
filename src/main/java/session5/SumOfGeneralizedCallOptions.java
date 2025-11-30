package session5;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationModel;
import net.finmath.montecarlo.assetderivativevaluation.products.AbstractAssetMonteCarloProduct;
import net.finmath.stochastic.RandomVariable;

/**
 * This class represents the sum of two call options, written on two (possibly
 * correlated) underlyings. It involves therefore a multi-dimensional process.
 * The options can have different maturities and different strikes. The
 * payoffs of the options might be multiplied by a constant. This class is
 * useful to compute the Monte-Carlo value of a cap involving two caplets.
 */
public class SumOfGeneralizedCallOptions extends AbstractAssetMonteCarloProduct {

	private final double firstMaturity;
	private final double secondMaturity;

	private final double firstStrike; // K_1
	private final double secondStrike; // K_2

	/*
	 * In the case of a cap, the multiplicative factors are
	 * are (T_2-T_1)P(T_2;0) and (T_3-T_2)P(T_3;0), respectively. For standard call options, these
	 * constants are instead equal to one.
	 */
	private final double firstMultiplier;
	private final double secondMultiplier;

	// typically they are 0 and 1
	private final int firstAssetIndex;
	private final int secondAssetIndex;

	/**
	 * It constructs a product representing the sum of two call options, with
	 * possibly different strikes and different maturities, multiplied by two
	 * constants.
	 *
	 * @param firstMaturity,    the maturity of the first call option
	 * @param secondMaturity,   the maturity of the second call option
	 * @param firstStrike,      the strike of the first call option
	 * @param secondStrike,     the strike of the second call option
	 * @param firstMultiplier,  multiplicative factor in the payoff of the
	 *                          first call option
	 * @param secondMultiplier, multiplicative factor in the payoff of the
	 *                          second call option
	 * @param firstAssetIndex,  the index identifying the first underlying
	 * @param secondAssetIndex, the index identifying the second underlying
	 */
	public SumOfGeneralizedCallOptions(double firstMaturity, double secondMaturity, double firstStrike, double secondStrike,
			double firstMultiplier, double secondMultiplier, int firstAssetIndex, int secondAssetIndex) {
		this.firstMaturity = firstMaturity;
		this.secondMaturity = secondMaturity;
		this.firstStrike = firstStrike;
		this.secondStrike = secondStrike;
		this.firstMultiplier = firstMultiplier;
		this.secondMultiplier = secondMultiplier;
		this.firstAssetIndex = firstAssetIndex;
		this.secondAssetIndex = secondAssetIndex;
	}

	/**
	 * It constructs a product representing the sum of two call options, with
	 * possibly different strikes and different maturities, multiplied by two
	 * constants. The first asset index and the second asset index are 0 and 1,
	 * respectively.
	 *
	 * @param firstMaturity,    the maturity of the first call option
	 * @param secondMaturity,   the maturity of the second call option
	 * @param firstStrike,      the strike of the first call option
	 * @param secondStrike,     the strike of the second call option
	 * @param firstMultiplier,  multiplicative factor in the payoff of the
	 *                          first call option
	 * @param secondMultiplier, multiplicative factor in the payoff of the
	 *                          second call option
	 */
	public SumOfGeneralizedCallOptions(double firstMaturity, double secondMaturity, double firstStrike, double secondStrike,
			double firstMultiplier, double secondMultiplier) {
		// note the use of this
		this(firstMaturity, secondMaturity, firstStrike, secondStrike, firstMultiplier, secondMultiplier, 0, 1);
	}
	
	
	/**
	 * It constructs a product representing the sum of two call options, with
	 * possibly different strikes and different maturities.
	 *
	 * @param firstMaturity,    the maturity of the first call option
	 * @param secondMaturity,   the maturity of the second call option
	 * @param firstStrike,      the strike of the first call option
	 * @param secondStrike,     the strike of the second call option
	 * @param firstAssetIndex,  the index identifying the first underlying
	 * @param secondAssetIndex, the index identifying the second underlying
	 */
	public SumOfGeneralizedCallOptions(double firstMaturity, double secondMaturity, double firstStrike, double secondStrike,
			int firstAssetIndex, int secondAssetIndex) {
		this(firstMaturity, secondMaturity, firstStrike, secondStrike, 1.0, 1.0, firstAssetIndex, secondAssetIndex);
	}
	
	/**
	 * It constructs a product representing the sum of two call options, with
	 * possibly different strikes and different maturities. The first asset
	 * index and the second asset index are 0 and 1, respectively.
	 *
	 * @param firstMaturity,    the maturity of the first call option
	 * @param secondMaturity,   the maturity of the second call option
	 * @param firstStrike,      the strike of the first call option
	 * @param secondStrike,     the strike of the second call option
	 */
	public SumOfGeneralizedCallOptions(double firstMaturity, double secondMaturity, double firstStrike, double secondStrike) {
		this(firstMaturity, secondMaturity, firstStrike, secondStrike, 1.0, 1.0, 0, 1);
	}

	@Override
	public RandomVariable getValue(double evaluationTime, AssetModelMonteCarloSimulationModel model)
			throws CalculationException {
		// We get L^1(T_1)
		final RandomVariable firstAssetAtMaturity = model.getAssetValue(firstMaturity, firstAssetIndex);

		// Payoff of the first call option
		RandomVariable firstValues = firstAssetAtMaturity.sub(firstStrike).floor(0.0);
		
		// Discounting...
		final RandomVariable numeraireAtFirstMaturity = model.getNumeraire(firstMaturity);
		firstValues = firstValues.div(numeraireAtFirstMaturity);

		// ...to evaluation time: this does not have effect for our cap
		final RandomVariable numeraireAtEvalTime = model.getNumeraire(evaluationTime);
		firstValues = firstValues.mult(numeraireAtEvalTime);

		/*
		 * We multiply the payoff of the second option by a constant (which is equal to
		 * (T_2-T_1)P(T_2;0) for our cap)
		 */
		firstValues = firstValues.mult(firstMultiplier);

		// We get L^2(T_2)
		final RandomVariable secondAssetAtMaturity = model.getAssetValue(secondMaturity, secondAssetIndex);

		// Payoff of the second call option
		RandomVariable secondValues = secondAssetAtMaturity.sub(secondStrike).floor(0.0);
		
		// Discounting...
		final RandomVariable numeraireAtSecondMaturity = model.getNumeraire(secondMaturity);
		secondValues = secondValues.div(numeraireAtSecondMaturity);

		// ...to evaluation time: this does not have effect for a cap
		secondValues = secondValues.mult(numeraireAtEvalTime);

		/*
		 * We multiply the payoff of the second option by a constant (which is equal to
		 * (T_3-T_2)P(T_3;0) for our cap)
		 */
		secondValues = secondValues.mult(secondMultiplier);

		return firstValues.add(secondValues);
	}

}