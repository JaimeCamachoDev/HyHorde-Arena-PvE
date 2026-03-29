package com.hyhorde.arenapve.horde;

import com.hypixel.hytale.common.util.ArrayUtil;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.ToClientPacket;
import com.hypixel.hytale.protocol.packets.setup.AssetFinalize;
import com.hypixel.hytale.protocol.packets.setup.AssetInitialize;
import com.hypixel.hytale.protocol.packets.setup.AssetPart;
import com.hypixel.hytale.protocol.packets.setup.RequestCommonAssetsRebuild;
import com.hypixel.hytale.server.core.asset.common.CommonAsset;
import com.hypixel.hytale.server.core.io.PacketHandler;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

final class PlayerFaceAsset extends CommonAsset {
    private static final String ASSET_PATH_PREFIX = "UI/Custom/Pages/Horde/PlayerFace/";
    private static final String ASSET_HASH = "0000000000000000000000000000000000000000000000000000000000000000";
    private static final int CHUNK_SIZE = 2621440;
    private final byte[] data;

    PlayerFaceAsset(UUID playerId, byte[] data) {
        super(PlayerFaceAsset.getPathForUi(playerId), ASSET_HASH, PlayerFaceAsset.copyData(data));
        this.data = PlayerFaceAsset.copyData(data);
    }

    public static String getPathForUi(UUID playerId) {
        if (playerId == null) {
            return ASSET_PATH_PREFIX + "unknown.png";
        }
        return ASSET_PATH_PREFIX + playerId.toString() + ".png";
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
        packetHandler.writeNoCache((ToClientPacket)new RequestCommonAssetsRebuild());
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
}
