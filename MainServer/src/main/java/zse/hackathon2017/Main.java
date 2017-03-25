package zse.hackathon2017;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.StandardProtocolFamily;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

/**
 * Created by Grzechu on 25.03.2017.
 */
public class Main {
    public static final int MAIN_SERVER_PORT = 24452;

    public static void main(String[] args) throws IOException {
        DatagramChannel channel = DatagramChannel.open(StandardProtocolFamily.INET);
        channel.bind(new InetSocketAddress(InetAddress.getLocalHost(), MAIN_SERVER_PORT));

        Selector selector = Selector.open();
        channel.register(selector, SelectionKey.OP_ACCEPT | SelectionKey.OP_READ);



        channel.receive()

    }

}
