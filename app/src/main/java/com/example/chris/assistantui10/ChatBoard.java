package com.example.chris.assistantui10;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.AlarmClock;
import android.speech.RecognizerIntent;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.widget.Toast;

import ai.api.AIListener;
import ai.api.AIServiceException;
import ai.api.android.AIConfiguration;
import ai.api.android.AIDataService;
import ai.api.android.AIService;
import ai.api.model.AIError;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;
import ai.api.model.Result;

import com.github.bassaer.chatmessageview.utils.ChatBot;
import com.google.gson.JsonElement;

import java.util.HashMap;
import java.util.Map;

import com.github.bassaer.chatmessageview.models.Message;
import com.github.bassaer.chatmessageview.models.User;
import com.github.bassaer.chatmessageview.views.ChatView;

import java.util.ArrayList;
import java.util.Locale;

public class ChatBoard extends AppCompatActivity  {
    private AIService aiService;
    public static final String EXTRA_CIRCULAR_REVEAL_X = "EXTRA_CIRCULAR_REVEAL_X";
    public static final String EXTRA_CIRCULAR_REVEAL_Y = "EXTRA_CIRCULAR_REVEAL_Y";
    View rootLayout;
    private AnimationDrawable animationDrawable;

    private int revealX;
    private int revealY;
    private ChatView mChatView;
    private FloatingActionButton mic;
    User bot1;
    User you1;
    private static final int REQ_CODE_SPEECH_INPUT = 100;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_board);
        final Intent intent=getIntent();
        rootLayout=findViewById(R.id.root_layout);
        if (savedInstanceState == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && intent.hasExtra(EXTRA_CIRCULAR_REVEAL_X) && intent.hasExtra(EXTRA_CIRCULAR_REVEAL_Y))
        {
            rootLayout.setVisibility(View.INVISIBLE);
            revealX=intent.getIntExtra(EXTRA_CIRCULAR_REVEAL_X,0);
            revealY=intent.getIntExtra(EXTRA_CIRCULAR_REVEAL_Y,0);
            ViewTreeObserver viewTreeObserver=rootLayout.getViewTreeObserver();
            if(viewTreeObserver.isAlive()){
                viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        revealActivity(revealX, revealY);
                        rootLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                });
            }
        }
        else
        {
            rootLayout.setVisibility(View.VISIBLE);
        }
        animationDrawable = (AnimationDrawable) rootLayout.getBackground();
        animationDrawable.setEnterFadeDuration(5000);
        animationDrawable.setExitFadeDuration(2000);
        mic=(FloatingActionButton)findViewById(R.id.floatingActionButton);
        mic.bringToFront();
        final AIConfiguration config = new AIConfiguration("5d028f25c2f94ee688c7b3381cd83eb5",
                AIConfiguration.SupportedLanguages.English,
                AIConfiguration.RecognitionEngine.System);

        final ai.api.AIDataService aiDataService = new ai.api.AIDataService(config);

        Bitmap botIcon= BitmapFactory. decodeResource(getResources(),R.drawable.bot);
        int botId=1;
        String botName="Blake";
        Bitmap userIcon=BitmapFactory.decodeResource(getResources(),R.drawable.user);
        int userId=0;
        String userName="you";
        final User bot=new User(botId,botName,botIcon);
        bot1=bot;
        final User you=new User(userId,userName,userIcon);
        you1=you;
        mChatView = (ChatView)findViewById(R.id.chat_view);
        mChatView.setRightBubbleColor(Color.WHITE);
        mChatView.setLeftBubbleColor(Color.BLUE);
        mChatView.setSendButtonColor(Color.RED);
        mChatView.setRightMessageTextColor(Color.BLACK);
        mChatView.setLeftMessageTextColor(Color.WHITE);
        mChatView.setUsernameTextColor(Color.WHITE);
        mChatView.setDateSeparatorColor(Color.WHITE);
        mChatView.setInputTextHint("How can I help you.");
        mChatView.setOptionIcon(R.drawable.circle);
        mChatView.setOnClickSendButtonListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Message message = new Message.Builder()
                        .setUser(you)
                        .setRightMessage(true)
                        .setMessageText(mChatView.getInputText())
                        .hideIcon(false)
                        .build();
                String userq=mChatView.getInputText();
                mChatView.send(message);
                mChatView.setInputText("");
                final AIRequest aiRequest = new AIRequest();
                aiRequest.setQuery(userq);
                new AsyncTask<AIRequest, Void, AIResponse>() {
                    @Override
                    protected AIResponse doInBackground(AIRequest... requests) {
                        final AIRequest request = requests[0];
                        try {
                            final AIResponse response = aiDataService.request(aiRequest);
                            return response;
                        } catch (AIServiceException e) {
                        }
                        return null;
                    }
                    @Override
                    protected void onPostExecute(AIResponse aiResponse) {
                        if (aiResponse != null) {
                            final Result result = aiResponse.getResult();
                            final String speech = result.getFulfillment().getSpeech();
                            final Message receivedMessage = new Message.Builder()
                                    .setUser(bot)
                                    .setRightMessage(false)
                                    .setMessageText(speech)
                                    .build();
                            mChatView.receive(receivedMessage);
                            String Action=aiResponse.getResult().getAction();
                            Log.d("Action",Action);
                            if(Action.contentEquals("alarm.set")) {
                                String time;
                                time = aiResponse.getResult().getStringParameter("time");
                                Log.d("Time", time);
                                String[] tx=time.split(":");
                                Intent i = new Intent(AlarmClock.ACTION_SET_ALARM);
                                i.putExtra(AlarmClock.EXTRA_HOUR, Integer.parseInt(tx[0]));
                                i.putExtra(AlarmClock.EXTRA_MINUTES,Integer.parseInt(tx[1]));
                                startActivity(i);
                                Toast.makeText(getApplicationContext(),"Set the alarm for 9",Toast.LENGTH_SHORT).show();

                            }
                            if(Action.contentEquals("device.settings.on"))
                            {
                                String module=aiResponse.getResult().getStringParameter("module");
                                if(module.contentEquals("wifi"))
                                {
                                    WifiManager wf=(WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                                    wf.setWifiEnabled(true);
                                    Toast.makeText(getApplicationContext(),"turning on wifi",Toast.LENGTH_SHORT).show();
                                }
                                if(module.contentEquals("bluetooth"))
                                {
                                    BluetoothAdapter bluetoothAdapter= BluetoothAdapter.getDefaultAdapter();
                                    if(!bluetoothAdapter.isEnabled())
                                     bluetoothAdapter.enable();
                                    Toast.makeText(getApplicationContext(),"turning on bluetooth",Toast.LENGTH_SHORT).show();

                                }

                            }
                            if(Action.contentEquals("device.settings.off"))
                            {
                                String module=aiResponse.getResult().getStringParameter("module");
                                if(module.contentEquals("wifi"))
                                {
                                    WifiManager wf=(WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                                    wf.setWifiEnabled(false);
                                    Toast.makeText(getApplicationContext(),"turning off wifi",Toast.LENGTH_SHORT).show();
                                }
                                if(module.contentEquals("bluetooth"))
                                {
                                    BluetoothAdapter bluetoothAdapter= BluetoothAdapter.getDefaultAdapter();
                                    if(bluetoothAdapter.isEnabled())
                                        bluetoothAdapter.disable();
                                    Toast.makeText(getApplicationContext(),"turning off bluetooth",Toast.LENGTH_SHORT).show();

                                }

                            }


                        }
                    }
                }.execute(aiRequest);

            }});


            mic.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    startVoiceInput();
                }
            });

    }
    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Hello, How can I help you?");
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {

        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        final AIConfiguration config = new AIConfiguration("5d028f25c2f94ee688c7b3381cd83eb5",
                AIConfiguration.SupportedLanguages.English,
                AIConfiguration.RecognitionEngine.System);
        final ai.api.AIDataService aiDataService = new ai.api.AIDataService(config);

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    Message message = new Message.Builder()
                            .setUser(you1)
                            .setRightMessage(true)
                            .setMessageText(result.get(0))
                            .hideIcon(false)
                            .build();
                    mChatView.send(message);
                    String userq=result.get(0);
                    final AIRequest aiRequest = new AIRequest();
                    aiRequest.setQuery(userq);
                    new AsyncTask<AIRequest, Void, AIResponse>() {
                        @Override
                        protected AIResponse doInBackground(AIRequest... requests) {
                            final AIRequest request = requests[0];
                            try {
                                final AIResponse response = aiDataService.request(aiRequest);
                                return response;
                            } catch (AIServiceException e) {
                            }
                            return null;
                        }
                        @Override
                        protected void onPostExecute(AIResponse aiResponse) {
                            if (aiResponse != null) {
                                final Result result = aiResponse.getResult();
                                final String speech = result.getFulfillment().getSpeech();
                                final Message receivedMessage = new Message.Builder()
                                        .setUser(bot1)
                                        .setRightMessage(false)
                                        .setMessageText(speech)
                                        .build();
                                mChatView.receive(receivedMessage);
                                String Action=aiResponse.getResult().getAction();
                                Log.d("Action",Action);
                                if(Action.contentEquals("alarm.set")) {
                                    String time;
                                    time = aiResponse.getResult().getStringParameter("time");
                                    Log.d("Time", time);
                                    String[] tx=time.split(":");
                                    Intent i = new Intent(AlarmClock.ACTION_SET_ALARM);
                                    i.putExtra(AlarmClock.EXTRA_HOUR, Integer.parseInt(tx[0]));
                                    i.putExtra(AlarmClock.EXTRA_MINUTES,Integer.parseInt(tx[1]));
                                    startActivity(i);
                                    Toast.makeText(getApplicationContext(),"Set the alarm for 9",Toast.LENGTH_SHORT).show();

                                }
                                if(Action.contentEquals("device.settings.on"))
                                {
                                    String module=aiResponse.getResult().getStringParameter("module");
                                    if(module.contentEquals("wifi"))
                                    {
                                        WifiManager wf=(WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                                        wf.setWifiEnabled(true);
                                        Toast.makeText(getApplicationContext(),"turning on wifi",Toast.LENGTH_SHORT).show();
                                    }
                                    if(module.contentEquals("bluetooth"))
                                    {
                                        BluetoothAdapter bluetoothAdapter= BluetoothAdapter.getDefaultAdapter();
                                        if(!bluetoothAdapter.isEnabled())
                                            bluetoothAdapter.enable();
                                        Toast.makeText(getApplicationContext(),"turning on bluetooth",Toast.LENGTH_SHORT).show();

                                    }

                                }
                                if(Action.contentEquals("device.settings.off"))
                                {
                                    String module=aiResponse.getResult().getStringParameter("module");
                                    if(module.contentEquals("wifi"))
                                    {
                                        WifiManager wf=(WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                                        wf.setWifiEnabled(false);
                                        Toast.makeText(getApplicationContext(),"turning off wifi",Toast.LENGTH_SHORT).show();
                                    }
                                    if(module.contentEquals("bluetooth"))
                                    {
                                        BluetoothAdapter bluetoothAdapter= BluetoothAdapter.getDefaultAdapter();
                                        if(bluetoothAdapter.isEnabled())
                                            bluetoothAdapter.disable();
                                        Toast.makeText(getApplicationContext(),"turning off bluetooth",Toast.LENGTH_SHORT).show();

                                    }

                                }



                            }
                        }
                    }.execute(aiRequest);
                }
                break;
            }

        }
    }


    protected void revealActivity(int x, int y)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            float finalRadius = (float) (Math.max(rootLayout.getWidth(), rootLayout.getHeight()) * 1.1);
            Animator circularReveal = ViewAnimationUtils.createCircularReveal(rootLayout, x, y, 0, finalRadius);
            circularReveal.setDuration(400);
            circularReveal.setInterpolator(new AccelerateInterpolator());
            rootLayout.setVisibility(View.VISIBLE);
            circularReveal.start();
        } else {
            finish();
        }

    }

    @Override
    public void onBackPressed()
    {
        System.exit(0);


    }
    @Override
    protected void onResume() {
     super.onResume();
     if (animationDrawable != null && !animationDrawable.isRunning()) {
         animationDrawable.start();
     }
     }

    @Override
    protected void onPause() {
     super.onPause();
        if (animationDrawable != null && animationDrawable.isRunning()) {
            animationDrawable.stop();
        }

     }

    }
