package com.example.sinvoicedemo;

import android.app.Activity;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.libra.sinvoice.LogHelper;
import com.libra.sinvoice.SinVoicePlayer;
import com.libra.sinvoice.SinVoiceRecognition;

public class MainActivity extends Activity implements SinVoiceRecognition.Listener, SinVoicePlayer.Listener {
    private final static String TAG = "MainActivity";
    private final static int MAX_NUMBER =4;
    private final static int MSG_SET_RECG_TEXT = 1;
    private final static int MSG_RECG_START = 2;
    private final static int MSG_RECG_END = 3;
    //private int length = 0;

    private final static String CODEBOOK = "1234";
    private EditText editText;
    private Handler mHanlder;
    private SinVoicePlayer mSinVoicePlayer;
    private SinVoiceRecognition mRecognition;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSinVoicePlayer = new SinVoicePlayer(CODEBOOK);
        mSinVoicePlayer.setListener(this);

        mRecognition = new SinVoiceRecognition(CODEBOOK);
        mRecognition.setListener(this);
        editText = (EditText) findViewById(R.id.editText);
        final TextView playTextView = (TextView) findViewById(R.id.playtext);
        TextView recognisedTextView = (TextView) findViewById(R.id.regtext);
        mHanlder = new RegHandler(recognisedTextView);
        Message msg = Message.obtain();
        Bundle bundle = new Bundle();

        Button playStart = (Button) this.findViewById(R.id.start_play);
        playStart.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {

                String text = editText.getText().toString();
                if(text.equals(null))
                    text = genText(4);
                playTextView.setText(text);
                mSinVoicePlayer.play(text, true, 1000);
            }
        });

        Button playStop = (Button) this.findViewById(R.id.stop_play);
        playStop.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                mSinVoicePlayer.stop();
            }
        });

        Button recognitionStart = (Button) this.findViewById(R.id.start_reg);
        recognitionStart.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                mRecognition.start();
            }
        });

        Button recognitionStop = (Button) this.findViewById(R.id.stop_reg);
        recognitionStop.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                mRecognition.stop();
            }
        });
    }

    private String genText(int count) {
        StringBuilder sb = new StringBuilder();
        int pre = 0;
        while (count > 0) {
            int x = (int) (Math.random() * MAX_NUMBER + 1);
            if (Math.abs(x - pre) > 0) {
                if(x>9){
                    switch (x){
                        case 10: sb.append('A');
                            break;
                        case 11: sb.append('B');
                            break;
                        case 12: sb.append('C');
                            break;
                        case 13: sb.append('D');
                            break;
                        case 14: sb.append('E');
                            break;
                        case 15: sb.append('F');
                            break;
                    }
                }
                else
                    sb.append(x);
                --count;
                pre = x;
            }
        }

        return sb.toString();
    }

    private static class RegHandler extends Handler {
        private StringBuilder mTextBuilder = new StringBuilder();
        private TextView mRecognisedTextView;
        private int length ;
        public RegHandler(TextView textView) {
            mRecognisedTextView = textView;
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Bundle bundle = msg.getData();
            String str = bundle.getString("Data");
            int size = str.length();
            for(int i=0;i<size;i++){
                switch (str.charAt(i)){
                    case '0':
                        length = 0;
                        mTextBuilder.delete(0,mTextBuilder.length());
                        break;
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                        length+=1;
                        mTextBuilder.append(str.charAt(i));
                        if(mRecognisedTextView!=null)
                            mRecognisedTextView.setText((mTextBuilder.toString()));
                        break;
                    case '5':
                        length+=1;
//                        LogHelper.d(TAG,"recognition end");
                        break;
                    case '6':
                        if(length%2==1){
                            LogHelper.d(TAG,"recognize correct");
                            length = 0;
                        }
                        else{
                            LogHelper.e(TAG,"Transmitting Error!");
                        }
                        break;
                    case '7':
                        if(length%2==0){
                            LogHelper.d(TAG,"recognize correct");
                            length = 0;
                        }
                        else{
                            LogHelper.e(TAG,"Transmitting Error!");
                        }
                        break;
                }
            }
            super.handleMessage(msg);

//            switch (msg.what) {
//            case MSG_SET_RECG_TEXT:
//                char ch = (char) msg.arg1;
//                mTextBuilder.append(ch);
//                if (null != mRecognisedTextView) {
//                    mRecognisedTextView.setText(mTextBuilder.toString());
//                }
//                break;
//
//            case MSG_RECG_START:
//                mTextBuilder.delete(0, mTextBuilder.length());
//                break;
//
//            case MSG_RECG_END:
//                LogHelper.d(TAG, "recognition end");
//                break;
//            }
//            super.handleMessage(msg);
        }
    }

    @Override
    public void onRecognitionStart() {
        mHanlder.sendEmptyMessage(MSG_RECG_START);
    }

    @Override
    public void onRecognition(String str) {
        Message msg = Message.obtain();
        Bundle b = new Bundle();
        b.putString("Data",str);
        msg.setData(b);
        mHanlder.sendMessage(msg);


       // mHanlder.sendMessage(mHanlder.obtainMessage(MSG_SET_RECG_TEXT, str, 0));
    }

    @Override
    public void onRecognitionEnd() {
        mHanlder.sendEmptyMessage(MSG_RECG_END);
    }

    @Override
    public void onPlayStart() {
        LogHelper.d(TAG, "start play");
    }

    @Override
    public void onPlayEnd() {
        LogHelper.d(TAG, "stop play");
    }

}
