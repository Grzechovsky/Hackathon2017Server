package zse.hackathon2017;

import zse.hackathon2017.Server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.UUID;

/**
 * Created by Grzechu on 25.03.2017.
 */
public class AddImageWorker implements Runnable {

    private Server server;
    private ServerSocket listener;
    private UUID token;
    private String channelName;

    public AddImageWorker(Server server, ServerSocket listener, UUID token, String channelName) {
        this.server = server;
        this.listener = listener;
        this.token = token;
        this.channelName = channelName;
    }

    @Override
    public void run() {
        Socket socket = null;
        try {
            System.out.println("LISTENING FOR SENDER");
            socket = listener.accept();
            System.out.println("GOT SENDER");
            InputStream input = socket.getInputStream();

//            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
//            int a;
//            while ((a = input.read()) != -1) {
//                buffer.write(a);
//            }


            PreparedStatement stmt = server.dbConn.prepareStatement(
                    "INSERT INTO images (channel_id, img, sender) " +
                        "VALUES (" +
                            "(SELECT id FROM channels WHERE name = ?), " +
                            "?, " +
                            "(SELECT user_id FROM tokens WHERE token = ?)" +
                        ");");

            stmt.setString(1, channelName);
            stmt.setBinaryStream(2, input);
            stmt.setObject(3, token, Types.OTHER);

            if (stmt.executeUpdate() == 1) {
                System.out.println("SUCCESSFULLY ADDED IMAGE");
            }
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (socket != null) {
                    socket.close();
                }
                listener.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
