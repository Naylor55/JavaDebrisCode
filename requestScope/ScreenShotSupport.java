
@Slf4j
@RequiredArgsConstructor
@Component
public class ScreenShotSupport {
    /**
     * 相机播放url缓存时间 ， 4分钟
     */
    private static final long URL_TTL_MS = 4 * 60 * 1000;
    private final ScreenShotRequestCache requestCache;
    /**
     * 截图
     * @param cameraIndexCode
     * @return
     */
    public String screenShot(String cameraIndexCode) {
        if (requestCache.containsScreenshot(cameraIndexCode)) {
            String cached = requestCache.getScreenshot(cameraIndexCode);
            log.debug("命中请求缓存 cameraIndexCode:{} -> {}", cameraIndexCode, cached);
            return cached;
        }
        String url = getValidCameraUrl(cameraIndexCode);
        if (url == null || url.isEmpty()) {
            return "";
        }
        String fileName = doScreenShot(cameraIndexCode, url);
        if (!fileName.isEmpty()) {
            requestCache.putScreenshot(cameraIndexCode, fileName);
        }
        return fileName;
    }
    /**
     * 截图
     * @param cameraIndexCode
     * @param url
     * @return
     */
    private String doScreenShot(String cameraIndexCode, String url) {
        String fileName = "/data/camera/screenshot/" + cameraIndexCode + "-" + LocalDateTimeUtil.getCurrentTime() + ".jpg";
        String requestUrl = xxxConfig.getxxServiceUrl() + "screenshot?name=" + fileName + "&url=" + URLEncoder.encode(url);
        JSONObject jsonObject = OpenAipHttpUtil.httpURLConectionGET(requestUrl);
        if (null != jsonObject ) {
            log.debug("截图成功cameraIndexCode:{} ,img_url:{}", cameraIndexCode, fileName);
            return fileName;
        } else {
            log.debug("截图失败cameraIndexCode:{} ,url:{};requestUrl:{}", cameraIndexCode, url, requestUrl);
            return "";
        }
    }
    /**
     * 获取有效的URL（带缓存+过期检查）
     */
    public String getValidCameraUrl(String cameraIndexCode) {
        if (requestCache.containsUrl(cameraIndexCode)) {
            CameraUrlCache cache = requestCache.getUrl(cameraIndexCode);
            if (System.currentTimeMillis() - cache.getCreateTime() < URL_TTL_MS) {
                log.debug("命中URL缓存 cameraIndexCode:{} -> {}", cameraIndexCode, cache.getUrl());
                return cache.getUrl();
            } else {
                log.debug("URL已过期 cameraIndexCode:{}", cameraIndexCode);
            }
        }
        // 没缓存或已过期，重新获取
        String newUrl = fetchCameraUrl(cameraIndexCode);
        if (!newUrl.isEmpty()) {
            requestCache.putUrl(cameraIndexCode, new CameraUrlCache(newUrl, System.currentTimeMillis()));
        }
        return newUrl;
    }
    /**
     * 调用接口获取新的URL
     */
    private String fetchCameraUrl(String cameraIndexCode) {
        GetVideoStreamingParam param = new GetVideoStreamingParam();
        param.setCameraIndexCode(cameraIndexCode);
        param.setProtocol("rtsp");
        Object streaming = xxxServiceClient.getStreamingUrl(param);
        if (streaming != null) {
            String url = streaming.getData();
            log.debug("获取视频流成功 cameraIndexCode:{} -> {}", cameraIndexCode, url);
            return url;
        } else {
            log.debug("获取视频流失败 cameraIndexCode:{}; param={}; response={}",
                    cameraIndexCode, JSON.toJSONString(param), JSON.toJSONString(streaming));
            return "";
        }
    }
}
