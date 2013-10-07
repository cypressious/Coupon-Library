package com.cypressworks.couponbackend;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.labs.repackaged.org.json.JSONException;
import com.google.appengine.labs.repackaged.org.json.JSONObject;

/**
 * 
 * @author Kirill Rakhman
 */
@SuppressWarnings("serial")
public class RedeemCouponServlet extends HttpServlet {

	@Override
	protected void doGet(final HttpServletRequest req,
			final HttpServletResponse resp) throws ServletException,
			IOException {
		try {
			resp.setContentType("application/json");
			final JSONObject json = new JSONObject();

			final String app = req.getParameter("app");
			final String coupon = req.getParameter("coupon");
			final String user = req.getParameter("user");

			if (app == null || coupon == null || user == null || app.equals("")
					|| coupon.equals("") || user.equals("")) {

				json.put("errorCode", 1);
				json.put("errorMessage", "Not enough paramters.");
				json.put("success", false);

			} else {
				final DatastoreService datastore = DatastoreServiceFactory
						.getDatastoreService();

				final Transaction transaction = datastore.beginTransaction();

				try {

					final Key appKey = KeyFactory.createKey("app", app);

					final Filter filter = new FilterPredicate("coupon",
							FilterOperator.EQUAL, coupon);

					final Query query = new Query("Coupon", appKey)
							.setFilter(filter);

					final List<Entity> result = datastore.prepare(query)
							.asList(FetchOptions.Builder.withDefaults()
									.limit(1));

					if (!result.isEmpty()) {
						final Entity couponEntity = result.get(0);

						final boolean redeemed = handleCoupon(couponEntity,
								app, user, json);

						if (redeemed) {
							datastore.put(couponEntity);

							final Entity userEntity = new Entity("User",
									couponEntity.getKey());

							userEntity.setProperty("user", user);
							// userEntity.setProperty("app", app);
							userEntity.setProperty("coupon",
									couponEntity.getKey());

							datastore.put(userEntity);

							transaction.commit();
						}

					} else {
						json.put("errorCode", 2);
						json.put("errorMessage", "Coupon code not found.");
						json.put("success", false);
					}
				} finally {
					if (transaction.isActive()) {
						transaction.rollback();
					}
				}

			}
			resp.getWriter().println(json.toString(2));
		} catch (final JSONException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param entity
	 * @param user
	 * @param app
	 * @param json
	 * @return True, if coupon can be redeemed.
	 * @throws JSONException
	 */
	private static boolean handleCoupon(final Entity entity, final String app,
			final String user, final JSONObject json) throws JSONException {
		// check if onetime and redeemed
		final boolean one_time = (boolean) entity.getProperty("one_time");

		// Check if one-time
		if (one_time) {
			final boolean redeemed = (boolean) entity.getProperty("redeemed");

			if (redeemed) {
				json.put("errorCode", 3);
				json.put("errorMessage",
						"Coupon already redeemed and not redeemable again.");
				json.put("success", false);
				return false;
			}
		}

		// Check if time limited
		final Date beginning = (Date) entity.getProperty("beginning");
		final Date end = (Date) entity.getProperty("end");
		final Date now = new Date();

		if (beginning != null) {
			if (now.before(beginning)) {
				json.put("errorCode", 4);
				json.put("errorMessage", "Coupon isn't valid, yet.");
				json.put("success", false);
				return false;
			}
		}

		if (end != null) {
			if (now.after(end)) {
				json.put("errorCode", 5);
				json.put("errorMessage", "Coupon isn't valid anymore.");
				json.put("success", false);
				return false;
			}
		}

		// Check if already redeemed
		final boolean hasUserRedeemed = CheckUserServlet.hasUserRedeemed(app,
				user);

		if (hasUserRedeemed) {
			json.put("errorCode", 6);
			json.put("errorMessage", "User has already redeemed a coupon");
			json.put("success", true);
			return false;
		}

		// Valid
		entity.setProperty("redemtion_date", now);
		entity.setProperty("redeemed", true);

		final Long count = (Long) entity.getProperty("users_count");
		entity.setProperty("users_count", (count != null ? count : 0L) + 1);

		json.put("errorCode", 0);
		json.put("success", true);
		return true;

	}

}
