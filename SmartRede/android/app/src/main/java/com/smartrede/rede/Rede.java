package com.smartrede.rede;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import br.com.gertec.gedi.enums.GEDI_PRNTR_e_Status;
import br.com.gertec.gedi.exceptions.GediException;
import br.com.gertec.gedi.interfaces.ICL;
import br.com.gertec.gedi.interfaces.IPRNTR;
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
    private static boolean isPrintInit = false;
    private RedePayments redePayments;
    private Promise mPaymentPromise;
    private int countImages = 0;
    private int countPrint = 0;


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

    @ReactMethod
    public void reversal(Promise promise) {
        try {
            Activity currentActivity = getCurrentActivity();
            Intent reversal = redePayments.intentForReversal();
            currentActivity.startActivityForResult(reversal, REVERSAL_REQUEST_CODE);
            mPaymentPromise = promise;
        } catch (ActivityNotFoundException e) {
            promise.reject("error", e.getMessage());
        }
    }

    @ReactMethod
    public void reprint(Promise promise) {
        try {
            Activity currentActivity = getCurrentActivity();
            Intent reprint = redePayments.intentForReprint();
            currentActivity.startActivityForResult(reprint, REPRINT_REQUEST_CODE);
            mPaymentPromise = promise;
        } catch (ActivityNotFoundException e) {
            promise.reject("error", e.getMessage());
        }
    }
    

    @ReactMethod
    public void getStatusPrint(final Promise promise) throws GediException {
        promise.resolve("00000");
    }

    @ReactMethod
    public void print(final Promise promise) throws IOException, Exception {
        countPrint = 0;
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Download/");
        File[] arquivos = file.listFiles();
        countImages = arquivos.length;
        Log.v("LOG133", String.valueOf(countImages));
        promise.resolve(String.valueOf(countImages));

        //Print Data

    }


    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, @Nullable Intent intent) {
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
        } else if (requestCode == REVERSAL_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                if (intent != null) {
                    Payment payment = RedePayments.getPaymentFromIntent(intent);
                    if (payment.getStatus() == PaymentStatus.AUTHORIZED) {
                        mPaymentPromise.resolve("Autorizado");
                    } else if (payment.getStatus() == PaymentStatus.DECLINED) {
                        mPaymentPromise.reject(null, "Reembolso Recusado");
                    } else if (payment.getStatus() == PaymentStatus.FAILED) {
                        mPaymentPromise.reject(null, "Reembolso Recusado");
                    }
                }
            } else {
                mPaymentPromise.reject(null, "Reembolso Cancelado pelo operador");
            }
        } else if (requestCode == REPRINT_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                mPaymentPromise.resolve("Reimpressão feita com sucesso");
            } else {
                mPaymentPromise.reject(null, "Reimpressão Cancelada");
            }
        }
    }


    @Override
    public void onNewIntent(Intent intent) {

    }
}
