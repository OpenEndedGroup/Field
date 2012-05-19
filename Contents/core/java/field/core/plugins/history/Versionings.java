package field.core.plugins.history;

import java.io.File;


public class Versionings {

	public static final String NoProperty = "(no property)";

	static public class Path
	{
		public final String sheet;
		public final String uid;
		public final String propertyName;
		
		public Path(String sheet, String uid, String propertyName)
		{
			this.sheet = sheet;
			this.uid = uid;
			this.propertyName = propertyName;
		}		
	
		@Override
		public String toString() {
			return "path:<"+sheet+"/"+uid+"/"+propertyName+">";
		}
		
		@Override
		public int hashCode() {
			return sheet.hashCode()+uid.hashCode()+propertyName.hashCode();
		}
		
		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof Path)) return false;
			return ((Path)obj).sheet.equals(sheet) &&  ((Path)obj).uid.equals(uid) && ((Path)obj).propertyName.equals(propertyName);
		}

		public boolean isSameElementAs(Path path) {
			return path.sheet.equals(sheet) && path.uid.equals(uid);
		}

		public String getPath() {
			return sheet+"/"+uid+(propertyName == NoProperty ? "" : ("/"+propertyName+".property"));
		}
		
		public File getFile()
		{
			return new File(getPath());
		}
	}
	
	// operations \u2014 all other operations are implied by the .xml file, we need to watch for copying from non-existant uid's (which have been added and deleted)
	// ideally, we should commit to the database before each deletion, if a copySource has occurred.
	
	// we are now in a position to monitor copies and adds of properties from within DefaultElement
	// we can't do this in a plug-in, because it relates to the actual storage of things inside iVisualElements.
	// we might even consider doing this inside VisualElement
	
	static public interface iOperation
	{
	}
	
	static public class CopyElementOp implements iOperation
	{
		Path oldElement;
		Path newElement;
	}
	
	static public class DeleteElementOp implements iOperation
	{
		Path oldElement;
	}
	
	static public class CopyPropertyOp implements iOperation
	{
		Path oldProperty;
		Path newProperty;
	}
	
	static public class DeletePropertyOp implements iOperation
	{
		Path oldProperty;
	}

}
