package com.smartrede.rede;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

import rede.smartrede.sdk.FlexTipoPagamento;
import rede.smartrede.sdk.Payment;
import rede.smartrede.sdk.PaymentIntentBuilder;
import rede.smartrede.sdk.PaymentStatus;
import rede.smartrede.sdk.Receipt;
import rede.smartrede.sdk.RedePaymentValidationError;
import rede.smartrede.sdk.RedePayments;

public class Rede extends ReactContextBaseJavaModule implements ActivityEventListener {

    private static final int PAYMENT_REQUEST_CODE = 1001;
    private static final int REVERSAL_REQUEST_CODE = 1002;
    private static final int REPRINT_REQUEST_CODE = 1003;

    private RedePayments redePayments;
    private Promise mPaymentPromise;

    //constructor
    public Rede(ReactApplicationContext reactContext) {
        super(reactContext);
        redePayments = RedePayments.getInstance(reactContext);
        reactContext.addActivityEventListener(this);

    }

    //Mandatory function getName that specifies the module name
    @Override
    public String getName() {
        return "Rede";
    }

    public Map<String, Object> getConstants() {
        Map<String, Object> constants = new HashMap<>();
        constants.put("PIX", FlexTipoPagamento.PIX.toString());
        constants.put("CREDITO_A_VISTA", FlexTipoPagamento.CREDITO_A_VISTA.toString());
        constants.put("CREDITO_PARCELADO", FlexTipoPagamento.CREDITO_PARCELADO.toString());
        constants.put("CREDITO_PARCELADO_EMISSOR", FlexTipoPagamento.CREDITO_PARCELADO_EMISSOR.toString());
        constants.put("DEBITO", FlexTipoPagamento.DEBITO.toString());
        constants.put("VOUCHER", FlexTipoPagamento.VOUCHER.toString());

        constants.put("FAILED", PaymentStatus.FAILED.toString());
        constants.put("DECLINED", PaymentStatus.DECLINED.toString());
        constants.put("FAILED", PaymentStatus.FAILED.toString());

        return constants;
    }

    @ReactMethod
    public void show(String text) {
        Context context = getReactApplicationContext();
        Toast.makeText(context, text, Toast.LENGTH_LONG).show();
    }

    @ReactMethod
    public void payment(String type, int value, int installments, Promise promise) {
        Activity currentActivity = getCurrentActivity();
        try {
            FlexTipoPagamento paymentType = FlexTipoPagamento.valueOf(type);
            PaymentIntentBuilder paymentIntentBuilder = redePayments.intentForPaymentBuilder(paymentType, value);
            if (installments > 1) paymentIntentBuilder.setInstallments(installments);
            Intent intent = paymentIntentBuilder.build();
            currentActivity.startActivityForResult(intent, PAYMENT_REQUEST_CODE);
            mPaymentPromise = promise;
        } catch (ActivityNotFoundException | RedePaymentValidationError e) {
            Logger.getLogger(e.getMessage());
            promise.reject("error", e.getMessage());
        }
    }

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, @Nullable Intent intent) {
        Context context = getReactApplicationContext();
        if (requestCode == PAYMENT_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                if (intent != null) {
                    Payment payment = RedePayments.getPaymentFromIntent(intent);
                    if (payment.getStatus() == PaymentStatus.AUTHORIZED) {
                        Receipt receipt = payment.getReceipt();
                        final WritableMap map = Arguments.createMap();
                        map.putString("retCode", payment.getReceipt().getNSU());
                        map.putString("transactionCode", payment.getTransactionNumber());
                        map.putString("transactionId", payment.getTransactionNumber());
                        map.putString("message", payment.getStatus().toString());
                        map.putString("nsu", receipt.getNSU());
                        mPaymentPromise.resolve(map);
                    } else if (payment.getStatus() == PaymentStatus.FAILED) {
                        mPaymentPromise.reject(null, "Pagamento Falhou");
                    } else if (payment.getStatus() == PaymentStatus.DECLINED) {
                        mPaymentPromise.reject(null, "Pagamento Recusado");
                    }
                }
            } else {
                mPaymentPromise.reject(null, "Pagamento Cancelado pelo operador");
            }
        } else {
            mPaymentPromise.reject(null, "ELSE");
        }
    }


    @Override
    public void onNewIntent(Intent intent) {

    }
}
