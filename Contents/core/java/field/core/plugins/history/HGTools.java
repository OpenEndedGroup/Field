package field.core.plugins.history;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.Cached;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.plugins.history.HistoryExplorerHG.VersionNode;
import field.core.util.ExecuteCommand;
import field.launch.SystemProperties;

/**
 * mainly for parsing the output of the hg file command
 * 
 * @author marc
 * 
 */
@Woven
public class HGTools {

	static public class HGLog {
		private String filename;

		// {copies} template doesn't work?
		String template = "rev<<{rev}>>\ndate<<{date}>>\nfiles<<{files}>>\ncopies<<{file_copies%filecopy}>>\ndesc<<{desc}>>\n---------";
		Pattern rev_pattern = Pattern.compile("rev<<(.*?)>>");
		Pattern date_pattern = Pattern.compile("date<<(.*?)>>");
		Pattern desc_pattern = Pattern.compile("desc<<(.*?)>>");

		Pattern files_pattern = Pattern.compile("files<<(.*?)>>");
		Pattern copies_pattern = Pattern.compile("copies<<(.*?)>>");

		Pattern inside_file_pattern = Pattern.compile("(.*?) ");

		String allText;

		public HGLog(String filename) {
			this.filename = filename;

			if (new File(filename).getParentFile().exists()) {
				ExecuteCommand command = new ExecuteCommand(new File(filename).getParentFile().getAbsolutePath(), HGVersioningSystem.hgCommand + " log -vfC --template '" + template + "' "/* + filename*/);
				command.waitFor(true);
				allText = command.getOutput();
			} else
				allText = "";
		}

		public HGLog(String filename, String allText) {
			this.filename = filename;
			this.allText = allText;

		}

		public List<VersionNode> getVersionNodes() {
			return getVersionNodes(new HashMap<Long, VersionNode>());
		}

		public List<VersionNode> getVersionNodes(HashMap<Long, VersionNode> foundSoFar) {
			Matcher rev_matcher = rev_pattern.matcher(allText);
			Matcher date_matcher = date_pattern.matcher(allText);
			Matcher desc_matcher = desc_pattern.matcher(allText);

			Matcher files_matcher = files_pattern.matcher(allText);
			Matcher copies_matcher = copies_pattern.matcher(allText);

			ArrayList<VersionNode> nodes = new ArrayList<VersionNode>();
			int at = 0;
			String currentFileName = filename;

			String vd = null;
			try {
				vd = new File(SystemProperties.getDirProperty("versioning.dir")).getCanonicalPath();
			} catch (IOException e1) {
				e1.printStackTrace();
			}

			String filenameInRepository = null;
			try {
				filenameInRepository = new File(filename).getCanonicalPath();
				vd = versioningDirAccountingForForest(vd, filenameInRepository);
			} catch (IOException e1) {
				e1.printStackTrace();
			}

			String shortname = filenameInRepository.replaceAll(vd + "/", "");
			;//System.out.println(" shortname is <" + shortname + ">");
			// ;//System.out.println("vd<" + vd + "> <" +
			// filenameInRepository + "> <" + shortname);
			// ;//System.out.println(" all text <" +
			// allText.substring(0, 1000) + ">");

			while (at < allText.length()) {
				rev_matcher.region(at, allText.length());
				date_matcher.region(at, allText.length());
				desc_matcher.region(at, allText.length());
				files_matcher.region(at, allText.length());
				copies_matcher.region(at, allText.length());
				boolean f = rev_matcher.find();
				if (f) {
					VersionNode already = foundSoFar.get(Long.parseLong(rev_matcher.group(1)));
					files_matcher.find();
					String found = files_matcher.group(1);
					// ;//System.out.println(at + " files:" +
					// found + " " + shortname + "(" +
					// rev_matcher.group(1) + ")");

					VersionNode n = new VersionNode();

					// if we're not mentioned in the
					// files, the it doesn't effect
					// us
					if (found.indexOf(shortname) == -1) {
						// unless we are being
						// copied
						copies_matcher.find();
						String c = copies_matcher.group(1);

						Pattern weveBeenCopied = Pattern.compile("([^\\)]*) \\(" + shortname + "\\)");
						Matcher m = weveBeenCopied.matcher(c);
						if (m.find()) {
							foundSoFar.put(Long.parseLong(rev_matcher.group(1)), n);
							n.revision = Long.parseLong(rev_matcher.group(1));
							// ;//System.out.println(" we've been copied to <"
							// + m.group(1) +
							// ">, building that history ");
							List<VersionNode> copyTargetHistory = new HGLog(vd + "/" + m.group(1), allText).getVersionNodes(foundSoFar);
							VersionNode prev = null;
							for (VersionNode q : copyTargetHistory) {

								// ;//System.out.println(" parallel history has node <"
								// + q +
								// ">\n we're looking for <"
								// +
								// rev_matcher.group(1)
								// + ">");

								if (q.revision == Long.parseLong(rev_matcher.group(1))) {
									n.copyTo = prev;
									if (prev != null) {
										prev.copiedFrom = n;
										prev.copiedFromFilename = filename;
									}

									// ;//System.out.println(" connected <"
									// + n +
									// "> to <"
									// +
									// prev
									// +
									// "> <"
									// + (n
									// ==
									// prev)
									// +
									// ">");

									break;
								}
								prev = q;
							}

							boolean f2 = date_matcher.find();
							assert f2;
							// try {
							// n.date = new
							// SimpleDateFormat("yyyy-MM-dd HH:mm Z").parse(date_matcher.group(1));
							n.date = new Date(Long.parseLong(date_matcher.group(1).split("\\.")[0]) * 1000L);
							// } catch
							// (ParseException e) {
							// e.printStackTrace();
							// }

							boolean f3 = desc_matcher.find();
							n.logEntry = desc_matcher.group(1);
							if (n.logEntry.equals("\"\""))
								n.logEntry = null;

							n.type = HistoryExplorerHG.VersionNodeType.copySource;
							n.copyToFilename = vd + "/" + m.group(1);

							n.path = filename;

							nodes.add(n);
							desc_matcher.find();
							at = desc_matcher.end() + 1;
						} else {
							desc_matcher.find();
							at = desc_matcher.end() + 1;
						}

						continue;
					}
					if (already != null) {
						desc_matcher.find();
						at = desc_matcher.end() + 1;

						// ;//System.out.println(" already seen <"
						// + rev_matcher.group(1) +
						// ">");
						// if (at < allText.length() +
						// 50)
						// ;//System.out.println(" string starts <"
						// + allText.substring(at, at +
						// 40) + ">");
						nodes.add(already);
					} else {
						foundSoFar.put(Long.parseLong(rev_matcher.group(1)), n);
						n.revision = Long.parseLong(rev_matcher.group(1));

						boolean f2 = date_matcher.find();
						assert f2;
						// try {
						// ;//System.out.println(" parsing date <"+date_matcher.group(1)+">");

						// n.date = new
						// SimpleDateFormat("yyyy-MM-dd HH:mm Z").parse(date_matcher.group(1));
						n.date = new Date(Long.parseLong(date_matcher.group(1).split("\\.")[0]) * 1000L);
						// // } catch (ParseException e)
						// {
						// e.printStackTrace();
						// }

						boolean f3 = desc_matcher.find();
						assert f3;
						n.logEntry = desc_matcher.group(1);
						if (n.logEntry.equals("\"\""))
							n.logEntry = null;

						// // copies template doesn't
						// work!
						// ExecuteCommand command = new
						// ExecuteCommand(new
						// File(filename).getParentFile().getAbsolutePath(),
						// HGVersioningSystem.hgCommand
						// + " log
						// -vfC -r"+n.revision+" "+
						// filename);
						// command.waitFor(true);

						n.path = filename;

						nodes.add(n);

						at = desc_matcher.end() + 1;

						copies_matcher.find();
						String c = copies_matcher.group(1);

						Pattern weAreACopy = Pattern.compile(shortname + " \\(([^\\)]*)\\)");
						Matcher m = weAreACopy.matcher(c);
						if (m.find()) {
							n.type = HistoryExplorerHG.VersionNodeType.copyTarget;
							n.copiedFromFilename = vd + "/" + m.group(1);
							filename = vd + "/" + m.group(1);
							shortname = m.group(1);
						} else {
						}

					}
				} else
					break;
			}
			for (int i = 0; i < nodes.size() - 1; i++) {
				nodes.get(i).next = nodes.get(i + 1);
				nodes.get(i + 1).previous = nodes.get(i);
			}
			return nodes;
		}

	}

	private final String fullPathToRepositoryDirectory;

	private final String fullPathToSheetDir;

	public HGTools(String fullPathToRepositoryDirectory, String sheetsub, String xmlFilename) {
		this.fullPathToRepositoryDirectory = fullPathToRepositoryDirectory;
		this.fullPathToSheetDir = fullPathToRepositoryDirectory + "/" + sheetsub;

	}

	static public String versioningDirAccountingForForest(String rootVersioningDir, String filenameInRepository) throws IOException {
		String root = new File(rootVersioningDir).getCanonicalPath();
		String name = new File(filenameInRepository).getCanonicalPath();
		if (new File(name).isDirectory()) {
			if (new File(name + "/.hg").exists()) {
				return name;
			}
		}
		if (name.equals(root))
			return root;
		if (name.equals("/")) {
			assert false;
			return root;
		}
		return versioningDirAccountingForForest(rootVersioningDir, new File(name).getParent());
	}

	public HGTools(VersioningSystem vs) {
		this.fullPathToRepositoryDirectory = vs.fullPathToRepositoryDirectory;
		this.fullPathToSheetDir = vs.fullPathToSheetDirectory;
	}

	public Object getOriginatingCopyFor(String file) {
		String fn = fullPathToSheetDir + "/" + file;
		HGLog o = new HGLog(fn);
		List<VersionNode> m = o.getVersionNodes();
		if (m.size() == 0)
			return null;
		long r = m.get(m.size() - 1).revision;

		ExecuteCommand command = new ExecuteCommand(fullPathToSheetDir, HGVersioningSystem.hgCommand + " cat -r " + r + " " + fn);
		command.waitFor(true);
		String out = command.getOutput();
		return VersioningSystem.objectRepresentationFor(out);
	}

	@Cached(name = "getPropetyAtVersion", max = 100)
	public Object getPropertyAtVersion(long version, String path, VisualElementProperty name) {

		if (path.endsWith(".xml")) return null;
		
		ExecuteCommand command = new ExecuteCommand(fullPathToSheetDir, HGVersioningSystem.hgCommand + " cat -r " + version + " " + path + (name == null ? "" : "/" + name.getName() + ".property"));
		command.waitFor(true);
		String out = command.getOutput();
		Object o = VersioningSystem.objectRepresentationFor(out);
		return o;
	}

}
