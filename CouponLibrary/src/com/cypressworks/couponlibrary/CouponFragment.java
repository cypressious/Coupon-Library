package com.cypressworks.couponlibrary;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockListFragment;

/**
 * Fragment which handles the user interaction for redeeming and checking
 * coupons. It is necessary to supply the following arguments via
 * {@link #setArguments(Bundle)} to the fragment:<br>
 * <br>
 * <b>baseUrl</b>: a string containing the url of your backend<br>
 * <b>couponName</b>: a string containing the name of the coupon, this must be
 * the same name that is used in the backend<br>
 * <b>salt</b>: a byte array for encrypting the preference file
 * 
 * @author Kirill Rakhman
 */
public class CouponFragment extends SherlockListFragment implements
		OnClickListener {
	private Account[] accounts;
	private TextView editTextCoupon;
	private ListView listView;
	private View buttonUnlock;
	private View buttonRedeem;
	private View layout;
	private Activity activity;
	private CouponManager couponManager;

	@Override
	public void onAttach(final Activity activity) {
		this.activity = activity;
		super.onAttach(activity);
	}

	@Override
	public void onDetach() {
		super.onDetach();
		activity = null;
	}

	public void setActivity(final Activity activity) {
		this.activity = activity;
	}

	@Override
	public ListView getListView() {
		if (listView != null) {
			return listView;
		} else {
			return super.getListView();
		}
	}

	public void setListView(final ListView listView) {
		this.listView = listView;
	}

	@Override
	public void setListAdapter(final ListAdapter adapter) {
		getListView().setAdapter(adapter);
	}

	@Override
	public View onCreateView(final LayoutInflater inflater,
			final ViewGroup container, final Bundle savedInstanceState) {
		layout = inflater.inflate(R.layout.coupon_layout, container, false);
		buttonUnlock = layout.findViewById(R.id.buttonCouponUnlock);
		buttonRedeem = layout.findViewById(R.id.buttonCouponRedeem);

		editTextCoupon = (TextView) layout.findViewById(R.id.editTextCoupon);
		return layout;
	}

	@Override
	public void onActivityCreated(final Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		final Bundle args = getArguments();

		if (args == null) {
			throw new IllegalArgumentException("Argument bundle is null");
		}

		final String baseUrl = args.getString("baseUrl");
		final String couponName = args.getString("couponName");
		final byte[] salt = args.getByteArray("salt");

		couponManager = new CouponManager(baseUrl, couponName, salt);

		final AccountManager am = AccountManager.get(getActivity());
		accounts = am.getAccountsByType("com.google");

		// simple adapter that enlarges the items
		setListAdapter(new ArrayAdapter<String>(activity,
				android.R.layout.simple_list_item_single_choice) {

			@Override
			public int getCount() {
				return accounts.length;
			}

			@Override
			public String getItem(final int position) {
				return accounts[position].name;
			}

			@Override
			public View getView(final int position, final View convertView,
					final ViewGroup parent) {
				final TextView view = (TextView) super.getView(position,
						convertView, parent);
				view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
				return view;
			}
		});

		listView = getListView();

		listView.setItemsCanFocus(false);
		listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

		editTextCoupon.setOnKeyListener(new OnKeyListener() {
			private final InputMethodManager imm = (InputMethodManager) activity
					.getSystemService(Context.INPUT_METHOD_SERVICE);

			@Override
			public boolean onKey(final View v, final int keyCode,
					final KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_ENTER) {
					redeem();
					imm.hideSoftInputFromWindow(
							editTextCoupon.getWindowToken(), 0);
					return true;
				}

				return false;
			}
		});

		buttonUnlock.setOnClickListener(this);
		buttonRedeem.setOnClickListener(this);

		if (accounts.length > 0) {
			listView.setSelection(0);
			listView.setItemChecked(0, true);

			buttonUnlock.setEnabled(true);
			buttonRedeem.setEnabled(true);

			// hide error message
			layout.findViewById(R.id.textViewCouponNoAccount).setVisibility(
					View.GONE);
		} else {
			// Hide control elements and only show error message
			buttonUnlock.setVisibility(View.GONE);
			buttonRedeem.setVisibility(View.GONE);
			editTextCoupon.setVisibility(View.GONE);
			layout.findViewById(R.id.textViewCouponSelectAccount)
					.setVisibility(View.GONE);
			listView.setVisibility(View.GONE);
		}
	}

	@Override
	public void onClick(final View v) {
		final int id = v.getId();

		if (id == R.id.buttonCouponUnlock) {
			check();
		} else if (id == R.id.buttonCouponRedeem) {
			redeem();
		}
	}

	/**
	 * Checks all of the available accounts. That's less error-prone than
	 * letting the user select the account to check and even if he does, it does
	 * no harm.
	 */
	private void check() {
		// Counter for checked accounts
		final AtomicInteger counter = new AtomicInteger(0);
		final AtomicBoolean redeemed = new AtomicBoolean(false);

		final ProgressDialog pd = showProgressDialog(R.string.coupon_checking);

		couponManager.checkUserAsync(activity, new RedemptionCallback() {

			@Override
			public void onCallback(final Account acc, final boolean result) {
				final int count = counter.addAndGet(1);

				if (result) {
					redeemed.set(true);
					toast(R.string.coupon_checking_success);
				}

				if (count == accounts.length) {
					try {
						pd.dismiss();
					} catch (final Exception e) {
						e.printStackTrace();
						// Hässlich aber funktioniert. Es geht nur um den Dialog
						// und wenn da irgendwas wirft, sollte der Task trotzdem
						// ordentlich abgearbeitet werden.
					}

					if (!redeemed.get()) {
						toast(R.string.coupon_checking_failure);
					} else {
						if (activity != null) {
							((CouponListener) activity).couponRedeemed();
						}
					}
				}
			}
		});
	}

	/**
	 * Tries to redeem the code for the selected account.
	 */
	private void redeem() {

		final String coupon = editTextCoupon.getText().toString().trim();

		if (coupon.equals("")) {
			toast(R.string.coupon_no_coupon);
		} else {
			final Account acc = accounts[listView.getCheckedItemPosition()];
			final ProgressDialog pd = showProgressDialog(R.string.coupon_redeeming);

			couponManager.redeemCouponAsync(activity, acc, coupon,
					new RedemptionCallback() {

						@Override
						public void onCallback(final Account acc,
								final boolean result) {
							try {
								pd.dismiss();
							} catch (final Exception e) {
								// Don't let the execution flow be interrupted
								// by Dialog errors.
							}

							if (result) {
								toast(R.string.coupon_redeeming_success);

								if (activity != null) {
									((CouponListener) activity)
											.couponRedeemed();
								}
							} else {
								toast(R.string.coupon_redeeming_success);
							}
						}
					});

		}
	}

	private void toast(final int res) {
		final Context context = activity;
		if (context != null) {
			Toast.makeText(context, res, Toast.LENGTH_LONG).show();
		}
	}

	private ProgressDialog showProgressDialog(final int message) {
		final ProgressDialog pd = new ProgressDialog(activity);
		pd.setIndeterminate(true);
		pd.setMessage(activity.getString(message));
		pd.setCancelable(false);
		pd.setCanceledOnTouchOutside(false);
		pd.show();

		return pd;
	}

	public static interface CouponListener {
		void couponRedeemed();
	}
}
