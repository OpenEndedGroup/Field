package field.bytecode.protect.yield;

public class YieldUtilities {

	static public Object yield(Object ret)
	{
		return ret;
	}

	static public Boolean yield(Boolean ret)
	{
		return ret;
	}
	
	static public class Finished extends RuntimeException
	{}
	
}
