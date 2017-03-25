package zse.hackathon2017;

import java.nio.channels.DatagramChannel;
import java.sql.Connection;
import java.util.concurrent.ExecutorService;

/**
 * Created by Grzechu on 25.03.2017.
 */
public class Server {

    public DatagramChannel main = null;
    public ExecutorService receiveThreads = null;
    public ExecutorService processThreads = null;
    public Connection dbConn;
}
