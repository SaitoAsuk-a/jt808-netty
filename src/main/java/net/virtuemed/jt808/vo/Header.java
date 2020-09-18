package net.virtuemed.jt808.vo;

import lombok.Data;

/**
 * @author liyu
 * date 2020/9/18 14:48
 * description
 */
@Data
public class Header {
    private short msgId;// 消息ID 2字节
    private short msgBodyProps;//消息体属性 2字节
    private String terminalPhone; // 终端手机号 6字节
    private short flowId;// 流水号 2字节
    private int packageEncapsulationItem;// 消息包封装项 4字节

    //获取包体长度
    public short getMsgBodyLength() {
        return (short) (msgBodyProps & 0x3ff);
    }

    //获取加密类型 3bits
    public byte getEncryptionType() {
        return (byte) ((msgBodyProps & 0x1c00) >> 10);
    }

    //是否分包
    public boolean hasSubPackage() {
        return ((msgBodyProps & 0x2000) >> 13) == 1;
    }

    /**
     * 获取分包总包数
     *
     * @return
     */
    public byte getPacketsTotalNumber() {
        return (byte) (packageEncapsulationItem >> 16);
    }

    /**
     * 获取分包序号
     *
     * @return
     */
    public byte getPacketSequenceNumber() {
        return (byte) (packageEncapsulationItem & 0xff);
    }

}
