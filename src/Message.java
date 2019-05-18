public class Message {

    private boolean encrypted = false;
    private String sender = null;
    private String destUser = null;
    private String destServer = null;
    private String subject = null;
    private String contents = null;

    public Message(boolean isEnc, String sender, String destUser, String subject, String contents, String destIP){
        this.encrypted = isEnc;
        this.sender = sender;
        this.destUser = destUser;
        this.subject = subject;
        this.contents = contents;
        this.destServer = destIP;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public String getSender() {
        return sender;
    }

    public String getSubject() {
        return subject;
    }

    public String getContents() {
        return contents;
    }

    public String getDestUser(){
        return destUser;
    }

    public String getDestServer(){
        return destServer;
    }

}
