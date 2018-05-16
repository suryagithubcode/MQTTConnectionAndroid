package com.mqttconnection;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MqttAndroidClient" ;
    private static final int REQUEST_CODE_PERMISSION = 200 ;


    MqttAndroidClient client ;



    android.os.Handler customHandler;

    IMqttMessageListener iMqttMessageListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Marshmallow+
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,



            }, REQUEST_CODE_PERMISSION);

        } else {
            // Pre-Marshmallow
        }



        StringBuffer sbfiles = new StringBuffer();

        String path = Environment.getExternalStorageDirectory().toString() + "/QDSS";
        Log.d("Files", "Path: " + path);
        File directory = new File(path);
        File[] files = directory.listFiles();
        Log.d("Files", "Size: " + files.length);
        //sbfiles.append("message:resource_request");
       // sbfiles.append("{\"DeviceID\":\"QT10_29_3_4_40\",");
       // sbfiles.append("\"filelist\":");
        sbfiles.append("{\"message\":\"resources\",\"data\":[");

        for (int i = 0; i < files.length; i++) {

            sbfiles.append("{");
            sbfiles.append("\"FileName\":" + "\""+files[i].getName()+ "\""+ ",");

            sbfiles.append("\"FileLength\":"+ "\"" +getFileSize(files[i].length())+ "\"" + ",");

            Date date = new Date(files[i].lastModified());
            SimpleDateFormat dateformat = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss a");


            Log.d(TAG,"Display last modified time of file"+dateformat.format(date));
            sbfiles.append("\"LastModified\":" + "\""+ dateformat.format(date) + "\"");

            if(i==files.length-1)
            {
                sbfiles.append("}]");
            }

            else
            {
                sbfiles.append("},");
            }




            Log.d(TAG, "FileName:" + files[i].getName());

            Log.d(TAG, "Last Modified in ms:" + files[i].lastModified());

            Log.d(TAG, "File Length:" + files[i].length());

            Log.d(TAG,"File Length in format"+getFileSize(files[i].length()));

            String extFile = files[i].getName().substring(files[i].getName().lastIndexOf(".") + 1, files[i].getName().length());

            Log.d(TAG, "File Extension" + extFile);


            if (files[i].toString().endsWith(".jpg") || files[i].toString().endsWith(".jpeg")||files[i].toString().endsWith("png")) {
                //Images

                Log.d(TAG, "images in the folder " + files[i].getName());


            } else if (files[i].toString().endsWith(".mp4")) {
                //video

                Log.d(TAG, " Videos in the folder" + files[i].getName());
            }
        }

        sbfiles.append("}");


        final String remoteFileDetails = sbfiles.toString();

        Log.d(TAG,"Display list files in remote"+remoteFileDetails);




        String clientId = MqttClient.generateClientId();

              client =   new MqttAndroidClient(this.getApplicationContext(), "tcp://52.41.145.26:1883",
                        clientId);

        final String username = "smarthome";

        final String password = "smarthome";


        final IMqttMessageListener iMqttMessageListener = new IMqttMessageListener() {

            @Override

            public void messageArrived(String topic, MqttMessage message) throws Exception {

                Log.d(TAG,"Message received "+message);

                //JSONObject signallStatus = new JSONObject(message.toString());
                NotificationCompat.Builder mBuilder =
                        new NotificationCompat.Builder(getApplicationContext())
                                .setSmallIcon(R.mipmap.ic_launcher)
                                .setContentTitle(topic)
                                .setContentText(message.toString());
                Intent resultIntent = new Intent(getApplicationContext(), MainActivity.class);

                TaskStackBuilder stackBuilder = TaskStackBuilder.create(getApplicationContext());
                stackBuilder.addParentStack(MainActivity.class);
                stackBuilder.addNextIntent(resultIntent);
                PendingIntent resultPendingIntent =
                        stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                mBuilder.setContentIntent(resultPendingIntent);
                NotificationManager mNotificationManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                mNotificationManager.notify(100, mBuilder.build());


                String filedelstr = Environment.getExternalStorageDirectory().toString()+"/QDSS/"+message;

                File file = new File(filedelstr);
                boolean deleted = file.delete();

                Log.d(TAG,"Display file deleted state"+deleted);



                if(deleted)
                {



                    MqttConnectOptions options = new MqttConnectOptions();

                    try {
                        options.setUserName(username);
                        options.setPassword(password.toCharArray());


                        IMqttToken token = client.connect(options);

                        token.setActionCallback(new IMqttActionListener() {



                            @Override
                            public void onSuccess(IMqttToken asyncActionToken) {
                                // We are connected
                                Log.d(TAG, "onSuccess");

                                String topic = "QT10_29_3_4_40";
                                String payload = "200";
                                byte[] encodedPayload = new byte[0];
                                try {
                                    encodedPayload = payload.getBytes("UTF-8");
                                    MqttMessage message = new MqttMessage(encodedPayload);
                                    message.setRetained(false);
                                    client.publish(topic, message);

                                    Log.d(TAG, "onmsg published");


                                } catch (UnsupportedEncodingException | MqttException e) {
                                    e.printStackTrace();
                                }





                                catch (NullPointerException e) {
                                    e.printStackTrace();
                                }




                            }

                            @Override
                            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                                Log.d(TAG, "onFailure");

                            }
                        });
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }




                }





            }

        };
        MqttConnectOptions options = new MqttConnectOptions();

        try {
            options.setUserName(username);
            options.setPassword(password.toCharArray());


            IMqttToken token = client.connect(options);

            token.setActionCallback(new IMqttActionListener() {



                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // We are connected
                    Log.d(TAG, "onSuccess");

                    String topic = "QT10_29_3_4_40";
                    String payload = remoteFileDetails;
                    byte[] encodedPayload = new byte[0];
                    try {
                        encodedPayload = payload.getBytes("UTF-8");
                        MqttMessage message = new MqttMessage(encodedPayload);
                        message.setRetained(false);
                        client.publish(topic, message);

                        Log.d(TAG, "onmsg published");


                    } catch (UnsupportedEncodingException | MqttException e) {
                        e.printStackTrace();
                    }



                    int qos = 0;
                    try {


                       IMqttToken subToken = client.subscribe(topic, qos,iMqttMessageListener);
                        subToken.setActionCallback(new IMqttActionListener() {
                            @Override
                            public void onSuccess(IMqttToken asyncActionToken) {
                                // The message was subscribe

                                Log.d(TAG, "onmsg subscribe");

                            }

                            @Override
                            public void onFailure(IMqttToken asyncActionToken,
                                                  Throwable exception) {
                                // The subscription could not be performed, maybe the user was not
                                // authorized to subscribe on the specified topic e.g. using wildcards

                            }
                        });
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }

                    catch (NullPointerException e) {
                        e.printStackTrace();
                    }




                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.d(TAG, "onFailure");

                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }






    }




    public static String getFileSize(long size) {
        if (size <= 0)
            return "0";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }



    public static JSON fromStringToJSON(String jsonString){

        boolean isJsonArray = false;
        Object obj = null;

        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            Log.d("JSON", jsonArray.toString());
            obj = jsonArray;
            isJsonArray = true;
        }
        catch (Throwable t) {
            Log.e("JSON", "Malformed JSON: \"" + jsonString + "\"");
        }

        if (obj == null) {
            try {
                JSONObject jsonObject = new JSONObject(jsonString);
                Log.d("JSON", jsonObject.toString());
                obj = jsonObject;
                isJsonArray = false;
            } catch (Throwable t) {
                Log.e("JSON", "Malformed JSON: \"" + jsonString + "\"");
            }
        }

        return new JSON(obj, isJsonArray);
    }


    private Runnable updateTimerThread = new Runnable()
    {
        public void run()
        {

            Log.d("mmmm","Screen update in every second");

            try {
                IMqttToken token = client.connect();
                token.setActionCallback(new IMqttActionListener() {



                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        // We are connected
                        Log.d(TAG, "onSuccess");

                        String topic = "player";
                    /*String payload = "test";
                    byte[] encodedPayload = new byte[0];
                    try {
                        encodedPayload = payload.getBytes("UTF-8");
                        MqttMessage message = new MqttMessage(encodedPayload);
                        message.setRetained(true);
                        client.publish(topic, message);

                        Log.d(TAG, "onmsg published");


                    } catch (UnsupportedEncodingException | MqttException e) {
                        e.printStackTrace();
                    }*/


                        int qos = 0;
                        try {





                            IMqttToken subToken = client.subscribe(topic, qos,iMqttMessageListener);
                            subToken.setActionCallback(new IMqttActionListener() {
                                @Override
                                public void onSuccess(IMqttToken asyncActionToken) {
                                    // The message was subscribe

                                    Log.d(TAG, "onmsg subscribe");

                                }

                                @Override
                                public void onFailure(IMqttToken asyncActionToken,
                                                      Throwable exception) {
                                    // The subscription could not be performed, maybe the user was not
                                    // authorized to subscribe on the specified topic e.g. using wildcards

                                }
                            });
                        } catch (MqttException e) {
                            e.printStackTrace();
                        }
/*
                    catch (NullPointerException e) {
                        e.printStackTrace();
                    }*/




                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        Log.d(TAG, "onFailure");

                    }
                });
            } catch (MqttException e) {
                e.printStackTrace();
            }


            customHandler.postDelayed(this, 1000);
        }
    };

    public static class JSON {

        public Object obj = null;
        public boolean isJsonArray = false;

        JSON(Object obj, boolean isJsonArray){
            this.obj = obj;
            this.isJsonArray = isJsonArray;
        }
    }
}
