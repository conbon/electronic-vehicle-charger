package evc;

/**
 * App constants
 *
 * @author Conal McLaughlin
 */
public final class EVCConstants {

    public static final String DISCOVERY_MESSAGE = "Retrieving device IP!";

    public static final String DISCOVERY_REQUEST = "evc-connect";

    public static final String DEVICE_DISCOVERED = "acknowledged";

    public static final int PACKET_SIZE = 12;

    public static final String OVERRIDE_STATUS = "51";

    public static final String STOP_STATUS = "50";

    public static final String DATE_STATUS = "52";

    public static final String STREAM_TERMINATION_CHARACTER = "z";

    public static final String RECEIVED = "200";

    public static final int PORT = 6969;

    public static final int TIMEOUT = 4000;


    // error messages
    public static final String BROADCAST_ADDRESS_ERROR = "Could not retrieve broadcast address";

    public static final String WIFI_CONNECTION_ERROR = "You are not connected to a network!";

}
