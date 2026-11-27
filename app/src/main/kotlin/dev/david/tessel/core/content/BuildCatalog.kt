package dev.david.tessel.core.content

import dev.david.tessel.core.rng.Rng

/** Stable IDs: append new entries; never reorder the serialized ones. */
enum class CardFamily { GUN, HEALTH, RAM, MOBILITY, ECONOMY }

enum class CharacterDef(
    val label: String, val description: String, val extraHp: Int,
    val damageScale: Float, val speedScale: Float, val ramBase: Float,
    private val weights: List<Int>,
    val fireScale: Float = 1f, val regenSeconds: Int = 0,
) {
    BALANCED("Balanced", "All-rounder with 10% shop discount; even card weights.", 0, 1f, 1f, 0f, listOf(10,10,10,10,10)),
    VANGUARD("Vanguard", "+3 HP; regenerates 1 HP every 20 seconds of combat; slower fire and movement.", 3, 0.95f, 0.92f, 0f, listOf(12,30,8,10,10), fireScale = 0.95f, regenSeconds = 20),
    STRIKER("Striker", "-1 HP; +35% bullet damage; slightly slower fire.", -1, 1.35f, 1f, 0f, listOf(28,8,8,10,10), fireScale = 0.95f),
    // Keep the old serialized RAM ID so existing profiles and saves remain readable.
    RAM("Rapid", "-1 HP; +50% fire rate; +10% movement speed; lighter shots.", -1, 0.85f, 1.1f, 0f, listOf(28,8,8,18,10), fireScale = 1.5f);

    fun weight(family: CardFamily) = weights[family.ordinal]
    companion object { fun fromId(id: Int) = entries.getOrElse(id) { BALANCED } }
}

/** Rarity is sampled independently of family and character; levels are capped by remaining room. */
enum class CardRarity(val label: String, val weight: Int, val levels: Int, val premium: Double) {
    COMMON("Common",50,1,1.0), UNCOMMON("Uncommon",27,2,1.03),
    RARE("Rare",15,3,1.10), EPIC("Epic",7,4,1.15), LEGENDARY("Legendary",1,5,1.20);
    companion object {
        fun fromId(id: Int) = entries.getOrElse(id) { COMMON }
        fun roll(rng: Rng, luck: Int = 0): CardRarity {
            val shift = luck.coerceIn(0, 6)
            var ticket = rng.int(1, 100)
            for (tier in entries) {
                val weight = tier.weight + when (tier) { COMMON -> -2 * shift; EPIC, LEGENDARY -> shift; else -> 0 }
                ticket -= weight
                if (ticket <= 0) return tier
            }
            error("Invalid rarity weights")
        }
    }
}

/** A compact 16-bit encoding used in checkpoints and shop snapshots. */
object CardCode {
    fun pack(id: Int, rarity: CardRarity): Int {
        require(id in 0 until Upgrades.count && id < 256)
        return id or (rarity.ordinal shl 8)
    }
    fun valid(raw: Int) = raw in 0..32767 && (raw and 255) < Upgrades.count && (raw shr 8) < CardRarity.entries.size
    fun item(raw: Int) = if (valid(raw)) raw and 255 else -1
    fun tier(raw: Int) = if (valid(raw)) CardRarity.fromId(raw shr 8) else CardRarity.COMMON
    fun migrateLegacy(raw: Int): Int {
        if (raw < 0 || (raw and 0x40.inv()) !in 0..10) return -1
        return pack(raw and 0x40.inv(), if (raw and 0x40 != 0) CardRarity.RARE else CardRarity.COMMON)
    }
}
