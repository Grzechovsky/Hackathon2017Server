package zse.hackathon2017;

import zse.hackathon2017.messages.*;
import zse.hackathon2017.responses.*;
import zse.hackathon2017.responses.GetChannelImagesResponse.ChannelImage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Created by Grzechu on 25.03.2017.
 */
public class ProcessWorker implements Runnable {

    private Server server;
    private Segment segment;
    private SocketAddress respondTo;

    public ProcessWorker(Server server, Segment segment, SocketAddress respondTo) {
        this.server = server;
        this.segment = segment;
        this.respondTo = respondTo;
    }

    @Override
    public void run() {
        Response response = null;
        System.out.println("MESSAGE " + segment.message);
        if (segment.message instanceof RegisterMessage) {
            RegisterMessage register = (RegisterMessage) segment.message;

            RegisterResponse r = new RegisterResponse();
            if (register.password.length != 32) {
                System.err.println("Incorrect password length");
                r.successful = false;
            } else {
                final byte[] salted = salt(register.password);
                try {
                    PreparedStatement stmt = server.dbConn.prepareStatement("INSERT INTO users (username, password) VALUES (?, ?)");
                    stmt.setString(1, register.username);
                    stmt.setBytes(2, salted);
                    stmt.execute();

                    r.token = createNewToken(register.username, salted);
                    r.successful = r.token != null;
                } catch (Exception e) {
                    e.printStackTrace();
                    r.token = null;
                    r.successful = false;
                }
            }
            response = r;
        }

        if (segment.message instanceof LoginMessage) {
            LoginMessage login = (LoginMessage) segment.message;

            if (login.password.length != 32) {
                System.err.println("Incorrect password length  " + login.password.length);
                return;
            }
            LoginResponse r = new LoginResponse();
            r.token = createNewToken(login.username, salt(login.password));
            r.successful = r.token != null;
            response = r;
        }

        if (segment.message instanceof CreateChannelMessage) {
            CreateChannelMessage createGroup = (CreateChannelMessage) segment.message;
            System.out.println("CreateChannelMessage !!!");
            CreateChannelResponse r = new CreateChannelResponse();
//
            try {
                PreparedStatement stmt = server.dbConn.prepareStatement(
                        "WITH uid AS (\n" +
                                "    SELECT user_id \n" +
                                "    FROM tokens \n" +
                                "    WHERE token = ?\n" +
                                "), cid AS (\n" +
                                "    INSERT INTO channels (name, owner) \n" +
                                "    VALUES (\n" +
                                "        ?, \n" +
                                "        (\n" +
                                "            SELECT user_id \n" +
                                "            FROM tokens \n" +
                                "            WHERE token = ?\n" +
                                "        )\n" +
                                "    ) RETURNING id\n" +
                                ")\n" +
                                "\n" +
                                "INSERT INTO channel_members (user_id, channel_id) \n" +
                                "VALUES ((SELECT * FROM uid), (SELECT * FROM cid));"
                );
                String uuid = segment.token.toString();
                System.out.println(uuid);
                stmt.setObject(1, uuid, Types.OTHER);
                stmt.setString(2, createGroup.name);
                stmt.setObject(3, uuid, Types.OTHER);

                r.success = stmt.executeUpdate() == 1;
            } catch (SQLException e) {
                r.success = false;
                e.printStackTrace();
            }
            response = r;
        }

        if (segment.message instanceof AddUserToChannelMessage) {
            AddUserToChannelMessage addUser = (AddUserToChannelMessage) segment.message;

            AddUserToChannelResponse r = new AddUserToChannelResponse();

            try {
                PreparedStatement stmt = server.dbConn.prepareStatement(
                        "INSERT INTO channel_members (user_id, channel_id) " +
                            "VALUES ((SELECT id FROM users WHERE username = ?), (SELECT id FROM channels WHERE name = ?));"
                );
                stmt.setString(1, addUser.username);
                stmt.setString(2, addUser.channelName);
                r.success = stmt.executeUpdate() == 1;
            } catch (SQLException e) {
                r.success = false;
                e.printStackTrace();
            }

        }

        if (segment.message instanceof GetUserChannelsMessage) {
            GetUserChannelsResponse r = new GetUserChannelsResponse();
            try {
                PreparedStatement stmt = server.dbConn.prepareStatement(
                        "SELECT name " +
                            "FROM channels " +
                            "INNER JOIN channel_members " +
                            "ON id = channel_id " +
                            "WHERE user_id = (SELECT user_id FROM tokens WHERE token = ?);"
                );
                stmt.setObject(1, segment.token.toString(), Types.OTHER);

                ResultSet resultSet = stmt.executeQuery();
                Set<String> set = new HashSet<>();

                while(resultSet.next()) {
                     set.add(resultSet.getString(1));
                }

                r.channelNames = set.toArray(new String[set.size()]);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            response = r;
        }

        if (segment.message instanceof GetChannelImagesMessage) {
            GetChannelImagesMessage getChannelImages = (GetChannelImagesMessage) segment.message;
            GetChannelImagesResponse r = new GetChannelImagesResponse();

            try {
                PreparedStatement stmt = server.dbConn.prepareStatement(
                        "SELECT images.id, username " +
                            "FROM images " +
                            "INNER JOIN users " +
                            "ON users.id = sender " +
                            "WHERE channel_id = (SELECT id FROM channels WHERE name = ?) " +
                            "ORDER BY id DESC;"
                );
                stmt.setString(1, getChannelImages.channelName);

                ResultSet resultSet = stmt.executeQuery();
                Set<ChannelImage> ids = new HashSet<>();

                while (resultSet.next()) {
                    ids.add(new ChannelImage(resultSet.getInt(1), resultSet.getString(2)));
                }

                r.images = ids.toArray(new ChannelImage[ids.size()]);
            } catch (SQLException e) {
                r.images = null;
                e.printStackTrace();
            }

            response = r;
        }

        if (segment.message instanceof AddImageMessage) {
            AddImageMessage addImage = (AddImageMessage) segment.message;
            AddImageResponse r = new AddImageResponse();

            try {
                ServerSocket serverSocket = new ServerSocket(0);
                server.transerThreads.submit(new AddImageWorker(server, serverSocket, segment.token, addImage.channelName));
                r.socketAddress = new InetSocketAddress("192.168.0.100", serverSocket.getLocalPort());
            } catch (IOException e) {
                r.socketAddress = null;
                e.printStackTrace();
            }

            response = r;
        }

        if (segment.message instanceof GetImageMessage) {
            GetImageMessage getImage = (GetImageMessage) segment.message;
            GetImageResponse r = new GetImageResponse();

            try {
                ServerSocket serverSocket = new ServerSocket(0);
                server.transerThreads.submit(new GetImageWorker(server, serverSocket, segment.token, getImage.imageId));
                r.socketAddress = new InetSocketAddress("192.168.0.100", serverSocket.getLocalPort());
            } catch (IOException e) {
                r.socketAddress = null;
                e.printStackTrace();
            }

            response = r;
        }

        if (response != null) {
            try {
                ByteArrayOutputStream buff = new ByteArrayOutputStream();
                ObjectOutputStream out = new ObjectOutputStream(buff);
                out.writeObject(response);

                Outgoing outgoing = new Outgoing();
                outgoing.respondTo = respondTo;
                outgoing.response = ByteBuffer.wrap(buff.toByteArray());

                server.outgoingQueue.add(outgoing);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private UUID createNewToken(String username, byte[] password) {
        try {
            PreparedStatement stmt = server.dbConn.prepareStatement(
                    "INSERT INTO tokens (user_id, token, expires) " +
                        "VALUES ((SELECT id FROM users WHERE username = ? AND password = ?), uuid_generate_v4(), now() + interval '1 day') " +
                        "RETURNING token;"
            );
            stmt.setString(1, username);
            stmt.setBytes(2, password);

            ResultSet resultSet = stmt.executeQuery();
            resultSet.next();
            return UUID.fromString(resultSet.getString(1));
        } catch (Exception ex) {
            return null;
        }
    }

    private static final byte[] PRE = new byte[]{
            -43, -110, -72, 64, 29, 116, 22, 20, -95, 70, -9, 23, 99, -105, -94, 112, -124, -68, 79, -118, -18, -112
    };

    private static final byte[] POST = new byte[]{
            55, 76, 46, -51, -98, -8, 75, 43, 96, -103, 35, 18, 2, 53, 69, 108, 105, -15, -12, 54, -107, 72
    };

    private byte[] salt(byte[] password) {
        byte[] salted = new byte[22 + 32 + 22];
        System.arraycopy(PRE, 0, salted, 0, 22);
        System.arraycopy(password, 0, salted, 22, password.length);
        System.arraycopy(POST, 0, salted, 22 + 32, 22);

        try {
            MessageDigest instance = MessageDigest.getInstance("SHA-256");
            return instance.digest(salted);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return password;
    }

}
