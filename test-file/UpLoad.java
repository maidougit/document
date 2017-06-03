package com.ycg.exteriorline.web.action.util;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.csource.common.MyException;
import org.csource.fastdfs.util.FileManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.commons.CommonsMultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

import com.ja.core.util.CommomUtil;
import com.ja.core.util.ImgCompress;

/****
 * 上传
 * @author zjs
 */
public class UpLoad {
    private static Log log = LogFactory.getLog(UpLoad.class);
    // 缓存文件头信息-文件头信息
    //public static final HashMap<String, String> mFileTypes = new HashMap<String, String>();

   
    /**
     * 使用fastdfs上传 （支持多文件上传）
     * @param fileType 存于CommomUtil.FILE_TYPE
     * @return Map<"jsp中file的id", Map<"文件服务器上文件名", "上传结果">>
     * @author zjs 异步上传
     */
    public static FutureTask<Map<String, Map<String, Boolean>>> upLoadAsyn(final HttpServletRequest request, HttpServletResponse response, final String fileType){

        //Map<String, Map<String, Boolean>> returnMap = new HashMap<String, Map<String, Boolean>>();
       // new Thread(new Runnable() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        FutureTask<Map<String, Map<String, Boolean>>> task = new FutureTask<Map<String, Map<String, Boolean>>>(new Callable<Map<String, Map<String, Boolean>>>() {
            @Override
                public Map<String, Map<String, Boolean>> call() {
                    log.info("---------文件上传开始开始时间 : " + System.currentTimeMillis());
                    
                    final Map<String, Map<String, Boolean>> resultMap = new HashMap<String, Map<String, Boolean>>();
                    // 解析器解析request的上下文
                    CommonsMultipartResolver multipartResolver = new CommonsMultipartResolver(request.getSession().getServletContext());

                    // 先判断request中是否包涵multipart类型的数据，
                    if (multipartResolver.isMultipart(request)) {
                        // /再将request中的数据转化成multipart类型的数据
                        MultipartHttpServletRequest multiRequest = (MultipartHttpServletRequest) request;
                        @SuppressWarnings("rawtypes")
                        Iterator iter = multiRequest.getFileNames();
                        int imgNumber = 0;
                        while (iter.hasNext()) { 
                            iter.next();
                            imgNumber++;
                        }
                        final CountDownLatch latch = new CountDownLatch(imgNumber);
                        ExecutorService threadPool = Executors.newFixedThreadPool(imgNumber);

                        iter = multiRequest.getFileNames();
                        while (iter.hasNext()) {
                            final MultipartFile file = multiRequest.getFile((String) iter.next());

                            threadPool.execute(new Runnable() {
                                @Override
                                public void run() {
                                    log.info("图片上传执行前时间 : " + System.currentTimeMillis());
                                    Map<String, Boolean> map = new HashMap<String, Boolean>();

                                  //获取图片名
                                    String originalFilename = file.getOriginalFilename();
                                    if (null == originalFilename || "".equals(originalFilename)) {
                                        //图片名字不正确，结束线程
                                        latch.countDown();
                                        return;
                                        //continue;
                                    }
                                    //获取file名
                                    String name = file.getName();
                                    CommonsMultipartFile cf = (CommonsMultipartFile) file;
                                    DiskFileItem fi = (DiskFileItem) cf.getFileItem();
                                    File fiGet = fi.getStoreLocation();
                                    String path2 = fiGet.getPath();
                                    path2 = path2.replaceAll("\\\\", "\\\\\\\\");
                                    log.info(path2);
                                    try {
                                        log.info("压缩图片开始");
                                        ImgCompress imgCompress = new ImgCompress(path2);
                                        log.info("压缩图片初始化");
                                        Map<String, Object> resizeFix = imgCompress.resizeFix(1000, 1000);
                                        byte[] buffer = (byte[]) resizeFix.get("byte");
                                        log.info("压缩图片结束");
                                        Map<String, String> mfileMap = clearAndCreateMfileTypes(fileType);
                                        if (getFileTypeByStream(mfileMap, buffer) == null) {
                                            log.error("文件格式不正确");
                                            map.put(name, false);
                                            synchronized(resultMap) {
                                                resultMap.put(name, map);
                                            }
                                            //fileUploadResultMap.get().put(name, map);
                                            log.info("文件结果 : name" + name + ",map : " + map);
                                            //returnMap.put(name, map);
                                            //return returnMap;
                                        }
                                        // 获取图片的扩展名
                                        String extensionName = originalFilename.substring(originalFilename.lastIndexOf(".") + 1);
                                        synchronized(FileManager.class) {
                                            String tempPath = FileManager.upload(buffer, extensionName);
                                            map.put(tempPath, true);
                                        }          
                                        
                                        log.info("before:"+resultMap);                                        
                                        //fileUploadResultMap.get().put(name, map);
                                        synchronized(resultMap) {
                                            resultMap.put(name, map);
                                        }
                                        log.info("after:"+resultMap);     
                                        log.info("文件结果 : name" + name + ",map : " + map);
                                        //returnMap.put(name, map);
                                    } catch (IOException ioException) {
                                        map.put(name, false);
                                        //returnMap.put(name, map);
                                        log.error("上传图片失败", ioException);
                                    } catch (MyException myException) {
                                        map.put(name, false);
                                        //returnMap.put(name, map);
                                        log.error("上传图片失败", myException);
                                    } catch (Exception exception) {
                                        map.put(name, false);
                                        //returnMap.put(name, map);
                                        log.error("上传图片失败", exception);
                                    } finally {
                                        latch.countDown();
                                    }
                                    
                                    log.info("图片上传执行后时间 : " + System.currentTimeMillis());
                                }
                            });
                        }

                        try {
                            //最长等待15秒
                            latch.await(15, TimeUnit.SECONDS);
                        } catch (InterruptedException ex) {
                          log.error("上传异常 : ", ex);
                        }
                    }

                    log.info("---------文件上传结束时间 : " + System.currentTimeMillis());
                    //log.info("before return:"+fileUploadResultMap.get());     
                    //return fileUploadResultMap.get();
                    log.info("before return:"+resultMap); 
                    return resultMap;
            }
        });
        
        executor.execute(task);

        return task;
    }
    
    /**
     * 使用fastdfs上传 （支持多文件上传）
     * @param fileType 存于CommomUtil.FILE_TYPE
     * @return Map<"jsp中file的id", Map<"文件服务器上文件名", "上传结果">>
     * @author zjs
     */
    public static Map<String, Map<String, Boolean>> upLoad(HttpServletRequest request, HttpServletResponse response, String fileType){
        Map<String, Map<String, Boolean>> returnMap = new HashMap<String, Map<String, Boolean>>();
        log.info("---------文件上传开始");

        // 解析器解析request的上下文
        CommonsMultipartResolver multipartResolver = new CommonsMultipartResolver(request.getSession().getServletContext());

        // 先判断request中是否包涵multipart类型的数据，
        if (multipartResolver.isMultipart(request)) {
            // /再将request中的数据转化成multipart类型的数据
            MultipartHttpServletRequest multiRequest = (MultipartHttpServletRequest) request;
            @SuppressWarnings("rawtypes")
            Iterator iter = multiRequest.getFileNames();
            while (iter.hasNext()) {

                Map<String, Boolean> map = new HashMap<String, Boolean>();
                MultipartFile file = multiRequest.getFile((String) iter.next());
                //获取图片名
                String originalFilename = file.getOriginalFilename();
                if (null == originalFilename || "".equals(originalFilename)) {
                    continue;
                }
                //获取file名
                String name = file.getName();
                CommonsMultipartFile cf = (CommonsMultipartFile) file;
                DiskFileItem fi = (DiskFileItem) cf.getFileItem();
                File fiGet = fi.getStoreLocation();
                String path2 = fiGet.getPath();
                path2 = path2.replaceAll("\\\\", "\\\\\\\\");
                log.info(path2);
                try {
                    log.info("压缩图片开始");
                    ImgCompress imgCompress = new ImgCompress(path2);
                    log.info("压缩图片初始化");
                    Map<String, Object> resizeFix = imgCompress.resizeFix(1000, 1000);
                    byte[] buffer = (byte[]) resizeFix.get("byte");
                    log.info("压缩图片结束");
                    clearAndCreateMfileTypes(fileType);
                    Map<String, String> mfileMap = clearAndCreateMfileTypes(fileType);
                    if (getFileTypeByStream(mfileMap, buffer) == null) {
                        log.error("文件格式不正确");
                        map.put(name, false);
                        returnMap.put(name, map);
                        return returnMap;
                    }
                    // 获取图片的扩展名
                    String extensionName = originalFilename.substring(originalFilename.lastIndexOf(".") + 1);
                    String tempPath = FileManager.upload(buffer, extensionName);
                    map.put(tempPath, true);
                    returnMap.put(name, map);
                } catch (IOException ioException) {
                    map.put(name, false);
                    returnMap.put(name, map);
                    log.error("上传图片失败", ioException);
                } catch (MyException myException) {
                    map.put(name, false);
                    returnMap.put(name, map);
                    log.error("上传图片失败", myException);
                } catch (Exception exception) {
                    map.put(name, false);
                    returnMap.put(name, map);
                    log.error("上传图片失败", exception);
                }
            }
        }
        return returnMap;
    }

    /**
     * 删除服务器上的图片
     * @param fileIds 由filedId(如"group1/M00/00/DB/rBD-t1gr8paAL8_AAAHcGECHyQo709.jpg")组成
     *
     */
    public static boolean del(List<String> fileIds){
        int boo = 0;
        try {
            for (String fileId : fileIds) {
                boo += FileManager.delete(fileId);
            }
        } catch (IOException ioException) {
            // TODO Auto-generated catch block
            log.error(ioException);
        } catch (MyException myException) {
            // TODO Auto-generated catch block
            log.error(myException);
        }
        return boo == 0?true:false;

    }
    public static  String getFileHexString(byte[] b) {
        StringBuilder stringBuilder = new StringBuilder();
        if (b == null || b.length <= 0) { return null; }
        for (int i = 0; i < b.length; i++) {
            int v = b[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    public static String getFileTypeByStream(Map<String, String> mFileTypes, byte[] b) {
        String filetypeHex = String.valueOf(getFileHexString(b));
        Iterator<Entry<String, String>> entryiterator = mFileTypes.entrySet().iterator();
        while (entryiterator.hasNext()) {
            Entry<String, String> entry = entryiterator.next();
            String fileTypeHexValue = entry.getKey().toUpperCase();
            if (filetypeHex.toUpperCase().startsWith(fileTypeHexValue)) { return entry.getValue(); }
        }
        return null;
    }

    private static Map<String, String> clearAndCreateMfileTypes(String fileType){
        //mFileTypes.clear();
        Map<String, String> mFileTypes = null;
        if (fileType.equals(CommomUtil.FILE_TYPE_AUDIO)) {
            mFileTypes = getAudioFileType();
        } else if (fileType.equals(CommomUtil.FILE_TYPE_IMG)) {
            mFileTypes = getImgFileType();
        } else if (fileType.equals(CommomUtil.FILE_TYPE_VIDEO)) {
            mFileTypes = getVideoFileType();
        }
        
        return mFileTypes;
    }
    // 图片类型目前以下几类
    public static  Map<String, String> getImgFileType() {
        Map<String, String> mFileTypes = new HashMap<String, String>(); 
        // images
        mFileTypes.put("FFD8FF", "jpg");
        mFileTypes.put("89504E47", "png");
        mFileTypes.put("47494638", "gif");
        mFileTypes.put("49492A00", "tif");
        mFileTypes.put("424D", "bmp");
        return mFileTypes;
    }

    // 视频类型：待添加
    public static  Map<String, String> getVideoFileType() {
        return Collections.EMPTY_MAP;
    }

    // 音频类型：待添加
    public static  Map<String, String> getAudioFileType() {
        return Collections.EMPTY_MAP;
    }

}