package com.magicmicky.freemiumlibrary;

/**
 * Custom exceptions thrown by the library
 * @author Mickael Goubin
 */
public class PremiumModeException extends RuntimeException {

	public PremiumModeException(String desc) {
		super(desc);
	}


	public static class WrongLayoutException extends PremiumModeException {

		public WrongLayoutException() {
			super("The layout given wasn't found");
		}
	}
    public static class ViewNotFoundException extends PremiumModeException {

        public ViewNotFoundException() {
            super("The resource given isn't found");
        }
    }

	public static class PremiumPackageIdError extends PremiumModeException {

		public PremiumPackageIdError() {
			super("The premium in-app package is null ");
		}
	}
}
