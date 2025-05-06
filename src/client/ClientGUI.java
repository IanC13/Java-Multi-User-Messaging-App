package client;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;

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


        // Add to farme
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(scroll, BorderLayout.CENTER);
        frame.getContentPane().add(bottom, BorderLayout.SOUTH);
        frame.setSize(800, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);



        // Send message
        ActionListener sendAction = e -> {
            String msg = inputArea.getText().trim();

            if (!msg.isEmpty()) {
                out.println(msg);
                inputArea.setText("");                
            }
        };

        sendButton.addActionListener(sendAction);

        frame.setVisible(true);


        // Thread for reading messages
        Thread reader = new Thread(() -> {
            try {
                String message;

                while ((message = in.readLine()) != null) {

                    // Display in middle for system message
                    if (message.endsWith("has joined the chat.") || message.endsWith("has left the chat")) {
                        appendMessage(message, centerAttr);

                    } else if (message.startsWith(username + ":")) {
                        // display own message on right side
                        appendMessage(message, rightAttr);
                    } else {
                        // left for other users' message
                        appendMessage(message, leftAttr);
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