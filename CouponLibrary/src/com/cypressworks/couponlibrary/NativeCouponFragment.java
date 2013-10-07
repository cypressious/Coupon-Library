package com.cypressworks.couponlibrary;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ListFragment;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Native wrapper for the coupon. This way you can integrate it in your
 * preferences using preference headers.
 * 
 * @author Kirill Rakhman
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class NativeCouponFragment extends ListFragment {
	CouponFragment cp;

	public NativeCouponFragment() {
		cp = new CouponFragment();
		cp.setArguments(getArguments());
	}

	@Override
	public void onAttach(final Activity activity) {
		super.onAttach(activity);
		cp.setActivity(activity);
	}

	@Override
	public void onDetach() {
		super.onDetach();
		cp.setActivity(null);
	}

	@Override
	public View onCreateView(final LayoutInflater inflater,
			final ViewGroup container, final Bundle savedInstanceState) {
		return cp.onCreateView(inflater, container, savedInstanceState);
	}

	@Override
	public void onActivityCreated(final Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		cp.setListView(getListView());
		cp.onActivityCreated(savedInstanceState);
	}
}
