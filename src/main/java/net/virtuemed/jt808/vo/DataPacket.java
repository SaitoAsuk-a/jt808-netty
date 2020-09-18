package net.virtuemed.jt808.vo;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import lombok.Data;
import net.virtuemed.jt808.config.MultiPacketManager;
import net.virtuemed.jt808.util.BCD;
import net.virtuemed.jt808.config.JT808Const;
import org.apache.commons.lang3.StringUtils;

/**
 * @Author: Zpsw
 * @Date: 2019-05-15
 * @Description:
 * @Version: 1.0
 */
@Data
public class DataPacket {
    private MultiPacketManager multiPacketManager = MultiPacketManager.getInstance();

    protected Header header = new Header(); //消息头
    protected ByteBuf payload; //消息体

    public DataPacket() {
    }

    public DataPacket(ByteBuf payload) {
        parse();
        if (header.hasSubPackage()) {
            multiPacketHandle();
        } else {
            this.payload = payload;
        }
        //由子类重写
        if (this.payload != null) {
            try {
                this.parseBody();
            } finally {
                //安全释放
                ReferenceCountUtil.safeRelease(this.payload);
            }
        }
    }

    public void parse() {
        try {
            this.parseHead();
            //验证包体长度
            if (this.header.getMsgBodyLength() != this.payload.readableBytes()) {
                throw new RuntimeException("包体长度有误");
            }
        } finally {
            //安全释放
            ReferenceCountUtil.safeRelease(this.payload);
        }
    }

    protected void parseHead() {
        header.setMsgId(payload.readShort());
        header.setMsgBodyProps(payload.readShort());
        header.setTerminalPhone(BCD.BCDtoString(readBytes(6)));
        header.setFlowId(payload.readShort());
        if (header.hasSubPackage()) {
            header.setPackageEncapsulationItem(payload.readInt());
        }
    }

    /**
     * 请求报文重写
     */
    protected void parseBody() {

    }

    /**
     * 响应报文重写 并调用父类
     * 当我们将响应转化为ByteBuf写出去的时候，此时并不知道消息体的具体长度，需要先占住位置，回头再来写
     *
     * @return
     */
    public ByteBuf toByteBufMsg() {
        /**
         * 尽量使用内存池分配ByteBuf，效率相比非池化Unpooled.buffer()高很多，但是得注意释放，否则会内存泄漏
         * 在ChannelPipeLine中我们可以使用ctx.alloc()或者channel.alloc()获取Netty默认内存分配器，
         * 其他地方不一定要建立独有的内存分配器，可以通过ByteBufAllocator.DEFAULT获取，跟前面获取的是同一个(不特别配置的话)。
         */
        ByteBuf bb = ByteBufAllocator.DEFAULT.heapBuffer();//在JT808Encoder escape()方法处回收
        bb.writeInt(0);//先占4字节用来写msgId和msgBodyProps，JT808Encoder中覆盖回来
        bb.writeBytes(BCD.toBcdBytes(StringUtils.leftPad(this.header.getTerminalPhone(), 12, "0")));
        bb.writeShort(this.header.getFlowId());
        //TODO 处理分包
        return bb;
    }

    /**
     * 从ByteBuf中read固定长度的数组,相当于ByteBuf.readBytes(byte[] dst)的简单封装
     *
     * @param length
     * @return
     */
    public byte[] readBytes(int length) {
        byte[] bytes = new byte[length];
        this.payload.readBytes(bytes);
        return bytes;
    }

    /**
     * 从ByteBuf中读出固定长度的数组 ，根据808默认字符集构建字符串
     *
     * @param length
     * @return
     */
    public String readString(int length) {
        return new String(readBytes(length), JT808Const.DEFAULT_CHARSET);
    }

    /**
     * 处理分包数据
     */
    public void multiPacketHandle() {
        byte[][] packages = multiPacketManager.addAndGet(header, payload.array());
        if (packages != null) {
            this.payload = Unpooled.wrappedBuffer(packages);
        }

    }
}
