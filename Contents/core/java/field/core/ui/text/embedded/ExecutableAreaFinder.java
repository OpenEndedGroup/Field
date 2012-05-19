package field.core.ui.text.embedded;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExecutableAreaFinder {

	public int lastSubstring_start;
	public int lastSubstring_end;

	public String findExecutableSubstring(int caretPosition, String string, int key) {
		if (key == 0)
			return string;
		
		// we search outwards until we come to a comment
		// that matches the key
		String before = string.substring(0, caretPosition);
		StringBuffer buffer = new StringBuffer(before.length());
		for (int i = 0; i < before.length(); i++)
			buffer.append(before.charAt(before.length() - 1 - i));

		// #--{num. comment}

		Matcher m = Pattern.compile("\\." + key + "[^\\{]*?\\{--#").matcher(buffer);
		if (m.find()) {
			int startAt = before.length() - m.end();
			Matcher m2 = Pattern.compile("#--\\{[^\\{]*\\\\" + key + "\\.").matcher(string.substring(caretPosition, string.length()));
			if (m2.find()) {
				int endAt = m2.start() - 1 + caretPosition;
				lastSubstring_start = startAt;
				lastSubstring_end = endAt;
				
				
				return string.substring(startAt, endAt);
			} else {
				return findExecutableSubstring(string, ""+key);
				
			}
		}
		return findExecutableSubstring(string, ""+key);
	}


	public String findExecutableSubstring(String string, String key) {
		// we search outwards until we come to a comment
		// that matches the key

		// #--{key} to #--{\key}



		String pat = "\\{"+key+".*?\\}(.*?)\\#--\\{\\\\"+key;
		System.out.println(" looking for pattern <"+pat+"> in <"+string+">");

		Pattern c = Pattern.compile(pat, Pattern.DOTALL);
		Matcher m = c.matcher(string);

		String s = "";
		while (m.find()) {
			s += m.group(1) + "\n";
		}
		return s;
	}

	public String findExecutableSubstringAnyKey(int caretPosition, String string) {
		// we search outwards until we come to a comment that matches the key
		String before = string.substring(0, caretPosition);
		StringBuffer buffer = new StringBuffer(before.length());
		for (int i = 0; i < before.length(); i++)
			buffer.append(before.charAt(before.length() - 1 - i));

		// #--{num. comment}

		String key = "\\d";

		Matcher m = Pattern.compile("\\." + key + "[^\\{]*?\\{--#").matcher(buffer);

		if (m.find()) {
			int startAt = before.length() - m.end();
			Matcher m2 = Pattern.compile("#--\\{[^\\{]*\\\\" + key + "\\.").matcher(string.substring(caretPosition, string.length()));
			if (m2.find()) {
				int endAt = m2.start() - 1 + caretPosition;
				lastSubstring_start = startAt;
				lastSubstring_end = endAt;
				return string.substring(startAt, endAt);
			} else {
				lastSubstring_start = startAt;
				lastSubstring_end = string.length();
				return string.substring(startAt, string.length());
			}
		}
		else
		{
			System.out.println(" didn't find <"+key+"> in <"+buffer+">");
		}
		return "";
	}



}
