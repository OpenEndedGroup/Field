
#include <Cocoa/Cocoa.h>
#include "field_syphon_SyphonClient.h"
#include "Syphon.h"

/*
 * Class:     field_syphon_SyphonClient
 * Method:    getServerDescriptions
 * Signature: ()[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_field_syphon_SyphonClient_getServerDescriptions
(JNIEnv *env, jclass)
{
	NSArray *servers = [[SyphonServerDirectory sharedDirectory] serversMatchingName:nil appName:nil];
	
	jobjectArray ret= (jobjectArray)env->NewObjectArray([servers count],
										   env->FindClass("java/lang/String"),
										   env->NewStringUTF(""));
	for(int i=0;i<[servers count];i++)
	{
		NSDictionary *d = (NSDictionary *)[servers objectAtIndex:i];
		
		NSString *s1 = [d objectForKey:SyphonServerDescriptionAppNameKey];
		NSString *s2 = [d objectForKey:SyphonServerDescriptionNameKey];
		NSString *s3 = [d objectForKey:SyphonServerDescriptionUUIDKey];
		
		NSString *all = [NSString stringWithFormat:@"%@ - %@ - %@", s1, s2, s3];
		env->SetObjectArrayElement(
								   ret,i,env->NewStringUTF([all UTF8String]));
	}
	
	return ret;
}

JNIEXPORT jlong JNICALL Java_field_syphon_SyphonClient_initWithUUID
(JNIEnv *env, jobject o, jstring uuid)
{
	const char *cuid = env->GetStringUTFChars(uuid,NULL);
	NSString *nsuid = [NSString stringWithUTF8String:(const char *)cuid];
	NSArray *servers = [[SyphonServerDirectory sharedDirectory] serversMatchingName:nil appName:nil];
	
	for(int i=0;i<[servers count];i++)
	{
		NSDictionary *d = (NSDictionary *)[servers objectAtIndex:i];
		NSLog(@"looking at uid %@", [d objectForKey:SyphonServerDescriptionUUIDKey]);
		
		if ([[d objectForKey:SyphonServerDescriptionUUIDKey] compare:nsuid]==0)
		{
			SyphonClient *c = [[SyphonClient alloc] initWithServerDescription: d options:nil newFrameHandler:nil];
			return (jlong)c;
			
		}
	}
	return 0;	
}

/*
 * Class:     field_syphon_SyphonClient
 * Method:    hasNewFrame
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_field_syphon_SyphonClient_hasNewFrame
(JNIEnv *, jobject, jlong c)
{
	return [((SyphonClient *)c) hasNewFrame];
}

/*
 * Class:     field_syphon_SyphonClient
 * Method:    isValid
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_field_syphon_SyphonClient_isValid
(JNIEnv *, jobject, jlong c)
{	
	return [((SyphonClient *)c) isValid];
}

/*
 * Class:     field_syphon_SyphonClient
 * Method:    bindNow
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_field_syphon_SyphonClient_bindNow
(JNIEnv *, jobject, jlong c)
{
	SyphonImage *image = [ ((SyphonClient *)c) newFrameImageForContext:CGLGetCurrentContext()];
	if(image)
	{
		glEnable(GL_TEXTURE_RECTANGLE_ARB);
		glBindTexture(GL_TEXTURE_RECTANGLE_ARB, [image textureName]);
	}		
}

/*
 * Class:     field_syphon_SyphonClient
 * Method:    widthNow
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_field_syphon_SyphonClient_widthNow
(JNIEnv *, jobject, jlong c)
{
	CGLContextObj ctx = CGLGetCurrentContext();
	return [[((SyphonClient *)c) newFrameImageForContext:ctx] textureSize].width;
}

/*
 * Class:     field_syphon_SyphonClient
 * Method:    heightNow
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_field_syphon_SyphonClient_heightNow
(JNIEnv *, jobject, jlong c)
{
	CGLContextObj ctx = CGLGetCurrentContext();
	return [[((SyphonClient *)c) newFrameImageForContext:ctx] textureSize].height;
}
