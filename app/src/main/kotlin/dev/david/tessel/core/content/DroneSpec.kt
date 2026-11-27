package dev.david.tessel.core.content
/** Trial balance templates. All caps count actual living units, not emitters. */
data class DroneSpec(val cap:Int,val hp:Int,val respawn:Float,val speed:Float,val reach:Float,val orbit:Float,val contact:Float,val impact:Float)
private val overseer=DroneSpec(4,3,1.1f,270f,300f,65f,.55f,3f)
private val overlord=DroneSpec(6,4,1.05f,290f,340f,78f,.48f,4f)
private val swarm=DroneSpec(8,2,.75f,350f,320f,90f,.44f,5f)
private val mothership=DroneSpec(8,6,.68f,320f,400f,98f,.38f,5f)
private val dreadnought=DroneSpec(8,7,.55f,345f,440f,106f,.35f,6f)
val TankEvolution.droneSpec:DroneSpec? get()=when(this){TankEvolution.OVERSEER->overseer;TankEvolution.OVERLORD->overlord;TankEvolution.SWARM->swarm;TankEvolution.MOTHERSHIP->mothership;TankEvolution.DREADNOUGHT->dreadnought;else->null}
