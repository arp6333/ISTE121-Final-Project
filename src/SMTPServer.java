import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.net.URL;

/**
 * Class SMTPServer drives storage and routing of messages both from client connections and through to other servers.
 */

public class SMTPServer extends JFrame implements SMTPConstants, ActionListener{
    private Vector<User> userList;
    private ServerThread serverThread = null;
    private SendWorker sendWorker;

    // GUI
    private JTextArea jtaLog = new JTextArea();
    private JLabel jlPort = new JLabel("Server Port:                        ");
    private JLabel jlMsgCount = new JLabel("Message(s) recieved:      ");

    private JTextField jtfPort = new JTextField(10);
    private JTextField jtfMsgCount = new JTextField(10);

    // Buttons
    private JButton jbStart = new JButton("Start");
    //private JButton jbClear = new JButton("Clear");

    public SMTPServer(){
        this.setTitle("SMTP Server");
        this.setSize(600,600);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        sendWorker = new SendWorker();
        sendWorker.start();
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                try {
                    FileOutputStream fos = new FileOutputStream("userList.obj");
                    ObjectOutputStream objOut = new ObjectOutputStream(fos);
                    objOut.writeObject(userList);
                    objOut.close();
                } catch (Exception exc){
                    exc.printStackTrace();
                }
            }
        });

        // JPanel
        JPanel jpNorth = new JPanel(new GridLayout(0,1));
        JPanel jpRow1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        jpRow1.add(jlPort);
        jpRow1.add(jtfPort);
        jpRow1.add(jbStart);
        jpNorth.add(jpRow1);
        JPanel jpRow2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        jpRow2.add(jlMsgCount);
        //jpRow2.add(jtfMsgCount);
        jpNorth.add(jpRow2);

        JPanel jpSouth = new JPanel(new FlowLayout(FlowLayout.CENTER));
        //jpSouth.add(jbClear);

        // ScrollPane
        JScrollPane jspCenter = new JScrollPane(jtaLog);
        this.add(jspCenter, BorderLayout.CENTER);

        //Font
        jtaLog.setFont(new Font("MONOSPACED", Font.PLAIN, 12));
        jtaLog.setLineWrap(true);

        jbStart.addActionListener(this);

        this.add(jpNorth, BorderLayout.NORTH);
        this.add(jpSouth, BorderLayout.SOUTH);

        this.setVisible(true);
    }

    public void doStart(){
        File userListLoc = new File("./userList.obj");
        if(userListLoc.exists()){
            try{
                FileInputStream fis = new FileInputStream(userListLoc);
                ObjectInputStream objIn = new ObjectInputStream(fis);
                Object holder = objIn.readObject();
                if(holder instanceof Vector){
                    userList = (Vector<User>)holder;
                    System.out.println("User list found: " + userList);
                } else {
                    userList = new Vector<>();
                    System.out.println("No file found.");
                }
                objIn.close();
            } catch (Exception e){
                e.printStackTrace();
            }
        } else {
            userList = new Vector<>();
        // Users for debugging, remove in production
        //User test1 = new User("test1");
        //userList.add(test1);
        //test1.userInbox.add(new MailConstants(false, "test1", "test2", "","12-12-12", "Test message", "Its a message alright."));
        //test1.userInbox.add(new MailConstants(false, "test1", "test2", "","12-12-12", "Test message", "Its a message alright."));
        //userList.add(new User("test2"));

        }
        //System.out.println(userList);
        serverThread = new ServerThread();
        serverThread.start();
        jtfPort.setText(Integer.toString(CONN_PORT));
        jbStart.setText("Stop");
        jtaLog.append("Starting server on port " + CONN_PORT + "\n");
    }

    public void doStop(){
        try {
            serverThread.running = false;
            serverThread.serverSocket.close();
            jbStart.setText("Start");
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void doClear(){
        jtaLog.setText("");
    }

    ConcurrentLinkedQueue<MailConstants> messageQueue = new ConcurrentLinkedQueue<>();

    public void enqueueMessage (MailConstants message) {
        messageQueue.add(message);
    }

    public String addUser(User user){
        String username = user.getUsername();
        //System.out.println("New user: " + username);
        if(verifyUser(username) == -1) {
            userList.add(user);
            return OK;
        } else {
            return EXISTS_ERROR;
        }
    }

    public int verifyUser(String candidateName){
        int userIndex = -1;
        for(int i = 0; i<userList.size(); i++){
            User candidate = userList.get(i);
            if(candidate.getUsername().equals(candidateName)){
                userIndex = i;
                return userIndex;
            }
        }
        return userIndex;
    }

    /**
     * Method caesarEncrypt accepts a string param in, whose characters will be shifted and returned as a new string.
     * @param in
     * @return outString (encrypted version of in String)
     */
    String caesarEncrypt(String in){
        String outString = "";
        for(int i = 0; i < in.length(); i++){
            char c = in.charAt(i);
            if(Character.toString(c).matches("\\S")) {
                int ascii = (int) c;
                ascii += 13;
                outString += (char) ascii;
            } else {
                outString += c;
            }
        }
        return outString;
    }

    /**
     * Method caesarDecrypt accepts a string param in, whose characters will be shifted back and returned as a new string.
     * @param in
     * @return outString, an unencrypted string based on the shift value.
     */
    String caesarDecrypt(String in){
        String outString = "";
        for(int i = 0; i<in.length(); i++){
            char c = in.charAt(i);
            if(Character.toString(c).matches("\\S")) {
                int ascii = (int) c;
                ascii -= 13;
                outString += (char) ascii;
            } else {
                outString += c;
            }
        }
        return outString;
    }

    class ServerThread extends Thread {
        ServerSocket serverSocket;
        Socket cSocket;
        Boolean running = false;
        public ServerThread(){
            // Just for instantiation
        }

        public void run(){
            try {
                serverSocket = new ServerSocket(CONN_PORT);
                sendWorker = new SendWorker();
                while(true) {
                    try {
                        cSocket = serverSocket.accept();
                        //System.out.println("Creating new client");
                        ClientThread client = new ClientThread(cSocket);
                        client.start();
                    } catch (Exception e) {
                        e.printStackTrace();
                        break;
                    }
                }
            } catch (Exception e){
                e.printStackTrace();
            }

        }
    }

    class ClientThread extends Thread {
        Socket clientSocket;
        Scanner clientReader;
        PrintWriter clientWriter;
        User connectedUser;
        public ClientThread(Socket cSocket){
            this.clientSocket = cSocket;
            //System.out.println("New Client created. " + clientSocket);
            jtaLog.append("New Client connected: " + clientSocket + "\n");
        }

        public void run(){
            //System.out.println("In client run method.");
            //TODO: Add interface inbox checking and message composition.
            try {
                clientReader = new Scanner(new InputStreamReader(clientSocket.getInputStream()));
                clientWriter = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
                //String command = clientReader.nextLine();
                //System.out.println("Command received: " + command);
                while (clientReader.hasNextLine()) {
                    jtaLog.append("Has next line.\n");
                    String command = clientReader.nextLine();
                    jtaLog.append("Command received: " + command + "\n");
                    int length = 4;
                    if(command.length() < 4){
                        length = command.length();
                    }
                    jtaLog.append(command.substring(0,length));
                    switch (command.substring(0,length)) {
                        default:
                            //System.out.println(command.substring(0,length));
                            //System.out.println("Logging in...");
                            jtaLog.append("Logging in...\n");
                            // This is the login case, as all other commands will be SMTP standard commands
                            String candidate = command;
                            clientReader.nextLine();
                            //System.out.println("Candidate name: " + candidate);
                            int userIndex = verifyUser(candidate);
                            if (userIndex == -1) {
                                clientWriter.println("DECLINED");
                                clientWriter.flush();
                                System.out.println("Declined");
                            } else {
                                clientWriter.println("ACCEPTED");
                                clientWriter.flush();
                                clientWriter.println("220");
                                clientWriter.flush();
                                }
                                connectedUser = userList.get(userIndex);
                                //System.out.println(connectedUser);
                                //System.out.println("Accepted");
                            break;
                        case REGISTER:
                            clientWriter.println("250");
                            clientWriter.flush();
                            String newUsername = clientReader.nextLine();
                            addUser(new User(newUsername));
                            break;
                        case "MLBX":
                            try {
                                ObjectOutputStream objOut = new ObjectOutputStream(clientSocket.getOutputStream());
                                objOut.writeObject(connectedUser.getUserInbox());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            break;
                        case "HELO":
                            boolean loop = true;
                            clientWriter.println("250 HELO");
                            clientWriter.flush();
                            String smtpCommand = "";
                            while (!smtpCommand.equals("QUIT")) {
                                smtpCommand = clientReader.nextLine();
                                System.out.println(smtpCommand);
                                Vector<String> recipients = new Vector<>();
                                String sender = null;
                                jtaLog.append("SMTP Command: " + smtpCommand + "\n");
                                if (smtpCommand.contains("MAIL FROM")) {
                                    System.out.println("In mail from");
                                    //jtaLog.append("here in my garage\n");
                                    sender = smtpCommand.substring(10);
                                    jtaLog.append("New sender: " + sender + "\n");
                                    clientWriter.println("250 OK");
                                    clientWriter.flush();
                                    while (true) {
                                        smtpCommand = clientReader.nextLine();
                                        jtaLog.append("SMTP Command (Lower): " + smtpCommand + "\n");
                                        if (smtpCommand.contains("RCPT TO")) {
                                            String recipient = smtpCommand.substring(8);
                                            recipients.add(recipient);
                                            jtaLog.append("Recipient: " + recipient + "\n");
                                            clientWriter.println("250 OK");
                                            clientWriter.flush();
                                        } else if (smtpCommand.contains("DATA")) {
                                            boolean encrypted = false;
                                            jtaLog.append("In contains DATA.\n");
                                            clientWriter.println("354");
                                            //End data with <CRLF>.<CRLF>
                                            clientWriter.flush();
                                            jtaLog.append("Received SMTP Command DATA\n");
                                            String contents = "";
                                            smtpCommand = clientReader.nextLine();
                                            if(smtpCommand.contains("_ENCRYPTED_")){
                                                encrypted = true;
                                            }
                                            while (!smtpCommand.equals(".")) {
                                                jtaLog.append("In the loop\n");
                                                contents += smtpCommand + "\n";
                                                if(encrypted){
                                                    smtpCommand = caesarDecrypt(clientReader.nextLine());
                                                } else {
                                                    smtpCommand = clientReader.nextLine();
                                                }
                                            }
                                            jtaLog.append("==================================\n");
                                            jtaLog.append("Message:\n" + contents + "\n");
                                            clientWriter.println("250 OK");
                                            clientWriter.flush();
                                            for(String recipient : recipients){
                                                enqueueMessage(parseMessage(contents, recipient, sender));
                                            }
                                            if(clientReader.nextLine().contains("QUIT")){
                                                clientWriter.println("221");
                                                clientWriter.flush();
                                            }
                                            break;
                                        }

                                    }
                                } else {
                                    clientWriter.println("554 Transaction Failed");
                                }
                                break;
                            }
                            if(smtpCommand.equals("QUIT")){
                                clientWriter.println("221");
                                clientWriter.flush();
                                return;
                            }
                            break;
                        case "QUIT":
                            return;
                        }
                    }
                } catch(Exception e){
                    e.printStackTrace();
                }
            }

            /*
                message string format:
                    From: test2
                    To: test1
                    Date: 30-04-2018
                    Subject: test
                    Test
             */

            public MailConstants parseMessage(String message, String recipient, String sender){

                MailConstants outMessage = null;
                String date = message.substring(message.indexOf("Date:") + 6, message.indexOf("Subject:"));
                //System.out.println(date);

                String subject = message.substring(message.indexOf("Subject:") + 9, message.indexOf("\n", message.indexOf("Subject:") + 9));
                //System.out.println("Subject: " + subject);

                String messageContent = message.substring(message.indexOf(subject) + subject.length());
                outMessage = new MailConstants(false, recipient, sender, null, date, subject, messageContent);

                return outMessage;
            }
        }

    class SendWorker extends Thread {
        Socket sendSocket;
        MailConstants msgToSend;
        PrintWriter pwt;
        Scanner serverReader;
        public SendWorker() {

        }

        private void doConnect(String senderName, String address, MailConstants currentMessage){
            try {
                sendSocket = new Socket();
                sendSocket.connect(new InetSocketAddress(address, CONN_PORT), 1000);
                serverReader = new Scanner(new InputStreamReader(sendSocket.getInputStream()));
                pwt = new PrintWriter(new OutputStreamWriter(sendSocket.getOutputStream()));

            } catch (Exception exc){
                exc.printStackTrace();
                return;
            }

            doSend(senderName, currentMessage);
        }

        private void doSend(String sender, MailConstants message){
            try{
                pwt.println("HELO");
                String msg = serverReader.nextLine();
                if(!msg.contains("HELO")){
                    JOptionPane.showMessageDialog(null, "Exception 450: Cannot send mail at this time", "Cannot Sent Mail", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                String from = sender;
                String to = message.getTo();
                String subject = message.getSubject();
                String content = message.getMessage();
                String date = message.getDate();

                try {
                    pwt.println("MAIL FROM:<" + from + ">");
                    pwt.flush();
                    msg = serverReader.nextLine();
                    if(msg.contains("250")){
                        pwt.println("RCPT TO:<" + to +">");
                        pwt.flush();
                        msg = serverReader.nextLine();
                        if(!msg.contains("250")){
                            JOptionPane.showMessageDialog(null, "Exception 447: Outgoing Message Timeout", "Cannot Send Message", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        pwt.println("DATA");
                        pwt.flush();
                        msg = serverReader.nextLine();
                        if(msg.contains("354")){
                            try {
                                // Username
                                pwt.println("server");
                                pwt.flush();
                                //Password
                                pwt.println("server");
                                pwt.flush();
                                // Print actual content with cipher if encrypted.
                                if(message.isEncrypted){
                                    pwt.println("_ENCRYPTED_");
                                    pwt.flush();
                                    pwt.println("From: " + caesarEncrypt(from));
                                    pwt.flush();
                                    pwt.println("To: " + caesarEncrypt(to));
                                    pwt.flush();
                                    pwt.println("Cc:" + caesarEncrypt(message.getCC()));
                                    pwt.flush();
                                    pwt.println("Date: " + caesarEncrypt(date));
                                    pwt.flush();
                                    pwt.println("Subject: " + caesarEncrypt(subject));
                                    pwt.flush();
                                    pwt.println(caesarEncrypt(content));
                                    pwt.flush();
                                    pwt.println(".");
                                    pwt.flush();
                                } else {
                                    // Otherwise just print normally.
                                    pwt.println("From: " + from);
                                    pwt.flush();
                                    pwt.println("To: " + to);
                                    pwt.flush();
                                    pwt.println("Cc:" + message.getCC());
                                    pwt.flush();
                                    pwt.println("Date: " + date);
                                    pwt.flush();
                                    pwt.println("Subject: " + subject);
                                    pwt.flush();
                                    pwt.println(content);
                                    pwt.flush();
                                    pwt.println(".");
                                    pwt.flush();
                                }
                            } catch(Exception e){
                                e.printStackTrace();
                                return;
                            }
                        } else {
                            JOptionPane.showMessageDialog(null, "Exception 450: Mailbox Does Not Exist", "Exception 450",JOptionPane.ERROR_MESSAGE);
                        }
                    }
                } catch (Exception e){
                    e.printStackTrace();
                    return;
                }
            } catch (Exception e){
                e.printStackTrace();
                return;
            }
        }

        public void run(){
            //System.out.println("IN SEND WORKER");
            try {
                while(true) {
                    MailConstants message = messageQueue.poll();
                    //System.out.println(message);
                    if(message != null) {
                        //System.out.println("Message in queue: " + message);
                        String recipient = message.getTo();
                        //System.out.println("To: " + recipient);
                        String serverAddress = recipient.substring(recipient.indexOf("@")+1, recipient.length()-1);
                        //System.out.println("Server Address: " + serverAddress);
                        if(serverAddress.equals("localhost") || serverAddress.equals("127.0.0.1") || serverAddress.equals(sendSocket.getInetAddress())){
                            String username = recipient.substring(1, recipient.indexOf("@"));
                            //System.out.println(username);
                            int userIndex = verifyUser(username);
                            //System.out.println(userIndex);
                            if(userIndex != -1){
                                userList.get(userIndex).userInbox.add(message);
                                //System.out.println("Recipient inbox: " + userList.get(userIndex).userInbox);
                            }
                        } else {
                            doConnect(message.getFrom(), serverAddress, message);
                            // This needs testing with other servers.
                        }
                    }
                }
            } catch (Exception exc){
                exc.printStackTrace();
            }
        }
    }

    /** Action Listener */
    public void actionPerformed(ActionEvent ae){
        switch(ae.getActionCommand()){
            case "Start":
                doStart();
                break;

            case "Stop":
                doStop();
                break;

            case "Clear":
                doClear();
                break;

        }
    }// End of actionlistener

    public static void main(String [] args){
        SMTPServer SMTP = new SMTPServer();
    }
}

