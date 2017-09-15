package evc;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Calendar;

import easicharge.conal.com.evc.R;
import evc.util.SwipeDismissTouchListener;
import mirko.android.datetimepicker.date.DatePickerDialog;
import mirko.android.datetimepicker.time.RadialPickerLayout;
import mirko.android.datetimepicker.time.TimePickerDialog;



/**
 * Main Activity
 *
 * @author Conal McLaughlin
 */
public class MainActivity extends Activity{

    private Context context;
    public static SharedPreferences settings;
    private static final String TAG = "MainActivity";

    private TextView tvDisplayTime;
    private TextView tvDisplayDate;
    private Switch m24;

    private final Calendar mCalendar = Calendar.getInstance();
    private int hourOfDay = mCalendar.get(Calendar.HOUR_OF_DAY);
    private int minute = mCalendar.get(Calendar.MINUTE);
    private int day = mCalendar.get(Calendar.DAY_OF_MONTH);
    private int month = mCalendar.get(Calendar.MONTH);
    private int year = mCalendar.get(Calendar.YEAR);
    private int newHourOfDay = hourOfDay;
    private int newMinute = minute;
    private int newDay = day;
    private int newMonth = month;
    private int newYear = year;
    private boolean dateChanged = false;

    private View contentView;
    private TextView broadcastValue;
    private TextView ipValue;
    private TextView sentValue;
    private TextView receivedValue;
    private TextView charging_at_label;
    private TextView charging_at_value;
    private Button stopOverrideBtn;
    private Button overrideBtn;
    private Button sendDataBtn;
    private UdpConnectionManager udpConnectionManager;
    private ConnectionBean currentSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_screen);
        init();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Restore preferences
        settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        boolean previouslyStarted = settings.getBoolean(getString(R.string.app_name), false);

        /**
         * Hardcoded ip of arduino device
         */
        //settings.edit().putString("DEVICE_ADDRESS", "192.168.0.177").commit();

        if(!previouslyStarted){
            SharedPreferences.Editor edit = settings.edit();
            edit.putBoolean(getString(R.string.app_name), Boolean.TRUE);
            edit.commit();
            LaunchHint();
        }

        // Retrieve IP every time app is resumed in case of change

        if(isConnectedToWifi()) {
            retrieveIP();
        } else {
            displayWifiAlert();
        }

        // check if charging time has been scheduled and display
        if(isSetToCharge()){
            Log.d(TAG, "isSetToCharge entering");
            String chargingAt = settings.getString("CHARGING_AT", "");
            StringBuilder builder = new StringBuilder();
            builder.append(chargingAt.substring(0, 2))
                   .append("/")
                   .append(chargingAt.substring(2, 4))
                   .append("/")
                   .append(chargingAt.substring(4, 8))
                   .append("  ")
                   .append(chargingAt.substring(8, 10))
                   .append(":")
                   .append(chargingAt.substring(10, 12));
            charging_at_value.append(builder.toString());
            charging_at_label.setVisibility(View.VISIBLE);
            charging_at_value.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Use connectivity manager to examine the current wifi state
     * @return
     */
    private boolean isConnectedToWifi() {
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getActiveNetworkInfo();//(ConnectivityManager.TYPE_WIFI);
        if(mWifi != null) {
            if (!mWifi.isConnected()) {
                Toast.makeText(context, EVCConstants.WIFI_CONNECTION_ERROR, Toast.LENGTH_SHORT).show();
                return false;
            }
        } else {
            return false;
        }

        return true;
    }

    /**
     * Initialise UI
     */
    private void init() {
        context = getApplicationContext();
        currentSession = new ConnectionBean();
        broadcastValue = (TextView) findViewById(R.id.broadcast_value);
        receivedValue = (TextView) findViewById(R.id.received_value);
        sentValue = (TextView) findViewById((R.id.sent_value));
        ipValue = (TextView) findViewById(R.id.ip_value);
        //charging_at_label = (TextView) findViewById(R.id.charging_at_label);
        //charging_at_value = (TextView) findViewById(R.id.charging_at_value);
        stopOverrideBtn = (Button) findViewById(R.id.stopOverride_btn);
        overrideBtn = (Button) findViewById(R.id.override_btn);
        sendDataBtn = (Button) findViewById(R.id.sendData_btn);

        tvDisplayTime = (TextView) findViewById(R.id.tvTime);
        tvDisplayDate = (TextView) findViewById(R.id.tvDate);

        resetDate();
        resetTime();

        tvDisplayDate.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // nothing to do, just to let onTouchListener work

            }
        });

        tvDisplayDate.setOnTouchListener(new SwipeDismissTouchListener(tvDisplayDate,
                null,
                new SwipeDismissTouchListener.DismissCallbacks() {
                    @Override
                    public boolean canDismiss(Object token) {
                        return true;
                    }

                    @Override
                    public void onDismiss(View view, Object token) {
                        resetDate();
                    }
                }));

        tvDisplayTime.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // nothing to do, just to let onTouchListener work

            }
        });

        tvDisplayTime.setOnTouchListener(new SwipeDismissTouchListener(tvDisplayTime,
                null,
                new SwipeDismissTouchListener.DismissCallbacks() {
                    @Override
                    public boolean canDismiss(Object token) {
                        return true;
                    }

                    @Override
                    public void onDismiss(View view, Object token) {
                        resetTime();
                    }
                }));

        m24 = (Switch) findViewById(R.id.switch1);
        m24.setChecked(true);
        m24.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean is24hmode) {
                if(m24.isChecked()){
                    buttonView.setChecked(true);
                    is24hmode=true;
                }
                else{
                    is24hmode=false;
                }
            }
        });

        /**
         * Initialise time/date pickers & setup listeners for
         * when the date/time has been set
         */
        final TimePickerDialog timePickerDialog12h = TimePickerDialog.newInstance(new mirko.android.datetimepicker.time.TimePickerDialog.OnTimeSetListener() {

            @Override
            public void onTimeSet(RadialPickerLayout view, int hourOfDay,
                                  int minute) {
                newHourOfDay = hourOfDay;
                newMinute = minute;
                Object c = appendTimeDescriptor(newHourOfDay);

                tvDisplayTime.setText(
                        new StringBuilder().append(adjust12h(newHourOfDay))
                                           .append(":")
                                           .append(padDigit(minute))
                                           .append(c));
                tvDisplayTime.setTextColor(getResources().getColor(android.R.color.holo_blue_light));
            }
        }, mCalendar.get(Calendar.HOUR_OF_DAY), mCalendar.get(Calendar.MINUTE), false);

        final TimePickerDialog timePickerDialog24h = TimePickerDialog.newInstance(new TimePickerDialog.OnTimeSetListener() {

            @Override
            public void onTimeSet(RadialPickerLayout view, int hourOfDay,
                                  int minute) {
                newHourOfDay = hourOfDay;
                newMinute = minute;
                tvDisplayTime.setText(new StringBuilder().append(padDigit(newHourOfDay))
                                                         .append(":")
                                                         .append(padDigit(newMinute)));

                tvDisplayTime.setTextColor(getResources().getColor(android.R.color.holo_blue_light));
            }
        }, mCalendar.get(Calendar.HOUR_OF_DAY), mCalendar.get(Calendar.MINUTE), true);

        final DatePickerDialog datePickerDialog = DatePickerDialog.newInstance(new DatePickerDialog.OnDateSetListener() {

            public void onDateSet(DatePickerDialog datePickerDialog, int year, int month, int day) {
                dateChanged = true;
                newDay = day;
                newMonth = month+1; // rectify 0 index for months
                newYear = year;
                tvDisplayDate.setText( new StringBuilder().append(padDigit(newDay))
                                                          .append(" ")
                                                          .append(padDigit(newMonth))
                                                          .append(" ")
                                                          .append(padDigit(newYear)));

                tvDisplayDate.setTextColor(getResources().getColor(android.R.color.holo_blue_light));
            }
        }, mCalendar.get(Calendar.YEAR), mCalendar.get(Calendar.MONTH), mCalendar.get(Calendar.DAY_OF_MONTH));

        /**
         * Setup click listeners for displaying date/time pickers
         */
        findViewById(R.id.btnChangeDate).setOnClickListener(new View.OnClickListener() {
            private String tag;

            @Override
            public void onClick(View v) {
                datePickerDialog.show(getFragmentManager(), tag);
            }
        });

        findViewById(R.id.btnChangeTime).setOnClickListener(new View.OnClickListener() {
            private String tag;

            @Override
            public void onClick(View v) {
                if(m24.isChecked()){
                    timePickerDialog24h.show(getFragmentManager(), tag);
                }
                else {
                    timePickerDialog12h.show(getFragmentManager(), tag);
                }
            }});

        stopOverrideBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    stopOverrideBtn.setBackgroundResource(R.drawable.black_marb);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    stopOverrideBtn.setBackgroundResource(R.drawable.red_marb);
                    sendStopSignal();
                }
                return true;
            }
        });

        overrideBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    overrideBtn.setBackgroundResource(R.drawable.black_marb);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    overrideBtn.setBackgroundResource(R.drawable.green_marb);
                    sendOverrideSignal();
                }
                return true;
            };
        });

        sendDataBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    sendDataBtn.setBackgroundResource(R.drawable.black_marb);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    sendDataBtn.setBackgroundResource(R.drawable.yellow_marb);
                    sendDateTime();
                }
                return true;
            };
        });
    }

    /**
     * Called on swiping the date/time displays
     * Resets values to original
     */
    private void resetTime() {
        tvDisplayTime.setText(new StringBuilder().append(padDigit(hourOfDay))
                .append(":").append(padDigit(minute)));
        tvDisplayTime.setTextColor(getResources().getColor(android.R.color.darker_gray));

        newMinute = minute;
        newHourOfDay = hourOfDay;

    }

    private void resetDate() {
        tvDisplayDate.setText(new StringBuilder().append(padDigit(day))
                .append(" ").append(padDigit(month + 1)).append(" ").append(padDigit(year)));
        tvDisplayDate.setTextColor(getResources().getColor(android.R.color.darker_gray));

        newDay = day;
        newMonth = month;
        newYear = year;
    }

    /**
     * Format parameter for display within time/date picker
     * eg. format month 1 -- > 01
     * @param c
     * @return formatted string to display in time/date
     */
    private static String padDigit(int c) {
        if (c >= 10)
            return String.valueOf(c);
        else
            return "0" + String.valueOf(c);
    }

    /**
     * Adjust hour for 12h time setting
     * @param c
     * @return
     */
    private static String adjust12h(int c) {
        if (c == 12)
            return String.valueOf(c) ;
        if (c == 00)
            return String.valueOf(c+12) ;
        if (c > 12)
            return String.valueOf(c-12) ;
        else
            return String.valueOf(c);
    }

    private static String appendTimeDescriptor(int c) {
        if (c == 12)
            return " PM";
        if (c == 00)
            return " AM";
        if (c > 12)
            return " PM";
        else
            return " AM";
    }

    /**
     * Send date/time data to device
     */
    private void sendDateTime() {
        if(isConnectedToWifi()) {
            if(!isIPRetrieved()) {
                retrieveIP();
            } else {
                String dateTimeParam = getDateTimeData();
                AsyncTask dateTimeConnection = new AsyncTcpConnection(this, this.getBaseContext(), EVCConstants.DATE_STATUS, settings.getString("DEVICE_ADDRESS", ""), EVCConstants.PORT, dateTimeParam);
                dateTimeConnection.execute();
                settings.edit().putString("CHARGING_AT", dateTimeParam);
            }
        }
    }

    /**
     * Send override signal to device
     */
    private void sendOverrideSignal() {
        if(isConnectedToWifi()) {
            if(!isIPRetrieved()) {
                retrieveIP();
            } else {
                AsyncTask overrideConnection = new AsyncTcpConnection(this, this.getBaseContext(), EVCConstants.OVERRIDE_STATUS, settings.getString("DEVICE_ADDRESS", ""), EVCConstants.PORT);
                overrideConnection.execute();
            }
        }
    }

    /**
     * Send stop signal to device
     */
    private void sendStopSignal() {
        if(isConnectedToWifi()) {
            if(!isIPRetrieved()) {
                retrieveIP();
            } else {
                AsyncTask stopOverrideConnection = new AsyncTcpConnection(this, this.getBaseContext(), EVCConstants.STOP_STATUS, settings.getString("DEVICE_ADDRESS", ""), EVCConstants.PORT);
                stopOverrideConnection.execute();
            }
        }
    }

    // build date-time string currently set on widget
    private String getDateTimeData() {
        // adjust date if scheduled without first using datepicker
        if(!dateChanged)
            newMonth++;

        StringBuilder dateTime = new StringBuilder();
        dateTime.append(padDigit(newDay))
                .append(padDigit(newMonth))
                .append(newYear)
                .append(padDigit(newHourOfDay))
                .append(padDigit(newMinute));

        return dateTime.toString();
    }

    /**
     * Discover IP using UDP
     * @return
     */
    private boolean retrieveIP() {
        try{
            udpConnectionManager = new UdpConnectionManager(this, this.getBaseContext());
            udpConnectionManager.start();

            //display info toast
            Toast.makeText(context, EVCConstants.DISCOVERY_MESSAGE, Toast.LENGTH_SHORT).show();
        } catch (SocketException e) {
            Log.d(TAG, e.getMessage());
            return false;
        } catch (UnknownHostException e) {
            Log.d(TAG, e.getMessage());
            return false;
        }
        return true;
    }

    private boolean isIPRetrieved() {
        if(settings.contains("DEVICE_ADDRESS")) {
            return true;
        }
        return false;
    }

    private boolean isSetToCharge() {
        if(settings.contains("CHARGING_AT")) {
            return true;
        }
        return false;
    }

    // popup for no wifi connection
    private void displayWifiAlert() {
        new AlertDialog.Builder(this)
                .setTitle("You are not connected to Wifi")
                .setMessage("To connect to your dyno device, you must enable Wifi")
                .setPositiveButton(R.string.wifi_settings, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void LaunchHint() {
        Intent launchNewIntent = new Intent(MainActivity.this, Hint.class);
        startActivityForResult(launchNewIntent, 0);
    }

    private void LaunchAbout() {
        Intent launchNewIntent = new Intent(MainActivity.this, About.class);
        startActivityForResult(launchNewIntent, 0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater mi = getMenuInflater();
        mi.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_hint:
                LaunchHint();
                break;
            case R.id.action_about:
                LaunchAbout();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }
}