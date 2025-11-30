package session5;

import java.text.DecimalFormat;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

import net.finmath.exception.CalculationException;

public class CapsTest {

	final DecimalFormat printNumberWithFourDecimalDigits = new DecimalFormat("0.0000");
	
	@Test
	public void capTwoCapletsTest() {
		// discount factors
		final double firstDiscountFactor = 0.91;
		final double secondDiscountFactor = 0.82;

		// parameters for the Libor dynamics
		final double initialFirstLibor = 0.05;
		final double initialSecondLibor = 0.04;

		final double firstLiborVolatility = 0.3;
		final double secondLiborVolatility = 0.25;

		// parameters for the options: dates..
		final double firstFixingDate = 1; // T_1
		final double secondFixingDate = 1.5; // T_2: we could also call it firstPaymentDate
		final double secondPaymentDate = 2; // T_3

		// ..and strikes
		final double firstStrike = 0.05;
		final double secondStrike = 0.04;
		final double notional = 1000;

		// parameters for the Monte Carlo simulation
		final int numberOfTimeSteps = 100;
		final int numberOfSimulations = 100000;

		final double correlation = 0.2;

		double capValue = 0.0;
		try {
			capValue = InterestRatesProductsEnhanced.calculateCapValueBlackModel(initialFirstLibor,
					initialSecondLibor, firstLiborVolatility, secondLiborVolatility, correlation, firstStrike, secondStrike,
					firstFixingDate, secondFixingDate, secondPaymentDate, firstDiscountFactor, secondDiscountFactor,
					notional, numberOfTimeSteps, numberOfSimulations);
		} catch (CalculationException e) {
			System.out.println("You got a calculation exception ");
		}
		
		// testing the result
		final double toleranceForMonteCarlo = 2 / Math.sqrt(numberOfSimulations);
		assertEquals(0, (capValue - 4.7) / 4.7, toleranceForMonteCarlo);
		System.out.println("The value of the cap is " + printNumberWithFourDecimalDigits.format(capValue));
	}
}
