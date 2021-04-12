package com.test.camera;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends Activity {

    private EditText editTextIPAddress;
    private EditText editTextPortAddress;
    private InputMethodManager imm;
    private String server  = "192.168.0.100";
    private String port  = "5001";
    private String camdir = "0";

    private Button buttonConnect;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if(Build.VERSION.SDK_INT >= 23){
            int permissionCheck = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA);

            if (permissionCheck == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, 0);
            }
        }
        setContentView(R.layout.intro);

        editTextIPAddress = (EditText)this.findViewById(R.id.editTextIPAddress);
        editTextPortAddress = (EditText)this.findViewById(R.id.editTextPortAddress);

        buttonConnect  = (Button)this.findViewById(R.id.buttonConnect);
        buttonConnect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(),Tutorial4.class);

                server = editTextIPAddress.getText().toString();
                port = editTextPortAddress.getText().toString();

                intent.putExtra("server", server);
                intent.putExtra("port", port);
                intent.putExtra("camdir", camdir);

                startActivity(intent);
            }
        });
    }
}