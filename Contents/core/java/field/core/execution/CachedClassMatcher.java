package field.core.execution;

import java.util.ArrayList;
import java.util.List;

import field.util.HashMapOfLists;

public class CachedClassMatcher<t_return, t_upper> {

	public interface iMatcher<t_return, t_upper> {
		public boolean canMatch(t_upper o);

		public t_return match(t_upper o);
	}

	HashMapOfLists<Class<? extends t_upper>, iMatcher<t_return, t_upper>> cache = new HashMapOfLists<Class<? extends t_upper>, iMatcher<t_return, t_upper>>();
	
	List<iMatcher<t_return, t_upper>> matcher = new ArrayList<iMatcher<t_return, t_upper>>();

	private iMatcher<t_return, t_upper> m;
	
	public void addMatcher(iMatcher<t_return, t_upper> m)
	{
		this.m = m;
	}

	
}
