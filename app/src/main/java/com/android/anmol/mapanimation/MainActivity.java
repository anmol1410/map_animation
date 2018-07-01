package com.android.anmol.mapanimation;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.animation.LinearInterpolator;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

public class MainActivity extends AppCompatActivity implements OnConfigurationCallback {

    private GoogleMap mGoogleMap;
    private Marker mFromMarker;
    private Marker mToMarker;
    private Polyline mPolyLine;
    private AnimatorSet mAnimatorSet;
    private String mFromLoc;
    private String mToLoc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Load the map.
        ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMapAsync(new MapReadyListener());
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.configuration_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_action_conf:
                final String CONF_FRAG_TAG = "CONF_FRAG_TAG";
                Fragment fragment = getSupportFragmentManager().findFragmentByTag(CONF_FRAG_TAG);
                ConfigurationFragment confFrag;
                if (fragment != null && fragment instanceof ConfigurationFragment) {
                    confFrag = (ConfigurationFragment) fragment;
                } else {
                    confFrag = ConfigurationFragment.getConfFragment(mFromLoc, mToLoc);
                }
                confFrag.show(getSupportFragmentManager(), CONF_FRAG_TAG);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onLocationsEntered(LatLng fromLoc, LatLng toLoc, String fromLocText, String toLocText) {
        setDefaultMapState();

        mFromLoc = fromLocText;
        mToLoc = toLocText;

        mFromMarker = setMarker(fromLoc);
        mToMarker = setMarker(toLoc);

        int googleMapPadding = getResources().getDimensionPixelOffset(R.dimen.google_map_padding);
        final LatLngBounds bounds = new LatLngBounds.Builder()
                .include(fromLoc)
                .include(toLoc)
                .build();
        mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, googleMapPadding));

        PolylineOptions line = new PolylineOptions()
                .add(fromLoc, toLoc)
                .width(10)
                .color(getResources().getColor(android.R.color.holo_red_dark));
        mPolyLine = mGoogleMap.addPolyline(line);

        startAnimation(fromLoc, toLoc);
    }

    /**
     * Remove all the markers/Lines/ongoing animations from the map.
     */
    private void setDefaultMapState() {

        // Remove markers.
        if (mFromMarker != null) {
            mFromMarker.remove();
        }

        if (mToMarker != null) {
            mToMarker.remove();
        }

        // Remove Lines.
        if (mPolyLine != null) {
            mPolyLine.remove();
        }

        // Stop animations.
        if (mAnimatorSet != null) {
            mAnimatorSet.removeAllListeners();
            mAnimatorSet.end();
            mAnimatorSet.cancel();
        }
    }

    /**
     * Begin the animation.
     *
     * @param fromLoc Animation to start from.
     * @param toLoc   Animation to end at.
     */
    private void startAnimation(LatLng fromLoc, LatLng toLoc) {
        ObjectAnimator animator = (ObjectAnimator) getAnimator(fromLoc, toLoc);
        animator.setRepeatCount(ValueAnimator.INFINITE);

        mAnimatorSet = new AnimatorSet();
        mAnimatorSet.playTogether(animator);
        mAnimatorSet.start();
    }

    /**
     * Animator set with the required properties.
     *
     * @param fromLoc Animator with starting point.
     * @param toLoc   Animator with ending point.
     * @return An Initialized Animator.
     */
    private Animator getAnimator(LatLng fromLoc, LatLng toLoc) {
        final ObjectAnimator objectAnimator = new ObjectAnimator();
        Marker animatorMarker = getAnimatorMarker(fromLoc, toLoc);
        objectAnimator.setTarget(animatorMarker);
        objectAnimator.setPropertyName("position");
        objectAnimator.setObjectValues(fromLoc, toLoc);
        objectAnimator.setInterpolator(new LinearInterpolator());
        objectAnimator.setEvaluator(new PositionEvaluator());
        objectAnimator.setDuration(getAnimationTime(fromLoc, toLoc));
        objectAnimator.setRepeatCount(0);
        objectAnimator.addListener(new OneWayAnimator(animatorMarker));
        return objectAnimator;
    }

    /**
     * Get time for the animation to occur single way.
     *
     * @param fromLoc Location from the animation to start.
     * @param toLoc   Location from the animation to start.
     * @return time for the animation to persist.
     */
    private long getAnimationTime(LatLng fromLoc, LatLng toLoc) {
        final double metreToKilometers = 0.001;

        final float[] dist = new float[1];
        Location.distanceBetween(fromLoc.latitude, fromLoc.longitude, toLoc.latitude, toLoc.longitude, dist);
        return (long) (dist[0] * metreToKilometers);
    }

    /**
     * Get the marker to animate between the FromLoc -> ToLoc.
     *
     * @param fromLoc FromLocation used for rotation of the marker.
     * @param toLoc   To Location used for the rotation of the marker.
     * @return {@link Marker} with the basic requirements.
     */
    Marker getAnimatorMarker(LatLng fromLoc, LatLng toLoc) {
        float[] bearing = new float[2];
        Location.distanceBetween(fromLoc.latitude, fromLoc.longitude, toLoc.latitude, toLoc.longitude, bearing);

        final MarkerOptions markerOptions = new MarkerOptions()
                .position(new LatLng(0, 0))
                .flat(true)
                .visible(false) // Hidden in the starting.
                .anchor(0.5f, 0.5f) // so that it is in the middle of the line.
                .rotation(bearing[1])
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_arrow));
        return mGoogleMap.addMarker(markerOptions);
    }

    /**
     * Add the marker to the Google Map.
     *
     * @param pos Location to add the marker at.
     * @return {@link Marker} added to the google map.
     */
    Marker setMarker(LatLng pos) {
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(pos);
        return mGoogleMap.addMarker(markerOptions);
    }

    /**
     * Handles the animation from fromLoc -> toLoc
     * It shows/hide the marker at the beginning/end of animation respectively.
     */
    private class OneWayAnimator implements Animator.AnimatorListener {
        private Marker mTargetMarker;

        OneWayAnimator(Marker target) {
            mTargetMarker = target;
        }

        @Override
        public void onAnimationStart(Animator animation) {
            // Show the marker at the animation start.
            mTargetMarker.setVisible(true);
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            // Hide the marker at the animation End so that it can be started again.
            mTargetMarker.setVisible(false);
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            // Hide the marker as the animation got cancelled.
            mTargetMarker.setVisible(false);
        }

        @Override
        public void onAnimationRepeat(Animator animation) {
            // Show the marker when animation begins repeat.
            mTargetMarker.setVisible(true);
        }
    }

    private class PositionEvaluator implements TypeEvaluator<LatLng> {

        @Override
        public LatLng evaluate(final float fraction, final LatLng startValue, final LatLng endValue) {
            // Evaluates the lat,lng between the 2 points
            double longitude = fraction * endValue.longitude + (1 - fraction) * startValue.longitude;
            double latitude = fraction * endValue.latitude + (1 - fraction) * startValue.latitude;

            return new LatLng(latitude, longitude);
        }
    }

    /**
     * Callback for the google map to get ready.
     */
    private class MapReadyListener implements OnMapReadyCallback {
        @Override
        public void onMapReady(GoogleMap googleMap) {
            // Once the map is ready, get handle to the google map.
            mGoogleMap = googleMap;
        }
    }
}
