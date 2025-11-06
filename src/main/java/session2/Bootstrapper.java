package session2;

/**
 * This class bootstraps the zero coupon bond curve from the values of coupon
 * bonds. Here, we suppose that the time step of the tenure
 * structure is constant.
 */

public class Bootstrapper {

	private final double[] coupons; // the value of the coupons C_i
	private final double[] couponBonds; // the value of the quoted coupon bonds CB_i
	private final double yearFraction;// the constant value T_{i+1}-T_i
	private double[] zeroBondPrices; // the array that will contain the bootstrapped zero-coupon bond

	public Bootstrapper(double[] coupons, double[] couponBonds, double yearFraction) {
		this.coupons = coupons;
		this.couponBonds = couponBonds;
		
        if (coupons.length != couponBonds.length) {
            throw new IllegalArgumentException(
                "The length of coupons and couponBonds must be the same. " +
                "coupons.length=" + coupons.length + ", couponBonds.length=" + couponBonds.length
            );
        }
		this.yearFraction = yearFraction;
	}
	
	public void bootstrapZeroCoupons() {

	    int n = couponBonds.length;
	    double[] zeroBondPrices = new double[n];

	    // First maturity: P(T_1) = CB_1 / (1 + C_1 * yearFraction)
	    // Pay attention to the indeces: index i corresponds to T_{i+1}
	    zeroBondPrices[0] = (couponBonds[0]) / (1 + coupons[0] * yearFraction);

	    // Following maturities: P(T_m) = (CB_m - CB_{m-1} + P(T_{m-1}) / (1 + C_m * yearFraction)
	    for (int m = 1; m < n; m++) {
	        zeroBondPrices[m] = (couponBonds[m] - couponBonds[m - 1] + zeroBondPrices[m - 1])
	                        / (1 + coupons[m] * yearFraction);
	    }
	    this.zeroBondPrices = zeroBondPrices;
	}

	/**
	 * Returns the bootstrapped zero-coupon bond prices.
	 * 
	 * @return an array containing the zero-coupon prices for each maturity
	 */
    public double[] getZeroPrices() {
    	if (zeroBondPrices == null) {
            throw new IllegalStateException("Zero-coupon bond prices have not been bootstrapped yet.");
        }
        return zeroBondPrices.clone(); // return a copy of the array
    }
	
}
