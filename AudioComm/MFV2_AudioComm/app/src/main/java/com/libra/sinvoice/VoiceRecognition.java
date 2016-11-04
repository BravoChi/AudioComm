/*
 * Copyright (C) 2013 gujicheng
 * 
 * Licensed under the GPL License Version 2.0;
 * you may not use this file except in compliance with the License.
 * 
 * If you have any question, please contact me.
 * 
 *************************************************************************
 **                   Author information                                **
 *************************************************************************
 ** Email: gujicheng197@126.com                                         **
 ** QQ   : 29600731                                                     **
 ** Weibo: http://weibo.com/gujicheng197                                **
 *************************************************************************
 */
package com.libra.sinvoice;

import android.util.Log;

import com.libra.sinvoice.Buffer.BufferData;

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
    public FreqPair freqPair;
    public static FreqPair lastPair;

    private int mSamplingPointCount = 0;

    private int mSampleRate;
    private int mChannel;
    private int mBits;

    private boolean mIsStartCounting = false;
    private int mStep;
    private boolean mIsBeginning = false;
    private boolean mStartingDet = false;
    private int mStartingDetCount;

    private int mRegValue;
    private int regFreq1;
    private int regFreq2;
    private int mRegIndex1;
    private int mRegIndex2;
    private int mRegCount;
    private int mPreRegCircle1 = 0;
    private int mPreRegCircle2 = 0;
    private boolean mIsRegStart = false;

    public static interface Listener {
        void onStartRecognition();

        void onRecognition(char ch);

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
                mPreRegCircle2 = -1;
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
        double[] spectrum ;
        int freq1 = 0;
        int freq2 = 0;
        double[] dataBlock = new double[(int)size/2+1];
        double sh = 0;
        for (int i = 0; i < size; i++) {
            short sh1 = data.mData[i];
            sh1 &= 0xff;
            short sh2 = data.mData[++i];
            sh2 <<= 8;
            sh = (double) ((sh1) | (sh2));
            dataBlock[(i-1)/2] = sh;
        }
        spectrum = fft.magnitudeSpectrum(dataBlock);
        double Max1 = spectrum[814];
        double Max2 = spectrum[814];

        for(int i = 819;i<930;i++){
            if(Max1<spectrum[i]&&(i>freq2+5||i<freq2-5)){
                Max1 = spectrum[i];
                freq1 = i;
            }
            else if(Max2 <spectrum[i]&&(i>freq1+5||i<freq1-5)){
                Max2 = spectrum[i];
                freq2 = i;
            }
        }
        Log.d("Max power frequency: ",freq1+"; "+freq2);

        freq1 = detect(freq1);
        freq2 = detect(freq2);

        if(lastPair==null)
            lastPair = new FreqPair(0,1);
        if (freq1 != -1 && freq2 != -1) {
            freqPair = new FreqPair(freq1, freq2, true);
            if (null != mListener) {
                if(!freqPair.compare(lastPair)){

                    char lastCh = translate(lastPair);
                    char ch = translate(freqPair);
                    Log.d("Detect: ","ch is "+ch+"; lastCh is: "+ lastCh);
                    if(ch=='S'){
                        lastPair = freqPair;
                        mListener.onRecognition(ch);

                    }
                    else if(lastCh=='I'&&ch!='I'&ch!='P'){
                        lastPair = freqPair;
                        mListener.onRecognition(ch);
                    }
                    else if(lastCh!='I'&&lastCh!='P'&&ch=='I'){
                        lastPair = freqPair;
                    }



//                    if(ch!='P'&&lastCh!=ch){
//                        if(ch!='I')
//                        mListener.onRecognition(ch);
//                        lastPair = freqPair;
//                    }

//                    if(ch=='S'){
//                        mListener.onRecognition(ch);
//                        lastPair = freqPair;
//                    }
//                    else if(lastCh=='I'&&ch!='I'&&ch!='P'){
//                        mListener.onRecognition(ch);
//                        lastPair = freqPair;
//                    }
//                    else if(lastCh!='I'&&lastCh!='P'&&ch=='I')
//                        lastPair = freqPair;
//                    if(ch!='I')
//
//                        else{
//                            if(lastCh=='I'||lastCh=='S')
//                                mListener.onRecognition(ch);
//                        }

//                regFreq1 = mRegIndex1;
//                if(regFreq1!=6)
//                    mListener.onRecognition(mRegIndex1, mRegIndex2);
                }
                //mPreRegCircle1 = mRegIndex1;


            }


        }

//        spectrum = FFT(dataBlock,(int)size/2+1);

    }
    public char translate(FreqPair pair){
        if(pair.compare(new FreqPair(0,1,true)))
            return 'S';
        else if(pair.compare(new FreqPair(1,2,true)))
            return '0';
        else if(pair.compare(new FreqPair(3,2,true)))
            return '1';
        else if(pair.compare(new FreqPair(3,4,true)))
            return '2';
        else if(pair.compare(new FreqPair(4,5,true)))
            return '3';
        else if(pair.compare(new FreqPair(5,6,true)))
            return '4';
        else if(pair.compare(new FreqPair(0,2,true)))
            return '5';
        else if(pair.compare(new FreqPair(1,3,true)))
            return '6';
        else if(pair.compare(new FreqPair(4,2,true)))
            return '7';
        else if(pair.compare(new FreqPair(3,5,true)))
            return '8';
        else if(pair.compare(new FreqPair(4,6,true)))
            return '9';
        else if(pair.compare(new FreqPair(0,3,true)))
            return 'A';
        else if(pair.compare(new FreqPair(1,4,true)))
            return 'B';
        else if(pair.compare(new FreqPair(5,2,true)))
            return 'C';
        else if(pair.compare(new FreqPair(3,6,true)))
            return 'D';
        else if(pair.compare(new FreqPair(0,4,true)))
            return 'E';
        else if(pair.compare(new FreqPair(1,5,true)))
            return 'F';
        else if(pair.compare(new FreqPair(8,7,true)))
            return 'I';
        else if(pair.compare(new FreqPair(0,5,true)))
            return 'O';
        else if(pair.compare(new FreqPair(1,6,true)))
            return 'V';
        else
            return 'P';
    }
    public boolean freqCheck(int freq){
        if(freq==829||freq==844||freq==859||freq==874||freq==889||freq==904||freq==919)
            return true;
        return false;
    }
    public int detect(int freq){
        int index=-1;
        if(true){
            switch (freq){
                case 828:
                case 829:
                case 830:
                    index = 0;
                    break;
                case 843:
                case 845:
                case 844:
                    index = 1;
                    break;
                case 859:
                case 860:
                case 858:
                    index = 2;
                    break;
                case 874:
                case 873:
                case 875:
                    index = 3;
                    break;
                case 889:
                case 890:
                case 888:
                    index = 4;
                    break;
                case 903:
                case 904:
                case 905:
                    index = 5;
                    break;
                case 918:
                case 919:
                case 920:
                    index = 6;
                    break;
                case 895:
                case 896:
                case 897:
                    index = 7;
                    break;
                case 911:
                case 912:
                case 913:
                    index = 8;
                    break;
            }
        }
        return index;
    }

//    private void reg(int samplingPointCount) {
//        if (!mIsBeginning) {
//            if (!mStartingDet) {
//                if (MAX_SAMPLING_POINT_COUNT == samplingPointCount) {
//                    mStartingDet = true;
//                    mStartingDetCount = 0;
//                }
//            } else {
//                if (MAX_SAMPLING_POINT_COUNT == samplingPointCount) {
//                    ++mStartingDetCount;
//
//                    if (mStartingDetCount >= MIN_REG_CIRCLE_COUNT) {
//                        mIsBeginning = true;
//                        mIsRegStart = false;
//                        mRegCount = 0;
//                    }
//                } else {
//                    mStartingDet = false;
//                }
//            }
//        } else {
//            if (!mIsRegStart) {
//                if (samplingPointCount > 0) {
//                    mRegValue = samplingPointCount;
//                    mRegIndex = INDEX[samplingPointCount];
//                    mIsRegStart = true;
//                    mRegCount = 1;
//                }
//            } else {
//                if (samplingPointCount == mRegValue) {
//                    ++mRegCount;
//
//                    if (mRegCount >= MIN_REG_CIRCLE_COUNT) {
//                        // ok
//                        if (mRegValue != mPreRegCircle) {
//                            if (null != mListener) {
//                                mListener.onRecognition(mRegIndex);
//                            }
//                            mPreRegCircle = mRegValue;
//                        }
//
//                        mIsRegStart = false;
//                    }
//                } else {
//                    mIsRegStart = false;
//                }
//            }
//        }
//    }

//    public short[] FFT(short[] dataBlock, int size){
//        short[] xConv;
//        int m = 11, j=2048;
//        if(size<1||size>2048){
//            return null;
//        }
//        xConv = new short[j];
//        for(int i=0;i<size;i++){
//            xConv[i] = dataBlock[i];
//        }
//        if(j>size){
//            for(int i=size;i<j;i++){
//                xConv[i] = 0;
//            }
//        }
//        //i2Sort(xConv,m);
//        //myFFT(xConv,m);
//
//        return dataBlock;
//    }


//    private int preReg(int samplingPointCount) {
//        //Log.i("Counting: ", samplingPointCount+"");
//        switch (samplingPointCount) {
////            case 5:
//            case 6:
//            case 7:
//            case 8:
//                samplingPointCount = 7;
//                break;
//            case 9:
//            case 10:
//            case 11:
//            case 12:
//                samplingPointCount = 10;
//                break;
//
//            case 13:
//            case 14:
//            case 15:samplingPointCount = 13;
//                break;
//            case 16:
//            case 17:
//
//            case 18:samplingPointCount = 16;
//                break;
//            case 19:
//            case 20:
//                samplingPointCount = 19;
//                break;
//
//            case 21:
//            case 22:
//            case 23:
//                samplingPointCount = 22;
//                break;
//
//            case 24:
//            case 25:
//            case 26:
//                samplingPointCount = 25;
//                break;
//
//            case 27:
//            case 28:
//            case 29:
//                samplingPointCount = 28;
//                break;
//
//            case 30:
//            case 31:
//            case 32:
//                samplingPointCount = 31;
//                break;
//
//
//            case 33:
//            case 34:
//            case 35:
//                samplingPointCount = 34;
//                break;

//            case 36:
//            case 37:
//            case 38:
//                samplingPointCount = 37;
//                break;
//
//            case 39:
//            case 40:
//            case 41:
//                samplingPointCount = 40;
//                break;
//            case 42:
//            case 43:
//            case 44:
//                samplingPointCount = 43;
//                break;
//
//            case 45:
//            case 46:
//            case 47:
//                samplingPointCount = 46;
//                break;
//            case 48:
//            case 49:
//            case 50:
//                samplingPointCount = 49;
//                break;
//
//            case 51:
//            case 52:
//            case 53:
//                samplingPointCount = 52;
//                break;
//
//            case 54:
//            case 55:
//            case 56:
//                samplingPointCount = 55;
//                break;
//
//            case 57:
//            case 58:
//            case 59:
//                samplingPointCount = 58;
//                break;
//
//
//        default:
//            samplingPointCount = 0;
//            break;
//        }
//
//        return samplingPointCount;
//    }


}
