import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.net.*;

public class Main extends JFrame
{
    int port;
    String address;
    String networkInterfaceName;

    JPanel panel;
    JButton button;
    JTextField textField;
    JTextArea textArea1, textArea2;

    public Main(int port, String address, String networkInterfaceName)
    {
        this.port = port;
        this.address = address;
        this.networkInterfaceName = networkInterfaceName;

        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        setTitle("Chatt " + System.getProperty("user.name"));

        addWindowListener(new WindowListener()
        {
            @Override
            public void windowOpened(WindowEvent e)
            {}

            @Override
            public void windowClosing(WindowEvent e)
            {
                try (MulticastSocket multicastSocket = new MulticastSocket())
                {
                    InetAddress ip = InetAddress.getByName(address);
                    InetSocketAddress socketAddress = new InetSocketAddress(ip, port);
                    NetworkInterface networkInterface = NetworkInterface.getByName(networkInterfaceName);
                    multicastSocket.joinGroup(socketAddress, networkInterface);

                    String message = "LEAVE:" + System.getProperty("user.name");

                    DatagramPacket datagramPacket = new DatagramPacket(message.getBytes(), message.length(), ip, port);
                    multicastSocket.send(datagramPacket);
                }
                catch (IOException ex)
                {
                    ex.printStackTrace();
                }
            }

            @Override
            public void windowClosed(WindowEvent e)
            {}

            @Override
            public void windowIconified(WindowEvent e)
            {}

            @Override
            public void windowDeiconified(WindowEvent e)
            {}

            @Override
            public void windowActivated(WindowEvent e)
            {}

            @Override
            public void windowDeactivated(WindowEvent e)
            {}
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

                String message = "LEAVE:" + System.getProperty("user.name");

                DatagramPacket datagramPacket = new DatagramPacket(message.getBytes(), message.length(), ip, port);
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

                        String message = "MSG:" + System.getProperty("user.name") + ": " + textField.getText() + "\n";

                        DatagramPacket datagramPacket = new DatagramPacket(message.getBytes(), message.length(), ip, port);
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
        textArea2 = new JTextArea("I chatten just nu:       \n");
        textArea1.setEditable(false);
        textArea2.setEditable(false);

        panel.add(button, BorderLayout.NORTH);
        panel.add(textField, BorderLayout.SOUTH);
        panel.add(textArea1, BorderLayout.WEST);
        panel.add(textArea2, BorderLayout.EAST);

        add(panel);

        setVisible(true);
    }
    public static void main(String[] args)
    {
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
        catch(ArrayIndexOutOfBoundsException e)
        {
            System.out.println("No port number");
            return;
        }
        Main window = new Main(portNumber, args[1], args[2]);

        try(MulticastSocket multicastSocket = new MulticastSocket(window.port))
        {
            InetAddress ip = InetAddress.getByName(window.address);
            InetSocketAddress socketAddress = new InetSocketAddress(ip, window.port);
            NetworkInterface networkInterface = NetworkInterface.getByName(window.networkInterfaceName);

            multicastSocket.joinGroup(socketAddress, networkInterface);

            String message = "JOIN:" + System.getProperty("user.name") + "\n";

            DatagramPacket datagramPacket = new DatagramPacket(message.getBytes(), message.getBytes().length, ip, window.port);
            multicastSocket.send(datagramPacket);

            while (true)
            {
                datagramPacket = new DatagramPacket(new byte[1024], 1024);
                multicastSocket.receive(datagramPacket);
                if(new String(datagramPacket.getData()).startsWith("MSG:"))
                    window.textArea1.setText(window.textArea1.getText() + new String(datagramPacket.getData(), 4, datagramPacket.getLength()));
                else if(new String(datagramPacket.getData()).startsWith("HERE:"))
                {
                    if(!System.getProperty("user.name").equals(new String(datagramPacket.getData(), 5, datagramPacket.getLength() - 1
                    ).trim()))
                    {
                        window.textArea2.setText(window.textArea2.getText() + new String(datagramPacket.getData(), 5, datagramPacket.getLength()));
                    }
                }
                else if(new String(datagramPacket.getData()).startsWith("LEAVE:"))
                {
                    String user = new String(datagramPacket.getData(), 6, datagramPacket.getLength() - 1).trim();
                    StringBuilder newTextArea = new StringBuilder();
                    for(String s : window.textArea2.getText().split("\n"))
                        if(!s.trim().equals(user) && !s.trim().isEmpty())
                            newTextArea.append(s).append("\n");
                    window.textArea2.setText(newTextArea.toString());
                }
                else
                {
                    if(System.getProperty("user.name").equals(new String(datagramPacket.getData(), 5, datagramPacket.getLength()).trim()))
                        window.textArea2.setText(window.textArea2.getText() + new String(datagramPacket.getData(), 5, datagramPacket.getLength()));

                    message = "HERE:" + System.getProperty("user.name") + "\n";

                    datagramPacket = new DatagramPacket(message.getBytes(), message.getBytes().length, ip, window.port);
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
