package zse.hackathon2017.responses;

import zse.hackathon2017.Response;

import java.util.UUID;

/**
 * Created by Grzechu on 25.03.2017.
 */
public class LoginResponse implements Response {
    public boolean successful;
    public UUID token;
}
