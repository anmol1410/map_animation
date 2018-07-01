package com.android.anmol.mapanimation;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatDialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;

import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class ConfigurationFragment extends AppCompatDialogFragment implements View.OnClickListener {

    private static final String FROM_LOCATION_KEY = "FROM_LOCATION_KEY";
    private static String TO_LOCATION_KEY = "TO_LOCATION_KEY";

    private OnConfigurationCallback listener;

    private TextInputLayout mTilFromLoc;
    private EditText mEtFromLoc;
    private TextInputLayout mTilToLoc;
    private EditText mEtToLoc;

    public static ConfigurationFragment getConfFragment(String fromLoc, String toLoc) {
        ConfigurationFragment configurationFragment = new ConfigurationFragment();
        Bundle bundle = new Bundle(2);
        bundle.putString(FROM_LOCATION_KEY, fromLoc);
        bundle.putString(TO_LOCATION_KEY, toLoc);
        configurationFragment.setArguments(bundle);
        return configurationFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.DialogStyle);
    }

    @NonNull
    @Override
    public final View onCreateView(final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.configuration_fragment_layout, container, false);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        }
        return dialog;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mTilFromLoc = view.findViewById(R.id.til_from_loc);
        mTilToLoc = view.findViewById(R.id.til_to_loc);

        mEtFromLoc = view.findViewById(R.id.et_from_loc);
        mEtToLoc = view.findViewById(R.id.et_to_loc);

        if (getArguments() != null) {
            String fromLocText = getArguments().getString(FROM_LOCATION_KEY);
            mEtFromLoc.setText(fromLocText);
            if (fromLocText != null) {
                mEtFromLoc.setSelection(fromLocText.length());
            }

            String toLocText = getArguments().getString(TO_LOCATION_KEY);
            mEtToLoc.setText(toLocText);
        }

        view.findViewById(R.id.btn_animate).setOnClickListener(this);
        view.findViewById(R.id.iv_close).setOnClickListener(this);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listener = (OnConfigurationCallback) context;
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(context + " must implement OnConfigurationCallback");
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_animate:
                Resources res = v.getResources();
                if (res == null) {
                    dismiss();
                    return;
                }
                String fromLocText = mEtFromLoc.getText().toString();
                String toLocText = mEtToLoc.getText().toString();

                LatLng fromLoc = getLatLngFrom(fromLocText);
                LatLng toLoc = getLatLngFrom(toLocText);
                mTilFromLoc.setError(null);
                mTilToLoc.setError(null);

                if (fromLoc == null) {
                    mTilFromLoc.setError(res.getString(R.string.err_invalid_from_loc));
                    mTilFromLoc.requestFocus();
                    return;
                } else if (toLoc == null) {
                    mTilToLoc.setError(res.getString(R.string.err_invalid_to_loc));
                    mTilToLoc.requestFocus();
                    return;
                }

                listener.onLocationsEntered(fromLoc, toLoc, fromLocText, toLocText);
            case R.id.iv_close:
                dismiss();
                break;
        }
    }

    /**
     * This  involves the network interaction.
     * So DON"T DO it on main thread.
     * <p>
     * It is done here on main thread for simplicity.
     *
     * @param location Location to get the lat and lng from.
     * @return Lat Lng values corresponding to the location.
     */
    private LatLng getLatLngFrom(String location) {
        LatLng res = null;
        if (location != null && !location.isEmpty()) {
            Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocationName(location, 1);
                if (addresses != null && addresses.size() != 0) {
                    Address add = addresses.get(0);
                    res = new LatLng(add.getLatitude(), add.getLongitude());
                }
            } catch (IOException e) {
                Log.e(ConfigurationFragment.class.getSimpleName(), e.getMessage());
            }
        }
        return res;
    }
}
