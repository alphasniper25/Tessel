package dev.david.tessel.core.sim

import dev.david.tessel.core.content.Difficulty
import dev.david.tessel.core.content.CardCode
import dev.david.tessel.core.content.CharacterDef
import dev.david.tessel.core.content.CardMission
import dev.david.tessel.core.content.Perks
import dev.david.tessel.core.content.SkillGraph
import dev.david.tessel.core.content.SkillTree
import dev.david.tessel.core.content.Scoring
import dev.david.tessel.core.content.Upgrades
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * A run between stages, kept so a game can stop and carry on another day.
 * Only what lasts from one stage to the next is in it: who plays, their
 * health, coins, upgrades and statistics, and what the shop has on offer.
 * Enemies and shots are not; a run picks up in the shop before the stage it
 * stopped in. One taken at the very start of a run ([inShop] false) starts
 * that run over with the same players.
 */
class Checkpoint(
    val difficulty: Difficulty,
    val stage: Int,
    val tick: Int,
    val players: Int,
    val inShop: Boolean,
    val bossesDown: Int,
) {
    val present = BooleanArray(MAX_PLAYERS)
    val hp = IntArray(MAX_PLAYERS)
    val maxHp = IntArray(MAX_PLAYERS)
    val coins = IntArray(MAX_PLAYERS)
    val earned = IntArray(MAX_PLAYERS)
    val kills = IntArray(MAX_PLAYERS)
    val points = IntArray(MAX_PLAYERS)
    val fired = IntArray(MAX_PLAYERS)
    val dealt = FloatArray(MAX_PLAYERS)
    val taken = IntArray(MAX_PLAYERS)
    val downs = IntArray(MAX_PLAYERS)
    val revived = IntArray(MAX_PLAYERS)
    val levels = Array(MAX_PLAYERS) { IntArray(Upgrades.count) }
    val stock = Array(MAX_PLAYERS) { IntArray(SHOP_SLOTS) { -1 } }
    val locked = Array(MAX_PLAYERS) { BooleanArray(SHOP_SLOTS) }
    val quotes = Array(MAX_PLAYERS) { IntArray(SHOP_SLOTS) }
    val stockSaved = BooleanArray(MAX_PLAYERS)
    internal var loadedUpgradeCount = Upgrades.count
    val lastEvolutionStage = IntArray(MAX_PLAYERS)
    val upgradeSpent = Array(MAX_PLAYERS) { IntArray(Upgrades.count) }
    val evolution = Array(MAX_PLAYERS) { dev.david.tessel.core.content.TankEvolution.BASIC }
    val character = Array(MAX_PLAYERS) { CharacterDef.BALANCED }
    val appearance = Array(MAX_PLAYERS) { dev.david.tessel.core.content.TankLook() }
    val missionMask = IntArray(MAX_PLAYERS)
    val rerolls = IntArray(MAX_PLAYERS)
    val freeRerollsUsed=IntArray(MAX_PLAYERS)
    val skillNodes=LongArray(MAX_PLAYERS)
    val heals = IntArray(MAX_PLAYERS)
    val perks = IntArray(MAX_PLAYERS)
    val rewardRounds=Array(MAX_PLAYERS){IntArray(dev.david.tessel.core.content.RewardRounds.COUNT){-1}}
    val perkOffer = Array(MAX_PLAYERS) { IntArray(Perks.OFFERED) { -1 } }

    /** Seconds of play so far. */
    val seconds: Int get() = tick / TICK_HZ

    /** Player [p]'s score as the run stood when it was saved. */
    fun score(p: Int = 0) = Scoring.total(
        difficulty, seconds, if (inShop) stage else 0, bossesDown, points[p], levels[p].sum(), revived[p],
    )

    /** The stage played next when this run carries on. */
    val nextStage: Int get() = if (inShop) stage + 1 else 1

    /** Who sat in each seat: an id that stays with the phone, and the name it showed. The lobby fills these in. */
    val ids = LongArray(MAX_PLAYERS)
    val names = Array(MAX_PLAYERS) { "" }

    fun write(): ByteArray {
        val buf = ByteBuffer.allocate(64 + players * (198 + Upgrades.count * 5 + SHOP_SLOTS * 6)).order(ByteOrder.LITTLE_ENDIAN)
        buf.put('T'.code.toByte())
        buf.put('C'.code.toByte())
        buf.put(FORMAT.toByte())
        buf.put(difficulty.ordinal.toByte())
        buf.putShort(stage.toShort())
        buf.putInt(tick)
        buf.put(players.toByte())
        buf.put((if (inShop) 1 else 0).toByte())
        buf.putShort(bossesDown.toShort())
        buf.put(Upgrades.count.toByte())
        buf.put(SHOP_SLOTS.toByte())
        for (p in 0 until players) {
            buf.put((if (present[p]) 1 else 0).toByte())
            buf.putLong(ids[p])
            val name = names[p].encodeToByteArray().let { it.copyOf(minOf(it.size, 60)) }
            buf.put(name.size.toByte())
            buf.put(name)
            buf.putShort(hp[p].toShort())
            buf.putShort(maxHp[p].toShort())
            buf.putInt(coins[p])
            buf.putInt(earned[p])
            buf.putInt(kills[p])
            buf.putInt(points[p])
            buf.putInt(fired[p])
            buf.putFloat(dealt[p])
            buf.putInt(taken[p])
            buf.putInt(downs[p])
            buf.putInt(revived[p])
            for (u in 0 until Upgrades.count) buf.put(levels[p][u].toByte())
            for (slot in 0 until SHOP_SLOTS) buf.putShort(stock[p][slot].toShort())
            buf.putInt(rerolls[p])
            buf.put(heals[p].toByte())
            buf.putInt(perks[p])
            for (k in 0 until Perks.OFFERED) buf.put(perkOffer[p][k].toByte())
            buf.put(character[p].ordinal.toByte())
            buf.putInt(missionMask[p])
            buf.putShort(appearance[p].code().toShort())
            buf.put(evolution[p].ordinal.toByte())
            buf.putShort(lastEvolutionStage[p].toShort())
            for(u in 0 until Upgrades.count) buf.putInt(upgradeSpent[p][u])
            buf.put(if (stockSaved[p]) 1.toByte() else 0.toByte())
            for (slot in 0 until SHOP_SLOTS) {
                buf.put(if (locked[p][slot]) 1.toByte() else 0.toByte())
                buf.putInt(quotes[p][slot])
            }
            buf.putLong(skillNodes[p] or SkillGraph.fromLegacy(perks[p]))
            buf.put(freeRerollsUsed[p].coerceIn(0,4).toByte())
            for(id in rewardRounds[p])buf.put(id.toByte())
        }
        val out = ByteArray(buf.position())
        buf.flip()
        buf.get(out)
        return out
    }

    companion object {
        /** Bump when the layout changes. Saves from a version before [OLDEST] are dropped rather than misread. */
        const val FORMAT = 9
        private const val OLDEST = 1

        /** Null when [bytes] are not a save this version can read. */
        fun read(bytes: ByteArray): Checkpoint? {
            val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            return try {
                if (buf.get() != 'T'.code.toByte() || buf.get() != 'C'.code.toByte()) return null
                val format = buf.get().toInt()
                if (format !in OLDEST..FORMAT) return null
                val difficulty = Difficulty.entries.getOrNull(buf.get().toInt()) ?: return null
                val stage = buf.short.toInt()
                val tick = buf.int
                val players = buf.get().toInt()
                if (players !in 1..MAX_PLAYERS || stage < 1) return null
                val inShop = buf.get().toInt() != 0
                val bosses = buf.short.toInt()
                val upgrades = buf.get().toInt() and 0xFF
                val slots = buf.get().toInt() and 0xFF
                val cp = Checkpoint(difficulty, stage, tick, players, inShop, bosses)
                cp.loadedUpgradeCount = upgrades
                for (p in 0 until players) {
                    cp.present[p] = buf.get().toInt() != 0
                    cp.ids[p] = buf.long
                    val name = ByteArray(buf.get().toInt() and 0xFF)
                    buf.get(name)
                    cp.names[p] = name.decodeToString()
                    cp.hp[p] = buf.short.toInt()
                    cp.maxHp[p] = buf.short.toInt()
                    cp.coins[p] = buf.int
                    cp.earned[p] = buf.int
                    cp.kills[p] = buf.int
                    // Saves from before the score existed start counting kill points from here.
                    if (format >= 2) cp.points[p] = buf.int
                    cp.fired[p] = buf.int
                    cp.dealt[p] = buf.float
                    cp.taken[p] = buf.int
                    cp.downs[p] = buf.int
                    cp.revived[p] = buf.int
                    // A save from a version with more or fewer kinds of upgrade keeps the ones both know.
                    for (u in 0 until upgrades) {
                        val level = buf.get().toInt()
                        if (u < Upgrades.count) cp.levels[p][u] = level.coerceIn(0, Upgrades.of(u).maxLevel)
                    }
                    for (slot in 0 until slots) {
                        val item = if (format >= 4) buf.short.toInt() else buf.get().toInt()
                        if (slot < SHOP_SLOTS) cp.stock[p][slot] = if (format < 4) CardCode.migrateLegacy(item) else if (CardCode.valid(item)) item else -1
                    }
                    cp.rerolls[p] = if (format >= 4) buf.int.coerceAtLeast(0) else buf.get().toInt() and 0xFF
                    cp.heals[p] = (buf.get().toInt() and 0xFF).coerceIn(0, Upgrades.HEALS_PER_SHOP)
                    if (format >= 3) {
                        cp.perks[p] = buf.int
                        for (k in 0 until Perks.OFFERED) {
                            val id = buf.get().toInt()
                            cp.perkOffer[p][k] = if (id in 0 until Perks.count) id else -1
                        }
                    }
                    if (format >= 4) {
                        cp.character[p] = CharacterDef.fromId(buf.get().toInt())
                        if (format >= 5) cp.missionMask[p] = buf.int and CardMission.allMask
                        if (format >= 6) cp.appearance[p] = dev.david.tessel.core.content.TankLook.fromCode(buf.short.toInt() and 0xFFFF)
                        if(format >= 7) {
                            cp.evolution[p] = dev.david.tessel.core.content.TankEvolution.fromId(buf.get().toInt())
                            cp.lastEvolutionStage[p] = buf.short.toInt().coerceAtLeast(0)
                            for(u in 0 until upgrades) { val spent=buf.int.coerceAtLeast(0); if(u<Upgrades.count) cp.upgradeSpent[p][u]=spent }
                        } else for(u in 0 until Upgrades.count) cp.upgradeSpent[p][u]=Upgrades.of(u).basePrice*cp.levels[p][u]
                        cp.stockSaved[p] = buf.get().toInt() != 0
                        for (slot in 0 until slots) {
                            val locked = buf.get().toInt() != 0
                            val quote = buf.int
                            if (slot < SHOP_SLOTS && locked && quote > 0 && CardCode.valid(cp.stock[p][slot])) {
                                cp.locked[p][slot] = true; cp.quotes[p][slot] = quote
                            }
                        }
                    } else cp.stockSaved[p] = cp.stock[p].any { it >= 0 }
                    if(format>=8){
                        cp.skillNodes[p]=(buf.long and SkillGraph.MASK) or SkillGraph.fromLegacy(cp.perks[p])
                        cp.freeRerollsUsed[p]=(buf.get().toInt() and 255).coerceIn(0,if(format>=9)4 else 3)
                        if(format>=9)for(i in cp.rewardRounds[p].indices){val id=buf.get().toInt();cp.rewardRounds[p][i]=if(dev.david.tessel.core.content.RewardRounds.valid(id))id else -1}
                    }else{
                        cp.skillNodes[p]=SkillGraph.fromLegacy(cp.perks[p])
                        val free=minOf(cp.rerolls[p],SkillTree.freeRefreshes(cp.perks[p]))
                        cp.freeRerollsUsed[p]=free
                        cp.rerolls[p]-=free
                    }
                    // Old unclaimed milestone offers must never become mandatory on upgrade.
                    for(k in cp.perkOffer[p].indices)if(SkillTree.isNode(cp.perkOffer[p][k]))cp.perkOffer[p][k]=-1
                }
                cp
            } catch (_: BufferUnderflowException) {
                null
            }
        }
    }
}
