package com.hyhorde.arenapve.horde;

import com.hypixel.hytale.common.util.ArrayUtil;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.ToClientPacket;
import com.hypixel.hytale.protocol.packets.setup.AssetFinalize;
import com.hypixel.hytale.protocol.packets.setup.AssetInitialize;
import com.hypixel.hytale.protocol.packets.setup.AssetPart;
import com.hypixel.hytale.server.core.asset.common.CommonAsset;
import com.hypixel.hytale.server.core.io.PacketHandler;
import java.security.MessageDigest;
import java.util.concurrent.CompletableFuture;

final class NpcFaceAsset extends CommonAsset {
    private static final String ASSET_PATH_PREFIX = "UI/Custom/Pages/Horde/NpcFace/";
    private static final int CHUNK_SIZE = 2621440;
    private static final char[] HEX = "0123456789abcdef".toCharArray();
    private final byte[] data;

    NpcFaceAsset(String npcKey, byte[] data) {
        super(NpcFaceAsset.getPathForUi(npcKey), NpcFaceAsset.computeHash(data), NpcFaceAsset.copyData(data));
        this.data = NpcFaceAsset.copyData(data);
    }

    public static String getPathForUi(String npcKey) {
        if (npcKey == null || npcKey.isBlank()) {
            return ASSET_PATH_PREFIX + "unknown.png";
        }
        return ASSET_PATH_PREFIX + npcKey + ".png";
    }

    public static void sendToPlayer(PacketHandler packetHandler, CommonAsset asset) {
        if (packetHandler == null || asset == null) {
            return;
        }
        byte[] blob = (byte[])asset.getBlob().join();
        if (blob == null || blob.length == 0) {
            return;
        }
        byte[][] chunks = ArrayUtil.split(blob, CHUNK_SIZE);
        Packet[] packets = new Packet[chunks.length + 2];
        packets[0] = new AssetInitialize(asset.toPacket(), blob.length);
        for (int i = 0; i < chunks.length; ++i) {
            packets[i + 1] = new AssetPart(chunks[i]);
        }
        packets[packets.length - 1] = new AssetFinalize();
        for (Packet packet : packets) {
            packetHandler.write((ToClientPacket)packet);
        }
    }

    @Override
    protected CompletableFuture<byte[]> getBlob0() {
        return CompletableFuture.completedFuture(this.data);
    }

    private static byte[] copyData(byte[] data) {
        if (data == null || data.length == 0) {
            return new byte[0];
        }
        return data.clone();
    }

    private static String computeHash(byte[] data) {
        if (data == null || data.length == 0) {
            return "0".repeat(64);
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(data);
            char[] chars = new char[digest.length * 2];
            for (int i = 0; i < digest.length; ++i) {
                int value = digest[i] & 255;
                chars[i * 2] = HEX[value >>> 4];
                chars[i * 2 + 1] = HEX[value & 15];
            }
            return new String(chars);
        }
        catch (Exception ignored) {
            return "0".repeat(64);
        }
    }
}
