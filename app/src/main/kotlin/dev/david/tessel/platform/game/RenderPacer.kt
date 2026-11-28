package dev.david.tessel.platform.game

/** A presentation clock independent of the 60 Hz simulation. Vsync quantization
 * is handled with a running deadline, not a reset-on-every-draw timer. */
class RenderPacer {
    private var next=Long.MIN_VALUE
    private var last=0L
    private var previousTarget=-1
    var drawnIntervalNanos=0L
        private set
    fun reset(){next=Long.MIN_VALUE;last=0L;previousTarget=-1;drawnIntervalNanos=0L}
    fun due(now:Long,target:Int,force:Boolean=false):Boolean{
        val fps=target.coerceAtLeast(0)
        val period=if(fps==0)0L else 1_000_000_000L/fps
        if(force||previousTarget!=fps||next==Long.MIN_VALUE){
            previousTarget=fps;next=now+period
        }else if(period>0L){
            // A small tolerance prevents alternating extra skipped frames on 60/120Hz screens.
            if(now+400_000L<next)return false
            next=if(now-next>period*2)now+period else next+period
        }
        drawnIntervalNanos=if(last==0L)if(period>0L)period else 16_666_667L else (now-last).coerceAtLeast(1L)
        last=now
        return true
    }
}
