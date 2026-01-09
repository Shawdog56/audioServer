package audioServer.Server;

import audioServer.Packet;
import audioServer.ServerConfig;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;


public class Sender {
    private final DatagramSocket socket;
    private final int PORT;
    private final int PORTd;
    private final InetAddress HOST;

    public Sender(String HOST, int PORT, int PORTd) throws SocketException, UnknownHostException{
        this.PORT = PORT;
        this.HOST = InetAddress.getByName(HOST);
        this.socket = new DatagramSocket(PORT);
        this.PORTd = PORTd;
        this.socket.setSoTimeout(50);
    }

    public int recvAck() throws SocketTimeoutException, IOException, ClassNotFoundException, Exception {
        byte[] message = new byte[ServerConfig.msgSize*2];
        DatagramPacket dp = new DatagramPacket(message, message.length);

        this.socket.receive(dp);
        try (ByteArrayInputStream bais = new ByteArrayInputStream(dp.getData(),0,dp.getLength());
            ObjectInputStream ois = new ObjectInputStream(bais);){
            Packet p = (Packet) ois.readObject();
            if(p.isAck())
                return p.getNumSeq();
            else
                throw new Exception("No ack received");
        }
        
    }

    public boolean sendPacket(Packet packet) throws IOException{
        boolean sentSuccessfully = true;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);

        oos.writeObject(packet);
        oos.flush();
        byte[] message = baos.toByteArray();

        DatagramPacket dp = new DatagramPacket(message, message.length, this.HOST, this.PORTd);
        this.socket.send(dp);

        oos.close();
        baos.close();

        return sentSuccessfully;
    }

    public List<Packet> openFile(String url) throws IOException{
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(url));
        int read,it = 0;
        List<Packet> chunks = new ArrayList<>();
        
        byte[] chunk = new byte[ServerConfig.msgSize];
        while((read = bis.read(chunk)) > 0){
            byte[] payload = new byte[read];
            System.arraycopy(chunk, 0, payload, 0, read);
            Packet p = new Packet(false,false,(it++)%ServerConfig.windowSize,payload);
            chunks.add(p);
        }
        Packet p = new Packet(false,true,(it++)%ServerConfig.windowSize,null);
        chunks.add(p);

        return chunks;
    }

    public DatagramSocket getSocket() {
        return socket;
    }
    public int getPORT() {
        return PORT;
    }
    public static void main(String[] args){
        if(args.length != 4){
            System.out.println("Execution: java AudioServer.Server.Sender <HOST> <PORT> <filename> <PORT destination>");
            return;
        }    
        try {
            Sender sender = new Sender(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[3])); 
            
            List<Packet> packets = sender.openFile(args[2]);
            int windowIt = 0;

            while(windowIt < packets.size()){
                int base = windowIt;
                System.out.println(base + " packets of " + packets.size());
                while(base < windowIt+ServerConfig.windowSize){

                    for(int i = 0; i + base < ServerConfig.windowSize+windowIt; i++){
                        sender.sendPacket(packets.get(i+base));
                    }
                    
                    boolean[] ackRcv = new boolean[ServerConfig.windowSize];
                    
                    long startTime = System.currentTimeMillis();
                    int rcv = 0;
                    while(System.currentTimeMillis() - startTime < 50 && rcv < ServerConfig.windowSize){
                        try{
                            int number = sender.recvAck();
                            ackRcv[number] = true;
                            rcv++;
                        } catch(SocketTimeoutException e) {
                            
                        } catch(Exception e){
                            
                        }
                        
                    }
                    
                    while(base < windowIt+ServerConfig.windowSize && ackRcv[base-windowIt])
                        base++;
                }
                windowIt += ServerConfig.windowSize;
            }

           System.out.println("Finished Sending packets");
           sender.getSocket().close();
        } catch (UnknownHostException e) {
            System.out.println("Error on name resolution for host " + args[0]);
            e.printStackTrace();
        } catch (SocketException e){
            System.out.println("Socket couldn't be created " + e.toString());
        } catch(FileNotFoundException e){
            System.out.println("File not found, please verify the path " + e.toString());
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getPORTd() {
        return PORTd;
    }

    public InetAddress getHOST() {
        return HOST;
    }

}
