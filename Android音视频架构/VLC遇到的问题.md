摄像头是RTSP协议的，需要在Android端实时显示摄像头视频流，这里采用了开源的VLC播放器，可能会有如下需求：

> 一、有截屏的需求

> 二、有屏幕录制的需求

> 三、视频本来是横的，但是现在要竖屏显示，如何旋转视频，另外旋转后视频会拉伸，因此需要截取一段显示

> 四、显示的视频可能需要做额外处理，比如识别出人脸后框出来

先说说直接用VLC播放器的SDK会遇到的问题，利用SDK显示视频通常是如下写法：

```
private MediaPlayer mMediaPlayer;
private LibVLC mVlc;

void createPlayer(String url, int width, int height) {
    ArrayList<String> options = new ArrayList<>();
    options.add("--aout=opensles");
    options.add("--audio-time-stretch"); 
    options.add("-vvv"); 
    mVlc = new LibVLC(context, options);

    mMediaPlayer = new MediaPlayer(mVlc);
	IVLCVout vout = mMediaPlayer.getVLCVout();
	vout.setVideoView(textureView);
	vout.attachViews();

	vout.setWindowSize(width, height);

    Media m = new Media(mVlc, Uri.parse(url));
    int cache = 1000;
    m.addOption(":network-caching=" + cache);
    m.addOption(":file-caching=" + cache);
    m.addOption(":live-cacheing=" + cache);
    m.addOption(":sout-mux-caching=" + cache);
    m.addOption(":codec=mediacodec,iomx,all");
    mMediaPlayer.setMedia(m);
    mMediaPlayer.play();
}

public void releasePlayer() {
	mMediaPlayer.setVideoCallback(null, null);
    mMediaPlayer.stop();
	IVLCVout vout = mMediaPlayer.getVLCVout();
	vout.detachViews();
    mVlc.release();
    mVlc = null;
}
```

这里值得一提的是调setVideoView设置视频输出，可以是TextureView，也可以是SurfaceView，也可以是SurfaceTexture。我尝试过使用SurfaceTexture，然后当Frame Available时再从SurfaceTexture绘制到Window Surface上，结果显示出来的是一团糟，原因尚未查明。

另外为了避免视频播放时卡顿，最好加上cache。

接下来说说这种方式的局限：

1，对于截屏的需求，如果采用的SurfaceView，是无法getDrawingCache的。采用TextureView的话系统提供了接口获取截屏Bitmap的。尝试过采用SurfaceTexture绘制再glReadPixels获取RGBA这种办法失败了，最终的图像是混沌的。

2，对于视频录制，除非能拿到每一帧视频数据，否则无解，如果能从SurfaceTexture上拷出数据就行了，但是实践中发现拷出来的图像是混沌的，原因未明。

3，对于横竖屏切换，假如视频是横的，手机分辨率是1920 * 1080，如果要竖屏显示，需要对视频进行旋转，对于TextureView可以采用setTransform(Matrix)，为了避免视频拉伸需要截取一部分来显示，但是默认截取是从左到右或从上到下的，假如我要截取视频中间的部分就不行了。

4，对于额外处理的需求最靠谱的办法还是拿到视频流，离线渲染完成后再显示。

综上，解决一切问题的核心就是拿到视频流。网上关于截屏和视频录制的方案都是抄来抄去的，VLC的native层本来是有截屏和视频录制功能的，只是没开放给Java层，所以自己加几行代码开放出来重新编译一下就OK了。但是仍然没解决根本问题：拿到视频流。


为了解决这个问题，我们只能翻vlc的代码。首先给vlc-android的代码同步下来，然后编译一遍，建议在linux下编，过程中会遇到各种各样的问题，google并解决之。编译完后会在libvlc目录下生成一堆so文件，包括jni目录中的libc++_shared.so, libvlc.so, libvlcjni.so，还有private_libs目录中的libiomx.*.so和libanw.*.xo，另外还会output一个aar文件，我们直接用这个aar文件就好了，里面已经给so都打包了。


接下来正式看vlc的代码了，libvlc是重点，这个相当于一个中间层，是封装了给Android端用的。里面最终还是调用底层的vlc框架，我们就不用关注了。libvlc里有两个文件是重点，一个是libvlcjni.c，一个是libvlcjni-mediaplayer.c。

先看看libvlcjni-media_player.h头文件，里面介绍了一些关键的接口，注释非常详细，需要仔细阅读，获取视频流的答案就在里面。就是这两个函数：

```
LIBVLC_API
void libvlc_video_set_callbacks( libvlc_media_player_t *mp,
                                 libvlc_video_lock_cb lock,
                                 libvlc_video_unlock_cb unlock,
                                 libvlc_video_display_cb display,
                                 void *opaque );

LIBVLC_API
void libvlc_video_set_format( libvlc_media_player_t *mp, const char *chroma,
                              unsigned width, unsigned height,
                              unsigned pitch );
```

为了获取视频流，首先要调用libvlc_video_set_format设置视频流编码格式和宽高，然后调用libvlc_video_set_callbacks设置回调，里面有三个回调，我们用到的是lock和display，在lock中传入buffer，解码后的视频流会写到该buffer中，然后在display中将buffer回调到java层。

胜利的曙光依稀就在眼前，我们在libvlcjni-mediaplayer.c中插入以下代码：

```
void
Java_org_videolan_libvlc_MediaPlayer_nativeSetVideoFormat(JNIEnv *env, jobject thiz, 
    jstring format, jint width, jint height, jint pitch) {

    vlcjni_object *p_obj = VLCJniObject_getInstance(env, thiz);

    if (!p_obj)
        return;

    const char *formatStr = (*env)->GetStringUTFChars(env, format, NULL);

    libvlc_video_set_format(p_obj->u.p_mp, formatStr, width, height, pitch);

    (*env)->ReleaseStringUTFChars(env, format, formatStr);
}

struct myfield {
    jclass mediaPlayerClazz;
    jmethodID onDisplayCallback;
    jobject thiz;
    void *buffer;
} myfield;

static void *lock(void *data, void ** p_pixels) {
    *p_pixels = myfield.buffer;
    return NULL;
}

static void unlock(void *data, void *id, void * const * p_pixels) {
    
}

static pthread_mutex_t myMutex = PTHREAD_MUTEX_INITIALIZER;

static void display(void *data, void *id) {
    JavaVM *jvm = fields.jvm;

    JNIEnv *env;
   
    int stat = (*jvm)->GetEnv(jvm, (void **)&env, JNI_VERSION_1_2);
    if (stat == JNI_EDETACHED) {
        if ((*jvm)->AttachCurrentThread(jvm, (void **) &env, NULL) != 0) {
            return;
        }
    } else if (stat == JNI_OK) {
        //
    } else if (stat == JNI_EVERSION) {
        return;
    }

    pthread_mutex_lock(&myMutex);

    if (myfield.thiz != NULL) {
        (*env)->CallVoidMethod(env, myfield.thiz, myfield.onDisplayCallback);
    }

    pthread_mutex_unlock(&myMutex);

    (*jvm)->DetachCurrentThread(jvm);
}

void
Java_org_videolan_libvlc_MediaPlayer_nativeSetVideoBuffer(JNIEnv *env, jobject thiz, jobject buffer) {
    vlcjni_object *p_obj = VLCJniObject_getInstance(env, thiz);

    libvlc_media_player_t *mp = p_obj->u.p_mp;
    if (!mp) {
        return;
    }

    if (buffer == NULL) {
        (*env)->DeleteGlobalRef(env, myfield.mediaPlayerClazz);
        pthread_mutex_lock(&myMutex);
        (*env)->DeleteGlobalRef(env, myfield.thiz);
        myfield.thiz = NULL;
        pthread_mutex_unlock(&myMutex);
        return;
    }

    myfield.mediaPlayerClazz = (*env)->FindClass(env, "org/videolan/libvlc/MediaPlayer");
    myfield.mediaPlayerClazz = (jclass) (*env)->NewGlobalRef(env, myfield.mediaPlayerClazz);
    myfield.onDisplayCallback = (*env)->GetMethodID(env, myfield.mediaPlayerClazz, "onDisplay", "()V");
    myfield.thiz = (*env)->NewGlobalRef(env, thiz);
    myfield.buffer = (*env)->GetDirectBufferAddress(env, buffer);

    libvlc_video_set_callbacks(mp, lock, NULL, display, NULL);
}
```

要注意的是这里用到了fields.jvm，是在libvlcjni.c中的Jni_OnLoad时保存的全局JavaVM，要在struct fields中添加成员jvm。此外org.videolan.libvlc.MediaPlayer.java中添加代码如下：

```
public void setVideoFormat(String format, int width, int height, int pitch) {
    nativeSetVideoFormat(format, width, height, pitch);
}

private native void nativeSetVideoFormat(String format, int width, int height, int pitch);

private ByteBuffer mBuffer;
private MediaPlayCallback mCallback;

public void setVideoCallback(ByteBuffer buffer, MediaPlayCallback callback) {
    mBuffer = buffer;
    mCallback = callback;
    nativeSetVideoBuffer(buffer);
}

private native void nativeSetVideoBuffer(ByteBuffer buffer);

private void onDisplay() {
    if (mCallback != null) {
        mCallback.onDisplay(mBuffer);
    }
}
```

MediaPlayerCallback.java定义如下，直接返回了视频流buffer。
```
public interface MediaPlayCallback {
    public void onDisplay(ByteBuffer buffer);
}
```

这个视频流是RGBA的，我们可以用OpenGL来渲染。剩下的代码我就不贴了