package field.util;

import java.text.NumberFormat;
import java.text.ParseException;

import javax.swing.text.NumberFormatter;

public class TextOnlyProgressBar {

	private final int width;
	private final String prefix;
	private long start;
	private long lastnow;

	float lastf = 0;

	public TextOnlyProgressBar(String prefix, int width) {
		this.prefix = prefix;
		this.width = width;
		System.out.print(prefix + " |" + num(width, " ") + "| --\r");
		start = System.currentTimeMillis();
	}

	private String num(int w, String string) {
		if (w<0) return "";
		
		StringBuffer f = new StringBuffer(w);
		for (int i = 0; i < w; i++) {
			f.append(string);
		}
		return f.toString();
	}

	public void amount(float f) {
		int n = (int) (width * f);
		NumberFormat i = NumberFormat.getNumberInstance();
		i.setMaximumFractionDigits(2);
		NumberFormatter format = new NumberFormatter(i);

		long now = System.currentTimeMillis();

		if (now - lastnow > 300 || (f == 1 && lastf != 1)) {
			lastnow = now;
			lastf = f;

			long secondsLeft = f == 1 ? (now-start)/1000 : (long) ((1-f)*(now - start)/f / 1000);

			long seconds = secondsLeft % 60;
			long mins = (secondsLeft / 60) % 60;
			long hours = ((secondsLeft / 60) / 60) % 24;
			long days = ((secondsLeft / 60) / 60) / 24;

			String left = seconds + "s";
			if (mins > 0) {
				left = mins + "m" + left;
			}
			if (hours > 0) {
				left = hours + "h" + left;
			}
			if (days > 0) {
				left = days + "d" + left;
			}

			if (f==0)
				left += " ";
			else
			if (f!=1)
				left += " remaining";
			else left = "("+left+")";
			
			try {
				System.out.print(ANSIColorUtils.eraseLine() + prefix + " |" + num(n, "*") + num(width - n, " ") + "| " + format.valueToString(f * 100) + "% " + left + "\r");
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
	}

}
