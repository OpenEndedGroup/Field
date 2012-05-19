package field.core.execution;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.python.core.PyFunction;
import org.python.core.PyGenerator;
import org.python.core.PyMethod;

import field.math.abstraction.iProvider;


/**
 * @author marc
 * Created on Dec 27, 2003
 */
public class EvaluationGeneratorStack extends PythonGeneratorStack {

	
	public EvaluationGeneratorStack(PyGenerator top) {
		super(top);
	}
	
	public EvaluationGeneratorStack(PyGenerator top, List addTo) {
		super(top, addTo);
	}
	public EvaluationGeneratorStack(PyFunction  top, List addTo) {
		super(top, addTo);
	}

	public EvaluationGeneratorStack(PyMethod  top, List addTo) {
		super(top, addTo);
	}

	public EvaluationGeneratorStack(iProvider top) {
		super(top);
	}
	
	public EvaluationGeneratorStack(iProvider top, List addTo) {
		super(top, addTo);
	}
	
	ArrayList returnList = new ArrayList();
	
	public List evaluateReturn()
	{
		returnList.clear();
		evaluate();
		return returnList;
	}
	
	HashSet pausedSet = new HashSet();
	
	boolean paused = false;
	
	public void pauseMain()
	{
		paused = true;
	}
	
	public void unpauseMain()
	{
		paused = false;
	}
	
	protected  boolean shouldEvaluateMain() {
		return !paused;
	}
	
	public void pauseChild(PythonGeneratorStack child)
	{
		pausedSet.add(child);	
	}
	
	public void unpauseChild(PythonGeneratorStack child){
		pausedSet.remove(child);
	}
	
	protected  boolean shouldEvaluate(PythonGeneratorStack stack2) {
		return !pausedSet.contains(stack2);
	}
	
	protected void newPythonGeneratorStack(iProvider g, List subUpdateables2) {
		new EvaluationGeneratorStack(g, subUpdateables2);
	}
	
	protected void handleEvaluation(int childIndex, Object a) {
		super.handleEvaluation(childIndex, a);
		if (childIndex>-1)
		{	
			returnList.add(childIndex, a);
		}
		else
			returnList.add(0,a);
	}
	
}
