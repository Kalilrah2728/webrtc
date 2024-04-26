package com.qifan.webrtcsamples.bhuviFile;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.Formatter;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.qifan.webrtcsamples.R;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class HomeActivity extends AppCompatActivity implements View.OnClickListener {


    private ServerSocket serverSocket;
    private Socket tempClientSocket;
    Thread serverThread = null;
    public static final int SERVER_PORT = 1997;
    private LinearLayout msgList;
    private Handler handler;
    private int greenColor;
    private EditText edMessage;

    TextView ip_add;

    public static List<UsrLst> lstSockt = new ArrayList<>();
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        setTitle("Server");
        greenColor = ContextCompat.getColor(this, R.color.colorPrimary);
        handler = new Handler();
        msgList = findViewById(R.id.msgList);
        edMessage = findViewById(R.id.edMessage);
        ip_add = findViewById(R.id.ip);

        WifiManager wifiMgr = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
        String ipAddress = Formatter.formatIpAddress(wifiInfo.getIpAddress());
        ip_add.setText("Your WiFi IP address: " + ipAddress);

        SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        RecyclerView recyclerView = findViewById(R.id.rcv);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));


        recyclerView.setAdapter(new MyAdapter(lstSockt));

        swipeRefreshLayout.setOnRefreshListener(() -> {

            recyclerView.setAdapter(new MyAdapter(lstSockt));
            fetchData(swipeRefreshLayout);
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // If not granted, request the permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Camera permission granted
                // Proceed with opening the camera
                Toast.makeText(this, "Camera permission Granted", Toast.LENGTH_SHORT).show();
            } else {
                // Camera permission denied
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void fetchData(SwipeRefreshLayout swipeRefreshLayout) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                swipeRefreshLayout.setRefreshing(false);
            }
        }, 2000);
    }

    public TextView textView(String message, int color) {
        if (null == message || message.trim().isEmpty()) {
            message = "<Empty Message>";
        }
        TextView tv = new TextView(this);
        tv.setTextColor(color);
        tv.setText(message + " [" + getTime() +"]");
        tv.setTextSize(20);
        tv.setPadding(0, 5, 0, 0);
        return tv;
    }

    public void showMessage(final String message, final int color) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                msgList.addView(textView(message, color));
            }
        });
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.start_server) {
            msgList.removeAllViews();
            findViewById(R.id.start_server).setVisibility(View.GONE);
            showMessage("Server Started.", Color.BLACK);
            this.serverThread = new Thread(new ServerThread());
            this.serverThread.start();
            return;
        }
        if (view.getId() == R.id.send_data) {
            String msg = edMessage.getText().toString().trim();
            showMessage("Server : " + msg, Color.BLUE);
            sendMessage(msg);
            if (msg.equals("StartStream")){
                startActivity(new Intent(this, VidoCallActivity.class));
                finish();
            }
        }
    }

    private void sendMessage(final String message) {
        List<UsrLst> checkedItemsLst = new ArrayList<>();
        for (UsrLst item : lstSockt) {
            if (item.isChecked()) {
                checkedItemsLst.add(item);
            }
        }

        if (!checkedItemsLst.isEmpty()){
            for (UsrLst ss : checkedItemsLst){
                try {
                    if (null != ss.getSockt() ) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                PrintWriter out = null;
                                try {
                                    out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(ss.getSockt().getOutputStream())), true);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                out.println(message);
                            }
                        }).start();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }


    }

    class ServerThread implements Runnable {

        public void run() {
            Socket socket;
            try {
                serverSocket = new ServerSocket(SERVER_PORT);
                //findViewById(R.id.start_server).setVisibility(View.GONE);
            } catch (IOException e) {
                e.printStackTrace();
                showMessage("Error Starting Server : " + e.getMessage(), Color.RED);
            }
            if (null != serverSocket) {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        socket = serverSocket.accept();
                        BufferedReader inputt = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                        lstSockt.add(new UsrLst(Objects.requireNonNull(socket.getInetAddress().getHostAddress()), socket, false));
                        CommunicationThread commThread = new CommunicationThread(socket);
                        new Thread(commThread).start();
                    } catch (IOException e) {
                        e.printStackTrace();
                        showMessage("Error Communicating to Client :" + e.getMessage(), Color.RED);
                    }
                }
            }
        }
    }

    class CommunicationThread implements Runnable {

        private Socket clientSocket;

        private BufferedReader input;

        public CommunicationThread(Socket clientSocket) {
            this.clientSocket = clientSocket;
            tempClientSocket = clientSocket;
            try {
                this.input = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
                showMessage("Error Connecting to Client!!", Color.RED);
            }
            showMessage("Connected to Client!!", greenColor);
        }

        public void run() {

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    String read = input.readLine();
                    if (null == read || "Disconnect".contentEquals(read)) {
                        Thread.interrupted();
                        read = "Client Disconnected";
                        showMessage("Client : " + read, greenColor);
                        break;
                    }
                    showMessage("Client : " + read, greenColor);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }

    }

    String getTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return sdf.format(new Date());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != serverThread) {
            sendMessage("Disconnect");
            serverThread.interrupt();
            serverThread = null;
        }
    }
}