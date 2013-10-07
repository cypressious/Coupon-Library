package com.cypressworks.couponlibrary;

import android.accounts.Account;

/**
 * Callback for redeeming and checking coupons.
 * 
 * @author Kirill Rakhman
 */
public interface RedemptionCallback {
	void onCallback(Account acc, boolean result);
}
