package host.thanco.NeptuneBase;

public class Profile {
    private String userName;
    private String compressedImageBytes;
    private double color;

    Profile(String userName) {
        this.userName = userName;
    }
    Profile(String userName, double color) {
        this.userName = userName;
        this.color = color;
    }

    /**
     * @return the userName
     */
    public String getUserName() {
        return userName;
    }
    /**
     * @param userName the userName to set
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }
    /**
     * @return the color
     */
    public double getColor() {
        return color;
    }
    /**
     * @param color the color to set
     */
    public void setColor(int color) {
        this.color = color;
    }
    /**
     * @return the image
     */
    public String getCompressedImageBytes() {
        return compressedImageBytes;
    }
    /**
     * @param image the image to set
     */
    public void setImageBytes(String image) {
        this.compressedImageBytes = image;
    }

}
