本篇将围绕以下两个函数来展开，在[VLC遇到的问题](VLC遇到的问题.md)一文中，我们已经知道如何获取视频流，核心即为以下两个函数：

```Java
libvlc_video_set_callbacks
libvlc_video_set_format
```

但是这两个函数的上下文是什么尚不得而知，因此本文的目的就在于搞清楚这一点。先从libvlc_video_set_callbacks入手，实现在media_player.c中，如下：

```
void libvlc_video_set_callbacks( libvlc_media_player_t *mp,
    void *(*lock_cb) (void *, void **),
    void (*unlock_cb) (void *, void *, void *const *),
    void (*display_cb) (void *, void *),
    void *opaque )
{
    var_SetAddress( mp, "vmem-lock", lock_cb );
    var_SetAddress( mp, "vmem-unlock", unlock_cb );
    var_SetAddress( mp, "vmem-display", display_cb );
    var_SetAddress( mp, "vmem-data", opaque );
    var_SetString( mp, "vout", "vmem" );
    var_SetString( mp, "avcodec-hw", "none" );
}
```

这里看到是将回调函数缓存起来了，肯定在别的地方会取出来，我们搜一下vmem-lock，定位到vmem.c中。这是vlc的一个module，以下是其描述，

```
vlc_module_begin()
    set_description(N_("Video memory output"))
    set_shortname(N_("Video memory"))

    set_category(CAT_VIDEO)
    set_subcategory(SUBCAT_VIDEO_VOUT)
    set_capability("vout display", 0)

    add_integer("vmem-width", 320, T_WIDTH, LT_WIDTH, false)
        change_private()
    add_integer("vmem-height", 200, T_HEIGHT, LT_HEIGHT, false)
        change_private()
    add_integer("vmem-pitch", 640, T_PITCH, LT_PITCH, false)
        change_private()
    add_string("vmem-chroma", "RV16", T_CHROMA, LT_CHROMA, true)
        change_private()
    add_obsolete_string("vmem-lock") /* obsoleted since 1.1.1 */
    add_obsolete_string("vmem-unlock") /* obsoleted since 1.1.1 */
    add_obsolete_string("vmem-data") /* obsoleted since 1.1.1 */

    set_callbacks(Open, Close)
vlc_module_end()
```

这里我们注意到几个熟悉的参数，如vmem-width, vmem-height, vmem-lock, vmem-unlock, vmme-data等。另外，通过set_callbacks设置了这个module的打开和关闭时的回调。我们看看Open函数:

```
static int Open(vlc_object_t *object)
{
    vout_display_t *vd = (vout_display_t *)object;
    vout_display_sys_t *sys = malloc(sizeof(*sys));

    vlc_format_cb setup = var_InheritAddress(vd, "vmem-setup");

    sys->lock = var_InheritAddress(vd, "vmem-lock");
    sys->unlock = var_InheritAddress(vd, "vmem-unlock");
    sys->display = var_InheritAddress(vd, "vmem-display");
    sys->cleanup = var_InheritAddress(vd, "vmem-cleanup");
    sys->opaque = var_InheritAddress(vd, "vmem-data");
    sys->pool = NULL;

    ......

    vd->sys     = sys;
    vd->fmt     = fmt;
    vd->info    = info;
    vd->pool    = Pool;
    vd->prepare = NULL;
    vd->display = Display;
    vd->control = Control;
    vd->manage  = NULL;

    vout_display_SendEventFullscreen(vd, false);
    vout_display_SendEventDisplaySize(vd, fmt.i_width, fmt.i_height, false);
    return VLC_SUCCESS;
}
```

可见，这里从缓存中读出所有的回调，保存在vout_display_t中，fmt是和视频流格式相关的。vmem.c中除了Open外，还有几个熟悉的函数，Display，Lock，Unlock，这里面会调用我们传入的回调。我们重点关注Display的调用栈，是在vlc_vout_wrapper.h中调用的，

```
static inline void vout_display_Display(vout_display_t *vd,
    picture_t *picture, subpicture_t *subpicture) {
    vd->display(vd, picture, subpicture);
```

这个函数一直往上走的调用栈如下：
```
stream_out/display.c: 
Open -> Add -> 

decoder.c
input_DecoderCreate -> decoder_New -> CreateDecoder -> vout_new_buffer -> 

resource.c
input_resource_RequestVout -> RequestVout ->

video_output.c
vout_Request -> VoutCreate -> Thread -> ThreadDisplayPicture -> ThreadDisplayRenderPicture -> 

vlc_vout_wrapper.h
vout_display_Display ->

vmem.c
Display
```

display.c也是一个模块，其中有Open和Close回调。我们看看这个module的open是怎么触发的，在entry.c的vlc_plugin_setter中设置的module->pf_activate。依次往上查看调用栈如下：

```
libvlcjni.c
Java_org_videolan_libvlc_LibVLC_nativeNew

core.c
libvlc_new ->

libvlc.c
libvlc_InternalInit ->

bank.c
module_LoadPlugins -> module_InitStaticModules -> module_InitStatic -> 

entry.c
vlc_plugin_describe -> vlc_plugin_setter
```

经过这一番追本溯源，总算和起始入口打通了，但是有点乱，我们需要再给整个流程梳理一番，彻底走通。

整个流程包括：如何发起的Rtsp请求，获取响应数据，如何获取的视频流，如何本地渲染。

这些我们下文再分析。
