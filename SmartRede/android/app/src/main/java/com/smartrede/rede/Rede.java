package com.smartrede.rede;

import static java.lang.Math.ceil;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import br.com.gertec.gedi.GEDI;
import br.com.gertec.gedi.enums.GEDI_PRNTR_e_Alignment;
import br.com.gertec.gedi.enums.GEDI_PRNTR_e_Status;
import br.com.gertec.gedi.exceptions.GediException;
import br.com.gertec.gedi.interfaces.ICL;
import br.com.gertec.gedi.interfaces.IGEDI;
import br.com.gertec.gedi.interfaces.IPRNTR;
import br.com.gertec.gedi.structs.GEDI_PRNTR_st_PictureConfig;
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

    private static final int WIDTH = 400;

    // CLASSE DE IMPRESSÃO
    private IGEDI iGedi = null;
    private IPRNTR iPrint = null;
    private ICL icl = null;

    private GEDI_PRNTR_st_PictureConfig pictureConfig;
    private GEDI_PRNTR_e_Status status;
    private ConfigPrint configPrint;

    //constructor
    public Rede(ReactApplicationContext reactContext) {
        super(reactContext);
        redePayments = RedePayments.getInstance(reactContext);
        reactContext.addActivityEventListener(this);

        new Thread(() -> {
            GEDI.init(reactContext);
            this.iGedi = GEDI.getInstance(reactContext);
            this.iPrint = this.iGedi.getPRNTR();
            try {
                new Thread().sleep(250);
                icl = GEDI.getInstance().getCL();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
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
        try {
            printerInit();
            promise.resolve(this.iPrint.Status().toString());
        } catch (GediException e) {
            promise.reject(null, e.getMessage());
        }
    }

    public void printerInit() throws GediException {
        try {
            if (this.iPrint != null && !isPrintInit) {
                this.icl.PowerOff();
                this.iPrint.Init();
                isPrintInit = true;
            }
        } catch (GediException e) {
            Log.d(e.getMessage(), e.getErrorCode().toString());
        }
    }

    public boolean isPrintOK() {
        if (status.getValue() == 0) {
            return true;
        }
        return false;
    }

    @ReactMethod
    public void print(String imgBase64, @NonNull Promise promise) throws GediException{
        Bitmap bmp;
        byte[] imageByte;
        try {
            imageByte = Base64.decode(imgBase64, Base64.DEFAULT);
            bmp = BitmapFactory.decodeByteArray(imageByte, 0, imageByte.length);
            pictureConfig = new GEDI_PRNTR_st_PictureConfig();
            pictureConfig.alignment = GEDI_PRNTR_e_Alignment.CENTER;
            pictureConfig.height = (int) ceil((WIDTH * bmp.getHeight()) / bmp.getWidth());
            pictureConfig.width = WIDTH;
            printerInit();
            this.iPrint.DrawPictureExt(pictureConfig,bmp);
            this.advanceLine(100);
            printOutput();
            promise.resolve("Success");
        }catch (IllegalArgumentException e){
            promise.reject(null, e.getMessage());
        } catch (GediException e) {
            promise.reject(null, e.getMessage());
        }
    }

    public void printOutput() throws GediException {
        try {
            if( this.iPrint != null  ){
                this.iPrint.Output();
                isPrintInit = false;
            }
        } catch (GediException e) {
            e.printStackTrace();
            throw new GediException(e.getErrorCode());
        }
    }

    public void advanceLine(int row) throws GediException {
        try {
            if(row > 0){
                this.iPrint.DrawBlankLine(row);
            }
        } catch (GediException e) {
            throw new GediException(e.getErrorCode());
        }
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
