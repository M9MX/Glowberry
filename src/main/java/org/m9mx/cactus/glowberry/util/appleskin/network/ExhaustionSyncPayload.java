package org.m9mx.cactus.glowberry.util.appleskin.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record ExhaustionSyncPayload(float exhaustion) implements CustomPacketPayload {
    public static final Type<ExhaustionSyncPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("glowberry", "exhaustion"));
    public static final StreamCodec<FriendlyByteBuf, ExhaustionSyncPayload> CODEC = StreamCodec.ofMember(
        ExhaustionSyncPayload::write,
        ExhaustionSyncPayload::new
    );

    public ExhaustionSyncPayload(FriendlyByteBuf buf) {
        this(buf.readFloat());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeFloat(this.exhaustion);
    }

    @Override
    public Type<ExhaustionSyncPayload> type() {
        return TYPE;
    }
}