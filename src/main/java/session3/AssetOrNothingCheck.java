package session3;

import java.text.DecimalFormat;

import net.finmath.exception.CalculationException;
import net.finmath.functions.AnalyticFormulas;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationModel;
import net.finmath.montecarlo.assetderivativevaluation.MonteCarloBlackScholesModel;
import net.finmath.montecarlo.assetderivativevaluation.products.AbstractAssetMonteCarloProduct;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

/**
 * This class has a main method that heuristically checks the relation by which
 * the value of the delta of a call option with Black-Scholes model is equal to
 * the valuation of a portfolio holding 1/S0 asset-or-nothing options with the same
 * maturity and the same strike, written on the same underlying.
 *
 */
public class AssetOrNothingCheck {



	public static void main(String[] args) throws CalculationException {

		final DecimalFormat printNumberWithFourDecimalDigits = new DecimalFormat("0.0000");
		final DecimalFormat printPercentageWithTwoDecimalDigits = new DecimalFormat("0.00%");
		
		// option parameters
		final double strike = 100.0;
		final double maturity = 1.0;
		
		final AbstractAssetMonteCarloProduct assetOrNothingOption = new AssetOrNothing(maturity, strike);
		
		// model parameters
		final double initialPrice = 100.0;
		final double volatility = 0.25; // volatility of the underlying
		final double riskFreeRate = 0;

		// simulation parameter
		final int numberOfSimulations = 100000;// the number of paths simulated

		// time discretization parameters
		final double initialTime = 0;
		final int numberOfTimeSteps = 100;
		final double timeStep = maturity / numberOfTimeSteps;
		final TimeDiscretization times = new TimeDiscretizationFromArray(initialTime, numberOfTimeSteps, timeStep);

		/*
		 * Look at the class: it links together the model, i.e., the specification of
		 * the dynamics of the underlying, and the process, i.e., the discretization of
		 * the paths.
		 */
		final AssetModelMonteCarloSimulationModel bsModel = new MonteCarloBlackScholesModel(times, numberOfSimulations,
				initialPrice, riskFreeRate, volatility);

		final double monteCarloValueOfAssetOrNothingOption = assetOrNothingOption.getValue(bsModel);

		System.out.println("B-S Monte Carlo value: " + printNumberWithFourDecimalDigits.format(monteCarloValueOfAssetOrNothingOption));

		final double monteCarloValueOfDelta = monteCarloValueOfAssetOrNothingOption / initialPrice;

		
		// have a look at this class!
		final double analyticValueOfTheDelta = AnalyticFormulas.blackScholesOptionDelta(initialPrice, riskFreeRate,
				volatility, maturity, strike);
		
		final double absolutePercentageError = Math.abs(analyticValueOfTheDelta - monteCarloValueOfDelta)
				/ analyticValueOfTheDelta;

		
		System.out.println("\n-------------------- Delta of the Call option -----------------------	\n");
		System.out.println("B-S Monte Carlo value of the portfolio: " + printNumberWithFourDecimalDigits.format(monteCarloValueOfDelta) + "\n"
				+ "Analytical value: " + printNumberWithFourDecimalDigits.format(analyticValueOfTheDelta) + "\n"
				+ "Absolute percentage error: " + printPercentageWithTwoDecimalDigits.format(absolutePercentageError) + "\n");
	}

}
