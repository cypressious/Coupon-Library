package com.cypressworks.couponlibrary;

import java.net.URL;
import java.util.concurrent.Semaphore;

import org.json.JSONObject;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.google.android.vending.licensing.AESObfuscator;
import com.google.android.vending.licensing.Obfuscator;
import com.google.android.vending.licensing.PreferenceObfuscator;

/**
 * 
 * @author Kirill Rakhman
 */
public class CouponManager {

	private final String prefName;
	private final String redeemUrl;
	private final String checkUrl;
	private final byte[] salt;

	public CouponManager(final String baseUrl, final String couponName,
			final byte[] salt) {

		if (baseUrl == null || couponName == null || salt == null) {
			throw new IllegalArgumentException(
					"Constructor arguments mustn't be null.");
		}

		prefName = "coupon:" + couponName;

		redeemUrl = Uri.parse(baseUrl).buildUpon().appendPath("redeem")
				.appendQueryParameter("app", couponName).toString()
				+ "&coupon=%COUPON%&user=%USER%";
		checkUrl = Uri.parse(baseUrl).buildUpon().appendPath("check")
				.appendQueryParameter("app", couponName).toString()
				+ "&user=%USER%";

		this.salt = salt;

	}

	/**
	 * Redeems the given coupon in a separate thread.
	 * 
	 * @param activity
	 *            Activity, this is necessary for obtaining the auth token.
	 * @param user
	 *            Account to redeem for. Must be of type "com.google"
	 * @param coupon
	 *            Actual coupon code.
	 * @param callback
	 *            Callback that will be called with the result. If the
	 *            redemption was successful, it will be called with result set
	 *            to <code>true</code>.
	 */
	public void redeemCouponAsync(final Activity activity, final Account user,
			final String coupon, final RedemptionCallback callback) {

		if (activity == null || user == null || coupon == null
				|| callback == null) {
			throw new IllegalArgumentException("Arguments mustn't be null.");
		}

		if (!user.type.equals("com.google")) {
			throw new IllegalArgumentException(
					"Account must be of type \"com.google\"");
		}

		new AsyncCouponTask(activity, user, callback).execute(redeemUrl
				.replace("%COUPON%", coupon));

	}

	/**
	 * Checks if any of the available Google accounts has successfully redeemed
	 * a coupon.
	 * 
	 * @param activity
	 *            Activity, this is necessary for obtaining the auth token.
	 * @param callback
	 *            Callback that will be called with the result. It will be
	 *            called once for every available account. If the account has
	 *            successfully redeemed, result will be set to <code>true</code>
	 *            .
	 */
	public void checkUserAsync(final Activity activity,
			final RedemptionCallback callback) {

		if (activity == null || callback == null) {
			throw new IllegalArgumentException("Arguments mustn't be null.");
		}

		final AccountManager am = AccountManager.get(activity);
		final Account[] accounts = am.getAccountsByType("com.google");

		for (final Account account : accounts) {
			new AsyncCouponTask(activity, account, callback).execute(checkUrl);
		}

	}

	private final class AsyncCouponTask extends
			AsyncTask<String, Void, Boolean> {
		private final Activity activity;
		private final Account account;
		private final RedemptionCallback callback;
		private static final String SCOPE = "oauth2:https://www.googleapis.com/auth/userinfo.profile";
		private static final String ID_URL = "https://www.googleapis.com/oauth2/v1/userinfo?access_token=";

		private AsyncCouponTask(final Activity a, final Account user,
				final RedemptionCallback callback) {
			activity = a;
			account = user;
			this.callback = callback;
		}

		@Override
		protected Boolean doInBackground(final String... params) {
			try {
				final AccountManager am = AccountManager.get(activity);
				final Container<String> tokenContainer = new Container<String>();
				final Semaphore mutex = new Semaphore(0);
				String id = null;
				int retry = 2;

				while (retry > 0) {
					retry--;

					am.getAuthToken(account, SCOPE, null, activity,
							new AccountManagerCallback<Bundle>() {

								@Override
								public void run(
										final AccountManagerFuture<Bundle> future) {
									// Token Success
									try {
										tokenContainer.value = future
												.getResult()
												.getString(
														AccountManager.KEY_AUTHTOKEN);
									} catch (final Exception e) {
										e.printStackTrace();
										tokenContainer.value = null;
									} finally {
										mutex.release();
									}

								}
							}, null);

					mutex.acquire();
					final String token = tokenContainer.value;

					if (token == null) {
						return false;
					}

					// Google id
					try {

						final String url = ID_URL + token;

						final String idResponse = DownloadUtils
								.downloadHTTPSString(new URL(url));
						final JSONObject idJson = new JSONObject(idResponse);

						id = idJson.optString("id", null);

						if (id != null) {
							break;
						}

						// Token will be invalidated

					} catch (final Exception e) {
						// Token will be invalidated
					}

					log("Invalidating token");
					am.invalidateAuthToken("com.google", token);
				}

				if (id == null) {
					return false;
				}

				// Couponserver kontaktieren
				final String url = params[0].replace("%USER%", id);
				final String couponResponse = DownloadUtils
						.downloadHTTPString(new URL(url));

				final JSONObject json = new JSONObject(couponResponse);

				return json.optBoolean("success");
			} catch (final Exception e) {
				e.printStackTrace();
			}
			return false;
		}

		@Override
		protected void onPostExecute(final Boolean redeemed) {
			writePref(activity, redeemed, account.name);
			callback.onCallback(account, redeemed);

		}
	}

	/**
	 * Checks whether a user has already redeemed a coupon by reading the
	 * preference file. This does not involve communicating with the backend.
	 * 
	 * @param c
	 *            Context
	 * @return <code>true</code> if a coupon was successfully redeemed for one
	 *         of the available accounts.
	 */
	public boolean hasUserRedeemed(final Context c) {
		final AccountManager am = AccountManager.get(c);
		final Account[] accounts = am.getAccountsByType("com.google");

		for (final Account account : accounts) {
			final String user = account.name;

			final boolean result = getPref(c, user);

			if (result) {
				return true;
			}
		}

		return false;
	}

	private void writePref(final Context c, final boolean result,
			final String user) {

		final SharedPreferences sp = c.getSharedPreferences(prefName,
				Context.MODE_PRIVATE);
		final Obfuscator obfuscator = new AESObfuscator(salt,
				c.getPackageName(), user);
		final PreferenceObfuscator prefs = new PreferenceObfuscator(sp,
				obfuscator);

		prefs.putString("redeemed" + user, Boolean.toString(result));
		prefs.commit();
	}

	private boolean getPref(final Context c, final String user) {

		final SharedPreferences sp = c.getSharedPreferences(prefName,
				Context.MODE_PRIVATE);
		final Obfuscator obfuscator = new AESObfuscator(salt,
				c.getPackageName(), user);
		final PreferenceObfuscator prefs = new PreferenceObfuscator(sp,
				obfuscator);

		final String stringResult = prefs.getString("redeemed" + user,
				Boolean.toString(false));

		return Boolean.parseBoolean(stringResult);
	}

	private static void log(final String msg) {
		if (BuildConfig.DEBUG) {
			Log.i(CouponManager.class.getSimpleName(), msg);
		}
	}

}
