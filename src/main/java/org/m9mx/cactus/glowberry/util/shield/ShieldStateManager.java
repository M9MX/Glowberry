package org.m9mx.cactus.glowberry.util.shield;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemCooldowns;


import java.util.HashMap;
import java.util.Map;

public class ShieldStateManager {

	private final long ATTACK_ENTRY_TTL_MS = 1000;
	private final Map<Player, Integer> shieldUseTicks = new HashMap<>();
	private final Map<Player, AttackEntry> attackedPlayerEntries = new HashMap<>();
	private final Minecraft client = Minecraft.getInstance();

	private record AttackEntry(Vec3 attackPos, Vec3 targetPos, long time, boolean wasBlocking) { }

	private boolean clientShieldDisabled = false;
	private long lastClientShieldDisabledAt = 0L;

	private final ShieldCooldownManager cooldownManager = new ShieldCooldownManager();

	public void set(Player player) {
		if (player == null) return;
		cooldownManager.setCooldown(player);
		if (client != null && client.player != null && player == client.player) {
			clientShieldDisabled = true;
			lastClientShieldDisabledAt = System.currentTimeMillis();
		}
	}

	public void update() {
		long now = System.currentTimeMillis();
		if (client.level == null) return;

		for (Player player : client.level.players()) {
			if (isHoldingUsableShield(player) && player.isUsingItem()) {
				int current = shieldUseTicks.getOrDefault(player, 0);
				shieldUseTicks.put(player, current + 1);
			} else {
				shieldUseTicks.put(player, 0);
			}
		}

		attackedPlayerEntries.entrySet().removeIf(e -> now - e.getValue().time > ATTACK_ENTRY_TTL_MS);

		cooldownManager.tick();

		if (client.player != null) {
			boolean nowDisabled = cooldownManager.isCoolingDown(client.player);
			if (nowDisabled && !clientShieldDisabled) {
				lastClientShieldDisabledAt = System.currentTimeMillis();
			}
			clientShieldDisabled = nowDisabled;
		} else {
			clientShieldDisabled = false;
		}
	}

	public void handleBreakPacket(double x, double y, double z) {
		if (client == null || client.level == null || client.player == null) return;

		Player local = client.player;
		long now = System.currentTimeMillis();

		long CLIENT_DISABLE_GRACE_MS = 250L;
		//if (now - lastClientShieldDisabledAt <= CLIENT_DISABLE_GRACE_MS) return;

		int MATCH_RADIUS = 5;
		final double matchRadiusSq = (double) MATCH_RADIUS * MATCH_RADIUS;
		Player best = null;
		double bestScore = Double.NEGATIVE_INFINITY;
		double bestClientSq = Double.POSITIVE_INFINITY;

		for (Map.Entry<Player, AttackEntry> e : attackedPlayerEntries.entrySet()) {
			Player candidate = e.getKey();
			AttackEntry ae = e.getValue();
			if (candidate == null || candidate == local) continue;
			if (now - ae.time > ATTACK_ENTRY_TTL_MS) continue;

			double apx = ae.attackPos.x, apy = ae.attackPos.y, apz = ae.attackPos.z;
			double tpx = ae.targetPos.x, tpy = ae.targetPos.y, tpz = ae.targetPos.z;

			double clientDx = apx - x, clientDy = apy - y, clientDz = apz - z;
			double clientSq = clientDx * clientDx + clientDy * clientDy + clientDz * clientDz;

			double dx = tpx - x, dy = tpy - y, dz = tpz - z;
			double distSq = dx * dx + dy * dy + dz * dz;
			if (distSq > matchRadiusSq) continue;

			double score = Math.max(0.0, (matchRadiusSq - distSq)) / matchRadiusSq;
			if (ae.wasBlocking) score += 0.30;
			if (!clientShieldDisabled && clientSq + 0.01 < distSq) score -= 0.35;

			if (score > bestScore) {
				bestScore = score;
				best = candidate;
				bestClientSq = clientSq;
			}
		}

		double threshold = clientShieldDisabled ? 0.05 : 0.25;
		if (best == null || bestScore <= threshold) return;

		AttackEntry bestEntry = attackedPlayerEntries.get(best);
		if (bestEntry == null) return;

		double dx = bestEntry.targetPos.x - x, dy = bestEntry.targetPos.y - y, dz = bestEntry.targetPos.z - z;
		double bestDistSq = dx * dx + dy * dy + dz * dz;

		double clientScore = Math.max(0.0, (matchRadiusSq - bestClientSq)) / matchRadiusSq;

		double candDx = best.getX() - x, candDy = best.getY() - y, candDz = best.getZ() - z;
		double candidateCurrentDistSq = candDx * candDx + candDy * candDy + candDz * candDz;

		final double EPS = 0.05;
		final double CLOSE_DIST_SQ = 0.5;

		boolean accept = false;

		if (bestEntry.wasBlocking) {
			if (bestDistSq <= bestClientSq + EPS || candidateCurrentDistSq <= bestClientSq + EPS) accept = true;
			if (bestDistSq <= CLOSE_DIST_SQ && bestClientSq <= CLOSE_DIST_SQ) accept = true;
		}

		if (!accept) {
			double relativeFactor, deltaThreshold;
			if (clientShieldDisabled) {
				relativeFactor = 1.02;
				deltaThreshold = 0.05;
			} else {
				relativeFactor = 1.05;
				deltaThreshold = 0.08;
			}
			if (bestScore > clientScore * relativeFactor) accept = true;
			else if (bestScore - clientScore > deltaThreshold) accept = true;
		}

		if (accept) {
			set(best);
			attackedPlayerEntries.remove(best);
		}
	}

	public void handleEntityStatus(Player player, byte status) {
		if (status == 30) {
			System.out.println("[Shield] Shield break for " + player.getName().getString());
			this.set(player);
		}
	}

	public void handlePlayerAttack(Player target) {
		boolean estBlocking = shieldUseTicks.getOrDefault(target, 0) >= 3;
		if (client == null || client.player == null) return;
		if (disablesShield(client.player)) {
			attackedPlayerEntries.put(
				target,
				new AttackEntry(
					client.player.position(),
					target.position(),
					System.currentTimeMillis(),
					estBlocking
				)
			);
		}
	}

	public boolean isCoolingDown(Player player) {
		if (player == client.player) {
			return client.player.getCooldowns().isOnCooldown(new ItemStack(Items.SHIELD));
		}
		return cooldownManager.isCoolingDown(player);
	}

	public float getCooldownProgress(Player player) {
		if (player == null) return 0.0f;
		if (player == client.player) {
			return client.player.getCooldowns().getCooldownPercent(new ItemStack(Items.SHIELD), 0);
		}
		int remaining = cooldownManager.getRemainingTicks(player);
		if (remaining <= 0) return 0.0f;
		float frac = remaining / (float) 100;
		return Math.max(0f, Math.min(1f, frac));
	}

	public boolean isUsingShield(Player player) {
		return shieldUseTicks.getOrDefault(player, 0) >= 5;
	}

	public boolean isHoldingUsableShield(Player entity) {
		return (entity.getMainHandItem().is(Items.SHIELD)
			|| entity.getOffhandItem().is(Items.SHIELD))
			&& !isHoldingAnimationItemMainHand(entity);
	}

	private boolean isHoldingAnimationItemMainHand(Player entity) {
		return entity.getMainHandItem().getUseDuration(entity) != 0
			&& !entity.getMainHandItem().is(Items.SHIELD);
	}

	public boolean disablesShield(Player player) {
		return player.getWeaponItem().getItem() instanceof AxeItem;
	}
}
