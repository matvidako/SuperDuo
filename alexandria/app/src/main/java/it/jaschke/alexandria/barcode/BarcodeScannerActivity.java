package it.jaschke.alexandria.barcode;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import java.util.Arrays;

import me.dm7.barcodescanner.zbar.BarcodeFormat;
import me.dm7.barcodescanner.zbar.Result;
import me.dm7.barcodescanner.zbar.ZBarScannerView;

public class BarcodeScannerActivity extends Activity implements ZBarScannerView.ResultHandler {

    public static final String EXTRA_CONTENTS = "EXTRA_CONTENTS";
    public static int REQUEST_SCAN = 145;

    private ZBarScannerView mScannerView;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        mScannerView = new ZBarScannerView(this);
        mScannerView.setFormats(Arrays.asList(BarcodeFormat.EAN13));
        setContentView(mScannerView);
    }

    @Override
    public void onResume() {
        super.onResume();
        mScannerView.setResultHandler(this);
        mScannerView.startCamera();
    }

    @Override
    public void onPause() {
        super.onPause();
        mScannerView.stopCamera();
    }

    @Override
    public void handleResult(Result rawResult) {
        Log.v("DISI", rawResult.getContents());
        Log.v("DISI", rawResult.getBarcodeFormat().getName());

        Intent returnIntent = new Intent();
        returnIntent.putExtra(EXTRA_CONTENTS, rawResult.getContents());
        setResult(Activity.RESULT_OK, returnIntent);
        finish();
    }

}