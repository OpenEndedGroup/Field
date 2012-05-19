package field.namespace.generic;

import java.io.Serializable;
import java.util.Iterator;

public class Generics {
	
	
	static public class Pair<A,B> implements Serializable
	{
		public A left;
		public B right;

		public Pair(A a, B b)
		{
			left = a;
			right = b;
		}
		
		public boolean equals(Object o)
		{
			if (!(o instanceof Pair)) return false;
			Pair p = (Pair)o;
			if  ( (((left==null) && (p.left==null)) || (left.equals(p.left)))
				&& (((right==null) && (p.right==null)) || (right.equals(p.right)))) return true;
			return false;
		}

		public int hashCode()
		{
			int l = (left==null ? 0 : left.hashCode());
			int r = (right==null ? 0 : right.hashCode());
			return l+r;
		}
		public String toString()
		{
			return "left:"+left+" right:"+right;
		}
	}
	
	static public class BiPair<A> implements Serializable
	{
		public A left;
		public A right;

		public BiPair(A a, A b)
		{
			left = a;
			right = b;
		}
		
		public boolean equals(Object o)
		{
			if (!(o instanceof BiPair)) return false;
			BiPair<A> p = (BiPair<A>)o;

			return (sequals(left, p.left) && sequals(right, p.right)) || (sequals(left, p.right) && sequals(right, p.left)); 
			
		}

		private boolean sequals(A a, A b) {
			if (a==null) return b==null;
			return a.equals(b);
		}

		public int hashCode()
		{
			int l = (left==null ? 0 : left.hashCode());
			int r = (right==null ? 0 : right.hashCode());
			return l+r;
		}
		public String toString()
		{
			return "left:"+left+" right:"+right;
		}

		public void swap() {
			A o = left;
			left = (A) right;
			right = (A) o;
		}
	}

	static public class Triple<A,B,C> implements Serializable
	{
		public A left;
		public B middle;
		public C right;

		public Triple(A a, B b, C c)
		{
			left = a;
			middle = b;
			right = c;
		}
		
		public boolean equals(Object o)
		{
			if (o == null) return false;
			if (!(o instanceof Triple)) return false;
			Triple p = (Triple)o;
			if (left==null && p.left!=null) return false;
			if (right==null && p.right!=null) return false;
			if (middle==null && p.middle!=null) return false;
			if (left!=null && p.left==null) return false;
			if (right!=null && p.right==null) return false;
			if (middle!=null && p.middle==null) return false;
			return left.equals(p.left) && right.equals(p.right) && middle.equals(p.middle);
		}

		public int hashCode()
		{
			int l = (left==null ? 0 : left.hashCode());
			int r = (middle==null ? 0 : middle.hashCode());
			int m = (right==null ? 0 : right.hashCode());
			return l+r+m;
		}
		public String toString()
		{
			return "left:"+left+" middle:"+middle+" right:"+right;
		}
	}
	
	
	static public class IterIter<T> implements Iterable<T>
	{
		private final Iterator<T> t;
		public IterIter(Iterator<T> t)
		{
			this.t = t;
		}
		public Iterator<T> iterator() {
			return t;
		}
	}
}
