package com.hfad.wificonnservice;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class WiFiService extends Service {

    private WifiManager wifiManager;
    private WifiConfiguration wifiConfig;
    private int networkID;
    private String ssid;
    private List<ScanResult> results;
    private BroadcastReceiver wifiReceiver;
    private ArrayList<String> arrayList = new ArrayList<>();


    private Socket client;
    private FileInputStream fileInputStream;
    private BufferedInputStream bufferedInputStream;
    private OutputStream outputStream;
    private Button button;
    private TextView text;
    public static final String ipaddr = "192.168.0.1"; //server IP address
    public static final int portId = 2000;

    public WiFiService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        //throw new UnsupportedOperationException("Not yet implemented");
        return null;
    }


    @Override
    public void onCreate() {
        super.onCreate();

        //Enable Wifi
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        try{
            if (!wifiManager.isWifiEnabled()) {
                //Toast.makeText(this, "WiFi is disabled ... We need to enable it", Toast.LENGTH_LONG).show();
                wifiManager.setWifiEnabled(true);
            }
        }catch (Exception e){
            Toast.makeText(this, "wifiManager.isWifiEnabled() returned null", Toast.LENGTH_LONG).show();
        }



        //For Network Scanning
        ssid = "TEST-MOSAIC-B01";
        wifiNetworkConfig();


        //Receiver
        wifiReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                results = wifiManager.getScanResults();
                //Toast.makeText(getApplicationContext(),"scan results received!!", Toast.LENGTH_LONG).show();
                //
                arrayList.clear();

                if(!results.isEmpty()) {
                    Toast.makeText(getApplicationContext(),"scan results received!!", Toast.LENGTH_LONG).show();

                    for (ScanResult scanResult : results) {
                        arrayList.add(scanResult.SSID + " - " + scanResult.capabilities);

                        //Broadcast to MainActivity
                        Intent intent_MainAct = new Intent("wifi_updates");
                        intent_MainAct.putExtra("scanlist", arrayList);
                        sendBroadcast(intent_MainAct);

                        //Toast.makeText(getApplicationContext(), scanResult.SSID, Toast.LENGTH_LONG).show();

                        if(scanResult.SSID.trim().equals(ssid)){
                            //Toast.makeText(getApplicationContext(), "matched", Toast.LENGTH_LONG).show();
                            wifiManager.disconnect();
                            unregisterReceiver(this);
                            wifiManager.enableNetwork(networkID, true);
                            boolean success = wifiManager.reconnect();
                            if(success) {
                                Toast.makeText(getApplicationContext(), "Connected to AIS", Toast.LENGTH_LONG).show();
                                Log.d("doInBack","Connected to AIS");
                                commAIS();
                                break;
                            }else {
                                wifiManager.reassociate();
                                Toast.makeText(getApplicationContext(), "Not connected to AIS", Toast.LENGTH_LONG).show();
                                break;
                            }
                        }
                    }
                }
                else{
                    Toast.makeText(getApplicationContext(),"Empty List!!", Toast.LENGTH_LONG).show();
                    //registerWifi();
                    //startScan();
                }
            }
        };

        registerWifi();
        startScan();
    }

    private void wifiNetworkConfig() {

        wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = String.format("\"%s\"", ssid);
        wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        networkID = wifiManager.addNetwork(wifiConfig);
    }


    private void registerWifi() {
        arrayList.clear();
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

    }

    private void startScan() {

        wifiManager.startScan();
        Toast.makeText(this, "Scanning WiFi ...", Toast.LENGTH_SHORT).show();
    }

    private void commAIS(){

        //Customize PoolSize
        int corePoolSize = 60;
        int maximumPoolSize = 80;
        int keepAliveTime = 10;

        BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<Runnable>(maximumPoolSize);
        Executor threadPoolExecutor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, TimeUnit.SECONDS, workQueue);

        Client myClient = new Client(ipaddr, portId, getApplicationContext());
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            myClient.executeOnExecutor(threadPoolExecutor); //AsyncTask.THREAD_POOL_EXECUTOR
        } else {
            myClient.execute();
        }

    }



    public class Client extends AsyncTask<String, String, TcpClient> {

        String destAddress;
        int destPort;
        String response = "";
        private ArrayList<String> arrayList;
        private final Context mContext;
        Socket socket = null;
        TcpClient mTcpClient;

        Client(String addr, int port, Context mContext) {
            destAddress = addr;
            destPort = port;
            //this.arrayList = arrayList;
            this.mContext = mContext;
            //Toast.makeText(mContext, "Constr.: ", Toast.LENGTH_LONG).show();
        }


        @Override
        protected void onPreExecute() {
            Toast.makeText(mContext, "onPreExecute", Toast.LENGTH_LONG).show();
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            //response received from server
            Log.d("doInBack", "response " + values[0]);
            //process server response here....
        }

        @Override
        protected TcpClient doInBackground(String... message) {

            //Toast.makeText(mContext, "In do", Toast.LENGTH_LONG).show();
            Log.d("doInBack","IN BACKGROUND");
            //we create a TCPClient object
            mTcpClient = new TcpClient(new TcpClient.OnMessageReceived() {
                @Override
                //here the messageReceived method is implemented
                public void messageReceived(String message) {
                    //this method calls the onProgressUpdate
                    publishProgress(message);
                }
            });
            mTcpClient.run();

            return null;
            //ConnectSocket();

            /*
            try {

                ByteBuffer byteBuffer = ByteBuffer.allocate(4);
                byteBuffer.putInt(0,192);
                byteBuffer.putInt(1,168);
                byteBuffer.putInt(2,0);
                byteBuffer.putInt(3,1);
                byte[] addr = byteBuffer.array();
                InetAddress serverAddr = InetAddress.getByAddress(addr);
                //SocketAddress socketAddress = new InetSocketAddress("192.168.0.1",2000);
                do {
                    //socket = new Socket(serverAddr, 2000);
                    socket = new Socket();
                    socket.connect(new InetSocketAddress("192.168.0.1",2000),100);
                    Log.d("doInBack",socket.getRemoteSocketAddress().toString());
                    if(socket.isConnected())
                        Log.d("doInBack","socketConnected");

                }while(socket == null);

                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(10);
                byte[] buffer = new byte[10];

                int bytesRead;

                InputStream inputStream = socket.getInputStream();


                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    byteArrayOutputStream.write(buffer, 0, bytesRead);
                    response += byteArrayOutputStream.toString("UTF-8");
                }

                //response = "";

            } catch (UnknownHostException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                //Toast.makeText(mContext, "UnknownHostException: ", Toast.LENGTH_LONG).show();
                Log.d("doInBack","UnknownHostException");
                response = "UnknownHostException: " + e.toString();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Log.d("doInBack",e.toString());
                //Toast.makeText(mContext, "IOException: ", Toast.LENGTH_LONG).show();
                response = "IOException: " + e.toString();
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }*/

            /*
            try {
                int port = 2000;
                //InetAddress serverAddr = new InetAddress();
                InetAddress serverAddr = InetAddress.getByName("192.168.0.1");
                InetAddress.getByAddress()
                DatagramSocket dsocket = new DatagramSocket(port, serverAddr);
                if(dsocket.isConnected())
                    Log.d("doInBack","socketConnected");

                byte[] buffer = new byte[512];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                while (true) {

                    dsocket.receive(packet);
                    response = new String(buffer, 0, packet.getLength());
                    Log.i("UDP packet received", response);
                    //data.setText(lText);

                    packet.setLength(buffer.length);
                }
            } catch (Exception e) {
                System.err.println(e);
                Log.d("doInBack",e.toString());
                e.printStackTrace();
            }
*/
            //Toast.makeText(mContext, response, Toast.LENGTH_LONG).show();
            //return response;
        }

        @Override
        protected void onPostExecute(TcpClient tcpClient) {
            super.onPostExecute(tcpClient);
            /*arrayList.clear();
            arrayList.add(response);

            //Broadcast to MainActivity
            Intent intent_MainAct = new Intent("wifi_updates");
            intent_MainAct.putExtra("scanlist", arrayList);
            mContext.sendBroadcast(intent_MainAct);*/
        }

        /*
        @Override
        protected void onCancelled() {
            super.onCancelled();
            if (mTcpClient != null) {
                mTcpClient.stopClient();
            }
        }*/

        /*
        public void ConnectSocket(){
            //Log.i(, "Running ConnectSocket");

            socket = new Socket();
            Runnable connectSocket = new SetUpSocketConnection();
            //communicationThread = new CommunicationThread();
            new Thread(connectSocket).start();
        }

        private class SetUpSocketConnection implements Runnable{
            @Override
            public void run(){
                try{
                    //ERROR IS HERE:
                    socket.connect(new InetSocketAddress("192.168.0.1", 2000));

                    writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                    socket.setSoTimeout(800);

                    inputStream = socket.getInputStream();
                    reader = new BufferedReader(new InputStreamReader(inputStream), 1000);

                    Log.d(TAG, "Connected to Socket");
                    //communicationThreadRunning = true;
                    //new Thread(communicationThread).start();
                } catch (IOException e){
                    e.printStackTrace();
                    Log.d("doInBack",e.toString());
                }
            }
        }*/

    }

    /*
    @Override
    public void onDestroy() {
        super.onDestroy();
        if(wifiReceiver != null)
            unregisterReceiver(wifiReceiver);

    }*/
}
