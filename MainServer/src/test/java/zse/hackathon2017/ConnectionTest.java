package zse.hackathon2017;

import org.junit.Test;
import zse.hackathon2017.messages.LoginMessage;
import zse.hackathon2017.messages.RegisterMessage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * Created by Grzechu on 25.03.2017.
 */
public class ConnectionTest {

    @Test
    public void udpTest() throws IOException, NoSuchAlgorithmException {
        DatagramSocket socket = new DatagramSocket();
        socket.connect(InetAddress.getLocalHost(), Main.MAIN_SERVER_PORT);

        byte[] digest = MessageDigest.getInstance("SHA-256").digest("LOLLOL".getBytes());

        Segment segment = new Segment();
        segment.token = UUID.randomUUID();

//        RegisterMessage registerMessage = new RegisterMessage();
//        registerMessage.username = "LOLOL";
//        registerMessage.password = digest;
//        segment.message = registerMessage;

        LoginMessage loginMessage = new LoginMessage();
        loginMessage.username = "LOLOL";
        loginMessage.password = digest;
        segment.message = loginMessage;

        ByteArrayOutputStream array = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(array);
        out.writeObject(segment);

        socket.send(new DatagramPacket(array.toByteArray(), array.size()));
    }

}
