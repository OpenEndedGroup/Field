package field.namespace.change;

import java.io.Serializable;

/**
 * @author marc
 * Created on May 6, 2003
 */
public interface iChangable extends Serializable
{

	
	public iModCount getModCount(Object withRespectTo);
	
	public interface iModCount extends Serializable
	{
		public iModCount setRecompute(iRecompute r);
		public Object data();
		public Object data(iRecompute recompute);
		public boolean hasChanged();
		public iModCount clear(Object newData);
		
		public iModCount localChainWith(iModCount[] also);
	}
	
	public interface iRecompute extends Serializable
	{ 
		public Object recompute();
	}

}
