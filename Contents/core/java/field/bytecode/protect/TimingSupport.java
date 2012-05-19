package field.bytecode.protect;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import field.util.ANSIColorUtils;
import field.util.HashMapOfLists;

public class TimingSupport {

	HashMap<String, HashMapOfLists<Object, Long>> starts = new HashMap<String, HashMapOfLists<Object, Long>>();
	
	public void handle_entry(Object fromThis, String name, Map<String, Object> parameterName) {
		HashMapOfLists<Object, Long> m = starts.get(name);
		if (m==null)
			starts.put(name, m = new HashMapOfLists<Object, Long>());
		m.getAndMakeCollection(fromThis).add(System.currentTimeMillis());
	}

	public void handle_exit(Object fromThis, String name, Map<String, Object> parameterName) {
		List<Long> m = starts.get(name).getList(fromThis);
		Long startedAt = m.remove(m.size()-1);
		long time = System.currentTimeMillis()-startedAt;
		System.err.println(ANSIColorUtils.blue(time+"ms inside <"+name+">"));
	}

}
