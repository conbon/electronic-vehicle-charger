package easicharge.conal.com.evc.util;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

/**
 * Created by conal on 15/12/14.
 */
public class utils {

    public static boolean execCmd(String command, ArrayList<String> results){
        Process process;
        try {
            process = Runtime.getRuntime().exec(new String [] {"sh", "-c", command});
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        int result;
        try {
            result = process.waitFor();
        } catch (InterruptedException e1) {
            e1.printStackTrace();
            return false;
        }

        if(result != 0){ //error executing command
            Log.d("execCmd", "result code : " + result);
            String line;
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            try {
                while ((line = bufferedReader.readLine()) != null){
                    if(results != null) results.add(line);
                    Log.d("execCmd", "Error: " + line);
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            return false;
        }

        //Command execution is OK
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        String line;
        try {
            while ((line = bufferedReader.readLine()) != null){
                if(results != null) results.add(line);
                Log.d("execCmd", line);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static ArrayList<String> scanSubNet(String subnet){
        ArrayList<String> hosts = new ArrayList<String>();

        Log.d("scanSubNet :: ", "entering");

        InetAddress inetAddress = null;
        for(int i=1; i<10; i++){
            Log.d("easicharge", "Trying: " + subnet + String.valueOf(i));
            try {
                inetAddress = InetAddress.getByName(subnet + String.valueOf(i));
                if(inetAddress.isReachable(1000)){
                    hosts.add(inetAddress.getHostName());
                    Log.d("easicharge", inetAddress.getHostName());
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return hosts;
    }

    public static InetAddress getBroadcastAddress(Context context) throws IOException {
        Log.d("easiCharge", "getBroadcastAddress :: entering");
        WifiManager mWifiManager=(WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = mWifiManager.getDhcpInfo();
        if (dhcp == null) {
            Log.d("easiCharge", "Could not get dhcp info");
            return null;
        }

        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;

        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++){
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        }

        Log.d("", InetAddress.getByAddress(quads).toString());
        return InetAddress.getByAddress(quads);}
}
