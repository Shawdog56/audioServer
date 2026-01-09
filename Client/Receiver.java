package audioServer.Client;

import audioServer.Packet;
import audioServer.ServerConfig;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import javax.sound.sampled.*;

public class Receiver {
    private final DatagramSocket socket;
    private final int PORT;
    private final int PORTd;

    public Receiver(int PORT, int PORTd) throws SocketException{
        this.PORT = PORT;
        this.PORTd = PORTd;
        this.socket = new DatagramSocket(PORT);
    }

    public Packet receivePacket() throws IOException, ClassNotFoundException {
        byte[] buffer = new byte[ServerConfig.msgSize * 2];
        DatagramPacket dp = new DatagramPacket(buffer, buffer.length);

        socket.receive(dp);

        try (
            ByteArrayInputStream bais =
                new ByteArrayInputStream(dp.getData(), 0, dp.getLength());
            ObjectInputStream ois = new ObjectInputStream(bais)
        ) {
            return (Packet) ois.readObject();
        }
    }


    public void sendAck(int numSeq) throws IOException{
        Packet ack = new Packet(numSeq);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);

        oos.writeObject(ack);
        oos.flush();

        byte[] message = baos.toByteArray();
        DatagramPacket dp = new DatagramPacket(message,message.length,InetAddress.getByName("127.0.0.1"),this.PORTd);

        this.socket.send(dp);
        oos.close();
        baos.close();
    }


    public DatagramSocket getSocket() {
        return socket;
    }

    public int getPORT() {
        return PORT;
    }

    public static void main(String[] args) {
        if(args.length != 3){
            System.out.println("Execution: java AudioServer.Client.Receiver <PORT> <filename> <PORT destination>");
            return;
        }
        try {
            Receiver receiver = new Receiver(Integer.parseInt(args[0]), Integer.parseInt(args[2]));
            FileOutputStream fos = new FileOutputStream(new File(args[1]));
            BufferedOutputStream bos = new BufferedOutputStream(fos);

            
            int expected = 0;
            while (true) { 
                Packet p;
                if((p = receiver.receivePacket()).isFinal()){
                    System.out.println("Final packet received!!!");
                    break;
                }
                expected = expected%ServerConfig.windowSize;
                System.out.println("Packet received: " + p.getNumSeq() + " expected: " + expected%ServerConfig.windowSize);
                if(p.getNumSeq() != expected){
                    System.out.println("Waiting for expected packet, skipping this one...");
                    continue;
                }
                receiver.sendAck(expected);
                expected++;
                try{
                    bos.write(p.getMessage());
                } catch(Exception e){
                    expected--;
                }
            }

            SimpleAudioPlayer sap = new SimpleAudioPlayer();

            sap.playWav(args[1]);
            bos.flush();

            fos.close();
            bos.close();
            receiver.getSocket().close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class SimpleAudioPlayer {
    public void playWav(String filePath) {
        try {
            File audioFile = new File(filePath);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            clip.start();
            
            System.out.println("Playing audio: " + filePath);
            
            // This keeps the program alive while the audio plays
            Thread.sleep(clip.getMicrosecondLength() / 1000); 
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}