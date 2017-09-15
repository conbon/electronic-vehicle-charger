package evc;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import easicharge.conal.com.evc.R;

/**
 * AsyncTask takes parameters as per connection type
 *
 * @author Conal McLaughlin
 */
public class AsyncTcpConnection extends AsyncTask<Object, Void, String> {
    private String TAG;
    protected TextView sentValue;
    protected TextView receivedValue;
    private Socket socket;
    private PrintWriter printWriter;

    private String status;
    private String param;
    private Activity activityContext;
    private Context context;
    private Integer PORT;
    private String IP;

    private static InputStreamReader inputStreamReader;
    private static BufferedReader bufferedReader;
    private static String message;

    private String dataSent;
    public String getdataSent() { return dataSent; }
    public void setdataSent(String dataSent) {
        this.dataSent = dataSent;
        //update textview
        updateUI(sentValue, dataSent);
    }

    private String received;
    public String getReceived() {
        return received;
    }
    public void setReceived(String received) {
        this.received = received;
        //update textview
        updateUI(receivedValue, received);
    }

    /**
     * Execute command on UI thread for any updates to screen
     *
     * @param tv
     * @param text
     */
    private void updateUI(final TextView tv, final String text) {
        activityContext.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tv.setText(text);
            }
        });
    }

    // constructor
    public AsyncTcpConnection(Activity activityContext, Context context, String status, String ip, Integer port) {
        this.activityContext = activityContext;
        this.context = context;
        this.status = status;
        this.IP = ip;
        this.PORT = port;

        init();
    }

    // overloaded constructor
    public AsyncTcpConnection(Activity activityContext, Context context, String status, String ip, Integer port, String param) {
        this.activityContext = activityContext;
        this.context = context;
        this.status = status;
        this.IP = ip;
        this.PORT = port;
        this.param = param;

        init();
    }

    private void init() {
        TAG = this.getClass().getSimpleName();
        sentValue = (TextView)activityContext.findViewById(R.id.sent_value);
        receivedValue = (TextView)activityContext.findViewById(R.id.received_value);
    }

    /**
     * Run by AsyncTask before executing doInBackground()
     */
    @Override
    public void onPreExecute() {
        String message;
        switch(status) {
            case EVCConstants.OVERRIDE_STATUS:
                message = "Sending override status to device";
                break;
            case EVCConstants.DATE_STATUS:
                message = "Sending date & time to device";
                break;
            case EVCConstants.STOP_STATUS:
                message = "Sending stop status to device";
                break;
            default:
                message = "Connecting";
                break;
        }

        //Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected String doInBackground(Object... objects) {
        Log.d(TAG, "Entering::doInBackground");
        String valueToSend = status;
        if(param != null) {
            valueToSend += param;
        }
        valueToSend += EVCConstants.STREAM_TERMINATION_CHARACTER;

        // Connect to server &
        // Write the message to output stream
        try {
            Log.d(TAG, "Socket setup");
            socket = new Socket(IP, PORT);
            printWriter = new PrintWriter(socket.getOutputStream(), true);
            printWriter.println(valueToSend);
            printWriter.flush();
            Log.d(TAG, "PrintWriter flushed");
            Log.d(TAG, "Status " + valueToSend + " sent to device");
            setdataSent(valueToSend);
            inputStreamReader = new InputStreamReader(socket.getInputStream());
            bufferedReader = new BufferedReader(inputStreamReader);
            Log.d(TAG, "Waiting for response from device");
            message = bufferedReader.readLine();
            Log.d(TAG, "Response received: " + message);
            setReceived(message);
        } catch (UnknownHostException e) {
            if(e != null) {
                Log.d(TAG, e.getMessage());
            }
        } catch (IOException e) {
            if(e != null) {
                Log.d(TAG, e.getMessage());
            }
        } finally {
            try{
                if(inputStreamReader != null)
                    inputStreamReader.close();

                if(bufferedReader != null)
                    bufferedReader.close();

                if(printWriter != null)
                   printWriter.close();

                if(socket != null)
                    socket.close();

                Log.d(TAG, "Closing socket and streams");
            } catch (IOException ioe) {
                Log.d(TAG, ioe.getMessage());
                Toast.makeText(context, ((ioe.getMessage() != null) ? ioe.getMessage() : "error"), Toast.LENGTH_SHORT).show();
            }
        }
        return message;
    }

    /**
     * Run after doInBackground() has finished executing
     * @param result
     */
    @Override
    protected void onPostExecute(String result) {
        //Toast.makeText(context, result, Toast.LENGTH_SHORT).show();
    }
}
