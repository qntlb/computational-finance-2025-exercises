package session9;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;

/**
 * This class represents a digital caplet on a LIBOR rate, for a given strike.
 * The underlying is a LIBOR rate from time periodStart to time periodEnd. Note
 * the implementation of the getValue method.
 */
public class MyDigitalCaplet extends AbstractLIBORMonteCarloProduct {
	private final double periodStart; // T_i
	private final double periodEnd; // T_{i+1}
	private final double strike;

	public MyDigitalCaplet(double periodStart, double periodEnd, double strike) {
		super();
		this.periodStart = periodStart;
		this.periodEnd = periodEnd;
		this.strike = strike;
	}

	/**
	 * Computes and returns the payoff of a digital caplet
	 *
	 * @param evaluationTime The time on which this products value is evaluated.
	 * @param model          The underlying of the product. A priori is an object of
	 *                       type TermStructureMonteCarloSimulationModel.
	 * @return The random variable representing the value of the product discounted
	 *         to evaluation time
	 */
	@Override
	public RandomVariable getValue(double evaluationTime, LIBORModelMonteCarloSimulationModel model)
			throws CalculationException {

		// (T_{i+1)-T_i)1_(L(T_i,T_{i+1};T_i)-K>0)
		final double periodLength = periodEnd - periodStart;

		// Get the value of the LIBOR L_i at T_i: L(T_i,T_{i+1};T_i)
		final double simulationTime = periodStart;
		final RandomVariable libor = model.getLIBOR(simulationTime, periodStart, periodEnd);

		final RandomVariable trigger = libor.sub(strike);
		// payoff = (T_{i+1}-T_i)1_{L(T_i)>K}
		RandomVariable values = trigger.choose(new Scalar(periodLength), new Scalar(0.0));

		// Get numeraire at payment time: you then divide by N(T_{i+1})
		final RandomVariable numeraireAtPaymentTime = model.getNumeraire(periodEnd);

		values = values.div(numeraireAtPaymentTime);

		// Get numeraire at evaluation time: you multiply by N(0)
		final RandomVariable numeraireAtEvaluationTime = model.getNumeraire(evaluationTime);
		values = values.mult(numeraireAtEvaluationTime);

		return values;
	}
}
