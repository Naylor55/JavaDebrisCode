

@Slf4j
@Component
@RequestScope
public class ScreenShotRequestCache {
    /**
     * 存储 cameraIndexCode -> CameraUrlCache
     */
    private final Map<String, CameraUrlCache> urlCache = new HashMap<>();
    /**
     * 存储 cameraIndexCode -> 截图结果
     */
    private final Map<String, String> screenshotCache = new HashMap<>();
    /**
     * 获取播放url
     * @param cameraIndexCode
     * @return
     */
    public CameraUrlCache getUrl(String cameraIndexCode) {
        return urlCache.get(cameraIndexCode);
    }
    public void putUrl(String cameraIndexCode, CameraUrlCache cache) {
        urlCache.put(cameraIndexCode, cache);
    }
    public boolean containsUrl(String cameraIndexCode) {
        return urlCache.containsKey(cameraIndexCode);
    }
    /**
     * 获取截图
     * @param cameraIndexCode
     * @return
     */
    public String getScreenshot(String cameraIndexCode) {
        return screenshotCache.get(cameraIndexCode);
    }
    public void putScreenshot(String cameraIndexCode, String fileName) {
        screenshotCache.put(cameraIndexCode, fileName);
    }
    public boolean containsScreenshot(String cameraIndexCode) {
        return screenshotCache.containsKey(cameraIndexCode);
    }
}
