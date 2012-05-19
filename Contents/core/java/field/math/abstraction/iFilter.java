package field.math.abstraction;


public interface iFilter<t_in, t_out>
{
	public t_out filter(t_in value);
}
