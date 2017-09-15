package evc;

/**
 * Created by Conal McLaughlin on 04/03/15.
 */
public class ConnectionBean {
    public ConnectionBean() {}

    private String broadCastAddress;
    private String deviceAddress;

    public String getBroadCastAddress() {
        return broadCastAddress;
    }

    public void setBroadCastAddress(String broadCastAddress) {
        this.broadCastAddress = broadCastAddress;
    }

    public String getDeviceAddress() {
        return deviceAddress;
    }

    public void setDeviceAddress(String deviceAddress) {
        this.deviceAddress = deviceAddress;
    }
}
