package zse.hackathon2017;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

/**
 * Created by Grzechu on 25.03.2017.
 */
public class ReceiveWorker implements Runnable {

    private SelectionKey key;

    public ReceiveWorker(SelectionKey key) {
        this.key = key;
    }

    @Override
    public void run() {
        try {
            Server server = (Server) key.attachment();
            ByteBuffer buffer = ByteBuffer.allocate(4096);


            SocketAddress addr;
            try {
                addr = server.main.receive(buffer);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            if (addr == null) {
                return;
            }

            ByteArrayInputStream input = new ByteArrayInputStream(buffer.array(), 0, buffer.position() + 1);
            ObjectInputStream objectInput = new ObjectInputStream(input);
            Object o = objectInput.readObject();

            if (!(o instanceof Segment)) {
                System.err.println("Unknown object received");
                return;
            }

            Segment seg = (Segment) o;
            server.processThreads.submit(new ProcessWorker(server, seg));


        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
