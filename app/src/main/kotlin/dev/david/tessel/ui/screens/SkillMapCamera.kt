package dev.david.tessel.ui.screens

import dev.david.tessel.core.content.SkillGraph
import kotlin.math.*

/** Pure coordinate transform shared by touch hit testing, rendering and regression tests. */
data class SkillMapCamera(val zoom:Float=1f,val x:Float=0f,val y:Float=0f) {
    companion object {
        const val MIN_ZOOM=1f
        const val MAX_ZOOM=5.5f
        fun fit(w:Float,h:Float)=min(w/SkillGraph.WIDTH,h/SkillGraph.HEIGHT).coerceAtLeast(.001f)
    }
    fun scale(w:Float,h:Float)=fit(w,h)*zoom
    fun sx(world:Float,w:Float,h:Float)=w*.5f+x+world*scale(w,h)
    fun sy(world:Float,w:Float,h:Float)=h*.5f+y+world*scale(w,h)
    fun gesture(w:Float,h:Float,cx:Float,cy:Float,dx:Float,dy:Float,factor:Float):SkillMapCamera {
        val z=(zoom*factor).coerceIn(MIN_ZOOM,MAX_ZOOM)
        val ratio=z/zoom
        val s=fit(w,h)*z
        val boundX=max(0f,(SkillGraph.WIDTH*s-w)*.5f)+w*.18f
        val boundY=max(0f,(SkillGraph.HEIGHT*s-h)*.5f)+h*.18f
        val px=(cx-w*.5f-(cx-w*.5f-x)*ratio+dx).coerceIn(-boundX,boundX)
        val py=(cy-h*.5f-(cy-h*.5f-y)*ratio+dy).coerceIn(-boundY,boundY)
        return SkillMapCamera(z,px,py)
    }
    fun hit(w:Float,h:Float,px:Float,py:Float,minRadius:Float):Int? {
        var best=max(minRadius,26f*scale(w,h))
        best*=best
        var id:Int?=null
        for(node in SkillGraph.nodes){
            val dx=sx(node.x,w,h)-px;val dy=sy(node.y,w,h)-py
            val d=dx*dx+dy*dy
            if(d<=best){best=d;id=node.id}
        }
        return id
    }
}
