package field.graphics.core;

import org.lwjgl.opengl.GL30;

import field.graphics.core.Base.StandardPass;
import field.graphics.core.BasicFrameBuffers.NullTexture;
import field.graphics.core.BasicTextures.TextureUnit;
import field.graphics.core.BasicUtilities.OnePassElement;

/*
 * 
 * 
in vec3 position;


out vec4 vertexColor;
out vec3 nn;
in vec4 s_Color;
in vec3 s_Normal;

out int id;

void main()
{
	gl_Position =  ( vec4(position, 1.0));

	id = gl_VertexID;
	nn = s_Normal;

	vertexColor = s_Color;
}


--

#version 420

layout(binding=0, rgba32f)  coherent restrict uniform image2D state;

layout(points) in;
layout(points, max_vertices = 4) out;

uniform mat4 _projMatrix;
uniform mat4 _viewMatrix;

uniform mat4 _projMatrix0;
uniform mat4 _viewMatrix0;

uniform mat4 _projMatrix1;
uniform mat4 _viewMatrix1;

in int[] id;

in vec3[] nn;


vec4 crossTrain(float value, float delta)
{
	float d = value/delta;
	
	float fd = fract(d);

	float d1 = d-fd;
	float d2 = d1+1;

	float a1 = 1-fd;
	float a2 = fd;

	return vec4(d1*delta, d2*delta, a1, a2);
}

out float fad;

void main()
{

	ivec2 state_at =  ivec2(id[0]%8192, id[0]/8192);
	vec4 st = imageLoad(state,state_at);

	st *= 0.99;

	vec4 d = _projMatrix * _viewMatrix * (gl_in[0].gl_Position);
	d.xyz /= d.w;

	st.x += max(0, -0.004+pow(length(d.xy),2)*0.02);

	imageStore(state, state_at, st);

	float p1 = sin(id[0]*0.2);

	vec4 ct = crossTrain(st, 0.5+0.4*abs(p1));

	gl_Position = _projMatrix0 * _viewMatrix0 * (gl_in[0].gl_Position+vec4(nn[0]*ct.x,0));
	fad = ct.z;
	gl_Layer=0;
	EmitVertex();

	gl_Position = _projMatrix1 * _viewMatrix1 * (gl_in[0].gl_Position+vec4(nn[0]*ct.x,0));
	fad = ct.z;
	gl_Layer=1;
	EmitVertex();

	gl_Position = _projMatrix0 * _viewMatrix0 * (gl_in[0].gl_Position+vec4(nn[0]*ct.y,0));
	fad = ct.w;
	gl_Layer=0;
	EmitVertex();

	gl_Position = _projMatrix1 * _viewMatrix1 * (gl_in[0].gl_Position+vec4(nn[0]*ct.y,0));
	fad = ct.w;
	gl_Layer=1;
	EmitVertex();
	
} */
public class BasicGLSLangImage_dynamic extends OnePassElement{


	private PointList target;
	private final int height;
	private final int unit;
	private final int stride;

	public BasicGLSLangImage_dynamic(PointList target, int stride, int height, int unit) {
		super(StandardPass.preRender);
		this.target = target;
		this.stride = stride;
		this.height = height;
		this.unit = unit;
	}
	
	NullTexture texture = null;
	int width = 0;
	BasicGLSLangImage image = null;
	private TextureUnit textureWrap;
	
	@Override
	public void performPass() {
		int nc= target.vertex(false).limit()/3;

		if (texture==null || width<nc)
		{
//			BasicFrameBuffers.use32 = true;
			BasicFrameBuffers.use32 = false;
			BasicFrameBuffers.useRG = true;
			
			int num = target.vertex(false).capacity()/3;
			texture = new NullTexture(stride, (1+num/stride)*height);
			textureWrap = new TextureUnit(unit, texture);
//			image = new BasicGLSLangImage(unit, texture,  GL30.GL_RGBA32F);
			image = new BasicGLSLangImage(unit, texture,  GL30.GL_RG16F);
			width = stride*(1+num/stride);
			System.out.println(" reallocted texture at dimensions <"+stride+"> <"+(1+num/stride)*height+"> <"+width+">");
		}
		
		textureWrap.performPass(null);
		image.performPass(null);
	}

	
	
}
