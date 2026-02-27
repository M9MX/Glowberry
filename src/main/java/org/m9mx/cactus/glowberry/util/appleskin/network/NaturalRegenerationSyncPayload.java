package org.m9mx.cactus.glowberry.util.appleskin.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record NaturalRegenerationSyncPayload(boolean enabled) implements CustomPacketPayload {
    public static final Type<NaturalRegenerationSyncPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("glowberry", "natural_regeneration"));
    public static final StreamCodec<FriendlyByteBuf, NaturalRegenerationSyncPayload> CODEC = StreamCodec.ofMember(
        NaturalRegenerationSyncPayload::write,
        NaturalRegenerationSyncPayload::new
    );

    public NaturalRegenerationSyncPayload(FriendlyByteBuf buf) {
        this(buf.readBoolean());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(this.enabled);
    }

    @Override
    public Type<NaturalRegenerationSyncPayload> type() {
        return TYPE;
    }
}