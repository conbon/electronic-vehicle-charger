package evc;

import android.app.Activity;
import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import easicharge.conal.com.evc.R;

/**
 * Handle initial discovery of device via Udp
 *
 * @author Conal McLaughlin
 */
public class UdpConnectionManager extends Thread{
    private String TAG;
    private Activity activityContext;
    private Context context;

    private WifiManager mWifiManager;
    private DatagramSocket socket;

    private TextView broadcastValue;
    private TextView ipValue;
    private TextView receivedValue;

    private InetAddress broadcastAddress;
    public InetAddress getBroadcastAddress() {
        return broadcastAddress;
    }
    private void setBroadcastAddress(InetAddress broadcastAddress) {
        this.broadcastAddress = broadcastAddress;
        //update textview
        updateUI(broadcastValue, broadcastAddress.getHostAddress());
    }

    private InetAddress deviceAddress;
    public InetAddress getDeviceAddress() {
        return deviceAddress;
    }
    private void setDeviceAddress(InetAddress deviceAddress) {
        this.deviceAddress = deviceAddress;
        //update textview
        updateUI(ipValue, deviceAddress.getHostAddress());
    }

    private String dataReceived;
    public String getDataReceived() { return dataReceived; }
    private void setDataReceived(String dataReceived) {
        this.dataReceived = dataReceived;

        //update textview
        updateUI(receivedValue, dataReceived);
    }

    /**
     * Execute command on UI thread for any updates to screen
     */
    private void updateUI(final TextView tv, final String text) {
        activityContext.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tv.setText(text);
            }
        });
    }

    /**
     * Constructor
     * @param activityContext
     * @param context
     * @throws SocketException
     * @throws UnknownHostException
     */
    public UdpConnectionManager(Activity activityContext, Context context) throws SocketException, UnknownHostException{
        this.activityContext = activityContext;
        this.context = context;
        TAG = this.getClass().getSimpleName();

        broadcastValue = (TextView)activityContext.findViewById(R.id.broadcast_value);
        ipValue = (TextView)activityContext.findViewById(R.id.ip_value);
        receivedValue = (TextView)activityContext.findViewById(R.id.received_value);
    }

    /**
     * Start execution of thread
     */
    public void run(){
        try {
            socket = new DatagramSocket(EVCConstants.PORT);
            socket.setBroadcast(true);
            socket.setSoTimeout(EVCConstants.TIMEOUT);
            if(!calculateBroadcastAddress()) {
                Toast.makeText(context, EVCConstants.BROADCAST_ADDRESS_ERROR, Toast.LENGTH_SHORT).show();
            }
            sendBroadcast(socket);
            listenForResponses(socket);
        } catch(SocketException se) {
            Log.d(TAG, se.getMessage());
        } catch (IOException e) {
            Log.d(TAG, e.getMessage());
        }
    }

    /**
     * Send a broadcast UDP packet containing a request for device to announce itself
     *
     * @throws java.io.IOException
     */
    private void sendBroadcast(DatagramSocket socket) throws IOException {
        Log.d(TAG, "sendDiscoveryRequest :: entering");
        Log.d(TAG, "Sending data " + EVCConstants.DISCOVERY_REQUEST);

        if(broadcastAddress != null){
            DatagramPacket packet = new DatagramPacket(EVCConstants.DISCOVERY_REQUEST.getBytes(), EVCConstants.DISCOVERY_REQUEST.length(),
                    broadcastAddress, EVCConstants.PORT);
            socket.send(packet);
        } else {
            Log.e(TAG, "Broadcast Address is null");
        }
        Log.d(TAG, "sendDiscoveryRequest :: exiting");
    }

    /**
     * Listen on socket for responses, timing out after TIMEOUT_MS
     *
     * @param socket
     *          socket on which the discovery request was sent
     * @throws IOException
     */
    private void listenForResponses(DatagramSocket socket) throws IOException {
        Log.d(TAG, "listenForResponses :: entering");
        byte[] buffer = new byte[EVCConstants.PACKET_SIZE];
        try {
            boolean receivedResponse = false;
            do {
                Log.d(TAG, "Waiting on acknowledgement packet");
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String data = new String(packet.getData(), 0, packet.getLength());
                Log.d(TAG, "Packet received from: " + packet.getAddress());
                Log.d(TAG, "Received response " + data);

                if(data.equals(EVCConstants.DEVICE_DISCOVERED)){
                    receivedResponse = true;
                    setDeviceAddress(packet.getAddress());
                    //Save IP to shared preferences
                    MainActivity.settings.edit().putString("DEVICE_ADDRESS", deviceAddress.getHostAddress()).commit();
                    setDataReceived(data);
                }
            } while (!receivedResponse);
        } catch (SocketTimeoutException e) {
            Log.e(TAG, "SocketTimeoutException");
        } finally {
            socket.close();
        }
        Log.d(TAG, "listenForResponses :: exiting");
    }

    /**
     * Calculate the broadcast IP we need to send the packet along.
     */
    public boolean calculateBroadcastAddress() throws IOException {
        Log.d(TAG, "getBroadcastAddress :: entering");

        mWifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = mWifiManager.getDhcpInfo();
        if (dhcp == null) {
            Log.d(TAG, "Could not get dhcp info");
            return false;
        }

        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;

        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++){
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        }
        setBroadcastAddress(InetAddress.getByAddress(quads));

        //Save broadcast address to shared preferences
        MainActivity.settings.edit().putString("BROADCAST_ADDRESS", broadcastAddress.getHostAddress()).commit();

        Log.d(TAG, "getBroadcastAddress :: exiting");

        return true;
    }
}
