package goodstadt.me.uk.androidsendm;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.os.SystemClock;
import android.os.Vibrator;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.media.MediaPlayer;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;
import org.fusesource.mqtt.client.Callback;
import org.fusesource.mqtt.client.CallbackConnection;
import org.fusesource.mqtt.client.Listener;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class MainActivity extends AppCompatActivity{
    int counter = 1;

    String TAG = "SendMessage";
    private static final String APP_ACTIVITY_PATH = "/app-activity-path";

    GoogleApiClient mGoogleApiClient;
    // Bool to track whether the app is already resolving an error
    private boolean mResolvingError = false;

    ArrayList<String> colors = new ArrayList<String>();
    private int progressPoints = 100;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> arrayList;
    // Convert the this for use with MATB
    private int alertFlagRM = 0; // Resource Management flag
    private int alertFlagL = 0; // Lights flag
    private int alertFlagG = 0; // Gages flag
    private int alertFlagT = 0; // Tracking flag
    private int alertFlagC = 0; // Communications flag
    private int alertDescribeFlag = 0;
    private int alertMissionFlag = 0;
    private long[] vibrateMessageUpdate = {0, 50, 25, 50, 25, 50};
    private long[] vibrateRM = {0, 500, 200, 500, 200, 500};
    private long[] vibrateL = {0, 350, 150, 350};
    private long[] vibrateG = {0, 200};
    private long[] vibrateT = {0, 50, 25, 50};
    private long[] vibrateC = {0, 500, 100, 300, 50, 150};
    Chronometer timer;
    private AlertDialog rmAlert;
    private AlertDialog lAlert;
    private AlertDialog gAlert;
    private AlertDialog tAlert;
    private AlertDialog cAlert;
    private AlertDialog describeAlert;
    private AlertDialog missionAlert;
    private String alertDescription;
    private ArrayList<String> alertDescriptionBank = new ArrayList<String>();
    private GestureDetector gdt;
    private ListView list;


    //************************************************ Activemq Setting **************************************************
    CallbackConnection connection;
    Buffer getpayload;
    LinkedList<Buffer> payloadQueue = new LinkedList<Buffer>(); // Change to Buffer LinkedList Once android wear 2.0 availible
    int newPayloadAvailable = 0;

    //************************************************ Activemq Setting **************************************************


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        createMissionOverviewAlert("Mission:");

        arrayList = new ArrayList<String>();
        adapter = new ArrayAdapter<String>(getApplicationContext(),
                android.R.layout.simple_list_item_1, arrayList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View row = super.getView(position, convertView, parent);
                TextView text = (TextView) row.findViewById(android.R.id.text1);

                for (int i = 0; i < arrayList.size(); i++) {
                    if (position == i) {
                        row.setBackgroundColor(Color.parseColor("#DCDCDC"));
                    }
                }

                text.setTextSize(50);
                text.setGravity(Gravity.CENTER);
                text.setPadding(0,0,0,0);
                text.setTextColor(Color.parseColor("#000000"));
                return row;

            }
        };

        gdt = new GestureDetector(new GestureDetector.OnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {

                return true;
            }

            @Override
            public void onShowPress(MotionEvent e) {

            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return false;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                return false;
            }

            @Override
            public void onLongPress(MotionEvent e) {
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                final int SWIPE_MIN_DISTANCE = 12;
                final int SWIPE_THRESHOLD_VELOCITY = 20;
                if (e2.getY() - e1.getY() > SWIPE_MIN_DISTANCE && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
                    alertMissionFlag = 1;
                    missionAlert.show();
                    TextView textView = (TextView) missionAlert.findViewById(android.R.id.message);
                    textView.setTextSize(20);
                    textView.setTextColor(Color.parseColor("#000000"));
                    textView.setHeight(300);
                    textView.setWidth(300);
                    textView.setBackgroundResource(R.drawable.textview_white);
                    final Button positiveButton = missionAlert.getButton(AlertDialog.BUTTON_POSITIVE);
                    LinearLayout.LayoutParams posParams = (LinearLayout.LayoutParams) positiveButton.getLayoutParams();
                    posParams.weight = 1;
                    posParams.width = LinearLayout.LayoutParams.MATCH_PARENT;
                    posParams.height = 100;
                    posParams.gravity = Gravity.CENTER_HORIZONTAL;
                    positiveButton.setGravity(Gravity.CENTER);
                    positiveButton.setPadding(0,0,0,0);

                }

                return false;
            }
        });

        View view = findViewById(R.id.background);
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                gdt.onTouchEvent(event);
                return true;
            }
        });

        //***********************************************************MQTT******************************************
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setCancelable(false);
        builder.setTitle("ActiveMQ IP");
        final EditText input = new EditText(this);
        input.setHint("Hint: 192.168.0.11");
        input.setGravity(Gravity.CENTER);
        input.setTextSize(18);
        int maxLength = 20;
        input.setFilters(new InputFilter[] {new InputFilter.LengthFilter(maxLength)});

        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                MQTT mqtt = new MQTT();

                try {
                    mqtt.setHost("tcp://" + input.getText().toString() + ":1883");
                    //timer.setBase(SystemClock.elapsedRealtime());
                    //timer.start();

                } catch (URISyntaxException e) {
                    e.printStackTrace();

                    Context context = getApplicationContext();
                    CharSequence text = "Failed to set Host";
                    int duration = Toast.LENGTH_LONG;
                    Toast toast = Toast.makeText(context, text, duration);
                    toast.show();


                }

                mqtt.setClientId("WADClient");
                connection = mqtt.callbackConnection();
                connection.listener(new Listener() {

                    public void onDisconnected() {
                    }

                    public void onConnected() {
                    }

                    public void onPublish(UTF8Buffer topic, Buffer payload, Runnable ack) {
                        // You can now process a received message from a topic.
                        // Once process execute the ack runnable.
                        getpayload = payload ;
                        ack.run();
                        newPayloadAvailable = 1 ;
                    }

                    public void onFailure(Throwable value) {

                    }
                });

                connection.connect(new Callback<Void>() {
                    public void onFailure(Throwable value) {

                    }

                    // Once we connect..
                    public void onSuccess(Void v) {
                        // Subscribe to a topic
                        Topic[] topics = {new Topic("MCITOPIC", QoS.AT_LEAST_ONCE)};
                        connection.subscribe(topics, new Callback<byte[]>() {
                            public void onSuccess(byte[] qoses) {

                            }

                            public void onFailure(Throwable value) {

                            }
                        });
                    }
                });

                //***************************NEXT STEP **************************


                final Handler getPayload = new Handler();
                new Thread()
                {
                    public void run()
                    {
                        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                        MainActivity.this.runOnUiThread(new Runnable()
                        {
                            public void run() {

                                if(newPayloadAvailable == 1 ){
                                    if(getpayload != null) {
                                        payloadQueue.add(getpayload);
                                        newPayloadAvailable = 0;
                                    }
                                }
                                getPayload.postDelayed(this, 250);
                            }

                        });
                    }

                }.start();

                final Handler dequeing = new Handler();

                new Thread() {
                    public void run() {
                        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                        MainActivity.this.runOnUiThread(new Runnable() {
                            public void run() {

                                try {
                                    if (!payloadQueue.isEmpty()) {
                                        String payloadContent = payloadQueue.removeFirst().toString();
                                        Map<String, String> map = new HashMap<String, String>();
                                        map.put("request", "\"" + payloadContent + "\"");//double quote json string with spaces
                                        //map.put("request", "Message8fromPhoneLONGTEXT");
                                        map.put("counter", String.valueOf(counter));
                                        packageAndSendMessage(APP_ACTIVITY_PATH, map);

                                        payloadContent = payloadContent.substring(7, payloadContent.length());
                                        if (payloadContent.startsWith("(S)")) {

                                            timer.setBase(SystemClock.elapsedRealtime());
                                            timer.start();

                                        } else if (payloadContent.startsWith("(SRM)")) {

                                            alertDescription = payloadContent.substring(5, payloadContent.length());
                                            alertDescriptionBank.add(alertDescription);

                                            if (alertDescription.contains("Attend")){

                                                if (alertDescription.contains("Tank A")){

                                                    // Add RM to the arrayList
                                                    colors.add("#ff0000");
                                                    arrayList.add("RM - A"); // \n(" + timer.getText() + ")");
                                                    adapter.notifyDataSetChanged();
                                                    // This works to play the recording when the message is displayed.
                                                    final MediaPlayer rmsound = MediaPlayer.create(MainActivity.this, R.raw.recording);
                                                    rmsound.start();

                                                } else if (alertDescription.contains("Tank B")){

                                                    // Add RM to the arrayList
                                                    colors.add("#ff0000");
                                                    arrayList.add("RM - B"); // \n(" + timer.getText() + ")");
                                                    adapter.notifyDataSetChanged();
                                                    // This works to play the recording when the message is displayed.
                                                    final MediaPlayer rmsound = MediaPlayer.create(MainActivity.this, R.raw.recording);
                                                    rmsound.start();

                                                }

                                            } else if (alertDescription.contains("User Responded")) {

                                                if (alertDescription.contains("Tank A")){

                                                    // Remove the first occurance of RM from the arrayList
                                                    arrayList.remove("RM - A");
                                                    adapter.notifyDataSetChanged();

                                                } else if (alertDescription.contains("Tank B")){

                                                    // Remove the first occurance of RM from the arrayList
                                                    arrayList.remove("RM - B");
                                                    adapter.notifyDataSetChanged();

                                                }

                                            }

                                        } else if (payloadContent.startsWith("(SL)")) {

                                            alertDescription = payloadContent.substring(4, payloadContent.length());
                                            alertDescriptionBank.add(alertDescription);

                                            if (alertDescription.contains("Attend")){

                                                if (alertDescription.contains("Red")){

                                                    colors.add("#FF8C00"); // value used by getView in arrayAdapter
                                                    // progressPoints -= 5;
                                                    arrayList.add("L - Red"); //\n(" + timer.getText() + ")");
                                                    adapter.notifyDataSetChanged();
                                                    // This works to play the recording when the message is displayed.
                                                    final MediaPlayer lsound = MediaPlayer.create(MainActivity.this, R.raw.recording);
                                                    lsound.start();

                                                } else if (alertDescription.contains("Green")){

                                                    colors.add("#FF8C00"); // value used by getView in arrayAdapter
                                                    // progressPoints -= 5;
                                                    arrayList.add("L - Green"); //\n(" + timer.getText() + ")");
                                                    adapter.notifyDataSetChanged();
                                                    // This works to play the recording when the message is displayed.
                                                    final MediaPlayer lsound = MediaPlayer.create(MainActivity.this, R.raw.recording);
                                                    lsound.start();

                                                }

                                            } else if (alertDescription.contains("User Responded")) {

                                                if (alertDescription.contains("Green")){

                                                    // Remove the first occurance of RM from the arrayList
                                                    arrayList.remove("L - Green");
                                                    adapter.notifyDataSetChanged();

                                                } else if (alertDescription.contains("Red")){

                                                    // Remove the first occurance of RM from the arrayList
                                                    arrayList.remove("L - Red");
                                                    adapter.notifyDataSetChanged();

                                                }

                                            } else if (alertDescription.contains("Timeout")) {

                                                if (alertDescription.contains("Green")){

                                                    // Remove the first occurance of RM from the arrayList
                                                    arrayList.remove("L - Green");
                                                    adapter.notifyDataSetChanged();

                                                } else if (alertDescription.contains("Red")){

                                                    // Remove the first occurance of RM from the arrayList
                                                    arrayList.remove("L - Red");
                                                    adapter.notifyDataSetChanged();

                                                }

                                            }



                                        } else if (payloadContent.startsWith("(SG)")) {

                                            alertDescription = payloadContent.substring(4, payloadContent.length());
                                            alertDescriptionBank.add(alertDescription);

                                            if (alertDescription.contains("Attend")){

                                                if (alertDescription.contains("Gauge 1")){

                                                    colors.add("#FFFF00");
                                                    //progressPoints -= 3;
                                                    arrayList.add("G - 1"); //\n(" + timer.getText() + ")");
                                                    adapter.notifyDataSetChanged();
                                                    // This works to play the recording when the message is displayed.
                                                    final MediaPlayer gsound = MediaPlayer.create(MainActivity.this, R.raw.recording);
                                                    gsound.start();

                                                } else if (alertDescription.contains("Gauge 2")){

                                                    colors.add("#FFFF00");
                                                    //progressPoints -= 3;
                                                    arrayList.add("G - 2"); //\n(" + timer.getText() + ")");
                                                    adapter.notifyDataSetChanged();
                                                    // This works to play the recording when the message is displayed.
                                                    final MediaPlayer gsound = MediaPlayer.create(MainActivity.this, R.raw.recording);
                                                    gsound.start();

                                                } else if (alertDescription.contains("Gauge 3")){

                                                    colors.add("#FFFF00");
                                                    //progressPoints -= 3;
                                                    arrayList.add("G - 3"); //\n(" + timer.getText() + ")");
                                                    adapter.notifyDataSetChanged();
                                                    // This works to play the recording when the message is displayed.
                                                    final MediaPlayer gsound = MediaPlayer.create(MainActivity.this, R.raw.recording);
                                                    gsound.start();

                                                } else if (alertDescription.contains("Gauge 4")){

                                                    colors.add("#FFFF00");
                                                    //progressPoints -= 3;
                                                    arrayList.add("G - 4"); //\n(" + timer.getText() + ")");
                                                    adapter.notifyDataSetChanged();
                                                    // This works to play the recording when the message is displayed.
                                                    final MediaPlayer gsound = MediaPlayer.create(MainActivity.this, R.raw.recording);
                                                    gsound.start();

                                                }

                                            } else if (alertDescription.contains("User Responded")) {

                                                if (alertDescription.contains("Gauge 1")){

                                                    // Remove the first occurance of RM from the arrayList
                                                    arrayList.remove("G - 1");
                                                    adapter.notifyDataSetChanged();

                                                } else if (alertDescription.contains("Gauge 2")){

                                                    // Remove the first occurance of RM from the arrayList
                                                    arrayList.remove("G - 2");
                                                    adapter.notifyDataSetChanged();

                                                } else if (alertDescription.contains("Gauge 3")){

                                                    // Remove the first occurance of RM from the arrayList
                                                    arrayList.remove("G - 3");
                                                    adapter.notifyDataSetChanged();

                                                } else if (alertDescription.contains("Gauge 4")){

                                                    // Remove the first occurance of RM from the arrayList
                                                    arrayList.remove("G - 4");
                                                    adapter.notifyDataSetChanged();

                                                }

                                            } else if (alertDescription.contains("Timeout")) {

                                                if (alertDescription.contains("Gauge 1")){

                                                    // Remove the first occurance of RM from the arrayList
                                                    arrayList.remove("G - 1");
                                                    adapter.notifyDataSetChanged();

                                                } else if (alertDescription.contains("Gauge 2")){

                                                    // Remove the first occurance of RM from the arrayList
                                                    arrayList.remove("G - 2");
                                                    adapter.notifyDataSetChanged();

                                                } else if (alertDescription.contains("Gauge 3")){

                                                    // Remove the first occurance of RM from the arrayList
                                                    arrayList.remove("G - 3");
                                                    adapter.notifyDataSetChanged();

                                                } else if (alertDescription.contains("Gauge 4")){

                                                    // Remove the first occurance of RM from the arrayList
                                                    arrayList.remove("G - 4");
                                                    adapter.notifyDataSetChanged();

                                                }

                                            }

                                        } else if (payloadContent.startsWith("(ST)")) {

                                            alertDescription = payloadContent.substring(4 , payloadContent.length());
                                            alertDescriptionBank.add(alertDescription);

                                            if (alertDescription.contains("Attend")){

                                                colors.add("#FF8C00"); // value used by getView in arrayAdapter
                                                // progressPoints -= 5;
                                                arrayList.add("T"); //\n(" + timer.getText() + ")");
                                                adapter.notifyDataSetChanged();
                                                // This works to play the recording when the message is displayed.
                                                final MediaPlayer tsound = MediaPlayer.create(MainActivity.this, R.raw.recording);
                                                tsound.start();

                                            } else if (alertDescription.contains("User Responded")) {

                                                // Remove the first occurance of RM from the arrayList
                                                arrayList.remove("T");
                                                adapter.notifyDataSetChanged();

                                            } else if (alertDescription.contains("Timeout")) {

                                                // Remove the first occurance of RM from the arrayList
                                                arrayList.remove("T");
                                                adapter.notifyDataSetChanged();

                                            }

                                        } else if (payloadContent.startsWith("(RM)")) {  // Written and haptic feedback

                                            alertDescription = payloadContent.substring(4, payloadContent.length());
                                            alertDescriptionBank.add(alertDescription);

                                            if (alertDescription.contains("Attend")){

                                                if (alertDescription.contains("Tank A")){

                                                    colors.add("#ff0000");
                                                    arrayList.add("RM - A"); // \n(" + timer.getText() + ")");
                                                    adapter.notifyDataSetChanged();
                                                    Vibrator redVib = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                                                    redVib.vibrate(vibrateRM, -1);

                                                } else if (alertDescription.contains("Tank B")){

                                                    colors.add("#ff0000");
                                                    arrayList.add("RM - B"); // \n(" + timer.getText() + ")");
                                                    adapter.notifyDataSetChanged();
                                                    Vibrator redVib = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                                                    redVib.vibrate(vibrateRM, -1);

                                                }

                                            } else if (alertDescription.contains("User Responded")) {

                                                if (alertDescription.contains("Tank A")){

                                                    // Remove the first occurance of RM from the arrayList
                                                    arrayList.remove("RM - A");
                                                    adapter.notifyDataSetChanged();

                                                } else if (alertDescription.contains("Tank B")){

                                                    // Remove the first occurance of RM from the arrayList
                                                    arrayList.remove("RM - B");
                                                    adapter.notifyDataSetChanged();

                                                }

                                            }

                                        } else if (payloadContent.startsWith("(L)")) {

                                            alertDescription = payloadContent.substring(3, payloadContent.length());
                                            alertDescriptionBank.add(alertDescription);

                                            if (alertDescription.contains("Attend")){

                                                if (alertDescription.contains("Red")){

                                                    colors.add("#FF8C00"); // value used by getView in arrayAdapter
                                                    // progressPoints -= 5;
                                                    arrayList.add("L - Red"); //\n(" + timer.getText() + ")");
                                                    adapter.notifyDataSetChanged();
                                                    // This works to play the recording when the message is displayed.
                                                    final MediaPlayer lsound = MediaPlayer.create(MainActivity.this, R.raw.recording);
                                                    lsound.start();

                                                } else if (alertDescription.contains("Green")){

                                                    colors.add("#FF8C00"); // value used by getView in arrayAdapter
                                                    // progressPoints -= 5;
                                                    arrayList.add("L - Green"); //\n(" + timer.getText() + ")");
                                                    adapter.notifyDataSetChanged();
                                                    // This works to play the recording when the message is displayed.
                                                    final MediaPlayer lsound = MediaPlayer.create(MainActivity.this, R.raw.recording);
                                                    lsound.start();

                                                }

                                            } else if (alertDescription.contains("User Responded")) {

                                                if (alertDescription.contains("Green")){

                                                    // Remove the first occurance of RM from the arrayList
                                                    arrayList.remove("L - Green");
                                                    adapter.notifyDataSetChanged();

                                                } else if (alertDescription.contains("Red")){

                                                    // Remove the first occurance of RM from the arrayList
                                                    arrayList.remove("L - Red");
                                                    adapter.notifyDataSetChanged();

                                                }

                                            } else if (alertDescription.contains("Timeout")) {

                                                if (alertDescription.contains("Green")){

                                                    // Remove the first occurance of RM from the arrayList
                                                    arrayList.remove("L - Green");
                                                    adapter.notifyDataSetChanged();

                                                } else if (alertDescription.contains("Red")){

                                                    // Remove the first occurance of RM from the arrayList
                                                    arrayList.remove("L - Red");
                                                    adapter.notifyDataSetChanged();

                                                }

                                            }

                                        } else if (payloadContent.startsWith("(G)")) {

                                            alertDescription = payloadContent.substring(3, payloadContent.length());
                                            alertDescriptionBank.add(alertDescription);

                                            if (alertDescription.contains("Attend")){

                                                if (alertDescription.contains("Gauge 1")){

                                                    colors.add("#FFFF00");
                                                    //progressPoints -= 3;
                                                    arrayList.add("G - 1"); //\n(" + timer.getText() + ")");
                                                    adapter.notifyDataSetChanged();
                                                    Vibrator gVib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                                                    gVib.vibrate(vibrateG, -1);

                                                } else if (alertDescription.contains("Gauge 2")){

                                                    colors.add("#FFFF00");
                                                    //progressPoints -= 3;
                                                    arrayList.add("G - 2"); //\n(" + timer.getText() + ")");
                                                    adapter.notifyDataSetChanged();
                                                    Vibrator gVib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                                                    gVib.vibrate(vibrateG, -1);

                                                } else if (alertDescription.contains("Gauge 3")){

                                                    colors.add("#FFFF00");
                                                    //progressPoints -= 3;
                                                    arrayList.add("G - 3"); //\n(" + timer.getText() + ")");
                                                    adapter.notifyDataSetChanged();
                                                    Vibrator gVib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                                                    gVib.vibrate(vibrateG, -1);

                                                } else if (alertDescription.contains("Gauge 4")){

                                                    colors.add("#FFFF00");
                                                    //progressPoints -= 3;
                                                    arrayList.add("G - 4"); //\n(" + timer.getText() + ")");
                                                    adapter.notifyDataSetChanged();
                                                    Vibrator gVib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                                                    gVib.vibrate(vibrateG, -1);

                                                }

                                            } else if (alertDescription.contains("User Responded")) {

                                                if (alertDescription.contains("Gauge 1")){

                                                    // Remove the first occurance of RM from the arrayList
                                                    arrayList.remove("G - 1");
                                                    adapter.notifyDataSetChanged();

                                                } else if (alertDescription.contains("Gauge 2")){

                                                    // Remove the first occurance of RM from the arrayList
                                                    arrayList.remove("G - 2");
                                                    adapter.notifyDataSetChanged();

                                                } else if (alertDescription.contains("Gauge 3")){

                                                    // Remove the first occurance of RM from the arrayList
                                                    arrayList.remove("G - 3");
                                                    adapter.notifyDataSetChanged();

                                                } else if (alertDescription.contains("Gauge 4")){

                                                    // Remove the first occurance of RM from the arrayList
                                                    arrayList.remove("G - 4");
                                                    adapter.notifyDataSetChanged();

                                                }

                                            } else if (alertDescription.contains("Timeout")) {

                                                if (alertDescription.contains("Gauge 1")){

                                                    // Remove the first occurance of RM from the arrayList
                                                    arrayList.remove("G - 1");
                                                    adapter.notifyDataSetChanged();

                                                } else if (alertDescription.contains("Gauge 2")){

                                                    // Remove the first occurance of RM from the arrayList
                                                    arrayList.remove("G - 2");
                                                    adapter.notifyDataSetChanged();

                                                } else if (alertDescription.contains("Gauge 3")){

                                                    // Remove the first occurance of RM from the arrayList
                                                    arrayList.remove("G - 3");
                                                    adapter.notifyDataSetChanged();

                                                } else if (alertDescription.contains("Gauge 4")){

                                                    // Remove the first occurance of RM from the arrayList
                                                    arrayList.remove("G - 4");
                                                    adapter.notifyDataSetChanged();

                                                }

                                            }

                                        } else if (payloadContent.startsWith("(T)")) {

                                            alertDescription = payloadContent.substring(3, payloadContent.length());
                                            alertDescriptionBank.add(alertDescription);

                                            if (alertDescription.contains("Attend")){

                                                colors.add("#FF8C00");
                                                arrayList.add("T"); // \n(" + timer.getText() + ")");
                                                adapter.notifyDataSetChanged();
                                                Vibrator tVib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                                                tVib.vibrate(vibrateT, -1);

                                            } else if (alertDescription.contains("User Responded")) {

                                                // Remove the first occurance of RM from the arrayList
                                                arrayList.remove("T");
                                                adapter.notifyDataSetChanged();

                                            } else if (alertDescription.contains("Timeout")) {

                                                // Remove the first occurance of RM from the arrayList
                                                arrayList.remove("T");
                                                adapter.notifyDataSetChanged();

                                            }

                                        } else if (payloadContent.startsWith("(RMAll)")) {  // All feedback methods

                                            alertDescription = payloadContent.substring(7, payloadContent.length());
                                            alertDescriptionBank.add(alertDescription);

                                            if (alertDescription.contains("Attend")){

                                                if (alertDescription.contains("Tank A")){

                                                    colors.add("#ff0000");
                                                    arrayList.add("RM - A"); // \n(" + timer.getText() + ")");
                                                    adapter.notifyDataSetChanged();
                                                    Vibrator redVib = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                                                    redVib.vibrate(vibrateRM, -1);
                                                    // This works to play the recording when the message is displayed.
                                                    final MediaPlayer rmsound = MediaPlayer.create(MainActivity.this, R.raw.recording);
                                                    rmsound.start();

                                                } else if (alertDescription.contains("Tank B")){

                                                    colors.add("#ff0000");
                                                    arrayList.add("RM - B"); // \n(" + timer.getText() + ")");
                                                    adapter.notifyDataSetChanged();
                                                    Vibrator redVib = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                                                    redVib.vibrate(vibrateRM, -1);
                                                    // This works to play the recording when the message is displayed.
                                                    final MediaPlayer rmsound = MediaPlayer.create(MainActivity.this, R.raw.recording);
                                                    rmsound.start();

                                                }

                                            } else if (alertDescription.contains("User Responded")) {

                                                if (alertDescription.contains("Tank A")){

                                                    // Remove the first occurance of RM from the arrayList
                                                    arrayList.remove("RM - A");
                                                    adapter.notifyDataSetChanged();

                                                } else if (alertDescription.contains("Tank B")){

                                                    // Remove the first occurance of RM from the arrayList
                                                    arrayList.remove("RM - B");
                                                    adapter.notifyDataSetChanged();

                                                }

                                            }

                                        } else if (payloadContent.startsWith("(LAll)")) {

                                            alertDescription = payloadContent.substring(6, payloadContent.length());
                                            alertDescriptionBank.add(alertDescription);

                                            if (alertDescription.contains("Attend")){

                                                if (alertDescription.contains("Red")){

                                                    colors.add("#FF8C00"); // value used by getView in arrayAdapter
                                                    // progressPoints -= 5;
                                                    arrayList.add("L - Red"); //\n(" + timer.getText() + ")");
                                                    adapter.notifyDataSetChanged();
                                                    Vibrator lVib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                                                    lVib.vibrate(vibrateL, -1);
                                                    // This works to play the recording when the message is displayed.
                                                    final MediaPlayer lsound = MediaPlayer.create(MainActivity.this, R.raw.recording);
                                                    lsound.start();

                                                } else if (alertDescription.contains("Green")){

                                                    colors.add("#FF8C00"); // value used by getView in arrayAdapter
                                                    // progressPoints -= 5;
                                                    arrayList.add("L - Green"); //\n(" + timer.getText() + ")");
                                                    adapter.notifyDataSetChanged();
                                                    Vibrator lVib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                                                    lVib.vibrate(vibrateL, -1);
                                                    // This works to play the recording when the message is displayed.
                                                    final MediaPlayer lsound = MediaPlayer.create(MainActivity.this, R.raw.recording);
                                                    lsound.start();

                                                }

                                            } else if (alertDescription.contains("User Responded")) {

                                                if (alertDescription.contains("Green")){

                                                    // Remove the first occurance of RM from the arrayList
                                                    arrayList.remove("L - Green");
                                                    adapter.notifyDataSetChanged();

                                                } else if (alertDescription.contains("Red")){

                                                    // Remove the first occurance of RM from the arrayList
                                                    arrayList.remove("L - Red");
                                                    adapter.notifyDataSetChanged();

                                                }

                                            } else if (alertDescription.contains("Timeout")) {

                                                if (alertDescription.contains("Green")){

                                                    // Remove the first occurance of RM from the arrayList
                                                    arrayList.remove("L - Green");
                                                    adapter.notifyDataSetChanged();

                                                } else if (alertDescription.contains("Red")){

                                                    // Remove the first occurance of RM from the arrayList
                                                    arrayList.remove("L - Red");
                                                    adapter.notifyDataSetChanged();

                                                }

                                            }

                                        } else if (payloadContent.startsWith("(GAll)")) {

                                            alertDescription = payloadContent.substring(6, payloadContent.length());
                                            alertDescriptionBank.add(alertDescription);

                                            if (alertDescription.contains("Attend")){

                                                if (alertDescription.contains("Gauge 1")){

                                                    colors.add("#FFFF00");
                                                    //progressPoints -= 3;
                                                    arrayList.add("G - 1"); //\n(" + timer.getText() + ")");
                                                    adapter.notifyDataSetChanged();
                                                    Vibrator gVib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                                                    gVib.vibrate(vibrateG, -1);
                                                    // This works to play the recording when the message is displayed.
                                                    final MediaPlayer gsound = MediaPlayer.create(MainActivity.this, R.raw.recording);
                                                    gsound.start();

                                                } else if (alertDescription.contains("Gauge 2")){

                                                    colors.add("#FFFF00");
                                                    //progressPoints -= 3;
                                                    arrayList.add("G - 2"); //\n(" + timer.getText() + ")");
                                                    adapter.notifyDataSetChanged();
                                                    Vibrator gVib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                                                    gVib.vibrate(vibrateG, -1);
                                                    // This works to play the recording when the message is displayed.
                                                    final MediaPlayer gsound = MediaPlayer.create(MainActivity.this, R.raw.recording);
                                                    gsound.start();

                                                } else if (alertDescription.contains("Gauge 3")){

                                                    colors.add("#FFFF00");
                                                    //progressPoints -= 3;
                                                    arrayList.add("G - 3"); //\n(" + timer.getText() + ")");
                                                    adapter.notifyDataSetChanged();
                                                    Vibrator gVib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                                                    gVib.vibrate(vibrateG, -1);
                                                    // This works to play the recording when the message is displayed.
                                                    final MediaPlayer gsound = MediaPlayer.create(MainActivity.this, R.raw.recording);
                                                    gsound.start();

                                                } else if (alertDescription.contains("Gauge 4")){

                                                    colors.add("#FFFF00");
                                                    //progressPoints -= 3;
                                                    arrayList.add("G - 4"); //\n(" + timer.getText() + ")");
                                                    adapter.notifyDataSetChanged();
                                                    Vibrator gVib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                                                    gVib.vibrate(vibrateG, -1);
                                                    // This works to play the recording when the message is displayed.
                                                    final MediaPlayer gsound = MediaPlayer.create(MainActivity.this, R.raw.recording);
                                                    gsound.start();

                                                }

                                            } else if (alertDescription.contains("User Responded")) {

                                                if (alertDescription.contains("Gauge 1")){

                                                    // Remove the first occurance of RM from the arrayList
                                                    arrayList.remove("G - 1");
                                                    adapter.notifyDataSetChanged();

                                                } else if (alertDescription.contains("Gauge 2")){

                                                    // Remove the first occurance of RM from the arrayList
                                                    arrayList.remove("G - 2");
                                                    adapter.notifyDataSetChanged();

                                                } else if (alertDescription.contains("Gauge 3")){

                                                    // Remove the first occurance of RM from the arrayList
                                                    arrayList.remove("G - 3");
                                                    adapter.notifyDataSetChanged();

                                                } else if (alertDescription.contains("Gauge 4")){

                                                    // Remove the first occurance of RM from the arrayList
                                                    arrayList.remove("G - 4");
                                                    adapter.notifyDataSetChanged();

                                                }

                                            } else if (alertDescription.contains("Timeout")) {

                                                if (alertDescription.contains("Gauge 1")){

                                                    // Remove the first occurance of RM from the arrayList
                                                    arrayList.remove("G - 1");
                                                    adapter.notifyDataSetChanged();

                                                } else if (alertDescription.contains("Gauge 2")){

                                                    // Remove the first occurance of RM from the arrayList
                                                    arrayList.remove("G - 2");
                                                    adapter.notifyDataSetChanged();

                                                } else if (alertDescription.contains("Gauge 3")){

                                                    // Remove the first occurance of RM from the arrayList
                                                    arrayList.remove("G - 3");
                                                    adapter.notifyDataSetChanged();

                                                } else if (alertDescription.contains("Gauge 4")){

                                                    // Remove the first occurance of RM from the arrayList
                                                    arrayList.remove("G - 4");
                                                    adapter.notifyDataSetChanged();

                                                }

                                            }

                                        } else if (payloadContent.startsWith("(TAll)")) {

                                            alertDescription = payloadContent.substring(6, payloadContent.length());
                                            alertDescriptionBank.add(alertDescription);

                                            if (alertDescription.contains("Attend")){

                                                colors.add("#FF8C00");
                                                arrayList.add("T"); // \n(" + timer.getText() + ")");
                                                adapter.notifyDataSetChanged();
                                                Vibrator tVib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                                                tVib.vibrate(vibrateT, -1);
                                                // This works to play the recording when the message is displayed.
                                                final MediaPlayer tsound = MediaPlayer.create(MainActivity.this, R.raw.recording);
                                                tsound.start();

                                            } else if (alertDescription.contains("User Responded")) {

                                                // Remove the first occurance of RM from the arrayList
                                                arrayList.remove("T");
                                                adapter.notifyDataSetChanged();

                                            } else if (alertDescription.contains("Timeout")) {

                                                // Remove the first occurance of RM from the arrayList
                                                arrayList.remove("T");
                                                adapter.notifyDataSetChanged();

                                            }


                                        } else if (payloadContent.startsWith("(UM)")) {
                                            alertDescription = payloadContent.substring(4, payloadContent.length());
                                            createMissionOverviewAlert("Mission: " + alertDescription);
                                            alertDescription = "MISSION UPDATED";
                                            Vibrator yellowVib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                                            yellowVib.vibrate(vibrateMessageUpdate, -1);
                                            createAlertDescription();
                                        }

                                        list = (ListView) findViewById(R.id.listEvents);
                                        list.setClickable(true);
                                        list.setAdapter(adapter);
                                        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                                alertDescription = alertDescriptionBank.get(position);                           //Display alert in alert array.
                                                createAlertDescription();                                                        //Depends on which cell the user clicks on in notification list
                                            }
                                        });

                                    }
                                } catch (Exception e) {

                                    Context context = getApplicationContext();
                                    CharSequence text = "Failed to Deque";
                                    int duration = Toast.LENGTH_SHORT;
                                    Toast toast = Toast.makeText(context, text, duration);
                                    toast.show();

                                }
                                dequeing.postDelayed(this, 1000);


                            }

                        });
                    }

                }.start();

            }
        });

        final AlertDialog dialogInput = builder.create();
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialogInput.getWindow().getAttributes());
        lp.width = 800;
        lp.height = 800;

        dialogInput.show();
        dialogInput.getWindow().setBackgroundDrawableResource(R.drawable.textview_custom);
        dialogInput.getWindow().setAttributes(lp);

        final Button neutralButton = dialogInput.getButton(AlertDialog.BUTTON_NEUTRAL);
        LinearLayout.LayoutParams posParams = (LinearLayout.LayoutParams) neutralButton.getLayoutParams();
        posParams.weight = 1;
        posParams.width = LinearLayout.LayoutParams.MATCH_PARENT;
        posParams.height = 100;
        posParams.gravity = Gravity.CENTER_HORIZONTAL;
        neutralButton.setGravity(Gravity.CENTER);
        neutralButton.setPadding(0,0,0,0);


        //************************************************ MQTT ************************************************

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.e(TAG, "PHONE onConnected: " + connectionHint);
                        // Now you can use the Data Layer API
                    }

                    @Override
                    public void onConnectionSuspended(int cause) {
                        Log.e(TAG, "PHONE onConnectionSuspended: " + cause);
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.e(TAG, "PHONE onConnectionFailed: " + result);
                    }
                })

                .addApi(Wearable.API)  // Request access only to the Wearable API
                .build();


        // Register the local broadcast receiver, defined in step 3.
        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
        MessageReceiver messageReceiver = new MessageReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);


    }

    public void createMissionOverviewAlert(String updatMessage) {
        final AlertDialog.Builder alertMission = new AlertDialog.Builder(MainActivity.this);
        alertMission.setPositiveButton(
                "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });


        missionAlert = alertMission.create();
        WindowManager.LayoutParams placement = missionAlert.getWindow().getAttributes();
        placement.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        placement.y = 100;   //y position

        missionAlert.getWindow().setBackgroundDrawableResource(R.drawable.textview_white);
        missionAlert.setMessage(updatMessage);

    }

    public void createAlertDescription() {
        final AlertDialog.Builder alertDescripe = new AlertDialog.Builder(MainActivity.this);
        alertDescripe.setPositiveButton(
                "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if(alertMissionFlag == 1){
                            missionAlert.show();
                            alertMissionFlag = 0;
                        }
                    }
                });


        describeAlert = alertDescripe.create();
        WindowManager.LayoutParams placement = describeAlert.getWindow().getAttributes();
        placement.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;

        describeAlert.getWindow().setBackgroundDrawableResource(R.drawable.textview_white);
        describeAlert.setMessage(alertDescription);


        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                describeAlert.show();

                TextView textView = (TextView) describeAlert.findViewById(android.R.id.message);
                textView.setTextSize(20);
                textView.setTextColor(Color.parseColor("#000000"));
                textView.setHeight(300);
                textView.setWidth(300);
                textView.setBackgroundResource(R.drawable.textview_white);

                final Button positiveButton = describeAlert.getButton(AlertDialog.BUTTON_POSITIVE);
                LinearLayout.LayoutParams posParams = (LinearLayout.LayoutParams) positiveButton.getLayoutParams();
                posParams.weight = 1;
                posParams.width = LinearLayout.LayoutParams.MATCH_PARENT;
                posParams.height = 100;
                posParams.gravity = Gravity.CENTER_HORIZONTAL;
                positiveButton.setGravity(Gravity.CENTER);
                positiveButton.setPadding(0,0,0,0);

                alertDescribeFlag = 1; // Alert is shown, set flag so that onSensorChange event knows to remove alert
                handler.removeCallbacksAndMessages(null);
            }
        }, 200); //Need to delay as the same threshold is used to display two different types of alert messages


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    /**
     * Take values and package them and then send them
     *
     * @param path unique string to bind calls tigether
     * @param map  dictionary of values to send to counterpart
     */
    private void packageAndSendMessage(final String path, final Map<String, String> map) {
        new Thread(new Runnable() {
            @Override
            public void run() {

                try {

                    String s = map.toString();
                    try {

                        JSONObject mainObject = new JSONObject(s);

                        NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await(); //can use await(5, TimeUnit.SECONDS);
                        for (Node node : nodes.getNodes()) {

                            if (node.isNearby()) { //ignore cloud - assumes one wearable attached


                                MessageApi.SendMessageResult sendMessageResult = Wearable.MessageApi.sendMessage(
                                        mGoogleApiClient, node.getId(), path, mainObject.toString().getBytes()).await();


                                if (sendMessageResult.getStatus().isSuccess()) {
                                    Log.e(TAG, "Message: {" + s + "} sent to: " + node.getDisplayName());

                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            counter++;
                                        }
                                    });

                                } else {
                                    // Log an error
                                    Log.e(TAG, "PHONE Failed to connect to Google Api Client with status " + sendMessageResult.getStatus());

                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                        }
                                    });
                                }
                            }
                        }

                    } catch (JSONException ex) {
                        Log.e(TAG, ex.toString());
                    }

                } catch (Exception ex) {
                    Log.e(TAG, ex.toString());
                }

            }
        }).start();
    }

    /**
     * Standard BroadcastReceiver called from ListenerService - with message as a JSON dictionary
     */
    public class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");

            try {
                JSONObject mainObject = new JSONObject(message);
                String request = mainObject.getString("request");
            } catch (JSONException ex) {
                Log.e(TAG, ex.toString());
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mResolvingError) {
            mGoogleApiClient.connect(); //connect to watch
        }
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect(); //disconnect from watch
        super.onStop();
    }
}
