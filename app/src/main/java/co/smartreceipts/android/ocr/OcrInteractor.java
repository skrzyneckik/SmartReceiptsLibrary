package co.smartreceipts.android.ocr;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.google.common.base.Preconditions;

import java.io.File;

import co.smartreceipts.android.apis.hosts.ServiceManager;
import co.smartreceipts.android.aws.s3.S3Manager;
import co.smartreceipts.android.ocr.apis.OcrService;
import co.smartreceipts.android.ocr.apis.model.OcrResponse;
import co.smartreceipts.android.ocr.push.OcrPushMessageReceiver;
import co.smartreceipts.android.push.PushManager;
import co.smartreceipts.android.utils.Feature;
import co.smartreceipts.android.utils.FeatureFlags;
import co.smartreceipts.android.utils.UriUtils;
import co.smartreceipts.android.utils.log.Logger;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import rx.Observable;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class OcrInteractor {

    private static final String OCR_FOLDER = "ocr/";
    private static final String PART_NAME = "file";

    private final Context context;
    private final S3Manager s3Manager;
    private final PushManager pushManager;
    private final ServiceManager ocrServiceManager;
    private final OcrPushMessageReceiver pushMessageReceiver;
    private final Feature ocrFeature;

    public OcrInteractor(@NonNull Context context, @NonNull S3Manager s3Manager, @NonNull ServiceManager serviceManager, @NonNull PushManager pushManager) {
        this(context, s3Manager, pushManager, serviceManager, new OcrPushMessageReceiver(), FeatureFlags.Ocr);
    }

    @VisibleForTesting
    OcrInteractor(@NonNull Context context, @NonNull S3Manager s3Manager, @NonNull PushManager pushManager,
                  @NonNull ServiceManager serviceManager, @NonNull OcrPushMessageReceiver pushMessageReceiver, @NonNull Feature ocrFeature) {
        this.context = Preconditions.checkNotNull(context.getApplicationContext());
        this.s3Manager = Preconditions.checkNotNull(s3Manager);
        this.pushManager = Preconditions.checkNotNull(pushManager);
        this.ocrServiceManager = Preconditions.checkNotNull(serviceManager);
        this.pushMessageReceiver = Preconditions.checkNotNull(pushMessageReceiver);
        this.ocrFeature = Preconditions.checkNotNull(ocrFeature);
    }

    @NonNull
    public Observable<OcrResponse> scan(@NonNull File file) {
        Preconditions.checkNotNull(file);

        if (ocrFeature.isEnabled()) {
            Logger.info(OcrInteractor.this, "Initiating scan of {}.", file);
            final String mimeType = UriUtils.getMimeType(Uri.fromFile(file), context.getContentResolver());
            final RequestBody requestBody = RequestBody.create(MediaType.parse(mimeType), file);
            final MultipartBody.Part filePart = MultipartBody.Part.createFormData(PART_NAME, file.getName(), requestBody);
            return s3Manager.upload(file, OCR_FOLDER)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .flatMap(new Func1<String, Observable<OcrResponse>>() {
                        @Override
                        public Observable<OcrResponse> call(String s) {
                            return ocrServiceManager.getService(OcrService.class).scanReceipt(filePart);
                        }
                    })
                    .doOnSubscribe(new Action0() {
                        @Override
                        public void call() {
                            pushManager.registerReceiver(pushMessageReceiver);
                        }
                    })
                    .onErrorReturn(new Func1<Throwable, OcrResponse>() {
                        @Override
                        public OcrResponse call(Throwable throwable) {
                            return new OcrResponse();
                        }
                    })
                    .flatMap(new Func1<OcrResponse, Observable<OcrResponse>>() {
                        @Override
                        public Observable<OcrResponse> call(OcrResponse ocrResponse) {
                            // TODO: Here's where we should wait for the push result to come in to validate everything
                            // TODO: Also include a timeout here so it doesn't take more than say 7 seconds or so
                            return Observable.just(ocrResponse);
                        }
                    })
                    .doOnTerminate(new Action0() {
                        @Override
                        public void call() {
                            pushManager.unregisterReceiver(pushMessageReceiver);
                        }
                    });
        } else {
            Logger.debug(OcrInteractor.this, "Ocr is disabled. Ignoring scan");
            return Observable.just(new OcrResponse());
        }
    }
}
