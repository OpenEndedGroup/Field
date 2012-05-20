package field.syphon;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL31.GL_TEXTURE_RECTANGLE;
import field.graphics.core.BasicContextManager;
import field.graphics.core.BasicTextures.BaseTexture;

public class SyphonClient {

	static {
		System.loadLibrary("syphon");
	}

	
	static public class ServerDescription
	{
		String name;
		String appname;
		String uid;
		
		protected ServerDescription(String c)
		{
			String[] cc = c.split(" - ");
			appname = cc[0];
			name = cc[1];
			uid = cc[2];
		}
	}
	
	
	static protected native String[] getServerDescriptions();

	static public ServerDescription[] getServers()
	{
		String[] ss = getServerDescriptions();
		
		ServerDescription[] d = new ServerDescription[ss.length]; 
		for(int i=0;i<ss.length;i++)
		{
			d[i] = new ServerDescription(ss[i]);
		}
		return d;
	}
	
	protected long id;

	public SyphonClient(ServerDescription uid) {
		id = initWithUUID(uid.uid);
	}

	public int width() {
		return widthNow(id);
	}

	public int height() {
		return widthNow(id);
	}

	public Texture texture() {
		return new Texture();
	}

	public boolean isValid() {
		return isValid(id);
	}

	public boolean hasNewFrame() {
		return hasNewFrame(id);
	}

	public void bindNow() {
		bindNow(id);
	}

	protected native long initWithUUID(String uuid);

	protected native boolean hasNewFrame(long id);

	protected native boolean isValid(long id);

	protected native void bindNow(long id);

	protected native int widthNow(long id);

	protected native int heightNow(long id);

	public class Texture extends BaseTexture {

		@Override
		protected void post() {
		}

		@Override
		protected void pre() {
			if (id != 0)
				bindNow(id);
			glEnable(GL_TEXTURE_RECTANGLE);
		}

		@Override
		protected void setup() {
			if (id != 0) {
				BasicContextManager.putId(this, (int) id);
				glTexParameteri(textureTarget, GL_TEXTURE_WRAP_S, gl_texture_wrap_s);
				glTexParameteri(textureTarget, GL_TEXTURE_WRAP_T, gl_texture_wrap_t);
				glTexParameteri(textureTarget, GL_TEXTURE_MAG_FILTER, gl_texture_mag_filter);
				glTexParameteri(textureTarget, GL_TEXTURE_MIN_FILTER, gl_texture_min_filter);
			}
		}

	}

}
