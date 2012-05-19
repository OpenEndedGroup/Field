package field.graphics.imageprocessing;

import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.Yield;
import field.bytecode.protect.dispatch.Cont;
import field.bytecode.protect.dispatch.Cont.ReturnCode;
import field.bytecode.protect.dispatch.Cont.aRun;
import field.bytecode.protect.yield.YieldUtilities;
import field.graphics.windowing.FullScreenCanvasSWT;
import field.launch.iUpdateable;
import field.math.abstraction.iFloatProvider;
import field.math.abstraction.iProvider;

@Woven
public class ImageProcessorCrossfader implements iUpdateable{

	private final iProvider<Integer> delayProvider;
	private final iUpdateable left;
	private final iUpdateable right;

	public ImageProcessorCrossfader(iProvider<Integer> delayProvider, iUpdateable left, iUpdateable right)
	{
		this.delayProvider = delayProvider;
		this.left = left;
		this.right = right;
	}

	// o means at right;
	float at= 0;
	int t = 0;
	int delay = 0;
	
	@Yield
	public void update() {
		at = 0;
		left.update();
		right.update();
		while(true)
		{
			left.update();
			delay = delayProvider.get();
			for(int i=0;i<delay;i++)
			{
				at = i/(delay-1f);
				YieldUtilities.yield(null);
				beat();
			}
			right.update();
			delay = delayProvider.get();
			for(int i=0;i<delay;i++)
			{
				at = 1- i/(delay-1f);
				YieldUtilities.yield(null);
				beat();
			}
		}
	}
	
	protected void beat()
	{
		
	}
	
	public iFloatProvider getCrossfade()
	{
		return new iFloatProvider(){
			public float evaluate() {
				return at;
			}
		};
	}

	public void join(FullScreenCanvasSWT canvas) {
		aRun arun = new Cont.aRun(){
			@Override
			public ReturnCode head(Object calledOn, Object[] args) {
				update();
				return super.head(calledOn, args);
			}
		};
		Cont.linkWith(canvas, canvas.method_beforeFlush, arun);

	}
	
}
