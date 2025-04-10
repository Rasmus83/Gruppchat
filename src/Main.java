import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

public class Main extends JFrame
{
    int port;
    String address;
    String networkInterfaceName;

    String username;
    Set<String> users;

    JPanel panel;
    JButton button;
    JTextField textField;
    JTextArea textArea1, textArea2;
    JScrollPane scrollPane1, scrollPane2;
    JSplitPane splitPane;

    public Main(int port, String address, String networkInterfaceName)
    {
        username = JOptionPane.showInputDialog("Enter your user name");
        if(username == null)
            System.exit(0);
        users = new HashSet<String>();

        this.port = port;
        this.address = address;
        this.networkInterfaceName = networkInterfaceName;

        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        setTitle("Chatt " + username);

        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                try (MulticastSocket multicastSocket = new MulticastSocket())
                {
                    InetAddress ip = InetAddress.getByName(address);
                    InetSocketAddress socketAddress = new InetSocketAddress(ip, port);
                    NetworkInterface networkInterface = NetworkInterface.getByName(networkInterfaceName);
                    multicastSocket.joinGroup(socketAddress, networkInterface);

                    String message = "LEAVE:" + username;

                    DatagramPacket datagramPacket = new DatagramPacket(message.getBytes(StandardCharsets.UTF_8), message.length(), ip, port);
                    multicastSocket.send(datagramPacket);
                }
                catch (IOException ex)
                {
                    ex.printStackTrace();
                }
            }
        });

        panel = new JPanel(new BorderLayout());

        button = new JButton("Koppla ner");
        button.addActionListener(e ->
        {
            try (MulticastSocket multicastSocket = new MulticastSocket())
            {
                InetAddress ip = InetAddress.getByName(address);
                InetSocketAddress socketAddress = new InetSocketAddress(ip, port);
                NetworkInterface networkInterface = NetworkInterface.getByName(networkInterfaceName);
                multicastSocket.joinGroup(socketAddress, networkInterface);

                String message = "LEAVE:" + username;

                DatagramPacket datagramPacket = new DatagramPacket(message.getBytes(StandardCharsets.UTF_8), message.length(), ip, port);
                multicastSocket.send(datagramPacket);
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
            }
            System.exit(0);
        });

        textField = new JTextField();
        textField.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyPressed(KeyEvent e)
            {
                if (e.getKeyCode() == KeyEvent.VK_ENTER)
                {
                    try (MulticastSocket multicastSocket = new MulticastSocket())
                    {
                        InetAddress ip = InetAddress.getByName(address);
                        InetSocketAddress socketAddress = new InetSocketAddress(ip, port);
                        NetworkInterface networkInterface = NetworkInterface.getByName(networkInterfaceName);
                        multicastSocket.joinGroup(socketAddress, networkInterface);

                        String message = "MSG:" + username + ": " + textField.getText() + "\n";

                        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
                        DatagramPacket datagramPacket = new DatagramPacket(messageBytes, messageBytes.length, ip, port);
                        multicastSocket.send(datagramPacket);
                    }
                    catch (IOException ex)
                    {
                        ex.printStackTrace();
                    }

                    textField.setText("");
                }
            }
        });

        textArea1 = new JTextArea();
        textArea1.setEditable(false);
        textArea1.setLineWrap(true);
        textArea1.setWrapStyleWord(true);
        textArea2 = new JTextArea("I chatten just nu:\n");
        textArea2.setEditable(false);
        textArea2.setLineWrap(true);
        textArea2.setWrapStyleWord(true);

        scrollPane1 = new JScrollPane(textArea1);
        scrollPane2 = new JScrollPane(textArea2);

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPane1, scrollPane2);
        splitPane.setResizeWeight(1);

        panel.add(button, BorderLayout.NORTH);
        panel.add(textField, BorderLayout.SOUTH);
        panel.add(splitPane, BorderLayout.CENTER);

        add(panel);

        setVisible(true);
    }
    public static void main(String[] args)
    {
        if(args.length < 3)
        {
            System.out.println("Required arguments: <port> <address> <network interface>");
            return;
        }
        int portNumber;
        try
        {
            portNumber = Integer.parseInt(args[0]);
        }
        catch(NumberFormatException e)
        {
            System.out.println("Invalid port number: " + args[0]);
            return;
        }

        Main window = new Main(portNumber, args[1], args[2]);

        try(MulticastSocket multicastSocket = new MulticastSocket(window.port))
        {
            InetAddress ip = InetAddress.getByName(window.address);
            InetSocketAddress socketAddress = new InetSocketAddress(ip, window.port);
            NetworkInterface networkInterface = NetworkInterface.getByName(window.networkInterfaceName);

            multicastSocket.joinGroup(socketAddress, networkInterface);

            String message = "JOIN:" + window.username + "\n";

            DatagramPacket datagramPacket = new DatagramPacket(message.getBytes(StandardCharsets.UTF_8), message.getBytes().length, ip, window.port);
            multicastSocket.send(datagramPacket);

            while (true)
            {
                datagramPacket = new DatagramPacket(new byte[1024], 1024);
                multicastSocket.receive(datagramPacket);

                String receivedData = new String(datagramPacket.getData(), 0, datagramPacket.getLength(), StandardCharsets.UTF_8);

                if(receivedData.startsWith("MSG:"))
                    window.textArea1.append(receivedData.substring(4));

                else if(receivedData.startsWith("HERE:"))
                {
                    if(!window.users.contains(receivedData.substring(5)))
                    {
                        window.textArea2.append(receivedData.substring(5));
                        window.users.add(receivedData.substring(5));
                    }
                }
                else if(receivedData.startsWith("LEAVE:"))
                {
                    String user = new String(datagramPacket.getData(), 6, datagramPacket.getLength() - 1, StandardCharsets.UTF_8).trim();
                    StringBuilder newTextArea = new StringBuilder();

                    if(window.users.contains(user))
                        window.users.remove(user);

                    for(String s : window.textArea2.getText().split("\n"))
                        if(!s.trim().equals(user) && !s.trim().isEmpty())
                            newTextArea.append(s).append("\n");

                    window.textArea2.setText(newTextArea.toString());
                }
                else if(receivedData.startsWith("JOIN:"))
                {
                    if(window.username.equals(receivedData.substring(5).trim()))
                    {
                        window.textArea2.append(receivedData.substring(5));
                        if(!window.users.contains(receivedData.substring(5).trim()))
                            window.users.add(receivedData.substring(5));
                    }

                    message = "HERE:" + window.username + "\n";

                    datagramPacket = new DatagramPacket(message.getBytes(StandardCharsets.UTF_8), message.getBytes().length, ip, window.port);
                    multicastSocket.send(datagramPacket);
                }
            }
        }
        catch (IOException e)
        {
            System.out.println("Kunde inte ansluta");
        }
    }
}
