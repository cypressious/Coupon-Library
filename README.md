Coupon-Library
==============

Redeemable coupons for your android application with App Engine backend

Backend
==============

The backend is powered by App Engine. Just create a new project, enter your application id and upload it.

To generate coupons open your App Engine project in a browser. The url looks like this
> http://%your-project-id%.appspot.com/coupons?app=%coupon_name%

Libray
==============

To use the library, add it as a library project to your Android application. You can either use the available
CouponFragment (or the native wrapper fragment) or write the UI yourself and directly use the CouponManager class.
