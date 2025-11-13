package session4;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

import java.text.DecimalFormat;

import net.finmath.exception.CalculationException;
import net.finmath.functions.AnalyticFormulas;

/**
 * In this class we test the implementation of the methods computing the value
 * of a caplet (both with Monte-Carlo and using the analytic formula of a call 
 * option) and a quanto. Note that we have two methods: we run both of them when
 * running the class. 
 */
public class CapletAndQuantoTest {

	//parameters which are useful for both the methods
	final DecimalFormat printNumberWithFourDecimalDigits = new DecimalFormat("0.0000");

	
	@Test
	public void capletTest() throws CalculationException {
		
		// discount factor
		final double discountFactorAtMaturity = 0.91; // P(T_2; 0)

		// parameters for the Libor dynamics
		final double initialForwardLibor = 0.04;
		final double liborVolatility = 0.18;

		// parameters for the both the options
		final double fixingDate = 1;
		final double paymentDate = 1.5;

		final double strike = 0.05;
		final double notional = 100;
		
		// parameters for the Monte Carlo simulation
		final int numberOfTimeSteps = 50;
		final int numberOfSimulations = 100000;
				
		// tolerances for assertEqual
		final double toleranceForMonteCarlo = 2/Math.sqrt(numberOfSimulations); // this tolerance should account for Monte Carlo error
		
		// this tolerance should only account for numerical errors
		final double toleranceForAnalytic = 1E-15;

		final double maturityOfTheOption = fixingDate;
		final double periodLength = paymentDate - fixingDate;
		
		// the benchmark value
		final double finmathLibraryValue = notional * AnalyticFormulas.blackModelCapletValue(initialForwardLibor,
				liborVolatility, maturityOfTheOption, strike, periodLength, discountFactorAtMaturity);

		System.out.println("Value of the caplet computed by the Finmath library: " 
				+ printNumberWithFourDecimalDigits.format(finmathLibraryValue));

		
		// the value computed using the analytic BS formula from the finmath library
		final double ourAnalyticValue = InterestRatesProducts.calculateCapletValueBlackModel(initialForwardLibor,
						liborVolatility, strike, fixingDate, paymentDate, discountFactorAtMaturity, notional);

		System.out.println("Value of the caplet computed using the analytic formula for a call option: "
						+ printNumberWithFourDecimalDigits.format(ourAnalyticValue) + "\n");
		
		// We check the relative error
		assertEquals(0., (ourAnalyticValue-finmathLibraryValue)/finmathLibraryValue, toleranceForAnalytic);
		System.out.println("Analytic value via Black-Scholes formula correctly computed!\n");
		

		
		

		// the value computed using Monte Carlo simulation
		final double ourMonteCarloValue = InterestRatesProducts.calculateCapletValueBlackModel(initialForwardLibor,
				liborVolatility, strike, fixingDate, paymentDate, discountFactorAtMaturity, notional, numberOfTimeSteps, numberOfSimulations);

		System.out.println("Value of the caplet computed using Monte Carlo valuation of a call option: "
				+ printNumberWithFourDecimalDigits.format(ourMonteCarloValue) + "\n");

		
		// We check the relative error
		assertEquals(0, (ourMonteCarloValue-finmathLibraryValue)/finmathLibraryValue, toleranceForMonteCarlo);
		System.out.println("Monte-Carlo value correctly computed!\n");
	}


	@Test
	public void quantoTest() {
		// fixing and payment dates
		final double liborFixingDate = 1;
		final double liborPayDate = 2;
		
		// discount factor at maturity
		final double discountAtMaturity = 0.91;
		// foreign Libor rate dynamics
		final double initialForwardForeignLibor = 0.05;
		final double liborForeignVolatility = 0.3;

		// forward ffx rate dynamics
		final double ffxVolatility = 0.2;

		// correlation between the forward fx rate process and the Libor rate process
		final double correlationFxLibor = 0.4;

		// the quanto rate (i.e., the the constant conversion factor)
		final double quantoRate = 0.9;
		
		// strike of the quanto caplet
		final double strike = 0.05;
		
		// notional of the quanto caplet
		final double notional = 10000;
		
		final double quantoPrice = InterestRatesProducts.calculateQuantoCapletValue(
				initialForwardForeignLibor, liborForeignVolatility, ffxVolatility, correlationFxLibor, liborFixingDate,
				liborPayDate, strike, discountAtMaturity, notional, quantoRate);

		System.out.println("Price of the Quanto Caplet: " + printNumberWithFourDecimalDigits.format(quantoPrice) + "\n");
		
		assertEquals(43.5456, quantoPrice, 0.0001);
		
		System.out.println("Price of the Quanto Caplet correctly computed!");
	}

}
