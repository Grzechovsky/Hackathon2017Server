package zse.hackathon2017;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.StandardProtocolFamily;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Grzechu on 25.03.2017.
 */
public class Main {
    public static final int MAIN_SERVER_PORT = 24452;

    public static void main(String[] args) throws IOException, SQLException {
        DatagramChannel channel = DatagramChannel.open(StandardProtocolFamily.INET);
        channel.bind(new InetSocketAddress(InetAddress.getByName("192.168.0.100"), MAIN_SERVER_PORT));
        channel.configureBlocking(false);

        ExecutorService executorService = Executors.newFixedThreadPool(4);
        ExecutorService executorService2 = Executors.newFixedThreadPool(4);

        Connection dbConn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/hackathon2017", "postgres", "123456");

        Server server = new Server();
        server.main = channel;
        server.receiveThreads = executorService;
        server.processThreads = executorService2;
        server.dbConn = dbConn;

        Selector selector = Selector.open();
        SelectionKey key = channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        key.attach(server);

        while (selector.isOpen()) {
            selector.select(Long.MAX_VALUE);

            if (key.isReadable()) {
                executorService.submit(new ReceiveWorker(key));
            }

            if (key.isWritable()) {
                while (!server.outgoingQueue.isEmpty()) {
                    Outgoing outgoing = server.outgoingQueue.poll();
                    System.out.println("SENDING RESPONSE");
                    channel.send(outgoing.response, outgoing.respondTo);
                }
            }
        }

    }

}
