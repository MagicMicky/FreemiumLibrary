package com.magicmicky.freemiumlibrary;

/**
 * @author Mickael Goubin
 */
public class PremiumModeException extends RuntimeException {

	public PremiumModeException(String desc) {
		super(desc);
	}


	public static class WrongLayoutException extends PremiumModeException {

		public WrongLayoutException(boolean isDrawer) {
			super(isDrawer  ? ("The resource given wasn't found in the drawer") : ("No layout were found for the ads"));
		}
	}

	public static class PremiumPackageIdError extends PremiumModeException {

		public PremiumPackageIdError() {
			super("The premium in-app package isn't set. Please use #setPremiumPackageSKU");
		}
	}
}
