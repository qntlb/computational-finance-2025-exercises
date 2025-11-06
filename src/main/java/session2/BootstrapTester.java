package session2;

import java.util.Arrays;
import java.text.DecimalFormat;

public class BootstrapTester {

	public static void main(String[] args) {
		// Initializing the array of the coupons
		double[] coupons = new double[8];
		// Filling the array of the coupons
		Arrays.fill(coupons, 0.03);
				
		// Initializing and filling the array of the coupon bond prices
		double[] couponBonds = {0.9902,	0.9822,	0.9757, 0.9706,	0.9667,	0.9639,	0.9621,	0.9611};
		
		System.out.println("coupons: " + Arrays.toString(coupons));
		System.out.println("coupon bond prices: " + Arrays.toString(couponBonds));
		
		double yearFraction = 0.5;// the constant T_{i+1}-T_i

		Bootstrapper bootstrapper = new Bootstrapper(coupons, couponBonds, yearFraction);
		System.out.println("Bootstrapper initialized");
		
		System.out.println("..........");
		
		System.out.println("Bootstrapping the zero-coupon bonds curve:");
		bootstrapper.bootstrapZeroCoupons();
		
		double[] zeroBondPrices = bootstrapper.getZeroPrices();
		final DecimalFormat printWithFourDecimalDigits = new DecimalFormat("0.0000");
		System.out.println("Maturity \t Zero-coupon bond price");
		for (int i = 0; i < zeroBondPrices.length; i++) {
			System.out.println((i+1) * yearFraction + "           \t" + printWithFourDecimalDigits.format(zeroBondPrices[i]));
		}

	}

}
