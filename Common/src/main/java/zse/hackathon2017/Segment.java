package zse.hackathon2017;

import java.io.Serializable;
import java.util.UUID;

/**
 * The main file of 'shit protocol' (SP) (the protocol to transfer shit).
 * Created by Grzechu on 25.03.2017.
 */
public class Segment implements Serializable {
    public Message message;
    public UUID token;

    @Override
    public String toString() {
        return String.format("Segment{message=%s, token=%s}", message, token);
    }
}
