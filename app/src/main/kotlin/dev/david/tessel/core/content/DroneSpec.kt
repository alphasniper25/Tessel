package dev.david.tessel.core.content
/** Trial balance templates. All caps count actual living units, not emitters. */
data class DroneSpec(val cap:Int,val hp:Int,val respawn:Float,val speed:Float,val reach:Float,val orbit:Float,val contact:Float,val impact:Float)
private val overseer=DroneSpec(4,3,.98f,285f,315f,65f,.52f,3.4f)
private val overlord=DroneSpec(6,4,.93f,305f,355f,78f,.46f,4.5f)
private val swarm=DroneSpec(8,2,.70f,365f,335f,90f,.42f,5.5f)
private val mothership=DroneSpec(8,6,.64f,335f,410f,98f,.36f,5.5f)
private val dreadnought=DroneSpec(8,7,.55f,355f,450f,106f,.35f,6.6f)
val TankEvolution.droneSpec:DroneSpec? get()=when(this){TankEvolution.OVERSEER->overseer;TankEvolution.OVERLORD->overlord;TankEvolution.SWARM->swarm;TankEvolution.MOTHERSHIP->mothership;TankEvolution.DREADNOUGHT->dreadnought;else->null}
