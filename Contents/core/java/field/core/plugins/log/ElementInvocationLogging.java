package field.core.plugins.log;

import java.util.LinkedHashSet;

import org.python.core.PyObject;

import field.core.Constants;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.dispatch.iVisualElementOverrides.Ref;
import field.core.plugins.log.AssemblingLogging.iUndoable;
import field.core.plugins.log.Logging.iLoggingEvent;
import field.core.plugins.selection.PopupInfoWindow;
import field.core.plugins.selection.SelectionSetDriver;
import field.core.ui.UbiquitousLinks;
import field.launch.SystemProperties;

/**
 * sources information about which velement is running
 *
 * @author marc
 *
 */
public class ElementInvocationLogging {

	static public class DidGetLocalVariable implements iLoggingEvent, iProvidesContextStack {

		private StackTrace stack;
		String name;
		String objectText;
		transient Object o;

		LinkedHashSet<iLoggingEvent> sus = new LinkedHashSet<iLoggingEvent>();

		public DidGetLocalVariable(String name, Object value) {
			this.name = name;

			if (value instanceof PyObject)
			{
				value = ((PyObject)value).__tojava__(Object.class);
			}

			this.objectText = safeText("" + value);
			this.o = value;
			if (storeStackTraces) stack = new StackTrace();

		}

		public String getLongDescription() {
			return "<html><font color='#"+Constants.defaultTreeColor+"'>global, get: <b>" + name + "</b>\n value <b>" + (o == null ? objectText : "" + dress(o)) + "</b>"+(stack==null ? "" :stack.format());
		}

		public String getReplayExpression() {
			return null;
		}

		public LinkedHashSet<iLoggingEvent> getSuspendedContext() {
			return sus;
		}

		public String getTextDescription() {
			return "<html><font color='#"+Constants.defaultTreeColor+"'>"+smallerStill("global,")+" get: " + copyToClipboard(name,name) + " (" + (o == null ? objectText : ("" + (openInBrowser(name, ""+dress(o), o)))) + ")";
		}
		public boolean isError() {
			return false;
		}
	}

	static public class DidGetLocalVariableByAutoExecution implements iLoggingEvent, iSuspendedContext {

		private final iVisualElement element;
		transient public final Object got;
		private final String gotText;
		private StackTrace stack;
		String name;

		public DidGetLocalVariableByAutoExecution(String name, iVisualElement target, Object got) {
			this.name = name;
			this.element = target;
			this.got = got;
			this.gotText = "" + got;
			if (storeStackTraces) stack = new StackTrace();
		}

		public String getLongDescription() {
			return "<html><font color='#"+Constants.defaultTreeColor+"'>global, got <b>" + copyToClipboard(name, name) + "</b> = " + gotText + " by <b>" + describeElement(element) + "</b> auto exec"+(stack==null ? "" :stack.format());
		}

		public Class getMatchingBegin() {
			return WillGetLocalVariableByAutoExecution.class;
		}

		public Class getMatchingEnd() {
			return null;
		}

		public String getReplayExpression() {
			return null;
		}

		public String getTextDescription() {
			return "<html><font color='#"+Constants.defaultTreeColor+"'>"+smallerStill("global,")+" got " + name + " = " + gotText + " by " + describeElementLink(element) + " auto exec";
		}

		public Object getToken() {
			return new Object[] { name, element };
		}

		public boolean isError() {
			return false;
		}
		public boolean isExclusivelyContextural() {
			return false;
		}
	}

	static public class DidGetProperty implements iLoggingEvent, iProvidesContextStack
	{
		private StackTrace stack;
		private final iVisualElement element;
		private final VisualElementProperty p;
		transient Object value;
		transient LinkedHashSet<iLoggingEvent> sus = new LinkedHashSet<iLoggingEvent>();

		public DidGetProperty(VisualElementProperty p, Ref<?> ref)
		{
			if (storeStackTraces) stack = new StackTrace();
			value = ref.get();
			element = ref.getStorageSource();
			this.p = p;
		}

		public String getLongDescription() {
			String s = "<html><font color='#"+Constants.defaultTreeColor+"'>";

			s+= PopupInfoWindow.title("Get Property \u2014 name:")+PopupInfoWindow.content(p.getName()+"")+"<BR>";
			s+= PopupInfoWindow.title("Value:")+PopupInfoWindow.content(safeText(value+"")+(value==null ? "" : ("<font size=-3>("+value.getClass()+")</font>")))+"<BR>";
			s+= PopupInfoWindow.title("From:")+PopupInfoWindow.content("element :"+describeElementLink(element))+"<BR>";
			if (stack!=null)
				s+= PopupInfoWindow.title("At:")+PopupInfoWindow.stackTrace(stack.trace)+"<BR>";

			return s;
		}

		public String getReplayExpression() {
			return null;
		}

		public LinkedHashSet<iLoggingEvent> getSuspendedContext() {
			return sus;
		}

		public String getTextDescription() {
			return "<html><font color='#"+Constants.defaultTreeColor+"'>"+smallerStill("property,")+" get: <b>" + p.getName()+ "</b>\n  \u2190 " + value+" <font size=-3>(element :"+describeElementLink(element)+")</font>";
		}

		public boolean isError() {
			return false;
		}
	}

	static public class DidSetLocalVariable implements iLoggingEvent, iProvidesContextStack {

		private StackTrace stack;
		String name;
		String objectText;
		transient Object o;
		String objectWasText;
		transient Object ow;

		LinkedHashSet<iLoggingEvent> sus = new LinkedHashSet<iLoggingEvent>();

		public DidSetLocalVariable(String name, Object value, Object was) {
			this.name = name;
			this.objectText = safeText("" + value);
			this.o = value;
			this.objectWasText = safeText("" + was);
			this.ow = was;
			if (storeStackTraces) stack = new StackTrace();
		}

		public String getLongDescription() {
			return "<html><font color='#"+Constants.defaultTreeColor+"'>global, set: <b>" + name + "</b>\n  \u2190 " + (o == null ? objectText : safeText("" + o)) + "\n was " + (ow == null ? objectWasText : safeText("" + ow))+(stack==null ? "" :stack.format());
		}

		public String getReplayExpression() {
			return null;
		}

		public LinkedHashSet<iLoggingEvent> getSuspendedContext() {
			return sus;
		}

		public String getTextDescription() {
			if (objectWasText.equals("null"))
				return "<html><font color='#"+Constants.defaultTreeColor+"'>"+smallerStill("global,")+" new set: " + name + " = " + (o == null ? objectText : safeText("" + o));
			return "<html><font color='#"+Constants.defaultTreeColor+"'>"+smallerStill("global,")+" set: " + name + " \u2190 " + (o == null ? objectText : safeText("" + o)) + " (was " + (ow == null ? objectWasText : safeText("" + ow)) + ")";
		}
		public boolean isError() {
			return false;
		}
	}

	static public class DidSetProperty implements iLoggingEvent, iProvidesContextStack, iUndoable
	{
		private StackTrace stack;
		private final VisualElementProperty p;
		private final iVisualElement element;
		private final Object newvalue;
		private final Object oldvalue;
		transient LinkedHashSet<iLoggingEvent> sus = new LinkedHashSet<iLoggingEvent>();

		public DidSetProperty(VisualElementProperty p, iVisualElement on, Object newvalue, Object oldvalue)
		{
			if (storeStackTraces) stack = new StackTrace();

			this.p = p;
			this.element = on;
			this.newvalue = newvalue;
			this.oldvalue = oldvalue;

		}

		public String getLongDescription() {
			String s = "<html><font color='#"+Constants.defaultTreeColor+"'>";

			s+= PopupInfoWindow.title("Set Property \u2014 name:")+PopupInfoWindow.content(p.getName()+"")+"<BR>";
			s+= PopupInfoWindow.title("From:")+PopupInfoWindow.content(safeText(newvalue+"")+(newvalue==null ? "" : ("<font size=-3>("+newvalue.getClass()+")</font>")))+"<BR>";
			s+= PopupInfoWindow.title("To:")+PopupInfoWindow.content(safeText(oldvalue+"")+(oldvalue==null ? "" : ("<font size=-3>("+oldvalue.getClass()+")</font>")))+"<BR>";
			if (stack!=null)
				s+= PopupInfoWindow.title("At:")+PopupInfoWindow.stackTrace(stack.trace)+"<BR>";

			return s;
		}

		public String getReplayExpression() {
			return null;
		}

		public LinkedHashSet<iLoggingEvent> getSuspendedContext() {
			return sus;
		}

		public String getTextDescription() {
			return "<html><font color='#"+Constants.defaultTreeColor+"'>"+smallerStill("property,")+" set: <b>" + p.getName()+ "</b>\n  \u2190 " + newvalue+"";
		}

		public boolean isError() {
			return false;
		}

		public void executeSimpleUndo() {
			p.set(element, element, oldvalue);
		}

		public String getDoExpression() {
			return null;
		}

		public String getUndoExpression() {
			return null;
		}
	}

	static public class ElementExecutionBegin implements iLoggingEvent, iSuspendedContext, iProvidesContextStack {
		public iVisualElement element;
		private StackTrace stack;

		public ElementExecutionBegin(iVisualElement element) {
			super();
			this.element = element;

			if (storeStackTraces) stack = new StackTrace();
		}

		public String getLongDescription() {
			return "<html><font color='#"+Constants.defaultTreeColor+"'>begin \u2014 " + describeElement(element)+(stack==null ? "" :stack.format());
		}

		public Class getMatchingBegin() {
			return null;
		}

		public Class getMatchingEnd() {
			return ElementExecutionEnd.class;
		}

		public String getReplayExpression() {
			return null;
		}

		public String getTextDescription() {
			return "<html><font color='#"+Constants.defaultTreeColor+"'>begin \u2014 " + describeElementLink(element);
		}

		public Object getToken() {
			return element;
		}

		public boolean isError() {
			return false;
		}
		public boolean isExclusivelyContextural() {
			return false;
		}

		LinkedHashSet<iLoggingEvent> sus = new LinkedHashSet<iLoggingEvent>();
		public LinkedHashSet<iLoggingEvent> getSuspendedContext() {
			return sus;
		}
	}

	static public class ElementExecutionEnd implements iLoggingEvent, iSuspendedContext, iProvidesContextStack {
		public iVisualElement element;
		private StackTrace stack;

		public ElementExecutionEnd(iVisualElement element) {
			super();
			this.element = element;
			if (storeStackTraces) stack = new StackTrace();
		}

		public String getLongDescription() {
			return "<html><font color='#"+Constants.defaultTreeColor+"'>end\u2014 " + element+(stack==null ? "" :stack.format());
		}

		public Class getMatchingBegin() {
			return ElementExecutionBegin.class;
		}

		public Class getMatchingEnd() {
			return null;
		}

		public String getReplayExpression() {
			return null;
		}

		public String getTextDescription() {
			return "<html><font color='#"+Constants.defaultTreeColor+"'>end\u2014 " + describeElementLink(element);
		}

		public Object getToken() {
			return element;
		}

		public boolean isError() {
			return false;
		}
		public boolean isExclusivelyContextural() {
			return false;
		}
		LinkedHashSet<iLoggingEvent> sus = new LinkedHashSet<iLoggingEvent>();
		public LinkedHashSet<iLoggingEvent> getSuspendedContext() {
			return sus;
		}


	}

	static public class ElementExecutionFocusBegin implements iLoggingEvent, iSuspendedContext {
		public iVisualElement element;
		private StackTrace stack;

		public ElementExecutionFocusBegin(iVisualElement element) {
			super();
			this.element = element;
			if (storeStackTraces) stack = new StackTrace();
		}

		public String getLongDescription() {
			return "<html><font color='#"+Constants.defaultTreeColor+"'>enter\u2014 " + describeElement(element)+(stack==null ? "" :stack.format());
		}

		public Class getMatchingBegin() {
			return null;
		}

		public Class getMatchingEnd() {
			return ElementExecutionFocusEnd.class;
		}

		public String getReplayExpression() {
			return null;
		}

		public String getTextDescription() {
			return "<html><font color='#"+Constants.defaultTreeColor+"'>enter\u2014 " + describeElementLink(element);
		}

		public Object getToken() {
			return element;
		}

		public boolean isError() {
			return false;
		}
		public boolean isExclusivelyContextural() {
			return true;
		}

	}

	static public class ElementExecutionFocusEnd implements iLoggingEvent, iSuspendedContext {
		public iVisualElement element;
		private StackTrace stack;

		public ElementExecutionFocusEnd(iVisualElement element) {
			super();
			this.element = element;
			if (storeStackTraces) stack = new StackTrace();
		}

		public String getLongDescription() {
			return "<html><font color='#"+Constants.defaultTreeColor+"'>exit\u2014 " + describeElement(element)+(stack==null ? "" :stack.format());
		}

		public Class getMatchingBegin() {
			return ElementExecutionFocusBegin.class;
		}

		public Class getMatchingEnd() {
			return null;
		}

		public String getReplayExpression() {
			return null;
		}

		public String getTextDescription() {
			return "<html><font color='#"+Constants.defaultTreeColor+"'>exit\u2014 " + describeElementLink(element);
		}

		public Object getToken() {
			return element;
		}

		public boolean isError() {
			return false;
		}
		public boolean isExclusivelyContextural() {
			return true;
		}

	}

	static public class ElementTextFragmentWasExecuted implements iLoggingEvent, iProvidesContextStack {
		public final String text;
		public final iVisualElement element;
		public StackTrace stack;

		LinkedHashSet<iLoggingEvent> sus = new LinkedHashSet<iLoggingEvent>();

		public ElementTextFragmentWasExecuted(String text, iVisualElement e) {
			this.text = text;
			this.element = e;
			if (storeStackTraces) stack = new StackTrace();
		}

		public String getLongDescription() {
			return "<html><font color='#"+Constants.defaultTreeColor+"'>"+ describeElement(element) + " executed text fragment\u2014 " + text+(stack==null ? "" :stack.format());
		}

		public String getReplayExpression() {
			return null;
		}

		public LinkedHashSet<iLoggingEvent> getSuspendedContext() {
			return sus;
		}

		public String getTextDescription() {
			String[] ll = text.split("\n");
			String firstList = ll[0];
			String ff = ll.length == 1 ? "'" + firstList + "'" : "'" + firstList + "...'";
			return "<html><font color='#"+Constants.defaultTreeColor+"'>element <i>" + describeElementLink(element) + "</i> executed text fragment\u2014 " + ff;
		}
		public boolean isError() {
			return false;
		}

	}

	// these need to be mushed to simply _label _ moves I guess

	static public class ElementTextWasExecuted implements iLoggingEvent, iProvidesContextStack {
		public final String text;
		private StackTrace stack;

		LinkedHashSet<iLoggingEvent> sus = new LinkedHashSet<iLoggingEvent>();

		public ElementTextWasExecuted(String text) {
			this.text = text;
			if (storeStackTraces) stack = new StackTrace();
		}

		public String getLongDescription() {

			//
			return "<html><font color='#"+Constants.defaultTreeColor+"'>executed text \u2014 " + text+(stack==null ? "" :stack.format());
		}

		public String getReplayExpression() {
			return null;
		}
		public LinkedHashSet<iLoggingEvent> getSuspendedContext() {
			return sus;
		}

		public String getTextDescription() {
			for(iLoggingEvent s : sus)
			{
				if (s instanceof ElementExecutionFocusBegin)
				{
					ElementExecutionFocusBegin b = ((ElementExecutionFocusBegin)s);
					return "<html><font color='#"+Constants.defaultTreeColor+"'> executed <b>"+describeElementLink(b.element)+"</b> "+smaller("(all "+text.split("\n").length + " lines"+")");
				}
			}
			return "executed text \u2014 " + text.split("\n").length + " lines";
		}
		public boolean isError() {
			return false;
		}

	}

	public interface iProvidesContextStack {
		public LinkedHashSet<iLoggingEvent> getSuspendedContext();
	}

	public interface iSuspendedContext {
		public Class getMatchingBegin();

		public Class getMatchingEnd();

		public Object getToken();

		public boolean isExclusivelyContextural();
	}

	static public class MakeAutoExecutionTarget implements iLoggingEvent, iProvidesContextStack {

		private final iVisualElement element;
		transient private final Object got;
		private final String gotText;
		private StackTrace stack;
		String name;

		LinkedHashSet<iLoggingEvent> sus = new LinkedHashSet<iLoggingEvent>();

		public MakeAutoExecutionTarget(String name, iVisualElement target, Object got) {
			this.name = name;
			this.element = target;
			this.got = got;
			this.gotText =safeText( "" + got);
			if (storeStackTraces) stack = new StackTrace();
		}

		public String getLongDescription() {
			return "<html><font color='#"+Constants.defaultTreeColor+"'>new autoexecution <b>" + name + "</b>\n element <b>" + describeElement(element) + "</b>\n currently <b>" + got + "</b>"+(stack==null ? "" :stack.format());
		}

		public String getReplayExpression() {
			return null;
		}
		public LinkedHashSet<iLoggingEvent> getSuspendedContext() {
			return sus;
		}

		public String getTextDescription() {
			return "<html><font color='#"+Constants.defaultTreeColor+"'>new autoexecution " + name + " element " + describeElementLink(element) + " currently " + got;
		}
		public boolean isError() {
			return false;
		}

	}

	static public class StackTrace
	{
		private final StackTraceElement[] trace;

		public StackTrace()
		{
			trace = new Exception().getStackTrace();
		}

		public String format()
		{
//			StringBuilder b = new StringBuilder();
//			b.append("<br><b>stack trace</b><font size=-2>");
//			for(int i=2;i<trace.length;i++)
//			{
//				b.append("<b>    "+className(trace[i].getClassName())+"</b>."+trace[i].getMethodName()+"<font size=-3>("+trace[i].getFileName()+":<b>"+trace[i].getLineNumber()+"</b>)</font><br>");
//			}
//			b.append("</font>");
//			return b.toString();
			
			return PopupInfoWindow.stackTrace(trace);
			
		}

		private String className(String className) {
			if (className.indexOf(".")==-1) return className;
			String[] q = className.split("\\.");
			StringBuilder b = new StringBuilder();
			b.append("<font size=-3>");
			for(int i=0;i<q.length-1;i++)
			{
				b.append(q[i]+".");
			}
			b.append("</font><b>"+q[q.length-1]+"</b>");
			return b.toString();
		}
	}

	static public class WillGetLocalVariableByAutoExecution implements iLoggingEvent, iSuspendedContext, iProvidesContextStack {

		private final iVisualElement element;
		private StackTrace stack;
		String name;

		LinkedHashSet<iLoggingEvent> sus = new LinkedHashSet<iLoggingEvent>();

		public WillGetLocalVariableByAutoExecution(String name, iVisualElement target) {
			this.name = name;
			this.element = target;
			if (storeStackTraces) stack = new StackTrace();
		}

		public String getLongDescription() {
			return "<html><font color='#"+Constants.defaultTreeColor+"'>global, will get <b>" + name + "</b> by <b>" + describeElement(element) + "</b> auto exec"+(stack==null ? "" :stack.format());
		}

		public Class getMatchingBegin() {
			return null;
		}

		public Class getMatchingEnd() {
			return DidGetLocalVariableByAutoExecution.class;
		}

		public String getReplayExpression() {
			return null;
		}

		public LinkedHashSet<iLoggingEvent> getSuspendedContext() {
			return sus;
		}

		public String getTextDescription() {
			return "<html><font color='#"+Constants.defaultTreeColor+"'>global, will get " + name + " by " + describeElementLink(element) + " auto exec";
		}

		public Object getToken() {
			return new Object[] { name, element };
		}

		public boolean isError() {
			return false;
		}
		public boolean isExclusivelyContextural() {
			return false;
		}
	}

	static public boolean storeStackTraces = SystemProperties.getIntProperty("logging.stacktraces", 0)==1;

	static public String dress(Object o) {
		if (o instanceof iVisualElement)
			return SelectionSetDriver.nameFor((iVisualElement) o);
		return safeText(""+o);
	}

	static String copyToClipboard(String text, String toclip)
	{
		return UbiquitousLinks.links.link(text, UbiquitousLinks.links.code_copyTextToClipboard(toclip), null);
	}

	static String describeElement(iVisualElement e) {
		if (e == null) return "\u2014\u2014unavailable\u2014\u2014";
		String name = e.getProperty(iVisualElement.name);
		if (name == null)
			return "no name";
		return name;
	}

	public static String describeElementLink(iVisualElement e) {
		if (e == null) return "\u2014\u2014unavailable\u2014\u2014";
		String name = e.getProperty(iVisualElement.name);
		if (name == null)
			name = "no name";

		return UbiquitousLinks.links.link("<b>"+name+"</b>", UbiquitousLinks.links.code_selectOrMarkByUID(e.getUniqueID()), null);
	}

	static String openInBrowser(String name, String valueAsText, Object value)
	{
		return UbiquitousLinks.links.link(valueAsText, UbiquitousLinks.links.code_openInBrowser(name, value), null);
	}

	static String smaller(String t) {
		return "<i><font size=-2>"+t+"</font></i>";
	}
	
	static protected String smallerStill(String text) {
		return "<font size=-3 color='#" + Constants.defaultTreeColorDim + "'>" + text + "</font>";
	}

	
	static String safeText(String s)
	{
		return s.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
	}

}
