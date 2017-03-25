package zse.hackathon2017.messages;

import zse.hackathon2017.Message;

/**
 * Created by Grzechu on 25.03.2017.
 */
public class AddUserToGroupMessage implements Message {
    public String username;
    public long groupId;
}
