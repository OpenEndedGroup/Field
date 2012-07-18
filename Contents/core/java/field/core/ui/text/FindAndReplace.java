package field.core.ui.text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Text;

import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.NextUpdate;
import field.core.StandardFluidSheet;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.plugins.selection.ToolBarFolder;
import field.core.ui.text.FindAndReplace.FindAll.Found;
import field.launch.Launcher;

@Woven
public class FindAndReplace extends Composite {
	private Text text;
	private Text text_1;
	private MenuItem mntmThisBoxOnly;
	private MenuItem mntmThisPropertyOnly;
	private MenuItem mntmUseRegularExpressions;
	private FindAll found;
	private Label lblNewLabel_1;
	private Button btnFindNext;
	private Button btnReplaceFind;
	private Button btnReplaceAll;
	private Found current;

	/**
	 * Create the composite.
	 * 
	 * @param parent
	 * @param style
	 */
	public FindAndReplace(Composite parent, int style) {
		super(parent, style | SWT.BORDER);
		setLayout(null);
		
		Label lblNewLabel = new Label(this, SWT.NONE);
		lblNewLabel.setBounds(-9, 15, 55, 15);
		lblNewLabel.setAlignment(SWT.RIGHT);
		lblNewLabel.setText("Find");

		text = new Text(this, SWT.BORDER);
		text.setBounds(50, 12, 231, 20);

		text.addModifyListener(new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent e) {
				rebuild();
			}
		});
		
		text_1 = new Text(this, SWT.BORDER);
		text_1.setBounds(342, 12, 231, 20);

		Label lblReplace = new Label(this, SWT.NONE);
		lblReplace.setBounds(281, 14, 55, 15);
		lblReplace.setText("Replace");
		lblReplace.setAlignment(SWT.RIGHT);

		btnFindNext = new Button(this, SWT.FLAT);
		btnFindNext.setBounds(50, 41, 73, 25);
		btnFindNext.setText("Find Next");

		btnReplaceFind = new Button(this, SWT.FLAT);
		btnReplaceFind.setBounds(129, 41, 73, 25);
		btnReplaceFind.setText("Replace");

		btnReplaceAll = new Button(this, SWT.FLAT);
		btnReplaceAll.setBounds(208, 41, 73, 25);
		btnReplaceAll.setText("Replace all");

		final Button btnNewButton = new Button(this, SWT.ARROW);
		btnNewButton.setBounds(548, 42, 25, 25);
		btnNewButton.setText("Options ...");

		final Menu menu = new Menu(btnNewButton);
		btnNewButton.setMenu(menu);
		
		btnNewButton.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				Point pp = Launcher.getLauncher().display.map(btnNewButton, null, new Point(e.x, e.y));
				menu.setLocation(pp.x, pp.y);
				menu.setVisible(true);
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		mntmThisBoxOnly = new MenuItem(menu, SWT.CHECK);
		mntmThisBoxOnly.setText("This box only");

		mntmThisPropertyOnly = new MenuItem(menu, SWT.CHECK);
		mntmThisPropertyOnly.setText("This property only");

		mntmUseRegularExpressions = new MenuItem(menu, SWT.CHECK);
		mntmUseRegularExpressions.setText("Use regular expressions");

		
		lblNewLabel_1 = new Label(this, SWT.NONE);
		lblNewLabel_1.setEnabled(false);
		lblNewLabel_1.setBounds(296, 47, 234, 15);

		btnReplaceFind.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				replace(text.getText(), text_1.getText(), mntmThisBoxOnly.getSelection(), mntmThisPropertyOnly.getSelection(), mntmUseRegularExpressions.getSelection());
			}
		});

		btnFindNext.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				findNext(text.getText(), mntmThisBoxOnly.getSelection(), mntmThisPropertyOnly.getSelection(), mntmUseRegularExpressions.getSelection());
			}
		});

		btnReplaceAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				replaceAll(text.getText(), text_1.getText(), mntmThisBoxOnly.getSelection(), mntmThisPropertyOnly.getSelection(), mntmUseRegularExpressions.getSelection());
			}
		});

//		setBackgroundRecursively(this);
		
	}

	protected void rebuild() {
		if (text.getText().length()==0)
		{
			found = null;
			lblNewLabel_1.setText("");
			btnFindNext.setEnabled(false);
			btnReplaceAll.setEnabled(false);
			btnReplaceFind.setEnabled(false);
		}
		found = new FindAll(text.getText(),  mntmThisBoxOnly.getSelection(), mntmThisPropertyOnly.getSelection(), mntmUseRegularExpressions.getSelection());
		found.build(getRoot());
		int x = found.found.size();
		if (x==0)
		{
			lblNewLabel_1.setText("no matches found");
			btnFindNext.setEnabled(false);
			btnReplaceAll.setEnabled(false);
			btnReplaceFind.setEnabled(false);
		}
		else if (x==1)
		{
			lblNewLabel_1.setText("1 match found");			
			btnFindNext.setEnabled(true);
			btnReplaceAll.setEnabled(true);
			btnReplaceFind.setEnabled(true);
		}
		else if (x>1)
		{
			lblNewLabel_1.setText(x+" matches found");			
			btnFindNext.setEnabled(true);
			btnReplaceAll.setEnabled(true);
			btnReplaceFind.setEnabled(true);
		}
		
		if (x>0)
		{
			Found q = found.found.get(0);
			if (q.box==getThisBox() && q.prop.equals(getThisProperty()))
			{
				ArrayList<Found> ff = new ArrayList<Found>();
				for(int i=0;i<found.found.size();i++)
				{
					if (found.found.get(i).box==getThisBox() && found.found.get(i).prop.equals(getThisProperty()))
						ff.add(found.found.get(i));
				}
				
				if (ff.size()==1)
					navigateTo(q);
				else
					navigateTo(getThisBox(), getThisProperty(), ff);
			}
		}
	}

	protected void navigateTo(iVisualElement thisBox, VisualElementProperty visualElementProperty, ArrayList<Found> ff) {
	}

	protected iVisualElement getRoot() {
		return null;
	}

	public class FindAll {

		boolean boxOnly;
		boolean propertyOnly;
		boolean regex;
		private Pattern r;

		public FindAll(String text, boolean boxOnly, boolean propertyOnly, boolean regex) {
			super();
			this.boxOnly = boxOnly;
			this.propertyOnly = propertyOnly;
			this.regex = regex;
			if (regex) {
				r = Pattern.compile(text);
			} else {
				r = Pattern.compile(Pattern.quote(text));
			}
		}

		public class Found {
			iVisualElement box;
			VisualElementProperty prop;
			int start, end;
		}

		List<Found> found;
		List<Found> prev = new ArrayList<FindAndReplace.FindAll.Found>();

		public void build(iVisualElement root) {
			found = new ArrayList<FindAndReplace.FindAll.Found>();
			prev = new ArrayList<FindAndReplace.FindAll.Found>();
			List<iVisualElement> all = StandardFluidSheet.allVisualElements(root);

			if (boxOnly) {
				all = new ArrayList<iVisualElement>();
				all.add(getThisBox());
			}

			all.remove(getThisBox());
			all.add(0, getThisBox());

			for (iVisualElement e : all) {
				findIn(e);
			}
		}

		private void findIn(iVisualElement e) {
			Map<Object, Object> m = e.payload();
			Set<Entry<Object, Object>> me = m.entrySet();
			for (Entry<Object, Object> ee : me) {
				if (ee.getValue() instanceof String) {
					if (!propertyOnly || (ee.getKey() instanceof VisualElementProperty) && ee.getKey().equals(getThisProperty())) {
						findIn(e, (VisualElementProperty) ee.getKey(), (String) ee.getValue());
					}
				}
			}
		}

		private void findIn(iVisualElement in, VisualElementProperty prop, String value) {
			Matcher q = r.matcher(value);
			while (q.find()) {
				Found f = new Found();
				f.start = q.start();
				f.end = q.end();
				f.prop = prop;
				f.box = in;
				found.add(f);
			}
		}
	}

	@NextUpdate(delay=3)
	protected void replaceAll(String find, String with, boolean boxOnly, boolean propertyOnly, boolean regex) {
		
		System.out.println(" replace all <"+found.found.size()+">");
		
		if (found.found.size()>0 || current!=null)
		{
			replace(find, with, boxOnly, propertyOnly, regex);
			replaceAll(find, with, boxOnly, propertyOnly, regex);
		}
	}

	public VisualElementProperty getThisProperty() {
		return null;
	}

	public iVisualElement getThisBox() {
		return null;
	}

	protected void findNext(String find, boolean boxOnly, boolean propertyOnly, boolean regex) {
		findNext(find, boxOnly, propertyOnly, regex, true);
	}
	protected void findNext(String find, boolean boxOnly, boolean propertyOnly, boolean regex, boolean allowWrap) {
		current = null;
		if (found==null) return;
		if (found.found.size()==0 && allowWrap)
		{
			found.prev.clear();
			rebuild();
			lblNewLabel_1.setText("search wrapped around");
		}
		
		if (found!=null && found.found.size()>0)
		{
			current = found.found.remove(0);
			found.prev.add(current);
			navigateTo(current);
		}
	}

	protected void navigateTo(Found f) {
	}

	protected void replace(String find, String with, boolean boxOnly, boolean propertyOnly, boolean regex) {
		if (current == null)
		{
			System.out.println(" no current ");
			findNext(find, boxOnly, propertyOnly, regex);
		}
		else
		{
			System.out.println(" current, replacing");
			replace(current, with);
			repair(current, with);
//			rebuild();
			findNext(find, boxOnly, propertyOnly, regex, false);			
		}
	}

	private void repair(Found c, String with) {
		for(Found f : found.found)
		{
			if (f.box == c.box && f.prop.equals(c.prop))
			{
				if (f.start>c.end)
				{
					f.start-=(c.end-c.start)-with.length();
					f.end-=(c.end-c.start)-with.length();
				}
			}
		}
	}

	private void replace(Found z, String with) {
		navigateTo(z);
		replaceInCurrent(z.start, z.end, with);
	}

	protected void replaceInCurrent(int start, int end, String with) {
	}

	private void setBackgroundRecursively(Composite c) {
		c.setBackground(ToolBarFolder.background);
		for (Control cc : c.getChildren()) {
			cc.setBackground(this.getBackground());
		}
	}

	@Override
	protected void checkSubclass() {
		// Disable the check that prevents subclassing of SWT components
	}

	public void setFind(String m) {
		text.setText(m);
	}
}
