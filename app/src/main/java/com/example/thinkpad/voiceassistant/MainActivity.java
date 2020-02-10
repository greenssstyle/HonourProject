package com.example.thinkpad.voiceassistant;

import android.Manifest;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.example.thinkpad.voiceassistant.recognition.OnlineRecognition;
import com.example.thinkpad.voiceassistant.ui.ListData;
import com.example.thinkpad.voiceassistant.ui.TextAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import ai.api.AIDataService;
import ai.api.AIListener;
import ai.api.AIServiceException;
import ai.api.android.AIConfiguration;
import ai.api.model.AIError;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;
import ai.api.model.Result;

import static com.example.thinkpad.voiceassistant.ui.ListData.RECEIVER;
import static com.example.thinkpad.voiceassistant.ui.ListData.SEND;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, AIListener {

    protected ListView lvList;
    protected ImageButton btnStart;

    protected int SPEECH_RECOGNIZE = 1;
    protected TextToSpeech textToSpeech;

    private List<ListData> lists;
    private TextAdapter adapter;
    private ListData listData;
    AIConfiguration config;
    AIDataService aiDataService;
    AIRequest aiRequest;

    private final String APIAI_CLIENT_TOKEN = "0c01e159babc4349b38eca698bd2f107";
    private String res;
    private boolean isMessage=false;
    private String msg_number;
    private String msg_name;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        initPermission();

        config = new AIConfiguration(APIAI_CLIENT_TOKEN, AIConfiguration.SupportedLanguages.English, AIConfiguration.RecognitionEngine.System);
        aiDataService = new AIDataService(config);
        aiRequest = new AIRequest();
    }

    protected void initView() {
        setContentView(R.layout.activity_list);

        lvList = findViewById(R.id.list_lv);
        btnStart = findViewById(R.id.list_btn_start);
        btnStart.setOnClickListener(this);

        lists = new ArrayList<ListData>();
        adapter = new TextAdapter(lists, this);
        lvList.setAdapter(adapter);

        // Refresh
        refresh("How can I help you?", RECEIVER);
        speak(listData.getContent());
    }

    protected void initPermission() {
        String[] permissions = {
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.INTERNET,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,

                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS,
                Manifest.permission.SEND_SMS,

                Manifest.permission.CALL_PHONE,
                Manifest.permission.READ_CONTACTS
        };

        ArrayList<String> toApplyList = new ArrayList<String>();

        for (String perm : permissions) {
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, perm)) {
                toApplyList.add(perm);
                // 进入到这里代表没有权限.

            }
        }
        String[] tmpList = new String[toApplyList.size()];
        if (!toApplyList.isEmpty()) {
            ActivityCompat.requestPermissions(this, toApplyList.toArray(tmpList), 123);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // 此处为android 6.0以上动态授权的回调，用户自行实现。
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.list_btn_start:
                Intent intent = new Intent(this, OnlineRecognition.class);
                startActivityForResult(intent, SPEECH_RECOGNIZE);
                break;
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SPEECH_RECOGNIZE && resultCode == RESULT_OK) {
            res = data.getStringExtra("results");
            refresh(res, SEND);

            if (isMessage){
                sendMessage(res);
                isMessage=false;
                return;
            }

            // Keyword: read
            if (res.contains("read")){
                readMessage();
                return;
            }

            // Keyword: send
            if (res.contains("send")){
                getSendMsgContactInfo();
                return;
            }

            // Keyword: open
            if (res.contains("open")){
                String command = "open";
                String appName= res.substring(res.indexOf("open")+command.length()+1);
                Log.d("tag app name",appName);
                openApp(appName);
                return;
            }

            // Keyword: call
            if (res.contains("call")){
                call();
                return;
            }

            // Keyword: search
            if (res.contains("search")){
                String command = "search";
                String searchContent=res.substring(res.indexOf("search")+command.length()+1);
                searchOnInternet(searchContent);
                return;
            }

            // Otherwise, chat with chatbot
            chat();
        }
    }


    private void chat() {
        aiRequest.setQuery(res);
        new AsyncTask<AIRequest,Void,AIResponse>(){

            @Override
            protected AIResponse doInBackground(AIRequest... aiRequests) {
                final AIRequest request = aiRequests[0];
                try {
                    final AIResponse response = aiDataService.request(aiRequest);
                    return response;
                } catch (AIServiceException e) {
                }
                return null;
            }
            @Override
            protected void onPostExecute(AIResponse response) {
                if (response != null) {

                    Result result = response.getResult();
                    String reply = result.getFulfillment().getSpeech();
                    refresh(reply, RECEIVER);
                    speak(listData.getContent());
                }
            }
        }.execute(aiRequest);
    }


    private void refresh(String content,int flag) {
        listData = new ListData(content, flag);
        lists.add(listData);
        if (lists.size() > 30) {
            for (int i = 0; i < lists.size(); i++) {
                lists.remove(i);
            }
        }
        adapter.notifyDataSetChanged();
    }


    private void speak(final String text) {
        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
              if (status == TextToSpeech.SUCCESS) {
                    // 设置使用美式英语朗读
                    int result = textToSpeech.setLanguage(Locale.US);

                    // 检测是否支持所设置的语言
                    if (result!=TextToSpeech.LANG_COUNTRY_AVAILABLE && result!=TextToSpeech.LANG_AVAILABLE) {
                        Toast.makeText(MainActivity.this, "TTS language not supported", Toast.LENGTH_LONG).show();
                    } else {
                        textToSpeech.speak(text, TextToSpeech.QUEUE_ADD, null);
                    }
              }
            }
        });
    }


    private void readMessage() {
        String[] msgInfo = readPhoneMessage();

        String msg = msgInfo[1];
        String sender = msgInfo[0];

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("From: "+sender);
        dialogBuilder.setMessage("Message: "+msg);
        dialogBuilder.setPositiveButton("OK", null);
        dialogBuilder.show();

        if (Pattern.compile( "[0-9]" ).matcher(sender).find()) {
            String tmp = "";
            char[] chars = msgInfo[0].toCharArray();
            for(int i = 0; i<chars.length; i++) {
                if(chars[i]>='0'&&chars[i]<='9') {
                    tmp += numToWord(chars[i]);
                } else {
                    tmp += chars[i];
                }
            }
            sender = tmp;
        }

        refresh("Your message is displayed.", RECEIVER);
        speak(msg);
    }

    private String[] readPhoneMessage(){
        String[] res = new String[2];

        Uri SMS_INBOX = Uri.parse("content://sms/inbox");
        ContentResolver cr = getContentResolver();
        String[] projection = new String[] {"_id", "address", "person", "body", "date", "type" };

        Cursor cur = cr.query(SMS_INBOX, projection, null, null, "date desc");
        if (cur == null) {
            Log.i("ooc","************cur == null");
            return res;
        }

        while(cur.moveToNext()) {
            String person = cur.getString(cur.getColumnIndex("person"));
            String number = cur.getString(cur.getColumnIndex("address"));
            String body = cur.getString(cur.getColumnIndex("body"));
            res[1] = body;
            res[0] = number;
            /*
            if (person != null) {
                res[0] = person;
            } else {
                res[0] = number;
            }*/
            break;
        }
        return res;
    }

    private String numToWord(char c) {
        String res = "";
        switch(c) {
            case '1':
                res = " one ";
                break;
            case '2':
                res = " two ";
                break;
            case '3':
                res = " three ";
                break;
            case '4':
                res = " four ";
                break;
            case '5':
                res = " five ";
                break;
            case '6':
                res = " six ";
                break;
            case '7':
                res = " seven ";
                break;
            case '8':
                res = " eight ";
                break;
            case '9':
                res = " nine ";
                break;
            case '0':
                res = " zero ";
                break;
        }
        return res;
    }


    private void sendMessage(String content) {
        if (msg_number==null){
            return;
        }

        AlertDialog.Builder dialogSendBuilder = new AlertDialog.Builder(this);
        dialogSendBuilder.setTitle("Send To: "+msg_name);
        //dialogSendBuilder.setMessage("Message: "+content);
        final EditText etMsg = new EditText(this);
        dialogSendBuilder.setView(etMsg);
        etMsg.setText(content);
        dialogSendBuilder.setPositiveButton("Send", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                sendSMS(etMsg.getText().toString());
            }
        });
        dialogSendBuilder.setNegativeButton("Cancel", null);
        dialogSendBuilder.show();
    }

    private void sendSMS(String content) {
        SmsManager manager = SmsManager.getDefault();
        ArrayList<String> list = manager.divideMessage(content);  //因为一条短信有字数限制，因此要将长短信拆分
        for(String text:list){
            manager.sendTextMessage(msg_number, null, text, null, null);
        }

        refresh("Message \""+content+"\" is sent to "+msg_name, RECEIVER);
        speak(listData.getContent());
    }

    private void getSendMsgContactInfo() {
        List<ContactInfo> contactLists = getContactLists(this);
        if (contactLists.isEmpty()){
            refresh("Your Contact list is empty.",RECEIVER);
            speak(listData.getContent());
            return;
        }

        for (ContactInfo contactInfo:contactLists){
            if (res.toLowerCase().contains(contactInfo.getName().toLowerCase())){
                msg_name=contactInfo.getName();
                msg_number=contactInfo.getNumber();
                refresh("What would you like to send to "+msg_name+"?",RECEIVER);
                speak(listData.getContent());
                isMessage=true;
                return;
            }
        }

        refresh("I cannot find a matched person in your Contact list.",RECEIVER);
        speak(listData.getContent());
    }


    private void openApp(String appName) {
        PackageManager packageManager = MainActivity.this.getPackageManager();
        List<PackageInfo> pkgs = packageManager.getInstalledPackages(0);

        for (int i=0; i<pkgs.size(); i++) {
            PackageInfo pInfo = pkgs.get(i);

            String label = packageManager.getApplicationLabel(pInfo.applicationInfo).toString();
            Log.d("tag", label);
            String labelLC = label.toLowerCase();
            if (labelLC.equals(appName)) { //比较label
                Intent resolveIntent = new Intent(Intent.ACTION_MAIN, null);
                resolveIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                resolveIntent.setPackage(pInfo.packageName);

                List<ResolveInfo> apps = packageManager.queryIntentActivities(resolveIntent, 0);
                ResolveInfo rInfo = apps.iterator().next();
                if (rInfo != null ) {
                    String packageName = rInfo.activityInfo.packageName;
                    String activityName = rInfo.activityInfo.name;
                    ComponentName cn = new ComponentName(packageName, activityName);

                    refresh(label+" is opened.", RECEIVER);
                    speak(listData.getContent());

                    Intent intent = new Intent(Intent.ACTION_MAIN);
                    intent.addCategory(Intent.CATEGORY_LAUNCHER);
                    intent.setComponent(cn);
                    startActivity(intent);
                }
                return;
            }
        }
        refresh(appName+" is not installed.",RECEIVER);
        speak(listData.getContent());
    }


    private void call() {
        List<ContactInfo> contactLists = getContactLists(this);
        if (contactLists.isEmpty()){
            refresh("Your Contact list is empty.",RECEIVER);
            speak(listData.getContent());
            return;
        }

        for (ContactInfo contactInfo:contactLists){
            if (res.toLowerCase().contains(contactInfo.getName().toLowerCase())){
                refresh("Dialing "+contactInfo.getName()+" now...",RECEIVER);
                speak(listData.getContent());

                String number = contactInfo.getNumber();
                Intent intent = new Intent();
                intent.setAction("android.intent.action.CALL");
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                intent.setData(Uri.parse("tel:"+number));
                startActivity(intent);
                return;
            }
        }

        refresh("I cannot find a matched person in your Contact list.",RECEIVER);
        speak(listData.getContent());
    }

    private List<ContactInfo> getContactLists(Context context) {
        List<ContactInfo> lists = new ArrayList<>();
        Cursor cursor = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null, null, null, null);
        while (cursor.moveToNext()) {
            String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            String number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            ContactInfo contactInfo = new ContactInfo(name, number);
            lists.add(contactInfo);
        }
        return lists;
    }


    private void searchOnInternet(String searchContent) {
        refresh("Searching "+"\""+searchContent+"\"", RECEIVER);
        speak(listData.getContent());

        // 指定intent的action是ACTION_WEB_SEARCH就能调用浏览器
        Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
        // 指定搜索关键字是选中的文本
        intent.putExtra(SearchManager.QUERY, searchContent);
        startActivity(intent);
    }


    @Override
    public void onResult(AIResponse result) {}

    @Override
    public void onError(AIError error) {}

    @Override
    public void onAudioLevel(float level) {}

    @Override
    public void onListeningStarted() {}

    @Override
    public void onListeningCanceled() {}

    @Override
    public void onListeningFinished() {}
}
