package com.test.camera;

import android.app.Activity;
import android.hardware.Camera;
import android.support.v7.app.AppCompatActivity;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import org.freedesktop.gstreamer.GStreamer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class Tutorial4 extends AppCompatActivity implements SurfaceHolder.Callback, SeekBar.OnSeekBarChangeListener {
    private native void nativeInit(String addr, String vport, String camdir);     // Initialize native code, build pipeline, etc
    private native void nativeFinalize(); // Destroy pipeline and shutdown native code
    private native void nativeSetUri(String uri); // Set the URI of the media to play
    private native void nativePlay();     // Set pipeline to PLAYING
    private native void nativeSetPosition(int milliseconds); // Seek to the indicated position, in milliseconds
    private native void nativePause();    // Set pipeline to PAUSED
    private static native boolean nativeClassInit(); // Initialize native class: cache Method IDs for callbacks
    private native void nativeSurfaceInit(Object surface); // A new surface is available
    private native void nativeSurfaceFinalize(); // Surface about to be destroyed
    private long native_custom_data;      // Native code will use this to keep private data
    private native void nativepicture();

    private boolean is_playing_desired;   // Whether the user asked to go to PLAYING
    private int position;                 // Current position, reported by native code
    private int duration;                 // Current clip duration, reported by native code
    private boolean is_local_media;       // Whether this clip is stored locally or is being streamed
    private int desired_position;         // Position where the users wants to seek to
    private String mediaUri;              // URI of the clip being played

    private static final int CAMERA_FACING = Camera.CameraInfo.CAMERA_FACING_BACK;

    private CameraPreview mCameraPreview;
    private SurfaceView surfaceView;

    private SurfaceView sv;
    private SurfaceHolder sh;

    private String vport="8555";
    private String server  = "192.168.0.100";
    private String camdir = "0";


    private static ServerSocket serverSocket;
    private static int cport = 8888;
    private OutputStream outs;

    private Socket socket;
    private int port = 8888;

    private UsbSerialDriver driver;
    private UsbDeviceConnection connection;
    UsbSerialPort uport;
    public byte com[];

    private TextView TestText;

    private final String defaultMediaUri = "http://docs.gstreamer.com/media/sintel_trailer-368p.ogv";

    // Called when the activity is first created.
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        surfaceView = findViewById(R.id.surfaceView);

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy =
                    new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        //gsv = (GStreamerSurfaceView) this.findViewById(R.id.surface_video);

        // Initialize GStreamer and warn if it fails
        try {
            GStreamer.init(this);
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
            return;
        }


        com =new byte[1];
        InitArduino();
        TestText  = (TextView)this.findViewById(R.id.testtext);

        Intent intent = getIntent();

        server = intent.getStringExtra("server");
        vport = intent.getStringExtra("port");
        camdir = intent.getStringExtra("camdir");

        try{

            if(socket!=null)
            {
                socket.close();
                socket = null;
            }

            socket = new Socket(server, port);

            Thread thread = new Thread(new rcvthread(socket, socket.getRemoteSocketAddress()));
            thread.start();

        } catch (UnknownHostException e) {
            Toast.makeText(getApplicationContext(), "Socket error", Toast.LENGTH_LONG).show();
        } catch (IOException e){
            e.printStackTrace();
        }

        ImageButton play = (ImageButton) this.findViewById(R.id.button_play);
        play.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                is_playing_desired = true;
                nativePlay();
            }
        });

        ImageButton pause = (ImageButton) this.findViewById(R.id.button_stop);
        pause.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if(socket!=null)
                {
                    exitFromRunLoop();
                    try{
                        socket.close();
                        socket = null;


                    } catch (IOException e){

                        e.printStackTrace();
                    }
                }
                is_playing_desired = false;
                nativePause();
            }
        });


        //LinearLayout lp=new LinearLayout(this);
        //lp.removeView ();

        sv = (SurfaceView) this.findViewById(R.id.surface_video);
        sh = sv.getHolder();
        sh.addCallback(this);

        SeekBar sb = (SeekBar) this.findViewById(R.id.seek_bar);
        sb.setOnSeekBarChangeListener(this);

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
        this.findViewById(R.id.button_play).setEnabled(false);
        this.findViewById(R.id.button_stop).setEnabled(false);

        nativeInit(server, vport, "0");

        is_playing_desired = true;
        nativePlay();
    }

    void InitArduino() {
        try {

            UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
            List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
            if (availableDrivers.isEmpty()) {
                return;
            }
            driver = availableDrivers.get(0);
            connection = manager.openDevice(driver.getDevice());
            if (connection == null) {
                return;
            }
            uport = driver.getPorts().get(0);
            uport.open(connection);
            uport.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            uport.purgeHwBuffers(true, true);
        } catch (IOException e) {
            System.out.println("IOException occured.");
        }
    }

    public void CommandWrite() {
        try {
            uport.write(com,200);
        } catch (IOException e) {
            System.out.println("IOException occured.");
        }
    }

    protected void onSaveInstanceState (Bundle outState) {
        Log.d ("GStreamer", "Saving state, playing:" + is_playing_desired + " position:" + position +
                " duration: " + duration + " uri: " + mediaUri);
        outState.putBoolean("playing", is_playing_desired);
        outState.putInt("position", position);
        outState.putInt("duration", duration);
        outState.putString("mediaUri", mediaUri);
    }

    protected void onDestroy() {
        nativeFinalize();
        super.onDestroy();

        if (connection != null) {
            try {
                uport.close();
            } catch (IOException e) {
                System.out.println("IOException occured.");
            }
        }
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
        nativeSetPosition (position);
        if (is_playing_desired) {
            nativePlay();
        } else {
            nativePause();
        }

        // Re-enable buttons, now that GStreamer is initialized
        final Activity activity = this;
        runOnUiThread(new Runnable() {
            public void run() {
                activity.findViewById(R.id.button_play).setEnabled(true);
                activity.findViewById(R.id.button_stop).setEnabled(true);
            }
        });
    }

    // The text widget acts as an slave for the seek bar, so it reflects what the seek bar shows, whether
    // it is an actual pipeline position or the position the user is currently dragging to.
    private void updateTimeWidget () {
        final TextView tv = (TextView) this.findViewById(R.id.textview_time);
        final SeekBar sb = (SeekBar) this.findViewById(R.id.seek_bar);
        final int pos = sb.getProgress();

        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        final String message = df.format(new Date (pos)) + " / " + df.format(new Date (duration));
        tv.setText(message);
    }

    // Called from native code
    private void setCurrentPosition(final int position, final int duration) {
        final SeekBar sb = (SeekBar) this.findViewById(R.id.seek_bar);

        // Ignore position messages from the pipeline if the seek bar is being dragged
        if (sb.isPressed()) return;

        runOnUiThread (new Runnable() {
            public void run() {
                sb.setMax(duration);
                sb.setProgress(position);
                updateTimeWidget();
            }
        });
        this.position = position;
        this.duration = duration;
    }

    static {
        System.loadLibrary("gstreamer_android");
        System.loadLibrary("tutorial-4");
        nativeClassInit();
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
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

    // The Seek Bar thumb has moved, either because the user dragged it or we have called setProgress()
    public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
        if (fromUser == false) return;
        desired_position = progress;
        // If this is a local file, allow scrub seeking, this is, seek as soon as the slider is moved.
        if (is_local_media) nativeSetPosition(desired_position);
        updateTimeWidget();
    }

    // The user started dragging the Seek Bar thumb
    public void onStartTrackingTouch(SeekBar sb) {
        nativePause();
    }

    // The user released the Seek Bar thumb
    public void onStopTrackingTouch(SeekBar sb) {
        // If this is a remote file, scrub seeking is probably not going to work smoothly enough.
        // Therefore, perform only the seek when the slider is released.
        if (!is_local_media) nativeSetPosition(desired_position);
        if (is_playing_desired) nativePlay();
    }

    void exitFromRunLoop(){
        try {
            String sndOpkey = "[close]";
            outs.write(sndOpkey.getBytes("UTF-8"));
            outs.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class rcvthread implements Runnable {

        private static final int sizeBuf = 50;
        private Socket 			clientSocket;
        private SocketAddress 	clientAddress;
        private int rcvBufSize;
        private byte[] rcvBuf = new byte[sizeBuf];

        public rcvthread(Socket clientSocket, SocketAddress clientAddress) {
            this.clientSocket  = clientSocket;
            this.clientAddress = clientAddress;
        }

        public void run() {
            try {
                InputStream ins   = clientSocket.getInputStream();
                OutputStream outs = clientSocket.getOutputStream();

                while ((rcvBufSize = ins.read(rcvBuf)) != -1) {
                    final String rcvData = new String(rcvBuf, 0, rcvBufSize, "UTF-8");

                    if (rcvData.compareTo("Up") == 0)  {
                        TestText.post(new Runnable() {
                            public void run() {
                                TestText.setText("Go!");
                            }
                        });

                        com[0] = 0x55;
                        //com[1] = 2;

                        CommandWrite();
                    }

                    if (rcvData.compareTo("Left") == 0) {
                        TestText.post(new Runnable() {
                            public void run() {
                                TestText.setText("LeftTurn!");
                            }
                        });
                        com[0] = 0x4C;
                        //com[1] = 10;

                        //CommandWrite();
                        uport.write(com,200);
                    }

                    if (rcvData.compareTo("Right") == 0) {
                        TestText.post(new Runnable() {
                            public void run() {
                                TestText.setText("RightTurn!");
                            }
                        });
                        com[0] = 0x52;
                        //com[1] = 10;

                        CommandWrite();
                    }

                    if (rcvData.compareTo("Down") == 0) {
                        TestText.post(new Runnable() {
                            public void run() {
                                TestText.setText("Back!");
                            }
                        });
                        com[0] = 0x44;
                        //com[1] = 2;

                        CommandWrite();

                    }

                    if (rcvData.compareTo("Stop") == 0) {
                        //Thread.interrupted();
                        Intent intent = new Intent(getApplicationContext(), CamView.class);
                        startActivity(intent);
                        //mCameraPreview.takePicture();
                        //nativepicture();
                    }

                    TestText.post(new Runnable() {
                        public void run() {
                            TestText.setText("Received data : " + rcvData + " (" + clientAddress + ")");
                        }
                    });
                    //TestText.setText("Received data : " + rcvData + " (" + clientAddress + ")");
                    outs.write(rcvBuf, 0, rcvBufSize);
                }
                System.out.println(clientSocket.getRemoteSocketAddress() + " Closed");

            } catch (IOException e) {
                System.out.println("Exception: " + e);
            } finally {
                try {
                    clientSocket.close();
                    TestText.post(new Runnable() {
                        public void run() {
                            TestText.setText("Disconnected! Client IP : " + clientAddress);
                        }
                    });

                    //TestText.setText("Disconnected! Client IP : " + clientAddress);
                } catch (IOException e) {
                    System.out.println("Exception: " + e);
                }
            }
        }
    }
    }
