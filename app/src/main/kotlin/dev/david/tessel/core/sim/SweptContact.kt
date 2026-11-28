package dev.david.tessel.core.sim

import kotlin.math.sqrt

/** First contact along two linearly moving circles in one fixed step.
 * Infinity denotes no contact. Allocation-free; also handles stationary shots. */
internal fun contactTime(ax:Float,ay:Float,bx:Float,by:Float,cx:Float,cy:Float,dx:Float,dy:Float,r:Float):Float {
    val ox=ax-cx;val oy=ay-cy
    val vx=(bx-ax)-(dx-cx);val vy=(by-ay)-(dy-cy)
    val c=ox*ox+oy*oy-r*r
    if(c<=0f)return 0f
    val a=vx*vx+vy*vy
    if(a<=.000001f)return Float.POSITIVE_INFINITY
    val b=ox*vx+oy*vy
    if(b>=0f)return Float.POSITIVE_INFINITY
    val disc=b*b-a*c
    if(disc<0f)return Float.POSITIVE_INFINITY
    val t=(-b-sqrt(disc))/a
    return if(t in 0f..1f)t else Float.POSITIVE_INFINITY
}
