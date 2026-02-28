package org.m9mx.cactus.glowberry.util.appleskin.network;
/**
 * Credits: https://github.com/squeek502/AppleSkin/tree/1.21.11-fabric
 */
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record SaturationSyncPayload(float saturation) implements CustomPacketPayload {
    public static final Type<SaturationSyncPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("glowberry", "saturation"));
    public static final StreamCodec<FriendlyByteBuf, SaturationSyncPayload> CODEC = StreamCodec.ofMember(
        SaturationSyncPayload::write,
        SaturationSyncPayload::new
    );

    public SaturationSyncPayload(FriendlyByteBuf buf) {
        this(buf.readFloat());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeFloat(this.saturation);
    }

    @Override
    public Type<SaturationSyncPayload> type() {
        return TYPE;
    }
}