package zse.hackathon2017.responses;

import zse.hackathon2017.Response;

import java.io.Serializable;

/**
 * Created by Grzechu on 25.03.2017.
 */
public class GetChannelImagesResponse implements Response {
    public ChannelImage[] images;


    public static class ChannelImage implements Serializable {
        public int imageId;
        public String username;
    }
}
