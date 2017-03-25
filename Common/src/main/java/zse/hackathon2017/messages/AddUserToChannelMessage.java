package zse.hackathon2017.messages;

import zse.hackathon2017.Message;

/**
 * Created by Grzechu on 25.03.2017.
 */
public class AddUserToChannelMessage implements Message {
    public String username;
    public String channelName;
}
