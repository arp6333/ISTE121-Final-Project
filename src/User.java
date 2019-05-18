import java.io.Serializable;
import java.util.Vector;

public class User implements Serializable{

    private String username = null;
    private String hostAddress = null;
    public Vector<MailConstants> userInbox = null;

    public User(String username, String hostIP){
        this.username = username;
        this.hostAddress = hostIP;
        this.userInbox = new Vector<MailConstants>();
    }

    public User(String username){
        this.username = username;
        this.hostAddress = "127.0.0.1";
        this.userInbox = new Vector<MailConstants>();
    }

    public void addToInbox (MailConstants msg){
        userInbox.add(msg);
    }

    public int getInboxQuantity(){
        return userInbox.size();
    }

    public Vector<MailConstants> getUserInbox() {
        return userInbox;
    }

    public String getUsername(){
        return this.username;
    }


}
