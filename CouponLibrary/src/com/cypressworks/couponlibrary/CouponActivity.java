package com.cypressworks.couponlibrary;

import android.os.Bundle;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.cypressworks.couponlibrary.CouponFragment.CouponListener;

/**
 * 
 * @author Kirill Rakhman
 */
public class CouponActivity extends SherlockFragmentActivity implements
		CouponListener {

	public static final int RESULT_REDEEMED = 1;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final ActionBar ab = getSupportActionBar();
		ab.setDisplayHomeAsUpEnabled(true);

		final CouponFragment frag = new CouponFragment();
		frag.setArguments(getIntent().getExtras());
		getSupportFragmentManager().beginTransaction()
				.replace(android.R.id.content, frag).commit();

	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			finish();
		}

		return false;
	}

	@Override
	public void couponRedeemed() {
		setResult(RESULT_REDEEMED);
		finish();
	}

}
