package com.mackwell.bluetoothtest;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

/**
 * Created by Weiyuan on 06/05/2015.
 * Test dialogfragment for display Serial Number and GTIN
 */
public class TestDialogFragment extends DialogFragment {

    int GTIN = 32145;
    int SN = 12345;
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage( "GTIN: " + GTIN + "\n" + "SN: " + SN)
                .setTitle("Device info received")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        return builder.create();
    }
}
