package dev.david.tessel.platform.game

import kotlin.math.min
import kotlin.math.max

/** Shared safe geometry for local presentation. Physics never depends on screen dimensions. */
object ScreenFit {
    data class Bounds(val left:Float,val top:Float,val right:Float,val bottom:Float){
        val width get()=(right-left).coerceAtLeast(1f)
        val height get()=(bottom-top).coerceAtLeast(1f)
    }
    fun safe(width:Int,height:Int,left:Int=0,top:Int=0,right:Int=0,bottom:Int=0):Bounds{
        val w=width.coerceAtLeast(1).toFloat();val h=height.coerceAtLeast(1).toFloat()
        val x0=left.coerceAtLeast(0).toFloat().coerceAtMost(w-1f)
        val y0=top.coerceAtLeast(0).toFloat().coerceAtMost(h-1f)
        return Bounds(x0,y0,(w-right.coerceAtLeast(0)).coerceIn(x0+1f,w),(h-bottom.coerceAtLeast(0)).coerceIn(y0+1f,h))
    }
    fun centered(value:Float,extent:Float,low:Float,high:Float):Float{
        val a=low+extent;val b=high-extent
        return if(a<=b)value.coerceIn(a,b) else (low+high)*.5f
    }
    fun stickRadius(preferred:Float,bounds:Bounds)=min(preferred,min(bounds.width*.17f,bounds.height*.29f)).coerceAtLeast(1f)
    fun textScale(measured:Float,available:Float)=if(measured<=0f)1f else min(1f,max(0f,available)/measured)
    fun hudCenterWidth(bounds:Bounds,density:Float)=min(190f*density,bounds.width*.30f).coerceAtLeast(1f)
}
