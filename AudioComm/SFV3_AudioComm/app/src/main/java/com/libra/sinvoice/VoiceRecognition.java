package com.libra.sinvoice;

import android.util.Log;

import com.libra.sinvoice.Buffer.BufferData;

import java.util.ArrayList;
import java.util.List;

public class VoiceRecognition {
    private final static String TAG = "Recognition";

    private final static int STATE_START = 1;
    private final static int STATE_STOP = 2;
    private final static int STEP1 = 1;
    private final static int STEP2 = 2;

    private FFT fft = new FFT();

    private int mState;
    private Listener mListener;
    private Callback mCallback;

    private int mSamplingPointCount = 0;

    private int mSampleRate;
    private int mChannel;
    private int mBits;

    private boolean mIsStartCounting = false;
    private int mStep;
    private boolean mIsBeginning = false;
    private boolean mStartingDet = false;
    private int mStartingDetCount;


    private int mPreRegCircle1 = 0;


    public static interface Listener {
        void onStartRecognition();

        void onRecognition(String str);

        void onStopRecognition();
    }

    public static interface Callback {
        BufferData getRecognitionBuffer();

        void freeRecognitionBuffer(BufferData buffer);
    }

    public VoiceRecognition(Callback callback, int SampleRate, int channel, int bits) {
        mState = STATE_STOP;

        mCallback = callback;
        mSampleRate = SampleRate;
        mChannel = channel;
        mBits = bits;
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public void start() {
        if (STATE_STOP == mState) {

            if (null != mCallback) {
                mState = STATE_START;
                mSamplingPointCount = 0;

                mIsStartCounting = false;
                mStep = STEP1;
                mIsBeginning = false;
                mStartingDet = false;
                mStartingDetCount = 0;
                mPreRegCircle1 = -1;

                if (null != mListener) {
                    mListener.onStartRecognition();
                }
                while (STATE_START == mState) {
                    BufferData data = mCallback.getRecognitionBuffer();
                    if (null != data) {
                        if (null != data.mData) {
                            process(data);

                            mCallback.freeRecognitionBuffer(data);
                        } else {
                            LogHelper.d(TAG, "end input buffer, so stop");
                            break;
                        }
                    } else {
                        LogHelper.e(TAG, "get null recognition buffer");
                        break;
                    }
                }

                mState = STATE_STOP;
                if (null != mListener) {
                    mListener.onStopRecognition();
                }
            }
        }
    }

    public void stop() {
        if (STATE_START == mState) {
            mState = STATE_STOP;
        }
    }

    private void process(BufferData data) {
        int size = data.getFilledSize() - 1;

        double[] dataBlock = new double[(int) size / 2 + 1];

        double[] partData1 = new double[256];
        double[] partData2 = new double[256];
        double[] partData3 = new double[256];
        double[] partData4 = new double[256];
        double[] partData5 = new double[256];
        double[] partData6 = new double[256];
        double[] partData7 = new double[256];
        double[] partData8 = new double[256];
        double[] partSpectrum1;
        double[] partSpectrum2;
        double[] partSpectrum3;
        double[] partSpectrum4;
        double[] partSpectrum5;
        double[] partSpectrum6;
        double[] partSpectrum7;
        double[] partSpectrum8;
        double sh;
        //Can be combined
        for (int i = 0; i < size; i++) {
            short sh1 = data.mData[i];
            sh1 &= 0xff;
            short sh2 = data.mData[++i];
            sh2 <<= 8;
            sh = (double) ((sh1) | (sh2));
            dataBlock[(i - 1) / 2] = sh;
        }

//        for(int i = 0;i<8;i++){
//            for(int j = 0;j<256;j++){
//                sepDate[j] = dataBlock[i*256+j];
//            }
//            sepDateList.add((Double[])sepDate);
//            fft.magnitudeSpectrum((double)sepDate)
//            sepDateSpec.add();
//        }
//        for(int i =0;i<8;i++){
//            sepDate =
//            sepDateSpec =
//        }

        for (int i = 0; i < 256; i++) {
            partData1[i] = dataBlock[i];
        }
        for (int i = 0; i < 256; i++) {
            partData2[i] = dataBlock[i + 256];
        }
        for (int i = 0; i < 256; i++) {
            partData3[i] = dataBlock[i + 512];
        }
        for (int i = 0; i < 256; i++) {
            partData4[i] = dataBlock[i + 768];
        }
        for (int i = 0; i < 256; i++) {
            partData5[i] = dataBlock[i + 1024];
        }
        for (int i = 0; i < 256; i++) {
            partData6[i] = dataBlock[i + 1280];
        }
        for (int i = 0; i < 256; i++) {
            partData7[i] = dataBlock[i + 1536];
        }
        for (int i = 0; i < 256; i++) {
            partData8[i] = dataBlock[i + 1791];
        }
        partSpectrum1 = fft.magnitudeSpectrum(partData1);
        partSpectrum2 = fft.magnitudeSpectrum(partData2);
        partSpectrum3 = fft.magnitudeSpectrum(partData3);
        partSpectrum4 = fft.magnitudeSpectrum(partData4);
        partSpectrum5 = fft.magnitudeSpectrum(partData5);
        partSpectrum6 = fft.magnitudeSpectrum(partData6);
        partSpectrum7 = fft.magnitudeSpectrum(partData7);
        partSpectrum8 = fft.magnitudeSpectrum(partData8);

//        spectrum = fft.magnitudeSpectrum(dataBlock);
//        double Max1 = spectrum[792];
        Log.d("STOP","end Point");

        double partMax1 = 0;
        double partMax2 = 0;
        double partMax3 = 0;
        double partMax4 = 0;
        double partMax5 = 0;
        double partMax6 = 0;
        double partMax7 = 0;
        double partMax8 = 0;
        int partFreq1 = 0;
        int partFreq2 = 0;
        int partFreq3 = 0;
        int partFreq4 = 0;
        int partFreq5 = 0;
        int partFreq6 = 0;
        int partFreq7 = 0;
        int partFreq8 = 0;

        for (int i = 95; i < 118; i++) {
            if (partMax1 < partSpectrum1[i]&&partSpectrum1[i]>10000) {
                partMax1 = partSpectrum1[i];
                partFreq1 = detect(i);
            }
        }
        for (int i = 95; i < 118; i++) {
            if (partMax2 < partSpectrum2[i]&&partSpectrum2[i]>10000) {
                partMax2 = partSpectrum2[i];
                partFreq2 = detect(i);
            }
        }
        for (int i = 95; i < 118; i++) {
            if (partMax3 < partSpectrum3[i]&&partSpectrum3[i]>10000) {
                partMax3 = partSpectrum3[i];
                partFreq3 = detect(i);
            }
        }
        for (int i = 95; i < 118; i++) {
            if (partMax4 < partSpectrum4[i]&&partSpectrum4[i]>10000) {
                partMax4 = partSpectrum4[i];
                partFreq4 = detect(i);
            }
        }
        for (int i = 95; i < 118; i++) {
            if (partMax5 < partSpectrum5[i]&&partSpectrum5[i]>10000) {
                partMax5 = partSpectrum5[i];
                partFreq5 = detect(i);
            }
        }
        for (int i = 95; i < 118; i++) {
            if (partMax6 < partSpectrum6[i]&&partSpectrum6[i]>10000) {
                partMax6 = partSpectrum6[i];
                partFreq6 = detect(i);
            }
        }
        for (int i = 95; i < 118; i++) {
            if (partMax7 < partSpectrum7[i]&&partSpectrum7[i]>10000) {
                partMax7 = partSpectrum7[i];
                partFreq7 = detect(i);
            }
        }
        for (int i = 95; i < 118; i++) {
            if (partMax8 < partSpectrum8[i]&&partSpectrum8[i]>10000) {
                partMax8 = partSpectrum8[i];
                partFreq8 = detect(i);
            }
        }
        Log.d("Part Detection", "part1: " + partFreq1 + "; part2: " + partFreq2 + "; part3: " + partFreq3 + "; \n" +
                "part4: " + partFreq4 + "; part5: " + partFreq5 + "; part6: " + partFreq6 + "; part7: " + partFreq7 + "; part8: " + partFreq8);

        ArrayList<Integer> result = new ArrayList<Integer>();

        if(partFreq1!=mPreRegCircle1&&partFreq1!=-1){
            if (partFreq1 == 5) {
                result.add(mPreRegCircle1);
            }
            else{
                result.add(partFreq1);
            }
            mPreRegCircle1 = partFreq1;
        }
        if(partFreq2!=mPreRegCircle1&&partFreq2!=-1){
            if (partFreq2 == 5) {
                result.add(mPreRegCircle1);
            }
            else{
                result.add(partFreq2);
            }
            mPreRegCircle1 = partFreq2;
        }
        if(partFreq3!=mPreRegCircle1&&partFreq3!=-1){
            if (partFreq3 == 5) {
                result.add(mPreRegCircle1);
            }
            else{
                result.add(partFreq3);
            }
            mPreRegCircle1 = partFreq3;
        }
        if(partFreq4!=mPreRegCircle1&&partFreq4!=-1){
            if (partFreq4 == 5) {
                result.add(mPreRegCircle1);
            }
            else{
                result.add(partFreq4);
            }
            mPreRegCircle1 = partFreq4;
        }
        if(partFreq5!=mPreRegCircle1&&partFreq5!=-1){
            if (partFreq5 == 5) {
                result.add(mPreRegCircle1);
            }
            else{
                result.add(partFreq5);
            }
            mPreRegCircle1 = partFreq5;
        }
        if(partFreq6!=mPreRegCircle1&&partFreq6!=-1){
            if (partFreq6 == 5) {
                result.add(mPreRegCircle1);
            }
            else{
                result.add(partFreq6);
            }
            mPreRegCircle1 = partFreq6;
        }
        if(partFreq7!=mPreRegCircle1&&partFreq7!=-1){
            if (partFreq7 == 5) {
                result.add(mPreRegCircle1);
            }
            else{
                result.add(partFreq7);
            }
            mPreRegCircle1 = partFreq7;
        }
        if(partFreq8!=mPreRegCircle1&&partFreq8!=-1) {
            if (partFreq8 == 5) {
                result.add(mPreRegCircle1);
            }
            else{
                result.add(partFreq8);
            }
            mPreRegCircle1 = partFreq8;
        }
        String str = "";
        for(int i = 0;i<result.size();i++){
            str = str+result.get(i);
        }
        if(mListener!=null){
            mListener.onRecognition(str);

        }

}

    public int detect(int num){
        switch (num){
            case 100:
                return 0;
            case 102:
                return 1;
            case 104:
                return 2;
            case 106:
                return 3;
            case 108:
                return 4;
            case 110:
                return 5;
            case 112:
                return 6;
            case 114:
                return 7;
            default:
                return -1;
        }
    }
}
