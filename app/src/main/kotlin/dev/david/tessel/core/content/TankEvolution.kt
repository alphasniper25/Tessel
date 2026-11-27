package dev.david.tessel.core.content

import kotlin.math.*

/** Stable IDs (ordinal); append only. All weapon geometry is also consumed by the client and renderer.
 * damage is per projectile; barrels are bounded, so extra barrels are not a free DPS multiplier. */
enum class TankEvolution(
    val label: String, val detail: String, val parent: Int, val tier: Int,
    val barrels: Int = 1, val damage: Float = 1f, val rate: Float = 1f,
    val spread: Float = .12f, val reach: Float = 1f, val velocity: Float = 1f,
    val move: Float = 1f, val extraPierce: Int = 0, val ram: Float = 0f,
    val shieldSeconds: Float = 0f, val regenSeconds: Int = 0,
    val drones: Boolean = false, val length: Float = 1.3f, val lateral: Float = .32f,
    val secret: Boolean = false, val laser: Boolean = false,
) {
    BASIC("Basic", "Single standard barrel", -1, 0),
    TWIN("Twin", "Two balanced forward barrels", 0, 1, 2, .61f, 1f),
    MACHINE("Machine Gun", "Fast, light shots", 0, 1, 1, .60f, 2.1f, length=1.1f),
    SNIPER("Sniper", "Long-range piercing shots", 0, 1, 1, 2.15f, .58f, reach=1.55f, velocity=1.4f, extraPierce=1, length=2f),
    RAMMER("Rammer", "Contact damage; rechargeable impact shield", 0, 1, 0, ram=48f, shieldSeconds=.65f, regenSeconds=6, move=1.15f),
    TRIPLET("Triplet", "Three focused barrels", 1, 2, 3, .50f, 1.05f, spread=.07f),
    QUAD("Quad", "Four wider barrels", 1, 2, 4, .45f, .95f, spread=.25f),
    MINIGUN("Minigun", "High sustained fire", 2, 2, 1, .57f, 2.75f, length=1.5f),
    DESTROYER("Destroyer", "Heavy cannon; slow reload", 2, 2, 1, 5.2f, .29f, reach=1.15f, extraPierce=2, length=1.1f),
    ASSASSIN("Assassin", "Precision and long penetration", 3, 2, 1, 3.2f, .48f, reach=1.8f, velocity=1.55f, extraPierce=2, length=2.3f),
    OVERSEER("Overseer", "Two escort drones; aim or auto-target", 3, 2, 2, .80f, 1f, drones=true, lateral=1.9f),
    SMASHER("Smasher", "Fast shielded contact attacks", 4, 2, 0, ram=66f, shieldSeconds=.50f, regenSeconds=5, move=1.25f),
    FORTRESS("Fortress", "Regeneration and impact defense", 4, 2, 0, ram=72f, shieldSeconds=.60f, regenSeconds=4, move=1.08f),
    FOCUS("Focus", "Three concentrated barrels", 5, 3, 3, .63f, 1.05f, spread=.035f, extraPierce=1),
    FAN("Fan", "Three broad coverage barrels", 5, 3, 3, .65f, 1.03f, spread=.27f),
    CROSSFIRE("Crossfire", "Four directional shots", 6, 3, 4, .58f, 1.05f, spread=.62f),
    QUAD_GUARD("Quad Guard", "Four tight barrels", 6, 3, 4, .52f, 1.05f, spread=.095f),
    GATLING("Gatling", "Two fast, light barrels", 7, 3, 2, .37f, 2.75f, spread=.10f),
    SUPPRESSOR("Suppressor", "Rapid, penetrating stream", 7, 3, 1, .66f, 3.0f, extraPierce=1, length=1.7f),
    ANNIHILATOR("Annihilator", "Massive cannon", 8, 3, 1, 7.8f, .25f, extraPierce=3, length=1.2f),
    ARTILLERY("Artillery", "Twin heavy cannons", 8, 3, 2, 3.25f, .32f, spread=.18f, reach=1.3f, extraPierce=2),
    RANGER("Ranger", "Extreme-range precise fire", 9, 3, 1, 4.1f, .48f, reach=2.15f, velocity=1.7f, extraPierce=3, length=2.5f),
    MARKSMAN("Marksman", "Faster precision shots", 9, 3, 1, 2.8f, .72f, reach=1.65f, velocity=1.5f, extraPierce=2, length=2f),
    OVERLORD("Overlord", "Four escort drones", 10, 3, 4, .50f, 1.05f, drones=true, lateral=1.7f),
    SWARM("Swarm", "Three rapid escort drones", 10, 3, 3, .45f, 1.45f, drones=true, lateral=1.85f),
    SPIKE("Spike", "Powerful impacts", 11, 3, 0, ram=90f, shieldSeconds=.45f, regenSeconds=5, move=1.27f),
    BLITZ("Blitz", "Quick shield recharge", 11, 3, 0, ram=75f, shieldSeconds=.35f, regenSeconds=5, move=1.35f),
    LANDMINE("Landmine", "Heavy impact, strong recovery", 12, 3, 0, ram=105f, shieldSeconds=.55f, regenSeconds=4, move=1.10f),
    GUARDIAN("Guardian", "Rapid recovery, reliable shield", 12, 3, 0, ram=82f, shieldSeconds=.40f, regenSeconds=4, move=1.10f),
    MOTHERSHIP("Mothership", "Eight independently aiming escort emitters", 23, 4, 8, .36f, 1.12f, drones=true, lateral=2.2f),
    PRISM("Prism Laser", "Secret: high-speed piercing light pulses", 0, 4, 1, 6.8f, .85f, reach=3.2f, velocity=6f, extraPierce=48, length=2.6f, secret=true, laser=true),
    DREADNOUGHT("Dreadnought", "Secret: ten escort emitters with distributed targeting", 0, 4, 10, .45f, 1.25f, reach=1.35f, drones=true, lateral=2.6f, secret=true),
    TEMPEST("Tempest", "Secret: eight barrels in a broad fan", 0, 4, 8, .69f, 1.65f, spread=.14f, lateral=.36f, secret=true);

    val melee get() = barrels == 0
    val hullBonus get() = if(melee) tier*2+2 else 0
    fun angle(aim: Float, k: Int, n: Int) = aim + (k-(n-1)/2f)*spread
    fun offset(k: Int, n: Int) = (k-(n-1)/2f)*lateral*Balance.PLAYER_RADIUS
    fun muzzle() = Balance.PLAYER_RADIUS * if (drones) lateral else length
    fun originAngle(aim: Float, k: Int, n: Int) = if(drones) aim + k * (2f * PI.toFloat() / n) else angle(aim,k,n)
    inline fun droneAim(aim: Float, ox: Float, oy: Float, range: Float, count: Int,
        x: (Int)->Float, y: (Int)->Float, eligible:(Int)->Boolean): Float {
        if(!drones) return aim
        val ax=cos(aim); val ay=sin(aim); var best=range*range; var target=-1
        for(j in 0 until count){ if(!eligible(j))continue; val dx=x(j)-ox; val dy=y(j)-oy; val d2=dx*dx+dy*dy
            if(d2<best && (dx*ax+dy*ay)>-.15f*sqrt(d2)){best=d2;target=j}
        }
        return if(target<0) aim else atan2(y(target)-oy,x(target)-ox)
    }
    companion object {
        const val REWARD_BASE = 32
        const val REWARD_SLOTS = 5
        const val STAY_BASIC = 31
        const val SECRET_STAGE = 40
        private val children = Array(entries.size) { id -> entries.filter { it.parent == id }.toTypedArray() }
        fun fromId(id: Int) = entries.getOrElse(id) { BASIC }
        fun options(frame: TankEvolution, stage: Int = 0): Array<TankEvolution> = when {
            frame==BASIC && stage>=SECRET_STAGE -> children[0].filter { it.secret }.toTypedArray()
            else -> children[frame.ordinal].filter { !it.secret }.toTypedArray()
        }
        fun targetTier(stage: Int) = when { stage >= 35 -> 4; stage >= 25 -> 3; stage >= 15 -> 2; stage >= 5 -> 1; else -> 0 }
        fun fromReward(id: Int) = entries.getOrNull(id - REWARD_BASE)?.takeIf { it != BASIC }
        fun validReward(id: Int) = id == STAY_BASIC || id in 0 until Perks.count || fromReward(id) != null
        fun rewardName(id: Int) = if(id==STAY_BASIC) "Stay Basic" else fromReward(id)?.label ?: Perks.of(id).name
        fun rewardDetail(id: Int) = if(id==STAY_BASIC) "Skip evolution; clear stage 40 as Basic for secret frames." else fromReward(id)?.detail ?: Perks.of(id).blurb
    }
}
