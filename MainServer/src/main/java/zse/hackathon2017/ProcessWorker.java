package zse.hackathon2017;

import zse.hackathon2017.messages.*;
import zse.hackathon2017.responses.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
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

        if (segment.message instanceof CreateGroupMessage) {
            CreateGroupMessage createGroup = (CreateGroupMessage) segment.message;

            CreateGroupResponse r = new CreateGroupResponse();
//
            try {
                PreparedStatement stmt = server.dbConn.prepareStatement(
                        "WITH uid AS (\n" +
                                "    SELECT user_id \n" +
                                "    FROM tokens \n" +
                                "    WHERE token = ?\n" +
                                "), gid AS (\n" +
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
                                "INSERT INTO channel_members (user_id, group_id) \n" +
                                "VALUES ((SELECT * FROM uid), (SELECT * FROM gid));"
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

            AddChannelToGroupResponse r = new AddChannelToGroupResponse();

            try {
                PreparedStatement stmt = server.dbConn.prepareStatement(
                        "INSERT INTO channel_members (user_id, group_id) " +
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
            try {
                PreparedStatement stmt = server.dbConn.prepareStatement("SELECT name FROM channels INNER JOIN channel_members ON id = group_id WHERE user_id = (SELECT user_id FROM tokens WHERE token = ?);");
                stmt.setObject(1, segment.token.toString(), Types.OTHER);

                GetUserChannelsResponse 

            } catch (SQLException e) {
                e.printStackTrace();
            }
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
