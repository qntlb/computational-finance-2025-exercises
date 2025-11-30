package session5;

import session4.InterestRatesProducts;

import net.finmath.exception.CalculationException;
import net.finmath.functions.AnalyticFormulas;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.BrownianMotionFromMersenneRandomNumbers;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationModel;
import net.finmath.montecarlo.assetderivativevaluation.MonteCarloMultiAssetBlackScholesModel;
import net.finmath.montecarlo.assetderivativevaluation.products.AbstractAssetMonteCarloProduct;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

public class InterestRatesProductsEnhanced {
	
	
	
	/**
	 * This method calculates and return the value of a cap involving two caplets
	 * under the Black model for the two Libors involved, using a Monte Carlo
	 * method.
	 *
	 * @param initialFirstLibor,                  i.e. L^1_0 = L(T_1,T_2;0)
	 * @param initialSecondLibor,                 i.e. L^2_0 = L(T_2,T_3;0)
	 * @param firstLiborVolatility,               the volatility of the first LIBOR
	 *                                            process under the Black model
	 * @param secondLiborVolatility               the volatility of the second LIBOR
	 *                                            process under the Black model
	 * @param correlation,                        the correlation between the two
	 *                                            Libors
	 * @param firstStrike,                        the strike of the first caplet
	 * @param secondStrike,                       the strike of the second caplet
	 * @param firstFixingDate,                    i.e. T_1
	 * @param secondFixingDate,                   i.e. T_2, it also corresponds to the first payment date
	 * @param secondPaymentDate,                  i.e. T_3
	 * @param firstPaymentDateDiscountFactor,     i.e. P(T_2;0)
	 * @param secondPaymentDateDiscountFactor,    i.e. P(T_3;0)
	 * @param notional
	 * @param numberOfTimeStepsForDiscretization, the number of steps for the time
	 *                                            discretization we want to fix
	 * @param numberOfSimulations,                the number of simulations we want
	 *                                            to use for the Monte-Carlo
	 *                                            approximation of the price
	 * @throws CalculationException
	 */
	public static double calculateCapValueBlackModel(double initialFirstLibor, double initialSecondLibor,
			double firstLiborVolatility, double secondLiborVolatility, double correlation, double firstStrike,
			double secondStrike, double firstFixingDate, double secondFixingDate, double secondPaymentDate,
			double firstPaymentDateDiscountFactor, double secondPaymentDateDiscountFactor, double notional,
			int numberOfTimeStepsForDiscretization, int numberOfSimulations) throws CalculationException {

		/*
		 * We first get the size of the time steps of the time discretization. Note that
		 * we take it until the second fixing date (even if the first process stops
		 * before)
		 */
		final double timeStep = secondFixingDate / numberOfTimeStepsForDiscretization;

		// we the create the time discretization
		final TimeDiscretization times = new TimeDiscretizationFromArray(0.0, numberOfTimeStepsForDiscretization,
				timeStep);

		/*
		 * And we create a two-dimensional Brownian motion: note that the components
		 * here are independent!
		 */
		final BrownianMotion twoDimBrownianMotion = new BrownianMotionFromMersenneRandomNumbers(times, 2,
				numberOfSimulations, 29);// (B^1,B^2), independent
		// we will then define W^1 := B^1, W^2 := \rho B^1 + \sqrt(1-\rho^2) B^2 

		/*
		 * This is the correlation matrix we want to have for the correlated Brownian
		 * motions: 1.0 represents the correlation of W^1 with itself and of W^2
		 * with itself. Instead, rho is the correlation of W^1 and W^2: so, in the end we have 
		 * W^1 = B^1, W^2 = rho B^1 + sqrt(1-rho^2) B_2.
		 * We will give this matrix to the constructor of MonteCarloMultiAssetBlackScholesModel
		 */
		final double[][] correlationMatrix = { { 1.0, correlation }, { correlation, 1.0 } };

		double[] initialLibors = { initialFirstLibor, initialSecondLibor };
		double[] volatilities = { firstLiborVolatility, secondLiborVolatility };

		/*
		 * Look at this class of the Finmath library: it allows you to simulate n
		 * processes, possibly correlated, all following log-normal dynamics.
		 */
		final AssetModelMonteCarloSimulationModel simulationTwoDimGeometricBrownian =
				new MonteCarloMultiAssetBlackScholesModel(
						twoDimBrownianMotion,
						initialLibors,
						0,
						volatilities,
						correlationMatrix);

		final double firstPeriodLength = secondFixingDate - firstFixingDate;//T_2-T_1
		final double secondPeriodLength = secondPaymentDate - secondFixingDate;//T_3-T_2

		// the constants we give to SumOfCallOptions
		final double firstMultiplier = firstPaymentDateDiscountFactor * firstPeriodLength; // P(T_2;0) (T_2 - T_1)
		final double secondMultiplier = secondPaymentDateDiscountFactor * secondPeriodLength; // P(T_3;0) (T_3 - T_2)

		AbstractAssetMonteCarloProduct sumOfCallOptionsCalculator = new SumOfGeneralizedCallOptions(
				firstFixingDate,
				secondFixingDate,
				firstStrike,
				secondStrike,
				firstMultiplier,
				secondMultiplier);

		return notional * sumOfCallOptionsCalculator.getValue(simulationTwoDimGeometricBrownian);
	}
	
	
	
	/**
	 * Computes the value of the convexity adjustment for a Caplet paying in arrears under the Black model.
	 *
	 * @param initialForwardLibor,       i.e. L_0 = L(T_1,T_2;0)
	 * @param liborVolatility,           the volatility of the LIBOR process
	 * @param strike,                    the strike of the option
	 * @param fixingDate,                i.e. T_1
	 * @param payingDate,                i.e. T_2
	 * @param paymentDateDiscountFactor, i.e. P(0;T_2)
	 * @param notional,                  i.e. N
	 */
	public static double convexityAdjustmentCapletInArrears(double initialForwardLibor, double liborVolatility,
			double strike, double fixingDate, double paymentDate, double paymentDateDiscountFactor, double notional) {
		
		double periodLength = paymentDate - fixingDate;
		
		return notional * paymentDateDiscountFactor * periodLength * periodLength * initialForwardLibor
				* AnalyticFormulas.blackScholesOptionValue(
						initialForwardLibor * Math.exp(liborVolatility * liborVolatility * fixingDate), 0,
						liborVolatility, fixingDate, strike);
	}

	/**
	 * It calculates the value of a Caplet payed in arrears under the Black model.
	 *
	 * @param initialForwardLibor,       i.e. L_0 = L(T_1,T_2;0)
	 * @param liborVolatility,           the volatility of the LIBOR process
	 * @param strike,                    the strike of the option
	 * @param fixingDate,                i.e. T_1
	 * @param paymentDate,               i.e. T_2
	 * @param paymentDateDiscountFactor, i.e. P(0;T_2)
	 * @param notional,                  i.e. N
	 */
	public static double calculateCapletInArrearsBlack(double initialForwardLibor, double liborVolatility,
			double strike, double fixingDate, double paymentDate, double paymentDateDiscountFactor, double notional) {
		
		return InterestRatesProducts.calculateCapletValueBlackModel(initialForwardLibor, liborVolatility, strike,
				fixingDate, paymentDate, paymentDateDiscountFactor, notional)
				+ convexityAdjustmentCapletInArrears(initialForwardLibor, liborVolatility, strike, fixingDate,
						paymentDate, paymentDateDiscountFactor, notional);
	}
}
