package field.util;

/**
 * 
 * @author marc
 */
public class NPermuteMIterator

{
	Object[] objects;
	int choosem;
	
	int[] counters;
	
	public NPermuteMIterator(Object[] objects, int choosem)
	{	this.objects = objects;
		this.choosem= choosem;
		counters = new int[choosem];
		finished = objects.length==0;
	}
	
	boolean finished = false;
	public boolean hasNext()
	{
		return !finished;
	}
	
	public void next(Object[] into)
	{
		assert into.length == counters.length : into.length+"!=="+counters.length;
		for(int i=0;i<into.length;i++)
		{
			into[i] = objects[counters[i]];			
		}	
		
		counters[0]++;
		int i = 0 ;
		while(counters[i]>=objects.length)
		{
			counters[i]=0;
			i+=1;
			if (i<counters.length)
			{
				counters[i]++;
			}
			else
			{
				finished = true;
				break;
			}
		}
	}
}

