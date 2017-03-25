package zse.hackathon2017.messages;

import zse.hackathon2017.Message;

/**
 * Created by Grzechu on 25.03.2017.
 */
public class RegisterMessage implements Message {
    public String username;
    public byte[] password;
}
