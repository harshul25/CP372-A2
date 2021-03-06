import javax.swing.*;
import java.io.*;
import java.net.*;

public class ReceiverHandler {
    DatagramSocket ds;

    // JLabel to update on GUI based on in-order packets received
    private JLabel inOrderPacketLabel;

    public void startReceiving(String address, int senderPort, int receiverPort, String outputFileName, boolean reliable) throws IOException {
        System.out.println("Starting to receive on address: " + address + " at port: " + receiverPort + " with output going to: " + outputFileName + " and ACKS to port: " + senderPort);

        // Create new file if not existing
        if (new File(outputFileName).createNewFile()) {
            System.out.println("File not found, created.");
        }
        // Initialize Datagram data
        ds = new DatagramSocket(null);
        ds.bind(new InetSocketAddress(address, receiverPort));
        byte[] buf = new byte[1024];
        DatagramPacket dp = new DatagramPacket(buf, 1024);

        // Final data to write to file after reading all Datagrams
        StringBuilder finalData = new StringBuilder();

        // Used for dropping each 10th packet in unreliable mode and counting packets in-order and received
        int packetCount = 0;
        int inOrderPacketCount = 0;

        // Loop infinitely until broken
        while (true) {
            try {
                System.out.println("Awaiting data...");

                // Blocking call until a Datagram is received
                ds.receive(dp);
                packetCount++;

                // If reliable is selected or if it is not a multiple of 10th datagram sent, handle it
                if (reliable || packetCount % 10 != 0) {

                    // Build the data String based on the Datagram data excluding last value (our sequence number)
                    StringBuilder data = new StringBuilder();
                    for (int i = 0; i < dp.getLength(); i++) {
                        if (dp.getData()[i] >= 9) {
                            data.append((char) dp.getData()[i]);
                        }
                    }

                    // Add to our final data we will be writing
                    finalData.append(data.toString());

                    // Our sequence number to send back as an acknowledgement or read EOT
                    int sequenceNumber = dp.getData()[dp.getLength() - 1];

                    // Checks if our Datagram is an EOT Datagram
                    if (data.toString().contains("\t") && sequenceNumber == 4) {
                        PrintWriter writer = new PrintWriter(new FileWriter(outputFileName));
                        writer.print(finalData);
                        writer.close();
                        finalData = new StringBuilder();
                        inOrderPacketLabel.setText(inOrderPacketCount + "");
                        inOrderPacketCount = 0;
                    } else {
                        // Otherwise just update variables
                        inOrderPacketCount++;
                        inOrderPacketLabel.setText(inOrderPacketCount + "");
                    }

                    // Send back an acknowledgement to our Sender which data was received
                    String ack = "ACK " + sequenceNumber;
                    System.out.println("Data received and sending ACK " + sequenceNumber);
                    ds.send(new DatagramPacket(ack.getBytes(), ack.getBytes().length, InetAddress.getByName(address), senderPort));
                }
            } catch (IOException exception) {
                break;
            }
        }
    }

    // Assign our JLabel variable appropriately after GUI is initialized from GUI
    public void setInOrderPacketLabel(JLabel label) {
        this.inOrderPacketLabel = label;
    }

    // Disconnect/stop receiving
    public void stopReceiving() {
        ds.close();
    }

}

