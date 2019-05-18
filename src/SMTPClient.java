import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import javax.swing.text.DefaultCaret;
import java.text.SimpleDateFormat;
import java.net.URL;

/**
* SMTPClient is the client side to SMTPServer to send and receieve emails with the server.
*/
public class SMTPClient extends JFrame implements SMTPConstants{
   // Login GUI elements
   private JButton jbNew = new JButton("New User");
   private JLabel jlUN = new JLabel("Username@IPAddress:");
   private JTextField jtfUN = new JTextField(15);
   private JLabel jlPW = new JLabel("Password:");
   private JTextField jtfPW = new JTextField(15);
   private JButton jbLogin = new JButton("Login");
   // Sending mail GUI
   private JButton jbSend = new JButton("Send");
   private JButton jbReceive = new JButton("Check Inbox");
   private JButton jbClear = new JButton("Clear fields");
   private JButton jbLogout = new JButton("Logout");
   private JCheckBox check = new JCheckBox("Encrypt Message");
   private JLabel jlTo = new JLabel("To (separate by commas, name@IPAddress):");
   private JTextField jtfTo = new JTextField(15);
   private JLabel jlFrom = new JLabel("From:");
   private JTextField jtfFrom = new JTextField(15);
   private JLabel jlSubject = new JLabel("Subject:");
   private JTextField jtfSubject = new JTextField(15);
   private boolean encrypt = false;
   private JTextArea jtaLog = new JTextArea(10, 80);
   // Streams
   private PrintWriter pwt = null;
   private Scanner scn = null;
   private ObjectOutputStream out = null;
   private ObjectInputStream in = null;
   // Connection
   public static final int SERVER_PORT = 42069;
   private Socket socket = null;

   /**
   * Main method to run GUI constructor.
   */
   public static void main(String[] args){
      new SMTPClient();
   }

   /**
   * Constructor to create GUI.
   */
   public SMTPClient(){
      this.setTitle("SMTPClient");
      this.setSize(800, 150);
      this.setLocation(100, 100);
      this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

      JPanel jpNorth = new JPanel();
      jpNorth.setLayout(new GridLayout(0,1));

      JPanel jpRow1 = new JPanel();
      jpRow1.add(jbNew);
      jpNorth.add(jpRow1);

      JPanel jpRow2 = new JPanel();
      jpRow2.add(jlUN);
      jpRow2.add(jtfUN);
      jpRow2.add(jlPW);
      jpRow2.add(jtfPW);
      jpRow2.add(jbLogin);
      jpNorth.add(jpRow2);
      this.add(jpNorth, BorderLayout.NORTH);

      jbLogin.addActionListener(e -> {
         String[] parts;
         try{
            parts = jtfUN.getText().split("@");
         }
         catch(Exception e2){
            JOptionPane.showMessageDialog(this, "Please use 'username@IPAddress' for login", "Cannot Login", JOptionPane.WARNING_MESSAGE);
            return;
         }
         doConnect(parts[0], parts[1], jtfPW.getText(), "no");
      });
      jbNew.addActionListener(e -> {
         doNew();
      });

      this.setVisible(true);
   }

   /**
   * Connects the client with the server using sockets.
   * @param- String username: the entered username, String ipAddress: the entered IPAddress
   */
   private void doConnect(String username, String address, String pass, String register){
      System.out.println("doConnect");
      try{
         socket = new Socket();
         socket.connect(new InetSocketAddress(address, SERVER_PORT), 1000);
         socket.setSoTimeout(1000);
         scn = new Scanner(new InputStreamReader(socket.getInputStream()));
         pwt = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
      }
      catch(Exception e){
         JOptionPane.showMessageDialog(this, "Exception 421: Service Not Available", "Cannot Connect to Server", JOptionPane.ERROR_MESSAGE);
         doDisconnect();
         return;
      }
      if(register.equals("no")){
         pwt.println(username);
         pwt.flush();
         pwt.println(pass);
         pwt.flush();
         String msg = scn.nextLine();
         System.out.println("Recieved: " + msg);
         if(msg.contains("ACCEPTED")){
            msg = scn.nextLine();
            System.out.println("Recieved: " + msg);
            if(msg.contains("220")){
               email();
            }
            else{
               JOptionPane.showMessageDialog(this, "Exception 421: Service Not Available", "Cannot Connect to Server", JOptionPane.ERROR_MESSAGE);
               doDisconnect();
               return;
            }
         }
         else{
            JOptionPane.showMessageDialog(this, "Exception 421: Service Not Available", "Cannot Connect to Server", JOptionPane.ERROR_MESSAGE);
            doDisconnect();
            return;
         }
      }
   }


   /**
   * Disconnects the client from the server and goes back to login.
   */
   private void doDisconnect(){
      System.out.println("doDisconnect");
      pwt.println("QUIT");
      pwt.flush();
      try{
         socket.close();
         scn.close();
         pwt.close();
      }
      catch(Exception e){

      }
      this.dispose();
      new SMTPClient();
   }

   /**
   * Initializes the email GUI when a user logs in.
   */
   public void email(){
      System.out.println("doEmail");
      getContentPane().removeAll();
      JPanel jpNorth = new JPanel();
      jpNorth.add(jbLogout);
      jbLogout.addActionListener(e -> {
          doDisconnect();
      });
      this.add(jpNorth, BorderLayout.NORTH);
      // Check box for encrypted message
      check.addActionListener(e -> {
         JCheckBox cbLog = (JCheckBox)e.getSource();
         if(cbLog.isSelected()){
            encrypt = true;
         }
         else{
            encrypt = false;
         }
      });

      this.setSize(1200, 350);

      DefaultCaret caret = (DefaultCaret)jtaLog.getCaret();
      caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
      jtaLog.setLineWrap(true);
      jtaLog.setWrapStyleWord(true);

      JPanel jpCenter = new JPanel();
      JPanel jpRow = new JPanel();
      jpRow.add(jlTo);
      jpRow.add(jtfTo);
      jpRow.add(jlFrom);
      jpRow.add(jtfFrom);
      jpRow.add(check);
      jpCenter.add(jpRow);
      jpCenter.add(jlSubject);
      jpCenter.add(jtfSubject);
      jpCenter.add(new JScrollPane(jtaLog));
      this.add(jpCenter, BorderLayout.CENTER);

      JPanel jpSouth = new JPanel();
      JPanel jpRow2 = new JPanel();
      jpRow2.add(jbSend);
      jbSend.addActionListener(e -> {
         doSend();
      });
      jpRow2.add(jbReceive);
      jbReceive.addActionListener(e -> {
         doInbox();
      });
      jpRow2.add(jbClear);
      jbClear.addActionListener(e -> {
         doClear();
      });
      jpSouth.add(jpRow2);
      this.add(jpSouth, BorderLayout.SOUTH);
      // Repaint the GUI
      validate();
      repaint();
   }

   /**
   * Get the ip address of the current machine.
   * @return - String of the local ipAddress
   */
   public String getIP() throws Exception{
      System.out.println("getIP");
      URL whatismyip = new URL("http://checkip.amazonaws.com/%22");
      BufferedReader in = null;
      try{
          in = new BufferedReader(new InputStreamReader(whatismyip.openStream()));
          String ip = in.readLine();
          return ip;
      }
      finally{
         if(in != null){
            try{
               in.close();
            }
            catch(Exception e){
               JOptionPane.showMessageDialog(this, "Error: Cannot retrieve IP", "Cannot Retrieve IP", JOptionPane.ERROR_MESSAGE);
               doDisconnect();
            }
         }
      }
   }

   /**
   * Send the entered message to the server.
   */
   public void doSend(){
      System.out.println("doSend");
      try{
         pwt.println("HELO " + getIP());
      }
      catch(Exception e){
         JOptionPane.showMessageDialog(this, "Error: Cannot retrieve IP", "Cannot Retrieve IP", JOptionPane.ERROR_MESSAGE);
         doDisconnect();
      }
      pwt.flush();
      String msg = scn.nextLine();
      System.out.println("Recieved: " + msg);
      if(!msg.contains("250")){
         JOptionPane.showMessageDialog(this, "Exception 450: Cannot send mail at this time", "Cannot Sent Mail", JOptionPane.ERROR_MESSAGE);
         doDisconnect();
         return;
      }
      String from = jtfFrom.getText();
      String[] to = jtfTo.getText().split(",");
      String subject = jtfSubject.getText();
      String message = jtaLog.getText();
      String date = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
      // Check for text fields
      if(to.equals("") || from.equals("") || message.equals("") || subject.equals("")){
         JOptionPane.showMessageDialog(this, "Please fill out all the forms!", "Cannot Send Message", JOptionPane.WARNING_MESSAGE);
         return;
      }
      // Send message
      msg = "";
      try{
         pwt.println("MAIL FROM:<" + from + ">");
         pwt.flush();
         msg = scn.nextLine();
         System.out.println("1- " + msg);
         if(msg.contains("250")){
            for(int i = 0; i < to.length; i++){
               pwt.println("RCPT TO:<" + to[i] +">");
               pwt.flush();
               msg = scn.nextLine();
               System.out.println("2- " + msg);
               if(!msg.contains("250")){
                  JOptionPane.showMessageDialog(this, "Exception 447: Outgoing Message Timeout", "Cannot Send Message", JOptionPane.ERROR_MESSAGE);
                  return;
               }
            }
            pwt.println("DATA");
            pwt.flush();
            msg = scn.nextLine();
            System.out.println("3- " + msg);
            if(msg.contains("354")){
               try{
                  pwt.println("Encrypted: " + encrypt);
                  pwt.flush();
                  pwt.println("From: " + from);
                  pwt.flush();
                  pwt.println("To: " + to[0]);
                  pwt.flush();
                  for(int i = 1; i < to.length; i++){
                     pwt.println("Cc: " + to[i]);
                     pwt.flush();
                  }
                  pwt.println("Date: " + date);
                  pwt.flush();
                  pwt.println("Subject: " + subject);
                  pwt.flush();
                  pwt.println(message);
                  pwt.flush();
                  pwt.println(".");
                  pwt.flush();
                  pwt.println("QUIT");
                  pwt.flush();
               }
               catch(Exception e){
                  JOptionPane.showMessageDialog(this, "Exception 447: Outgoing Message Timeout", "Cannot Send Message", JOptionPane.ERROR_MESSAGE);
                  return;
               }
            }
            else{
               JOptionPane.showMessageDialog(this, "Exception 450: Mailbox Does Not Exist", "Cannot Send Message", JOptionPane.ERROR_MESSAGE);
               return;
            }
         }
         else{
            JOptionPane.showMessageDialog(this, "Exception 447: Outgoing Message Timeout", "Cannot Send Message", JOptionPane.ERROR_MESSAGE);
            return;
         }
      }
      catch(Exception e){
         JOptionPane.showMessageDialog(this, "Exception 447: Outgoing Message Timeout", "Cannot Send Message", JOptionPane.ERROR_MESSAGE);
         return;
      }
      // Check if email successfully sent
      if(scn.nextLine().contains("250")){
         JOptionPane.showMessageDialog(this, "Email successfully sent!", "Email Sent", JOptionPane.INFORMATION_MESSAGE);
         return;
      }
      else{
         JOptionPane.showMessageDialog(this, "Exception 447: Outgoing Message Timeout", "Cannot Send Message", JOptionPane.ERROR_MESSAGE);
         return;
      }
   }

   /**
   * Initialize the inbox for viewing received mail.
   */
   public void doInbox(){
      System.out.println("doInbox");
      JFrame frame2 = new JFrame();
      frame2.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
      frame2.addWindowListener(new WindowAdapter(){
         public void windowClosing(WindowEvent windowEvent){
            frame2.dispose();
         }
      });
      frame2.setLayout(new GridLayout());
      frame2.setTitle("Inbox");
      pwt.println("MLBX");
      pwt.flush();
      Vector<MailConstants> emails = new Vector<>();
      try{
         in = new ObjectInputStream(socket.getInputStream());
         emails = (Vector)in.readObject();
      }
      catch(Exception e){
         JOptionPane.showMessageDialog(this, "Exception: " + e, "Cannot Retrieve Messages", JOptionPane.ERROR_MESSAGE);
         return;
      }

      JPanel jpCenter2 = new JPanel();
      jpCenter2.setLayout(new GridLayout(0, 5));
      // Display each email in inbox
      try{
         for(int i = 0; i < emails.size(); i++){
            JLabel jlFrom2 = new JLabel("From:");
            JTextField jtfFrom2 = new JTextField(15);
            jtfFrom2.setText(emails.get(i).getFrom());
            JLabel jlSubject2 = new JLabel("Subject:");
            JTextField jtfSubject2 = new JTextField(15);
            jtfSubject2.setText(emails.get(i).getSubject());
            JTextArea jtaEmail2 = new JTextArea();
            jtaEmail2.setText(emails.get(i).getMessage());
            // Do not allow the fields to be edited
            jpCenter2.add(jlFrom2);
            jpCenter2.add(jtfFrom2);
            jtfFrom2.setEditable(false);
            jpCenter2.add(jlSubject2);
            jpCenter2.add(jtfSubject2);
            jtfSubject2.setEditable(false);
            jpCenter2.add(jtaEmail2);
            jtaEmail2.setEditable(false);
         }
      }
      catch(Exception e){
         JOptionPane.showMessageDialog(this, "Mailbox Empty or Unavailable!", "Cannot Retrieve Messages", JOptionPane.ERROR_MESSAGE);
         return;
      }

      frame2.add(jpCenter2, BorderLayout.CENTER);
      frame2.pack();
      frame2.setVisible(true);
   }
   /**
   * Clear the text fields and message log.
   */
   public void doClear(){
      System.out.println("doClear");
      jtfTo.setText("");
      jtfFrom.setText("");
      jtfSubject.setText("");
      jtaLog.setText("");
      check.setSelected(false);
   }

   /**
   * Creates a new user account
   */
   public void doNew(){
      System.out.println("doRegister");
      String[] parts = null;
      String name = "";
      try{
         name = (String)JOptionPane.showInputDialog(this, "Enter new username@IPAddress to connect to:");
         if(name == null){
          return;
         }
         parts = name.split("@");

      }
      catch(Exception e){
         JOptionPane.showMessageDialog(this, "Exception: " + e, "Cannot Create New User", JOptionPane.ERROR_MESSAGE);
         return;
      }
      doConnect(parts[0], parts[1], jtfPW.getText(), "yes");
      pwt.println("REGISTER");
      pwt.flush();
      String reply = scn.nextLine();
      if(reply.contains("250")){
         pwt.println(parts[0]);
         pwt.flush();
         email();
         return;
      }
      else if(reply.equals(EXISTS_ERROR)){
         JOptionPane.showMessageDialog(this, "Error: Username already exists.", "User Already Exists", JOptionPane.ERROR_MESSAGE);
         doDisconnect();
         return;
      }
      else{
         JOptionPane.showMessageDialog(this, "Error: Cannot create new user.", "Cannot Create User", JOptionPane.ERROR_MESSAGE);
         doDisconnect();
         return;
      }
   }
}