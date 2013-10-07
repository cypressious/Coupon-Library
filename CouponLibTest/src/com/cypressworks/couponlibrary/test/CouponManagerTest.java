package com.cypressworks.couponlibrary.test;

import java.lang.reflect.Field;

import junit.framework.Assert;
import android.test.AndroidTestCase;

import com.cypressworks.couponlibrary.CouponManager;

public class CouponManagerTest extends AndroidTestCase {

	public void testUrl() throws Exception {
		final String baseUrl = "http://test.appspot.com";
		final String couponName = "test";
		final CouponManager tester = new CouponManager(baseUrl, couponName,
				new byte[] {});

		final Class<? extends CouponManager> clazz = tester.getClass();

		final Field redeemUrl = clazz.getDeclaredField("redeemUrl");
		redeemUrl.setAccessible(true);
		final Field checkUrl = clazz.getDeclaredField("checkUrl");
		checkUrl.setAccessible(true);

		Assert.assertEquals(redeemUrl.get(tester),
				"http://test.appspot.com/redeem?app=test&coupon=%COUPON%&user=%USER%");
		Assert.assertEquals(checkUrl.get(tester),
				"http://test.appspot.com/check?app=test&user=%USER%");
	}

}
