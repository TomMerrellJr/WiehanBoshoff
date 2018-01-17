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
    private int alertFlagRed = 0;
    private int alertFlagYellow = 0;
    private int alertFlagOrange = 0;
    private int alertDescribeFlag = 0;
    private int alertMissionFlag = 0;
    private long[] vibrateMessageUpdate = {0, 50, 25, 50, 25, 50};
    private long[] vibrateRed = {0, 500, 200, 500, 200, 500};
    private long[] vibrateOrange = {0, 350, 150, 350};
    private long[] vibrateYellow = {0, 200};
    Chronometer timer;
    private AlertDialog redAlert;
    private AlertDialog yellowAlert;
    private AlertDialog orangeAlert;
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
        final ProgressBar prg = (ProgressBar) findViewById(R.id.progressBar);
        final TextView pointPercentage = (TextView) findViewById(R.id.mciScore);
        pointPercentage.setText(progressPoints + "%");
        prg.setMax(100);
        prg.setProgress(progressPoints);
        Drawable draw = getResources().getDrawable(R.drawable.progress_custom);
        prg.setProgressDrawable(draw);
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
                        row.setBackgroundColor(Color.parseColor(colors.get(i)));
                    }
                }

                text.setTextSize(12);
                text.setGravity(Gravity.CENTER);
                text.setPadding(0,0,0,0);
                text.setTextColor(Color.parseColor("#000000"));
                return row;

            }
        };

        timer = (Chronometer) findViewById(R.id.timer);
        timer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer cArg) {
                long time = SystemClock.elapsedRealtime() - cArg.getBase();
                int h = (int) (time / 3600000);
                int m = (int) (time - h * 3600000) / 60000;
                int s = (int) (time - h * 3600000 - m * 60000) / 1000;
                String hh = h < 10 ? "0" + h : h + "";
                String mm = m < 10 ? "0" + m : m + "";
                String ss = s < 10 ? "0" + s : s + "";
                cArg.setText(hh + ":" + mm + ":" + ss);
            }
        });

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
                    timer.setBase(SystemClock.elapsedRealtime());
                    timer.start();

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
                                        if (payloadContent.startsWith("(H)")) {

                                            colors.add("#ff0000");
                                            progressPoints -= 7;
                                            arrayList.add("High Priority Alert\n(" + timer.getText() + ")");
                                            adapter.notifyDataSetChanged();

                                            Vibrator redVib = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);

                                            redVib.vibrate(vibrateRed, -1);

                                            final AlertDialog.Builder redAppAlert = new AlertDialog.Builder(MainActivity.this);
                                            redAppAlert.setPositiveButton(
                                                    "OK",
                                                    new DialogInterface.OnClickListener() {
                                                        public void onClick(DialogInterface dialog, int id) {
                                                            alertFlagRed = 0;
                                                            createAlertDescription();
                                                        }
                                                    });


                                            redAlert = redAppAlert.create();
                                            WindowManager.LayoutParams placement = redAlert.getWindow().getAttributes();
                                            placement.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;

                                            redAlert.getWindow().setBackgroundDrawableResource(R.drawable.textview_red);

                                            redAlert.setMessage("High Priority Alert Available\n(" + timer.getText() + ")");
                                            alertFlagRed = 1; // Alert is shown, set flag so that onSensorChange event knows to remove alert

                                            redAlert.setOnShowListener( new DialogInterface.OnShowListener() {
                                                @Override
                                                public void onShow(DialogInterface arg0) {
                                                    redAlert.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.WHITE);
                                                }
                                            });

                                            redAlert.show();
                                            TextView textView = (TextView) redAlert.findViewById(android.R.id.message);
                                            textView.setTextSize(20);
                                            textView.setGravity(Gravity.CENTER_HORIZONTAL);
                                            textView.setHeight(300);
                                            textView.setWidth(300);
                                            textView.setBackgroundResource(R.drawable.textview_red);

                                            final Button positiveButton = redAlert.getButton(AlertDialog.BUTTON_POSITIVE);
                                            LinearLayout.LayoutParams posParams = (LinearLayout.LayoutParams) positiveButton.getLayoutParams();
                                            posParams.weight = 1;
                                            posParams.width = LinearLayout.LayoutParams.MATCH_PARENT;
                                            posParams.height = 100;
                                            posParams.gravity = Gravity.CENTER_HORIZONTAL;
                                            positiveButton.setGravity(Gravity.CENTER);
                                            positiveButton.setPadding(0,0,0,0);

                                            alertDescription = payloadContent.substring(3, payloadContent.length());
                                            alertDescriptionBank.add(alertDescription);

                                        } else if (payloadContent.startsWith("(M)")) {
                                            colors.add("#FF8C00"); // value used by getView in arrayAdapter
                                            progressPoints -= 5;
                                            arrayList.add("Medium Priority Alert\n(" + timer.getText() + ")");
                                            adapter.notifyDataSetChanged();

                                            Vibrator orangeVib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                                            orangeVib.vibrate(vibrateOrange, -1);

                                            final AlertDialog.Builder orangeAppAlert = new AlertDialog.Builder(MainActivity.this);
                                            orangeAppAlert.setPositiveButton(
                                                    "OK",
                                                    new DialogInterface.OnClickListener() {
                                                        public void onClick(DialogInterface dialog, int id) {
                                                            alertFlagOrange = 0;
                                                            createAlertDescription();
                                                        }
                                                    });


                                            orangeAlert = orangeAppAlert.create();
                                            WindowManager.LayoutParams placement = orangeAlert.getWindow().getAttributes();
                                            placement.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;

                                            orangeAlert.getWindow().setBackgroundDrawableResource(R.drawable.textview_orange);

                                            orangeAlert.setMessage("Medium Priority Alert Available\n(" + timer.getText() + ")");
                                            alertFlagOrange = 1; // Alert is shown, set flag so that onSensorChange event knows to remove alert

                                            orangeAlert.setOnShowListener( new DialogInterface.OnShowListener() {
                                                @Override
                                                public void onShow(DialogInterface arg0) {
                                                        orangeAlert.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.WHITE);
                                                }
                                            });

                                            orangeAlert.show();
                                            TextView textView = (TextView) orangeAlert.findViewById(android.R.id.message);
                                            textView.setTextSize(20);
                                            textView.setGravity(Gravity.CENTER_HORIZONTAL);
                                            textView.setHeight(300);
                                            textView.setWidth(300);
                                            textView.setBackgroundResource(R.drawable.textview_orange);

                                            final Button positiveButton = orangeAlert.getButton(AlertDialog.BUTTON_POSITIVE);
                                            LinearLayout.LayoutParams posParams = (LinearLayout.LayoutParams) positiveButton.getLayoutParams();
                                            posParams.weight = 1;
                                            posParams.width = LinearLayout.LayoutParams.MATCH_PARENT;
                                            posParams.height = 100;
                                            posParams.gravity = Gravity.CENTER_HORIZONTAL;
                                            positiveButton.setGravity(Gravity.CENTER);
                                            positiveButton.setPadding(0,0,0,0);

                                            alertDescription = payloadContent.substring(3, payloadContent.length());
                                            alertDescriptionBank.add(alertDescription);

                                        } else if (payloadContent.startsWith("(L)")) {
                                            colors.add("#FFFF00");
                                            progressPoints -= 3;
                                            arrayList.add("Low Priority Alert\n(" + timer.getText() + ")");
                                            adapter.notifyDataSetChanged();

                                            Vibrator yellowVib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                                            yellowVib.vibrate(vibrateYellow, -1);


                                            final AlertDialog.Builder yellowAppAlert = new AlertDialog.Builder(MainActivity.this);
                                            yellowAppAlert.setPositiveButton(
                                                    "OK",
                                                    new DialogInterface.OnClickListener() {
                                                        public void onClick(DialogInterface dialog, int id) {
                                                            alertFlagYellow = 0;
                                                            createAlertDescription();
                                                        }
                                                    });


                                            yellowAlert = yellowAppAlert.create();
                                            WindowManager.LayoutParams placement = yellowAlert.getWindow().getAttributes();
                                            placement.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;

                                            yellowAlert.getWindow().setBackgroundDrawableResource(R.drawable.textview_yellow);

                                            yellowAlert.setMessage("Low Priority Alert Available\n(" + timer.getText() + ")");
                                            alertFlagYellow = 1; // Alert is shown, set flag so that onSensorChange event knows to remove alert

                                            yellowAlert.setOnShowListener( new DialogInterface.OnShowListener() {
                                                @Override
                                                public void onShow(DialogInterface arg0) {
                                                    yellowAlert.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK);
                                                }
                                            });

                                            yellowAlert.show();

                                            TextView textView = (TextView) yellowAlert.findViewById(android.R.id.message);
                                            textView.setTextSize(20);
                                            textView.setGravity(Gravity.CENTER_HORIZONTAL);
                                            textView.setHeight(300);
                                            textView.setWidth(300);
                                            textView.setBackgroundResource(R.drawable.textview_yellow);

                                            final Button positiveButton = yellowAlert.getButton(AlertDialog.BUTTON_POSITIVE);
                                            LinearLayout.LayoutParams posParams = (LinearLayout.LayoutParams) positiveButton.getLayoutParams();
                                            posParams.weight = 1;
                                            posParams.width = LinearLayout.LayoutParams.MATCH_PARENT;
                                            posParams.height = 100;
                                            posParams.gravity = Gravity.CENTER_HORIZONTAL;
                                            positiveButton.setGravity(Gravity.CENTER);
                                            positiveButton.setPadding(0,0,0,0);

                                            alertDescription = payloadContent.substring(3, payloadContent.length());
                                            alertDescriptionBank.add(alertDescription);

                                        } else if (payloadContent.startsWith("(UM)")) {
                                            alertDescription = payloadContent.substring(4, payloadContent.length());
                                            createMissionOverviewAlert("Mission: " + alertDescription);
                                            alertDescription = "MISSION UPDATED";
                                            Vibrator yellowVib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                                            yellowVib.vibrate(vibrateMessageUpdate, -1);
                                            createAlertDescription();
                                        }
                                        pointPercentage.setText(progressPoints + "%");
                                        prg.setMax(100);
                                        prg.setProgress(progressPoints);
                                        Drawable draw = getResources().getDrawable(R.drawable.progress_custom);
                                        prg.setProgressDrawable(draw);
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
