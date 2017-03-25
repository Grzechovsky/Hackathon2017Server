package zse.hackathon2017;

import org.junit.Assert;
import org.junit.Test;
import zse.hackathon2017.messages.GetChannelImagesMessage;
import zse.hackathon2017.messages.GetUserChannelsMessage;
import zse.hackathon2017.messages.LoginMessage;
import zse.hackathon2017.responses.GetChannelImagesResponse;
import zse.hackathon2017.responses.GetUserChannelsResponse;
import zse.hackathon2017.responses.LoginResponse;

import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import static zse.hackathon2017.responses.GetChannelImagesResponse.*;

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

        GetUserChannelsMessage groups = new GetUserChannelsMessage();
        segment.message = groups;
        send(socket, segment);

        GetUserChannelsResponse getUserChannelsResponse = (GetUserChannelsResponse) receive(socket);
        System.out.println(Arrays.toString(getUserChannelsResponse.channelNames));

        for (String channelName : getUserChannelsResponse.channelNames) {
            GetChannelImagesMessage getChannelImagesMessage = new GetChannelImagesMessage();
            getChannelImagesMessage.channelName = channelName;

            segment.message = getChannelImagesMessage;
            send(socket, segment);

            GetChannelImagesResponse response = (GetChannelImagesResponse) receive(socket);
            for (ChannelImage image : response.images) {
                System.out.println(channelName + " - " + image.imageId + " - " + image.username);
            }
        }

//        CreateChannelMessage createGroup = new CreateChannelMessage();
//        createGroup.name = "test_group";
//        segment.message = createGroup;
//
//        send(socket, segment);
//
//        CreateChannelResponse createGroupResponse = (CreateChannelResponse) receive(socket);
//        assert createGroupResponse.success;


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
