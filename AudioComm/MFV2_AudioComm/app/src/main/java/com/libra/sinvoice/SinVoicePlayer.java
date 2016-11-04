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

import java.util.ArrayList;
import java.util.List;

import android.media.AudioFormat;
import android.text.TextUtils;

import com.libra.sinvoice.Buffer.BufferData;

public class SinVoicePlayer implements Encoder.Listener, Encoder.Callback, PcmPlayer.Listener, PcmPlayer.Callback {
    private final static String TAG = "SinVoicePlayer";

    private final static int STATE_START = 1;
    private final static int STATE_STOP = 2;
    private final static int STATE_PENDING = 3;

    private final static int DEFAULT_GEN_DURATION = 100;

    private String mCodeBook="0123456789ABCDEFG";
    private List<Character> mCodes = new ArrayList<Character>();
    private List<Character> hexCode = new ArrayList<Character>();

    private Encoder mEncoder;
    private PcmPlayer mPlayer;
    private Buffer mBuffer;

    private int mState;
    private Listener mListener;
    private Thread mPlayThread;
    private Thread mEncodeThread;

    public static interface Listener {
        void onPlayStart();

        void onPlayEnd();
    }



    public SinVoicePlayer(String codeBook) {
        this(codeBook, Common.DEFAULT_SAMPLE_RATE, Common.DEFAULT_BUFFER_SIZE, Common.DEFAULT_BUFFER_COUNT);
    }

    public SinVoicePlayer(String codeBook, int sampleRate, int bufferSize, int buffCount) {
        mState = STATE_STOP;
        mBuffer = new Buffer(buffCount, bufferSize);

        mEncoder = new Encoder(this, sampleRate, SinGenerator.BITS_16, bufferSize);
        mEncoder.setListener(this);
        mPlayer = new PcmPlayer(this, sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
        mPlayer.setListener(this);

        setCodeBook(codeBook);
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public void setCodeBook(String codeBook) {
        if (!TextUtils.isEmpty(codeBook) && codeBook.length() < Encoder.getMaxCodeCount() - 1) {
            mCodeBook = codeBook;
        }
    }

    private boolean convertTextToCodes(String text) {
        boolean ret = true;

        if (!TextUtils.isEmpty(text)) {
            mCodes.clear();
            mCodes.add(Common.START_TOKEN);
            mCodes.add(Common.INTERVAL);
            int len = text.length();
            for (int i = 0; i < len; ++i) {
                char ch = text.charAt(i);
                int index = mCodeBook.indexOf(ch);
                if (index > -1) {
                    mCodes.add(ch);
                    mCodes.add(Common.INTERVAL);

                } else {
                    ret = false;
                    LogHelper.d(TAG, "invalidate char:" + ch);
                    break;
                }
            }
            if (ret&&len%2==0) {
                mCodes.add(Common.STOP_TOKEN_EVEN);
            }
            else if (ret&&len%2==1) {
                mCodes.add(Common.STOP_TOKEN_ODD);
            }
        } else {
            ret = false;
        }
//        converToHex(mCodes);
//        mCodes = hexCode;

        return ret;
    }
    public void converToHex(List<Character> code){
        int len = code.size()*2+1;
        if(code!=null){
            hexCode.clear();
            for(int i = 0;i<len;i++){
                char ch = code.get(i);
                int dexNum = (int)ch;
                int highNibble, lowNibble;
                highNibble = dexNum/16;
                lowNibble = dexNum%16;
                char highCh = decToHex(highNibble);
                char lowCh = decToHex(lowNibble);
                if(highCh!='P'&&lowCh!='P'){
                    hexCode.add(highCh);
                    hexCode.add(lowCh);
                }

            }
        }

    }
    public char decToHex(int num){
        if(num>=0&&num<16){
            switch (num){
                case 0 :
                    return '0';
                case 1 :
                    return '1';
                case 2 :
                    return '2';
                case 3 :
                    return '3';
                case 4 :
                    return '4';
                case 5 :
                    return '5';
                case 6 :
                    return '6';
                case 7 :
                    return '7';
                case 8 :
                    return '8';
                case 9 :
                    return '9';
                case 10 :
                    return 'A';
                case 11 :
                    return 'B';
                case 12 :
                    return 'C';
                case 13 :
                    return 'D';
                case 14 :
                    return 'E';
                case 15 :
                    return 'F';
            }
        }
        return 'P';
    }

    public void play(final String text) {
        play(text, false, 0);
    }

    public void play(final String text, final boolean repeat, final int muteInterval) {
        if (STATE_STOP == mState && null != mCodeBook && convertTextToCodes(text)) {
            mState = STATE_PENDING;

            mPlayThread = new Thread() {
                @Override
                public void run() {
                    mPlayer.start();
                }
            };
            if (null != mPlayThread) {
                mPlayThread.start();
            }

            mEncodeThread = new Thread() {
                @Override
                public void run() {
                    do {
                        LogHelper.d(TAG, "encode start");
                        mEncoder.encode(mCodes, DEFAULT_GEN_DURATION, muteInterval);
                        LogHelper.d(TAG, "encode end");

                        mEncoder.stop();
                    } while (repeat && STATE_PENDING != mState);
                    stopPlayer();
                }
            };
            if (null != mEncodeThread) {
                mEncodeThread.start();
            }

            LogHelper.d(TAG, "play");
            mState = STATE_START;
        }
    }

    public void stop() {
        if (STATE_START == mState) {
            mState = STATE_PENDING;

            LogHelper.d(TAG, "force stop start");
            mEncoder.stop();
            if (null != mEncodeThread) {
                try {
                    mEncodeThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    mEncodeThread = null;
                }
            }

            LogHelper.d(TAG, "force stop end");
        }
    }

    private void stopPlayer() {
        if (mEncoder.isStoped()) {
            mPlayer.stop();
        }

        // put end buffer
        mBuffer.putFull(BufferData.getEmptyBuffer());

        if (null != mPlayThread) {
            try {
                mPlayThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                mPlayThread = null;
            }
        }

        mBuffer.reset();
        mState = STATE_STOP;
    }

    @Override
    public void onStartEncode() {
        LogHelper.d(TAG, "onStartGen");
    }

    @Override
    public void freeEncodeBuffer(BufferData buffer) {
        if (null != buffer) {
            mBuffer.putFull(buffer);
        }
    }

    @Override
    public BufferData getEncodeBuffer() {
        return mBuffer.getEmpty();
    }

    @Override
    public void onEndEncode() {
    }

    @Override
    public BufferData getPlayBuffer() {
        return mBuffer.getFull();
    }

    @Override
    public void freePlayData(BufferData data) {
        mBuffer.putEmpty(data);
    }

    @Override
    public void onPlayStart() {
        if (null != mListener) {
            mListener.onPlayStart();
        }
    }

    @Override
    public void onPlayStop() {
        if (null != mListener) {
            mListener.onPlayEnd();
        }
    }

}
