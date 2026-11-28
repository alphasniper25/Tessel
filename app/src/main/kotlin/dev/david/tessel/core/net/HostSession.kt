package dev.david.tessel.core.net

import dev.david.tessel.core.sim.Events
import dev.david.tessel.core.sim.Phase
import dev.david.tessel.core.sim.MAX_PLAYERS
import dev.david.tessel.core.sim.PlayerInput
import dev.david.tessel.core.sim.World

/** The host is always player 0; phones that joined are 1 and up, in the order the lobby gave them. */
const val HOST_PLAYER = 0

/**
 * The host's side of a co-op game. The host runs the only real simulation;
 * this feeds it the other players' input and what their phones saw them hit
 * or get hit by, applies their shop choices and sends each of them what to
 * draw, 30 times a second.
 *
 * Each other phone says where its player is, every frame. Those reports
 * arrive unevenly, so the host plays them back a little late, at an even
 * pace, the same way the other phones draw the host's world.
 *
 * [send] returns false when a latest-state message was skipped because the
 * previous one to that player is still on its way; the events it carried are
 * kept for the next.
 */
class HostSession(private val world: World, private val send: (player: Int, bytes: ByteArray, latest: Boolean) -> Boolean) {
    private val remote = Array(MAX_PLAYERS) { PlayerInput() }
    private val quiet = PlayerInput()
    private val lastInputNanos = LongArray(MAX_PLAYERS)
    private val nextSendNanos = LongArray(MAX_PLAYERS)
    private val outbox = Array(MAX_PLAYERS) { Events(512) }
    private val born = Array(MAX_PLAYERS) { Events(SNAPSHOT_SHOTS) }
    private val gone = Array(MAX_PLAYERS) { Events(SNAPSHOT_SHOTS) }
    private val reports = Reports()
    private val echoIn = Echo()
    private val echoOut = Echo()

    // Per player: their last input number and clock reading, when it came, and the measured round trip.
    private val lastSeq = IntArray(MAX_PLAYERS)
    private val lastStamp = IntArray(MAX_PLAYERS)
    private val stampAt = IntArray(MAX_PLAYERS)
    private val roundTrip = FloatArray(MAX_PLAYERS) { -1f }
    private val buf = Wire.buffer(MAX_MESSAGE * 2)
    private val packetEncoder = SnapshotPacket.Encoder()
    private val lastState = IntArray(MAX_PLAYERS) { -1 }
    private var shopRevision = 0
    private val sentShopRevision = IntArray(MAX_PLAYERS) { -1 }

    // Per player: where their phone said they were, by that phone's clock, played back smoothly.
    private val trail = Array(MAX_PLAYERS) { Trail() }
    private val smoothed = Array(MAX_PLAYERS) { PlayerInput() }

    /** Bit p set once player p pressed Ready in the shop. */
    val rematchMasks = IntArray(MAX_PLAYERS)

    var ready = 0
        private set

    /** Bit p set while player p's phone is linked. The host's own bit is always set. */
    var linked = 1
        private set

    /** Pause, resume and restart asks from other players; the game loop decides what they mean. */
    var onRequest: ((Int) -> Unit)? = null
    var onShopChanged: (() -> Unit)? = null

    fun connect(player: Int) {
        linked = linked or (1 shl player)
        lastSeq[player] = 0
        lastStamp[player] = 0
        roundTrip[player] = -1f
        outbox[player].clear()
        born[player].clear()
        gone[player].clear()
        nextSendNanos[player] = 0L
        lastState[player] = -1
        sentShopRevision[player] = -1
        slow[player] = false
        trail[player].reset()
    }

    fun disconnect(player: Int) {
        linked = linked and (1 shl player).inv()
        ready = ready and (1 shl player).inv()
    }

    fun receive(player: Int, bytes: ByteArray, nowNanos: Long) {
        if (player !in 1 until world.playerCount) return
        val type = Wire.typeOf(bytes)
        if (type != Wire.INPUT && type != Wire.ACTION) return
        val buf = Wire.open(bytes)
        when (type) {
            Wire.INPUT -> {
                Protocol.readInput(buf, remote[player], reports, echoIn)
                lastInputNanos[player] = nowNanos
                trail[player].add(echoIn.stamp, remote[player].posX, remote[player].posY, nowNanos)
                val now = millis(nowNanos)
                lastSeq[player] = echoIn.seq
                lastStamp[player] = echoIn.stamp
                stampAt[player] = now
                val sample = echoIn.roundTrip(now)
                if (sample >= 0) roundTrip[player] = smooth(roundTrip[player], sample)
                for (k in 0 until reports.pickups) world.remotePickup(player, reports.pickupId[k])
                for (k in 0 until reports.hits) world.remoteHit(player, reports.hitId[k], reports.hitX[k], reports.hitY[k])
                for (k in 0 until reports.hurts) world.remoteHurt(player, reports.hurtBy[k])
            }
            Wire.ACTION -> {
                if (buf.remaining() < 2) return
                val kind = buf.get().toInt()
                val arg = buf.get().toInt()
                if (isShopAction(kind) &&
                    (world.phase != Phase.SHOP || ready and (1 shl player) != 0 || linked and (1 shl player)==0 || !world.players.present[player])) return
                val changed = when (kind) {
                    Wire.LOCK_CARD -> world.shop.setLocked(world, player, arg, true)
                    Wire.UNLOCK_CARD -> world.shop.setLocked(world, player, arg, false)
                    Wire.BUY -> world.shop.buy(world, player, arg)
                    Wire.BUY_SKILL -> world.shop.buySkill(world,player,arg)
                    Wire.HEAL -> world.shop.heal(world, player)
                    Wire.REROLL -> world.shop.reroll(world, player)
                    Wire.READY -> { markReady(player); false }
                    Wire.RESTART -> { markRematchReady(player, arg and 255); false }
                    Wire.PERK -> world.shop.pickPerk(world, player, arg)
                    else -> { onRequest?.invoke(kind); false }
                }
                if (changed) { shopRevision++; onShopChanged?.invoke() }
            }
        }
    }

    /**
     * A player's input for this tick, with their position played back at an
     * even pace. If their phone has gone quiet, they stand still and stop firing.
     */
    fun remoteInput(player: Int, nowNanos: Long): PlayerInput {
        if (nowNanos - lastInputNanos[player] > STALE_NANOS) {
            quiet.clear()
            return quiet
        }
        val out = smoothed[player]
        out.copyFrom(remote[player])
        trail[player].play(nowNanos, out)
        return out
    }

    /** How far behind [player]'s own reports the host plays them back, in milliseconds. */
    fun playbackMillis(player: Int): Int = (trail[player].bufferNanos / 1_000_000L).toInt()

    /** How unevenly [player]'s reports arrive: the late ones are this late, in milliseconds. */
    fun spreadMillis(player: Int): Int = (trail[player].jitter.spreadNanos / 1_000_000L).toInt()

    fun markReady(player: Int) {
        // A perk on offer has to be taken first. Ignore out-of-phase or stale shop actions.
        if (world.phase != Phase.SHOP || player !in 0 until world.playerCount || !world.players.present[player] || linked and (1 shl player) == 0 || world.shop.perkPending(player)) return
        ready = ready or (1 shl player)
    }

    /** The same ready mask is used on the game-over screen, but only a distinct action can set it. */
    fun markRematchReady(player: Int, unlockMask: Int) {
        if (world.phase != Phase.OVER || player !in 0 until world.playerCount || !world.players.present[player] || linked and (1 shl player) == 0) return
        rematchMasks[player] = unlockMask and dev.david.tessel.core.content.CardMission.allMask
        ready = ready or (1 shl player)
    }

    fun allRematchReady(): Boolean {
        if (world.phase != Phase.OVER) return false
        var needed = 0
        for (p in 0 until world.playerCount) if (world.players.present[p]) needed = needed or (1 shl p)
        return Integer.bitCount(needed) >= 2 && linked and needed == needed && ready and needed == needed
    }

    /** Everyone still in the game has pressed Ready. */
    fun allReady(): Boolean {
        var needed = 0
        for (p in 0 until world.playerCount) if (world.players.present[p]) needed = needed or (1 shl p)
        return ready and needed == needed
    }

    fun readyCount() = Integer.bitCount(ready)

    fun clearReady() {
        ready = 0
        rematchMasks.fill(0)
    }

    /** Keeps the events the other phones need; call before the world's events are cleared. */
    fun collect(events: Events) {
        for (i in 0 until events.count) {
            val kind = events.kind[i]
            // Coin pickups need no show, and hit flashes travel with each enemy.
            if (kind == Events.COIN || kind == Events.ENEMY_HIT) continue
            for (p in 1 until world.playerCount) {
                if (linked and (1 shl p) == 0) continue
                // A phone flies its own shots already.
                if (kind == Events.SHOT && events.who[i] == p) continue
                val to = when (kind) {
                    Events.HOSTILE_SPAWN -> born[p]
                    Events.HOSTILE_GONE -> gone[p]
                    else -> outbox[p]
                }
                to.add(kind, events.who[i], events.x[i], events.y[i], events.dx[i], events.dy[i], events.angle[i], events.ref[i])
            }
        }
    }

    /** Sends each linked phone a snapshot if one is due. Keeps sending while paused or shopping so menus stay current. */
    fun pump(nowNanos: Long, paused: Boolean, waiting: Boolean, breaking: Boolean = false) {
        for (p in 1 until world.playerCount) {
            if (linked and (1 shl p) == 0) continue
            // Menus need fewer updates, but phase/ready changes and purchases are sent immediately.
            val idle = paused || waiting || world.phase == Phase.SHOP || world.phase == Phase.OVER
            val state = (world.stage shl 16) or (ready shl 8) or (world.phase.ordinal shl 3) or
                (if (paused) 1 else 0) or (if (waiting) 2 else 0) or (if (breaking) 4 else 0)
            val urgent = state != lastState[p] || (world.phase == Phase.SHOP && shopRevision != sentShopRevision[p])
            if (!urgent && nowNanos < nextSendNanos[p] - SEND_SLACK_NANOS) continue
            val now = millis(nowNanos)
            echoOut.seq = lastSeq[p]
            echoOut.stamp = now
            echoOut.echo = lastStamp[p]
            echoOut.held = if (lastStamp[p] == 0) 0 else now - stampAt[p]
            Protocol.writeSnapshot(buf, world, paused, waiting, ready, outbox[p], born[p], gone[p], p, echoOut, breaking)
            if (send(p, packetEncoder.encode(buf), true)) {
                lastState[p] = state
                sentShopRevision[p] = shopRevision
                outbox[p].clear()
                born[p].clear()
                gone[p].clear()
                val step = SNAPSHOT_NANOS * if (idle) 3 else beatsFor(p)
                val due = if (urgent) nowNanos + step else nextSendNanos[p] + step
                nextSendNanos[p] = if (due < nowNanos) nowNanos + step else due
            }
        }
    }

    /** Round trip to [player]'s phone in milliseconds, smoothed, or -1 before it is known. */
    fun roundTripMillis(player: Int): Int = roundTrip[player].toInt()

    private val slow = BooleanArray(MAX_PLAYERS)

    /**
     * A link that is far behind gets every other snapshot, which halves what it has to carry, and
     * goes back to the full rate once it recovers. The gap between the two limits stops it flapping.
     */
    private fun beatsFor(p: Int): Int {
        val rtt = roundTrip[p]
        if (rtt > SLOW_RTT_MS) slow[p] = true else if (rtt in 0f..RECOVER_RTT_MS) slow[p] = false
        return if (slow[p]) 2 else 1
    }

    /** Whether the link to [player] is being sent half the snapshots. */
    fun isThrottled(player: Int) = slow[player]

    /** Tells every other phone a fresh run has begun, and which player it is. */
    fun announceStart() {
        for (p in 1 until world.playerCount) {
            outbox[p].clear()
            born[p].clear()
            gone[p].clear()
            nextSendNanos[p] = 0L
            lastState[p] = -1
            sentShopRevision[p] = -1
            trail[p].reset()
            if (linked and (1 shl p) != 0) send(p, Wire.start(world.difficulty.ordinal, p, world.playerCount), false)
        }
        clearReady()
    }

    fun close() = packetEncoder.close()

    private fun isShopAction(kind: Int) = when (kind) {
        Wire.BUY, Wire.HEAL, Wire.REROLL, Wire.PERK, Wire.LOCK_CARD, Wire.UNLOCK_CARD, Wire.BUY_SKILL -> true
        else -> false
    }

    companion object {
        /** A clock reading in milliseconds; never 0, which means "none yet" on the wire. */
        fun millis(nanos: Long): Int = (nanos / 1_000_000L).toInt().let { if (it == 0) 1 else it }

        /** Follows a measured round trip closely, but not every single bump. */
        fun smooth(old: Float, sample: Int): Float = if (old < 0f) sample.toFloat() else old + (sample - old) * 0.2f

        /** 30 snapshots a second: small enough now that bullets do not travel. */
        const val SNAPSHOT_NANOS = 1_000_000_000L / 30
        const val SLOW_RTT_MS = 220f
        const val RECOVER_RTT_MS = 130f
        const val SEND_SLACK_NANOS = 4_000_000L
        const val STALE_NANOS = 400_000_000L
    }
}
