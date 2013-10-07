package com.cypressworks.couponbackend;

import java.io.IOException;
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
import com.google.appengine.labs.repackaged.org.json.JSONException;
import com.google.appengine.labs.repackaged.org.json.JSONObject;

/**
 * 
 * @author Kirill Rakhman
 */
@SuppressWarnings("serial")
public class CheckUserServlet extends HttpServlet {

	@Override
	protected void doGet(final HttpServletRequest req,
			final HttpServletResponse resp) throws ServletException,
			IOException {
		try {
			resp.setContentType("application/json");
			final JSONObject json = new JSONObject();

			final String app = req.getParameter("app");
			final String user = req.getParameter("user");

			if (app == null || user == null) {

				json.put("success", false);
				json.put("errorCode", 1);
				json.put("errorMessage", "Not enough paramters.");

			} else {
				final boolean hasUserRedeemed = hasUserRedeemed(app, user);
				json.put("success", hasUserRedeemed);
				json.put("errorCode", 0);

			}

			resp.getWriter().println(json.toString(2));
		} catch (final JSONException e) {
			e.printStackTrace();
		}
	}

	public static boolean hasUserRedeemed(final String app, final String user) {
		final DatastoreService datastore = DatastoreServiceFactory
				.getDatastoreService();

		final Key appKey = KeyFactory.createKey("app", app);

		final Filter filter = new FilterPredicate("user", FilterOperator.EQUAL,
				user);
		final Query userQuery = new Query("User", appKey).setFilter(filter);
		final List<Entity> users = datastore.prepare(userQuery).asList(
				FetchOptions.Builder.withDefaults().limit(1));

		return !users.isEmpty();
	}

}
