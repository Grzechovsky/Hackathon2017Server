package zse.hackathon2017;

import java.io.Serializable;

/**
 * The main file of 'shit protocol' (SP) (the protocol to transfer shit).
 * Created by Grzechu on 25.03.2017.
 */
public class Segment implements Serializable {
    public UserInfo sender;
    public Message message;

    @Override
    public String toString() {
        return "Segment{sender=" + sender + ", message=" + message +    '}';
    }
}
