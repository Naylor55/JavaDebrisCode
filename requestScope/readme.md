## 背景

代码要实现的功能是：
* 1：feign调用A获取相机的播放串
* 2：feign调用B根据播放串获取截图
* 3： 相机是集合，该集合外层还有一堆逻辑和嵌套循环

现在发现的问题是整个流程下来总耗时太长，初步排查发现： 获取截图接口太慢，3s才返回一条数据；大量的http调用。

## 假设
假设即前提，无法改变的因素：
* 1：截图接口不做优化（如果可以优化直接解决问题）
* 2：获取播放串和获取截图无法合并为一个接口（如果可以合并将减少大量的http请求）
* 3：不要多线程调用截图接口（3s+的响应时间，其实多线程调用该接口也没有意义）
* 4：代码不增加异步处理机制（例如增加多线程和MQ解耦）
* 5：代码不增加批量处理机制（例如将多层嵌套的循环扁平化，较少数据库访问）
* 6：无法从需求角度减少数据量，无法从需求角度改变用户操作流程


## 解决方案
在毙掉了“假设”中的解决方案后，从缓存的思路解决这个问题，核心改动为：
* 以相机为维度缓存相机的截图
* 以相机为维度缓存相机的播放串



简单来说，将获取播放串和获取截图封装到一个组件中，当需要截图的时候，先检查缓存中是否有数据，若没有需要获取播放串执行截图并在得到截图后缓存起来，在获取播放串的时候先检查缓存中是否有数据，若没有才调用获取播放串接口并在得到播放串后缓存起来。

缓存作用域设定：
* 线程维度太细，并且如果采用ThreadLocal存储还有内存泄露的风险
* 全局维度太粗，而且也不符合需求，虽然从短周期来说允许截图相同，例如3m内截图一样是业务可以接受的，但是3h的话就无法接受了。
* **Request请求维度合适**，虽然全局维度通过一些自定义设置和逻辑处理也能达到要求，但是难免开发复杂，性价比不高。在SpringBoot中使用@RequestScope注解可以很好的处理请求级别的缓存。

* 由于视频汇聚平台对播放串有有效期控制，所以对于“播放串”的缓存需要在Request请求中进一步限定是否超过有效期，好在这个有效期是固定值，可以直接写死。


## 使用RequestScope缓存数据

首先增加一个 **ScreenShotRequestCache** 类，并添加 @RequestScope 注解，在其中定义两个缓存 map，一个用来存放播放串，另外一个用来存放截图。


```java


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




```

然后定义一个 **ScreenShotSupport** 类，用来处理获取播放串和获取截图，并且在里面增加缓存的功能。

具体来说，在外部代码需要获取截图时候：
* 先检查screenshotCache（截图缓存）中是否有数据
* 如果没有截图缓存数据，需要获取播放串并截图
* 获取播放串时候先检查urlCache缓存是否有数据并且是否满足有效期
* 如果没有播放串缓存或者缓存已过有效期，重新获取播放串并缓存下来
* 根据播放串截图并缓存下来




```java


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



```

