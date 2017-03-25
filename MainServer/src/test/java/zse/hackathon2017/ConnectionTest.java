package zse.hackathon2017;

import org.junit.Assert;
import org.junit.Test;
import zse.hackathon2017.messages.CreateGroupMessage;
import zse.hackathon2017.messages.LoginMessage;
import zse.hackathon2017.messages.RegisterMessage;
import zse.hackathon2017.responses.CreateGroupResponse;
import zse.hackathon2017.responses.LoginResponse;
import zse.hackathon2017.responses.RegisterResponse;

import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * Created by Grzechu on 25.03.2017.
 */
public class ConnectionTest {



    @Test
    public void udpTest() throws IOException, NoSuchAlgorithmException, ClassNotFoundException {
        DatagramSocket socket = new DatagramSocket();
        socket.connect(InetAddress.getByName("192.168.0.100"), Main.MAIN_SERVER_PORT);

        byte[] digest = MessageDigest.getInstance("SHA-256").digest("LOLLOL".getBytes());

        Segment segment = new Segment();

//        RegisterMessage registerMessage = new RegisterMessage();
//        registerMessage.username = "LOLOL";
//        registerMessage.password = digest;
//        segment.message = registerMessage;
//
//        send(socket, segment);
//
//        RegisterResponse registerResponse = (RegisterResponse) receive(socket);
//        segment.token = registerResponse.token;
//        Assert.assertTrue(registerResponse.successful);


        LoginMessage loginMessage = new LoginMessage();
        loginMessage.username = "LOLOL";
        loginMessage.password = digest;
        segment.message = loginMessage;

        send(socket, segment);

        LoginResponse loginResponse = (LoginResponse) receive(socket);
        segment.token = loginResponse.token;
        Assert.assertTrue(loginResponse.successful);

        CreateGroupMessage createGroup = new CreateGroupMessage();
        createGroup.name = "test_group2";
        segment.message = createGroup;

        send(socket, segment);

        CreateGroupResponse createGroupResponse = (CreateGroupResponse) receive(socket);
        assert createGroupResponse.success;


    }

    private Response receive(DatagramSocket socket) throws IOException, ClassNotFoundException {
        DatagramPacket packet = new DatagramPacket(new byte[4096], 4096);
        socket.receive(packet);

        ByteArrayInputStream input = new ByteArrayInputStream(packet.getData());
        ObjectInputStream in = new ObjectInputStream(input);

        return (Response) in.readObject();
    }

    private void send(DatagramSocket socket, Segment segment) throws IOException {
        ByteArrayOutputStream array = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(array);
        out.writeObject(segment);
        socket.send(new DatagramPacket(array.toByteArray(), array.size()));
    }

}
