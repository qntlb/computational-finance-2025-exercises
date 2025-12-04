package session6;

/**
 * This is an abstract class implementing the computations of the
 * price in natural units (i.e., for payment date T_2) and of the price in arrears (i.e., for
 * payment date T_1) of a general European option whose payoff is f(L_1), where (L_t) is a
 * Libor rate following the Black model. In particular, the
 * computation of the convexity adjustment and consequently of the price in
 * arrears is implemented here: these quantities can be effectively computed
 * once the value of the price in natural units as a function of the initial value of
 * the Libor is known, see Theorem 241 in the slides. This last computation is specific to any
 * particular derivative, and it is therefore implemented in the derived
 * classes: for this reason, here it is defined as an abstract method.
 */
public abstract class EuropeanOptionPossiblyInArrears {

	/*
	 * All these parameters are common to all the interest rates derivatives we
	 * consider
	 */
	private final double firstTime, secondTime;
	private double firstBond; // P(T_1; 0)
	private final double secondBond; // P(T_2; 0)
	private double initialLibor; // L(T_1, T_2; 0) =: L_0 = 1/(T_2 - T_1) (P(T_1; 0)/P(T_2; 0) - 1)
	private final double liborVolatility;

	private final double notional;

	/**
	 * It constructs an object that compute the value in arrears and in natural unit
	 * of an option. The value of the zero coupon bond P(T_2;0) must be given.
	 * One can pass P(T_1;0) or L(T_1,T_2;0), and the other quantity is computed consequently
	 * from the given one and P(T_2; 0).
	 *
	 * @param firstTime,               T_1
	 * @param secondTime,              T_2
	 * @param firstBondOrInitialLibor, P(T_1 0);
	 * @param secondBond,              P(T_2;0)
	 * @param liborVolatility,         the volatility of the Libor in the Black model
	 * @param notional                 notional amount
	 * @param giveLibor,               Boolean: True if gives L(T_1,T_2;0), false if one gives P(T_1;0)
	 */
	public EuropeanOptionPossiblyInArrears(double firstTime, double secondTime, double firstBondOrInitialLibor,
			double secondBond, double liborVolatility, double notional, Boolean giveLibor) {
		this.firstTime = firstTime;
		this.secondTime = secondTime;
		this.secondBond = secondBond;
		this.liborVolatility = liborVolatility;
		this.notional = notional;

		if (giveLibor) { // in this case, we get directly L(T_1,T_2;0) and compute P(T_1;0)
			initialLibor = firstBondOrInitialLibor;
			computeFirstBond();
		} else {// in this case, we get directly P(T_1;0) and compute L(T_1,T_2;0)
			firstBond = firstBondOrInitialLibor;
			computeInitialLibor();
		}
	}

	private void computeInitialLibor() {
		final double timeInterval = getTimeInterval();
		initialLibor = 1 / timeInterval * (firstBond / secondBond - 1);
	}

	private void computeFirstBond() {
		final double timeInterval = secondTime - firstTime;
		firstBond = secondBond * (initialLibor * timeInterval + 1);

	}

	/*
	 * The fields should be private, but we might need their value in the derived
	 * class (or even in not derived classes). Therefore, we write getters
	 */

	public double getFirstTime() {
		return firstTime;
	}

	public double getSecondTime() {
		return secondTime;
	}

	public double getFirstBond() {
		return firstBond;
	}

	public double getSecondBond() {
		return secondBond;
	}

	public double getLiborVolatility() {
		return liborVolatility;
	}

	public double getNotional() {
		return notional;
	}

	public double getTimeInterval() {
		return secondTime - firstTime;
	}

	public double getInitialValueLibor() {
		return initialLibor;
	}

	/**
	 * It computes and returns the price of the contract paid in natural units (i.e., if the
	 * payment date is T_2) as a function of the initial value of the Libor
	 *
	 * @param initialValue, initial value of the Libor
	 * @return price of the contract paid in natural units (i.e., if the payment date is
	 *         T_2) as a function of the initial value of the Libor.
	 */
	/*
	 * We want the "natural" price (i.e., the price in units of P(T_2;0)) as
	 * a function of the initial value of the Libor because we want to use this
	 * formula to derive the convexity adjustment: in this case, we give the initial
	 * value of the Libor multiplied by the exponential of sigma_L^2 T_1
	 */
	public abstract double getValueInClassicUnits(double initialValueLibor);

	
	public double getValueInClassicUnits() {
		return getValueInClassicUnits(initialLibor);
	}
	
	
	/**
	 * It computes and returns the convexity adjustment using the derivation of Theorem 241. Here the
	 * point is that the formula is identical for all the contracts, once you know
	 * the classical price as a function of the initial value of the Libor.
	 *
	 * @return convexity adjustment
	 */
	public double computeConvexityAdjustment() {
		final double valueForAdjustedLibor = getValueInClassicUnits(
				initialLibor * Math.exp(liborVolatility * liborVolatility * firstTime));
		return initialLibor * getTimeInterval() * valueForAdjustedLibor;
	}
	
	/**
	 * It computes and returns the price in arrears of the contract (i.e., if the
	 * payment date is T_1). Here the point is that the
	 * formula is identical for all the contracts, once one knows the classical price
	 * as a function of the initial value of the Libor.
	 *
	 * @return the price in arrears of the contract (i.e., if the payment date is T_1)
	 */
	public double getValueInArrears() {
		return getValueInClassicUnits() + computeConvexityAdjustment();
	}
}
