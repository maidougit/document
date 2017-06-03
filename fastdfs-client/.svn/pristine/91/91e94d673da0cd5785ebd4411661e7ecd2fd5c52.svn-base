package org.csource.fastdfs.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.csource.common.MyException;
import org.csource.common.NameValuePair;
import org.csource.fastdfs.*;

public class FileManager  implements FileManagerConfig {
	private static final long serialVersionUID = 1L;  
	  
    private static Log log  = LogFactory.getLog(FileManager.class);  
      
    private static TrackerClient  trackerClient;  
    private static TrackerServer  trackerServer;  
    private static StorageServer  storageServer;  
    private static StorageClient  storageClient;  
    private static  StorageClient1 storageClient1;
  
    static { // Initialize Fast DFS Client configurations  
          
        try {  
            String classPath = new File(FileManager.class.getResource("/").getFile()).getCanonicalPath();  
              
            String fdfsClientConfigFilePath = classPath + File.separator + CLIENT_CONFIG_FILE;  
              
            log.info("FastDFS 配置文件:" + fdfsClientConfigFilePath);  
            ClientGlobal.init(fdfsClientConfigFilePath);  
              
            trackerClient = new TrackerClient();  
            trackerServer = trackerClient.getConnection();  
              
            storageClient = new StorageClient(trackerServer, storageServer);  
            storageClient1 = new StorageClient1(trackerServer, storageServer); 
        } catch (Exception e) {  
        	log.error("fastdfs初始化错误", e);
        }  
    }  
    
    /**
     * 上传
     * @param fileBuff  文件内容(字节数组)
     * @param fileExtName  扩展名
     * @return
     * @throws IOException
     * @throws MyException
     */
    public static String upload(byte[] fileBuff,String fileExtName) throws IOException, MyException {
    	//设置元信息  
        NameValuePair[] metaList = new NameValuePair[0];  
        
//        metaList[0] = new NameValuePair("fileName", fileName);  
//        metaList[1] = new NameValuePair("fileExtName", fileExtName);  
//        metaList[2] = new NameValuePair("fileLength", fileLength);  
          
        //上传文件  
       String fileId = storageClient1.upload_file1(fileBuff, fileExtName, metaList);
       
       log.info("上传的文件路径：" + fileId);
       return fileId;
	}
    
    /**
     * 根据文件地址   删除文件
     * 
     * @param fileId    类似：group1/M00/00/00/rBD-t1byWCyATgmsAADWt48RAXA423.jpg
     * @return   
     * @throws IOException
     * @throws MyException
     */
    public static int delete(String fileId) throws IOException, MyException { 
    	return storageClient1.delete_file1(fileId); 
	}
    
   /**
    * 下载
    * @param fileId
    * @param localFilename  保存的本地服务器地址(例如：H:/111.jpg)
    */
    public static int download(String fileId,String localFilename) throws IOException, MyException { 
    	return storageClient1.download_file1(fileId, localFilename);
	}
    
     
     public static byte[] getBytesFromFile(File f) {
         if (f == null) {
             return null;
         }
         try {
             FileInputStream stream = new FileInputStream(f);
             ByteArrayOutputStream out = new ByteArrayOutputStream(1000);
             byte[] b = new byte[1000];
             for (int n;(n = stream.read(b)) != -1;) { 
		         out.write(b, 0, n); 
		     }
             stream.close();
             out.close();
             return out.toByteArray();
         } catch (IOException e){
         }
         return null;
     }
      
     public static void main(String[] args){
    	 File file = new File("H:/hangye_bottom_small.png");
    	 
    	 byte[] bytes =  getBytesFromFile(file);
    	 try {
    		 
			upload(bytes, "png" );
    		 //System.out.println(delete("group1/M00/00/00/rBD-t1byWCyATgmsAADWt48RAXA423.jpg"));
    		 //System.out.println(download("group1/M00/00/00/rBD-t1bzZKqAJj_DAAGCJ_qdtNw852.jpg", "H:/111.jpg"));
    		 
		} catch (IOException e) {
			e.printStackTrace();
		} catch (MyException e) {
			e.printStackTrace();
		}
     }
     
}
