package zse.hackathon2017;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * Created by Grzechu on 25.03.2017.
 */
public class GetImageWorker implements Runnable {

    private final Server server;
    private final ServerSocket serverSocket;
    private final UUID token;
    private final long imageId;

    public GetImageWorker(Server server, ServerSocket serverSocket, UUID token, int imageId) {
        this.server = server;
        this.serverSocket = serverSocket;
        this.token = token;
        this.imageId = imageId;
    }

    @Override
    public void run() {

        Socket socket = null;
        try {
            System.out.println("LISTENING FOR RECEIVER");
            socket = serverSocket.accept();
            System.out.println("GOT RECEIVER");
            OutputStream output = socket.getOutputStream();

            PreparedStatement stmt = server.dbConn.prepareStatement("SELECT img FROM images WHERE id = ?;");
            stmt.setLong(1, imageId);

            ResultSet resultSet = stmt.executeQuery();
            if (!resultSet.next()) {
                return;
            }

            InputStream img = resultSet.getBinaryStream(1);

            int a;
            while ((a = img.read()) != -1) {
                output.write(a);
            }
            output.write(-1);
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (socket != null) {
                    socket.close();
                }
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
