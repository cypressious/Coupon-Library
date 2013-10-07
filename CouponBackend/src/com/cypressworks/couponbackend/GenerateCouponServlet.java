package com.cypressworks.couponbackend;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;

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
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Transaction;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;

@SuppressWarnings("serial")
public class GenerateCouponServlet extends HttpServlet {
	private static SimpleDateFormat sdf = new SimpleDateFormat(
			"dd.MM.yyyy HH:mm");
	private Configuration cfg;

	@Override
	public void init() throws ServletException {
		super.init();
		cfg = new Configuration();
		cfg.setServletContextForTemplateLoading(getServletContext(),
				"WEB-INF/templates");
		cfg.setObjectWrapper(new DefaultObjectWrapper());
	}

	@Override
	protected void doGet(final HttpServletRequest req,
			final HttpServletResponse resp) throws ServletException,
			IOException {
		resp.setContentType("text/html");

		final Map<String, Object> root = new HashMap<>();
		final Map<String, Object> app = new HashMap<>();
		root.put("app", app);

		final String appname = req.getParameter("app");

		if (appname == null || appname.equals("")) {
			app.put("defined", false);

		} else {
			app.put("defined", true);
			app.put("name", appname);

			final String action = req.getParameter("action");

			if (action != null) {
				root.put("action", action);

				switch (action) {
				case "generate":
					generateCoupon(req, root, appname);
					break;
				case "delete":
					deleteCoupon(req, root, appname);
					break;
				default:
				}
			} else {
				root.put("action", "");
			}

			showCoupons(root, appname);
			printTineZoneInfo(root);

		}

		final PrintWriter out = resp.getWriter();
		final Template temp = cfg.getTemplate("coupons.ftl");

		try {
			temp.process(root, out);
		} catch (final TemplateException e) {
			e.printStackTrace();
		}
		out.flush();

	}

	private static void showCoupons(final Map<String, Object> root,
			final String app) {

		final List<Object> couponsList = new ArrayList<>();

		final Key appKey = KeyFactory.createKey("app", app);

		final DatastoreService datastore = DatastoreServiceFactory
				.getDatastoreService();

		final Query query = new Query("Coupon", appKey).addSort("created",
				Query.SortDirection.DESCENDING);
		final List<Entity> coupons = datastore.prepare(query).asList(
				FetchOptions.Builder.withDefaults());

		for (final Entity entity : coupons) {

			final Map<String, Object> c = new HashMap<>();

			c.put("code", entity.getProperty("coupon").toString());
			c.put("one_time", entity.getProperty("one_time").toString());
			c.put("redeemed", entity.getProperty("redeemed").toString());

			final Object beginning = entity.getProperty("beginning");
			c.put("beginning", beginning != null ? sdf.format(beginning)
					: "null");

			final Object end = entity.getProperty("end");
			c.put("end", beginning != null ? sdf.format(end) : "null");
			c.put("created", sdf.format(entity.getProperty("created")));

			// Users
			final String size = getUserCount(datastore, entity);
			c.put("users", size);

			final Object redemption = entity.getProperty("redemtion_date");
			c.put("redemption_date",
					redemption != null ? sdf.format(redemption) : "null");

			couponsList.add(c);

		}

		root.put("coupons", couponsList);

	}

	private static String getUserCount(final DatastoreService datastore,
			final Entity entity) {
		final Number count = (Number) entity.getProperty("users_count");

		if (count != null) {
			return String.valueOf(count);
		} else {
			// Anzahl erstellen
			final Transaction transaction = datastore.beginTransaction();

			final List<Entity> users = getUsersForCoupon(datastore, entity);
			final int size = users.size();

			entity.setProperty("users_count", size);
			datastore.put(entity);

			transaction.commitAsync();

			return String.valueOf(size);
		}

	}

	public static List<Entity> getUsersForCoupon(
			final DatastoreService datastore, final Entity entity) {
		final Query userQuery = new Query("User", entity.getKey());
		final List<Entity> users = datastore.prepare(userQuery).asList(
				FetchOptions.Builder.withDefaults());
		return users;
	}

	private static void deleteCoupon(final HttpServletRequest req,
			final Map<String, Object> root, final String app) {

		final Map<String, Object> deleteMap = new HashMap<>();

		final DatastoreService datastore = DatastoreServiceFactory
				.getDatastoreService();
		final Key appKey = KeyFactory.createKey("app", app);

		final String coupon = req.getParameter("coupon");
		if (coupon == null || coupon.equals("")) {
			deleteMap.put("specified", false);

		} else {
			deleteMap.put("specified", true);

			final Query existsQuery = new Query("Coupon", appKey)
					.setFilter(new FilterPredicate("coupon",
							FilterOperator.EQUAL, coupon));
			final List<Entity> results = datastore.prepare(existsQuery).asList(
					FetchOptions.Builder.withDefaults());
			final boolean exists = !results.isEmpty();

			if (!exists) {
				deleteMap.put("exists", false);
			} else {
				deleteMap.put("exists", true);
				final Entity entity = results.get(0);

				final List<Entity> users = getUsersForCoupon(datastore, entity);

				for (final Entity user : users) {
					datastore.delete(user.getKey());
				}

				final Key couponKey = entity.getKey();
				datastore.delete(couponKey);
				deleteMap.put("code", coupon);
			}
		}

		root.put("delete", deleteMap);

	}

	private static void generateCoupon(final HttpServletRequest req,
			final Map<String, Object> root, final String app) {

		final Map<String, Object> couponMap = new HashMap<>();

		final DatastoreService datastore = DatastoreServiceFactory
				.getDatastoreService();
		final Key appKey = KeyFactory.createKey("app", app);

		// Code
		String coupon;
		{
			final String paramCoupon = req.getParameter("coupon");
			if (paramCoupon != null && !paramCoupon.equals("")) {
				final Query existsQuery = new Query("Coupon", appKey)
						.setFilter(new FilterPredicate("coupon",
								FilterOperator.EQUAL, paramCoupon));
				final boolean exists = !datastore.prepare(existsQuery)
						.asList(FetchOptions.Builder.withDefaults()).isEmpty();

				if (exists) {
					couponMap.put("exists", true);
					coupon = String.format("%08x", new Random().nextInt());
				} else {
					couponMap.put("exists", false);
					coupon = paramCoupon;
				}
			} else {
				couponMap.put("exists", false);
				coupon = String.format("%08x", new Random().nextInt());
			}
		}

		// One-time
		final boolean onetime = "on".equals(req.getParameter("onetime"));

		// Dates

		// Beginning
		final Date beginning;
		{
			final String beginDate = req.getParameter("beginning_date");
			final String beginHour = req.getParameter("beginning_hour");
			final String beginMinute = req.getParameter("beginning_minute");
			beginning = getDate(beginDate, beginHour, beginMinute);
		}

		// End
		final Date end;
		{
			final String endDate = req.getParameter("end_date");
			final String endHour = req.getParameter("end_hour");
			final String endMinute = req.getParameter("end_minute");
			end = getDate(endDate, endHour, endMinute);

		}

		final Entity couponEntity = new Entity("Coupon", appKey);
		couponEntity.setProperty("coupon", coupon);
		couponEntity.setProperty("one_time", onetime);
		couponEntity.setProperty("redeemed", false);
		couponEntity.setProperty("beginning", beginning);
		couponEntity.setProperty("end", end);
		couponEntity.setProperty("created", new Date());

		datastore.put(couponEntity);

		couponMap.put("code", coupon);
		root.put("coupon", couponMap);
	}

	private static Date getDate(final String date, final String hour,
			final String minute) {
		if (date != null) {
			final SimpleDateFormat sdf = new SimpleDateFormat("yyy.MM.dd");

			try {
				final Date dateOnly = sdf.parse(date);

				if (hour == null || minute == null || hour.equals("")
						|| minute.equals("")) {
					return dateOnly;
				}

				final int h = Integer.parseInt(hour);
				final int m = Integer.parseInt(minute);

				if (h < 0 || h > 23 || m < 0 || m > 59) {
					return dateOnly;
				}

				final Calendar cal = Calendar.getInstance();
				cal.setTime(dateOnly);
				cal.set(Calendar.HOUR_OF_DAY, h);
				cal.set(Calendar.MINUTE, m);

				return cal.getTime();

			} catch (ParseException | NumberFormatException e) {
				return null;
			}
		} else {
			return null;
		}
	}

	private static void printTineZoneInfo(final Map<String, Object> root) {
		final Map<String, Object> time = new HashMap<>();

		final TimeZone tz = Calendar.getInstance().getTimeZone();
		time.put("zone_name", tz.getDisplayName());
		time.put("zone_id", tz.getID());
		time.put("current", sdf.format(new Date()));

		root.put("time", time);
	}
}
