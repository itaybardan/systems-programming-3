package bgu.spl.net.impl.Messages;

public class Logout extends Message {

    public Logout() {
        this.opcode = Opcode.LOGOUT;
    }

    @Override
    public byte[] convertMessageToBytes() {
        return this.shortToBytes(this.opcode.getCode());
    }

}
