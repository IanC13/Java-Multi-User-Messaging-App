package client;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.swing.text.*;

import common.ProtocolConstants;

public class ClientGUI {
    private JFrame frame;
    private JTextPane messageDisplayPane;
    private JTextArea inputArea;
    private JButton sendButton;
    private StyledDocument doc;
    private SimpleAttributeSet leftAttr, rightAttr, centerAttr;

    private PrintWriter out;
    private BufferedReader in;
    private String username;

    private DefaultListModel<String> connectedUserListModel;
    private JList<String> connectedUserList;

    public void start() throws IOException {
        // Connection
        Socket socket = new Socket("localhost", ProtocolConstants.PORT);

        in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        // Ask user for name
        String name = null;
        String response;

        // Repeatedly ask if name already in use
        do {
            // Read from server
            String prompt = in.readLine();
            if (prompt == null) {JOptionPane.showMessageDialog(null, "Server closed the connection.", "Error", JOptionPane.ERROR_MESSAGE);
                return; 
            }
      
            // Prompt user
            name = JOptionPane.showInputDialog(null, prompt, "Login", JOptionPane.QUESTION_MESSAGE);

            if (name == null) {
              socket.close();
              return;  
            }


            name = name.trim();
      
            // Send name to server for validation
            out.println(name);
      
            // Read server response
            response = in.readLine();
            if (response == null) {
                JOptionPane.showMessageDialog(null, "Server closed the connection.","Error",JOptionPane.ERROR_MESSAGE);
                return;
            }

            // If invalid name, promtp again
            if (response.startsWith("Invalid")) {
                JOptionPane.showMessageDialog(null, response, "Login Failed", JOptionPane.WARNING_MESSAGE);
            }
        } while (!response.startsWith("You have entered"));

        this.username = name;

        // Chat UI
        frame = new JFrame("Chat Client - " + name);

        // message display area
        messageDisplayPane = new JTextPane();
        messageDisplayPane.setEditable(false);
        
        doc = messageDisplayPane.getStyledDocument();
        
        // Left alignment for others' message
        leftAttr = new SimpleAttributeSet();
        StyleConstants.setAlignment(leftAttr, StyleConstants.ALIGN_LEFT);
        // indentations
        StyleConstants.setLeftIndent(leftAttr, 20);    // push in 20px from left
        StyleConstants.setSpaceAbove(leftAttr, 5);     // 5px gap above
        StyleConstants.setSpaceBelow(leftAttr, 5);     // 5px gap below
        StyleConstants.setRightIndent(leftAttr, 200);


        // right for own
        rightAttr = new SimpleAttributeSet();
        StyleConstants.setAlignment(rightAttr, StyleConstants.ALIGN_RIGHT);
        StyleConstants.setRightIndent(rightAttr, 20);
        StyleConstants.setSpaceAbove(rightAttr, 5);
        StyleConstants.setSpaceBelow(rightAttr, 5);
        StyleConstants.setLeftIndent(rightAttr, 200);

        // mid for system
        centerAttr = new SimpleAttributeSet();
        StyleConstants.setAlignment(centerAttr, StyleConstants.ALIGN_CENTER);
        StyleConstants.setSpaceAbove(centerAttr, 10);
        StyleConstants.setSpaceBelow(centerAttr, 10);

        JScrollPane scroll = new JScrollPane(messageDisplayPane);


        // Bottom of window - Input area
        inputArea = new JTextArea(3, 30);
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);

        // Make enter key send message
        inputArea.addKeyListener(new KeyAdapter(){

            public void keyPressed(KeyEvent e){

                if(e.getKeyCode()==KeyEvent.VK_ENTER){

                    String msg = inputArea.getText().trim();
                        if(!msg.isEmpty()){
                            out.println(msg);
                            inputArea.setText("");
                        }
                    e.consume(); // don’t insert newline
                }
            }
        });


        JScrollPane inputScroll = new JScrollPane(inputArea);
        
        
        sendButton = new JButton("Send");
        
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(sendButton, BorderLayout.EAST);
        bottom.add(inputScroll, BorderLayout.CENTER);

        // Send message
        ActionListener sendAction = e -> {
            String msg = inputArea.getText().trim();

            if (!msg.isEmpty()) {
                out.println(msg);
                inputArea.setText("");                
            }
        };

        sendButton.addActionListener(sendAction);

        // Add to farme
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(scroll, BorderLayout.CENTER);
        frame.getContentPane().add(bottom, BorderLayout.SOUTH);
        frame.setSize(800, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);


        // Display connected users on a right list
        connectedUserListModel = new DefaultListModel<>();
        connectedUserList = new JList<>(connectedUserListModel);

        connectedUserList.setBorder(BorderFactory.createTitledBorder("Connected Users"));
        connectedUserList.setPreferredSize(new Dimension(200, 0));

        // Add it as a sidebar:
        frame.add(new JScrollPane(connectedUserList), BorderLayout.EAST);
        frame.validate();
        frame.repaint();

        frame.setVisible(true);

        // Thread for reading messages
        Thread reader = new Thread(() -> {
            try {
                String message;

                while ((message = in.readLine()) != null) {
                    // User join/leave
                    if (message.startsWith("USERLIST")) {
                        String[] names = message.substring(9).split(",");

                        SwingUtilities.invokeLater(() -> {
                            connectedUserListModel.clear();
                            for (String n: names) {
                                if (!n.isBlank()) connectedUserListModel.addElement(n);
                            }
                        });
                    }
                    // Display in middle for system message
                    else if (message.endsWith("has joined the chat.") || message.endsWith("has left the chat")) {
                        appendMessage(message, centerAttr);
                    } 
                    // History message
                    else if (message.startsWith("HISTORY ")) {
                        // HISTORY ts|sender|body
                        String[] components = message.substring(8).split("\\|", 3);

                        String ts = components[0], sender = components[1], content = components[2];
                        
                        // Determine display position, left for others or right for own message
                        AttributeSet attr = sender.equals(username) ? rightAttr : leftAttr;

                        appendMessage("[" + ts + "] " + sender + ": " + content, attr);
                    }
                    // Current session messages
                    else if (message.startsWith("MSG ")) {

                        // MSG sender|body
                        String[] components = message.substring(4).split("\\|", 2);

                        String sender = components[0], body = components[1];

                        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                        // Determine display position, left for others or right for own message
                        AttributeSet attr = sender.equals(username) ? rightAttr : leftAttr;

                        appendMessage("[" + ts + "] " + sender + ": " + body, attr);
                    }

                }
            } catch (IOException ex) {
                appendMessage("Connection lost.", centerAttr);
            }
        });

        reader.setDaemon(true);
        reader.start();
    }

    // function to append message to left right or center
    private void appendMessage(String msg, AttributeSet posAttr) {
        try {
            int start = doc.getLength();

            doc.insertString(start, msg + "\n", null);

            // apply paragraph style to that line
            doc.setParagraphAttributes(start, msg.length(), posAttr, false);

            messageDisplayPane.setCaretPosition(doc.getLength());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new ClientGUI().start();

            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null, "Unable to connect: " + ex.getMessage());
            }
        });
    }
}