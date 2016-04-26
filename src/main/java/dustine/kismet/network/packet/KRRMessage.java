package dustine.kismet.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * Report back to client the success/failure of using the key
 * <p>
 * Includes: success flag
 */
public class KRRMessage implements IMessage {
    public boolean success;

    public KRRMessage() {
    }

    public KRRMessage(boolean success) {
        this.success = success;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        success = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(success);
    }
}