import java.io.Serializable;

public class MailConstants implements Serializable{

    public boolean isEncrypted;
    public String mailTo;
    public String mailFrom;
    public String ccAddress;
    public String date;
    public String subject;
    public String message;

    public MailConstants(
            boolean _isEncrypted,
            String _mailTo,
            String _mailFrom,
            String _ccAddress,
            String _date,
            String _subject,
            String _message) {
        setEncrypted(_isEncrypted);
        setTo(_mailTo);
        setFrom(_mailFrom);
        setCC(_ccAddress);
        setDate(_date);
        setSubject(_subject);
        setMessage(_message);
    }

    public boolean getEncrypted(){
        return isEncrypted;
    }

    public String getTo(){
        return mailTo;
    }

    public String getFrom(){
        return mailFrom;
    }

    public String getCC(){
        return ccAddress;
    }

    public String getDate(){
        return date;
    }

    public String getSubject(){
        return subject;
    }

    public String getMessage(){
        return message;
    }


    public void setEncrypted(boolean setEncr){
        isEncrypted = setEncr;
    }

    public void setTo(String receiver){
        mailTo = receiver;
    }

    public void setFrom(String sender){
        mailFrom = sender;
    }

    public void setCC(String cc){
        ccAddress = cc;
    }

    public void setDate(String sendDate){
        date = sendDate;
    }

    public void setSubject(String sendSub){
        subject = sendSub;
    }

    public void setMessage(String sentMessage){
        message = sentMessage;
    }

    @Override
    public String toString() {
        String output = "Message: \nTo: " + mailTo + "\nFrom: " + mailFrom + "\n Subject: " + subject + "\n Message Content: " + message;
        return output;
    }
}
