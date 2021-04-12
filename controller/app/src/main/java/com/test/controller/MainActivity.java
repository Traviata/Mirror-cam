package com.test.controller;

import android.Manifest;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;

import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.test.permission.PermissionsActivity;
import com.test.permission.PermissionsChecker;

import org.freedesktop.gstreamer.GStreamer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback{

    private static final String[] PERMISSIONS = new String[]{Manifest.permission.ACCESS_NETWORK_STATE,Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.INTERNET,Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private native void nativeInit(String addr, String vport);     // Initialize native code, build pipeline, etc
    private native void nativeSnapshot(String addr, String vport);
    private native void nativeFinalize(); // Destroy pipeline and shutdown native code
    private native void nativeSetUri(String uri); // Set the URI of the media to play
    private native void nativePlay();     // Set pipeline to PLAYING
    private native void nativePause();    // Set pipeline to PAUSED
    private static native boolean nativeClassInit(); // Initialize native class: cache Method IDs for callbacks
    private native void nativeSurfaceInit(Object surface); // A new surface is available
    private native void nativeSurfaceFinalize(); // Surface about to be destroyed
    private long native_custom_data;      // Native code will use this to keep private data

    private boolean is_playing_desired;   // Whether the user asked to go to PLAYING
    private int position;                 // Current position, reported by native code
    private int duration;                 // Current clip duration, reported by native code
    private boolean is_local_media;       // Whether this clip is stored locally or is being streamed
    private int desired_position;         // Position where the users wants to seek to
    private String mediaUri;              // URI of the clip being played

    private final String defaultMediaUri = "http://docs.gstreamer.com/media/sintel_trailer-368p.ogv";
    private String vport="8554";
    private int cam = 1;

    private TextView IPAddress;
    private String server  = "192.168.0.100";

    private static ServerSocket serverSocket;
    private static int cport = 8888;
    private OutputStream outs;

    private Button buttonSetChange;
    private Button buttonUp;
    private Button buttonLeftTurn;
    private Button buttonRightTurn;
    private Button buttonDown;
    private Button buttonCenter;

    private SurfaceView sv;
    private SurfaceHolder sh;
    private LinearLayout main;

    // Called when the activity is first created.
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if(Build.VERSION.SDK_INT >= 23){
            PermissionsChecker checker;
            checker = new PermissionsChecker(this);
            if (checker.lacksPermissions(PERMISSIONS)) {
                startPermissionsActivity(PERMISSIONS);
            }
        }

        // Initialize GStreamer and warn if it fails
        try {
            GStreamer.init(this);
            //Toast.makeText(getApplicationContext(), "Gstreamer Set success", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        IPAddress = (TextView)this.findViewById(R.id.editTextIPAddress);
        //editTextIPAddress.setText(server);
        sv = (SurfaceView) this.findViewById(R.id.surface_video);
        sh = sv.getHolder();
        sh.addCallback(this);

        ImageButton play = (ImageButton) this.findViewById(R.id.button_play);
        ImageButton pause = (ImageButton) this.findViewById(R.id.button_stop);
        //buttonSetChange        = (Button)this.findViewById(R.id.SetChange);
        buttonUp        = (Button)this.findViewById(R.id.buttonUp);
        buttonLeftTurn  = (Button)this.findViewById(R.id.buttonLeftTurn);
        buttonRightTurn = (Button)this.findViewById(R.id.buttonRightTurn);
        buttonDown      = (Button)this.findViewById(R.id.buttonDown);
        buttonCenter    = (Button)this.findViewById(R.id.buttonCenter);

        WifiManager myWifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        WifiInfo myWifiInfo = myWifiManager.getConnectionInfo();
        int ip = myWifiInfo.getIpAddress();

        String ipAddress = String.format(
                "%d.%d.%d.%d",
                (ip & 0xff),
                (ip >> 8 & 0xff),
                (ip >> 16 & 0xff),
                (ip >> 24 & 0xff));

        IPAddress.setText(ipAddress);

        play.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                is_playing_desired = true;
                nativePlay();
            }
        });


        pause.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                is_playing_desired = false;
                nativePause();
            }
        });


        buttonUp.setOnClickListener(new View.OnClickListener() {
            String sndOpkey;

            @Override
            public void onClick(View v) {
                sndOpkey = "Up";
                try {
                    outs.write(sndOpkey.getBytes("UTF-8"));
                    outs.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        buttonDown.setOnClickListener(new View.OnClickListener() {
            String sndOpkey;

            @Override
            public void onClick(View v) {
                sndOpkey = "Down";
                try {
                    outs.write(sndOpkey.getBytes("UTF-8"));
                    outs.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        buttonLeftTurn.setOnClickListener(new View.OnClickListener() {
            String sndOpkey;

            @Override
            public void onClick(View v) {
                sndOpkey = "Left";
                try {
                    outs.write(sndOpkey.getBytes("UTF-8"));
                    outs.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        buttonRightTurn.setOnClickListener(new View.OnClickListener() {
            String sndOpkey;

            @Override
            public void onClick(View v) {
                sndOpkey = "Right";
                try {
                    outs.write(sndOpkey.getBytes("UTF-8"));
                    outs.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        buttonCenter.setOnClickListener(new View.OnClickListener() {
            String sndOpkey;

           @Override
            public void onClick(View v) {
               sndOpkey = "Stop";
               try {
                   outs.write(sndOpkey.getBytes("UTF-8"));
                   outs.flush();
               } catch (IOException e) {
                   e.printStackTrace();
               }
           }
        });


        //sv = (SurfaceView) this.findViewById(R.id.surface_video);
        //SurfaceHolder sh = sv.getHolder();
        //sh.addCallback(this);

        // Retrieve our previous state, or initialize it to default values
        if (savedInstanceState != null) {
            is_playing_desired = savedInstanceState.getBoolean("playing");
            position = savedInstanceState.getInt("position");
            duration = savedInstanceState.getInt("duration");
            mediaUri = savedInstanceState.getString("mediaUri");
            Log.i ("GStreamer", "Activity created with saved state:");
        } else {
            is_playing_desired = false;
            position = duration = 0;
            mediaUri = defaultMediaUri;
            Log.i ("GStreamer", "Activity created with no saved state:");
        }
        is_local_media = false;
        Log.i ("GStreamer", "  playing:" + is_playing_desired + " position:" + position +
                " duration: " + duration + " uri: " + mediaUri);

        // Start with disabled buttons, until native code is initialized
        //this.findViewById(R.id.button_play).setEnabled(false);
        //this.findViewById(R.id.button_stop).setEnabled(false);

        nativeInit(server, vport);

        is_playing_desired = true;
        nativePlay();

        /*
        try {

			serverSocket = new ServerSocket(cport);

			editTextIPAddress.setText("Client Searching !! ");

			while (true) {

					Socket clientSocket = serverSocket.accept();


					if(clientSocket.isConnected()) {
						editTextIPAddress.setText("Connected Client IP : " + clientSocket.getRemoteSocketAddress());
						outs = clientSocket.getOutputStream();

						break;
					}
			}

	    } catch (IOException e) {
	    	System.out.println("Exception: " + e);
	    }

	    try{ Thread.sleep(100);}catch(Exception e){}

        nativeInit(server, vport);
        */
        Thread ClientFind = new Thread(new CFind());
        ClientFind.start();
    }



    protected void onSaveInstanceState (Bundle outState) {
        Log.d ("GStreamer", "Saving state, playing:" + is_playing_desired + " position:" + position +
                " duration: " + duration + " uri: " + mediaUri);
        outState.putBoolean("playing", is_playing_desired);
        outState.putString("mediaUri", mediaUri);
    }

    private void startPermissionsActivity(String[] permission) {
        PermissionsActivity.startActivityForResult(this, 0, permission);
    }

    protected void onDestroy() {

        try {
            serverSocket.close();
        } catch (IOException e) {
            System.out.println("Exception: " + e);
        }

        nativeFinalize();
        super.onDestroy();
    }

    // Called from native code. This sets the content of the TextView from the UI thread.
    private void setMessage(final String message) {
        final TextView tv = (TextView) this.findViewById(R.id.textview_message);
        runOnUiThread (new Runnable() {
            public void run() {
                tv.setText(message);
            }
        });
    }

    // Set the URI to play, and record whether it is a local or remote file
    private void setMediaUri() {
        nativeSetUri (mediaUri);
        is_local_media = mediaUri.startsWith("file://");
    }

    // Called from native code. Native code calls this once it has created its pipeline and
    // the main loop is running, so it is ready to accept commands.
    private void onGStreamerInitialized () {
        Log.i ("GStreamer", "GStreamer initialized:");
        Log.i ("GStreamer", "  playing:" + is_playing_desired + " position:" + position + " uri: " + mediaUri);

        // Restore previous playing state
        setMediaUri ();
        //nativeSetPosition (position);
        if (is_playing_desired) {
            nativePlay();
        } else {
            nativePause();
        }

        // Re-enable buttons, now that GStreamer is initialized
        final Activity activity = this;
        runOnUiThread(new Runnable() {
            public void run() {
                // activity.findViewById(R.id.button_play).setEnabled(true);
                //activity.findViewById(R.id.button_stop).setEnabled(true);
            }
        });
    }

    static {
        System.loadLibrary("gstreamer_android");
        System.loadLibrary("tutorial-4");
        nativeClassInit();
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        Log.d("GStreamer", "Surface changed to format " + format + " width "
                + width + " height " + height);
        nativeSurfaceInit (holder.getSurface());
    }

    public void surfaceCreated(SurfaceHolder holder) {

        Log.d("GStreamer", "Surface created: " + holder.getSurface());
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d("GStreamer", "Surface destroyed");
        nativeSurfaceFinalize ();
    }

    // Called from native code when the size of the media changes or is first detected.
    // Inform the video surface about the new size and recalculate the layout.
    private void onMediaSizeChanged (int width, int height) {
        Log.i ("GStreamer", "Media size changed to " + width + "x" + height);
        final GStreamerSurfaceView gsv = (GStreamerSurfaceView) this.findViewById(R.id.surface_video);
        gsv.media_width = width;
        gsv.media_height = height;
        runOnUiThread(new Runnable() {
            public void run() {
                gsv.requestLayout();
            }
        });
    }


    public class CFind implements Runnable {
        public void run() {
            try {
                serverSocket = new ServerSocket(cport);
                //editTextIPAddress.setText("Client Searching !! ");
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    final SocketAddress Saddr = clientSocket.getRemoteSocketAddress();
                    if(clientSocket.isConnected()) {
                        //editTextIPAddress.setText("Connected Client IP : " + clientSocket.getRemoteSocketAddress());
                     /*   editTextIPAddress.post(new Runnable() {
                            public void run() {
                                editTextIPAddress.setText("Connected Client IP : " + Saddr);
                            }
                        });*/
                        outs = clientSocket.getOutputStream();
                        break;
                    }
                }
            } catch (IOException e) {
                System.out.println("Exception: " + e);
            }
        }
    }
}
