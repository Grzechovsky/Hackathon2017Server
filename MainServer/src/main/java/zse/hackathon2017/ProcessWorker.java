package zse.hackathon2017;

import zse.hackathon2017.messages.LoginMessage;
import zse.hackathon2017.messages.RegisterMessage;
import zse.hackathon2017.responses.LoginResponse;
import zse.hackathon2017.responses.RegisterResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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

            if (register.password.length != 32) {
                System.err.println("Incorrect password length");
                return;
            }

            try {
                PreparedStatement stmt = server.dbConn.prepareStatement("INSERT INTO users (username, password) VALUES (?, ?)");
                stmt.setString(1, register.username);
                stmt.setBytes(2, salt(register.password));
                stmt.execute();
            } catch (SQLException e) {
                e.printStackTrace();
            }



            try {
                PreparedStatement stmt = server.dbConn.prepareStatement("INSERT INTO tokens (user_id, token, expires) VALUES ((SELECT id FROM users WHERE username = ? AND password = ?), uuid_generate_v4(), now() + interval '1 day') RETURNING token;");
                stmt.setString(1, register.username);
                stmt.setBytes(2, salt(register.password));

                RegisterResponse res = new RegisterResponse();

                try {
                    ResultSet resultSet = stmt.executeQuery();
                    resultSet.next();
                    res.successful = true;
                    res.token = UUID.fromString(resultSet.getString(1));
                } catch (SQLException ex) {
                    res.successful = false;
                    res.token = null;
                }

                response = res;
            } catch (SQLException e) {
                e.printStackTrace();
            }


        }
        if (segment.message instanceof LoginMessage) {
            LoginMessage login = (LoginMessage) segment.message;

            if (login.password.length != 32) {
                System.err.println("Incorrect password length");
                return;
            }

            try {
                PreparedStatement stmt = server.dbConn.prepareStatement("INSERT INTO tokens (user_id, token, expires) VALUES ((SELECT id FROM users WHERE username = ? AND password = ?), uuid_generate_v4(), now() + interval '1 day') RETURNING token;");
                stmt.setString(1, login.username);
                stmt.setBytes(2, salt(login.password));

                LoginResponse res = new LoginResponse();

                try {
                    ResultSet resultSet = stmt.executeQuery();
                    resultSet.next();
                    res.successful = true;
                    res.token = UUID.fromString(resultSet.getString(1));
                } catch (SQLException ex) {
                    res.successful = false;
                    res.token = null;
                }

                response = res;
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
