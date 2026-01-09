package audioServer;

import java.io.Serializable;

public class Packet implements Serializable {
    private boolean isAck;
    private boolean isFinal;
    private int numSeq;
    private byte[] message;

    public Packet(int numSeq){
        this.message = null;
        this.isFinal = false;
        this.isAck = true;
        this.numSeq = numSeq;

    }

    public Packet(boolean isAck, boolean isFinal, int numSeq, byte[] message){
        this.isAck = isAck;
        this.isFinal = isFinal;
        this.numSeq = numSeq;
        this.message = message != null ? message.clone() : message;
    }

    public boolean isAck() {
        return isAck;
    }
    public void setAck(boolean isAck) {
        this.isAck = isAck;
    }
    public boolean isFinal() {
        return isFinal;
    }
    public void setFinal(boolean isFinal) {
        this.isFinal = isFinal;
    }
    public int getNumSeq() {
        return numSeq;
    }
    public void setNumSeq(int numSeq) {
        this.numSeq = numSeq;
    }
    public byte[] getMessage() {
        return message;
    }
    public void setMessage(byte[] message) {
        this.message = message;
    }

    
}
