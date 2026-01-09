package session9;

import net.finmath.exception.CalculationException;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.marketdata.model.curves.ForwardCurveInterpolation;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.BrownianMotionFromMersenneRandomNumbers;
import net.finmath.montecarlo.interestrate.LIBORMonteCarloSimulationFromLIBORModel;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.models.LIBORMarketModelStandard;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCorrelationModel;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCorrelationModelExponentialDecay;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCovarianceModel;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCovarianceModelFromVolatilityAndCorrelation;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORVolatilityModel;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORVolatilityModelFromGivenMatrix;
import net.finmath.montecarlo.model.ProcessModel;
import net.finmath.montecarlo.process.EulerSchemeFromProcessModel;
import net.finmath.montecarlo.process.MonteCarloProcess;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

/**
 * This class creates a LIBOR market model using the classes implemented in the Finmath library
 */
public class LIBORMarketModelConstruction {

	/**
	 * It specifies and creates a Rebonato volatility structure, represented by a
	 * matrix, for the LIBOR Market Model. In particular, we have
	 * dL_j(t_i)=\sigma_j(t_i)L_j(t_i)dW_j(t_i) with
	 * \sigma_j(t_i)=(a+b(T_j-t_i))\exp(-c(T_j-t_i))+d, for t_i < T_j, for four
	 * parameters a,b,c,d with b,c>0. This class creates the matrix
	 * volatility[i,j]=sigma_j(t_i)
	 *
	 * @param a
	 * @param b
	 * @param c
	 * @param d
	 * @param simulationTimeDiscretization,  the time discretization for the
	 *                                       evolution of the processes
	 * @param tenureStructureDiscretization, the tenure structure T_0 < T_1< ...<T_n
	 * @return the matrix that represents the volatility structure:
	 *         volatility[i,j]=sigma_j(t_i)
	 */
	private static double[][] createVolatilityStructure(double a, double b, double c, double d,
			TimeDiscretization simulationTimeDiscretization, TimeDiscretization tenureStructureDiscretization) {
		// volatility[i,j]=sigma_j(t_i)
		final int numberOfSimulationTimes = simulationTimeDiscretization.getNumberOfTimeSteps();
		final int numberOfTenureStructureTimes = tenureStructureDiscretization.getNumberOfTimeSteps();
		final double[][] volatility = new double[numberOfSimulationTimes][numberOfTenureStructureTimes];

		for (int timeIndex = 0; timeIndex < numberOfSimulationTimes; timeIndex++) {
			for (int LIBORIndex = 0; LIBORIndex < numberOfTenureStructureTimes; LIBORIndex++) {
				final double currentTime = simulationTimeDiscretization.getTime(timeIndex);// t_j
				final double currentMaturity = tenureStructureDiscretization.getTime(LIBORIndex);// T_i
				final double timeToMaturity = currentMaturity - currentTime; // T_i-t_j
				double instVolatility;
				if (timeToMaturity <= 0) {
					instVolatility = 0; // This forward rate is already fixed, no volatility
				} else {
					instVolatility = d + (a + b * timeToMaturity) * Math.exp(-c * timeToMaturity);// \sigma_j(t)=(a+b(T_j-t))\exp(-c(T_j-t))+d
				}
				// Store
				volatility[timeIndex][LIBORIndex] = instVolatility;
			}
		}
		return volatility;
	}

	/**
	 * It simulates a LIBOR Market Model, by using the implementation of the Finmath library.
	 *
	 * @param numberOfPaths:          number of simulations
	 * @param simulationTimeStep:     the time step for the simulation of the LIBOR
	 *                                processes
	 * @param LIBORPeriodLength:      the length of the interval between times of
	 *                                the tenure structure
	 * @param LIBORRateTimeHorizon:   final LIBOR maturity
	 * @param fixingForGivenForwards: the times of the tenure structure where the
	 *                                initial forwards are given
	 * @param givenForwards:          the given initial forwards (from which the
	 *                                others are interpolated)
	 * @param correlationDecayParam,  parameter \alpha>0, for the correlation of the
	 *                                forwards: in particular, we have
	 *                                dL_i(t_j)=\sigma_i(t_j)L_i(t_j)dW_i(t_j) with
	 *                                d<W_i,W_k>(t)= \rho_{i,k}(t)dt where
	 *                                \rho_{i,j}(t)=\exp(-\alpha|T_i-T_k|)
	 * @param a,                      the first term for the volatility structure:
	 *                                the volatility in the SDEs above is given by
	 *                                \sigma_i(t_j)=(a+b(T_i-t_j))\exp(-c(T_i-t_j))+d,
	 *                                for t_j < T_i.
	 * @param b,                      the second term for the volatility structure
	 * @param c,                      the third term for the volatility structure
	 * @param d,                      the fourth term for the volatility structure
	 * @return an object of a class implementing LIBORModelMonteCarloSimulationModel, i.e.,
	 *         representing the simulation of a LMM
	 * @throws CalculationException
	 */
	public static TermStructureMonteCarloSimulationModel createLIBORMarketModel(int numberOfPaths,
			double simulationTimeStep, double LIBORPeriodLength, // T_i-T_{i-1}, we suppose it to be fixed
			double LIBORRateTimeHorizon, // T_n
			double[] fixingForGivenForwards, double[] givenForwards, double correlationDecayParam, double a, double b,
			double c, double d) throws CalculationException {
		/*
		 * In order to simulate a LIBOR market model, we need to proceed along the
		 * following steps:
		 * 1) provide the time discretization for the evolution of the
		 * processes
		 * 2) provide the time discretization of the tenure structure
		 * 3) provide the observed term structure of the initial forward rates and if needed
		 * interpolate the ones missing: in this way we obtain the initial values for
		 * the LIBOR processes
		 * 4) create the volatility structure, i.e., the terms
		 * sigma_i(t_j) in dL_i(t_j)=\sigma_i(t_j)L_i(t_j)dW_i(t_j)
		 * 5) create the correlation structure, i.e., define the terms \rho_{i,j}(t) such that
		 * d<W_i,W_k>(t)= \rho_{i,k}(t)dt
		 * 6) combine all steps 1, 2, 4, 5 to create a covariance model
		 * 7) combine steps 2, 3, 6 to create the LIBOR model
		 * 8) create an Euler discretization of the model we defined in step 7, specifying the
		 * model itself and a Brownian motion that uses the time discretization defined
		 * in step 1
		 * 9) give the Euler scheme to the constructor of LIBORMonteCarloSimulationFromLIBORModel,
		 * to create an object of type TermStructureMonteCarloSimulationModel
		 */

		// Step 1: create the time discretization for the simulation of the processes
		final TimeDiscretization timeDiscretization = new TimeDiscretizationFromArray(0.0,
				(int) (LIBORRateTimeHorizon / simulationTimeStep), simulationTimeStep);

		/*
		 * Step 2: create the time discretization for the tenure structure (i.e., the
		 * dates T_1,..,T_n)
		 */
		final TimeDiscretization LIBORPeriodDiscretization = new TimeDiscretizationFromArray(0.0,
				(int) (LIBORRateTimeHorizon / LIBORPeriodLength), LIBORPeriodLength);

		/*
		 * Step 3 Create the forward curve (initial values for the LIBOR market model).
		 * We might fail to have (or do not want to give) all the forwards: the others
		 * are interpolated using the specific method of the Finmath library
		 */
		final ForwardCurve forwardCurve = ForwardCurveInterpolation.createForwardCurveFromForwards("forwardCurve",
				fixingForGivenForwards, // fixing dates of the forwards we provide
				givenForwards, // the forwards we provide
				LIBORPeriodLength);

		// Step 4, the volatility model: we only have to provide the matrix
		final double[][] volatility = createVolatilityStructure(a, b, c, d, timeDiscretization,
				LIBORPeriodDiscretization);

		final LIBORVolatilityModel volatilityModel = new LIBORVolatilityModelFromGivenMatrix(timeDiscretization,
				LIBORPeriodDiscretization, volatility);

		// Step 5 Create a correlation model rho_{i,j} = exp(−a ∗ |T_i −T_j|)
		final LIBORCorrelationModel correlationModel = new LIBORCorrelationModelExponentialDecay(timeDiscretization,
				LIBORPeriodDiscretization, LIBORPeriodDiscretization.getNumberOfTimes() - 1, // no factor reduction
				correlationDecayParam);

		/*
		 * Step 6 Combine volatility model and correlation model, together with the two
		 * time discretizations, to get a covariance model
		 */
		final LIBORCovarianceModel covarianceModel = new LIBORCovarianceModelFromVolatilityAndCorrelation(
				timeDiscretization, LIBORPeriodDiscretization, volatilityModel, correlationModel);

		/*
		 * Step 7 Combine the forward curve and the covariance model, together with the
		 * time discretization of the tenure structure, to define the model
		 */
		final ProcessModel LIBORMarketModel = new LIBORMarketModelStandard(LIBORPeriodDiscretization, forwardCurve,
				covarianceModel);

		// Step 8: create an Euler scheme of the LIBOR model defined above
		final BrownianMotion brownianMotion = new BrownianMotionFromMersenneRandomNumbers(timeDiscretization,
				LIBORPeriodDiscretization.getNumberOfTimes() - 1, // no factor reduction for now
				numberOfPaths, 1897 // seed
		);

		final MonteCarloProcess process = new EulerSchemeFromProcessModel(LIBORMarketModel, brownianMotion);

		
		// Step 9: give the Euler scheme to the constructor of  LIBORMonteCarloSimulationFromLIBORModel
		return new LIBORMonteCarloSimulationFromLIBORModel(process);

	}

}
