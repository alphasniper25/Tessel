package dev.david.tessel.core.sim

import dev.david.tessel.core.content.TankEvolution
import dev.david.tessel.core.content.Balance
import dev.david.tessel.core.content.CharacterDef
import dev.david.tessel.core.content.Elite
import dev.david.tessel.core.content.Perks
import dev.david.tessel.core.content.PerkCombo
import dev.david.tessel.core.content.Upgrades
import dev.david.tessel.core.content.UpgradeStat
import dev.david.tessel.core.content.SkillGraph
import dev.david.tessel.core.content.SkillBonus

/** Ticks without a hit before Overclock kicks in. */
const val OVERCLOCK_TICKS = 8 * TICK_HZ

/*
 * Entity storage as parallel arrays. Removal swaps the last entry into the
 * hole, so indices move between ticks; ids do not.
 */

class PlayerState {
    val x = FloatArray(MAX_PLAYERS)
    val y = FloatArray(MAX_PLAYERS)
    val prevX = FloatArray(MAX_PLAYERS)
    val prevY = FloatArray(MAX_PLAYERS)
    val aimX = FloatArray(MAX_PLAYERS)
    val aimY = FloatArray(MAX_PLAYERS)
    val hp = IntArray(MAX_PLAYERS)
    val maxHp = IntArray(MAX_PLAYERS)
    val evolution = Array(MAX_PLAYERS) { TankEvolution.BASIC }
    val shieldCooldown = IntArray(MAX_PLAYERS)
    val lastEvolutionStage = IntArray(MAX_PLAYERS)
    val upgradeSpent = Array(MAX_PLAYERS) { IntArray(Upgrades.count) }
    val character = Array(MAX_PLAYERS) { CharacterDef.BALANCED }
    val appearance = Array(MAX_PLAYERS) { dev.david.tessel.core.content.TankLook() }

    /** Ticks left during which contact does no damage. */
    val invuln = IntArray(MAX_PLAYERS)

    /** Ticks left of the red hit tint. */
    val hurt = IntArray(MAX_PLAYERS)

    /** Seconds until the next shot may leave. */
    val reload = FloatArray(MAX_PLAYERS)

    /** Ticks a partner has spent standing next to this downed player. */
    val revive = IntArray(MAX_PLAYERS)

    /** False for an empty seat, or for a player whose phone left the game. */
    val present = BooleanArray(MAX_PLAYERS)
    /** Coins in hand, spent in the shop. */
    val coins = IntArray(MAX_PLAYERS)

    /** Every coin picked up this run, spent or not. */
    val earned = IntArray(MAX_PLAYERS)
    val kills = IntArray(MAX_PLAYERS)

    /** Score points from kills; see Scoring. */
    val points = IntArray(MAX_PLAYERS)

    /** What took the last health, as a [Cause] code; -1 while standing. */
    val killedBy = IntArray(MAX_PLAYERS) { Cause.NONE }

    // For the run's statistics: bullets fired, damage dealt, hits taken, times downed, teammates revived.
    val fired = IntArray(MAX_PLAYERS)
    val dealt = FloatArray(MAX_PLAYERS)
    val taken = IntArray(MAX_PLAYERS)
    val downs = IntArray(MAX_PLAYERS)
    val revived = IntArray(MAX_PLAYERS)

    /** Shop upgrade levels, indexed by [Upgrades] id. */
    val levels = Array(MAX_PLAYERS) { IntArray(Upgrades.count) }
    val missionMask = IntArray(MAX_PLAYERS)

    fun alive(i: Int) = hp[i] > 0 && present[i]

    /** Upgrade levels bought so far, all kinds together. */
    fun bought(p: Int) = levels[p].sum()

    /** Perks taken, as a mask of [Perks] ids. */
    val perks = IntArray(MAX_PLAYERS)
    val rewardRounds=Array(MAX_PLAYERS){IntArray(dev.david.tessel.core.content.RewardRounds.COUNT){-1}}
    fun recordReward(p:Int,stage:Int,id:Int){val index=dev.david.tessel.core.content.RewardRounds.index(stage);if(index>=0&&dev.david.tessel.core.content.RewardRounds.valid(id))rewardRounds[p][index]=id}
    private val graphMasks=LongArray(MAX_PLAYERS)
    private val graphBonuses=Array(MAX_PLAYERS){IntArray(SkillBonus.entries.size)}
    fun skillMask(p:Int)=graphMasks[p]
    fun skill(p:Int,bonus:SkillBonus)=graphBonuses[p][bonus.ordinal]
    fun setSkills(p:Int,mask:Long){graphMasks[p]=mask and SkillGraph.MASK;SkillGraph.fill(graphMasks[p],graphBonuses[p])}
    private fun skillFactor(p:Int,bonus:SkillBonus)=1f+skill(p,bonus)*.01f

    /** Second Wind spent this stage. */
    val windUsed = BooleanArray(MAX_PLAYERS)

    /** Ticks since this player was last hit, for Overclock. */
    val sinceHit = IntArray(MAX_PLAYERS)

    /** Ticks left of a surge from an elite. */
    val surge = IntArray(MAX_PLAYERS)

    /** Ticks left of a stun: no moving, no shooting. */
    val stun = IntArray(MAX_PLAYERS)
    val poisonTicks = IntArray(MAX_PLAYERS)
    val poisonSource = IntArray(MAX_PLAYERS) { -1 }

    fun has(p: Int, perk: Int) = perks[p] shr perk and 1 != 0
    fun combo(p:Int,combo:PerkCombo)=combo.active(perks[p])
    fun capacitorReady(p:Int)=combo(p,PerkCombo.REACTOR_SYNC)&&sinceHit[p]>=OVERCLOCK_TICKS
    fun purgeDue(p:Int)=combo(p,PerkCombo.FIELD_RUNNER)&&kills[p]>0&&kills[p]%10==0
    fun clearStatus(p:Int){poisonTicks[p]=0;poisonSource[p]=-1;stun[p]=0}

    fun damage(p: Int): Float {
        var d = scaled(p, UpgradeStat.DAMAGE, Balance.BULLET_DAMAGE) * character[p].damageScale * (1f + Upgrades.delta(levels[p], UpgradeStat.DAMAGE_BONUS))
        if (has(p, Perks.HEAVY_ROUNDS)) d *= 1.3f
        d*=perkDamageFactor(p)
        if (has(p, Perks.LAST_STAND) && hp[p] in 1..2) d *= 1.5f
        if (surge[p] > 0) d *= Elite.SURGE_DAMAGE
        return d * evolution[p].damage * skillFactor(p,SkillBonus.DAMAGE) * if(evolution[p].drones) 1f + Upgrades.delta(levels[p],UpgradeStat.DRONE_POWER) else 1f
    }

    fun fireRate(p: Int): Float {
        var r = scaled(p, UpgradeStat.FIRE_RATE, Balance.FIRE_RATE) * character[p].fireScale
        if (has(p, Perks.OVERCLOCK) && sinceHit[p] >= OVERCLOCK_TICKS) r *= 1.3f
        if (has(p, Perks.HEAVY_ROUNDS)) r *= .85f
        if (surge[p] > 0) r *= Elite.SURGE_FIRE
        if(has(p,Perks.RAPID_CYCLE))r*=1.1f
        return r * evolution[p].rate * skillFactor(p,SkillBonus.FIRE_RATE)
    }
    fun volley(p: Int) = if (evolution[p] == TankEvolution.BASIC) 1 + Upgrades.delta(levels[p], UpgradeStat.VOLLEY).toInt() else evolution[p].barrels
    fun bulletSpeed(p: Int) = scaled(p, UpgradeStat.VELOCITY, Balance.BULLET_SPEED) * evolution[p].velocity
    fun pierce(p: Int) = evolution[p].extraPierce + Upgrades.delta(levels[p], UpgradeStat.PIERCE).toInt() + if (has(p, Perks.HEAVY_ROUNDS)) 1 else 0
    private fun perkDamageFactor(p:Int)=(if(has(p,Perks.HARDPOINT))1.1f else 1f)*(if(has(p,Perks.DEADEYE))1.12f else 1f)
    fun perkRangeFactor(p:Int)=(if(has(p,Perks.RANGEFINDER))1.1f else 1f)*(if(has(p,Perks.LONGSHOT))1.15f else 1f)
    fun range(p: Int) = scaled(p, UpgradeStat.RANGE, Balance.BULLET_RANGE) * evolution[p].reach * skillFactor(p,SkillBonus.RANGE)*perkRangeFactor(p)
    fun moveSpeed(p: Int) = scaled(p, UpgradeStat.SPEED, Balance.PLAYER_SPEED) * character[p].speedScale * evolution[p].move * (1f + Upgrades.delta(levels[p], UpgradeStat.SPEED_BONUS)) * skillFactor(p,SkillBonus.SPEED) * (if (has(p, Perks.QUICK_FEET)) 1.15f else 1f) * (if(has(p,Perks.AFTERBURNER))1.1f else 1f) * (if(combo(p,PerkCombo.HORIZON)&&hp[p]==maxHp[p])1.15f else 1f)
    fun ramDamage(p: Int) = (character[p].ramBase + Upgrades.delta(levels[p], UpgradeStat.RAM) + evolution[p].ram * (1f + Upgrades.delta(levels[p], UpgradeStat.DAMAGE) + .075f * Upgrades.delta(levels[p], UpgradeStat.RAM))) * skillFactor(p,SkillBonus.DAMAGE)*perkDamageFactor(p)
    fun regenSeconds(p: Int): Int {
        val a=character[p].regenSeconds; val b=evolution[p].regenSeconds
        val base=if(a==0) b else if(b==0) a else minOf(a,b)
        val level=Upgrades.delta(levels[p],UpgradeStat.REGEN).toInt()
        val normal=if(level==0) base else if(base==0) 28-4*level else (base-2*level).coerceAtLeast(if(evolution[p].melee) 2 else 4)
        return if(combo(p,PerkCombo.REACTIVE_PLATING)&&hp[p]>0&&hp[p]*2<=maxHp[p])if(normal==0)8 else minOf(normal,8) else normal
    }
    fun shieldTicks(p: Int) = (evolution[p].shieldSeconds*TICK_HZ / (1f+Upgrades.delta(levels[p],UpgradeStat.SHIELD_RECHARGE))).toInt().coerceAtLeast(15)
    fun wealth(p: Int) = Upgrades.delta(levels[p], UpgradeStat.WEALTH)
    fun magnet(p: Int) = scaled(p, UpgradeStat.MAGNET, Balance.MAGNET_RADIUS) * skillFactor(p,SkillBonus.PICKUP) * (if (has(p, Perks.MAGNET_CORE)) 2f else 1f) * (if(has(p,Perks.VACUUM))1.25f else 1f) * (if(combo(p,PerkCombo.SURVEYOR))1.2f else 1f)
    fun bulletRadius(p: Int) = scaled(p, UpgradeStat.CALIBER, Balance.BULLET_RADIUS)

    private fun scaled(p: Int, stat: UpgradeStat, base: Float) = base * (1f + Upgrades.delta(levels[p], stat))

    /** [bonusHp] comes from the difficulty: Easy starts with more health, Impossible with less. */
    fun reset(count: Int, bonusHp: Int = 0) {
        for (i in 0 until MAX_PLAYERS) {
            // A lone player starts dead centre, a team stands in a row around it.
            x[i] = ARENA_W / 2f + (i - (count - 1) / 2f) * 70f
            y[i] = ARENA_H / 2f
            prevX[i] = x[i]
            prevY[i] = y[i]
            aimX[i] = 1f
            aimY[i] = 0f
            character[i] = CharacterDef.BALANCED
            evolution[i] = TankEvolution.BASIC
            shieldCooldown[i] = 0
            lastEvolutionStage[i] = 0
            upgradeSpent[i].fill(0)
            appearance[i] = dev.david.tessel.core.content.TankLook()
            maxHp[i] = (Balance.PLAYER_HP + bonusHp).coerceAtLeast(1)
            hp[i] = if (i < count) maxHp[i] else 0
            invuln[i] = 0
            hurt[i] = 0
            reload[i] = 0f
            revive[i] = 0
            present[i] = i < count
            coins[i] = 0
            earned[i] = 0
            kills[i] = 0
            points[i] = 0
            killedBy[i] = Cause.NONE
            fired[i] = 0
            dealt[i] = 0f
            taken[i] = 0
            downs[i] = 0
            revived[i] = 0
            levels[i].fill(0)
            missionMask[i] = 0
            perks[i] = 0
            rewardRounds[i].fill(-1)
            setSkills(i,0L)
            windUsed[i] = false
            sinceHit[i] = OVERCLOCK_TICKS
            surge[i] = 0
            stun[i] = 0
            poisonTicks[i] = 0
            poisonSource[i] = -1
        }
    }

    /** New-run-only character selection. */
    fun chooseCharacter(p: Int, def: CharacterDef, bonusHp: Int) {
        require(p in 0 until MAX_PLAYERS && present[p])
        character[p] = def
        maxHp[p] = (Balance.PLAYER_HP + bonusHp + def.extraHp).coerceAtLeast(1)
        hp[p] = maxHp[p]
    }

    fun remember() {
        x.copyInto(prevX)
        y.copyInto(prevY)
    }
}

class BulletPool(private val capacity: Int) {
    val id = IntArray(capacity)
    val x = FloatArray(capacity)
    val y = FloatArray(capacity)
    val prevX = FloatArray(capacity)
    val prevY = FloatArray(capacity)
    val vx = FloatArray(capacity)
    val vy = FloatArray(capacity)
    val life = IntArray(capacity)
    val damage = FloatArray(capacity)
    val radius = FloatArray(capacity)
    val owner = IntArray(capacity)

    /** For enemy shots: the type of enemy that fired it, or -1. */
    val source = IntArray(capacity)

    /** Enemies it may still pass through. */
    val pierce = IntArray(capacity)

    /** Id of the enemy it last went through, so a piercing bullet does not hit the same one twice. */
    val lastHit = IntArray(capacity)
    private val hitCount = IntArray(capacity)
    private val hitHistory = IntArray(capacity * 64)
    fun hasHit(i:Int, enemy:Int):Boolean { if(lastHit[i]==enemy)return true; for(k in 0 until hitCount[i])if(hitHistory[i*64+k]==enemy)return true;return false }
    fun recordHit(i:Int,enemy:Int){lastHit[i]=enemy;val n=hitCount[i];if(n<64){hitHistory[i*64+n]=enemy;hitCount[i]=n+1}}

    var count = 0
        private set

    fun add(id: Int, x: Float, y: Float, vx: Float, vy: Float, life: Int, damage: Float, radius: Float, pierce: Int, owner: Int): Int {
        if (count == capacity) return -1
        val i = count++
        this.id[i] = id
        this.x[i] = x
        this.y[i] = y
        prevX[i] = x
        prevY[i] = y
        this.vx[i] = vx
        this.vy[i] = vy
        this.life[i] = life
        this.damage[i] = damage
        this.radius[i] = radius
        this.pierce[i] = pierce
        lastHit[i] = -1
        hitCount[i]=0
        this.owner[i] = owner
        source[i] = -1
        return i
    }

    fun remember() {
        x.copyInto(prevX, endIndex = count)
        y.copyInto(prevY, endIndex = count)
    }

    /** Drops every bullet whose life ran out. */
    fun sweep() {
        for (i in count - 1 downTo 0) if (life[i] <= 0) removeAt(i)
    }

    fun clear() {
        count = 0
    }

    private fun removeAt(i: Int) {
        val last = --count
        if (i == last) return
        id[i] = id[last]
        x[i] = x[last]
        y[i] = y[last]
        prevX[i] = prevX[last]
        prevY[i] = prevY[last]
        vx[i] = vx[last]
        vy[i] = vy[last]
        life[i] = life[last]
        damage[i] = damage[last]
        radius[i] = radius[last]
        pierce[i] = pierce[last]
        lastHit[i] = lastHit[last]
        hitCount[i]=hitCount[last]
        hitHistory.copyInto(hitHistory,i*64,last*64,last*64+hitCount[last])
        owner[i] = owner[last]
        source[i] = source[last]
    }
}

class EnemyPool(private val capacity: Int) {
    val id = IntArray(capacity)
    val type = IntArray(capacity)
    val x = FloatArray(capacity)
    val y = FloatArray(capacity)
    val prevX = FloatArray(capacity)
    val prevY = FloatArray(capacity)

    /** Knockback velocity, decays every tick. */
    val kx = FloatArray(capacity)
    val ky = FloatArray(capacity)
    val hp = FloatArray(capacity)

    /** Ticks left of the white hit flash. */
    val flash = IntArray(capacity)

    /** Facing in radians; only shapes that are not round use it. */
    val angle = FloatArray(capacity)
    val maxHp = FloatArray(capacity)

    /** Own velocity, for things that keep a heading (bouncers, chargers) or a target point (parkers). */
    val vx = FloatArray(capacity)
    val vy = FloatArray(capacity)

    /** Ticks until the next attack. */
    val timer = IntArray(capacity)

    /** Step in a routine (charging, parked, a boss phase) and ticks spent in it. */
    val mode = IntArray(capacity)
    val modeTicks = IntArray(capacity)

    /** Free per-routine value: orbit direction, spiral angle, children released. */
    val aux = FloatArray(capacity)

    /** 0..1 build-up before an attack, drawn so nothing fires without warning. */
    val cue = FloatArray(capacity)

    /** An [Elite] trait, or 0. */
    val elite = IntArray(capacity)

    var count = 0
        private set

    fun add(id: Int, type: Int, x: Float, y: Float, hp: Float): Int {
        if (count == capacity) return -1
        val i = count++
        this.id[i] = id
        this.type[i] = type
        this.x[i] = x
        this.y[i] = y
        prevX[i] = x
        prevY[i] = y
        kx[i] = 0f
        ky[i] = 0f
        this.hp[i] = hp
        maxHp[i] = hp
        flash[i] = 0
        angle[i] = 0f
        vx[i] = 0f
        vy[i] = 0f
        timer[i] = 0
        mode[i] = 0
        modeTicks[i] = 0
        aux[i] = 0f
        cue[i] = 0f
        elite[i] = 0
        return i
    }

    fun remember() {
        x.copyInto(prevX, endIndex = count)
        y.copyInto(prevY, endIndex = count)
    }

    fun sweep() {
        for (i in count - 1 downTo 0) if (hp[i] <= 0f) removeAt(i)
    }

    fun clear() {
        count = 0
    }

    private fun removeAt(i: Int) {
        val last = --count
        if (i == last) return
        id[i] = id[last]
        type[i] = type[last]
        x[i] = x[last]
        y[i] = y[last]
        prevX[i] = prevX[last]
        prevY[i] = prevY[last]
        kx[i] = kx[last]
        ky[i] = ky[last]
        hp[i] = hp[last]
        maxHp[i] = maxHp[last]
        flash[i] = flash[last]
        angle[i] = angle[last]
        vx[i] = vx[last]
        vy[i] = vy[last]
        timer[i] = timer[last]
        mode[i] = mode[last]
        modeTicks[i] = modeTicks[last]
        aux[i] = aux[last]
        cue[i] = cue[last]
        elite[i] = elite[last]
    }
}

class CoinPool(private val capacity: Int) {
    val id = IntArray(capacity)
    val x = FloatArray(capacity)
    val y = FloatArray(capacity)
    val prevX = FloatArray(capacity)
    val prevY = FloatArray(capacity)
    val vx = FloatArray(capacity)
    val vy = FloatArray(capacity)
    val value = IntArray(capacity)

    /** Player the coin is flying to, or -1 while it lies on the floor. */
    val target = IntArray(capacity)

    var count = 0
        private set

    fun add(id: Int, x: Float, y: Float, vx: Float, vy: Float, value: Int): Int {
        if (count == capacity) return -1
        val i = count++
        this.id[i] = id
        this.x[i] = x
        this.y[i] = y
        prevX[i] = x
        prevY[i] = y
        this.vx[i] = vx
        this.vy[i] = vy
        this.value[i] = value
        target[i] = -1
        return i
    }

    fun remember() {
        x.copyInto(prevX, endIndex = count)
        y.copyInto(prevY, endIndex = count)
    }

    fun sweep() {
        for (i in count - 1 downTo 0) if (value[i] <= 0) removeAt(i)
    }

    fun clear() {
        count = 0
    }

    private fun removeAt(i: Int) {
        val last = --count
        if (i == last) return
        id[i] = id[last]
        x[i] = x[last]
        y[i] = y[last]
        prevX[i] = prevX[last]
        prevY[i] = prevY[last]
        vx[i] = vx[last]
        vy[i] = vy[last]
        value[i] = value[last]
        target[i] = target[last]
    }
}
