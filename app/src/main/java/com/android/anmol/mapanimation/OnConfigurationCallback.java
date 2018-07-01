package com.android.anmol.mapanimation;

import com.google.android.gms.maps.model.LatLng;

interface OnConfigurationCallback {

    void onLocationsEntered(LatLng fromLoc, LatLng toLoc);

    void onCancancelled();
}
