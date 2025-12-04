package session6;

import net.finmath.functions.AnalyticFormulas;

/**
 * This class takes care of the computations of the price in natural units (i.e., for  payment date T_2)
 * and of the price in arrears (i.e., for payment date T_1) of a digital Caplet under the Black model. 
 * Note that the computation of the convexity adjustment and consequently of the price in arrears are
 * implemented in the parent, abstract class EuropeanOptionPossiblyInArrears: these quantities can
 * be computed once the value of the classical price as a function of the initial value of the Libor
 * is known.
 */
public class DigitalCapletWithBlack extends EuropeanOptionPossiblyInArrears {

	private final double strike;// this is a field specific to the digital caplet

	public DigitalCapletWithBlack(double firstTime, double secondTime, double firstBondOrInitialLibor,
			double secondBond, double liborVolatility, double notional, Boolean giveLibor, double strike) {
		// parent constructor
		super(firstTime, secondTime, firstBondOrInitialLibor, secondBond, liborVolatility, notional, giveLibor);
		this.strike = strike;// initialization of the derived class specific field
	}

	@Override
	public double getValueInClassicUnits(double initialValueLibor) {
		// see slide 190
		return getNotional() * getSecondBond() * getTimeInterval() * AnalyticFormulas
				.blackScholesDigitalOptionValue(initialValueLibor, 0, getLiborVolatility(), getFirstTime(), strike);
	}
}