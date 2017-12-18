package goodstadt.me.uk.androidsendm;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
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
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
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
    // Unique tag for the error dialog fragment
    private static final String DIALOG_ERROR = "dialog_error";
    // Request code to use when launching the resolution activity
    private static final int REQUEST_RESOLVE_ERROR = 1001;

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
                                    Log.e(TAG, "PHONE Failed to connect to Google Api Client with status "
                                            + sendMessageResult.getStatus());

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
